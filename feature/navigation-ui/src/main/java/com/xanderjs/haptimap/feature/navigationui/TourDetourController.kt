package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.intelligence.PoiEvaluation
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.MeasuredPath
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteResult
import com.xanderjs.haptimap.core.model.distanceTo
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationProgress
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationThresholds
import com.xanderjs.haptimap.core.navigation.navigatingRoute
import com.xanderjs.haptimap.core.navigation.navigationProfile
import com.xanderjs.haptimap.core.network.ApiCallBudget
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Orquesta el desvío a un POI del Modo Tour (paso 20) sobre la máquina pura [reduce]. Vive a nivel de proceso, como
 * [TourSuggestionController], y toma el relevo en la costura ACCEPTED: al aceptarse una sugerencia, pide UNA ruta con
 * el POI como parada intermedia (actual → POI → destino original: una sola llamada cubre la ida y la vuelta), la navega
 * con [NavigationEvent.FollowRoute], avisa con el háptico [HapticEvent.POI_ARRIVAL] al alcanzar el POI y sigue solo
 * hacia el destino. Cancelar repone la ruta original: sin pedir otra si se sigue cerca ([FollowRoute] de la cacheada),
 * y con una sola llamada si se está lejos. Respeta [ApiCallBudget] (no empieza un desvío sin presupuesto).
 *
 * Coste en llamadas a Routes: aceptar = 1; cancelar cerca de la ruta = 0; cancelar lejos = 1; fuera de ruta durante el
 * desvío = las del recálculo normal ([com.xanderjs.haptimap.core.navigation.RouteRecalculator]), ya contadas en el presupuesto.
 *
 * No conoce la UI: expone [state] para la tarjeta de desvío y [cancel] como entrada; [accept] es el relevo que invoca
 * [TourSuggestionController]. La ruta original se conserva en [DetourStore] durante todo el desvío (para cancelar sin
 * pedir y para restaurar tras la muerte del proceso); la del desvío la guarda ya la caché de ruta normal (es la activa).
 */
