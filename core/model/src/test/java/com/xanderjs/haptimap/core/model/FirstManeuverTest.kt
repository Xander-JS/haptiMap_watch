package com.xanderjs.haptimap.core.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Test

class FirstManeuverTest {

    private val point = GeoPoint(40.4168, -3.7038)
    private val destination = Place("prado", "Museo del Prado", point, null, null, 0)

    private fun step(maneuver: Maneuver, meters: Int) = RouteStep(maneuver, meters, Duration.ZERO, listOf(point), point, point, null, null)

    private fun route(vararg steps: RouteStep) = Route(destination, 1240, 15.minutes, listOf(point), steps.toList())

    @Test
    fun `is the second step, at the distance of the departure step`() {
        val turn = step(Maneuver.TURN_LEFT, 1120)
        assertEquals(NextManeuver(turn, 120), route(step(Maneuver.CONTINUE, 120), turn, step(Maneuver.ARRIVAL, 0)).firstManeuver())
    }

    @Test
    fun `a single real step leads straight to the arrival`() {
        val arrival = step(Maneuver.ARRIVAL, 0)
        assertEquals(NextManeuver(arrival, 300), route(step(Maneuver.CONTINUE, 300), arrival).firstManeuver())
    }

    @Test
    fun `a route with only the arrival step arrives at the total distance`() {
        val arrival = step(Maneuver.ARRIVAL, 0)
        assertEquals(NextManeuver(arrival, 1240), route(arrival).firstManeuver())
    }
}
