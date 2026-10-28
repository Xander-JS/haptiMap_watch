package com.xanderjs.haptimap.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

const val ROUTES_BASE_URL = "https://routes.googleapis.com/"

/** Solo los campos que usa Route: Routes API exige la máscara y cobra según lo pedido. */
const val ROUTES_FIELD_MASK =
    "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline," +
        "routes.legs.steps.distanceMeters,routes.legs.steps.staticDuration,routes.legs.steps.polyline.encodedPolyline," +
        "routes.legs.steps.startLocation,routes.legs.steps.endLocation,routes.legs.steps.navigationInstruction"

/** Routes API por REST. La clave y las cabeceras Android las pone [googleRetrofit]. */
interface RoutesApi {
    @Headers("X-Goog-FieldMask: $ROUTES_FIELD_MASK")
    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(@Body body: RoutesRequest): RoutesResponse
}

fun createRoutesApi(
    apiKey: String,
    packageName: String,
    certSha1: String,
    baseUrl: String = ROUTES_BASE_URL,
): RoutesApi = googleRetrofit(baseUrl, apiKey, packageName, certSha1).create(RoutesApi::class.java)

@Serializable
data class LocationDto(val latLng: LatLng)

/** Uno de los dos: [placeId] (de Places) o [location]. */
@Serializable
data class Waypoint(val location: LocationDto? = null, val placeId: String? = null)

@Serializable
data class RoutesRequest(
    val origin: Waypoint,
    val destination: Waypoint,
    val languageCode: String,
    val computeAlternativeRoutes: Boolean,
    val travelMode: String = "WALK",
    val units: String = "METRIC",
)

/** Routes devuelve `{}` cuando no hay ruta; proto3 omite los campos a 0. */
@Serializable
data class RoutesResponse(val routes: List<RouteDto> = emptyList())

@Serializable
data class RouteDto(
    val distanceMeters: Int = 0,
    val duration: String? = null,
    val polyline: PolylineDto? = null,
    val legs: List<LegDto> = emptyList(),
)

@Serializable
data class LegDto(val steps: List<StepDto> = emptyList())

@Serializable
data class StepDto(
    val distanceMeters: Int = 0,
    val staticDuration: String? = null,
    val polyline: PolylineDto? = null,
    val startLocation: LocationDto? = null,
    val endLocation: LocationDto? = null,
    val navigationInstruction: NavigationInstructionDto? = null,
)

@Serializable
data class PolylineDto(val encodedPolyline: String = "")

@Serializable
data class NavigationInstructionDto(val maneuver: String? = null, val instructions: String? = null)
