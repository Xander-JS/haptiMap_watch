package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route

/**
 * Estado de la sesión de navegación (sección 20): qué está haciendo el viaje, no qué pantalla se ve.
 * Los estados POI de la v2 (PoiDetected, PoiSuggestion, Detour) se añadirán como nuevos subtipos que llevan la [Route].
 *
 * El [NavigationProfile] elegido (paso 16) viaja en cada estado del trayecto (de RouteLoading en adelante), para que
 * el Navigation Engine y los futuros módulos de Tour (pasos 17-20) lo lean de la sesión, nunca de la UI. Por defecto
 * FOCUS, el comportamiento de la v1, así que cualquier estado construido sin perfil sigue siendo FOCUS.
 */
sealed interface NavigationSession {
    data object Idle : NavigationSession
    data object Searching : NavigationSession
    data class DestinationSelected(val destination: Place, val error: SessionError? = null, val profile: NavigationProfile = NavigationProfile.FOCUS) : NavigationSession

    /** Elegir perfil (paso 16), entre DestinationSelected y RouteLoading: Focus o Tour, o atrás a la búsqueda. */
    data class ProfileSelection(val destination: Place) : NavigationSession
    data class RouteLoading(val destination: Place, val profile: NavigationProfile = NavigationProfile.FOCUS) : NavigationSession

    /** Ruta cargada y resumida; la navegación empieza cuando el usuario la inicia (StartNavigation). */
    data class RouteReady(val route: Route, val profile: NavigationProfile = NavigationProfile.FOCUS) : NavigationSession
    data class NavigationActive(val route: Route, val profile: NavigationProfile = NavigationProfile.FOCUS) : NavigationSession
    data class OffRoute(val route: Route, val error: SessionError? = null, val profile: NavigationProfile = NavigationProfile.FOCUS) : NavigationSession
    data class Recalculating(val route: Route, val profile: NavigationProfile = NavigationProfile.FOCUS) : NavigationSession
    data class Arrived(val route: Route, val profile: NavigationProfile = NavigationProfile.FOCUS) : NavigationSession
    data class Completed(val route: Route, val profile: NavigationProfile = NavigationProfile.FOCUS) : NavigationSession
}

/** El perfil elegido para el trayecto; FOCUS donde aún no se ha elegido (Idle, Searching, ProfileSelection). */
val NavigationSession.navigationProfile: NavigationProfile
    get() = when (this) {
        is NavigationSession.DestinationSelected -> profile
        is NavigationSession.RouteLoading -> profile
        is NavigationSession.RouteReady -> profile
        is NavigationSession.NavigationActive -> profile
        is NavigationSession.OffRoute -> profile
        is NavigationSession.Recalculating -> profile
        is NavigationSession.Arrived -> profile
        is NavigationSession.Completed -> profile
        else -> NavigationProfile.FOCUS
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
