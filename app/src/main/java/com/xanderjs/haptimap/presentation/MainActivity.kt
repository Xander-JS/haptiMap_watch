package com.xanderjs.haptimap.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.wear.compose.material3.AppScaffold
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import com.xanderjs.haptimap.designsystem.theme.ThemeMode
import com.xanderjs.haptimap.presentation.catalog.DesignSystemCatalog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // shortcut: el tema vive en memoria; se persistirá en ajustes en un paso posterior
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
            HaptiMapTheme(themeMode = themeMode) {
                AppScaffold(timeText = { HaptiTimeText() }) {
                    DesignSystemCatalog(themeMode = themeMode, onThemeModeChange = { themeMode = it })
                }
            }
        }
    }
}
