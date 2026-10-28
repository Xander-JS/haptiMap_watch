package com.xanderjs.haptimap.di

import android.content.Context
import com.xanderjs.haptimap.core.location.FusedLocationSource
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.location.simulatedGpsStatus
import com.xanderjs.haptimap.core.location.withDetour
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.navigatingRoute
import com.xanderjs.haptimap.presentation.debug.WalkSimulationSetting
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.math.ceil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

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
 * fuera de ruta y recalculando (paso 08); el desvío del modo solo en la primera ruta, no en la recalculada.
 */
private class SimulatableLocationSource(
    private val real: LocationSource,
    private val store: NavigationSessionStore,
    private val setting: WalkSimulationSetting,
) : LocationSource {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun gpsStatus(intervalMillis: Long): Flow<GpsStatus> {
        var previous: Route? = null
        return combine(setting.mode, store.state.map { it.navigatingRoute }, ::Pair)
            .distinctUntilChanged()
            .flatMapLatest { (mode, route) ->
                val rerouted = route != null && previous != null && route != previous
                previous = route
                val walk = mode.walk?.copy(intervalMillis = intervalMillis)
                val detour = mode.detour.takeUnless { rerouted }
                when {
                    walk == null || route == null -> real.gpsStatus(intervalMillis)
                    detour == null -> simulatedGpsStatus(route.points, walk)
                    else -> {
                        // sin red desde el fix en que sale de la ruta, durante offlineSeconds
                        val leaveAt = ceil(detour.atMeters / (walk.speedMetersPerSecond * intervalMillis / 1000)).toInt()
                        val offline = leaveAt until leaveAt + (mode.offlineSeconds * 1000 / intervalMillis).toInt()
                        var index = -1
                        simulatedGpsStatus(route.points.withDetour(detour), walk)
                            .onEach { if (it != GpsStatus.Searching) setting.offline.value = ++index in offline }
                            .onCompletion { setting.offline.value = false }
                    }
                }
            }
    }
}
