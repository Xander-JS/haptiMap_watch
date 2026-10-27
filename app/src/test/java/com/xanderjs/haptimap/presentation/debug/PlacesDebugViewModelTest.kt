package com.xanderjs.haptimap.presentation.debug

import com.xanderjs.haptimap.core.location.FakeLocationSource
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.core.model.PlaceSearchResult
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.network.FakeConnectivityMonitor
import com.xanderjs.haptimap.core.network.FakePlacesRepository
import com.xanderjs.haptimap.designsystem.organisms.SearchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlacesDebugViewModelTest {

    private val sol = GeoPoint(40.4168, -3.7038)
    private val pharmacy = Place("1", "Farmacia", sol, PlaceCategory.PHARMACY, null, 120)
    private val store = NavigationSessionStore()
    private val repository = FakePlacesRepository(PlaceSearchResult.Success(listOf(pharmacy)))
    private val connectivity = FakeConnectivityMonitor()

    private fun viewModel(location: FakeLocationSource = FakeLocationSource(LocationFix(sol, 5f, null, null, 0L))) =
        PlacesDebugViewModel(repository, location, store, connectivity)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `category search shows the places and starts the search session`() {
        val viewModel = viewModel()
        viewModel.searchCategory(PlaceCategory.PHARMACY)
        assertEquals(PlacesDebugUiState(PlaceCategory.PHARMACY, listOf(pharmacy), status = null), viewModel.uiState.value)
        assertEquals(PlaceCategory.PHARMACY, repository.lastQuery)
        assertEquals(sol, repository.lastLocation)
        assertEquals(NavigationSession.Searching, store.state.value)
    }

    @Test
    fun `a failed search shows its error`() {
        repository.result = PlaceSearchResult.Failure(PlaceSearchError.API_QUOTA)
        val viewModel = viewModel()
        viewModel.searchText("cafe")
        assertEquals(SearchStatus.Failed(PlaceSearchError.API_QUOTA), viewModel.uiState.value.status)
        assertEquals("cafe", repository.lastQuery)
    }

    @Test
    fun `without location permission the search gets no location`() {
        viewModel(FakeLocationSource(hasPermission = false)).searchCategory(PlaceCategory.CAFE)
        assertNull(repository.lastLocation)
    }

    @Test
    fun `offline shows no connection without waiting for location or calling the repository`() {
        connectivity.status.value = ConnectivityStatus.Disconnected
        // sin fixes: si esperara al GPS, la búsqueda seguiría en Loading
        val viewModel = viewModel(FakeLocationSource())
        viewModel.searchCategory(PlaceCategory.CAFE)
        assertEquals(SearchStatus.Failed(PlaceSearchError.NO_CONNECTION), viewModel.uiState.value.status)
        assertNull(repository.lastQuery)
    }

    @Test
    fun `blank text is not searched`() {
        viewModel().searchText("  ")
        assertNull(repository.lastQuery)
    }

    @Test
    fun `selecting a place makes it the session destination`() {
        val viewModel = viewModel()
        viewModel.searchCategory(PlaceCategory.PHARMACY)
        viewModel.selectPlace(pharmacy)
        assertEquals(NavigationSession.DestinationSelected(pharmacy), store.state.value)
    }

    @Test
    fun `searching again after choosing restarts the search`() {
        val viewModel = viewModel()
        viewModel.searchCategory(PlaceCategory.PHARMACY)
        viewModel.selectPlace(pharmacy)
        store.dispatch(NavigationEvent.RequestRoute)
        viewModel.searchCategory(PlaceCategory.CAFE)
        assertEquals(NavigationSession.Searching, store.state.value)
    }
}
