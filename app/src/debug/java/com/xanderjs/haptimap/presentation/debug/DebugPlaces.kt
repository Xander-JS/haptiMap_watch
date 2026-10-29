package com.xanderjs.haptimap.presentation.debug

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.core.model.PlaceSearchResult
import com.xanderjs.haptimap.core.model.distanceTo
import com.xanderjs.haptimap.core.model.offset
import com.xanderjs.haptimap.core.network.PlacesRepository
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/** Lo que tarda la búsqueda falsa: deja ver "Buscando…". */
private val FAKE_LATENCY = 600.milliseconds

/** Lo que hay que dictar (o escribir) para ver la búsqueda sin resultados. */
internal val NO_RESULTS_QUERIES = setOf("nothing", "nada")

// rumbo, metros y calle de cada lugar falso respecto a donde está el usuario
private val SPOTS = listOf(Triple(90.0, 150.0, "Calle de las Huertas, 12"), Triple(0.0, 420.0, "Calle del Prado, 4"), Triple(200.0, 900.0, "Calle de Atocha, 31"))

/**
 * Places (paso 11). Con clave, el de Google; sin clave, en debug, tres lugares alrededor de la posición para
 * recorrer el Modo Búsqueda hasta la navegación en el emulador. [label] da el nombre de la categoría en el idioma
 * de la app. Sin posición, NO_LOCATION, como el real; sin red no se llega aquí (lo corta el ViewModel).
 */
fun debugPlaces(places: PlacesRepository, hasApiKey: Boolean, label: (PlaceCategory) -> String): PlacesRepository =
    if (hasApiKey) places else object : PlacesRepository {
        override suspend fun searchByCategory(category: PlaceCategory, location: GeoPoint?) =
            fake(location) { SPOTS.map { (bearing, meters, street) -> place(it, bearing, meters, "${label(category)} ${street.substringBefore(',')}", category, street) } }

        override suspend fun searchByText(query: String, location: GeoPoint?) =
            fake(location) { if (query.lowercase() in NO_RESULTS_QUERIES) emptyList() else listOf(place(it, 45.0, 300.0, query, null, SPOTS[0].third)) }
    }

private suspend fun fake(location: GeoPoint?, places: (GeoPoint) -> List<Place>): PlaceSearchResult {
    delay(FAKE_LATENCY)
    location ?: return PlaceSearchResult.Failure(PlaceSearchError.NO_LOCATION)
    return places(location).takeIf { it.isNotEmpty() }?.let(PlaceSearchResult::Success) ?: PlaceSearchResult.Failure(PlaceSearchError.NO_RESULTS)
}

private fun place(from: GeoPoint, bearing: Double, meters: Double, name: String, category: PlaceCategory?, address: String): Place {
    val point = from.offset(bearing, meters)
    return Place("debug-$name", name, point, category, address, from.distanceTo(point).roundToInt())
}
