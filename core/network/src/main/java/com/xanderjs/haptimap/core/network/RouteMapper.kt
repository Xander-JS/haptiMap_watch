package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.decodePolyline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Todas las rutas de la respuesta como [Route] (paso 21). Los campos que falten no fallan: la posición de un paso
 * sale de su polilínea, o del final del paso anterior (el primero, de [origin]).
 */
internal fun RoutesResponse.toRoutes(destination: Place, origin: GeoPoint): List<Route> =
    routes.map { dto -> dto.toRoute(destination, origin) }

/** Decodifica un JSON de Routes API en todas sus [Route] candidatas (usado en tests de integración con fixtures reales). */
fun parseRoutesResponse(json: String, destination: Place, origin: GeoPoint): List<Route> =
    googleJson.decodeFromString<RoutesResponse>(json).toRoutes(destination, origin)

/** La primera ruta de la respuesta como [Route], o null si no hay ninguna. */
internal fun RoutesResponse.toRoute(destination: Place, origin: GeoPoint): Route? =
    toRoutes(destination, origin).firstOrNull()

private fun RouteDto.toRoute(destination: Place, origin: GeoPoint): Route {
    var at = origin
    val steps = legs.flatMap { it.steps }.map { step ->
        val points = decodePolyline(step.polyline?.encodedPolyline.orEmpty())
        val start = step.startLocation?.point ?: points.firstOrNull() ?: at
        val end = step.endLocation?.point ?: points.lastOrNull() ?: start
        at = end
        RouteStep(
            maneuver = maneuverFor(step.navigationInstruction?.maneuver),
            distanceMeters = step.distanceMeters,
            duration = parseDuration(step.staticDuration),
            points = points,
            start = start,
            end = end,
            instruction = step.navigationInstruction?.instructions,
            sourceManeuver = step.navigationInstruction?.maneuver,
        )
    }
    val arrival = RouteStep(Maneuver.ARRIVAL, 0, Duration.ZERO, listOf(at), at, at, null, null)
    return Route(
        destination = destination,
        distanceMeters = distanceMeters,
        duration = parseDuration(duration),
        points = decodePolyline(polyline?.encodedPolyline.orEmpty()),
        steps = steps + arrival,
    )
}

/**
 * Maniobra de Google a semántica. Todo `*_LEFT` / `*_RIGHT` (suave, cerrado, cambio de sentido,
 * bifurcación, rampa, rotonda) es un giro; lo demás (STRAIGHT, DEPART, NAME_CHANGE, MERGE,
 * desconocida o ausente) es seguir.
 */
internal fun maneuverFor(google: String?): Maneuver = when {
    google == null -> Maneuver.CONTINUE
    google.endsWith("_LEFT") -> Maneuver.TURN_LEFT
    google.endsWith("_RIGHT") -> Maneuver.TURN_RIGHT
    else -> Maneuver.CONTINUE
}

/** Las duraciones de Google son texto en segundos: "754s", "12.5s". */
internal fun parseDuration(value: String?): Duration = value?.removeSuffix("s")?.toDoubleOrNull()?.seconds ?: Duration.ZERO

/**
 * Destino de la petición: el place ID de Places cuando lo hay, porque Google lleva la ruta a la entrada
 * del lugar; si no, sus coordenadas.
 */
internal fun Place.toWaypoint(): Waypoint =
    if (id.isNotBlank()) Waypoint(placeId = id) else Waypoint(LocationDto(LatLng(location.latitude, location.longitude)))

private val LocationDto.point get() = GeoPoint(latLng.latitude, latLng.longitude)
