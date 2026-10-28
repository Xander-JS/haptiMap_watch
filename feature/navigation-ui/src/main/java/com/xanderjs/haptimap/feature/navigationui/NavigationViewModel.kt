package com.xanderjs.haptimap.feature.navigationui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.navigation.NavigationEngine
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.RouteTracker
import com.xanderjs.haptimap.core.navigation.activeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface NavigationUiState {
    /** Ya no se navega (p. ej. detenida): no hay nada que mostrar mientras la pantalla sale. */
    data object Ended : NavigationUiState
    data class Navigating(val guidance: GuidanceUi, val notice: NavigationNotice?, val confirmingStop: Boolean = false) : NavigationUiState
    data class Arrived(val destinationName: String) : NavigationUiState
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
        combine(store.state, activeRoute(), engine.progress, confirmingStop) { session, active, progress, confirming ->
            when (session) {
                is NavigationSession.Arrived -> NavigationUiState.Arrived(session.route.destination.name)
                is NavigationSession.Completed -> NavigationUiState.Arrived(session.route.destination.name)
                // shortcut: OffRoute y Recalculating se muestran como navegando hasta que el paso 08 les dé pantalla
                is NavigationSession.NavigationActive, is NavigationSession.OffRoute, is NavigationSession.Recalculating -> {
                    val route = session.activeRoute!!
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
        }.stateIn(viewModelScope, SharingStarted.Eagerly, NavigationUiState.Ended)

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

    /** Arrived → Completed al cerrar la llegada; la búsqueda empieza desde ahí una sesión nueva. */
    fun finish() = store.dispatch(NavigationEvent.Finish)
}
