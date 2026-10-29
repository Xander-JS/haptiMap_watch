package com.xanderjs.haptimap.presentation.debug

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.core.model.PlaceSearchResult
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.RouteResult
import com.xanderjs.haptimap.core.network.FakePlacesRepository
import com.xanderjs.haptimap.core.network.FakeRoutesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

// Paso 11: los lugares y rutas falsos de debug sin clave, con los que se recorre la búsqueda en el emulador.
class DebugApisTest {

    private val sol = GeoPoint(40.4168, -3.7038)
    private val real = FakePlacesRepository(PlaceSearchResult.Failure(PlaceSearchError.API_INVALID_KEY))
    private val noKey = debugPlaces(real, hasApiKey = false) { "Farmacia" }

    @Test
    fun `with a key both are the real ones`() {
        assertSame(real, debugPlaces(real, hasApiKey = true) { "" })
        val routes = FakeRoutesRepository(RouteResult.Failure(RouteError.NO_ROUTE))
        assertSame(routes, debugRoutes(routes) { "en" })
    }

    @Test
    fun `a category gives three nearby places, nearest first, named in the app language`() = runTest {
        val places = (noKey.searchByCategory(PlaceCategory.PHARMACY, sol) as PlaceSearchResult.Success).places
        assertEquals(listOf(150, 420, 900), places.map { it.distanceMeters })
        assertTrue(places.all { it.category == PlaceCategory.PHARMACY && it.name.startsWith("Farmacia ") && it.address != null })
    }

    @Test
    fun `text gives one place, or none for nothing, and no location fails like Places`() = runTest {
        assertEquals("Museo", (noKey.searchByText("Museo", sol) as PlaceSearchResult.Success).places.single().name)
        assertEquals(PlaceSearchResult.Failure(PlaceSearchError.NO_RESULTS), noKey.searchByText("Nada", sol))
        assertEquals(PlaceSearchResult.Failure(PlaceSearchError.NO_LOCATION), noKey.searchByCategory(PlaceCategory.CAFE, null))
    }

    @Test
    fun `without a key the route is the fake one and says it has a key`() = runTest {
        val place = (noKey.searchByCategory(PlaceCategory.PHARMACY, sol) as PlaceSearchResult.Success).places.last()
        val routes = debugRoutes(FakeRoutesRepository(RouteResult.Failure(RouteError.MISSING_KEY), hasApiKey = false)) { "es" }
        assertTrue(routes.hasApiKey)
        assertEquals(place, (routes.computeRoute(sol, place) as RouteResult.Success).route.destination)
        assertEquals(RouteResult.Failure(RouteError.NO_LOCATION), routes.computeRoute(null, place))
    }
}
