package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.distanceTo
import com.xanderjs.haptimap.core.model.interpolate
import com.xanderjs.haptimap.core.model.offset
import kotlin.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometricDetourEstimatorTest {

    private val config = PoiEvaluationConfig()
    private val estimator = GeometricDetourEstimator(config)

    // ruta recta de 300 m hacia el este
    private val start = GeoPoint(40.0, -3.0)
    private val end = start.offset(bearingDegrees = 90.0, meters = 300.0)
    private val mid = interpolate(start, end, 0.5)
    private val route = straightRoute(start, end)

    private fun straightRoute(a: GeoPoint, b: GeoPoint) = Route(
        destination = Place("d", "Destino", b, null, null, 0),
        distanceMeters = a.distanceTo(b).toInt(),
        duration = Duration.ZERO,
        points = listOf(a, b),
        steps = listOf(RouteStep(Maneuver.ARRIVAL, 0, Duration.ZERO, emptyList(), a, b, null, null)),
    )

    private fun poi(location: GeoPoint) = PoiCandidate("p", "POI", null, location)

    @Test
    fun `a POI on the route has almost no detour`() {
        val est = estimator.estimate(poi(mid), route, fromAlongMeters = 0.0)
        assertEquals(0.0, est.distanceFromRouteMeters, 1.0)
        assertEquals(0.0, est.detourMeters, 2.0)
    }

    @Test
    fun `the detour is twice the perpendicular distance and the time matches the walking speed`() {
        val d = 50.0
        val est = estimator.estimate(poi(mid.offset(bearingDegrees = 0.0, meters = d)), route, fromAlongMeters = 0.0)
        assertEquals(d, est.distanceFromRouteMeters, 1.0)
        assertEquals(2 * d, est.detourMeters, 2.0)
        assertEquals(est.detourMeters / config.walkingSpeedMetersPerSecond, est.addedTime.inWholeMilliseconds / 1000.0, 0.1)
    }

    @Test
    fun `a POI behind the current progress is measured from the current position, not from the route behind`() {
        // POI a 10 m al norte del inicio; el usuario ya está al final de la ruta
        val behind = poi(start.offset(bearingDegrees = 0.0, meters = 10.0))
        val est = estimator.estimate(behind, route, fromAlongMeters = route.points[0].distanceTo(route.points[1]))
        // no se queda con los 10 m del inicio: mide desde el final (~300 m)
        assertTrue("was ${est.distanceFromRouteMeters}", est.distanceFromRouteMeters > 250.0)
    }
}
