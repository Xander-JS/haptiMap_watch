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
