package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.location.FakeLocationSource
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.RouteResult
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.SessionError
import com.xanderjs.haptimap.core.network.FakeConnectivityMonitor
import com.xanderjs.haptimap.core.network.FakeRoutesRepository
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteSummaryViewModelTest {

    private val sol = GeoPoint(40.4168, -3.7038)
    private val prado = Place("prado", "Museo del Prado", GeoPoint(40.4138, -3.6921), null, null, 1000)
    private val route = Route(prado, 1089, 14.minutes, emptyList(), emptyList())
    private val store = NavigationSessionStore()
    private val repository = FakeRoutesRepository(RouteResult.Success(route))
    private val connectivity = FakeConnectivityMonitor()

    private fun viewModel(location: LocationSource = FakeLocationSource(LocationFix(sol, 5f, null, null, 0L))) =
        RouteSummaryViewModel(repository, location, store, connectivity)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        store.dispatch(NavigationEvent.StartSearch)
        store.dispatch(NavigationEvent.SelectDestination(prado))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads the route from the current position and ends in RouteReady`() {
        val viewModel = viewModel()
        assertEquals(RouteSummaryUiState(status = null, route = route), viewModel.uiState.value)
        assertEquals(NavigationSession.RouteReady(route), store.state.value)
        assertEquals(sol, repository.lastOrigin)
    }

    @Test
    fun `while the request is pending the session is RouteLoading and the screen shows loading`() {
        // sin posición todavía: la petición espera al GPS
        val noFix = object : LocationSource {
            override fun gpsStatus(intervalMillis: Long) = flow { emit(GpsStatus.Searching); awaitCancellation() }
        }
        val viewModel = viewModel(noFix)
        assertEquals(RouteSummaryUiState(RouteStatus.Loading), viewModel.uiState.value)
        assertEquals(NavigationSession.RouteLoading(prado), store.state.value)
        assertEquals(0, repository.requests)
    }

    @Test
    fun `a failed route goes back to DestinationSelected and shows the detailed error`() {
        repository.result = RouteResult.Failure(RouteError.NO_ROUTE)
        val viewModel = viewModel()
        assertEquals(RouteSummaryUiState(RouteStatus.Failed(RouteError.NO_ROUTE)), viewModel.uiState.value)
        assertEquals(NavigationSession.DestinationSelected(prado, SessionError.ROUTE_UNAVAILABLE), store.state.value)
    }

    @Test
    fun `connection lost during the request is NO_CONNECTION in the session too`() {
        repository.result = RouteResult.Failure(RouteError.NO_CONNECTION)
        viewModel()
        assertEquals(NavigationSession.DestinationSelected(prado, SessionError.NO_CONNECTION), store.state.value)
    }

    @Test
    fun `offline the request is rejected without RouteLoading or a repository call`() {
        connectivity.status.value = ConnectivityStatus.Disconnected
        val viewModel = viewModel()
        assertEquals(RouteSummaryUiState(RouteStatus.Failed(RouteError.NO_CONNECTION)), viewModel.uiState.value)
        assertEquals(NavigationSession.DestinationSelected(prado, SessionError.NO_CONNECTION), store.state.value)
        assertEquals(0, repository.requests)
    }

    @Test
    fun `without a key the error shows at once, without waiting for GPS or calling the repository`() {
        repository.hasApiKey = false
        // sin fix nunca: si esperara al GPS, seguiría en Loading
        val noFix = object : LocationSource {
            override fun gpsStatus(intervalMillis: Long) = flow { emit(GpsStatus.Searching); awaitCancellation() }
        }
        val viewModel = viewModel(noFix)
        assertEquals(RouteSummaryUiState(RouteStatus.Failed(RouteError.MISSING_KEY)), viewModel.uiState.value)
        assertEquals(NavigationSession.DestinationSelected(prado, SessionError.ROUTE_UNAVAILABLE), store.state.value)
        assertEquals(0, repository.requests)
    }

    @Test
    fun `retry after a failure loads the route`() {
        repository.result = RouteResult.Failure(RouteError.API_ERROR)
        val viewModel = viewModel()
        repository.result = RouteResult.Success(route)
        viewModel.loadRoute()
        assertEquals(NavigationSession.RouteReady(route), store.state.value)
        assertEquals(route, viewModel.uiState.value.route)
        assertEquals(2, repository.requests)
    }

    @Test
    fun `an already ready route is shown without a new request`() {
        viewModel()
        val again = viewModel()
        assertEquals(route, again.uiState.value.route)
        assertEquals(1, repository.requests)
    }

    @Test
    fun `start moves the ready route to NavigationActive`() {
        viewModel().start()
        assertEquals(NavigationSession.NavigationActive(route), store.state.value)
    }
}
