package com.xanderjs.haptimap.core.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticPatternTest {

    @Test
    fun `intensity scales amplitudes, keeps silences and never drops a pulse or passes 255`() {
        val pattern = HapticPattern(longArrayOf(100, 50, 100, 50, 100), intArrayOf(200, 0, 2, 0, 160))
        assertArrayEquals(intArrayOf(100, 0, 1, 0, 80), pattern.scaled(0.5f).amplitudes)
        assertArrayEquals(intArrayOf(255, 0, 3, 0, 240), pattern.scaled(1.5f).amplitudes)
        assertArrayEquals(pattern.timings, pattern.scaled(0.5f).timings)
    }

    @Test
    fun `without amplitude control the pattern becomes off-on timings and contiguous segments merge`() {
        val swell = HapticTokens.patternFor(HapticEvent.BACK_ON_ROUTE)
        assertArrayEquals(longArrayOf(0, 400), swell.onOffTimings())
        assertArrayEquals(longArrayOf(0, 200, 250, 200), HapticTokens.patternFor(HapticEvent.TURN_LEFT).onOffTimings())
        // un patrón que empieza en silencio y acaba en silencio: la pausa final sobra
        assertArrayEquals(longArrayOf(30, 100), HapticPattern(longArrayOf(30, 100, 40), intArrayOf(0, 90, 0)).onOffTimings())
    }

    @Test
    fun `every pattern keeps the counts of the haptic language`() {
        val pulses = HapticEvent.entries.associateWith { HapticTokens.patternFor(it).pulseCount }
        assertEquals(2, pulses[HapticEvent.TURN_LEFT])
        assertEquals(3, pulses[HapticEvent.TURN_RIGHT])
        assertEquals(1, pulses[HapticEvent.CONTINUE])
        assertEquals(1, pulses[HapticEvent.OFF_ROUTE])
        assertEquals(1, pulses[HapticEvent.BACK_ON_ROUTE])
        assertEquals(3, pulses[HapticEvent.ARRIVAL])
        assertEquals(3, pulses[HapticEvent.POI_ARRIVAL])
    }

    @Test
    fun `left and right differ in rhythm, not only in count`() {
        val left = HapticTokens.patternFor(HapticEvent.TURN_LEFT).onOffTimings()
        val right = HapticTokens.patternFor(HapticEvent.TURN_RIGHT).onOffTimings()
        // el pulso de la izquierda dura más del doble que el de la derecha
        assertTrue(left[1] >= 2 * right[1])
    }
}
