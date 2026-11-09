package com.xanderjs.haptimap.feature.navigationui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.feature.navigationui.BuildConfig
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

    // Cada carga de mapa se factura. El ViewModel vive un trayecto (está en el back stack de navegación), así que este
    // contador es "cargas por trayecto". Con la instancia única debe quedarse en 1; si sube, el mapa se está
    // recreando (p. ej. desmontándose en ambiente). Solo en debug, por Logcat.
    private var mapLoads = 0

    fun onMapLoaded() {
        mapLoads++
        if (BuildConfig.DEBUG) Log.d("HaptiMapCost", "Carga de mapa #$mapLoads en este trayecto")
    }
}
