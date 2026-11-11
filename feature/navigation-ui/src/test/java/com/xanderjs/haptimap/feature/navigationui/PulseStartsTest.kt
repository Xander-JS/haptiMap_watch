package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticTokens
import org.junit.Assert.assertEquals
import org.junit.Test

/** Los anillos de la maniobra laten al ritmo del patrón real: un anillo por pulso, cuando empieza cada uno. */
class PulseStartsTest {
    @Test
    fun `left and right start each pulse at its own rhythm`() {
        assertEquals(listOf(0L, 450L), HapticTokens.patternFor(HapticEvent.TURN_LEFT).pulseStartsMs())
        assertEquals(listOf(0L, 180L, 360L), HapticTokens.patternFor(HapticEvent.TURN_RIGHT).pulseStartsMs())
    }

    @Test
    fun `a continuous rise is one pulse`() {
        assertEquals(listOf(0L), HapticTokens.patternFor(HapticEvent.BACK_ON_ROUTE).pulseStartsMs())
    }

    @Test
    fun `one ring per felt pulse for every event`() {
        HapticEvent.entries.forEach { event ->
            val pattern = HapticTokens.patternFor(event)
            assertEquals(event.name, pattern.pulseCount, pattern.pulseStartsMs().size)
        }
    }
}
