package com.xanderjs.haptimap.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PolylineTest {

    private fun assertPoints(expected: List<Pair<Double, Double>>, actual: List<GeoPoint>) {
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (e, a) ->
            assertEquals(e.first, a.latitude, 1e-6)
            assertEquals(e.second, a.longitude, 1e-6)
        }
    }

    @Test
    fun `decodes the example from Google's documentation`() {
        assertPoints(
            listOf(38.5 to -120.2, 40.7 to -120.95, 43.252 to -126.453),
            decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@"),
        )
    }

    @Test
    fun `decodes a single point with negative coordinates`() {
        // Puerta del Sol, codificada con el algoritmo de referencia
        assertPoints(listOf(40.41678 to -3.70379), decodePolyline("{{tuFtkrU"))
    }

    @Test
    fun `empty string has no points`() {
        assertEquals(emptyList<GeoPoint>(), decodePolyline(""))
    }

    @Test
    fun `truncated input keeps the complete points and does not throw`() {
        assertPoints(listOf(38.5 to -120.2), decodePolyline("_p~iF~ps|U_ulL"))
    }

    @Test
    fun `encodes Google's example and Sol, and decoding gives the same points back`() {
        val example = listOf(GeoPoint(38.5, -120.2), GeoPoint(40.7, -120.95), GeoPoint(43.252, -126.453))
        assertEquals("_p~iF~ps|U_ulLnnqC_mqNvxq`@", encodePolyline(example))
        assertEquals("{{tuFtkrU", encodePolyline(listOf(GeoPoint(40.41678, -3.70379))))
        assertEquals(example, decodePolyline(encodePolyline(example)))
        assertEquals("", encodePolyline(emptyList()))
    }
}
