package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.Transport
import com.xanderjs.haptimap.core.navigation.NavigationEvent as E
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.network.FakeConnectivityMonitor
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveActiveRouteTest {

    private val prado = Place("prado", "Museo del Prado", GeoPoint(40.4138, -3.6921), null, null, 1000)
    private val route = Route(prado, 1089, Duration.ZERO, emptyList(), emptyList())
    private val store = NavigationSessionStore()
    private val connectivity = FakeConnectivityMonitor()

    @Test
    fun `going offline and back online during navigation keeps the route and flips offline`() = runTest(UnconfinedTestDispatcher()) {
        val seen = mutableListOf<ActiveRoute?>()
        backgroundScope.launch { ObserveActiveRoute(store, connectivity)().collect { seen += it } }

        listOf(E.StartSearch, E.SelectDestination(prado), E.RequestRoute, E.RouteLoaded(route), E.StartNavigation).forEach(store::dispatch)
        connectivity.status.value = ConnectivityStatus.Disconnected
        assertEquals(NavigationSession.NavigationActive(route), store.state.value)
        connectivity.status.value = ConnectivityStatus.Connected(Transport.BLUETOOTH)
        store.dispatch(E.Cancel)

        // RouteReady → NavigationActive no cambia la ruta ni la red: no se emite de nuevo
        assertEquals(listOf(null, ActiveRoute(route, false), ActiveRoute(route, true), ActiveRoute(route, false), null), seen)
    }

    @Test
    fun `offline without an active route is no active route`() = runTest(UnconfinedTestDispatcher()) {
        connectivity.status.value = ConnectivityStatus.Disconnected
        val seen = mutableListOf<ActiveRoute?>()
        backgroundScope.launch { ObserveActiveRoute(store, connectivity)().collect { seen += it } }
        store.dispatch(E.StartSearch)
        assertEquals(listOf<ActiveRoute?>(null), seen)
    }
}
