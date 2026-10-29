package com.xanderjs.haptimap.feature.search

import com.xanderjs.haptimap.core.location.FakeLocationSource
import com.xanderjs.haptimap.core.location.LOCATION_FIX_TIMEOUT_MILLIS
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.core.model.PlaceSearchResult
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.Transport
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.network.FakeConnectivityMonitor
import com.xanderjs.haptimap.core.network.FakePlacesRepository
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val sol = GeoPoint(40.4168, -3.7038)
    private val pharmacy = Place("1", "Farmacia", sol, PlaceCategory.PHARMACY, "Calle Mayor, 1", 120)
    private val museum = Place("2", "Museo", sol, null, null, 900)
    private val route = Route(museum, 900, 12.minutes, emptyList(), emptyList())
    private val store = NavigationSessionStore()
    private val repository = FakePlacesRepository(PlaceSearchResult.Success(listOf(pharmacy)))
    private val connectivity = FakeConnectivityMonitor()

    private fun viewModel(location: LocationSource = FakeLocationSource(LocationFix(sol, 5f, null, null, 0L))) =
        SearchViewModel(repository, location, store, connectivity)

    private val scheduler = TestCoroutineScheduler()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun navigating() {
        store.dispatch(NavigationEvent.RestoreRoute(route))
        store.dispatch(NavigationEvent.StartNavigation)
    }

    @Test
    fun `category search shows the places and starts the search session`() {
        val viewModel = viewModel()
        viewModel.searchCategory(PlaceCategory.PHARMACY)
        val state = viewModel.uiState.value
        assertEquals(SearchQuery.Category(PlaceCategory.PHARMACY), state.query)
        assertEquals(listOf(pharmacy), state.places)
        assertNull(state.status)
        assertEquals(PlaceCategory.PHARMACY, repository.lastQuery)
        assertEquals(sol, repository.lastLocation)
        assertEquals(NavigationSession.Searching, store.state.value)
    }

    @Test
    fun `voice text is trimmed and searched as text`() {
        val viewModel = viewModel()
        viewModel.searchText("  farmacia de guardia ")
        assertEquals(SearchQuery.Text("farmacia de guardia"), viewModel.uiState.value.query)
        assertEquals("farmacia de guardia", repository.lastQuery)
    }

    @Test
    fun `blank voice text is not searched`() {
        val viewModel = viewModel()
        viewModel.searchText("  ")
        assertNull(repository.lastQuery)
        assertNull(viewModel.uiState.value.query)
    }

    @Test
    fun `empty results and Places errors become the status`() {
        val viewModel = viewModel()
        for (error in PlaceSearchError.entries.filter { it != PlaceSearchError.NO_CONNECTION }) {
            repository.result = PlaceSearchResult.Failure(error)
            viewModel.searchText("cafe")
            assertEquals(SearchStatus.Failed(error), viewModel.uiState.value.status)
            assertEquals(emptyList<Place>(), viewModel.uiState.value.places)
        }
    }

    @Test
    fun `retry repeats the last query`() {
        repository.result = PlaceSearchResult.Failure(PlaceSearchError.API_QUOTA)
        val viewModel = viewModel()
        viewModel.searchCategory(PlaceCategory.CAFE)
        repository.result = PlaceSearchResult.Success(listOf(pharmacy))
        viewModel.retry()
        assertEquals(SearchQuery.Category(PlaceCategory.CAFE), viewModel.uiState.value.query)
        assertEquals(listOf(pharmacy), viewModel.uiState.value.places)
        assertNull(viewModel.uiState.value.status)
    }

    @Test
    fun `only errors a retry can fix offer it`() {
        val retryable = PlaceSearchError.entries.filter { SearchStatus.Failed(it).canRetry }
        assertEquals(listOf(PlaceSearchError.NO_CONNECTION, PlaceSearchError.NO_LOCATION, PlaceSearchError.API_QUOTA, PlaceSearchError.API_ERROR), retryable)
        assertFalse(SearchStatus.NoPermission.canRetry)
        assertFalse(SearchStatus.Loading.canRetry)
    }

    @Test
    fun `without location permission the search says so without calling Places`() {
        val viewModel = viewModel(FakeLocationSource(hasPermission = false))
        viewModel.searchCategory(PlaceCategory.CAFE)
        assertEquals(SearchStatus.NoPermission, viewModel.uiState.value.status)
        assertNull(repository.lastQuery)
    }

    @Test
    fun `location off searches without location and Places reports it`() {
        repository.result = PlaceSearchResult.Failure(PlaceSearchError.NO_LOCATION)
        val viewModel = viewModel(FakeLocationSource(locationEnabled = false))
        viewModel.searchCategory(PlaceCategory.CAFE)
        assertNull(repository.lastLocation)
        assertEquals(SearchStatus.Failed(PlaceSearchError.NO_LOCATION), viewModel.uiState.value.status)
    }

    @Test
    fun `offline is shown at once and a search fails without waiting for location or calling Places`() {
        connectivity.status.value = ConnectivityStatus.Disconnected
        // sin fixes: si esperara al GPS, la búsqueda seguiría en Loading
        val viewModel = viewModel(FakeLocationSource())
        assertTrue(viewModel.uiState.value.offline)
        viewModel.searchCategory(PlaceCategory.CAFE)
        assertEquals(SearchStatus.Failed(PlaceSearchError.NO_CONNECTION), viewModel.uiState.value.status)
        assertNull(repository.lastQuery)
        connectivity.status.value = ConnectivityStatus.Connected(Transport.WIFI)
        assertFalse(viewModel.uiState.value.offline)
    }

    @Test
    fun `GPS on but no fix searches without location once the timeout passes`() {
        // como el GPS real: Searching y nunca termina
        val noFix = object : LocationSource {
            override fun gpsStatus(intervalMillis: Long) = flow { emit(GpsStatus.Searching); awaitCancellation() }
        }
        val viewModel = viewModel(noFix)
        viewModel.searchCategory(PlaceCategory.CAFE)
        scheduler.advanceTimeBy(LOCATION_FIX_TIMEOUT_MILLIS - 1)
        assertEquals(SearchStatus.Loading, viewModel.uiState.value.status)
        assertNull(repository.lastQuery)
        scheduler.advanceTimeBy(2)
        assertEquals(PlaceCategory.CAFE, repository.lastQuery)
        assertNull(repository.lastLocation)
    }

    @Test
    fun `selecting a place with no active route makes it the destination`() {
        val viewModel = viewModel()
        viewModel.searchCategory(PlaceCategory.PHARMACY)
        assertTrue(viewModel.select(pharmacy))
        assertEquals(NavigationSession.DestinationSelected(pharmacy), store.state.value)
    }

    @Test
    fun `choosing another place after a route request replaces the destination without asking`() {
        val viewModel = viewModel()
        viewModel.select(pharmacy)
        store.dispatch(NavigationEvent.RequestRoute)
        assertTrue(viewModel.select(museum))
        assertEquals(NavigationSession.DestinationSelected(museum), store.state.value)
    }

    @Test
    fun `after arriving a new destination starts a new session`() {
        navigating()
        store.dispatch(NavigationEvent.Arrive)
        val viewModel = viewModel()
        assertNull(viewModel.uiState.value.activeRoute)
        assertTrue(viewModel.select(pharmacy))
        assertEquals(NavigationSession.DestinationSelected(pharmacy), store.state.value)
    }

    @Test
    fun `an active navigation is shown and searching does not touch it`() {
        navigating()
        val viewModel = viewModel()
        assertEquals(ActiveRouteUi("Museo", navigating = true), viewModel.uiState.value.activeRoute)
        viewModel.searchCategory(PlaceCategory.PHARMACY)
        assertEquals(listOf(pharmacy), viewModel.uiState.value.places)
        assertEquals(NavigationSession.NavigationActive(route), store.state.value)
    }

    @Test
    fun `a loaded route that has not started is shown as ready`() {
        store.dispatch(NavigationEvent.RestoreRoute(route))
        assertEquals(ActiveRouteUi("Museo", navigating = false), viewModel().uiState.value.activeRoute)
    }

    @Test
    fun `selecting a place during navigation asks first and keeping the route changes nothing`() {
        navigating()
        val viewModel = viewModel()
        assertFalse(viewModel.select(pharmacy))
        assertEquals(pharmacy, viewModel.uiState.value.pendingReplace)
        assertEquals(NavigationSession.NavigationActive(route), store.state.value)
        viewModel.keepRoute()
        assertNull(viewModel.uiState.value.pendingReplace)
        assertEquals(NavigationSession.NavigationActive(route), store.state.value)
    }

    @Test
    fun `confirming the replacement cancels the route and selects the new destination`() {
        navigating()
        store.dispatch(NavigationEvent.LeaveRoute)
        val viewModel = viewModel()
        viewModel.select(pharmacy)
        assertTrue(viewModel.confirmReplace())
        assertNull(viewModel.uiState.value.pendingReplace)
        assertNull(viewModel.uiState.value.activeRoute)
        assertEquals(NavigationSession.DestinationSelected(pharmacy), store.state.value)
        assertFalse(viewModel.confirmReplace())
    }

    @Test
    fun `voice unavailable is shown until the next search`() {
        val viewModel = viewModel()
        viewModel.voiceUnavailable()
        assertTrue(viewModel.uiState.value.voiceUnavailable)
        viewModel.searchCategory(PlaceCategory.CAFE)
        assertFalse(viewModel.uiState.value.voiceUnavailable)
    }
}
