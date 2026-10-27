package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.Place

/**
 * Estado de la sesión de navegación (sección 20): qué está haciendo el viaje, no qué pantalla se ve.
 * Los estados POI de la v2 (PoiDetected, PoiSuggestion, Detour) se añadirán como nuevos subtipos que llevan la [Route].
 */
sealed interface NavigationSession {
    data object Idle : NavigationSession
    data object Searching : NavigationSession
    data class DestinationSelected(val destination: Place, val error: SessionError? = null) : NavigationSession
    data class RouteLoading(val destination: Place) : NavigationSession
    data class NavigationActive(val route: Route) : NavigationSession
    data class OffRoute(val route: Route, val error: SessionError? = null) : NavigationSession
    data class Recalculating(val route: Route) : NavigationSession
    data class Arrived(val route: Route) : NavigationSession
    data class Completed(val route: Route) : NavigationSession
}

/** Por qué falló la carga o el recálculo de la ruta. */
enum class SessionError { ROUTE_UNAVAILABLE, NO_CONNECTION }

/** PLACEHOLDER: el paso 05 (Google Routes) añade la geometría y las maniobras. */
data class Route(val destination: Place)
