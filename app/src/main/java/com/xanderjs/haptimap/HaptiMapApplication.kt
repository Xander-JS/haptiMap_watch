package com.xanderjs.haptimap

import android.app.Application
import com.xanderjs.haptimap.core.haptics.HapticCues
import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.haptics.hapticsActive
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.location.fixes
import com.xanderjs.haptimap.core.navigation.NavigationEngine
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.RouteCache
import com.xanderjs.haptimap.core.navigation.RouteRecalculator
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import com.xanderjs.haptimap.core.network.RoutesRepository
import com.xanderjs.haptimap.designsystem.theme.appLanguage
import com.xanderjs.haptimap.presentation.debug.rerouteRequest
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

@HiltAndroidApp
class HaptiMapApplication : Application() {
    @Inject lateinit var store: NavigationSessionStore
    @Inject lateinit var routeCache: RouteCache
    @Inject lateinit var engine: NavigationEngine
    @Inject lateinit var locationSource: LocationSource
    @Inject lateinit var recalculator: RouteRecalculator
    @Inject lateinit var routes: RoutesRepository
    @Inject lateinit var connectivity: ConnectivityMonitor
    @Inject lateinit var haptics: HapticEngine

    override fun onCreate() {
        super.onCreate()
        // la caché sigue a la sesión mientras viva el proceso, con o sin pantallas
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        appScope.launch { routeCache.follow(store.state) }
        // el GPS solo se usa en NavigationActive; el servicio del paso 10 hará esta misma llamada
        appScope.launch { engine.follow(locationSource.fixes()) }
        // los avisos de la ruta, en vibración (paso 09); se mudará al servicio del paso 10 junto con engine.follow
        val cues = HapticCues(haptics::playCount)
        appScope.launch { haptics.follow(engine.cues.mapNotNull(cues::eventFor), store.state.map { it.hapticsActive }) }
        // recálculo al desviarse (paso 08); en debug sin clave, una ruta falsa
        appScope.launch { recalculator.follow(rerouteRequest(routes) { appLanguage() }, connectivity.status) }
    }
}
