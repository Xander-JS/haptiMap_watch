package com.xanderjs.haptimap.presentation.debug

import com.xanderjs.haptimap.core.location.WalkSimulation
import com.xanderjs.haptimap.core.location.simulatedWalk
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.MeasuredPath
import com.xanderjs.haptimap.core.navigation.NavigationCue
import com.xanderjs.haptimap.core.navigation.RouteTracker
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleRouteTest {

    private val route = sampleRoute("en")!!

    @Test
    fun `several left and right turns, ending at the arrival`() {
        val maneuvers = route.steps.map { it.maneuver }
        assertEquals(2, maneuvers.count { it == Maneuver.TURN_LEFT })
        assertEquals(2, maneuvers.count { it == Maneuver.TURN_RIGHT })
        assertEquals(Maneuver.ARRIVAL, maneuvers.last())
        assertEquals(route.distanceMeters.toDouble(), MeasuredPath(route.points).length, 2.0)
    }

    @Test
    fun `instructions follow the app language`() {
        assertEquals("Turn right onto Calle del Príncipe", route.steps[1].instruction)
        assertEquals("Gira a la derecha por la Calle del Príncipe", sampleRoute("es")!!.steps[1].instruction)
    }

    @Test
    fun `a simulated walk along it announces every maneuver and arrives`() = runTest {
        val tracker = RouteTracker(route)
        val fixes = simulatedWalk(route.points, WalkSimulation(speedMetersPerSecond = 5.0), nowMillis = { currentTime }).take(240).toList()
        val cues = fixes.flatMap { tracker.update(it).cues }
        val passed = cues.filterIsInstance<NavigationCue.ManeuverPassed>().map { it.stepIndex }
        assertEquals((1 until route.steps.lastIndex).toList(), passed)
        assertTrue(NavigationCue.Arrived == cues.last())
    }
}
