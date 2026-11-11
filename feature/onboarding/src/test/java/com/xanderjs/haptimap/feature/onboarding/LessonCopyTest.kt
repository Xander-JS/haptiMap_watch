package com.xanderjs.haptimap.feature.onboarding

import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticTokens
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los textos cortos del tutorial describen el patrón que de verdad suena (rediseño): en inglés y en español, los
 * números de cada lección suman los pulsos del patrón de HapticTokens ("1 long pulse, 2 taps" = 3). Si el paso 14
 * recalibra un patrón, este test obliga a corregir el texto.
 */
class LessonCopyTest {
    private val meaningKeys = mapOf(
        HapticEvent.TURN_LEFT to "lesson_turn_left_meaning",
        HapticEvent.TURN_RIGHT to "lesson_turn_right_meaning",
        HapticEvent.CONTINUE to "lesson_continue_meaning",
        HapticEvent.TURN_IMMINENT to "lesson_imminent_meaning",
        HapticEvent.OFF_ROUTE to "lesson_off_route_meaning",
        HapticEvent.POI_ARRIVAL to "lesson_poi_arrival_meaning",
        HapticEvent.ARRIVAL to "lesson_arrival_meaning",
    )

    @Test
    fun `every lesson is covered`() {
        assertEquals(hapticLessons.map { it.event }.toSet(), meaningKeys.keys)
    }

    @Test
    fun `stated pulse counts match the real patterns in both languages`() {
        listOf("values", "values-es").forEach { dir ->
            val xml = File("src/main/res/$dir/strings.xml").readText()
            meaningKeys.forEach { (event, key) ->
                val text = Regex("""<string name="$key">(.*?)</string>""").find(xml)?.groupValues?.get(1)
                    ?: error("$key missing in $dir")
                val stated = Regex("""\d+""").findAll(text).sumOf { it.value.toInt() }
                assertEquals("$dir/$key: \"$text\"", HapticTokens.patternFor(event).pulseCount, stated)
            }
        }
    }

    @Test
    fun `left is long and slow, right short and quick, off route the longest`() {
        fun longestPulse(event: HapticEvent) = HapticTokens.patternFor(event).onOffTimings().filterIndexed { i, _ -> i % 2 == 1 }.max()
        assertTrue(longestPulse(HapticEvent.TURN_LEFT) > longestPulse(HapticEvent.TURN_RIGHT))
        assertTrue(HapticEvent.entries.all { longestPulse(HapticEvent.OFF_ROUTE) >= longestPulse(it) })
    }
}
