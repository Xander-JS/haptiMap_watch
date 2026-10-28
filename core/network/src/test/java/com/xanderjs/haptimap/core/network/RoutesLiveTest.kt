package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceSearchResult
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Llama a Places y Routes de verdad. PENDIENTE (se salta) mientras no haya clave en el entorno:
 * `MAPS_API_KEY=… MAPS_CERT_SHA1=… ./gradlew --no-daemon :core:network:testDebugUnitTest --tests '*RoutesLiveTest'`.
 * MAPS_CERT_SHA1 es el SHA-1 de debug en hex sin ":", el mismo que restringe la clave a la app.
 */
class RoutesLiveTest {

    private val apiKey = System.getenv("MAPS_API_KEY").orEmpty()
    private val certSha1 = System.getenv("MAPS_CERT_SHA1").orEmpty()
    private val sol = GeoPoint(40.416775, -3.703790)
    private val connectivity = FakeConnectivityMonitor()

    @Before
    fun requireKey() = assumeTrue("Pending: set MAPS_API_KEY to run the live Places + Routes test", isApiKeyConfigured(apiKey))

    private val routes by lazy {
        GoogleRoutesRepository(createRoutesApi(apiKey, PACKAGE, certSha1), connectivity, { "es" }, hasApiKey = true)
    }

    @Test
    fun `Places finds the Prado and Routes walks there by place ID`() = runTest {
        val places = GooglePlacesRepository(createPlacesApi(apiKey, PACKAGE, certSha1), connectivity) { "es" }
        val search = places.searchByText("Museo del Prado", sol)
        val prado = (search as? PlaceSearchResult.Success)?.places?.first() ?: throw AssertionError("Expected places, got $search")
        assertTrue("place ID ${prado.id}", prado.id.isNotBlank())

        assertSolToPrado(routes.computeRoute(sol, prado))
    }

    @Test
    fun `a destination without place ID walks there by coordinates`() = runTest {
        val prado = Place("", "Museo del Prado", GeoPoint(40.413780, -3.692127), null, null, 0)
        assertSolToPrado(routes.computeRoute(sol, prado))
    }

    private fun assertSolToPrado(result: RouteResult) {
        val route: Route = (result as? RouteResult.Success)?.route ?: throw AssertionError("Expected a route, got $result")
        assertTrue("distance ${route.distanceMeters} m", route.distanceMeters in 800..2_000)
        assertTrue("duration ${route.duration}", route.duration.inWholeMinutes in 8..30)
        assertTrue(route.points.size > 2)
        assertTrue(route.steps.size > 2)
        assertEquals(Maneuver.ARRIVAL, route.steps.last().maneuver)
        assertTrue("turns ${route.steps.map { it.sourceManeuver }}", route.steps.any { it.maneuver == Maneuver.TURN_LEFT || it.maneuver == Maneuver.TURN_RIGHT })
        assertTrue(route.steps.first().instruction!!.isNotBlank())
    }

    private companion object {
        const val PACKAGE = "com.xanderjs.haptimap"
    }
}
