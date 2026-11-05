package com.xanderjs.haptimap.feature.navigationui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.navigation.NavigationEngine
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationProgress
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.RouteTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface NavigationUiState {
    /** Ya no se navega (p. ej. detenida): no hay nada que mostrar mientras la pantalla sale. */
    data object Ended : NavigationUiState
    data class Navigating(val guidance: GuidanceUi, val notice: NavigationNotice?, val confirmingStop: Boolean = false) : NavigationUiState
    data class Arrived(val destinationName: String) : NavigationUiState

    /** Fuera de ruta o recalculando (paso 08): la guía de la ruta vieja ya no sirve y no se muestra. */
    data class OffRoute(
        val status: OffRouteStatus,
        val metersFromRoute: Int?,
        val notice: NavigationNotice?,
        val confirmingStop: Boolean = false,
    ) : NavigationUiState
}

/**
 * Pantalla de navegación (paso 07): muestra el avance del Navigation Engine. No lo arranca ni lo para:
 * el motor sigue a la sesión, así que salir de la pantalla no detiene la navegación; Detener sí (Cancel).
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val store: NavigationSessionStore,
    engine: NavigationEngine,
    activeRoute: ObserveActiveRoute,
) : ViewModel() {
    private val confirmingStop = MutableStateFlow(false)

    val uiState: StateFlow<NavigationUiState> =
        combine(store.state, activeRoute(), engine.progress, confirmingStop, ::navigationUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, NavigationUiState.Ended)

    /** La vista de ambiente (paso 12): solo emite cuando cambia lo que muestra, no con cada fix. */
    val ambientState: StateFlow<AmbientNavigationUi?> =
        uiState.map(::ambientNavigationUi).stateIn(viewModelScope, SharingStarted.Eagerly, ambientNavigationUi(uiState.value))

    fun requestStop() {
        confirmingStop.value = true
    }

    fun dismissStop() {
        confirmingStop.value = false
    }

    /** Detener: Idle, y la caché se borra al seguir la sesión (paso 06). No se puede retomar. */
    fun confirmStop() {
        confirmingStop.value = false
        store.dispatch(NavigationEvent.Cancel)
    }

    /** Reintentar tras un error de recálculo: Recalculating, y el RouteRecalculator hace la petición. */
    fun retry() = store.dispatch(NavigationEvent.Recalculate)

    /** Arrived → Completed al cerrar la llegada; la búsqueda empieza desde ahí una sesión nueva. */
    fun finish() = store.dispatch(NavigationEvent.Finish)
}

/**
 * Lo que muestra la navegación para una sesión, su conectividad y el avance del motor. Pura: la usan la pantalla
 * y la notificación del servicio en primer plano (paso 10).
 */
fun navigationUiState(
    session: NavigationSession,
    active: ActiveRoute?,
    progress: NavigationProgress?,
    confirming: Boolean,
): NavigationUiState = when (session) {
        is NavigationSession.Arrived -> NavigationUiState.Arrived(session.route.destination.name)
        is NavigationSession.Completed -> NavigationUiState.Arrived(session.route.destination.name)
        is NavigationSession.OffRoute, is NavigationSession.Recalculating -> NavigationUiState.OffRoute(
            status = offRouteStatus(
                recalculating = session is NavigationSession.Recalculating,
                offline = active?.offline == true,
                failed = (session as? NavigationSession.OffRoute)?.error != null,
            ),
            metersFromRoute = progress?.crossTrackMeters?.roundToInt(),
            notice = NavigationNotice.WEAK_GPS.takeIf { progress?.weakSignal == true },
            confirmingStop = confirming,
        )
        is NavigationSession.NavigationActive -> {
            val route = session.route
            // el motor publica el avance al momento, pero esta pantalla puede ver la sesión antes
            val current = progress ?: RouteTracker(route).progress
            NavigationUiState.Navigating(
                guidance = current.toGuidance(route.destination.name),
                notice = navigationNotice(current.weakSignal, active?.offline == true),
                confirmingStop = confirming,
            )
        }
        else -> NavigationUiState.Ended
    }
