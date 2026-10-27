package com.xanderjs.haptimap.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.wear.compose.navigation.composable

/**
 * Pantallas de la app. Wear compose-navigation solo admite rutas de texto: este tipo las encierra
 * para que no se pueda navegar a una ruta inexistente. Sin argumentos por ahora.
 */
sealed class Screen(val route: String) {
    data object Permission : Screen("permission")
    data object Catalog : Screen("catalog")
    data object LocationDebug : Screen("location_debug")
    data object SessionDebug : Screen("session_debug")
}

fun NavGraphBuilder.screen(screen: Screen, content: @Composable (NavBackStackEntry) -> Unit) =
    composable(screen.route, content = content)

// no se llama navigate(): en navigation 2.8+ el miembro navigate(Any) taparía la extensión y esperaría una ruta serializable
fun NavController.navigateTo(screen: Screen, builder: NavOptionsBuilder.() -> Unit = {}) = navigate(screen.route, builder)
