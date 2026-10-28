package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.RouteResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Llama a Routes API de verdad. PENDIENTE (se salta) mientras no haya clave en el entorno:
 * `MAPS_API_KEY=… MAPS_CERT_SHA1=… ./gradlew :core:network:testDebugUnitTest --tests '*RoutesLiveTest'`.
 * MAPS_CERT_SHA1 es el SHA-1 de debug en hex sin ":", el mismo que restringe la clave a la app.
 */
class RoutesLiveTest {

    private val apiKey = System.getenv("MAPS_API_KEY").orEmpty()
    private val sol = GeoPoint(40.416775, -3.703790)
    private val prado = Place("prado", "Museo del Prado", GeoPoint(40.413780, -3.692127), null, null, 0)

    @Before
    fun requireKey() = assumeTrue("Pending: set MAPS_API_KEY to run the live Routes test", isApiKeyConfigured(apiKey))

    @Test
    fun `walking route from Sol to the Prado`() = runTest {
        val api = createRoutesApi(apiKey, "com.xanderjs.haptimap", System.getenv("MAPS_CERT_SHA1").orEmpty())
        val result = GoogleRoutesRepository(api, FakeConnectivityMonitor(), { "es" }, hasApiKey = true).computeRoute(sol, prado)

        val route = (result as? RouteResult.Success)?.route ?: throw AssertionError("Expected a route, got $result")
        assertTrue("distance ${route.distanceMeters} m", route.distanceMeters in 800..2_000)
        assertTrue("duration ${route.duration}", route.duration.inWholeMinutes in 8..30)
        assertTrue(route.points.size > 2)
        assertTrue(route.steps.size > 2)
        assertEquals(Maneuver.ARRIVAL, route.steps.last().maneuver)
        assertTrue("turns ${route.steps.map { it.sourceManeuver }}", route.steps.any { it.maneuver == Maneuver.TURN_LEFT || it.maneuver == Maneuver.TURN_RIGHT })
        assertTrue(route.steps.first().instruction!!.isNotBlank())
    }
}
