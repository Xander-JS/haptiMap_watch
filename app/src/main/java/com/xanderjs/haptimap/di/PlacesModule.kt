package com.xanderjs.haptimap.di

import android.content.Context
import android.content.pm.PackageManager
import com.xanderjs.haptimap.BuildConfig
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import com.xanderjs.haptimap.core.network.GooglePlacesRepository
import com.xanderjs.haptimap.core.network.PlacesRepository
import com.xanderjs.haptimap.core.network.createPlacesApi
import com.xanderjs.haptimap.designsystem.theme.appLanguage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.MessageDigest
import javax.inject.Singleton

/** El enlace vive en :app para que :core:network no dependa de Hilt ni de BuildConfig. */
@Module
@InstallIn(SingletonComponent::class)
object PlacesModule {
    @Provides
    @Singleton
    fun placesRepository(@ApplicationContext context: Context, connectivity: ConnectivityMonitor): PlacesRepository =
        GooglePlacesRepository(
            api = createPlacesApi(BuildConfig.MAPS_API_KEY, context.packageName, signingCertSha1(context)),
            connectivity = connectivity,
            languageCode = { context.appLanguage() },
        )

    /** SHA-1 del certificado de firma en hex mayúsculas, como lo espera X-Android-Cert. Vale para debug y release. */
    private fun signingCertSha1(context: Context): String {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val cert = info.signingInfo!!.apkContentsSigners.first().toByteArray()
        return MessageDigest.getInstance("SHA-1").digest(cert).joinToString("") { "%02X".format(it) }
    }
}
