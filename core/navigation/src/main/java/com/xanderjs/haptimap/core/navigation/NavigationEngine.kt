package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.LocationFix
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
 * Navigation Engine (paso 07): mientras la sesión está en NavigationActive pasa cada fix por un
 * [RouteTracker] de la ruta activa, publica el avance y los avisos y lleva la sesión a Arrived.
 * Vive fuera de las pantallas, como el store: el servicio del paso 10 llamará a [follow] en lugar de la app.
 */
@Singleton
class NavigationEngine @Inject constructor(private val store: NavigationSessionStore) {
    private val _progress = MutableStateFlow<NavigationProgress?>(null)

    /** El avance en la ruta activa; null fuera de NavigationActive. */
    val progress: StateFlow<NavigationProgress?> = _progress.asStateFlow()

    // tryEmit con búfer: un aviso nunca se pierde por cancelar la espera del fix siguiente
    private val _cues = MutableSharedFlow<NavigationCue>(extraBufferCapacity = CUE_BUFFER)

    /** Avisos para el Haptic Engine (paso 09). Sin suscriptores se descartan: un aviso viejo ya no sirve. */
    val cues: SharedFlow<NavigationCue> = _cues.asSharedFlow()

    /**
     * Sigue la sesión hasta que se cancele quien llama. [fixes] es frío y solo se recolecta en
     * NavigationActive: fuera de ella el GPS no se usa. Una ruta nueva (RouteRecalculated) empieza un tracker nuevo.
     */
    suspend fun follow(fixes: Flow<LocationFix>, thresholds: NavigationThresholds = NavigationThresholds()) {
        store.state.map { (it as? NavigationSession.NavigationActive)?.route }.distinctUntilChanged().collectLatest { route ->
            _progress.value = null
            if (route == null) return@collectLatest
            val tracker = RouteTracker(route, thresholds)
            _progress.value = tracker.progress
            // null al empezar: el silencio cuenta desde el inicio aunque no llegue ningún fix
            fixes.map<LocationFix, LocationFix?> { it }.onStart { emit(null) }.collectLatest { fix ->
                if (fix != null) {
                    val update = tracker.update(fix)
                    _progress.value = update.progress
                    update.cues.forEach(_cues::tryEmit)
                    if (NavigationCue.Arrived in update.cues) store.dispatch(NavigationEvent.Arrive)
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
