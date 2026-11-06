package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.distanceTo
import com.xanderjs.haptimap.core.model.interpolate
import com.xanderjs.haptimap.core.model.offset
import kotlin.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cada motivo de descarte es alcanzable con el estimador geométrico real ([GeometricDetourEstimator]) y la
 * configuración por defecto (paso 18, arreglo de umbrales coherentes). Un POI a distancia perpendicular d de
 * una ruta recta produce un desvío de 2 × d; con los valores por defecto las ventanas son
 * TOO_MUCH_TIME en d≈(78,100], DETOUR_TOO_LARGE en (100,120] y TOO_FAR en d>120, así que basta elegir una d
 * en cada tramo para que esa regla sea la que decide.
 */
class PoiThresholdReachabilityTest {

    private val config = PoiEvaluationConfig()
    private val rejected = RejectedPoiMemory()

    // ruta recta de 400 m hacia el este; el POI se coloca al norte del punto medio
    private val start = GeoPoint(40.0, -3.0)
    private val end = start.offset(bearingDegrees = 90.0, meters = 400.0)
    private val mid = interpolate(start, end, 0.5)
    private val route = Route(
        destination = Place("d", "Destino", end, null, null, 0),
        distanceMeters = start.distanceTo(end).toInt(),
        duration = Duration.ZERO,
        points = listOf(start, end),
        steps = listOf(RouteStep(Maneuver.ARRIVAL, 0, Duration.ZERO, emptyList(), start, end, null, null)),
    )

    private fun evaluateAt(distanceMeters: Double, relevance: Double = 1.0): PoiEvaluation {
        val candidate = PoiCandidate("p", "POI", null, mid.offset(bearingDegrees = 0.0, meters = distanceMeters))
        val evaluator = PoiEvaluator(config, GeometricDetourEstimator(config), RelevanceProvider { relevance })
        return evaluator.evaluate(listOf(candidate), route, 0.0, NavigationProfile.TOUR, rejected).single()
    }

    @Test
    fun `a close relevant POI is suggested`() {
        val result = evaluateAt(distanceMeters = 50.0)
        assertEquals(PoiDecision.SUGGEST, result.decision)
    }

    @Test
    fun `TOO_MUCH_TIME is reachable as the deciding reason`() {
        assertEquals(DiscardReason.TOO_MUCH_TIME, evaluateAt(distanceMeters = 90.0).discardReason)
    }

    @Test
    fun `DETOUR_TOO_LARGE is reachable as the deciding reason`() {
        assertEquals(DiscardReason.DETOUR_TOO_LARGE, evaluateAt(distanceMeters = 110.0).discardReason)
    }

    @Test
    fun `TOO_FAR is reachable as the deciding reason`() {
        assertEquals(DiscardReason.TOO_FAR, evaluateAt(distanceMeters = 130.0).discardReason)
    }

    @Test
    fun `LOW_RELEVANCE is reachable as the deciding reason`() {
        assertEquals(DiscardReason.LOW_RELEVANCE, evaluateAt(distanceMeters = 20.0, relevance = config.minRelevance - 0.1).discardReason)
    }

    @Test
    fun `ALREADY_REJECTED is reachable as the deciding reason`() {
        rejected.reject("p")
        assertEquals(DiscardReason.ALREADY_REJECTED, evaluateAt(distanceMeters = 20.0).discardReason)
    }
}
