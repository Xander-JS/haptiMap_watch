package com.xanderjs.haptimap.presentation.debug

import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.Transport
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.SessionError
import com.xanderjs.haptimap.core.network.FakeConnectivityMonitor
import com.xanderjs.haptimap.feature.navigationui.ObserveActiveRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDebugViewModelTest {

    private val destination = Place("sol", "Sol", GeoPoint(40.4168, -3.7038), null, null, 0)
    private val route = Route(destination, 0, Duration.ZERO, emptyList(), emptyList())
    private val store = NavigationSessionStore()
    private val connectivity = FakeConnectivityMonitor()
    private lateinit var viewModel: SessionDebugViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = SessionDebugViewModel(store, connectivity, ObserveActiveRoute(store, connectivity))
        viewModel.onEvent(NavigationEvent.StartSearch)
        viewModel.onEvent(NavigationEvent.SelectDestination(destination))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `online, requesting a route loads it`() {
        viewModel.onEvent(NavigationEvent.RequestRoute)
        assertEquals(NavigationSession.RouteLoading(destination), store.state.value)
    }

    @Test
    fun `offline, requesting a route is rejected without loading`() {
        connectivity.status.value = ConnectivityStatus.Disconnected
        viewModel.onEvent(NavigationEvent.RequestRoute)
        assertEquals(NavigationSession.DestinationSelected(destination, SessionError.NO_CONNECTION), store.state.value)
    }

    @Test
    fun `losing connection during navigation keeps the session`() = runTest(UnconfinedTestDispatcher()) {
        // uiState es WhileSubscribed: sin alguien que lo recolecte no se actualiza
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onEvent(NavigationEvent.RequestRoute)
        viewModel.onEvent(NavigationEvent.RouteLoaded(route))
        viewModel.onEvent(NavigationEvent.StartNavigation)
        connectivity.status.value = ConnectivityStatus.Disconnected
        assertEquals(true, viewModel.uiState.value.routeOffline)
        connectivity.status.value = ConnectivityStatus.Connected(Transport.BLUETOOTH)
        assertEquals(NavigationSession.NavigationActive(route), store.state.value)
        assertEquals(false, viewModel.uiState.value.routeOffline)
    }

    @Test
    fun `route events carry the chosen destination and are hidden without one`() {
        assertEquals(listOf(route), viewModel.eventsFor(store.state.value).filterIsInstance<NavigationEvent.RouteLoaded>().map { it.route })
        assertEquals(emptyList<NavigationEvent>(), viewModel.eventsFor(NavigationSession.Searching).filterIsInstance<NavigationEvent.RouteLoaded>())
    }
}
