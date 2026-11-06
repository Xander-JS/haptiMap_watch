package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.Maneuver.CONTINUE
import com.xanderjs.haptimap.core.navigation.NavigationEvent as E
import com.xanderjs.haptimap.core.navigation.NavigationSession as S
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticTokens
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Paso 10: cuándo corre el servicio en primer plano, con la sesión real y tiempo virtual
@OptIn(ExperimentalCoroutinesApi::class)
class NavigationServicePolicyTest {

    private val store = NavigationSessionStore()
    private val route = routeOf(Leg(CONTINUE, 90.0, 200.0))
    private val other = routeOf(Leg(CONTINUE, 0.0, 150.0))

    /** Cada cambio del servicio: en qué segundo y si corre. */
    private val changes = mutableListOf<Pair<Long, Boolean>>()

    private fun TestScope.observe() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            navigationServiceRuns(store.state).collect { changes += currentTime / 1000 to it }
        }
    }

    private fun startNavigation() {
        store.dispatch(E.RestoreRoute(route))
        store.dispatch(E.StartNavigation)
    }

    @Test
    fun `idle, search and a ready route don't run it`() = runTest {
        observe()
        store.dispatch(E.StartSearch)
        store.dispatch(E.SelectDestination(route.destination))
        store.dispatch(E.RequestRoute)
        store.dispatch(E.RouteLoaded(route))
        assertEquals(S.RouteReady(route), store.state.value)
        assertEquals(listOf(0L to false), changes)
    }

    @Test
    fun `a restored route stays off until the user starts it`() = runTest {
        observe()
        store.dispatch(E.RestoreRoute(route))
        advanceTimeBy(600.seconds)
        assertEquals(listOf(0L to false), changes)
        store.dispatch(E.StartNavigation)
        assertEquals(listOf(0L to false, 600L to true), changes)
    }

    @Test
    fun `off route, recalculating, failure and a new route keep it running without restarts`() = runTest {
        observe()
        startNavigation()
        store.dispatch(E.LeaveRoute)
        store.dispatch(E.Recalculate)
        store.dispatch(E.RecalculationFailed(SessionError.NO_CONNECTION))
        store.dispatch(E.ReturnToRoute)
        store.dispatch(E.LeaveRoute)
        store.dispatch(E.Recalculate)
        store.dispatch(E.RouteRecalculated(other))
        assertEquals(S.NavigationActive(other), store.state.value)
        assertEquals(listOf(0L to false, 0L to true), changes)
    }

    @Test
    fun `arrival keeps it for the linger, then stops it`() = runTest {
        observe()
        startNavigation()
        advanceTimeBy(100.seconds)
        store.dispatch(E.Arrive)
        advanceTimeBy(ARRIVED_LINGER - 1.seconds)
        assertEquals(listOf(0L to false, 0L to true), changes)
        advanceTimeBy(2.seconds)
        assertEquals(listOf(0L to false, 0L to true, 160L to false), changes)
        assertEquals(S.Arrived(route), store.state.value)
    }

    @Test
    fun `finish during the linger stops it at once, and nothing fires later`() = runTest {
        observe()
        startNavigation()
        store.dispatch(E.Arrive)
        advanceTimeBy(10.seconds)
        store.dispatch(E.Finish)
        advanceTimeBy(120.seconds)
        assertEquals(listOf(0L to false, 0L to true, 10L to false), changes)
    }

    @Test
    fun `cancel stops it from every running state`() = runTest {
        val running = listOf<() -> Unit>(
            {},
            { store.dispatch(E.LeaveRoute) },
            { store.dispatch(E.LeaveRoute); store.dispatch(E.Recalculate) },
            { store.dispatch(E.Arrive) },
        )
        observe()
        running.forEach { reach ->
            changes.clear()
            startNavigation()
            reach()
            store.dispatch(E.Cancel)
            assertEquals(S.Idle, store.state.value)
            assertEquals(listOf(currentTime / 1000 to true, currentTime / 1000 to false), changes)
            advanceTimeBy(ARRIVED_LINGER * 2)
        }
    }

    // paso 15A: el bloqueo de CPU usa la misma política con una espera corta tras llegar
    @Test
    fun `the wake lock is released a few seconds after arrival, while the service lingers`() = runTest {
        val lock = mutableListOf<Pair<Long, Boolean>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            navigationServiceRuns(store.state, ARRIVED_WAKE_LOCK_LINGER).collect { lock += currentTime / 1000 to it }
        }
        observe()
        startNavigation()
        advanceTimeBy(30.seconds)
        store.dispatch(E.Arrive)
        advanceTimeBy(ARRIVED_LINGER * 2)
        assertEquals(listOf(0L to false, 0L to true, 35L to false), lock)
        assertEquals(listOf(0L to false, 0L to true, 90L to false), changes)
    }

    @Test
    fun `the arrival pattern starts and ends well inside the wake lock linger`() {
        val pattern = HapticTokens.patternFor(HapticEvent.ARRIVAL).totalDurationMs.milliseconds
        // más la ventana de fusión del motor (100 ms) y margen para un tic del planificador
        assertTrue(pattern + 1.seconds < ARRIVED_WAKE_LOCK_LINGER)
    }
}
