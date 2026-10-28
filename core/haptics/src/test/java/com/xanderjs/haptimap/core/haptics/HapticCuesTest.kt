package com.xanderjs.haptimap.core.haptics

import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticEvent.BACK_ON_ROUTE
import com.xanderjs.haptimap.core.model.HapticEvent.TURN_IMMINENT
import com.xanderjs.haptimap.core.model.HapticEvent.TURN_LEFT
import com.xanderjs.haptimap.core.model.HapticEvent.TURN_RIGHT
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.navigation.NavigationCue
import com.xanderjs.haptimap.core.navigation.NavigationCue.ManeuverApproaching
import com.xanderjs.haptimap.core.navigation.NavigationCue.ManeuverImminent
import com.xanderjs.haptimap.core.navigation.NavigationCue.ManeuverPassed
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HapticCuesTest {

    @Test
    fun `every cue and maneuver maps to its event`() {
        // nada ha sonado nunca: el Imminent de un giro repite la dirección (ver los tests de abajo)
        val silent = HapticCues { 0 }
        var leftPlays = 0
        val played = HapticCues { leftPlays }.also { it.eventFor(ManeuverApproaching(1, Maneuver.TURN_LEFT)) }
        leftPlays++
        val expected = mapOf<NavigationCue, HapticEvent?>(
            ManeuverApproaching(1, Maneuver.TURN_LEFT) to TURN_LEFT,
            ManeuverApproaching(1, Maneuver.TURN_RIGHT) to TURN_RIGHT,
            ManeuverApproaching(1, Maneuver.CONTINUE) to HapticEvent.CONTINUE,
            ManeuverApproaching(1, Maneuver.ARRIVAL) to null,
            ManeuverImminent(1, Maneuver.CONTINUE) to null,
            ManeuverImminent(1, Maneuver.ARRIVAL) to null,
            ManeuverPassed(1, Maneuver.TURN_LEFT) to null,
            ManeuverPassed(1, Maneuver.TURN_RIGHT) to null,
            ManeuverPassed(1, Maneuver.CONTINUE) to null,
            ManeuverPassed(1, Maneuver.ARRIVAL) to null,
            NavigationCue.Arrived to HapticEvent.ARRIVAL,
            NavigationCue.OffRoute to HapticEvent.OFF_ROUTE,
            NavigationCue.BackOnRoute to BACK_ON_ROUTE,
            NavigationCue.Rerouted to BACK_ON_ROUTE,
        )
        expected.forEach { (cue, event) -> assertEquals("$cue", event, silent.eventFor(cue)) }
        assertEquals(TURN_IMMINENT, played.eventFor(ManeuverImminent(1, Maneuver.TURN_LEFT)))
        assertEquals(TURN_LEFT, silent.eventFor(ManeuverImminent(1, Maneuver.TURN_LEFT)))
        assertEquals(TURN_RIGHT, silent.eventFor(ManeuverImminent(1, Maneuver.TURN_RIGHT)))
    }

    // con el motor de verdad: lo que de verdad llega a sonar

    private val cues = MutableSharedFlow<NavigationCue>(extraBufferCapacity = 16)

    private fun TestScope.start(): FakeHapticPlayer {
        val player = FakeHapticPlayer { currentTime }
        val engine = HapticEngine(player)
        val adapter = HapticCues(engine::playCount)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.follow(cues.mapNotNull(adapter::eventFor), flowOf(true)) }
        return player
    }

    @Test
    fun `normal case - direction at 18 m, imminent at 6 m`() = runTest {
        val player = start()
        cues.tryEmit(ManeuverApproaching(1, Maneuver.TURN_LEFT))
        advanceTimeBy(9.seconds)
        cues.tryEmit(ManeuverImminent(1, Maneuver.TURN_LEFT))
        advanceTimeBy(10.seconds)
        assertEquals(listOf(TURN_LEFT, TURN_IMMINENT), player.events)
    }

    @Test
    fun `approaching and imminent in one update play the direction once`() = runTest {
        val player = start()
        cues.tryEmit(ManeuverApproaching(1, Maneuver.TURN_RIGHT))
        cues.tryEmit(ManeuverImminent(1, Maneuver.TURN_RIGHT))
        advanceTimeBy(10.seconds)
        assertEquals(listOf(TURN_RIGHT), player.events)
    }

    @Test
    fun `imminent after a dropped approaching plays the direction`() = runTest {
        val player = start()
        // vuelve a la ruta justo antes del giro: BACK_ON_ROUTE gana y la derecha se descarta
        cues.tryEmit(NavigationCue.BackOnRoute)
        cues.tryEmit(ManeuverApproaching(2, Maneuver.TURN_RIGHT))
        advanceTimeBy(8.seconds)
        cues.tryEmit(ManeuverImminent(2, Maneuver.TURN_RIGHT))
        advanceTimeBy(10.seconds)
        assertEquals(listOf(BACK_ON_ROUTE, TURN_RIGHT), player.events)
    }

    @Test
    fun `imminent while the direction is still waiting does not repeat it`() = runTest {
        val player = start()
        cues.tryEmit(NavigationCue.OffRoute)
        advanceTimeBy(200.milliseconds)
        // la izquierda espera a que acabe el fuera de ruta; el inminente llega antes de que suene
        cues.tryEmit(ManeuverApproaching(1, Maneuver.TURN_LEFT))
        cues.tryEmit(ManeuverImminent(1, Maneuver.TURN_LEFT))
        advanceTimeBy(10.seconds)
        assertEquals(listOf(HapticEvent.OFF_ROUTE, TURN_LEFT), player.events)
    }
}
