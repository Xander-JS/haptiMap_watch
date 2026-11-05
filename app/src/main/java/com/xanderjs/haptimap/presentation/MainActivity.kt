package com.xanderjs.haptimap.presentation

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.rememberAmbientModeManager
import androidx.wear.compose.material3.AppScaffold
import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import com.xanderjs.haptimap.designsystem.theme.ThemeMode
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.feature.navigationui.AmbientNavigationScreen
import com.xanderjs.haptimap.feature.onboarding.TutorialStore
import com.xanderjs.haptimap.presentation.debug.DebugHapticOverlay
import com.xanderjs.haptimap.presentation.debug.WalkSimulationSetting
import com.xanderjs.haptimap.presentation.navigation.HaptiMapNavHost
import com.xanderjs.haptimap.presentation.navigation.showsNavigation
import com.xanderjs.haptimap.presentation.navigation.startScreen
import com.xanderjs.haptimap.service.NavigationService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var connectivity: ConnectivityMonitor
    @Inject lateinit var walkSimulation: WalkSimulationSetting
    @Inject lateinit var store: NavigationSessionStore
    @Inject lateinit var haptics: HapticEngine
    @Inject lateinit var tutorial: TutorialStore

    /** Abrir la navegación: el toque en la notificación o la Ongoing Activity, o arrancar con una navegación en curso. */
    private var openNavigation by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // al recrearse, la pila de pantallas se restaura sola
        if (savedInstanceState == null) openNavigation = intent.opensNavigation() || store.state.value.showsNavigation
        setContent {
            // shortcut: el tema vive en memoria; se persistirá en ajustes en un paso posterior
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
            // revocar el permiso en ajustes mata el proceso, así que basta con comprobarlo al crear la Activity
            // con permiso se arranca en ResumeRoute: lee la ruta guardada fuera del hilo principal y sigue a la búsqueda si no hay
            // con una navegación en curso (el proceso siguió vivo con el servicio), la búsqueda con la navegación encima
            // la primera vez, el tutorial háptico antes (paso 13)
            val startScreen = remember {
                startScreen(
                    hasPermission = checkSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED,
                    navigating = store.state.value.showsNavigation,
                    tutorialSeen = tutorial.seen,
                )
            }
            // con la Activity en segundo plano nadie recolecta y el NetworkCallback se desregistra
            val connectivityStatus by connectivity.status.collectAsStateWithLifecycle()
            // paso 12: observar el ambiente hace la app "ambiactiva" en todas las versiones de Wear: pinta su propia vista
            val ambientModeManager = rememberAmbientModeManager()
            val ambient = ambientModeManager.currentAmbientMode is AmbientMode.Ambient
            // el tema de ambiente en la raíz: también la hora de AppScaffold, que se compone fuera de las pantallas
            HaptiMapTheme(themeMode = themeMode, ambient = ambient) {
                CompositionLocalProvider(
                    LocalAmbientModeManager provides ambientModeManager,
                    LocalOffline provides (connectivityStatus == ConnectivityStatus.Disconnected),
                ) {
                    AppScaffold(timeText = { HaptiTimeText() }) {
                        // en ambiente las pantallas de debajo se paran: no se redibujan con cada fix
                        CompositionLocalProvider(LocalLifecycleOwner provides rememberStoppableLifecycleOwner(stopped = ambient)) {
                            HaptiMapNavHost(
                                startScreen = startScreen,
                                themeMode = themeMode,
                                onThemeModeChange = { themeMode = it },
                                walkSimulation = walkSimulation,
                                store = store,
                                tutorialSeen = { tutorial.seen },
                                openNavigation = openNavigation,
                                onNavigationOpened = { openNavigation = false },
                            )
                        }
                        // navegando, la vista de ambiente encima de cualquier pantalla; sin navegación no pinta nada
                        if (ambient) AmbientNavigationScreen()
                        // solo en debug: el patrón que acaba de sonar, sobre cualquier pantalla
                        DebugHapticOverlay(haptics.played)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.opensNavigation()) openNavigation = true
    }

    private fun Intent.opensNavigation() = getBooleanExtra(NavigationService.EXTRA_OPEN_NAVIGATION, false)
}
