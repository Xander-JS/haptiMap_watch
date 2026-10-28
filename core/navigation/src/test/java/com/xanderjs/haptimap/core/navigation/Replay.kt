package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.MeasuredPath
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.offset
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Rutas y recorridos sintéticos para reproducir en RouteTracker y NavigationEngine.

internal val ORIGIN = GeoPoint(40.4168, -3.7038)
internal const val WALK_SPEED = 1.4

/** Un tramo recto: [maneuver] al empezarlo y [meters] hacia [bearing]. El primero es la salida. */
internal data class Leg(val maneuver: Maneuver, val bearing: Double, val meters: Double)

/** Ruta con un vértice cada 10 m (como una polilínea real) y el paso de llegada al final. */
internal fun routeOf(vararg legs: Leg): Route = routeFrom(ORIGIN, *legs)

internal fun routeFrom(origin: GeoPoint, vararg legs: Leg): Route {
    val points = mutableListOf(origin)
    val steps = mutableListOf<RouteStep>()
    var start = origin
    for (leg in legs) {
        val pieces = ceil(leg.meters / 10).toInt()
        val stepPoints = (0..pieces).map { start.offset(leg.bearing, leg.meters * it / pieces) }
        points += stepPoints.drop(1)
        steps += RouteStep(leg.maneuver, leg.meters.toInt(), (leg.meters / WALK_SPEED).seconds, stepPoints, start, stepPoints.last(), "${leg.maneuver}", null)
        start = stepPoints.last()
    }
    steps += RouteStep(Maneuver.ARRIVAL, 0, Duration.ZERO, listOf(start), start, start, null, null)
    val total = legs.sumOf { it.meters }
    val destination = Place("dest", "Destination", start, null, null, total.toInt())
    return Route(destination, total.toInt(), (total / WALK_SPEED).seconds, points, steps)
}

internal fun fix(point: GeoPoint, millis: Long, accuracy: Float = 5f) = LocationFix(point, accuracy, WALK_SPEED.toFloat(), null, millis)

/** Un fix por segundo a [speed] desde [from] hasta [to] metros a lo largo de la ruta; [shift] desplaza cada punto. */
internal fun walk(
    route: Route,
    from: Double = 0.0,
    to: Double = MeasuredPath(route.points).length,
    speed: Double = WALK_SPEED,
    startMillis: Long = 0,
    shift: (index: Int, GeoPoint) -> GeoPoint = { _, p -> p },
): List<LocationFix> {
    val path = MeasuredPath(route.points)
    val count = ((to - from) / speed).toInt()
    return (0..count).map { i -> fix(shift(i, path.pointAt(from + i * speed)), startMillis + i * 1000L) }
}

internal fun RouteTracker.replay(fixes: List<LocationFix>): List<TrackingUpdate> = fixes.map(::update)

internal val List<TrackingUpdate>.cues get() = flatMap { it.cues }
