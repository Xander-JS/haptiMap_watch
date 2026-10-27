package com.xanderjs.haptimap.core.model

/** Lectura de posición entregada por el Location Engine. */
data class LocationFix(
    val point: GeoPoint,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float?,
    val bearingDegrees: Float?,
    val timestampMillis: Long,
)
