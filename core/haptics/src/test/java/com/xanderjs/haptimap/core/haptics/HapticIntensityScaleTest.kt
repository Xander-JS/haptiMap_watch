package com.xanderjs.haptimap.core.haptics

import com.xanderjs.haptimap.core.model.HapticIntensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticIntensityScaleTest {

    @Test
    fun `medium keeps the patterns unchanged and intensity grows with the level`() {
        assertEquals(1.0f, HapticIntensity.MEDIUM.amplitudeScale)
        assertTrue(HapticIntensity.LOW.amplitudeScale < HapticIntensity.MEDIUM.amplitudeScale)
        assertTrue(HapticIntensity.MEDIUM.amplitudeScale < HapticIntensity.HIGH.amplitudeScale)
    }
}
