package com.xanderjs.haptimap.feature.navigationui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.navigation.NavigationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Estado del mapa continuo (paso 22). Combina datos que YA existen —la ruta activa, el avance del motor, el desvío
 * (paso 20) y la sugerencia (paso 19)— en un [NavigationMapState] puro. No pide nada a la red: reutiliza la ruta y
 * los POIs ya descargados. Null sin ruta activa.
 */
@HiltViewModel
class NavigationMapViewModel @Inject constructor(
    activeRoute: ObserveActiveRoute,
    engine: NavigationEngine,
    detour: TourDetourController,
    suggestions: TourSuggestionController,
) : ViewModel() {

    val mapState: StateFlow<NavigationMapState?> =
        combine(activeRoute(), engine.progress, detour.state, suggestions.state, ::navigationMapState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
