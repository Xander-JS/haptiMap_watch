package com.xanderjs.haptimap.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.xanderjs.haptimap.feature.navigationui.ProfileSelectorScreen
import com.xanderjs.haptimap.feature.navigationui.ResumeRouteScreen
import com.xanderjs.haptimap.feature.navigationui.RouteSummaryScreen
import com.xanderjs.haptimap.feature.onboarding.HapticTutorialScreen
import com.xanderjs.haptimap.feature.onboarding.LocationPermissionScreen
import com.xanderjs.haptimap.feature.settings.SettingsScreen
import com.xanderjs.haptimap.feature.search.SearchResultsScreen
import com.xanderjs.haptimap.feature.search.SearchScreen
import com.xanderjs.haptimap.presentation.catalog.DesignSystemCatalog
import com.xanderjs.haptimap.presentation.debug.HapticsDebugViewModel
import com.xanderjs.haptimap.presentation.debug.HapticsDebugScreen
import com.xanderjs.haptimap.presentation.debug.LocationDebugScreen
import com.xanderjs.haptimap.presentation.debug.SessionDebugScreen
import com.xanderjs.haptimap.presentation.debug.WalkSimulationSetting
import com.xanderjs.haptimap.presentation.debug.sampleRoute

/** Hay una navegación o una llegada que mostrar: lo que mantiene vivo el servicio en primer plano (paso 10). */
val NavigationSession.showsNavigation: Boolean
    get() = navigatingRoute != null || this is NavigationSession.Arrived

/** Grafo de pantallas. Deslizar a la derecha vuelve atrás en todas menos en la inicial (la búsqueda, desde el paso 11). */
@Composable
fun HaptiMapNavHost(
    startScreen: Screen,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    walkSimulation: WalkSimulationSetting,
    store: NavigationSessionStore,
    tutorialSeen: () -> Boolean = { true },
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
                // el permiso sale de la pila: la búsqueda (o el tutorial la primera vez) pasa a ser la pantalla inicial
                onGranted = {
                    navController.navigateTo(screenAfterPermission(tutorialSeen())) { popUpTo(Screen.Permission.route) { inclusive = true } }
                },
            )
        }
        // Modo Búsqueda (paso 11), la pantalla inicial; los resultados comparten su ViewModel
        screen(Screen.Search) {
            SearchScreen(
                onOpenResults = { navController.navigateTo(Screen.SearchResults) },
                onOpenRoute = { navigating ->
                    navController.navigateTo(if (navigating) Screen.Navigation else Screen.RouteSummary) { launchSingleTop = true }
                },
                // ajustes (paso 25): desde ahí se repite el tutorial háptico, entre otras cosas
                onOpenSettings = { navController.navigateTo(Screen.Settings) },
                // catálogo y pantallas de depuración: solo en debug
                onOpenCatalog = { navController.navigateTo(Screen.Catalog) }.takeIf { BuildConfig.DEBUG },
            )
        }
        // al elegir un lugar se elige el perfil (paso 16); atrás vuelve a los resultados
        screen(Screen.SearchResults) { entry ->
            val searchEntry = remember(entry) { navController.getBackStackEntry(Screen.Search.route) }
            SearchResultsScreen(
                onDestinationSelected = { navController.navigateTo(Screen.ProfileSelection) },
                onExit = { navController.popBackStack() },
                viewModel = hiltViewModel(searchEntry),
            )
        }
        // elegir perfil (paso 16), entre el destino y el resumen; atrás (deslizar) vuelve a los resultados
        screen(Screen.ProfileSelection) {
            ProfileSelectorScreen(onSelected = { navController.navigateTo(Screen.RouteSummary) })
        }
        // ajustes (paso 25), desde la búsqueda; el tutorial háptico se repite desde aquí
        screen(Screen.Settings) {
            SettingsScreen(onReplayTutorial = { navController.navigateTo(Screen.Tutorial) })
        }
        if (BuildConfig.DEBUG) screen(Screen.Catalog) {
            val simulationMode by walkSimulation.mode.collectAsStateWithLifecycle()
            val noVibrator by walkSimulation.noVibrator.collectAsStateWithLifecycle()
            val sample = sampleRoute(LocalContext.current.appLanguage())
            val haptics: HapticsDebugViewModel = hiltViewModel()
            val pulse by haptics.pulse.collectAsStateWithLifecycle()
            DesignSystemCatalog(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onOpenLocationDebug = { navController.navigateTo(Screen.LocationDebug) },
                onOpenSessionDebug = { navController.navigateTo(Screen.SessionDebug) },
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
                noVibrator = noVibrator.takeIf { BuildConfig.DEBUG },
                onNoVibratorChange = { walkSimulation.noVibrator.value = it },
                pulse = pulse,
                onPlayCue = haptics::play,
            )
        }
        // banco de pruebas háptico: solo en debug, ni siquiera la ruta existe en release
        if (BuildConfig.DEBUG) screen(Screen.Haptics) { HapticsDebugScreen() }
        screen(Screen.LocationDebug) { LocationDebugScreen() }
        screen(Screen.SessionDebug) { SessionDebugScreen() }
        // la navegación sustituye al resumen: volver atrás no debe ofrecer "Iniciar" otra vez
        screen(Screen.RouteSummary) {
            RouteSummaryScreen(
                onStart = { navController.navigateTo(Screen.Navigation) { popUpTo(Screen.RouteSummary.route) { inclusive = true } } },
            )
        }
        // detener y "Nueva búsqueda" tras la llegada vuelven a la búsqueda, la raíz de la pila
        screen(Screen.Navigation) {
            NavigationScreen(
                onStopped = { navController.popBackStack(Screen.Search.route, inclusive = false) },
                onNewSearch = { navController.popBackStack(Screen.Search.route, inclusive = false) },
            )
        }
        // tutorial háptico (paso 13). La primera vez es la raíz: al acabar sigue como un arranque normal (ruta guardada
        // o búsqueda). Repetido desde la búsqueda: vuelve a ella. Deslizar fuera la primera vez no lo marca como visto
        screen(Screen.Tutorial) {
            HapticTutorialScreen(
                onDone = {
                    if (navController.previousBackStackEntry == null) {
                        navController.navigateTo(Screen.ResumeRoute) { popUpTo(Screen.Tutorial.route) { inclusive = true } }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        // pantalla inicial con permiso: ofrece la ruta guardada o sigue a la búsqueda; sale de la pila, como el permiso
        screen(Screen.ResumeRoute) {
            val toSearch: NavOptionsBuilder.() -> Unit = { popUpTo(Screen.ResumeRoute.route) { inclusive = true } }
            ResumeRouteScreen(
                onResumed = {
                    navController.navigateTo(Screen.Search, toSearch)
                    navController.navigateTo(Screen.RouteSummary)
                },
                onDiscarded = { navController.navigateTo(Screen.Search, toSearch) },
            )
        }
    }
}
