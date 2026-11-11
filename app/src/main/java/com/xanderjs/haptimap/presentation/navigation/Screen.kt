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
    data object Search : Screen("search")
    data object SearchResults : Screen("search_results")
    data object PlaceDetail : Screen("place_detail")
    data object ProfileSelection : Screen("profile_selection")
    data object Settings : Screen("settings")
    data object Haptics : Screen("haptics")
    data object RouteSummary : Screen("route_summary")
    data object ResumeRoute : Screen("resume_route")
    data object Navigation : Screen("navigation")
    data object Tutorial : Screen("haptic_tutorial")
}

/**
 * La pantalla al abrir la app. El tutorial háptico (paso 13) solo la primera vez, después del permiso y antes de
 * ofrecer la ruta guardada; nunca con una navegación en curso, que se muestra siempre.
 */
fun startScreen(hasPermission: Boolean, navigating: Boolean, tutorialSeen: Boolean): Screen = when {
    !hasPermission -> Screen.Permission
    navigating -> Screen.Search
    !tutorialSeen -> Screen.Tutorial
    else -> Screen.ResumeRoute
}

/** Tras conceder el permiso: el tutorial si aún no se vio, si no la búsqueda. */
fun screenAfterPermission(tutorialSeen: Boolean): Screen = if (tutorialSeen) Screen.Search else Screen.Tutorial

fun NavGraphBuilder.screen(screen: Screen, content: @Composable (NavBackStackEntry) -> Unit) =
    composable(screen.route, content = content)

// no se llama navigate(): en navigation 2.8+ el miembro navigate(Any) taparía la extensión y esperaría una ruta serializable
fun NavController.navigateTo(screen: Screen, builder: NavOptionsBuilder.() -> Unit = {}) = navigate(screen.route, builder)
