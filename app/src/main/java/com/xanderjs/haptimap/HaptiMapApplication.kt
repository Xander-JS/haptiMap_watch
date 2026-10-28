package com.xanderjs.haptimap

import android.app.Application
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.RouteCache
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class HaptiMapApplication : Application() {
    @Inject lateinit var store: NavigationSessionStore
    @Inject lateinit var routeCache: RouteCache

    override fun onCreate() {
        super.onCreate()
        // la caché sigue a la sesión mientras viva el proceso, con o sin pantallas
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { routeCache.follow(store.state) }
    }
}
