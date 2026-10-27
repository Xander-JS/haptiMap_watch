package com.xanderjs.haptimap.core.navigation

/** Lo que mueve la sesión de un estado a otro. Ver la tabla de transiciones en ARCHITECTURE.md. */
sealed interface NavigationEvent {
    data object StartSearch : NavigationEvent
    data class SelectDestination(val destination: Destination) : NavigationEvent
    data object RequestRoute : NavigationEvent
    data class RouteLoaded(val route: Route) : NavigationEvent
    data class RouteFailed(val error: SessionError) : NavigationEvent
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
