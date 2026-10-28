package com.xanderjs.haptimap.di

import android.content.Context
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.network.AndroidConnectivityMonitor
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import com.xanderjs.haptimap.presentation.debug.WalkSimulationSetting
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Singleton con scope de aplicación: un solo NetworkCallback compartido por todas las pantallas. En debug,
 * el modo "Salir sin red" del simulador (paso 08) fuerza Disconnected mientras dura.
 */
@Module
@InstallIn(SingletonComponent::class)
object ConnectivityModule {
    @Provides
    @Singleton
    fun connectivityMonitor(@ApplicationContext context: Context, setting: WalkSimulationSetting): ConnectivityMonitor {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val real = AndroidConnectivityMonitor(context, scope)
        return object : ConnectivityMonitor {
            // mismo reparto que el real: el NetworkCallback se suelta 5 s después del último suscriptor
            override val status: StateFlow<ConnectivityStatus> =
                combine(real.status, setting.offline) { status, offline -> if (offline) ConnectivityStatus.Disconnected else status }
                    .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), real.status.value)
        }
    }

    private const val STOP_TIMEOUT_MILLIS = 5_000L
}
