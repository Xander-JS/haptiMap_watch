package com.xanderjs.haptimap.core.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// Geometría a escala peatonal (paso 07). Las proyecciones usan un plano local (equirectangular)
// centrado en el segmento: en tramos de cientos de metros el error es de centímetros.

/** Rumbo inicial de este punto a [other], en grados [0, 360): 0 norte, 90 este. */
fun GeoPoint.bearingTo(other: GeoPoint): Double {
    val lat1 = Math.toRadians(latitude)
    val lat2 = Math.toRadians(other.latitude)
    val dLon = Math.toRadians(other.longitude - longitude)
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}

/** El punto a [meters] en dirección [bearingDegrees] (plano local). Para el simulador y los tests. */
fun GeoPoint.offset(bearingDegrees: Double, meters: Double): GeoPoint {
    val bearing = Math.toRadians(bearingDegrees)
    val north = meters * cos(bearing)
    val east = meters * sin(bearing)
    return GeoPoint(
        latitude + Math.toDegrees(north / EARTH_RADIUS_METERS),
        longitude + Math.toDegrees(east / (EARTH_RADIUS_METERS * cos(Math.toRadians(latitude)))),
    )
}

/** El punto a [fraction] (0..1) del camino entre [a] y [b]. */
fun interpolate(a: GeoPoint, b: GeoPoint, fraction: Double) = GeoPoint(
    a.latitude + (b.latitude - a.latitude) * fraction,
    a.longitude + (b.longitude - a.longitude) * fraction,
)

/** Dónde cae un punto sobre un segmento: [fraction] 0..1 desde el inicio y la distancia al punto más cercano. */
data class SegmentProjection(val fraction: Double, val distanceMeters: Double)

/** Proyecta este punto sobre el segmento [a]–[b]; fuera del segmento se queda en el extremo más cercano. */
fun GeoPoint.projectOnto(a: GeoPoint, b: GeoPoint): SegmentProjection {
    val metersPerLon = Math.toRadians(1.0) * EARTH_RADIUS_METERS * cos(Math.toRadians(a.latitude))
    val metersPerLat = Math.toRadians(1.0) * EARTH_RADIUS_METERS
    val bx = (b.longitude - a.longitude) * metersPerLon
    val by = (b.latitude - a.latitude) * metersPerLat
    val px = (longitude - a.longitude) * metersPerLon
    val py = (latitude - a.latitude) * metersPerLat
    val lengthSquared = bx * bx + by * by
    val fraction = if (lengthSquared == 0.0) 0.0 else ((px * bx + py * by) / lengthSquared).coerceIn(0.0, 1.0)
    return SegmentProjection(fraction, hypot(px - bx * fraction, py - by * fraction))
}

/**
 * Una polilínea con la distancia acumulada hasta cada vértice, para hablar de posiciones "a lo largo"
 * de la ruta en metros. [cumulative][i] es la distancia del inicio al vértice i.
 */
class MeasuredPath(val points: List<GeoPoint>) {
    val cumulative: DoubleArray = DoubleArray(points.size).also {
        for (i in 1 until points.size) it[i] = it[i - 1] + points[i - 1].distanceTo(points[i])
    }
    val length: Double get() = cumulative.lastOrNull() ?: 0.0
    val segmentCount: Int get() = (points.size - 1).coerceAtLeast(0)

    /** El punto a [meters] del inicio, recortado a la polilínea. */
    fun pointAt(meters: Double): GeoPoint {
        if (segmentCount == 0) return points.first()
        val clamped = meters.coerceIn(0.0, length)
        val i = segmentAt(clamped)
        val segmentLength = cumulative[i + 1] - cumulative[i]
        val fraction = if (segmentLength == 0.0) 0.0 else (clamped - cumulative[i]) / segmentLength
        return interpolate(points[i], points[i + 1], fraction)
    }

    /** El segmento que contiene la posición [meters] (el último si está al final). */
    fun segmentAt(meters: Double): Int {
        var low = 0
        var high = segmentCount - 1
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (cumulative[mid] <= meters) low = mid else high = mid - 1
        }
        return low
    }

    /** Posición a lo largo (m) y distancia de [point] proyectado sobre el segmento [segment]. */
    fun project(point: GeoPoint, segment: Int): Pair<Double, Double> {
        val p = point.projectOnto(points[segment], points[segment + 1])
        return cumulative[segment] + p.fraction * (cumulative[segment + 1] - cumulative[segment]) to p.distanceMeters
    }
}
