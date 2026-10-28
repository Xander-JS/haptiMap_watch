package com.xanderjs.haptimap.di

import android.content.Context
import com.xanderjs.haptimap.core.navigation.RouteCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** La ruta activa (paso 06) en noBackupFilesDir: es de este reloj y de este momento, no va a la copia en la nube. */
@Module
@InstallIn(SingletonComponent::class)
object RouteCacheModule {
    @Provides
    @Singleton
    fun routeCache(@ApplicationContext context: Context) = RouteCache(File(context.noBackupFilesDir, "active_route.json"))

    /** Donde se lee la caché: fuera del hilo principal. Los tests pasan un dispatcher de test. */
    @Provides
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
