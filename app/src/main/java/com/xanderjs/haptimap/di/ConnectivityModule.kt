package com.xanderjs.haptimap.di

import android.content.Context
import com.xanderjs.haptimap.core.network.AndroidConnectivityMonitor
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Singleton con scope de aplicación: un solo NetworkCallback compartido por todas las pantallas. */
@Module
@InstallIn(SingletonComponent::class)
object ConnectivityModule {
    @Provides
    @Singleton
    fun connectivityMonitor(@ApplicationContext context: Context): ConnectivityMonitor =
        AndroidConnectivityMonitor(context, CoroutineScope(SupervisorJob() + Dispatchers.Default))
}
