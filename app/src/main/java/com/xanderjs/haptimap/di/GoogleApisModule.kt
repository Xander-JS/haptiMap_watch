package com.xanderjs.haptimap.di

import android.content.Context
import android.content.pm.PackageManager
import com.xanderjs.haptimap.BuildConfig
import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.model.SettingsRepository
import com.xanderjs.haptimap.core.navigation.NavigationEngine
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.network.ApiCallBudget
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import com.xanderjs.haptimap.core.network.GooglePlacesRepository
import com.xanderjs.haptimap.core.network.GooglePoiSource
import com.xanderjs.haptimap.core.network.GoogleRoutesRepository
import com.xanderjs.haptimap.core.network.PlacesRepository
import com.xanderjs.haptimap.core.network.PoiSource
import com.xanderjs.haptimap.core.network.RoutesRepository
import com.xanderjs.haptimap.core.network.createPlacesApi
import com.xanderjs.haptimap.core.network.createRoutesApi
import com.xanderjs.haptimap.core.network.isApiKeyConfigured
import com.xanderjs.haptimap.feature.navigationui.TourPoiPipeline
import com.xanderjs.haptimap.feature.navigationui.TourSuggestionController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.MessageDigest
import javax.inject.Singleton

/**
 * Places y Routes: misma clave (mismo proyecto de Google Cloud) y misma firma. El enlace vive en :app
 * para que :core:network no dependa de Hilt ni de BuildConfig. Siempre son los de Google (datos reales): sin
 * clave válida fallan con el error de clave (MISSING_KEY / API_INVALID_KEY) y la UI muestra ese estado; nunca
 * hay lugares ni rutas falsos. Para recorrer el flujo sin clave en el emulador está el simulador de ubicación.
 */
@Module
@InstallIn(SingletonComponent::class)
object GoogleApisModule {
    /** Único presupuesto de llamadas a Google de la app (paso 17): lo comparten Places, Routes y el POI source. */
    @Provides
    @Singleton
    fun apiCallBudget(): ApiCallBudget = ApiCallBudget()

    @Provides
    @Singleton
    fun placesRepository(@ApplicationContext context: Context, connectivity: ConnectivityMonitor, language: AppLanguageState, budget: ApiCallBudget): PlacesRepository =
        GooglePlacesRepository(
            api = createPlacesApi(BuildConfig.MAPS_API_KEY, context.packageName, signingCertSha1(context)),
            connectivity = connectivity,
            languageCode = { language.current },
            budget = budget,
        )

    @Provides
    @Singleton
    fun routesRepository(@ApplicationContext context: Context, connectivity: ConnectivityMonitor, language: AppLanguageState, budget: ApiCallBudget): RoutesRepository =
        GoogleRoutesRepository(
            api = createRoutesApi(BuildConfig.MAPS_API_KEY, context.packageName, signingCertSha1(context)),
            connectivity = connectivity,
            languageCode = { language.current },
            hasApiKey = isApiKeyConfigured(BuildConfig.MAPS_API_KEY),
            budget = budget,
        )

    /**
     * Fuente de POIs del Modo Tour (paso 17): siempre Google Places (New), sin datos falsos. Sin clave válida
     * fallará con el error de clave; nunca inventa lugares.
     */
    @Provides
    @Singleton
    fun poiSource(@ApplicationContext context: Context, connectivity: ConnectivityMonitor, budget: ApiCallBudget): PoiSource =
        GooglePoiSource(
            api = createPlacesApi(BuildConfig.MAPS_API_KEY, context.packageName, signingCertSha1(context)),
            connectivity = connectivity,
            budget = budget,
        )

    /** Tubería de POIs del Tour (paso 17): sesión + progreso + ajustes -> consultas -> evaluación -> sugerencias. */
    @Provides
    @Singleton
    fun tourPoiPipeline(
        poiSource: PoiSource,
        store: NavigationSessionStore,
        engine: NavigationEngine,
        connectivity: ConnectivityMonitor,
        settings: SettingsRepository,
        budget: ApiCallBudget,
        language: AppLanguageState,
    ): TourPoiPipeline = TourPoiPipeline(
        poiSource = poiSource,
        sessions = store.state,
        progress = engine.progress,
        connectivity = connectivity.status,
        settings = settings.settings,
        budget = budget,
        languageCode = { language.current },
        debug = BuildConfig.DEBUG,
    )

    /**
     * Controlador de sugerencias del Tour (paso 19): consume el flujo del paso 17 (sin pedir nada nuevo a Google),
     * muestra una a la vez, avisa con el háptico y la rechaza al cerrarse. El tiempo de espera sale de ajustes.
     */
    @Provides
    @Singleton
    fun tourSuggestionController(
        pipeline: TourPoiPipeline,
        store: NavigationSessionStore,
        engine: NavigationEngine,
        settings: SettingsRepository,
        haptics: HapticEngine,
    ): TourSuggestionController = TourSuggestionController(
        suggestions = pipeline.suggestions,
        sessions = store.state,
        progress = engine.progress,
        settings = settings.settings,
        reject = pipeline::reject,
        playHaptic = haptics::play,
    )

    /** SHA-1 del certificado de firma en hex mayúsculas, como lo espera X-Android-Cert. Vale para debug y release. */
    private fun signingCertSha1(context: Context): String {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val cert = info.signingInfo!!.apkContentsSigners.first().toByteArray()
        return MessageDigest.getInstance("SHA-1").digest(cert).joinToString("") { "%02X".format(it) }
    }
}
