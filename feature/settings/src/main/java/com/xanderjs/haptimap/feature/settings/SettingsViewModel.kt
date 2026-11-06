package com.xanderjs.haptimap.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.haptics.amplitudeScale
import com.xanderjs.haptimap.core.model.DetourTolerance
import com.xanderjs.haptimap.core.model.DisplayMode
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticIntensity
import com.xanderjs.haptimap.core.model.LanguageOption
import com.xanderjs.haptimap.core.model.SettingsRepository
import com.xanderjs.haptimap.core.model.ThemeOption
import com.xanderjs.haptimap.core.model.UserSettings
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.navigatingRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Ajustes (paso 25). Lee los [UserSettings] del repositorio (única fuente) y los modifica; el resto de la app
 * reacciona a través del propio repositorio (tema e idioma en MainActivity, intensidad en la Application, etc.).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val engine: HapticEngine,
    store: NavigationSessionStore,
) : ViewModel() {

    val settings: StateFlow<UserSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    /** Hay un trayecto en curso: repetir el tutorial se desactiva para que sus patrones no choquen con los avisos de ruta. */
    val navigating: StateFlow<Boolean> =
        store.state.map { it.navigatingRoute != null }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Cambiar la intensidad suena al instante a la nueva, por el Haptic Engine (nunca el vibrador directo). Usa
     * [HapticEvent.CONTINUE], un patrón real de navegación (un pulso suave): deja notar la intensidad tal como se
     * sentirá al caminar, sin inventar un evento solo para la vista previa.
     */
    fun setHapticIntensity(intensity: HapticIntensity) {
        engine.intensity = intensity.amplitudeScale
        engine.play(HapticEvent.CONTINUE)
        update { it.copy(hapticIntensity = intensity) }
    }

    // el repositorio recorta el timeout a su rango; la UI apaga los extremos
    fun setSuggestionTimeout(seconds: Int) = update { it.copy(suggestionTimeoutSeconds = seconds) }

    fun toggleFavorite(category: ExplorationCategory) = update {
        val favorites = if (category in it.favoriteCategories) it.favoriteCategories - category else it.favoriteCategories + category
        it.copy(favoriteCategories = favorites)
    }

    fun setDetourTolerance(tolerance: DetourTolerance) = update { it.copy(detourTolerance = tolerance) }
    fun setDisplayMode(mode: DisplayMode) = update { it.copy(displayMode = mode) }
    fun setTheme(theme: ThemeOption) = update { it.copy(theme = theme) }
    fun setLanguage(language: LanguageOption) = update { it.copy(language = language) }

    private fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { repository.update(transform) }
    }
}
