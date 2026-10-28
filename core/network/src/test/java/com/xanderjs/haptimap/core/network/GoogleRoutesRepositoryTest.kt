package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.RouteResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GoogleRoutesRepositoryTest {

    private val sol = GeoPoint(40.41678, -3.70379)
    private val prado = Place("prado", "Museo del Prado", GeoPoint(40.41380, -3.69210), null, null, 1000)
    private val server = MockWebServer()
    private val connectivity = FakeConnectivityMonitor()
    private lateinit var api: RoutesApi

    @Before
    fun setUp() {
        server.start()
        api = createRoutesApi("test-key", "com.xanderjs.haptimap", "AB12CD", server.url("/").toString())
    }

    @After
    fun tearDown() = server.shutdown()

    private fun repository(hasApiKey: Boolean = true) = GoogleRoutesRepository(api, connectivity, { "es" }, hasApiKey)

    private fun enqueue(fixture: String, code: Int = 200) {
        val body = javaClass.getResource("/$fixture")!!.readText()
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    private fun failure(error: RouteError) = RouteResult.Failure(error)

    @Test
    fun `success returns the mapped route`() = runTest {
        enqueue("routes/sol_to_prado.json")
        val route = (repository().computeRoute(sol, prado) as RouteResult.Success).route
        assertEquals(1089, route.distanceMeters)
        assertEquals(Maneuver.ARRIVAL, route.steps.last().maneuver)
    }

    @Test
    fun `sends a WALK request with field mask, Android headers and no alternatives by default`() = runTest {
        enqueue("routes/sol_to_prado.json")
        repository().computeRoute(sol, prado)
        val request = server.takeRequest()
        assertEquals("/directions/v2:computeRoutes", request.path)
        assertEquals("POST", request.method)
        assertEquals(ROUTES_FIELD_MASK, request.getHeader("X-Goog-FieldMask"))
        assertEquals("test-key", request.getHeader("X-Goog-Api-Key"))
        assertEquals("com.xanderjs.haptimap", request.getHeader("X-Android-Package"))
        assertEquals("AB12CD", request.getHeader("X-Android-Cert"))
        val body = googleJson.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("WALK", body["travelMode"]!!.jsonPrimitive.content)
        assertEquals("false", body["computeAlternativeRoutes"]!!.jsonPrimitive.content)
        assertEquals("es", body["languageCode"]!!.jsonPrimitive.content)
        val origin = body["origin"]!!.jsonObject["location"]!!.jsonObject["latLng"]!!.jsonObject
        assertEquals(sol.latitude, origin["latitude"]!!.jsonPrimitive.content.toDouble(), 0.0)
        val destination = body["destination"]!!.jsonObject["location"]!!.jsonObject["latLng"]!!.jsonObject
        assertEquals(prado.location.longitude, destination["longitude"]!!.jsonPrimitive.content.toDouble(), 0.0)
    }

    @Test
    fun `alternatives are requested only when asked`() = runTest {
        enqueue("routes/two_routes.json")
        val route = (repository().computeRoute(sol, prado, alternatives = true) as RouteResult.Success).route
        val body = googleJson.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("true", body["computeAlternativeRoutes"]!!.jsonPrimitive.content)
        assertEquals(1089, route.distanceMeters)
    }

    @Test
    fun `offline returns NO_CONNECTION without a request`() = runTest {
        connectivity.status.value = ConnectivityStatus.Disconnected
        assertEquals(failure(RouteError.NO_CONNECTION), repository().computeRoute(sol, prado))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `missing key returns MISSING_KEY without a request`() = runTest {
        assertEquals(failure(RouteError.MISSING_KEY), repository(hasApiKey = false).computeRoute(sol, prado))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `no location returns NO_LOCATION without a request`() = runTest {
        assertEquals(failure(RouteError.NO_LOCATION), repository().computeRoute(null, prado))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `empty routes is NO_ROUTE`() = runTest {
        enqueue("routes/empty.json")
        assertEquals(failure(RouteError.NO_ROUTE), repository().computeRoute(sol, prado))
    }

    @Test
    fun `invalid key 400 is API_INVALID_KEY`() = runTest {
        enqueue("places/invalid_key.json", 400)
        assertEquals(failure(RouteError.API_INVALID_KEY), repository().computeRoute(sol, prado))
    }

    @Test
    fun `key blocked 403 is API_INVALID_KEY`() = runTest {
        enqueue("places/app_blocked.json", 403)
        assertEquals(failure(RouteError.API_INVALID_KEY), repository().computeRoute(sol, prado))
    }

    @Test
    fun `quota 429 is API_QUOTA`() = runTest {
        enqueue("places/quota.json", 429)
        assertEquals(failure(RouteError.API_QUOTA), repository().computeRoute(sol, prado))
    }

    @Test
    fun `other 4xx is API_ERROR`() = runTest {
        enqueue("places/bad_request.json", 400)
        assertEquals(failure(RouteError.API_ERROR), repository().computeRoute(sol, prado))
    }

    @Test
    fun `5xx is API_ERROR`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("{}"))
        assertEquals(failure(RouteError.API_ERROR), repository().computeRoute(sol, prado))
    }

    @Test
    fun `malformed body is API_ERROR`() = runTest {
        server.enqueue(MockResponse().setBody("{\"routes\": 42}"))
        assertEquals(failure(RouteError.API_ERROR), repository().computeRoute(sol, prado))
    }

    @Test
    fun `network dropping mid-request is NO_CONNECTION`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals(failure(RouteError.NO_CONNECTION), repository().computeRoute(sol, prado))
    }
}
