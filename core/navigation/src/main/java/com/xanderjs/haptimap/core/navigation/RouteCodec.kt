package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.decodePolyline
import com.xanderjs.haptimap.core.model.encodePolyline
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.serialization.Serializable

// (De)serialización de una Route a JSON, compartida por la caché de ruta (paso 06) y la del desvío (paso 20).
// Formato en disco propio: no depende de cómo cambie el dominio. Las geometrías van como polilínea codificada
// (precisión 1e5, la misma con la que llegan de Google); solo el destino y el inicio/fin de cada paso van sueltos.

@Serializable
data class CachedRoute(
    val destination: CachedPlace,
    val distanceMeters: Int,
    val durationMillis: Long,
    val polyline: String,
    val steps: List<CachedStep>,
)

@Serializable
data class CachedPlace(
    val id: String,
    val name: String,
    val location: CachedPoint,
    val category: PlaceCategory?,
    val address: String?,
    val distanceMeters: Int,
)

@Serializable
data class CachedStep(
    val maneuver: Maneuver,
    val distanceMeters: Int,
    val durationMillis: Long,
    val polyline: String,
    val start: CachedPoint,
    val end: CachedPoint,
    val instruction: String?,
    val sourceManeuver: String?,
)

@Serializable
data class CachedPoint(val latitude: Double, val longitude: Double)

fun Route.toCached() = CachedRoute(
    CachedPlace(destination.id, destination.name, destination.location.toCached(), destination.category, destination.address, destination.distanceMeters),
    distanceMeters,
    duration.inWholeMilliseconds,
    encodePolyline(points),
    steps.map {
        CachedStep(it.maneuver, it.distanceMeters, it.duration.inWholeMilliseconds, encodePolyline(it.points), it.start.toCached(), it.end.toCached(), it.instruction, it.sourceManeuver)
    },
)

fun CachedRoute.toRoute() = Route(
    Place(destination.id, destination.name, destination.location.toGeoPoint(), destination.category, destination.address, destination.distanceMeters),
    distanceMeters,
    durationMillis.milliseconds,
    decodePolyline(polyline),
    steps.map {
        RouteStep(it.maneuver, it.distanceMeters, it.durationMillis.milliseconds, decodePolyline(it.polyline), it.start.toGeoPoint(), it.end.toGeoPoint(), it.instruction, it.sourceManeuver)
    },
)

fun GeoPoint.toCached() = CachedPoint(latitude, longitude)

fun CachedPoint.toGeoPoint() = GeoPoint(latitude, longitude)
