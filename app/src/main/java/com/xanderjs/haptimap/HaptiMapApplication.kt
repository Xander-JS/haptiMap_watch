package com.xanderjs.haptimap

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Application
import android.content.pm.PackageManager.PERMISSION_GRANTED
import com.xanderjs.haptimap.core.haptics.HapticCues
import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.haptics.amplitudeScale
import com.xanderjs.haptimap.core.haptics.hapticsActive
import com.xanderjs.haptimap.core.model.SettingsRepository
import com.xanderjs.haptimap.core.navigation.NavigationEngine
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.RouteCache
import com.xanderjs.haptimap.core.navigation.navigationServiceRuns
import com.xanderjs.haptimap.feature.navigationui.TourDetourController
import com.xanderjs.haptimap.feature.navigationui.TourPoiPipeline
import com.xanderjs.haptimap.feature.navigationui.TourSuggestionController
import com.xanderjs.haptimap.service.NavigationService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

@HiltAndroidApp
class HaptiMapApplication : Application() {
    @Inject lateinit var store: NavigationSessionStore
    @Inject lateinit var routeCache: RouteCache
    @Inject lateinit var engine: NavigationEngine
    @Inject lateinit var haptics: HapticEngine
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var tourPoi: TourPoiPipeline
    @Inject lateinit var tourSuggestions: TourSuggestionController
    @Inject lateinit var tourDetour: TourDetourController

    override fun onCreate() {
        super.onCreate()
        // la caché sigue a la sesión mientras viva el proceso, con o sin pantallas
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        appScope.launch { routeCache.follow(store.state) }
        // la intensidad háptica de ajustes (paso 25) alimenta el motor, única fuente; a nivel de proceso para que
        // valga también en el servicio de navegación, no solo con la app visible
        appScope.launch {
            settings.settings.map { it.hapticIntensity.amplitudeScale }.distinctUntilChanged().collect { haptics.intensity = it }
        }
        // los avisos de la ruta, en vibración (paso 09); aquí y no en el servicio: el banco de pruebas los usa sin navegar
        val cues = HapticCues(haptics::playCount)
        appScope.launch { haptics.follow(engine.cues.mapNotNull(cues::eventFor), store.state.map { it.hapticsActive }) }
        // POIs del Modo Tour (paso 17): consulta y evalúa por delante en la ruta mientras se navega en Tour
        appScope.launch { tourPoi.run() }
        // Sugerencias del Tour (paso 19): muestra una a la vez, avisa en vibración y la rechaza al cerrarse.
        // A nivel de proceso como el háptico: sigue funcionando con la app en segundo plano y la pantalla apagada
        appScope.launch { tourSuggestions.run() }
        // Desvío a un POI del Tour (paso 20): al aceptar una sugerencia, lleva al POI y de vuelta al destino.
        // A nivel de proceso como las sugerencias: sobrevive a la pantalla apagada y a segundo plano.
        appScope.launch { tourDetour.run() }
        // el GPS, el Navigation Engine y el recálculo viven en el servicio (paso 10); StartNavigation llega de un toque,
        // así que la app está visible al arrancarlo. Revocar el permiso mata el proceso: basta con mirarlo aquí
        appScope.launch {
            navigationServiceRuns(store.state).filter { it }.collect {
                if (checkSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) NavigationService.start(this@HaptiMapApplication)
            }
        }
    }
}