class TourDetourController(
    private val sessions: Flow<NavigationSession>,
    private val progress: Flow<NavigationProgress?>,
    private val computeRoute: suspend (origin: GeoPoint, destination: Place, intermediates: List<Place>) -> RouteResult,
    private val dispatch: (NavigationEvent) -> Unit,
    private val playHaptic: (HapticEvent) -> Unit,
    private val budget: ApiCallBudget,
    private val store: DetourStore,
    private val thresholds: NavigationThresholds = NavigationThresholds(),
    private val poiPause: Duration = POI_PAUSE,
) {
    private val _state = MutableStateFlow<TourDetour>(TourDetour.Inactive)

    /** El desvío en curso (o [TourDetour.Inactive]). La tarjeta de desvío lo pinta; las sugerencias se pausan si [TourDetour.active]. */
    val state: StateFlow<TourDetour> = _state.asStateFlow()

    private val actions = Channel<Action>(Channel.UNLIMITED)

    private sealed interface Action {
        data class Accept(val poi: PoiEvaluation) : Action
        data object Cancel : Action
    }

    /** Relevo de la costura ACCEPTED (lo llama [TourSuggestionController]): empieza el desvío al POI aceptado. */
    fun accept(poi: PoiEvaluation) { actions.trySend(Action.Accept(poi)) }

    /** Cancela el desvío (botón de la tarjeta): repone la ruta original. Sin desvío no hace nada. */
    fun cancel() { actions.trySend(Action.Cancel) }

    suspend fun run() = coroutineScope {
        val session = sessions.stateIn(this)
        val prog = progress.stateIn(this)
        restoreInto(session)
        while (true) {
            when (val s = _state.value) {
                TourDetour.Inactive -> begin(awaitAccept(), session, prog)
                is TourDetour.Loading -> Unit // begin() lo resuelve a ToPoi o Inactive; no se queda aquí
                is TourDetour.ToPoi -> onToPoi(s, session, prog)
                is TourDetour.PoiReached -> onPoiReached(s, session, prog)
                is TourDetour.Returning -> onReturning(s, session, prog)
            }
        }
    }

    /** Empieza el desvío: pide la ruta (actual → POI → destino) y la navega, o se queda en la original si no se puede. */
    private suspend fun begin(poi: PoiEvaluation, session: StateFlow<NavigationSession>, prog: StateFlow<NavigationProgress?>) {
        val original = session.value.navigatingRoute
        val origin = prog.value?.position
        // sin ruta en curso, fuera de TOUR, sin posición o sin presupuesto: no se empieza (se sigue el trayecto normal)
        if (original == null || session.value.navigationProfile != NavigationProfile.TOUR || origin == null || !budget.canQuery()) return
        _state.value = reduce(_state.value, TourDetourEvent.Start(poi, original)) // -> Loading
        val result = computeRoute(origin, original.destination, listOf(poi.toPlace()))
        when {
            pollCancel() -> { // cancelado mientras cargaba: seguimos en la ruta original, no navegamos el desvío
                _state.value = reduce(_state.value, TourDetourEvent.Cancel)
                store.clear()
            }
            result is RouteResult.Success -> {
                dispatch(NavigationEvent.FollowRoute(result.route)) // navega el desvío
                _state.value = reduce(_state.value, TourDetourEvent.Loaded(result.route)) // -> ToPoi
                store.save(_state.value)
            }
            else -> { // Failure: no hay desvío, se sigue la ruta original
                _state.value = reduce(_state.value, TourDetourEvent.LoadFailed)
                store.clear()
            }
        }
    }

    private suspend fun onToPoi(s: TourDetour.ToPoi, session: StateFlow<NavigationSession>, prog: StateFlow<NavigationProgress?>) =
        when (race(
            { awaitCancel(); Outcome.CANCEL },
            { awaitPoiReached(s.poi, prog); Outcome.REACHED },
            { awaitRouteGone(s.detourRoute, session); Outcome.ENDED },
        )) {
            Outcome.CANCEL -> doCancel(s, prog)
            Outcome.REACHED -> {
                playHaptic(HapticEvent.POI_ARRIVAL)
                _state.value = reduce(_state.value, TourDetourEvent.ReachPoi) // -> PoiReached
                store.save(_state.value)
            }
            Outcome.ENDED -> endUnderfoot() // la ruta cambió (recálculo) o se dejó de navegar
            else -> Unit
        }

    private suspend fun onPoiReached(s: TourDetour.PoiReached, session: StateFlow<NavigationSession>, prog: StateFlow<NavigationProgress?>) =
        when (race(
            { awaitCancel(); Outcome.CANCEL },
            { awaitRouteGone(s.detourRoute, session); Outcome.ENDED },
            { delay(poiPause); Outcome.RESUME }, // se continúa solo: el mismo desvío sigue hacia el destino
        )) {
            Outcome.CANCEL -> doCancel(s, prog)
            Outcome.ENDED -> endUnderfoot()
            Outcome.RESUME -> {
                _state.value = reduce(_state.value, TourDetourEvent.Resume) // -> Returning
                store.save(_state.value)
            }
            else -> Unit
        }

    private suspend fun onReturning(s: TourDetour.Returning, session: StateFlow<NavigationSession>, prog: StateFlow<NavigationProgress?>) =
        when (race(
            { awaitCancel(); Outcome.CANCEL },
            { awaitRouteGone(s.detourRoute, session); Outcome.ENDED },
        )) {
            Outcome.CANCEL -> doCancel(s, prog)
            // la ruta se acabó (llegada al destino) o cambió (recálculo): el desvío termina
            Outcome.ENDED -> { _state.value = reduce(_state.value, TourDetourEvent.Finish); store.clear() }
            else -> Unit
        }

    /** Repone la ruta original al cancelar: sin pedir si se sigue cerca; con una llamada si se está lejos. */
    private suspend fun doCancel(s: TourDetour, prog: StateFlow<NavigationProgress?>) {
        val original = s.originalRoute!!
        val pos = prog.value?.position
        when {
            pos != null && pos.distanceToRoute(original) <= rejoinMeters -> dispatch(NavigationEvent.FollowRoute(original)) // 0 llamadas
            pos != null && budget.canQuery() -> when (val r = computeRoute(pos, original.destination, emptyList())) { // 1 llamada
                is RouteResult.Success -> dispatch(NavigationEvent.FollowRoute(r.route))
                is RouteResult.Failure -> dispatch(NavigationEvent.FollowRoute(original)) // si falla, al menos la cacheada
            }
            else -> dispatch(NavigationEvent.FollowRoute(original)) // sin posición o sin presupuesto: la cacheada
        }
        _state.value = reduce(_state.value, TourDetourEvent.Cancel) // -> Inactive
        store.clear()
    }

    /** La ruta en curso cambió (recálculo) o se dejó de navegar: el desvío termina sin reponer nada. */
    private fun endUnderfoot() {
        _state.value = reduce(_state.value, TourDetourEvent.Cancel) // -> Inactive
        store.clear()
    }

    private suspend fun restoreInto(session: StateFlow<NavigationSession>) {
        val restored = store.load() ?: return
        // espera a que se reanude la navegación; la ruta del desvío es la activa restaurada por la caché de ruta
        val route = session.map { it.navigatingRoute }.filterNotNull().first()
        if (session.value.navigationProfile == NavigationProfile.TOUR && route.destination.id == restored.originalRoute.destination.id) {
            _state.value = when (restored.phase) {
                DetourPhase.TO_POI -> TourDetour.ToPoi(restored.poi, restored.originalRoute, route)
                DetourPhase.RETURNING -> TourDetour.Returning(restored.poi, restored.originalRoute, route)
            }
        } else {
            store.clear() // otro trayecto: el contexto guardado es viejo
        }
    }

    private suspend fun awaitAccept(): PoiEvaluation {
        while (true) (actions.receive() as? Action.Accept)?.let { return it.poi }
    }

    private suspend fun awaitCancel() {
        while (actions.receive() !is Action.Cancel) Unit
    }

    private fun pollCancel(): Boolean {
        var cancelled = false
        while (true) {
            val a = actions.tryReceive().getOrNull() ?: return cancelled
            if (a is Action.Cancel) cancelled = true
        }
    }

    private suspend fun awaitPoiReached(poi: PoiEvaluation, prog: StateFlow<NavigationProgress?>) {
        prog.first { p -> p?.position?.let { it.distanceTo(poi.candidate.location) <= poiArrivalMeters } == true }
    }

    /** Suspende hasta que la ruta en curso deja de ser [detourRoute] (recálculo o fin de la navegación). */
    private suspend fun awaitRouteGone(detourRoute: Route, session: StateFlow<NavigationSession>) {
        session.first { it.navigatingRoute != detourRoute }
    }

    private enum class Outcome { CANCEL, REACHED, RESUME, ENDED }

    /** El primero de [branches] gana; el resto se cancela. Como [TourSuggestionController.awaitOutcome]. */
    private suspend fun race(vararg branches: suspend () -> Outcome): Outcome = coroutineScope {
        val result = CompletableDeferred<Outcome>()
        val jobs = branches.map { branch -> launch { result.complete(branch()) } }
        val out = result.await()
        jobs.forEach { it.cancel() }
        out
    }

    private fun PoiEvaluation.toPlace() = Place(candidate.id, candidate.name, candidate.location, null, null, 0)

    /** Distancia mínima de un punto a la polilínea de [route] (para decidir si se sigue cerca al cancelar). */
    private fun GeoPoint.distanceToRoute(route: Route): Double {
        val path = MeasuredPath(route.points)
        if (path.segmentCount == 0) return route.points.firstOrNull()?.let { distanceTo(it) } ?: Double.MAX_VALUE
        return (0 until path.segmentCount).minOf { path.project(this, it).second }
    }

    private val poiArrivalMeters = thresholds.arrivalRadiusMeters + thresholds.arrivalAccuracyAllowanceMeters
    private val rejoinMeters = thresholds.offRouteMeters + thresholds.offRouteAccuracyAllowanceMeters

    companion object {
        /** Pausa breve en el POI antes de continuar solo hacia el destino (paso 20). */
        val POI_PAUSE: Duration = 4.seconds
    }
}
