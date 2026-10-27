package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.navigation.NavigationEvent as E
import com.xanderjs.haptimap.core.navigation.NavigationSession as S

/**
 * Transiciones de la sesión como función pura. Un evento que no aplica al estado actual devuelve
 * el mismo estado (el llamador decide si lo registra); nunca lanza.
 */
fun reduce(state: S, event: E): S = when (event) {
    E.StartSearch -> if (state is S.Idle) S.Searching else state
    is E.SelectDestination -> if (state is S.Searching) S.DestinationSelected(event.destination) else state
    E.RequestRoute -> if (state is S.DestinationSelected) S.RouteLoading(state.destination) else state
    is E.RouteLoaded -> if (state is S.RouteLoading) S.NavigationActive(event.route) else state
    is E.RouteFailed -> if (state is S.RouteLoading) S.DestinationSelected(state.destination, event.error) else state
    E.LeaveRoute -> if (state is S.NavigationActive) S.OffRoute(state.route) else state
    E.ReturnToRoute -> if (state is S.OffRoute) S.NavigationActive(state.route) else state
    E.Recalculate -> if (state is S.OffRoute) S.Recalculating(state.route) else state
    is E.RouteRecalculated -> if (state is S.Recalculating) S.NavigationActive(event.route) else state
    is E.RecalculationFailed -> if (state is S.Recalculating) S.OffRoute(state.route, event.error) else state
    E.Arrive -> if (state is S.NavigationActive) S.Arrived(state.route) else state
    E.Finish -> if (state is S.Arrived) S.Completed(state.route) else state
    E.Cancel -> S.Idle
}
