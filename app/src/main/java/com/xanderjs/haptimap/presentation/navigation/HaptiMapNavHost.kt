package com.xanderjs.haptimap.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptionsBuilder
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.xanderjs.haptimap.BuildConfig
import androidx.compose.ui.platform.LocalContext
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.navigatingRoute
import com.xanderjs.haptimap.designsystem.theme.ThemeMode
import com.xanderjs.haptimap.designsystem.theme.appLanguage
import com.xanderjs.haptimap.feature.navigationui.NavigationScreen
import com.xanderjs.haptimap.feature.navigationui.ResumeRouteScreen
import com.xanderjs.haptimap.feature.navigationui.RouteSummaryScreen
import com.xanderjs.haptimap.feature.onboarding.LocationPermissionScreen
import com.xanderjs.haptimap.presentation.catalog.DesignSystemCatalog
import com.xanderjs.haptimap.presentation.debug.HapticsDebugScreen
import com.xanderjs.haptimap.presentation.debug.LocationDebugScreen
import com.xanderjs.haptimap.presentation.debug.PlacesDebugScreen
import com.xanderjs.haptimap.presentation.debug.SessionDebugScreen
import com.xanderjs.haptimap.presentation.debug.WalkSimulationSetting
import com.xanderjs.haptimap.presentation.debug.sampleRoute

/** Hay una navegación o una llegada que mostrar: lo que mantiene vivo el servicio en primer plano (paso 10). */
val NavigationSession.showsNavigation: Boolean
    get() = navigatingRoute != null || this is NavigationSession.Arrived

/** Grafo de pantallas. Deslizar a la derecha vuelve atrás en todas menos en la inicial. */
@Composable
fun HaptiMapNavHost(
    startScreen: Screen,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    walkSimulation: WalkSimulationSetting,
    store: NavigationSessionStore,
    openNavigation: Boolean = false,
    onNavigationOpened: () -> Unit = {},
) {
    val navController = rememberSwipeDismissableNavController()
    // la notificación del servicio (paso 10) vuelve a la navegación desde cualquier pantalla, sin duplicarla
    LaunchedEffect(openNavigation) {
        if (!openNavigation) return@LaunchedEffect
        if (store.state.value.showsNavigation) navController.navigateTo(Screen.Navigation) { launchSingleTop = true }
        onNavigationOpened()
    }
    SwipeDismissableNavHost(navController = navController, startDestination = startScreen.route) {
        screen(Screen.Permission) {
            LocationPermissionScreen(
                // el permiso sale de la pila: el catálogo pasa a ser la pantalla inicial
                onGranted = { navController.navigateTo(Screen.Catalog) { popUpTo(Screen.Permission.route) { inclusive = true } } },
            )
        }
        screen(Screen.Catalog) {
            val simulationMode by walkSimulation.mode.collectAsStateWithLifecycle()
            val sample = sampleRoute(LocalContext.current.appLanguage())
            DesignSystemCatalog(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onOpenLocationDebug = { navController.navigateTo(Screen.LocationDebug) },
                onOpenSessionDebug = { navController.navigateTo(Screen.SessionDebug) },
                onOpenPlacesDebug = { navController.navigateTo(Screen.PlacesDebug) },
                onOpenNavigation = { navController.navigateTo(Screen.Navigation) },
                // como retomar una ruta guardada: Idle → RouteReady, sin pedir nada a la red
                onLoadSampleRoute = sample?.let { route ->
                    {
                        store.dispatch(NavigationEvent.Cancel)
                        store.dispatch(NavigationEvent.RestoreRoute(route))
                        navController.navigateTo(Screen.RouteSummary)
                    }
                },
                // el simulador solo existe en debug (src/debug/.../LocationModule)
                simulationMode = simulationMode.takeIf { BuildConfig.DEBUG },
                onSimulationModeChange = { walkSimulation.mode.value = it },
                onOpenHaptics = { navController.navigateTo(Screen.Haptics) }.takeIf { BuildConfig.DEBUG },
            )
        }
        // banco de pruebas háptico: solo en debug, ni siquiera la ruta existe en release
        if (BuildConfig.DEBUG) screen(Screen.Haptics) { HapticsDebugScreen() }
        screen(Screen.LocationDebug) { LocationDebugScreen() }
        screen(Screen.SessionDebug) { SessionDebugScreen() }
        // al elegir un lugar se pide la ruta; atrás vuelve a los resultados
        screen(Screen.PlacesDebug) { PlacesDebugScreen(onPlaceSelected = { navController.navigateTo(Screen.RouteSummary) }) }
        // la navegación sustituye al resumen: volver atrás no debe ofrecer "Iniciar" otra vez
        screen(Screen.RouteSummary) {
            RouteSummaryScreen(
                onStart = { navController.navigateTo(Screen.Navigation) { popUpTo(Screen.RouteSummary.route) { inclusive = true } } },
            )
        }
        // detener vuelve al catálogo; tras la llegada, "Nueva búsqueda" abre la búsqueda sobre el catálogo
        screen(Screen.Navigation) {
            NavigationScreen(
                onStopped = { navController.popBackStack(Screen.Catalog.route, inclusive = false) },
                onNewSearch = { navController.navigateTo(Screen.PlacesDebug) { popUpTo(Screen.Catalog.route) } },
            )
        }
        // pantalla inicial con permiso: ofrece la ruta guardada o sigue al catálogo; sale de la pila, como el permiso
        screen(Screen.ResumeRoute) {
            val toCatalog: NavOptionsBuilder.() -> Unit = { popUpTo(Screen.ResumeRoute.route) { inclusive = true } }
            ResumeRouteScreen(
                onResumed = {
                    navController.navigateTo(Screen.Catalog, toCatalog)
                    navController.navigateTo(Screen.RouteSummary)
                },
                onDiscarded = { navController.navigateTo(Screen.Catalog, toCatalog) },
            )
        }
    }
}
