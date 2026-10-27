package com.xanderjs.haptimap.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.xanderjs.haptimap.designsystem.theme.ThemeMode
import com.xanderjs.haptimap.feature.onboarding.LocationPermissionScreen
import com.xanderjs.haptimap.presentation.catalog.DesignSystemCatalog
import com.xanderjs.haptimap.presentation.debug.LocationDebugScreen

/** Grafo de pantallas. Deslizar a la derecha vuelve atrás en todas menos en la inicial. */
@Composable
fun HaptiMapNavHost(startScreen: Screen, themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val navController = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(navController = navController, startDestination = startScreen.route) {
        screen(Screen.Permission) {
            LocationPermissionScreen(
                // el permiso sale de la pila: el catálogo pasa a ser la pantalla inicial
                onGranted = { navController.navigateTo(Screen.Catalog) { popUpTo(Screen.Permission.route) { inclusive = true } } },
            )
        }
        screen(Screen.Catalog) {
            DesignSystemCatalog(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onOpenLocationDebug = { navController.navigateTo(Screen.LocationDebug) },
            )
        }
        screen(Screen.LocationDebug) { LocationDebugScreen() }
    }
}
