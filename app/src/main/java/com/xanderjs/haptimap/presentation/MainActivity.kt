package com.xanderjs.haptimap.presentation

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import com.xanderjs.haptimap.designsystem.theme.ThemeMode
import com.xanderjs.haptimap.presentation.navigation.HaptiMapNavHost
import com.xanderjs.haptimap.presentation.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var connectivity: ConnectivityMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // shortcut: el tema vive en memoria; se persistirá en ajustes en un paso posterior
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
            // revocar el permiso en ajustes mata el proceso, así que basta con comprobarlo al crear la Activity
            val startScreen = remember {
                if (checkSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) Screen.Catalog else Screen.Permission
            }
            // con la Activity en segundo plano nadie recolecta y el NetworkCallback se desregistra
            val connectivityStatus by connectivity.status.collectAsStateWithLifecycle()
            HaptiMapTheme(themeMode = themeMode) {
                CompositionLocalProvider(LocalOffline provides (connectivityStatus == ConnectivityStatus.Disconnected)) {
                    AppScaffold(timeText = { HaptiTimeText() }) {
                        HaptiMapNavHost(startScreen = startScreen, themeMode = themeMode, onThemeModeChange = { themeMode = it })
                    }
                }
            }
        }
    }
}
