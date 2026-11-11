package com.xanderjs.haptimap.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Paleta base "Pulso" (rediseño, docs/design/directions): el ÚNICO lugar con valores de color crudos.
 * Solo la leen los esquemas de theme/; los componentes nunca la usan directamente.
 * Contraste y distancia entre colores comprobados en ColorContrastTest.
 */
internal object BasePalette {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)

    // Neutros con un punto de oliva, para que el gris "pertenezca" al volt
    val Neutral8 = Color(0xFF12150C)
    val Neutral12 = Color(0xFF161A10)
    val Neutral35 = Color(0xFF4A5240)
    val Neutral50 = Color(0xFF747E66)
    val Neutral55 = Color(0xFF7A846C)
    val Neutral75 = Color(0xFFB4BCA6)
    val Neutral92 = Color(0xFFE8ECDE)
    val Neutral96 = Color(0xFFF3F6EC)
    val Neutral97 = Color(0xFFF7F9F1)

    // Marca / Focus: volt
    val Volt10 = Color(0xFF172000)
    val Volt35 = Color(0xFF3F5600)
    val Volt90 = Color(0xFFD4F53C)

    // Tour y sus lugares: orquídea
    val Orchid10 = Color(0xFF3A0030)
    val Orchid35 = Color(0xFF8E2A9E)
    val Orchid80 = Color(0xFFFF8FD8)

    // Llegada / éxito: cielo
    val Sky15 = Color(0xFF00283A)
    val Sky40 = Color(0xFF00658A)
    val Sky80 = Color(0xFF5CCFFF)

    // Aviso (sin red, GPS débil): ámbar
    val Amber35 = Color(0xFF8F5600)
    val Amber80 = Color(0xFFFFB547)

    // Problema (fuera de ruta, fallo): rojo
    val Red40 = Color(0xFFC0122C)
    val Red70 = Color(0xFFFF5A3C)
}

/**
 * Colores semánticos: lo único que consumen los componentes. Cada esquema (claro/oscuro) asigna un color de
 * [BasePalette] a cada rol, y cada color tiene un solo significado: volt = marca y Focus, orquídea = Tour y sus
 * lugares, cielo = llegada, ámbar = aviso, rojo = problema (fuera de ruta o un fallo).
 */
@Immutable
data class HaptiColors(
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    /** Borde de tarjetas y botones sobre surface: ≥ 3:1 contra el fondo (WCAG 1.4.11), así se ve el contorno. */
    val outline: Color,
    val primary: Color,
    val onPrimary: Color,
    /** Modo Tour, sus sugerencias y sus POIs. */
    val tour: Color,
    val onTour: Color,
    val navigationArrow: Color,
    val offRoute: Color,
    /** Llegada al destino. */
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val error: Color,
)
