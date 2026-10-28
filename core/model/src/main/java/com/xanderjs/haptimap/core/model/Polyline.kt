package com.xanderjs.haptimap.core.model

/**
 * Decodifica una polilínea codificada de Google (precisión 1e5). Una cadena truncada devuelve los
 * puntos completos que haya, sin lanzar: viene de la red.
 */
fun decodePolyline(encoded: String): List<GeoPoint> {
    val points = mutableListOf<GeoPoint>()
    var index = 0
    var lat = 0
    var lng = 0

    // cada valor: grupos de 5 bits + 63, el bit 0x20 indica que sigue otro grupo; el bit 0 es el signo
    fun nextValue(): Int? {
        var result = 0
        var shift = 0
        while (index < encoded.length) {
            val chunk = encoded[index++].code - 63
            result = result or ((chunk and 0x1f) shl shift)
            shift += 5
            if (chunk < 0x20) return if (result and 1 != 0) (result shr 1).inv() else result shr 1
        }
        return null
    }

    while (true) {
        lat += nextValue() ?: break
        lng += nextValue() ?: break
        points += GeoPoint(lat / 1e5, lng / 1e5)
    }
    return points
}

/** Codifica [points] con el mismo algoritmo (precisión 1e5): lo inverso de [decodePolyline]. */
fun encodePolyline(points: List<GeoPoint>): String = buildString {
    var lastLat = 0
    var lastLng = 0

    fun put(delta: Int) {
        var value = if (delta < 0) (delta shl 1).inv() else delta shl 1
        while (value >= 0x20) {
            append(((0x20 or (value and 0x1f)) + 63).toChar())
            value = value ushr 5
        }
        append((value + 63).toChar())
    }

    for (point in points) {
        val lat = Math.round(point.latitude * 1e5).toInt()
        val lng = Math.round(point.longitude * 1e5).toInt()
        put(lat - lastLat)
        put(lng - lastLng)
        lastLat = lat
        lastLng = lng
    }
}
