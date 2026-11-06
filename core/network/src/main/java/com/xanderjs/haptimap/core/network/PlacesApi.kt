package com.xanderjs.haptimap.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

const val PLACES_BASE_URL = "https://places.googleapis.com/"

/** Solo los campos que usamos: Places (New) cobra por SKU según los campos pedidos. */
const val PLACES_FIELD_MASK =
    "places.id,places.displayName,places.location,places.primaryType,places.shortFormattedAddress"

/**
 * Máscara mínima para los POIs del Modo Tour (paso 17): lo justo que necesita el PoiMapper (id, nombre,
 * posición y tipo). Sin dirección: cae en el SKU Pro de Nearby Search. Añadir rating/userRatingCount lo
 * subiría al SKU Enterprise (más caro), así que no se piden; el evaluador puntúa por relevancia y cercanía.
 */
const val POI_FIELD_MASK = "places.id,places.displayName,places.location,places.primaryType"

/** Endpoints REST de Places API (New). La clave y las cabeceras Android las pone [googleRetrofit]. */
interface PlacesApi {
    @Headers("X-Goog-FieldMask: $PLACES_FIELD_MASK")
    @POST("v1/places:searchNearby")
    suspend fun searchNearby(@Body body: NearbyRequest): PlacesResponse

    /** Nearby Search para POIs del Tour, con la máscara mínima [POI_FIELD_MASK]. */
    @Headers("X-Goog-FieldMask: $POI_FIELD_MASK")
    @POST("v1/places:searchNearby")
    suspend fun searchNearbyPois(@Body body: NearbyRequest): PlacesResponse

    @Headers("X-Goog-FieldMask: $PLACES_FIELD_MASK")
    @POST("v1/places:searchText")
    suspend fun searchText(@Body body: TextRequest): PlacesResponse
}

fun createPlacesApi(
    apiKey: String,
    packageName: String,
    certSha1: String,
    baseUrl: String = PLACES_BASE_URL,
): PlacesApi = googleRetrofit(baseUrl, apiKey, packageName, certSha1).create(PlacesApi::class.java)

@Serializable
data class LatLng(val latitude: Double, val longitude: Double)

@Serializable
data class Circle(val center: LatLng, val radius: Double)

@Serializable
data class Area(val circle: Circle)

@Serializable
data class NearbyRequest(
    val includedTypes: List<String>,
    val locationRestriction: Area,
    val languageCode: String,
    val maxResultCount: Int,
    val rankPreference: String = "DISTANCE",
)

@Serializable
data class TextRequest(
    val textQuery: String,
    val locationBias: Area,
    val languageCode: String,
    val pageSize: Int,
)

/** Places (New) devuelve `{}` cuando no hay resultados. */
@Serializable
data class PlacesResponse(val places: List<PlaceDto> = emptyList())

@Serializable
data class PlaceDto(
    val id: String,
    val displayName: LocalizedText? = null,
    val location: LatLng,
    val primaryType: String? = null,
    val shortFormattedAddress: String? = null,
)

@Serializable
data class LocalizedText(val text: String)
