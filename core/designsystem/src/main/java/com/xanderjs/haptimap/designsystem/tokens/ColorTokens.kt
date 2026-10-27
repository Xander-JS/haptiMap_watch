package com.xanderjs.haptimap.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Paleta base: el ÚNICO lugar con valores de color crudos.
 * Solo la leen los esquemas de theme/; los componentes nunca la usan directamente.
 */
internal object BasePalette {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)

    val Neutral10 = Color(0xFF0F1414)
    val Neutral15 = Color(0xFF1A1D1D)
    val Neutral30 = Color(0xFF3D4644)
    val Neutral75 = Color(0xFFB5BDBB)
    val Neutral92 = Color(0xFFE8ECEB)
    val Neutral95 = Color(0xFFEEF2F1)

    // Primario: "Compass Teal"
    val Teal10 = Color(0xFF00201C)
    val Teal40 = Color(0xFF00695C)
    val Teal80 = Color(0xFF4FD8C4)

    // Acento / POI / advertencia: ámbar
    val Amber10 = Color(0xFF2A1800)
    val Amber40 = Color(0xFF8A5300)
    val Amber80 = Color(0xFFFFC046)

    // Estados
    val Green40 = Color(0xFF1E6B2E)
    val Green80 = Color(0xFF7DD88A)
    val Red40 = Color(0xFFB3261E)
    val Red80 = Color(0xFFFF8A80)
}

/**
 * Colores semánticos: lo único que consumen los componentes.
 * Cada esquema (claro/oscuro) asigna un color de [BasePalette] a cada rol.
 */
@Immutable
data class HaptiColors(
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
    val navigationArrow: Color,
    val offRoute: Color,
    val poiHighlight: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
)
