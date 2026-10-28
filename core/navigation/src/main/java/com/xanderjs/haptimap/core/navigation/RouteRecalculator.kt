package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.ConnectivityPolicy
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.RouteResult
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

/**
 * Recalcula la ruta al desviarse (paso 08). Pide en OffRoute cuando la política lo permite (dispara
 * Recalculate) y hace la petición al entrar en Recalculating, que también dispara Reintentar desde la
 * pantalla. Volver a la ruta sale de Recalculating y cancela la petición. Ver ARCHITECTURE.md, "Off route".
 *
 * Recibe la petición como función para que :core:navigation no dependa de :core:network: :app le pasa
 * RoutesRepository.computeRoute (en debug sin clave, una ruta falsa).
 */
@Singleton
class RouteRecalculator @Inject constructor(private val store: NavigationSessionStore, private val engine: NavigationEngine) {

    suspend fun follow(
        computeRoute: suspend (origin: GeoPoint, destination: Place) -> RouteResult,
        connectivity: StateFlow<ConnectivityStatus>,
        thresholds: NavigationThresholds = NavigationThresholds(),
        timeSource: TimeSource = TimeSource.Monotonic,
    ) {
        var lastRequest: TimeMark? = null
        var requests = 0
        var episode = 0
        var inEpisode = false
        // por desvío: peticiones automáticas, fallos (para el backoff) y si el error no se arregla reintentando solo
        var attempts = 0
        var failures = 0
        var stopped = false
        store.state.collectLatest { session ->
            if (session !is NavigationSession.OffRoute && session !is NavigationSession.Recalculating) {
                inEpisode = false
                return@collectLatest
            }
            if (!inEpisode) {
                inEpisode = true
                episode++
                attempts = 0
                failures = 0
                stopped = false
            }
            if (session is NavigationSession.OffRoute) {
                if (stopped || attempts >= thresholds.maxAutoRecalculations) return@collectLatest
                delay(if (failures == 0) thresholds.recalculationDelay else backoff(failures, thresholds))
                // sin red o sin posición reciente se espera, sin contar intentos; al volver se pide sola
                connectivity.first(ConnectivityPolicy::canRequestRoute)
                engine.progress.first { it != null && !it.weakSignal && it.position != null }
                attempts++
                store.dispatch(NavigationEvent.Recalculate)
                return@collectLatest
            }
            val route = (session as NavigationSession.Recalculating).route
            lastRequest?.let { delay(thresholds.recalculationMinInterval - it.elapsedNow()) }
            val origin = engine.progress.value?.position
            if (!ConnectivityPolicy.canRequestRoute(connectivity.value) || origin == null) {
                logger.info("Not requested: ${if (origin == null) "no position" else "offline"}")
                store.dispatch(NavigationEvent.RecalculationFailed(if (origin == null) SessionError.ROUTE_UNAVAILABLE else SessionError.NO_CONNECTION))
                return@collectLatest
            }
            requests++
            lastRequest = timeSource.markNow()
            val id = "#$requests (episode $episode, auto $attempts/${thresholds.maxAutoRecalculations})"
            logger.info("Request $id from $origin to ${route.destination.name}")
            val result = try {
                computeRoute(origin, route.destination)
            } catch (e: CancellationException) {
                logger.info("Request $id cancelled: back on route")
                throw e
            }
            // una respuesta que no se pudo cancelar y llega ya de vuelta en la ruta: el reductor la ignora
            if (!currentCoroutineContext().isActive) logger.info("Request $id cancelled: back on route; its late result is ignored")
            when (result) {
                is RouteResult.Success -> {
                    logger.info("Request $id: ${result.route.distanceMeters} m")
                    store.dispatch(NavigationEvent.RouteRecalculated(result.route))
                }
                is RouteResult.Failure -> {
                    failures++
                    stopped = result.error in FINAL_ERRORS
                    logger.info("Request $id failed: ${result.error}${if (stopped) ", no more automatic requests" else ""}")
                    val error = if (result.error == RouteError.NO_CONNECTION) SessionError.NO_CONNECTION else SessionError.ROUTE_UNAVAILABLE
                    store.dispatch(NavigationEvent.RecalculationFailed(error))
                }
            }
        }
    }

    private fun backoff(failures: Int, thresholds: NavigationThresholds): Duration =
        minOf(thresholds.recalculationMinInterval * (1 shl (failures - 1).coerceAtMost(SHIFT_CAP)), thresholds.recalculationMaxBackoff)

    private companion object {
        // reintentar no los arregla: sin clave, clave no válida, cuota agotada
        val FINAL_ERRORS = setOf(RouteError.MISSING_KEY, RouteError.API_INVALID_KEY, RouteError.API_QUOTA)
        const val SHIFT_CAP = 10
        val logger: Logger = Logger.getLogger("HaptiReroute")
    }
}
