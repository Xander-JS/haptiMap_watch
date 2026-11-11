package com.xanderjs.haptimap.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.location.firstPoint
import com.xanderjs.haptimap.core.model.ConnectivityPolicy
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.core.model.PlaceSearchResult
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.activeRoute
import com.xanderjs.haptimap.core.navigation.navigatingRoute
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import com.xanderjs.haptimap.core.network.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que se buscó: una categoría rápida o el texto dictado. */
sealed interface SearchQuery {
    data class Category(val category: PlaceCategory) : SearchQuery
    data class Text(val text: String) : SearchQuery
}

/** La ruta que ya hay (cargada o en curso): la búsqueda la ofrece y no la sustituye sin preguntar. */
data class ActiveRouteUi(val destinationName: String, val navigating: Boolean)

/**
 * [query] null = aún no se ha buscado. [status] null con [query] = se muestran [places].
 * [detailPage]: el resultado que abre el detalle (la lista lo fija al tocar un lugar).
 * [pendingReplace]: el lugar elegido espera a que el usuario confirme que sustituye la ruta activa.
 */
data class SearchUiState(
    val query: SearchQuery? = null,
    val places: List<Place> = emptyList(),
    val status: SearchStatus? = null,
    val offline: Boolean = false,
    val activeRoute: ActiveRouteUi? = null,
    val pendingReplace: Place? = null,
    val detailPage: Int = 0,
    val voiceUnavailable: Boolean = false,
)

/**
 * Modo Búsqueda (sección 7): categorías y voz contra Places, y la elección del destino. Buscar nunca toca
 * una ruta activa; elegir un lugar con una ruta activa pide confirmación antes de cancelarla.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: PlacesRepository,
    private val locationSource: LocationSource,
    private val store: NavigationSessionStore,
    private val connectivity: ConnectivityMonitor,
) : ViewModel() {
    private val search = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = combine(search, store.state, connectivity.status) { state, session, network ->
        state.copy(offline = network == ConnectivityStatus.Disconnected, activeRoute = session.toActiveRouteUi())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, search.value)
    private var searchJob: Job? = null

    fun searchCategory(category: PlaceCategory) = search(SearchQuery.Category(category))

    /** El texto dictado; vacío (el reconocedor no entendió nada) no busca. */
    fun searchText(text: String) {
        if (text.isNotBlank()) search(SearchQuery.Text(text.trim()))
    }

    fun retry() {
        search.value.query?.let(::search)
    }

    /** No hay reconocedor de voz en el reloj: se avisa en la pantalla de búsqueda. */
    fun voiceUnavailable() = search.update { it.copy(voiceUnavailable = true) }

    /** Tocar un resultado abre su detalle; elegirlo como destino es [select], desde el detalle. */
    fun openDetail(index: Int) = search.update { it.copy(detailPage = index) }

    /**
     * Elige el destino. true = la sesión está en DestinationSelected y se puede pedir la ruta; false = hay una
     * ruta activa y queda en [SearchUiState.pendingReplace] hasta [confirmReplace] o [keepRoute].
     */
    fun select(place: Place): Boolean {
        if (store.state.value.activeRoute != null) {
            search.update { it.copy(pendingReplace = place) }
            return false
        }
        selectDestination(place)
        return true
    }

    /** Sustituye la ruta activa por el lugar pendiente. true si había uno. */
    fun confirmReplace(): Boolean {
        val place = search.value.pendingReplace ?: return false
        search.update { it.copy(pendingReplace = null) }
        selectDestination(place)
        return true
    }

    fun keepRoute() = search.update { it.copy(pendingReplace = null) }

    private fun search(query: SearchQuery) {
        // buscar es Idle → Searching; con una ruta activa la sesión no cambia hasta elegir y confirmar
        if (store.state.value == NavigationSession.Idle) store.dispatch(NavigationEvent.StartSearch)
        // una búsqueda nueva sustituye a la anterior: no llegan resultados viejos tarde
        searchJob?.cancel()
        search.update { it.copy(query = query, places = emptyList(), status = SearchStatus.Loading, voiceUnavailable = false) }
        searchJob = viewModelScope.launch {
            val status = when {
                // sin red se avisa ya, sin esperar al GPS; el repositorio vuelve a comprobarlo antes de pedir
                !ConnectivityPolicy.canSearch(connectivity.status.value) -> SearchStatus.Failed(PlaceSearchError.NO_CONNECTION)
                // el primer estado del GPS llega al momento: sin permiso no tiene sentido esperar 10 s a un fix
                locationSource.gpsStatus().first() == GpsStatus.NoPermission -> SearchStatus.NoPermission
                else -> when (val result = query.run(locationSource.firstPoint())) {
                    is PlaceSearchResult.Success -> {
                        search.update { it.copy(places = result.places, status = null) }
                        return@launch
                    }
                    is PlaceSearchResult.Failure -> SearchStatus.Failed(result.error)
                }
            }
            search.update { it.copy(status = status) }
        }
    }

    private suspend fun SearchQuery.run(location: GeoPoint?) = when (this) {
        is SearchQuery.Category -> repository.searchByCategory(category, location)
        is SearchQuery.Text -> repository.searchByText(text, location)
    }

    // sin ruta activa: lo que quede de otra elección (DestinationSelected, RouteLoading) o de una llegada se cancela
    private fun selectDestination(place: Place) {
        val session = store.state.value
        if (session != NavigationSession.Idle && session != NavigationSession.Searching) store.dispatch(NavigationEvent.Cancel)
        if (store.state.value == NavigationSession.Idle) store.dispatch(NavigationEvent.StartSearch)
        store.dispatch(NavigationEvent.SelectDestination(place))
    }
}

private fun NavigationSession.toActiveRouteUi() = activeRoute?.let { ActiveRouteUi(it.destination.name, navigating = navigatingRoute != null) }
