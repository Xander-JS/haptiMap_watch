package com.xanderjs.haptimap

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Application
import android.content.pm.PackageManager.PERMISSION_GRANTED
import com.xanderjs.haptimap.core.haptics.HapticCues
import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.haptics.hapticsActive
import com.xanderjs.haptimap.core.navigation.NavigationEngine
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.RouteCache
import com.xanderjs.haptimap.core.navigation.navigationServiceRuns
import com.xanderjs.haptimap.service.NavigationService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    override fun onCreate() {
        super.onCreate()
        // la caché sigue a la sesión mientras viva el proceso, con o sin pantallas
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        appScope.launch { routeCache.follow(store.state) }
        // los avisos de la ruta, en vibración (paso 09); aquí y no en el servicio: el banco de pruebas los usa sin navegar
        val cues = HapticCues(haptics::playCount)
        appScope.launch { haptics.follow(engine.cues.mapNotNull(cues::eventFor), store.state.map { it.hapticsActive }) }
        // el GPS, el Navigation Engine y el recálculo viven en el servicio (paso 10); StartNavigation llega de un toque,
        // así que la app está visible al arrancarlo. Revocar el permiso mata el proceso: basta con mirarlo aquí
        appScope.launch {
            navigationServiceRuns(store.state).filter { it }.collect {
                if (checkSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) NavigationService.start(this@HaptiMapApplication)
            }
        }
    }
}
