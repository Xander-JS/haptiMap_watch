package com.xanderjs.haptimap.core.model

/** Resultado de una búsqueda de lugares: lista no vacía o un error explícito. */
sealed interface PlaceSearchResult {
    data class Success(val places: List<Place>) : PlaceSearchResult
    data class Failure(val error: PlaceSearchError) : PlaceSearchResult
}

/** Por qué una búsqueda no devolvió lugares. Los API_* son fallos del servicio de Google. */
enum class PlaceSearchError {
    NO_CONNECTION,
    NO_LOCATION,
    NO_RESULTS,
    API_QUOTA,
    API_INVALID_KEY,
    API_ERROR,
}
