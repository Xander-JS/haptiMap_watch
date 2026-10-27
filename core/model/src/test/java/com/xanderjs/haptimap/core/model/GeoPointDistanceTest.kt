package com.xanderjs.haptimap.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoPointDistanceTest {

    private val sol = GeoPoint(40.416775, -3.703790)
    private val plazaMayor = GeoPoint(40.415363, -3.707398)

    @Test
    fun `same point is zero`() = assertEquals(0.0, sol.distanceTo(sol), 0.0)

    @Test
    fun `one degree of latitude is pi R over 180`() =
        assertEquals(Math.PI * EARTH_RADIUS_METERS / 180, GeoPoint(0.0, 0.0).distanceTo(GeoPoint(1.0, 0.0)), 0.01)

    @Test
    fun `walking distance in Madrid`() = assertEquals(343.4, sol.distanceTo(plazaMayor), 0.5)

    @Test
    fun `long distance London to Paris`() =
        // Big Ben → torre Eiffel, ~340,5 km en gran círculo
        assertEquals(340_539.0, GeoPoint(51.5007, -0.1246).distanceTo(GeoPoint(48.8584, 2.2945)), 100.0)

    @Test
    fun `is symmetric`() = assertEquals(sol.distanceTo(plazaMayor), plazaMayor.distanceTo(sol), 1e-9)

    @Test
    fun `crosses the antimeridian the short way`() =
        assertEquals(Math.PI * EARTH_RADIUS_METERS / 180, GeoPoint(0.0, 179.5).distanceTo(GeoPoint(0.0, -179.5)), 0.01)
}
