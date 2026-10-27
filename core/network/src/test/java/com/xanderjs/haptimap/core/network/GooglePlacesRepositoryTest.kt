package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.core.model.PlaceSearchResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GooglePlacesRepositoryTest {

    private val sol = GeoPoint(40.416775, -3.703790)
    private val server = MockWebServer()
    private val connectivity = FakeConnectivityMonitor()
    private var language = "es"
    private lateinit var repository: GooglePlacesRepository

    @Before
    fun setUp() {
        server.start()
        val api = createPlacesApi("test-key", "com.xanderjs.haptimap", "AB12CD", server.url("/").toString())
        repository = GooglePlacesRepository(api, connectivity) { language }
    }

    @After
    fun tearDown() = server.shutdown()

    private fun enqueue(fixture: String, code: Int = 200) {
        val body = javaClass.getResource("/places/$fixture")!!.readText()
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    private fun RecordedRequest.json(): JsonObject = placesJson.parseToJsonElement(body.readUtf8()).jsonObject

    @Test
    fun `category search returns places with distance and category`() = runTest {
        enqueue("nearby_pharmacies.json")
        val result = repository.searchByCategory(PlaceCategory.PHARMACY, sol) as PlaceSearchResult.Success
        assertEquals(listOf("Farmacia Puerta del Sol", "Farmacia Mayor"), result.places.map { it.name })
        assertEquals(listOf(PlaceCategory.PHARMACY, PlaceCategory.PHARMACY), result.places.map { it.category })
        assertEquals(343, result.places[1].distanceMeters)
        assertEquals("Puerta del Sol, 2, Madrid", result.places[0].address)
        assertNull(result.places[1].address)
    }

    @Test
    fun `category search sends field mask, Android headers and a distance-ranked nearby request`() = runTest {
        enqueue("nearby_pharmacies.json")
        repository.searchByCategory(PlaceCategory.TRANSIT, sol)
        val request = server.takeRequest()
        assertEquals("/v1/places:searchNearby", request.path)
        assertEquals("POST", request.method)
        assertEquals(PLACES_FIELD_MASK, request.getHeader("X-Goog-FieldMask"))
        assertEquals("test-key", request.getHeader("X-Goog-Api-Key"))
        assertEquals("com.xanderjs.haptimap", request.getHeader("X-Android-Package"))
        assertEquals("AB12CD", request.getHeader("X-Android-Cert"))
        val body = request.json()
        assertEquals(PlaceCategory.TRANSIT.placeTypes, body["includedTypes"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("DISTANCE", body["rankPreference"]!!.jsonPrimitive.content)
        assertEquals(MAX_RESULTS, body["maxResultCount"]!!.jsonPrimitive.content.toInt())
        assertEquals("es", body["languageCode"]!!.jsonPrimitive.content)
        val circle = body["locationRestriction"]!!.jsonObject["circle"]!!.jsonObject
        assertEquals(NEARBY_RADIUS_METERS, circle["radius"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertEquals(sol.latitude, circle["center"]!!.jsonObject["latitude"]!!.jsonPrimitive.content.toDouble(), 0.0)
    }

    @Test
    fun `text search is biased to the user and maps known types to categories`() = runTest {
        language = "en"
        enqueue("text_museum.json")
        val result = repository.searchByText("museo", sol) as PlaceSearchResult.Success
        assertEquals(listOf(null, PlaceCategory.TRANSIT), result.places.map { it.category })

        val request = server.takeRequest()
        assertEquals("/v1/places:searchText", request.path)
        assertEquals(PLACES_FIELD_MASK, request.getHeader("X-Goog-FieldMask"))
        assertEquals("AB12CD", request.getHeader("X-Android-Cert"))
        val body = request.json()
        assertEquals("museo", body["textQuery"]!!.jsonPrimitive.content)
        assertEquals("en", body["languageCode"]!!.jsonPrimitive.content)
        assertEquals(MAX_RESULTS, body["pageSize"]!!.jsonPrimitive.content.toInt())
        val circle = body["locationBias"]!!.jsonObject["circle"]!!.jsonObject
        assertEquals(TEXT_BIAS_RADIUS_METERS, circle["radius"]!!.jsonPrimitive.content.toDouble(), 0.0)
    }

    @Test
    fun `empty response is NO_RESULTS`() = runTest {
        enqueue("empty.json")
        assertEquals(failure(PlaceSearchError.NO_RESULTS), repository.searchByCategory(PlaceCategory.CAFE, sol))
    }

    @Test
    fun `quota error is API_QUOTA`() = runTest {
        enqueue("quota.json", 429)
        assertEquals(failure(PlaceSearchError.API_QUOTA), repository.searchByCategory(PlaceCategory.CAFE, sol))
    }

    @Test
    fun `invalid key is API_INVALID_KEY`() = runTest {
        enqueue("invalid_key.json", 400)
        assertEquals(failure(PlaceSearchError.API_INVALID_KEY), repository.searchByText("cafe", sol))
    }

    @Test
    fun `key blocked for this app is API_INVALID_KEY`() = runTest {
        enqueue("app_blocked.json", 403)
        assertEquals(failure(PlaceSearchError.API_INVALID_KEY), repository.searchByText("cafe", sol))
    }

    @Test
    fun `other bad request is API_ERROR`() = runTest {
        enqueue("bad_request.json", 400)
        assertEquals(failure(PlaceSearchError.API_ERROR), repository.searchByCategory(PlaceCategory.CAFE, sol))
    }

    @Test
    fun `malformed body is API_ERROR`() = runTest {
        server.enqueue(MockResponse().setBody("""{"places": [{"id": 1}]}"""))
        assertEquals(failure(PlaceSearchError.API_ERROR), repository.searchByCategory(PlaceCategory.CAFE, sol))
    }

    @Test
    fun `offline returns NO_CONNECTION without a request`() = runTest {
        connectivity.status.value = ConnectivityStatus.Disconnected
        assertEquals(failure(PlaceSearchError.NO_CONNECTION), repository.searchByCategory(PlaceCategory.CAFE, sol))
        assertEquals(failure(PlaceSearchError.NO_CONNECTION), repository.searchByText("cafe", sol))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `no location returns NO_LOCATION without a request`() = runTest {
        assertEquals(failure(PlaceSearchError.NO_LOCATION), repository.searchByCategory(PlaceCategory.CAFE, null))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `network failure is NO_CONNECTION`() = runTest {
        server.shutdown()
        assertEquals(failure(PlaceSearchError.NO_CONNECTION), repository.searchByCategory(PlaceCategory.CAFE, sol))
    }

    private fun failure(error: PlaceSearchError) = PlaceSearchResult.Failure(error)
}
