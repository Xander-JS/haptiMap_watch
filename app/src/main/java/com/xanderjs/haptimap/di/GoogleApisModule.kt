package com.xanderjs.haptimap.di

import android.content.Context
import android.content.pm.PackageManager
import com.xanderjs.haptimap.BuildConfig
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import com.xanderjs.haptimap.core.network.GooglePlacesRepository
import com.xanderjs.haptimap.core.network.GoogleRoutesRepository
import com.xanderjs.haptimap.core.network.PlacesRepository
import com.xanderjs.haptimap.core.network.RoutesRepository
import com.xanderjs.haptimap.core.network.createPlacesApi
import com.xanderjs.haptimap.core.network.createRoutesApi
import com.xanderjs.haptimap.core.network.isApiKeyConfigured
import com.xanderjs.haptimap.feature.search.label
import com.xanderjs.haptimap.presentation.debug.debugPlaces
import com.xanderjs.haptimap.presentation.debug.debugRoutes
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.MessageDigest
import javax.inject.Singleton

/**
 * Places y Routes: misma clave (mismo proyecto de Google Cloud) y misma firma. El enlace vive en :app
 * para que :core:network no dependa de Hilt ni de BuildConfig. Sin clave, en debug, lugares y rutas falsos
 * (debugPlaces, debugRoutes); en release, los de Google, que fallan con el error de clave.
 */
@Module
@InstallIn(SingletonComponent::class)
object GoogleApisModule {
    @Provides
    @Singleton
    fun placesRepository(@ApplicationContext context: Context, connectivity: ConnectivityMonitor, language: AppLanguageState): PlacesRepository = debugPlaces(
        GooglePlacesRepository(
            api = createPlacesApi(BuildConfig.MAPS_API_KEY, context.packageName, signingCertSha1(context)),
            connectivity = connectivity,
            languageCode = { language.current },
        ),
        hasApiKey = isApiKeyConfigured(BuildConfig.MAPS_API_KEY),
    ) { context.getString(it.label) }

    @Provides
    @Singleton
    fun routesRepository(@ApplicationContext context: Context, connectivity: ConnectivityMonitor, language: AppLanguageState): RoutesRepository = debugRoutes(
        GoogleRoutesRepository(
            api = createRoutesApi(BuildConfig.MAPS_API_KEY, context.packageName, signingCertSha1(context)),
            connectivity = connectivity,
            languageCode = { language.current },
            hasApiKey = isApiKeyConfigured(BuildConfig.MAPS_API_KEY),
        ),
    ) { language.current }

    /** SHA-1 del certificado de firma en hex mayúsculas, como lo espera X-Android-Cert. Vale para debug y release. */
    private fun signingCertSha1(context: Context): String {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val cert = info.signingInfo!!.apkContentsSigners.first().toByteArray()
        return MessageDigest.getInstance("SHA-1").digest(cert).joinToString("") { "%02X".format(it) }
    }
}
