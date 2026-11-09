package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.intelligence.RouteEvaluationConfig
import com.xanderjs.haptimap.core.intelligence.RouteEvaluator
import com.xanderjs.haptimap.core.location.FakeLocationSource
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.RouteResult
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.Transport
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.SessionError
import com.xanderjs.haptimap.core.network.FakeConnectivityMonitor
import com.xanderjs.haptimap.core.network.FakeRoutesRepository
import com.xanderjs.haptimap.core.network.parseRoutesResponse
import java.io.File
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private fun viewModel(
        location: LocationSource = FakeLocationSource(LocationFix(sol, 5f, null, null, 0L)),
        evaluator: RouteEvaluator = RouteEvaluator(),
        debug: Boolean = false,
    ) = RouteSummaryViewModel(repository, location, store, connectivity, ObserveActiveRoute(store, connectivity), evaluator, debug)

    private fun loadRecordedFixture(name: String): List<Route> {
        val file = sequenceOf(
            File("../../core/network/src/test/resources/routes/$name"),
            File("../core/network/src/test/resources/routes/$name"),
            File("core/network/src/test/resources/routes/$name"),
        ).first { it.exists() }
        return parseRoutesResponse(file.readText(), prado, sol)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        store.dispatch(NavigationEvent.StartSearch)
        store.dispatch(NavigationEvent.SelectDestination(prado))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads the route from the current position with alternatives and ends in RouteReady`() {
        val viewModel = viewModel()
        assertEquals(RouteSummaryUiState(status = null, route = route), viewModel.uiState.value)
        assertEquals(NavigationSession.RouteReady(route), store.state.value)
        assertEquals(sol, repository.lastOrigin)
        assertTrue(repository.lastAlternatives)
    }

    @Test
    fun `while the request is pending the session is RouteLoading and the screen shows loading`() {
        // sin posición todavía: la petición espera al GPS
        val noFix = object : LocationSource {
            override fun gpsStatus(sampling: com.xanderjs.haptimap.core.location.LocationSampling) = flow { emit(GpsStatus.Searching); awaitCancellation() }
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
            override fun gpsStatus(sampling: com.xanderjs.haptimap.core.location.LocationSampling) = flow { emit(GpsStatus.Searching); awaitCancellation() }
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
    fun `retrying while offline fails at once without a request`() {
        repository.result = RouteResult.Failure(RouteError.API_ERROR)
        val viewModel = viewModel()
        connectivity.status.value = ConnectivityStatus.Disconnected
        viewModel.loadRoute()
        assertEquals(RouteSummaryUiState(RouteStatus.Failed(RouteError.NO_CONNECTION)), viewModel.uiState.value)
        assertEquals(1, repository.requests)
    }

    @Test
    fun `losing the connection with a ready route keeps it and shows the offline notice`() {
        val viewModel = viewModel()
        connectivity.status.value = ConnectivityStatus.Disconnected
        assertEquals(RouteSummaryUiState(status = null, route = route, offline = true), viewModel.uiState.value)
        assertEquals(NavigationSession.RouteReady(route), store.state.value)
        connectivity.status.value = ConnectivityStatus.Connected(Transport.WIFI)
        assertEquals(RouteSummaryUiState(status = null, route = route, offline = false), viewModel.uiState.value)
    }

    @Test
    fun `an error while offline is not shown as the offline notice`() {
        connectivity.status.value = ConnectivityStatus.Disconnected
        assertEquals(false, viewModel().uiState.value.offline)
    }

    @Test
    fun `start moves the ready route to NavigationActive`() {
        viewModel().start()
        assertEquals(NavigationSession.NavigationActive(route), store.state.value)
    }

    @Test
    fun `single-route real fixture sol_to_prado loads that route into RouteReady`() {
        val candidates = loadRecordedFixture("sol_to_prado.json")
        assertEquals(1, candidates.size)
        repository.result = RouteResult.Success(candidates.first(), candidates)

        val viewModel = viewModel(debug = true)
        assertEquals(1089, viewModel.uiState.value.route?.distanceMeters)
        assertEquals(NavigationSession.RouteReady(candidates.first(), NavigationProfile.FOCUS), store.state.value)
    }

    @Test
    fun `two-route real fixture two_routes discards the +27 percent slower alternative under default cap`() {
        val candidates = loadRecordedFixture("two_routes.json")
        assertEquals(listOf(1086, 1378), candidates.map { it.distanceMeters })
        repository.result = RouteResult.Success(candidates.first(), candidates)
        store.dispatch(NavigationEvent.ChooseProfile)
        store.dispatch(NavigationEvent.SelectProfile(NavigationProfile.TOUR))

        val viewModel = viewModel(debug = true)
        // La 2ª ruta real (1125 s vs 882 s = +27.5%) supera el +25% máximo por defecto y se queda la 1ª (1086 m)
        assertEquals(1086, viewModel.uiState.value.route?.distanceMeters)
        assertEquals(NavigationSession.RouteReady(candidates[0], NavigationProfile.TOUR), store.state.value)
    }

    @Test
    fun `chooses the faster alternative in FOCUS and the calmer alternative in TOUR from the same response`() {
        val base = loadRecordedFixture("two_routes.json")
        val fastComplex = base[0] // 1086 m, 882 s, 9 giros y 4 tramos cortos (< 25 m)
        // Alternativa dentro del margen (+10% tiempo, +8% distancia) y con un único giro suave sin tramos cortos
        val calmTour = Route(
            destination = prado,
            distanceMeters = 1170,
            duration = 970.seconds,
            points = base[1].points,
            steps = listOf(
                RouteStep(Maneuver.CONTINUE, 585, 485.seconds, emptyList(), sol, prado.location, null, "DEPART"),
                RouteStep(Maneuver.TURN_RIGHT, 585, 485.seconds, emptyList(), sol, prado.location, null, "TURN_SLIGHT_RIGHT"),
                RouteStep(Maneuver.ARRIVAL, 0, 0.seconds, emptyList(), prado.location, prado.location, null, null),
            ),
        )
        repository.result = RouteResult.Success(fastComplex, listOf(fastComplex, calmTour))

        // En FOCUS gana la más rápida (1086 m)
        store.dispatch(NavigationEvent.ChooseProfile)
        store.dispatch(NavigationEvent.SelectProfile(NavigationProfile.FOCUS))
        val focusVm = viewModel(debug = true)
        assertEquals(fastComplex, focusVm.uiState.value.route)
        assertEquals(NavigationSession.RouteReady(fastComplex, NavigationProfile.FOCUS), store.state.value)

        // Reiniciamos la selección para TOUR con la misma respuesta de Routes: gana la más tranquila (1170 m)
        store.dispatch(NavigationEvent.Cancel)
        store.dispatch(NavigationEvent.StartSearch)
        store.dispatch(NavigationEvent.SelectDestination(prado))
        store.dispatch(NavigationEvent.ChooseProfile)
        store.dispatch(NavigationEvent.SelectProfile(NavigationProfile.TOUR))
        val tourVm = viewModel(
            evaluator = RouteEvaluator(RouteEvaluationConfig()),
            debug = true,
        )
        assertEquals(calmTour, tourVm.uiState.value.route)
        assertEquals(NavigationSession.RouteReady(calmTour, NavigationProfile.TOUR), store.state.value)
    }
}
