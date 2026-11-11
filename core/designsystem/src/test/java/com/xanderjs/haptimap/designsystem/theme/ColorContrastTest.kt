package com.xanderjs.haptimap.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance
import com.xanderjs.haptimap.designsystem.tokens.HaptiColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Accesibilidad de la paleta Mapa en los dos temas: texto ≥ 4.5:1 (WCAG AA), contornos de tarjetas y botones
 * ≥ 3:1 contra el fondo (WCAG 1.4.11), y cada color semántico lejos de los demás para que ninguno se confunda con el
 * azul de la marca ni con el verde del Tour.
 */
class ColorContrastTest {
    private val themes = mapOf("dark" to DarkHaptiColors, "light" to LightHaptiColors)

    @Test
    fun `text and accents reach AA on background and surface`() = themes.forEach { (name, c) ->
        val onBoth = listOf("onBackground" to c.onBackground, "onSurfaceVariant" to c.onSurfaceVariant, "warning" to c.warning,
            "offRoute" to c.offRoute, "tour" to c.tour, "navigationArrow" to c.navigationArrow)
        onBoth.forEach { (role, color) ->
            assertContrast("$name $role/background", color, c.background, AA_TEXT)
            assertContrast("$name $role/surface", color, c.surface, AA_TEXT)
        }
        assertContrast("$name onPrimary/primary", c.onPrimary, c.primary, AA_TEXT)
        assertContrast("$name onTour/tour", c.onTour, c.tour, AA_TEXT)
        assertContrast("$name background/offRoute", c.background, c.offRoute, AA_TEXT)
    }

    @Test
    fun `card and button boundaries reach 3 to 1`() = themes.forEach { (name, c) ->
        assertContrast("$name outline/background", c.outline, c.background, NON_TEXT)
        assertContrast("$name outline/surface", c.outline, c.surface, NON_TEXT)
        assertContrast("$name primary/background", c.primary, c.background, NON_TEXT)
        assertContrast("$name tour/background", c.tour, c.background, NON_TEXT)
    }

    @Test
    fun `each semantic color is clearly distinct from the others`() = themes.forEach { (name, c) ->
        val semantic = semantic(c)
        semantic.forEachIndexed { i, (a, ca) ->
            semantic.drop(i + 1).forEach { (b, cb) ->
                val distance = deltaE(ca, cb)
                assertTrue("$name $a vs $b: ΔE $distance", distance >= MIN_DELTA_E)
            }
        }
    }

    private fun semantic(c: HaptiColors) =
        listOf("primary" to c.primary, "tour" to c.tour, "warning" to c.warning, "offRoute" to c.offRoute)

    private fun assertContrast(what: String, a: Color, b: Color, min: Double) {
        val ratio = (max(a.luminance(), b.luminance()) + 0.05) / (min(a.luminance(), b.luminance()) + 0.05)
        assertTrue("$what: ${"%.2f".format(ratio)} < $min", ratio >= min)
    }

    /** Distancia CIE76 en Lab: por encima de ~10 dos colores ya se distinguen sin compararlos lado a lado. */
    private fun deltaE(a: Color, b: Color): Float {
        val la = a.convert(ColorSpaces.CieLab)
        val lb = b.convert(ColorSpaces.CieLab)
        val dl = la.red - lb.red
        val da = la.green - lb.green
        val db = la.blue - lb.blue
        return sqrt(dl * dl + da * da + db * db)
    }

    private companion object {
        const val AA_TEXT = 4.5
        const val NON_TEXT = 3.0
        const val MIN_DELTA_E = 30f
    }
}
