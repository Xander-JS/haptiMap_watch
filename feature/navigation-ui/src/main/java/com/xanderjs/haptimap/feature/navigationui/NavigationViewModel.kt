package com.xanderjs.haptimap.feature.navigationui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.model.DisplayMode
import com.xanderjs.haptimap.core.model.HapticPattern
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.SettingsRepository
import com.xanderjs.haptimap.core.navigation.NavigationEngine
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationProgress
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.RouteTracker
import com.xanderjs.haptimap.designsystem.atoms.HapticPulse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface NavigationUiState {
    /** Ya no se navega (p. ej. detenida): no hay nada que mostrar mientras la pantalla sale. */
    data object Ended : NavigationUiState
    /** [tour]: el trayecto es del Modo Tour; la maniobra va en su color (rediseño Pulso). */
    data class Navigating(
        val guidance: GuidanceUi,
        val notice: NavigationNotice?,
        val confirmingStop: Boolean = false,
        val tour: Boolean = false,
    ) : NavigationUiState
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
    settings: SettingsRepository,
    haptics: HapticEngine,
) : ViewModel() {
    private val confirmingStop = MutableStateFlow(false)

    val uiState: StateFlow<NavigationUiState> =
        combine(store.state, activeRoute(), engine.progress, confirmingStop, ::navigationUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, NavigationUiState.Ended)

    /** El modo de pantalla elegido en ajustes (paso 25). Reactivo: cambiarlo conmuta Eco ↔ mapa sin reiniciar el trayecto (paso 22). */
    val displayMode: StateFlow<DisplayMode> =
        settings.settings.map { it.displayMode }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplayMode.ECO_AOD)

    /**
     * El último aviso háptico real, para que la maniobra lata a su ritmo (rediseño Pulso). Se sella con la hora al
     * sonar, no al mirarse: al volver a la pantalla, un aviso ya pasado no se repite. Sin el valor que ya había al abrir.
     */
    val pulse: StateFlow<HapticPulse?> =
        haptics.played.drop(1).filterNotNull()
            .map { HapticPulse(it.pattern.pulseStartsMs(), SystemClock.uptimeMillis()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
                tour = session.profile == NavigationProfile.TOUR,
            )
        }
        else -> NavigationUiState.Ended
    }

/** Cuándo empieza cada pulso desde el inicio del patrón (los tramos con amplitud seguidos son un pulso). */
fun HapticPattern.pulseStartsMs(): List<Long> {
    var elapsed = 0L
    return onOffTimings().withIndex().mapNotNull { (i, duration) ->
        // índice impar = encendido (onOffTimings empieza por un apagado)
        elapsed.takeIf { i % 2 == 1 }.also { elapsed += duration }
    }
}
