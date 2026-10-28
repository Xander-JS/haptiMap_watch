package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.core.model.Maneuver.ARRIVAL
import com.xanderjs.haptimap.core.model.Maneuver.CONTINUE
import com.xanderjs.haptimap.core.navigation.NavigationEvent as E
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationEngineTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val store = NavigationSessionStore()
    private val engine = NavigationEngine(store)
    private val route = routeOf(Leg(CONTINUE, 90.0, 60.0))
    private var collections = 0

    /** Un fix por segundo de tiempo virtual; después, silencio. Cuenta cuántas veces se empieza a recolectar (GPS encendido). */
    private fun timed(fixes: List<LocationFix>): Flow<LocationFix> = flow {
        collections++
        fixes.forEach {
            delay(1.seconds)
            emit(it)
        }
        awaitCancellation()
    }

    private fun startNavigation() {
        store.dispatch(E.RestoreRoute(route))
        store.dispatch(E.StartNavigation)
    }

    private fun TestScope.follow(fixes: Flow<LocationFix>) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.follow(fixes) }

    @Test
    fun `fixes are only collected while navigating`() = runTest {
        follow(timed(walk(route)))
        store.dispatch(E.RestoreRoute(route))
        runCurrent()
        assertEquals(0, collections)
        assertNull(engine.progress.value)
        store.dispatch(E.StartNavigation)
        runCurrent()
        assertEquals(1, collections)
        assertEquals(60.0, engine.progress.value!!.remainingMeters, 0.5)
    }

    @Test
    fun `a walk to the end emits the cues and moves the session to Arrived`() = runTest {
        val cues = mutableListOf<NavigationCue>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.cues.toList(cues) }
        follow(timed(walk(route)))
        startNavigation()
        advanceTimeBy(60.seconds)
        assertEquals(listOf(NavigationCue.ManeuverApproaching(1, ARRIVAL), NavigationCue.Arrived), cues)
        assertEquals(NavigationSession.Arrived(route), store.state.value)
        assertNull(engine.progress.value)
    }

    @Test
    fun `silence for 10 s flags a weak signal until the next good fix`() = runTest {
        val start = route.points.first()
        val fixes = flow {
            emit(fix(start, 0))
            delay(15.seconds)
            emit(fix(start, 15_000))
            awaitCancellation()
        }
        follow(fixes)
        startNavigation()
        advanceTimeBy(9.seconds)
        assertFalse(engine.progress.value!!.weakSignal)
        advanceTimeBy(2.seconds)
        assertTrue(engine.progress.value!!.weakSignal)
        advanceTimeBy(5.seconds)
        assertFalse(engine.progress.value!!.weakSignal)
    }

    @Test
    fun `no fix at all since starting also flags a weak signal`() = runTest {
        follow(flow { awaitCancellation() })
        startNavigation()
        advanceTimeBy(10.seconds + 1.seconds)
        assertTrue(engine.progress.value!!.weakSignal)
    }

    @Test
    fun `stopping cancels tracking and clears the progress`() = runTest {
        follow(timed(walk(route)))
        startNavigation()
        advanceTimeBy(5.seconds)
        store.dispatch(E.Cancel)
        runCurrent()
        assertNull(engine.progress.value)
    }

    @Test
    fun `arrival then finish goes through Arrived to Completed and clears the cached route`() = runTest {
        val file = folder.root.resolve("active_route.json")
        val cache = RouteCache(file)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { cache.follow(store.state) }
        follow(timed(walk(route)))
        startNavigation()
        runCurrent()
        assertTrue(file.exists())
        advanceTimeBy(60.seconds)
        assertEquals(NavigationSession.Arrived(route), store.state.value)
        assertFalse(file.exists())
        store.dispatch(E.Finish)
        assertEquals(NavigationSession.Completed(route), store.state.value)
        assertFalse(file.exists())
    }
}
