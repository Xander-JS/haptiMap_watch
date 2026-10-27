package com.xanderjs.haptimap.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

const val PLACES_BASE_URL = "https://places.googleapis.com/"

/** Solo los campos que usamos: Places (New) cobra por SKU según los campos pedidos. */
const val PLACES_FIELD_MASK =
    "places.id,places.displayName,places.location,places.primaryType,places.shortFormattedAddress"

/** Endpoints REST de Places API (New). La clave y las cabeceras Android las pone [createPlacesApi]. */
interface PlacesApi {
    @Headers("X-Goog-FieldMask: $PLACES_FIELD_MASK")
    @POST("v1/places:searchNearby")
    suspend fun searchNearby(@Body body: NearbyRequest): PlacesResponse

    @Headers("X-Goog-FieldMask: $PLACES_FIELD_MASK")
    @POST("v1/places:searchText")
    suspend fun searchText(@Body body: TextRequest): PlacesResponse
}

/**
 * [certSha1] es la huella SHA-1 de la firma en hex sin separadores: con la clave restringida a la app
 * Android, Google la compara junto con el paquete en cada petición.
 */
fun createPlacesApi(
    apiKey: String,
    packageName: String,
    certSha1: String,
    baseUrl: String = PLACES_BASE_URL,
): PlacesApi {
    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Android-Package", packageName)
                    .header("X-Android-Cert", certSha1)
                    .build(),
            )
        }
        .build()
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(placesJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(PlacesApi::class.java)
}

// encodeDefaults: rankPreference y maxResultCount tienen valor por defecto y deben viajar
internal val placesJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

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
