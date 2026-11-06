package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** El POI source solo contra MockWebServer; nunca llama a la API real. El fixture es una respuesta real capturada. */
class GooglePoiSourceTest {

    private val prado = GeoPoint(40.4138, -3.6921)
    private val server = MockWebServer()
    private val connectivity = FakeConnectivityMonitor()
    private lateinit var budget: ApiCallBudget
    private lateinit var source: GooglePoiSource

    @Before
    fun setUp() {
        server.start()
        budget = ApiCallBudget(maxPerTrip = 5, maxPerDay = 100)
        val api = createPlacesApi("test-key", "com.xanderjs.haptimap", "AB12CD", server.url("/").toString())
        source = GooglePoiSource(api, connectivity, budget, retryBackoff = 1.milliseconds)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun enqueue(fixture: String, code: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(javaClass.getResource("/pois/$fixture")!!.readText()))
    }

    private fun RecordedRequest.json(): JsonObject = googleJson.parseToJsonElement(body.readUtf8()).jsonObject

    @Test
    fun `a real nearby response is parsed into places`() = runTest {
        enqueue("nearby_along_route.json")
        val result = source.searchAhead(prado, radiusMeters = 150.0, languageCode = "en") as PoiFetch.Found
        assertTrue(result.places.any { it.displayName?.text == "Museo Nacional del Prado" && it.primaryType == "art_museum" })
        assertEquals(10, result.places.size)
    }

    @Test
    fun `the request uses the POI field mask, Android headers and an exploration nearby query`() = runTest {
        enqueue("nearby_along_route.json")
        source.searchAhead(prado, radiusMeters = 150.0, languageCode = "es")
        val request = server.takeRequest()
        assertEquals("/v1/places:searchNearby", request.path)
        assertEquals(POI_FIELD_MASK, request.getHeader("X-Goog-FieldMask"))
        assertEquals("com.xanderjs.haptimap", request.getHeader("X-Android-Package"))
        assertEquals("AB12CD", request.getHeader("X-Android-Cert"))
        val body = request.json()
        assertEquals(EXPLORATION_PLACE_TYPES, body["includedTypes"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("DISTANCE", body["rankPreference"]!!.jsonPrimitive.content)
        assertEquals("es", body["languageCode"]!!.jsonPrimitive.content)
        val circle = body["locationRestriction"]!!.jsonObject["circle"]!!.jsonObject
        assertEquals(150.0, circle["radius"]!!.jsonPrimitive.content.toDouble(), 0.0)
    }

    @Test
    fun `offline suspends the query without hitting the network`() = runTest {
        connectivity.status.value = ConnectivityStatus.Disconnected
        assertEquals(PoiFetch.Offline, source.searchAhead(prado, 150.0, "en"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `when the budget is exhausted queries stop without hitting the network`() = runTest {
        repeat(5) { budget.record(ApiKind.PLACES) } // agota maxPerTrip=5
        assertEquals(PoiFetch.BudgetReached, source.searchAhead(prado, 150.0, "en"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `each query counts against the budget`() = runTest {
        enqueue("nearby_along_route.json")
        source.searchAhead(prado, 150.0, "en")
        assertTrue(budget.debugSummary().startsWith("Places=1"))
    }

    @Test
    fun `a quota error is reported and never retried`() = runTest {
        // reutiliza el fixture de cuota real de Places; 429 no se reintenta
        server.enqueue(MockResponse().setResponseCode(429).setBody(javaClass.getResource("/places/quota.json")!!.readText()))
        assertEquals(PoiFetch.Failed(PoiSourceError.QUOTA), source.searchAhead(prado, 150.0, "en"))
        assertEquals(1, server.requestCount) // una sola llamada, sin reintento
    }

    @Test
    fun `a network drop retries once, then succeeds`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) // primer intento: caída de red
        enqueue("nearby_along_route.json") // reintento: ok
        val result = source.searchAhead(prado, 150.0, "en")
        assertTrue(result is PoiFetch.Found)
        assertEquals(2, server.requestCount) // un intento + un reintento
    }

    @Test
    fun `malformed body is an API error`() = runTest {
        server.enqueue(MockResponse().setBody("""{"places":[{"id":1}]}"""))
        assertEquals(PoiFetch.Failed(PoiSourceError.API_ERROR), source.searchAhead(prado, 150.0, "en"))
    }
}
