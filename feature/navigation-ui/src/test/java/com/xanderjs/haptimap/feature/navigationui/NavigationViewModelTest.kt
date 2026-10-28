package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.Transport
import com.xanderjs.haptimap.core.model.offset
import com.xanderjs.haptimap.core.navigation.NavigationEngine
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.network.FakeConnectivityMonitor
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationViewModelTest {

    private val start = GeoPoint(40.4168, -3.7038)
    private val corner = start.offset(90.0, 60.0)
    private val end = corner.offset(0.0, 50.0)
    private val prado = Place("prado", "Museo del Prado", end, null, null, 110)

    // 60 m al este, giro a la izquierda y 50 m al norte
    private val route = Route(
        destination = prado,
        distanceMeters = 110,
        duration = 80.seconds,
        points = listOf(start, corner, end),
        steps = listOf(
            RouteStep(Maneuver.CONTINUE, 60, 40.seconds, listOf(start, corner), start, corner, "Head east", null),
            RouteStep(Maneuver.TURN_LEFT, 50, 40.seconds, listOf(corner, end), corner, end, "Turn left onto Calle Mayor", null),
            RouteStep(Maneuver.ARRIVAL, 0, 0.seconds, listOf(end), end, end, null, null),
        ),
    )

    private val store = NavigationSessionStore()
    private val engine = NavigationEngine(store)
    private val connectivity = FakeConnectivityMonitor()
    private val fixes = MutableSharedFlow<LocationFix>()

    private fun TestScope.navigating(): NavigationViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.follow(fixes) }
        store.dispatch(NavigationEvent.RestoreRoute(route))
        store.dispatch(NavigationEvent.StartNavigation)
        return NavigationViewModel(store, engine, ObserveActiveRoute(store, connectivity))
    }

    private val NavigationViewModel.navigatingState get() = uiState.value as NavigationUiState.Navigating

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `at the start it shows the first turn, its instruction and the whole route`() = runTest {
        val guidance = navigating().navigatingState.guidance
        assertEquals(GuidanceUi(ArrowDirection.LEFT, 60, "Turn left onto Calle Mayor", 110, 80.seconds), guidance)
    }

    @Test
    fun `maps the engine progress to distances and remaining time`() = runTest {
        val viewModel = navigating()
        fixes.emit(LocationFix(start.offset(90.0, 20.0), 5f, null, null, 0))
        val guidance = viewModel.navigatingState.guidance
        assertEquals(40, guidance.distanceMeters)
        assertEquals(90, guidance.remainingMeters)
        assertEquals(ArrowDirection.LEFT, guidance.direction)
        // dos tercios del primer paso (40 s) más el segundo (40 s)
        assertEquals(66.67, guidance.remainingTime.inWholeMilliseconds / 1000.0, 0.1)
    }

    @Test
    fun `after the turn the arrival shows the destination name`() = runTest {
        val viewModel = navigating()
        fixes.emit(LocationFix(corner.offset(0.0, 10.0), 5f, null, null, 0))
        val guidance = viewModel.navigatingState.guidance
        assertEquals(ArrowDirection.ARRIVE, guidance.direction)
        assertEquals("Museo del Prado", guidance.instruction)
        assertEquals(40, guidance.distanceMeters)
    }

    @Test
    fun `offline shows the offline notice and keeps navigating`() = runTest {
        val viewModel = navigating()
        assertNull(viewModel.navigatingState.notice)
        connectivity.status.value = ConnectivityStatus.Disconnected
        assertEquals(NavigationNotice.OFFLINE, viewModel.navigatingState.notice)
        assertTrue(store.state.value is NavigationSession.NavigationActive)
        connectivity.status.value = ConnectivityStatus.Connected(Transport.WIFI)
        assertNull(viewModel.navigatingState.notice)
    }

    @Test
    fun `a weak signal wins over offline and clears with the next good fix`() = runTest {
        val viewModel = navigating()
        connectivity.status.value = ConnectivityStatus.Disconnected
        fixes.emit(LocationFix(start, 5f, null, null, 0))
        fixes.emit(LocationFix(start, 40f, null, null, 10_000))
        assertEquals(NavigationNotice.WEAK_GPS, viewModel.navigatingState.notice)
        fixes.emit(LocationFix(start, 5f, null, null, 11_000))
        assertEquals(NavigationNotice.OFFLINE, viewModel.navigatingState.notice)
    }

    @Test
    fun `stop asks for confirmation, keep going keeps navigating`() = runTest {
        val viewModel = navigating()
        viewModel.requestStop()
        assertTrue(viewModel.navigatingState.confirmingStop)
        viewModel.dismissStop()
        assertFalse(viewModel.navigatingState.confirmingStop)
        assertTrue(store.state.value is NavigationSession.NavigationActive)
    }

    @Test
    fun `confirming stop cancels the session and ends the screen`() = runTest {
        val viewModel = navigating()
        viewModel.requestStop()
        viewModel.confirmStop()
        assertEquals(NavigationSession.Idle, store.state.value)
        assertEquals(NavigationUiState.Ended, viewModel.uiState.value)
    }

    @Test
    fun `arriving shows the arrival screen and new search completes the session`() = runTest {
        val viewModel = navigating()
        fixes.emit(LocationFix(end, 5f, null, null, 0))
        assertEquals(NavigationSession.Arrived(route), store.state.value)
        assertEquals(NavigationUiState.Arrived("Museo del Prado"), viewModel.uiState.value)
        viewModel.finish()
        assertEquals(NavigationSession.Completed(route), store.state.value)
        assertEquals(NavigationUiState.Arrived("Museo del Prado"), viewModel.uiState.value)
    }
}
