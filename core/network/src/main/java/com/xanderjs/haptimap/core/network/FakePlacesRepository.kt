package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchResult

/** Para tests y previews: devuelve [result] y guarda la última búsqueda y su posición. */
class FakePlacesRepository(var result: PlaceSearchResult) : PlacesRepository {
    var lastQuery: Any? = null
        private set
    var lastLocation: GeoPoint? = null
        private set

    override suspend fun searchByCategory(category: PlaceCategory, location: GeoPoint?): PlaceSearchResult {
        lastQuery = category
        lastLocation = location
        return result
    }

    override suspend fun searchByText(query: String, location: GeoPoint?): PlaceSearchResult {
        lastQuery = query
        lastLocation = location
        return result
    }
}
