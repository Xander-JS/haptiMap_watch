package com.xanderjs.haptimap.core.navigation

import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Dueño único del estado de la sesión. Vive fuera de los ViewModels para que el servicio en primer
 * plano (paso 10) vea el mismo estado con la pantalla apagada.
 */
@Singleton
class NavigationSessionStore @Inject constructor() {
    private val _state = MutableStateFlow<NavigationSession>(NavigationSession.Idle)
    val state: StateFlow<NavigationSession> = _state.asStateFlow()

    fun dispatch(event: NavigationEvent) {
        _state.update { before ->
            reduce(before, event).also { after ->
                // java.util.logging llega a logcat en Android sin que este módulo dependa de Android
                if (after == before) logger.info("Ignored ${event.label} in ${before.label}") else logger.info("${before.label} + ${event.label} -> ${after.label}")
            }
        }
    }

    // una ruta tiene cientos de puntos: entera no cabe en una línea de logcat y se cortaba la transición
    private val NavigationSession.label: String
        get() = when (this) {
            is NavigationSession.DestinationSelected -> "DestinationSelected(${destination.name}${error?.let { ", $it" } ?: ""})"
            is NavigationSession.RouteLoading -> "RouteLoading(${destination.name})"
            is NavigationSession.RouteReady -> "RouteReady(${route.destination.name})"
            is NavigationSession.NavigationActive -> "NavigationActive(${route.destination.name})"
            is NavigationSession.OffRoute -> "OffRoute(${route.destination.name}${error?.let { ", $it" } ?: ""})"
            is NavigationSession.Recalculating -> "Recalculating(${route.destination.name})"
            is NavigationSession.Arrived -> "Arrived(${route.destination.name})"
            is NavigationSession.Completed -> "Completed(${route.destination.name})"
            NavigationSession.Idle, NavigationSession.Searching -> toString()
        }

    private val NavigationEvent.label: String
        get() = when (this) {
            is NavigationEvent.SelectDestination -> "SelectDestination(${destination.name})"
            is NavigationEvent.RouteLoaded -> "RouteLoaded(${route.destination.name})"
            is NavigationEvent.RestoreRoute -> "RestoreRoute(${route.destination.name})"
            is NavigationEvent.RouteRecalculated -> "RouteRecalculated(${route.destination.name})"
            else -> toString()
        }

    private companion object {
        val logger: Logger = Logger.getLogger("HaptiSession")
    }
}
