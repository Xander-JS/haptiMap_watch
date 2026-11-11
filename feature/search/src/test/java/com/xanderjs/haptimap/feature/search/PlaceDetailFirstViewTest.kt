package com.xanderjs.haptimap.feature.search

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regla de la primera vista del detalle de un lugar, en las referencias de las capturas (la primera vista): en cada
 * forma, tema, idioma y tamaño de letra, el botón "Ir" (una franja primary de al menos un 25 % del ancho que acaba
 * en el 15 % inferior) y, encima y separado de él, el disco de la categoría entero (otra franja primary).
 * Tras updateDebugScreenshotTest, si algo empuja el disco bajo el botón, este test falla.
 */
class PlaceDetailFirstViewTest {
    private val dir = File("src/screenshotTestDebug/reference/com/xanderjs/haptimap/feature/search/PlaceDetailScreenshotsKt")

    // primary: Blue75 en oscuro, Blue40 en claro (paleta Mapa)
    private val primaries = listOf(0x7FAEFF, 0x1A5FD0)

    private fun isPrimary(rgb: Int) = primaries.any { p ->
        abs((rgb shr 16 and 0xFF) - (p shr 16 and 0xFF)) + abs((rgb shr 8 and 0xFF) - (p shr 8 and 0xFF)) + abs((rgb and 0xFF) - (p and 0xFF)) < 40
    }

    private fun bands(image: BufferedImage, minFraction: Double): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var start = -1
        for (y in 0..image.height) {
            val on = y < image.height && (0 until image.width).count { x -> isPrimary(image.getRGB(x, y)) } >= image.width * minFraction
            if (on && start < 0) start = y
            if (!on && start >= 0) { out += start until y; start = -1 }
        }
        return out
    }

    @Test
    fun `glance and go button are fully visible on every shape, theme, language and font size`() {
        val files = dir.listFiles { f -> f.name.endsWith(".png") }!!.sortedBy { it.name }
        assertEquals("5 states × 12 combinations + 3 at the largest font × 12", 96, files.size)
        for (file in files) {
            val image = ImageIO.read(file)
            val button = bands(image, 0.25).lastOrNull()
            assertTrue("${file.name}: no Go button in the first view", button != null && button.count() >= 40)
            assertTrue("${file.name}: Go button ends at ${button!!.last} of ${image.height}", button.last >= image.height * 0.85)
            // el disco: franjas más estrechas (el icono blanco lo parte en varias) por encima del botón y sin tocarlo
            val disc = bands(image, 0.08).filter { it.last < button.first - 1 }
            val discHeight = if (disc.isEmpty()) 0 else disc.last().last - disc.first().first + 1
            assertTrue("${file.name}: category disc hidden or cut by the Go button", discHeight >= image.height * 0.12)
        }
    }

    @Test
    fun `walking time is rounded up and never zero`() {
        assertEquals(1, walkingMinutes(0))
        assertEquals(1, walkingMinutes(80))
        assertEquals(2, walkingMinutes(81))
        assertEquals(12, walkingMinutes(960))
    }
}
