package com.xanderjs.haptimap.feature.navigationui

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.intelligence.RouteEvaluator
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.location.firstPoint
import com.xanderjs.haptimap.core.model.ConnectivityPolicy
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.RouteResult
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.SessionError
import com.xanderjs.haptimap.core.navigation.navigationProfile
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import com.xanderjs.haptimap.core.network.RoutesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.logging.Logger
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [status] null = la ruta está lista y se muestra [route]. [offline]: la ruta lista sigue sin red. */
data class RouteSummaryUiState(
    val status: RouteStatus? = RouteStatus.Loading,
    val route: Route? = null,
    val offline: Boolean = false,
)

/**
 * Pide la ruta inicial al destino de la sesión: DestinationSelected → RouteLoading → RouteReady, o de vuelta a
 * DestinationSelected con el error. En el paso 21 solicita rutas alternativas en la misma llamada inicial
 * (`alternatives = true`) y delega en [RouteEvaluator] la elección según el perfil activo ([NavigationProfile]).
 * Los recálculos y desvíos siguen pidiendo una sola ruta sin alternativas.
 */
@HiltViewModel
class RouteSummaryViewModel(
    private val repository: RoutesRepository,
    private val locationSource: LocationSource,
    private val store: NavigationSessionStore,
    private val connectivity: ConnectivityMonitor,
    activeRoute: ObserveActiveRoute,
    private val evaluator: RouteEvaluator = RouteEvaluator(),
    private val debug: Boolean = false,
) : ViewModel() {

    @Inject
    constructor(
        repository: RoutesRepository,
        locationSource: LocationSource,
        store: NavigationSessionStore,
        connectivity: ConnectivityMonitor,
        activeRoute: ObserveActiveRoute,
        @ApplicationContext context: Context,
    ) : this(
        repository = repository,
        locationSource = locationSource,
        store = store,
        connectivity = connectivity,
        activeRoute = activeRoute,
        evaluator = RouteEvaluator(),
        debug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
    )

    private val _uiState = MutableStateFlow(RouteSummaryUiState())
    val uiState: StateFlow<RouteSummaryUiState> = combine(_uiState, activeRoute()) { state, active ->
        state.copy(offline = active?.offline == true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value)

    init {
        loadRoute()
    }

    /**
     * También es "Reintentar". Fuera de DestinationSelected no pide nada: ya hay una petición o una ruta.
     * Sin red falla al momento, también al reintentar.
     */
    fun loadRoute() {
        val session = store.state.value
        if (session is NavigationSession.RouteReady) {
            _uiState.value = RouteSummaryUiState(status = null, route = session.route)
            return
        }
        // RouteLoading: recién llegado tras elegir el perfil (paso 16). DestinationSelected: "Reintentar" tras un fallo.
        val destination = when (session) {
            is NavigationSession.RouteLoading -> session.destination
            is NavigationSession.DestinationSelected -> session.destination
            else -> return
        }
        val profile = session.navigationProfile ?: NavigationProfile.FOCUS
        // sin clave o sin red se rechaza ya, sin esperar al GPS
        val rejected = when {
            !repository.hasApiKey -> RouteError.MISSING_KEY
            !ConnectivityPolicy.canRequestRoute(connectivity.status.value) -> RouteError.NO_CONNECTION
            else -> null
        }
        if (rejected != null) {
            // desde RouteLoading el rechazo baja a DestinationSelected con el error (conserva el perfil); desde
            // DestinationSelected se queda ahí. En ambos casos "Reintentar" vuelve a pedirla.
            store.dispatch(
                if (session is NavigationSession.RouteLoading) NavigationEvent.RouteFailed(rejected.toSessionError())
                else NavigationEvent.RouteRequestRejected(rejected.toSessionError()),
            )
            _uiState.value = RouteSummaryUiState(RouteStatus.Failed(rejected))
            return
        }
        // RouteLoading ya está pidiendo; desde DestinationSelected hay que dispararlo
        if (session is NavigationSession.DestinationSelected) store.dispatch(NavigationEvent.RequestRoute)
        _uiState.value = RouteSummaryUiState(RouteStatus.Loading)
        viewModelScope.launch {
            when (val result = repository.computeRoute(locationSource.firstPoint(), destination, alternatives = true)) {
                is RouteResult.Success -> {
                    val candidates = result.alternatives.ifEmpty { listOf(result.route) }
                    val selection = evaluator.select(candidates, profile)
                    if (debug) {
                        logger.info(
                            "Selected route #${selection.chosen.index} of ${selection.evaluations.size} for $profile " +
                                "(${selection.reason}, score=${"%.2f".format(selection.chosen.score)})",
                        )
                        selection.evaluations.forEach { eval ->
                            val verdict = eval.discardReason?.name ?: "score=${"%.2f".format(eval.score)}"
                            logger.info(
                                "  #${eval.index}: ${eval.route.distanceMeters}m, ${eval.route.duration}, " +
                                    "turns=${eval.turnCount} (sharp=${eval.sharpTurnCount}, short=${eval.shortStepCount}), " +
                                    "complexity=${"%.2f".format(eval.complexity)} -> $verdict",
                            )
                        }
                    }
                    val chosen = selection.chosen.route
                    store.dispatch(NavigationEvent.RouteLoaded(chosen))
                    _uiState.value = RouteSummaryUiState(status = null, route = chosen)
                }
                is RouteResult.Failure -> {
                    store.dispatch(NavigationEvent.RouteFailed(result.error.toSessionError()))
                    _uiState.value = RouteSummaryUiState(RouteStatus.Failed(result.error))
                }
            }
        }
    }

    /** "Iniciar": la sesión pasa a NavigationActive. Seguir la ruta es del paso 07. */
    fun start() = store.dispatch(NavigationEvent.StartNavigation)

    private companion object {
        val logger: Logger = Logger.getLogger("HaptiRouteOpt")
    }
}

private fun RouteError.toSessionError() = if (this == RouteError.NO_CONNECTION) SessionError.NO_CONNECTION else SessionError.ROUTE_UNAVAILABLE
