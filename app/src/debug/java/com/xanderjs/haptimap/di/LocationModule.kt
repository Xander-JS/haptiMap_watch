package com.xanderjs.haptimap.di

import android.content.Context
import com.xanderjs.haptimap.core.location.FusedLocationSource
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.location.simulatedGpsStatus
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.presentation.debug.WalkSimulationSetting
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

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
 * Con un modo de simulación elegido en el catálogo y navegando, recorre la ruta activa en lugar de usar
 * el GPS; si no, el GPS real (la búsqueda y la petición de ruta siguen usando la posición real).
 */
private class SimulatableLocationSource(
    private val real: LocationSource,
    private val store: NavigationSessionStore,
    private val setting: WalkSimulationSetting,
) : LocationSource {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun gpsStatus(intervalMillis: Long): Flow<GpsStatus> =
        combine(setting.mode, store.state.map { (it as? NavigationSession.NavigationActive)?.route }, ::Pair)
            .distinctUntilChanged()
            .flatMapLatest { (mode, route) ->
                val walk = mode.walk
                if (walk != null && route != null) simulatedGpsStatus(route.points, walk.copy(intervalMillis = intervalMillis))
                else real.gpsStatus(intervalMillis)
            }
}
