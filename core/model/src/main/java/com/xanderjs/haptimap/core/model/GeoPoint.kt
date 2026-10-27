package com.xanderjs.haptimap.core.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Radio medio de la Tierra (IUGG). El error de la esfera (<0,5 %) no importa a distancias peatonales. */
const val EARTH_RADIUS_METERS = 6_371_008.8

/** Coordenada geográfica en grados decimales (WGS84). */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

/** Distancia en línea recta (haversine) en metros. */
fun GeoPoint.distanceTo(other: GeoPoint): Double {
    val dLat = Math.toRadians(other.latitude - latitude)
    val dLon = Math.toRadians(other.longitude - longitude)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
}
