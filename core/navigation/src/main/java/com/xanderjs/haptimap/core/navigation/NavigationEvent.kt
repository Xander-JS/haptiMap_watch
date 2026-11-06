package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route

/** Lo que mueve la sesión de un estado a otro. Ver la tabla de transiciones en ARCHITECTURE.md. */
sealed interface NavigationEvent {
    data object StartSearch : NavigationEvent
    data class SelectDestination(val destination: Place) : NavigationEvent

    /** Elegir perfil (paso 16): DestinationSelected → ProfileSelection. */
    data object ChooseProfile : NavigationEvent

    /** Perfil elegido (paso 16): ProfileSelection → RouteLoading con el perfil. */
    data class SelectProfile(val profile: NavigationProfile) : NavigationEvent

    /** Pide la ruta ya con el perfil decidido: DestinationSelected → RouteLoading. También es "Reintentar". */
    data object RequestRoute : NavigationEvent

    /** La ruta no se pide (p. ej. sin conexión): vuelve con error sin pasar por RouteLoading, que dispara la petición de red. */
    data class RouteRequestRejected(val error: SessionError) : NavigationEvent
    data class RouteLoaded(val route: Route) : NavigationEvent
    data class RouteFailed(val error: SessionError) : NavigationEvent
    data object StartNavigation : NavigationEvent

    /** Retoma la ruta guardada en caché tras la muerte del proceso (paso 06): Idle → RouteReady, con el perfil recordado. */
    data class RestoreRoute(val route: Route, val profile: NavigationProfile = NavigationProfile.FOCUS) : NavigationEvent
    data object LeaveRoute : NavigationEvent
    data object ReturnToRoute : NavigationEvent
    data object Recalculate : NavigationEvent
    data class RouteRecalculated(val route: Route) : NavigationEvent
    data class RecalculationFailed(val error: SessionError) : NavigationEvent
    data object Arrive : NavigationEvent
    data object Finish : NavigationEvent

    /** Vuelve a Idle desde cualquier estado; en Completed sirve para cerrar. */
    data object Cancel : NavigationEvent
}
