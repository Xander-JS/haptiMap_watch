package com.xanderjs.haptimap.feature.navigationui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.RouteCache
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Loading mientras se lee la caché; Ready con la ruta a ofrecer; NoRoute si no hay nada que retomar. */
sealed interface ResumeRouteUiState {
    data object Loading : ResumeRouteUiState
    data class Ready(val route: Route) : ResumeRouteUiState
    data object NoRoute : ResumeRouteUiState
}

/**
 * Primera pantalla con permiso (paso 06): lee la ruta guardada en [io] y ofrece retomarla tras la muerte
 * del proceso. Solo con la sesión en Idle: tras retomarla o descartarla ya no hay nada que ofrecer.
 */
@HiltViewModel
class ResumeRouteViewModel @Inject constructor(
    private val store: NavigationSessionStore,
    private val cache: RouteCache,
    io: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ResumeRouteUiState>(ResumeRouteUiState.Loading)
    val uiState: StateFlow<ResumeRouteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val route = if (store.state.value == NavigationSession.Idle) withContext(io) { cache.load() } else null
            _uiState.value = route?.let(ResumeRouteUiState::Ready) ?: ResumeRouteUiState.NoRoute
        }
    }

    /** Vuelve a RouteReady con la ruta guardada; la caché la vuelve a guardar al seguir la sesión. */
    fun resume() {
        (uiState.value as? ResumeRouteUiState.Ready)?.let { store.dispatch(NavigationEvent.RestoreRoute(it.route)) }
    }

    fun discard() = cache.clear()
}
