package com.xanderjs.haptimap.presentation.debug

import androidx.lifecycle.ViewModel
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.navigation.Destination
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.Route
import com.xanderjs.haptimap.core.navigation.SessionError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** El estado vive en el store singleton: salir de la pantalla no lo pierde. */
@HiltViewModel
class SessionDebugViewModel @Inject constructor(private val store: NavigationSessionStore) : ViewModel() {
    val uiState: StateFlow<NavigationSession> = store.state

    /** Todos los eventos, válidos o no en el estado actual, para comprobar que los inválidos no cambian nada. */
    val events: List<NavigationEvent> = listOf(
        NavigationEvent.StartSearch,
        NavigationEvent.SelectDestination(sampleDestination),
        NavigationEvent.RequestRoute,
        NavigationEvent.RouteLoaded(Route(sampleDestination)),
        NavigationEvent.RouteFailed(SessionError.ROUTE_UNAVAILABLE),
        NavigationEvent.LeaveRoute,
        NavigationEvent.ReturnToRoute,
        NavigationEvent.Recalculate,
        NavigationEvent.RouteRecalculated(Route(sampleDestination)),
        NavigationEvent.RecalculationFailed(SessionError.NO_CONNECTION),
        NavigationEvent.Arrive,
        NavigationEvent.Finish,
        NavigationEvent.Cancel,
    )

    fun onEvent(event: NavigationEvent) = store.dispatch(event)

    private companion object {
        // shortcut: destino fijo de depuración, hasta que el paso 04 traiga lugares reales
        val sampleDestination = Destination(name = "Puerta del Sol", point = GeoPoint(40.416775, -3.703790))
    }
}
