package com.xanderjs.haptimap.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {

    private val sol = GeoPoint(40.4168, -3.7038)

    @Test
    fun `bearing to the four cardinal points`() {
        assertEquals(0.0, sol.bearingTo(GeoPoint(40.4268, -3.7038)), 0.01)
        assertEquals(90.0, sol.bearingTo(GeoPoint(40.4168, -3.6938)), 0.1)
        assertEquals(180.0, sol.bearingTo(GeoPoint(40.4068, -3.7038)), 0.01)
        assertEquals(270.0, sol.bearingTo(GeoPoint(40.4168, -3.7138)), 0.1)
    }

    @Test
    fun `offset moves the given distance in the given bearing`() {
        val east = sol.offset(90.0, 100.0)
        assertEquals(100.0, sol.distanceTo(east), 0.05)
        assertEquals(90.0, sol.bearingTo(east), 0.01)
        assertEquals(50.0, sol.distanceTo(sol.offset(225.0, 50.0)), 0.05)
    }

    @Test
    fun `interpolate halfway`() {
        val mid = interpolate(GeoPoint(0.0, 0.0), GeoPoint(2.0, 4.0), 0.5)
        assertEquals(GeoPoint(1.0, 2.0), mid)
    }

    @Test
    fun `projection inside, before and after a segment`() {
        val a = sol
        val b = sol.offset(90.0, 100.0)
        // 30 m al este del inicio y 10 m al norte
        val inside = a.offset(90.0, 30.0).offset(0.0, 10.0).projectOnto(a, b)
        assertEquals(0.3, inside.fraction, 0.001)
        assertEquals(10.0, inside.distanceMeters, 0.05)
        val before = a.offset(270.0, 20.0).projectOnto(a, b)
        assertEquals(0.0, before.fraction, 0.0)
        assertEquals(20.0, before.distanceMeters, 0.05)
        val after = b.offset(90.0, 5.0).projectOnto(a, b)
        assertEquals(1.0, after.fraction, 0.0)
        assertEquals(5.0, after.distanceMeters, 0.05)
    }

    @Test
    fun `projection onto a zero-length segment is the distance to the point`() {
        val p = sol.offset(0.0, 7.0).projectOnto(sol, sol)
        assertEquals(0.0, p.fraction, 0.0)
        assertEquals(7.0, p.distanceMeters, 0.05)
    }

    @Test
    fun `measured path lengths, positions and segments`() {
        val corner = sol.offset(90.0, 100.0)
        val path = MeasuredPath(listOf(sol, corner, corner.offset(0.0, 50.0)))
        assertEquals(150.0, path.length, 0.1)
        assertEquals(100.0, path.cumulative[1], 0.05)
        assertEquals(0, path.segmentAt(99.0))
        assertEquals(1, path.segmentAt(100.0))
        assertEquals(1, path.segmentAt(150.0))
        assertEquals(0.0, path.pointAt(125.0).distanceTo(corner.offset(0.0, 25.0)), 0.05)
        assertEquals(path.points.last(), path.pointAt(999.0))
        assertEquals(sol, path.pointAt(-5.0))
        val (along, distance) = path.project(corner.offset(0.0, 20.0).offset(90.0, 3.0), 1)
        assertEquals(120.0, along, 0.1)
        assertEquals(3.0, distance, 0.05)
    }
}
