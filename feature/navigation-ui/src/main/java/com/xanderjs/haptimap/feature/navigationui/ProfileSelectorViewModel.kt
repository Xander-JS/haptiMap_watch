package com.xanderjs.haptimap.feature.navigationui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.ProfilePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Elegir perfil (paso 16), entre elegir destino y pedir la ruta. Mueve la sesión a ProfileSelection al entrar y a
 * RouteLoading con el perfil al elegir. El perfil es de la sesión, no de la UI: el Navigation Engine y el Tour
 * (pasos 17-20) lo leen de ahí. [preselected] es el último elegido (DataStore), resaltado para confirmar con un toque.
 */
@HiltViewModel
class ProfileSelectorViewModel @Inject constructor(
    private val store: NavigationSessionStore,
    private val engine: HapticEngine,
    private val preference: ProfilePreference,
) : ViewModel() {

    val preselected: StateFlow<NavigationProfile> =
        preference.profile.stateIn(viewModelScope, SharingStarted.Eagerly, NavigationProfile.FOCUS)

    init {
        store.dispatch(NavigationEvent.ChooseProfile)
    }

    /** Elegir un perfil: confirmación háptica por evento semántico (nunca el vibrador directo), recordarlo y seguir. */
    fun select(profile: NavigationProfile) {
        engine.play(HapticEvent.PROFILE_SELECTED)
        viewModelScope.launch { preference.save(profile) }
        store.dispatch(NavigationEvent.SelectProfile(profile))
    }
}
