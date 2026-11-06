package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.navigation.NavigationEvent as E
import com.xanderjs.haptimap.core.navigation.NavigationSession as S

/**
 * Transiciones de la sesión como función pura. Un evento que no aplica al estado actual devuelve
 * el mismo estado (el llamador decide si lo registra); nunca lanza.
 */
fun reduce(state: S, event: E): S = when (event) {
    // atrás desde la elección de perfil vuelve a la búsqueda (paso 16), igual que Idle la arranca
    E.StartSearch -> if (state is S.Idle || state is S.ProfileSelection) S.Searching else state
    is E.SelectDestination -> if (state is S.Searching) S.DestinationSelected(event.destination) else state
    // elegir perfil se intercala entre destino y carga (paso 16)
    E.ChooseProfile -> if (state is S.DestinationSelected) S.ProfileSelection(state.destination) else state
    is E.SelectProfile -> if (state is S.ProfileSelection) S.RouteLoading(state.destination, event.profile) else state
    // "Reintentar": ya hay perfil, se vuelve a pedir la ruta directamente
    E.RequestRoute -> if (state is S.DestinationSelected) S.RouteLoading(state.destination, state.profile) else state
    is E.RouteRequestRejected -> if (state is S.DestinationSelected) S.DestinationSelected(state.destination, event.error, state.profile) else state
    is E.RouteLoaded -> if (state is S.RouteLoading) S.RouteReady(event.route, state.profile) else state
    is E.RouteFailed -> if (state is S.RouteLoading) S.DestinationSelected(state.destination, event.error, state.profile) else state
    is E.RestoreRoute -> if (state is S.Idle) S.RouteReady(event.route, event.profile) else state
    E.StartNavigation -> if (state is S.RouteReady) S.NavigationActive(state.route, state.profile) else state
    E.LeaveRoute -> if (state is S.NavigationActive) S.OffRoute(state.route, profile = state.profile) else state
    // también recalculando: volver a la ruta cancela la petición (paso 08)
    E.ReturnToRoute -> if (state is S.OffRoute || state is S.Recalculating) S.NavigationActive(state.activeRoute!!, state.navigationProfile) else state
    E.Recalculate -> if (state is S.OffRoute) S.Recalculating(state.route, state.profile) else state
    is E.RouteRecalculated -> if (state is S.Recalculating) S.NavigationActive(event.route, state.profile) else state
    is E.RecalculationFailed -> if (state is S.Recalculating) S.OffRoute(state.route, event.error, state.profile) else state
    E.Arrive -> if (state is S.NavigationActive) S.Arrived(state.route, state.profile) else state
    E.Finish -> if (state is S.Arrived) S.Completed(state.route, state.profile) else state
    E.Cancel -> S.Idle
}
