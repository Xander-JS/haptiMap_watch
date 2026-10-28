package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.core.model.Route
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Navigation Engine (paso 07): mientras se navega (NavigationActive, OffRoute, Recalculating) pasa cada fix
 * por un [RouteTracker] de la ruta, publica el avance y los avisos y mueve la sesión: Arrived, y LeaveRoute /
 * ReturnToRoute al salirse y volver (paso 08).
 * Vive fuera de las pantallas, como el store: el servicio del paso 10 llamará a [follow] en lugar de la app.
 */
@Singleton
class NavigationEngine @Inject constructor(private val store: NavigationSessionStore) {
    private val _progress = MutableStateFlow<NavigationProgress?>(null)

    /** El avance en la ruta que se navega; null si no se navega. */
    val progress: StateFlow<NavigationProgress?> = _progress.asStateFlow()

    // tryEmit con búfer: un aviso nunca se pierde por cancelar la espera del fix siguiente
    private val _cues = MutableSharedFlow<NavigationCue>(extraBufferCapacity = CUE_BUFFER)

    /** Avisos para el Haptic Engine (paso 09). Sin suscriptores se descartan: un aviso viejo ya no sirve. */
    val cues: SharedFlow<NavigationCue> = _cues.asSharedFlow()

    /**
     * Sigue la sesión hasta que se cancele quien llama. [fixes] es frío y solo se recolecta mientras se
     * navega: fuera, el GPS no se usa. Una ruta nueva (RouteRecalculated) empieza un tracker nuevo y avisa Rerouted.
     */
    suspend fun follow(fixes: Flow<LocationFix>, thresholds: NavigationThresholds = NavigationThresholds()) {
        var previous: Route? = null
        store.state.map { it.navigatingRoute }.distinctUntilChanged().collectLatest { route ->
            _progress.value = null
            // de una ruta a otra sin dejar de navegar solo se pasa con RouteRecalculated
            if (route != null && previous != null) _cues.tryEmit(NavigationCue.Rerouted)
            previous = route
            if (route == null) return@collectLatest
            val tracker = RouteTracker(route, thresholds)
            _progress.value = tracker.progress
            // null al empezar: el silencio cuenta desde el inicio aunque no llegue ningún fix
            fixes.map<LocationFix, LocationFix?> { it }.onStart { emit(null) }.collectLatest { fix ->
                if (fix != null) {
                    val update = tracker.update(fix)
                    _progress.value = update.progress
                    update.cues.forEach(_cues::tryEmit)
                    update.cues.forEach {
                        when (it) {
                            NavigationCue.Arrived -> store.dispatch(NavigationEvent.Arrive)
                            NavigationCue.OffRoute -> store.dispatch(NavigationEvent.LeaveRoute)
                            NavigationCue.BackOnRoute -> store.dispatch(NavigationEvent.ReturnToRoute)
                            else -> Unit
                        }
                    }
                }
                delay(thresholds.weakSignalAfter)
                _progress.value = tracker.signalLost()
            }
        }
    }

    private companion object {
        const val CUE_BUFFER = 16
    }
}
