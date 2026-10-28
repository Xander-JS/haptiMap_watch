package com.xanderjs.haptimap.core.haptics

import com.xanderjs.haptimap.core.haptics.HapticEngine.Companion.COLLAPSE_WINDOW
import com.xanderjs.haptimap.core.haptics.HapticEngine.Companion.MIN_GAP
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticEvent.ARRIVAL
import com.xanderjs.haptimap.core.model.HapticEvent.BACK_ON_ROUTE
import com.xanderjs.haptimap.core.model.HapticEvent.CONTINUE
import com.xanderjs.haptimap.core.model.HapticEvent.OFF_ROUTE
import com.xanderjs.haptimap.core.model.HapticEvent.TURN_IMMINENT
import com.xanderjs.haptimap.core.model.HapticEvent.TURN_LEFT
import com.xanderjs.haptimap.core.model.HapticEvent.TURN_RIGHT
import com.xanderjs.haptimap.core.model.HapticTokens
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HapticEngineTest {

    private val events = MutableSharedFlow<HapticEvent>(extraBufferCapacity = 16)
    private val active = MutableStateFlow(true)

    private fun TestScope.start(): Pair<HapticEngine, FakeHapticPlayer> {
        val player = FakeHapticPlayer { currentTime }
        val engine = HapticEngine(player)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.follow(events, active) }
        return engine to player
    }

    private fun duration(event: HapticEvent) = HapticTokens.patternFor(event).totalDurationMs.milliseconds

    @Test
    fun `several events in one update collapse to the most relevant`() = runTest {
        val (_, player) = start()
        listOf(TURN_LEFT, TURN_IMMINENT, OFF_ROUTE, CONTINUE).forEach(events::tryEmit)
        advanceTimeBy(10.seconds)
        assertEquals(listOf(OFF_ROUTE), player.events)
        assertEquals(COLLAPSE_WINDOW.inWholeMilliseconds, player.played.single().atMillis)
    }

    @Test
    fun `at equal priority the newest wins`() = runTest {
        val (_, player) = start()
        listOf(TURN_LEFT, TURN_RIGHT).forEach(events::tryEmit)
        advanceTimeBy(10.seconds)
        assertEquals(listOf(TURN_RIGHT), player.events)
    }

    @Test
    fun `a pattern is never cut or overlapped and the next waits for the minimum gap`() = runTest {
        val (_, player) = start()
        events.tryEmit(TURN_LEFT)
        advanceTimeBy(COLLAPSE_WINDOW + 50.milliseconds)
        // llega más prioritario con la izquierda sonando: espera
        events.tryEmit(ARRIVAL)
        advanceTimeBy(10.seconds)
        assertEquals(listOf(TURN_LEFT, ARRIVAL), player.events)
        val (left, arrival) = player.played
        // lo que esperaba también pasa por la ventana de fusión
        assertEquals((duration(TURN_LEFT) + MIN_GAP + COLLAPSE_WINDOW).inWholeMilliseconds, arrival.atMillis - left.atMillis)
        assertEquals(0, player.cancels)
    }

    @Test
    fun `what arrives while a pattern plays waits in one slot, highest priority first`() = runTest {
        val (_, player) = start()
        events.tryEmit(OFF_ROUTE)
        advanceTimeBy(300.milliseconds)
        listOf(CONTINUE, BACK_ON_ROUTE, TURN_RIGHT).forEach(events::tryEmit)
        advanceTimeBy(10.seconds)
        assertEquals(listOf(OFF_ROUTE, BACK_ON_ROUTE), player.events)
    }

    @Test
    fun `nothing plays closer than the minimum gap`() = runTest {
        val (_, player) = start()
        repeat(6) {
            events.tryEmit(HapticEvent.entries[it])
            advanceTimeBy(200.milliseconds)
        }
        advanceTimeBy(10.seconds)
        player.played.zipWithNext().forEach { (a, b) ->
            val end = a.atMillis + a.pattern.totalDurationMs
            assertTrue("$a y $b se solapan o van pegados", b.atMillis - end >= MIN_GAP.inWholeMilliseconds)
        }
    }

    @Test
    fun `stopping navigation cancels the pattern and drops what was pending`() = runTest {
        val (_, player) = start()
        events.tryEmit(OFF_ROUTE)
        advanceTimeBy(300.milliseconds)
        events.tryEmit(BACK_ON_ROUTE)
        active.value = false
        advanceTimeBy(10.seconds)
        assertEquals(listOf(OFF_ROUTE), player.events)
        assertEquals(1, player.cancels)
        // y vuelve a sonar al navegar otra vez
        active.value = true
        events.tryEmit(CONTINUE)
        advanceTimeBy(10.seconds)
        assertEquals(listOf(OFF_ROUTE, CONTINUE), player.events)
    }

    @Test
    fun `arriving does not cut the arrival pattern, finishing does`() = runTest {
        val route = Route(Place("p", "P", GeoPoint(0.0, 0.0), null, null, 0), 0, 0.seconds, emptyList(), emptyList())
        val session = MutableStateFlow<NavigationSession>(NavigationSession.NavigationActive(route))
        val player = FakeHapticPlayer { currentTime }
        val engine = HapticEngine(player)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.follow(events, session.map { it.hapticsActive }) }
        events.tryEmit(ARRIVAL)
        session.value = NavigationSession.Arrived(route)
        advanceTimeBy(10.seconds)
        assertEquals(0, player.cancels)
        session.value = NavigationSession.Completed(route)
        advanceTimeBy(10.seconds)
        assertEquals(1, player.cancels)
        assertEquals(listOf(ARRIVAL), player.events)
    }

    @Test
    fun `the test bench plays without navigating and the intensity scales it`() = runTest {
        active.value = false
        val (engine, player) = start()
        engine.intensity = 0.5f
        engine.play(TURN_RIGHT)
        advanceTimeBy(10.seconds)
        assertArrayEquals(HapticTokens.patternFor(TURN_RIGHT).scaled(0.5f).amplitudes, player.played.single().pattern.amplitudes)
        assertEquals(TURN_RIGHT, engine.played.value?.event)
        assertEquals(1, engine.playCount(TURN_RIGHT))
    }
}
