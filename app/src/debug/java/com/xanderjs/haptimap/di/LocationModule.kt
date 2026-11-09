package com.xanderjs.haptimap.di

import android.content.Context
import com.xanderjs.haptimap.core.location.FusedLocationSource
import com.xanderjs.haptimap.core.location.LocationSampling
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.location.simulatedGpsStatus
import com.xanderjs.haptimap.core.location.withDetour
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.navigatingRoute
import com.xanderjs.haptimap.presentation.debug.WalkSimulationSetting
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.logging.Logger
import javax.inject.Singleton
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn

/** El enlace vive en :app para que :core:location no dependa de Hilt. En debug, con el simulador del paso 07. */
@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides
    @Singleton
    fun locationSource(
        @ApplicationContext context: Context,
        store: NavigationSessionStore,
        setting: WalkSimulationSetting,
    ): LocationSource = SimulatableLocationSource(FusedLocationSource(context), store, setting)
}

/**
 * Con un modo de simulación elegido en el catálogo y navegando, recorre la ruta en lugar de usar el GPS;
 * si no, el GPS real (la búsqueda y la petición de ruta siguen usando la posición real). Sigue andando
 * fuera de ruta y recalculando (paso 08); el desvío del modo solo en la ruta con la que se empezó, no en la recalculada.
 */
private class SimulatableLocationSource(
    private val real: LocationSource,
    private val store: NavigationSessionStore,
    private val setting: WalkSimulationSetting,
) : LocationSource {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // la ruta con la que empezó la sesión (la de RouteReady); una recalculada no pasa por ahí. No vale
    // recordarla dentro de gpsStatus(): el motor vuelve a recolectar los fixes con cada ruta nueva
    private val startedWith = store.state.filterIsInstance<NavigationSession.RouteReady>().map { it.route }
        .stateIn(scope, SharingStarted.Eagerly, null)

    // El paseo simulado va al ritmo fijo del modo y se COMPARTE: el muestreo adaptativo (paso 23) reengancha
    // gpsStatus con cada cambio de ritmo, y sin compartir eso reiniciaría el paseo desde el principio. Compartido
    // (Lazily + replay 1) el paseo sigue y el nuevo suscriptor retoma donde iba. emptyFlow cuando no se simula: lo
    // cubre el GPS real en gpsStatus().
    @OptIn(ExperimentalCoroutinesApi::class)
    private val simulated: Flow<GpsStatus> =
        combine(setting.mode, store.state.map { it.navigatingRoute }, ::Pair)
            .distinctUntilChanged()
            .flatMapLatest { (mode, route) ->
                val walk = mode.walk
                val detour = mode.detour.takeIf { route != null && route == startedWith.value }
                if (walk != null && route != null) logger.info("Simulating $mode to ${route.destination.name}, detour ${detour != null}")
                when {
                    walk == null || route == null -> emptyFlow()
                    detour == null -> simulatedGpsStatus(route.points, walk)
                    else -> {
                        // sin red desde el fix en que sale de la ruta, durante offlineSeconds
                        val leaveAt = ceil(detour.atMeters / (walk.speedMetersPerSecond * walk.intervalMillis / 1000)).toInt()
                        val offline = leaveAt until leaveAt + (mode.offlineSeconds * 1000 / walk.intervalMillis).toInt()
                        var index = -1
                        simulatedGpsStatus(route.points.withDetour(detour), walk)
                            .onEach { if (it != GpsStatus.Searching) setting.offline.value = ++index in offline }
                            .onCompletion { setting.offline.value = false }
                    }
                }
            }
            .shareIn(scope, SharingStarted.Lazily, replay = 1)

    override fun gpsStatus(sampling: LocationSampling): Flow<GpsStatus> =
        // simular a ritmo fijo (flujo compartido) o GPS real adaptativo; se reevalúa en cada reenganche de gpsStatus
        if (setting.mode.value.walk != null && store.state.value.navigatingRoute != null) simulated
        else real.gpsStatus(sampling)

    private companion object {
        val logger: Logger = Logger.getLogger("HaptiLocation")
    }
}
