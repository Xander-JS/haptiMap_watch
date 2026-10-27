package com.xanderjs.haptimap.presentation.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.model.ConnectivityPolicy
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.Route
import com.xanderjs.haptimap.core.navigation.SessionError
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SessionDebugUiState(val session: NavigationSession, val connectivity: ConnectivityStatus)

/** El estado vive en el store singleton: salir de la pantalla no lo pierde. */
@HiltViewModel
class SessionDebugViewModel @Inject constructor(
    private val store: NavigationSessionStore,
    private val connectivity: ConnectivityMonitor,
) : ViewModel() {
    /** Sesión y conectividad son estados independientes; solo se combinan aquí para mostrarlos. */
    val uiState: StateFlow<SessionDebugUiState> = combine(store.state, connectivity.status, ::SessionDebugUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SessionDebugUiState(store.state.value, connectivity.status.value))

    /**
     * Todos los eventos, válidos o no en [session], para comprobar que los inválidos no cambian nada.
     * El destino se elige en Places debug; los eventos que llevan una ruta usan el destino de la sesión
     * y no aparecen mientras no haya uno.
     */
    fun eventsFor(session: NavigationSession): List<NavigationEvent> {
        val route = session.destination?.let(::Route)
        return listOfNotNull(
            NavigationEvent.StartSearch,
            NavigationEvent.RequestRoute,
            NavigationEvent.RouteRequestRejected(SessionError.NO_CONNECTION),
            route?.let(NavigationEvent::RouteLoaded),
            NavigationEvent.RouteFailed(SessionError.ROUTE_UNAVAILABLE),
            NavigationEvent.LeaveRoute,
            NavigationEvent.ReturnToRoute,
            NavigationEvent.Recalculate,
            route?.let(NavigationEvent::RouteRecalculated),
            NavigationEvent.RecalculationFailed(SessionError.NO_CONNECTION),
            NavigationEvent.Arrive,
            NavigationEvent.Finish,
            NavigationEvent.Cancel,
        )
    }

    fun onEvent(event: NavigationEvent) {
        // sin conexión la ruta se rechaza antes de RouteLoading, que en el paso 05 lanzará la petición de red
        if (event == NavigationEvent.RequestRoute && !ConnectivityPolicy.canRequestRoute(connectivity.status.value)) {
            store.dispatch(NavigationEvent.RouteRequestRejected(SessionError.NO_CONNECTION))
        } else {
            store.dispatch(event)
        }
    }
}

/** El destino elegido, en cualquier estado que ya lo tenga. */
val NavigationSession.destination: Place?
    get() = when (this) {
        NavigationSession.Idle, NavigationSession.Searching -> null
        is NavigationSession.DestinationSelected -> destination
        is NavigationSession.RouteLoading -> destination
        is NavigationSession.NavigationActive -> route.destination
        is NavigationSession.OffRoute -> route.destination
        is NavigationSession.Recalculating -> route.destination
        is NavigationSession.Arrived -> route.destination
        is NavigationSession.Completed -> route.destination
    }
