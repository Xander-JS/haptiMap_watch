package com.xanderjs.haptimap

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regla del sistema de diseño (DESIGN_SYSTEM §19): la etiqueta de un HaptiEdgeButton tiene una o dos palabras, en
 * todos los idiomas. El ancho del botón lo fija la forma de la pantalla: "Empezar a buscar" se cortaba en el paso 13.
 * Revisa el código de todos los módulos: cada llamada debe pasar un stringResource(R.string.x).
 */
class EdgeButtonLabelTest {
    private val root = File("..").canonicalFile
    private val call = Regex("""HaptiEdgeButton\(\s*text\s*=\s*stringResource\(\w*R\.string\.(\w+)\)""")

    @Test
    fun `edge button labels have one or two words in every language`() {
        val sources = root.walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith(".") }
            .filter { it.extension == "kt" && ("/src/main/" in it.path || "/src/debug/" in it.path) && it.name != "HaptiEdgeButton.kt" }
            .filter { "HaptiEdgeButton(" in it.readText() }
            .toList()
        // el recurso puede ser de otro módulo (el catálogo usa los del sistema de diseño)
        val stringFiles = root.walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith(".") }
            .filter { it.name == "strings.xml" && "/src/main/res/values" in it.path }
            .toList()
        var checked = 0
        for (source in sources) {
            val text = source.readText()
            // ninguna llamada sin comprobar: un literal o una variable se saltarían la regla
            assertEquals("${source.path}: pass text = stringResource(R.string.x)", Regex("""HaptiEdgeButton\(""").findAll(text).count(), call.findAll(text).count())
            for (name in call.findAll(text).map { it.groupValues[1] }) {
                val labels = stringFiles.mapNotNull { file ->
                    Regex("""<string name="$name"[^>]*>([^<]*)</string>""").find(file.readText())?.let { file.parentFile.name to it.groupValues[1] }
                }
                assertTrue("$name not found in values and values-es", labels.size >= 2)
                labels.forEach { (folder, label) ->
                    val words = label.trim().split(Regex("""\s+""")).size
                    assertTrue("$folder/$name = \"$label\": $words words, an edge button takes one or two", words <= 2)
                    checked++
                }
            }
        }
        // los tres botones del tutorial, en inglés y en español
        assertTrue("only $checked labels found", checked >= 6)
    }
}
