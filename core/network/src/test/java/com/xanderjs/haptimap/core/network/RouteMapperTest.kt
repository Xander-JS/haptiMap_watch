package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteMapperTest {

    private val sol = GeoPoint(40.41678, -3.70379)
    private val prado = Place("prado", "Museo del Prado", GeoPoint(40.41380, -3.69210), null, null, 1000)

    private fun map(fixture: String): Route? =
        googleJson.decodeFromString<RoutesResponse>(javaClass.getResource("/routes/$fixture")!!.readText()).toRoute(prado, sol)

    @Test
    fun `maps totals, geometry and every step of a real-shaped response`() {
        val route = map("sol_to_prado.json")!!
        assertEquals(prado, route.destination)
        assertEquals(1089, route.distanceMeters)
        assertEquals(814.seconds, route.duration)
        assertEquals(9, route.points.size)
        assertEquals(sol, route.points.first())

        val first = route.steps.first()
        assertEquals(118, first.distanceMeters)
        assertEquals(88.seconds, first.duration)
        assertEquals(listOf(sol, GeoPoint(40.41690, -3.70240)), first.points)
        assertEquals(sol, first.start)
        assertEquals(GeoPoint(40.41690, -3.70240), first.end)
        assertEquals("Head east on Puerta del Sol toward Calle de Alcalá", first.instruction)
    }

    @Test
    fun `left, right, straight, slight and sharp turns fold into the semantic maneuvers`() {
        val steps = map("sol_to_prado.json")!!.steps
        assertEquals(
            listOf(
                Maneuver.CONTINUE, // DEPART
                Maneuver.TURN_RIGHT, // TURN_SLIGHT_RIGHT
                Maneuver.CONTINUE, // STRAIGHT
                Maneuver.TURN_LEFT, // TURN_SHARP_LEFT
                Maneuver.TURN_LEFT,
                Maneuver.CONTINUE, // NAME_CHANGE
                Maneuver.TURN_RIGHT,
                Maneuver.CONTINUE, // sin maniobra
                Maneuver.ARRIVAL,
            ),
            steps.map { it.maneuver },
        )
        // el valor original se conserva para afinar después
        assertEquals("TURN_SLIGHT_RIGHT", steps[1].sourceManeuver)
        assertEquals("TURN_SHARP_LEFT", steps[3].sourceManeuver)
        assertNull(steps[7].sourceManeuver)
    }

    @Test
    fun `ends with a zero-length ARRIVAL step at the end of the last step`() {
        val route = map("sol_to_prado.json")!!
        val arrival = route.steps.last()
        assertEquals(Maneuver.ARRIVAL, arrival.maneuver)
        assertEquals(0, arrival.distanceMeters)
        assertEquals(Duration.ZERO, arrival.duration)
        assertEquals(route.steps[route.steps.size - 2].end, arrival.start)
        assertEquals(arrival.start, arrival.end)
        assertNull(arrival.instruction)
    }

    @Test
    fun `unknown maneuvers continue and missing fields fall back without failing`() {
        val steps = map("missing_fields.json")!!.steps
        assertEquals(listOf(Maneuver.CONTINUE, Maneuver.TURN_LEFT, Maneuver.CONTINUE, Maneuver.CONTINUE, Maneuver.ARRIVAL), steps.map { it.maneuver })
        assertEquals("SOME_FUTURE_MANEUVER", steps[2].sourceManeuver)

        // sin startLocation/endLocation: de la polilínea
        assertEquals(sol, steps[0].start)
        assertEquals(GeoPoint(40.41690, -3.70240), steps[0].end)
        // sin distancia ni duración: 0
        assertEquals(0, steps[1].distanceMeters)
        assertEquals(Duration.ZERO, steps[1].duration)
        assertNull(steps[0].instruction)
        // sin polilínea ni posiciones: en el final del paso anterior
        assertEquals(steps[1].end, steps[2].start)
        assertEquals(steps[2].start, steps[2].end)
        assertEquals(emptyList<GeoPoint>(), steps[2].points)
        assertEquals(steps[1].end, steps.last().end)
    }

    @Test
    fun `route without polyline has no points`() {
        assertEquals(emptyList<GeoPoint>(), map("missing_fields.json")!!.points)
    }

    @Test
    fun `with alternatives only the first route is used`() {
        assertEquals(1089, map("two_routes.json")!!.distanceMeters)
    }

    @Test
    fun `empty response has no route`() {
        assertNull(map("empty.json"))
    }

    @Test
    fun `route without steps is just the arrival at the origin`() {
        val route = RoutesResponse(listOf(RouteDto())).toRoute(prado, sol)!!
        assertEquals(listOf(Maneuver.ARRIVAL), route.steps.map { it.maneuver })
        assertEquals(sol, route.steps.single().end)
    }

    @Test
    fun `durations parse whole and fractional seconds`() {
        assertEquals(754.seconds, parseDuration("754s"))
        assertEquals(12.5.seconds, parseDuration("12.5s"))
        assertEquals(Duration.ZERO, parseDuration(null))
        assertEquals(Duration.ZERO, parseDuration("soon"))
    }
}
