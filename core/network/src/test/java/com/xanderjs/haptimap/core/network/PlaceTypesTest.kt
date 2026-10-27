package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceTypesTest {

    @Test
    fun `each category maps to its Places types`() {
        assertEquals(listOf("store"), PlaceCategory.STORE.placeTypes)
        assertEquals(listOf("hardware_store"), PlaceCategory.HARDWARE_STORE.placeTypes)
        assertEquals(listOf("restaurant"), PlaceCategory.RESTAURANT.placeTypes)
        assertEquals(listOf("cafe"), PlaceCategory.CAFE.placeTypes)
        assertEquals(listOf("pharmacy"), PlaceCategory.PHARMACY.placeTypes)
        assertEquals(
            listOf("transit_station", "bus_station", "subway_station", "train_station", "light_rail_station"),
            PlaceCategory.TRANSIT.placeTypes,
        )
        assertEquals(listOf("supermarket"), PlaceCategory.SUPERMARKET.placeTypes)
    }

    @Test
    fun `no Places type belongs to two categories`() {
        val all = PlaceCategory.entries.flatMap { it.placeTypes }
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun `every Places type maps back to its category`() {
        PlaceCategory.entries.forEach { category ->
            category.placeTypes.forEach { assertEquals(it, category, categoryForPlaceType(it)) }
        }
    }

    @Test
    fun `unknown or missing type has no category`() {
        assertNull(categoryForPlaceType("museum"))
        assertNull(categoryForPlaceType(null))
    }

    @Test
    fun `HTTP errors map to explicit search errors`() {
        assertEquals(PlaceSearchError.API_QUOTA, placesErrorFor(429, null))
        assertEquals(PlaceSearchError.API_INVALID_KEY, placesErrorFor(403, null))
        assertEquals(PlaceSearchError.API_INVALID_KEY, placesErrorFor(400, """{"reason": "API_KEY_INVALID"}"""))
        assertEquals(PlaceSearchError.API_ERROR, placesErrorFor(400, """{"status": "INVALID_ARGUMENT"}"""))
        assertEquals(PlaceSearchError.API_ERROR, placesErrorFor(500, null))
    }
}
