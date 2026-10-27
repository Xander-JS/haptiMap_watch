package com.xanderjs.haptimap.presentation.debug

import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Transport
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.Route
import com.xanderjs.haptimap.core.navigation.SessionError
import com.xanderjs.haptimap.core.network.FakeConnectivityMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDebugViewModelTest {

    private val destination = Place("sol", "Sol", GeoPoint(40.4168, -3.7038), null, null, 0)
    private val store = NavigationSessionStore()
    private val connectivity = FakeConnectivityMonitor()
    private lateinit var viewModel: SessionDebugViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = SessionDebugViewModel(store, connectivity)
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
    fun `losing connection during navigation keeps the session`() {
        viewModel.onEvent(NavigationEvent.RequestRoute)
        viewModel.onEvent(NavigationEvent.RouteLoaded(Route(destination)))
        connectivity.status.value = ConnectivityStatus.Disconnected
        connectivity.status.value = ConnectivityStatus.Connected(Transport.BLUETOOTH)
        assertEquals(NavigationSession.NavigationActive(Route(destination)), store.state.value)
    }
}
