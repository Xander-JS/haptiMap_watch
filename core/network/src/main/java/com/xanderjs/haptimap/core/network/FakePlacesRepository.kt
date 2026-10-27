package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchResult

/** Para tests y previews: devuelve [result] y guarda la última búsqueda. */
class FakePlacesRepository(var result: PlaceSearchResult) : PlacesRepository {
    var lastQuery: Any? = null
        private set

    override suspend fun searchByCategory(category: PlaceCategory, location: GeoPoint?): PlaceSearchResult {
        lastQuery = category
        return result
    }

    override suspend fun searchByText(query: String, location: GeoPoint?): PlaceSearchResult {
        lastQuery = query
        return result
    }
}
