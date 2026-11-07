package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.RouteResult

/** Para tests y previews: devuelve [result] y guarda cuántas veces se pidió y con qué origen. */
class FakeRoutesRepository(var result: RouteResult, override var hasApiKey: Boolean = true) : RoutesRepository {
    var requests = 0
        private set
    var lastOrigin: GeoPoint? = null
        private set
    var lastDestination: Place? = null
        private set
    var lastIntermediates: List<Place> = emptyList()
        private set

    override suspend fun computeRoute(origin: GeoPoint?, destination: Place, intermediates: List<Place>, alternatives: Boolean): RouteResult {
        requests++
        lastOrigin = origin
        lastDestination = destination
        lastIntermediates = intermediates
        return result
    }
}
