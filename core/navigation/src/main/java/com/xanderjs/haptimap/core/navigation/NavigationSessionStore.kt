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
                if (after == before) logger.info("Ignored $event in $before") else logger.info("$before + $event -> $after")
            }
        }
    }

    private companion object {
        val logger: Logger = Logger.getLogger("HaptiSession")
    }
}
