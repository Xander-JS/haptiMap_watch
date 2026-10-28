package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route

/**
 * Estado de la sesión de navegación (sección 20): qué está haciendo el viaje, no qué pantalla se ve.
 * Los estados POI de la v2 (PoiDetected, PoiSuggestion, Detour) se añadirán como nuevos subtipos que llevan la [Route].
 */
sealed interface NavigationSession {
    data object Idle : NavigationSession
    data object Searching : NavigationSession
    data class DestinationSelected(val destination: Place, val error: SessionError? = null) : NavigationSession
    data class RouteLoading(val destination: Place) : NavigationSession

    /** Ruta cargada y resumida; la navegación empieza cuando el usuario la inicia (StartNavigation). */
    data class RouteReady(val route: Route) : NavigationSession
    data class NavigationActive(val route: Route) : NavigationSession
    data class OffRoute(val route: Route, val error: SessionError? = null) : NavigationSession
    data class Recalculating(val route: Route) : NavigationSession
    data class Arrived(val route: Route) : NavigationSession
    data class Completed(val route: Route) : NavigationSession
}

/**
 * La ruta que se va a seguir o se está siguiendo: de RouteReady a Recalculating. Es la que guarda la
 * caché (paso 06) y la que sigue sin red. Null en Arrived y Completed: ya no hay nada que retomar.
 */
val NavigationSession.activeRoute: Route?
    get() = when (this) {
        is NavigationSession.RouteReady -> route
        is NavigationSession.NavigationActive -> route
        is NavigationSession.OffRoute -> route
        is NavigationSession.Recalculating -> route
        else -> null
    }

/**
 * La ruta que se está navegando: la activa una vez empezada (NavigationActive, OffRoute, Recalculating).
 * El Navigation Engine sigue la misma ruta con el mismo tracker durante el desvío (paso 08).
 */
val NavigationSession.navigatingRoute: Route?
    get() = if (this is NavigationSession.RouteReady) null else activeRoute

/** Por qué falló la carga o el recálculo de la ruta. */
enum class SessionError { ROUTE_UNAVAILABLE, NO_CONNECTION }
