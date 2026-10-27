package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ConnectivityPolicy
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.core.model.PlaceSearchResult
import com.xanderjs.haptimap.core.model.distanceTo
import java.io.IOException
import kotlin.math.roundToInt
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/** Radio de la búsqueda por categoría: ~20 min a pie. */
const val NEARBY_RADIUS_METERS = 1_500.0

/** Radio de preferencia (no límite) de la búsqueda por texto. */
const val TEXT_BIAS_RADIUS_METERS = 3_000.0

/** Pocos resultados: en una pantalla redonda pequeña más de 10 no se recorren. */
const val MAX_RESULTS = 10

/** Búsqueda de lugares. [location] null (sin posición todavía) devuelve NO_LOCATION. */
interface PlacesRepository {
    suspend fun searchByCategory(category: PlaceCategory, location: GeoPoint?): PlaceSearchResult
    suspend fun searchByText(query: String, location: GeoPoint?): PlaceSearchResult
}

/**
 * Places API (New) por REST. [languageCode] se lee en cada búsqueda para seguir el idioma de la app
 * aunque cambie mientras vive el singleton.
 */
class GooglePlacesRepository(
    private val api: PlacesApi,
    private val connectivity: ConnectivityMonitor,
    private val languageCode: () -> String,
) : PlacesRepository {

    override suspend fun searchByCategory(category: PlaceCategory, location: GeoPoint?) =
        search(location) { center ->
            api.searchNearby(
                NearbyRequest(
                    includedTypes = category.placeTypes,
                    locationRestriction = Area(Circle(center, NEARBY_RADIUS_METERS)),
                    languageCode = languageCode(),
                    maxResultCount = MAX_RESULTS,
                ),
            ).places.map { it to category }
        }

    override suspend fun searchByText(query: String, location: GeoPoint?) =
        search(location) { center ->
            api.searchText(
                TextRequest(
                    textQuery = query,
                    locationBias = Area(Circle(center, TEXT_BIAS_RADIUS_METERS)),
                    languageCode = languageCode(),
                    pageSize = MAX_RESULTS,
                ),
            ).places.map { it to categoryForPlaceType(it.primaryType) }
        }

    private suspend fun search(
        location: GeoPoint?,
        request: suspend (LatLng) -> List<Pair<PlaceDto, PlaceCategory?>>,
    ): PlaceSearchResult {
        // sin red no se hace la petición (sección 17)
        if (!ConnectivityPolicy.canSearch(connectivity.status.value)) return PlaceSearchResult.Failure(PlaceSearchError.NO_CONNECTION)
        if (location == null) return PlaceSearchResult.Failure(PlaceSearchError.NO_LOCATION)
        val found = try {
            request(LatLng(location.latitude, location.longitude))
        } catch (e: HttpException) {
            return PlaceSearchResult.Failure(placesErrorFor(e.code(), e.response()?.errorBody()?.string()))
        } catch (e: IOException) {
            // el monitor aún no ha visto la caída (periodo de gracia) o la red falló a mitad
            return PlaceSearchResult.Failure(PlaceSearchError.NO_CONNECTION)
        } catch (e: SerializationException) {
            return PlaceSearchResult.Failure(PlaceSearchError.API_ERROR)
        }
        if (found.isEmpty()) return PlaceSearchResult.Failure(PlaceSearchError.NO_RESULTS)
        return PlaceSearchResult.Success(found.map { (dto, category) -> dto.toPlace(category, location) })
    }
}

private fun PlaceDto.toPlace(category: PlaceCategory?, from: GeoPoint): Place {
    val point = GeoPoint(location.latitude, location.longitude)
    return Place(
        id = id,
        name = displayName?.text.orEmpty(),
        location = point,
        category = category,
        address = shortFormattedAddress,
        distanceMeters = from.distanceTo(point).roundToInt(),
    )
}

/**
 * Errores de Google APIs: 429 = cuota agotada; 403 = clave sin permiso (restricción de app o API no
 * habilitada); 400 con API_KEY_INVALID = clave inexistente. Lo demás es un fallo genérico.
 */
fun placesErrorFor(httpCode: Int, errorBody: String?): PlaceSearchError = when {
    httpCode == 429 -> PlaceSearchError.API_QUOTA
    httpCode == 403 -> PlaceSearchError.API_INVALID_KEY
    httpCode == 400 && errorBody?.contains("API_KEY_INVALID") == true -> PlaceSearchError.API_INVALID_KEY
    else -> PlaceSearchError.API_ERROR
}
