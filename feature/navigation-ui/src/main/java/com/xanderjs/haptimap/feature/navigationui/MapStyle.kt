package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Estilo del mapa (paso 22) DERIVADO de los tokens del design system: un mapa monocromo y limpio, en el mismo tema
 * claro/oscuro que el resto de la app. Un cambio en los tokens se propaga aquí.
 *
 * Es un JSON (MapStyleOptions), no un Map ID en la nube: así los colores salen de los tokens (no de la consola de
 * Google) y las cargas del mapa siguen en el SKU gratuito "Maps SDK" en vez del SKU "Dynamic Maps" (con Map ID).
 * shortcut: el estilo JSON está en desuso (Google empuja el estilo en la nube); funciona de sobra para el proyecto.
 * Migrar a Map ID solo si Google lo retira, asumiendo el coste del SKU Dynamic Maps.
 *
 * Se ocultan POIs, tránsito y fronteras: en una pantalla redonda pequeña estorban y la guía ya la dan la flecha y
 * el háptico. El mapa es un complemento.
 */
fun mapStyleJson(background: Color, road: Color, label: Color): String {
    val bg = background.hex()
    val rd = road.hex()
    val tx = label.hex()
    return """
        [
          {"elementType":"geometry","stylers":[{"color":"$bg"}]},
          {"elementType":"labels.text.fill","stylers":[{"color":"$tx"}]},
          {"elementType":"labels.text.stroke","stylers":[{"color":"$bg"}]},
          {"elementType":"labels.icon","stylers":[{"visibility":"off"}]},
          {"featureType":"poi","stylers":[{"visibility":"off"}]},
          {"featureType":"transit","stylers":[{"visibility":"off"}]},
          {"featureType":"administrative","elementType":"geometry","stylers":[{"visibility":"off"}]},
          {"featureType":"road","elementType":"geometry","stylers":[{"color":"$rd"}]},
          {"featureType":"road","elementType":"labels","stylers":[{"visibility":"simplified"}]},
          {"featureType":"water","elementType":"geometry","stylers":[{"color":"$rd"}]}
        ]
    """.trimIndent()
}

/** "#RRGGBB" del color del token (sin alfa: el estilo del mapa es opaco). */
private fun Color.hex(): String = "#%06X".format(0xFFFFFF and toArgb())
