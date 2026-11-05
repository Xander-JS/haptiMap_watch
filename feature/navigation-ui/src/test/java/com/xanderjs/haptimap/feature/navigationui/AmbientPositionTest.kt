package com.xanderjs.haptimap.feature.navigationui

import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Paso 12: la primera vista no se mueve al entrar y salir de ambiente. Compara en las referencias de las capturas
 * (oscuras, tres formas, dos idiomas) cada vista de ambiente con su gemela Eco: cada franja de filas con píxeles
 * encendidos de ambiente (aviso, flecha, distancia, estado) tiene que estar en las mismas filas que una de Eco.
 * Tras updateDebugScreenshotTest, si algo la mueve, este test falla.
 */
class AmbientPositionTest {

    private val dir = File("src/screenshotTestDebug/reference/com/xanderjs/haptimap/feature/navigationui")
    private val pairs = mapOf(
        "AmbientNavigating" to "NavigationTurn",
        "AmbientNavigatingOffline" to "NavigationOffline",
        "AmbientNavigatingLongDistance" to "NavigationVeryLongDistance",
        "AmbientToArrival" to "NavigationToArrival",
        "AmbientOffRoute" to "NavigationOffRoute",
        "AmbientRecalculating" to "NavigationOffRouteRecalculating",
        "AmbientOffRouteOffline" to "NavigationOffRouteOffline",
        "AmbientOffRouteWeakGpsFailed" to "NavigationOffRouteWeakGpsFailed",
        "AmbientArrived" to "NavigationArrived",
    )
    private val configs = listOf("round-small", "round-large", "square").flatMap { listOf("$it-dark-en", "$it-dark-es") }

    private fun reference(folder: String, name: String, config: String): File =
        File(dir, folder).listFiles { f -> f.name.startsWith("${name}_${config}_") }!!.single()

    /** Las franjas de filas con algún píxel encendido sobre el fondo negro, de arriba abajo (la primera, la hora). */
    private fun bands(file: File): List<IntRange> {
        val image = ImageIO.read(file)
        val lit = (0 until image.height).map { y ->
            (0 until image.width).any { x ->
                val rgb = image.getRGB(x, y)
                (rgb ushr 24) > 0 && maxOf(rgb shr 16 and 0xFF, rgb shr 8 and 0xFF, rgb and 0xFF) > 60
            }
        }
        val bands = mutableListOf<IntRange>()
        var start = -1
        lit.forEachIndexed { y, on ->
            if (on && start < 0) start = y
            if (!on && start >= 0) { bands += start until y; start = -1 }
        }
        if (start >= 0) bands += start until lit.size
        return bands
    }

    @Test
    fun `the first view stays in place between Eco and ambient`() {
        for (config in configs) for ((ambient, eco) in pairs) {
            val a = bands(reference("AmbientNavigationScreenshotsKt", ambient, config))
            val e = bands(reference("NavigationScreenshotsKt", eco, config))
            // cada franja de ambiente (sin la hora) en las mismas filas que alguna de Eco: Eco pinta además lo que
            // ambiente solo reserva (detalles, spinner, píldora); un desplazamiento real es media línea o más (~20 px)
            for (band in a.drop(1)) {
                val center = (band.first + band.last) / 2
                val nearest = e.minOf { abs((it.first + it.last) / 2 - center) }
                assertTrue("$ambient $config: band $band is $nearest px from any Eco band", nearest <= 3)
            }
        }
    }

    @Test
    fun `the reference pairs exist for every shape and language`() {
        assertEquals(54, configs.size * pairs.size)
        for (config in configs) for ((ambient, eco) in pairs) {
            reference("AmbientNavigationScreenshotsKt", ambient, config)
            reference("NavigationScreenshotsKt", eco, config)
        }
    }
}
