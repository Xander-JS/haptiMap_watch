package com.xanderjs.haptimap.feature.navigationui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.location.firstPoint
import com.xanderjs.haptimap.core.model.ConnectivityPolicy
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.RouteResult
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.SessionError
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import com.xanderjs.haptimap.core.network.RoutesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** [status] null = la ruta está lista y se muestra [route]. */
data class RouteSummaryUiState(
    val status: RouteStatus? = RouteStatus.Loading,
    val route: Route? = null,
)

/**
 * Pide la ruta al destino de la sesión: DestinationSelected → RouteLoading → RouteReady, o de vuelta a
 * DestinationSelected con el error. La sesión solo guarda un SessionError; el RouteError detallado
 * es de la pantalla.
 */
@HiltViewModel
class RouteSummaryViewModel @Inject constructor(
    private val repository: RoutesRepository,
    private val locationSource: LocationSource,
    private val store: NavigationSessionStore,
    private val connectivity: ConnectivityMonitor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RouteSummaryUiState())
    val uiState: StateFlow<RouteSummaryUiState> = _uiState.asStateFlow()

    init {
        loadRoute()
    }

    /** También es "Reintentar". Fuera de DestinationSelected no pide nada: ya hay una petición o una ruta. */
    fun loadRoute() {
        val session = store.state.value
        if (session is NavigationSession.RouteReady) {
            _uiState.value = RouteSummaryUiState(status = null, route = session.route)
            return
        }
        if (session !is NavigationSession.DestinationSelected) return
        // sin clave o sin red se rechaza ya, sin esperar al GPS ni pasar por RouteLoading, que es lo que dispara la petición
        val rejected = when {
            !repository.hasApiKey -> RouteError.MISSING_KEY
            !ConnectivityPolicy.canRequestRoute(connectivity.status.value) -> RouteError.NO_CONNECTION
            else -> null
        }
        if (rejected != null) {
            store.dispatch(NavigationEvent.RouteRequestRejected(rejected.toSessionError()))
            _uiState.value = RouteSummaryUiState(RouteStatus.Failed(rejected))
            return
        }
        store.dispatch(NavigationEvent.RequestRoute)
        _uiState.value = RouteSummaryUiState(RouteStatus.Loading)
        viewModelScope.launch {
            when (val result = repository.computeRoute(locationSource.firstPoint(), session.destination)) {
                is RouteResult.Success -> {
                    store.dispatch(NavigationEvent.RouteLoaded(result.route))
                    _uiState.value = RouteSummaryUiState(status = null, route = result.route)
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
}

private fun RouteError.toSessionError() = if (this == RouteError.NO_CONNECTION) SessionError.NO_CONNECTION else SessionError.ROUTE_UNAVAILABLE
