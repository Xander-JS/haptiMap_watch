package com.xanderjs.haptimap.feature.onboarding

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Paso 13: en cada pantalla del tutorial la acción principal (Empezar, Siguiente, Buscar) se ve sin hacer scroll, en
 * las tres formas, los dos temas y los dos idiomas. Lo comprueba en las referencias de las capturas, que son la
 * primera vista: una franja del color primary (Blue75 oscuro, Blue40 claro) de al menos 40 px de alto y un 25 % del
 * ancho que acaba en el 15 % inferior. Y en "Gira ya", el significado sigue en la primera vista: paso, distancia,
 * título y significado son 4 franjas de texto entre la hora y el botón.
 * Tras updateDebugScreenshotTest, si algo empuja el botón fuera de la pantalla, este test falla. Desde el paso 15A
 * incluye las referencias con la letra del sistema más grande (LargeFont…).
 */
class TutorialFirstViewTest {

    private val dir = File("src/screenshotTestDebug/reference/com/xanderjs/haptimap/feature/onboarding/TutorialScreenshotsKt")
    private val primaries = listOf(0x7FAEFF, 0x1A5FD0)

    private fun references(): List<File> = dir.listFiles { f -> f.name.endsWith(".png") }!!.sortedBy { it.name }

    private fun isPrimary(rgb: Int) = primaries.any { p ->
        abs((rgb shr 16 and 0xFF) - (p shr 16 and 0xFF)) + abs((rgb shr 8 and 0xFF) - (p shr 8 and 0xFF)) + abs((rgb and 0xFF) - (p and 0xFF)) < 40
    }

    /** Filas seguidas que cumplen [row], de arriba abajo. */
    private fun bands(image: BufferedImage, row: (Int) -> Boolean): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var start = -1
        for (y in 0..image.height) {
            val on = y < image.height && row(y)
            if (on && start < 0) start = y
            if (!on && start >= 0) { out += start until y; start = -1 }
        }
        return out
    }

    /** La franja del botón principal: la más baja con primary en al menos un 25 % del ancho (30 % hasta el paso 15A: a 1,24 en la redonda grande la etiqueta tapa más). */
    private fun primaryButton(image: BufferedImage): IntRange? = bands(image) { y ->
        (0 until image.width).count { x -> isPrimary(image.getRGB(x, y)) } >= image.width * 0.25
    }.lastOrNull()

    @Test
    fun `every tutorial screen shows its primary action without scrolling`() {
        val files = references()
        // paso 15A: también las 4 pantallas con la letra más grande (LargeFont…), donde el botón debe seguir a la vista
        assertEquals("12 screens × 12 combinations + 4 at the largest font × 12", 192, files.size)
        for (file in files) {
            val image = ImageIO.read(file)
            val button = primaryButton(image)
            assertTrue("${file.name}: no primary button in the first view", button != null)
            assertTrue("${file.name}: button only ${button!!.count()} px tall", button.count() >= 40)
            assertTrue("${file.name}: button ends at ${button.last} of ${image.height}", button.last >= image.height * 0.85)
        }
    }

    @Test
    fun `turn now shows its meaning above the button on every shape and language`() {
        for (file in references().filter { it.name.startsWith("TutorialImminent_") }) {
            val image = ImageIO.read(file)
            val buttonTop = primaryButton(image)?.first ?: error("${file.name}: no primary button in the first view")
            // el fondo, dentro de la pantalla también en las redondas: a la izquierda, a media altura
            val background = image.getRGB(image.width / 10, image.height / 2)
            val text = bands(image) { y ->
                y < buttonTop && (image.width / 6 until image.width * 5 / 6).any { x ->
                    val rgb = image.getRGB(x, y)
                    abs((rgb shr 16 and 0xFF) - (background shr 16 and 0xFF)) + abs((rgb shr 8 and 0xFF) - (background shr 8 and 0xFF)) > 120
                }
            }
            // la primera franja es la hora
            assertTrue("${file.name}: ${text.size - 1} text bands above the button, expected 4 (step, distance, title, meaning)", text.size - 1 >= 4)
        }
    }
}
