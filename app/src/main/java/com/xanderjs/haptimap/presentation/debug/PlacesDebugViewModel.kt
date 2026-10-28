package com.xanderjs.haptimap.presentation.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.model.ConnectivityPolicy
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.core.model.PlaceSearchResult
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import com.xanderjs.haptimap.core.network.PlacesRepository
import com.xanderjs.haptimap.designsystem.organisms.SearchStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Cuánto se espera a la primera posición antes de buscar; pasado esto la búsqueda da NO_LOCATION. */
const val LOCATION_FIX_TIMEOUT_MILLIS = 10_000L

/** [status] null = sin búsqueda en curso ni fallida: se muestran [places] (vacío antes de la primera búsqueda). */
data class PlacesDebugUiState(
    val selectedCategory: PlaceCategory? = null,
    val places: List<Place> = emptyList(),
    val status: SearchStatus? = null,
)

/** Pantalla temporal (paso 04): busca lugares reales y elige uno como destino de la sesión. */
@HiltViewModel
class PlacesDebugViewModel @Inject constructor(
    private val repository: PlacesRepository,
    private val locationSource: LocationSource,
    private val store: NavigationSessionStore,
    private val connectivity: ConnectivityMonitor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlacesDebugUiState())
    val uiState: StateFlow<PlacesDebugUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    fun searchCategory(category: PlaceCategory) = search(category) { repository.searchByCategory(category, it) }

    fun searchText(query: String) {
        if (query.isNotBlank()) search(null) { repository.searchByText(query.trim(), it) }
    }

    fun selectPlace(place: Place) = store.dispatch(NavigationEvent.SelectDestination(place))

    private fun search(category: PlaceCategory?, request: suspend (GeoPoint?) -> PlaceSearchResult) {
        startSearchSession()
        // una búsqueda nueva sustituye a la anterior: no llegan resultados viejos tarde
        searchJob?.cancel()
        _uiState.value = PlacesDebugUiState(selectedCategory = category, status = SearchStatus.Loading)
        searchJob = viewModelScope.launch {
            // sin red se avisa ya, sin esperar al GPS; el repositorio vuelve a comprobarlo antes de pedir
            val result = if (ConnectivityPolicy.canSearch(connectivity.status.value)) {
                request(currentLocation())
            } else {
                PlaceSearchResult.Failure(PlaceSearchError.NO_CONNECTION)
            }
            _uiState.update {
                when (result) {
                    is PlaceSearchResult.Success -> it.copy(places = result.places, status = null)
                    is PlaceSearchResult.Failure -> it.copy(status = SearchStatus.Failed(result.error))
                }
            }
        }
    }

    // buscar es el paso Idle → Searching; desde otro estado la depuración vuelve a empezar la sesión
    private fun startSearchSession() {
        val session = store.state.value
        if (session is NavigationSession.Searching) return
        if (session !is NavigationSession.Idle) store.dispatch(NavigationEvent.Cancel)
        store.dispatch(NavigationEvent.StartSearch)
    }

    private suspend fun currentLocation(): GeoPoint? {
        val status = withTimeoutOrNull(LOCATION_FIX_TIMEOUT_MILLIS) {
            locationSource.gpsStatus().firstOrNull { it != GpsStatus.Searching }
        }
        return when (status) {
            is GpsStatus.Fix -> status.location.point
            is GpsStatus.LowAccuracy -> status.location.point
            else -> null
        }
    }
}
