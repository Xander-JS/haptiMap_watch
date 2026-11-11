package com.xanderjs.haptimap.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Paleta base "Mapa" (inspirada en los cuatro tonos de Google Maps, con matices propios: no son los hex de la marca
 * de Google y nunca aparecen juntos como motivo decorativo): el ÚNICO lugar con valores de color crudos.
 * Solo la leen los esquemas de theme/; los componentes nunca la usan directamente.
 * Contraste y distancia entre colores comprobados en ColorContrastTest.
 */
internal object BasePalette {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)

    // Neutros con un punto de azul pizarra, para que el gris "pertenezca" al azul de la marca
    val Neutral8 = Color(0xFF11141A)
    val Neutral12 = Color(0xFF151820)
    val Neutral35 = Color(0xFF474D59)
    val Neutral50 = Color(0xFF717887)
    val Neutral55 = Color(0xFF757C8A)
    val Neutral75 = Color(0xFFB4BAC6)
    val Neutral92 = Color(0xFFE6E9EF)
    val Neutral96 = Color(0xFFF1F3F7)
    val Neutral97 = Color(0xFFF7F8FA)

    // Marca / Focus / ruta / llegada: azul
    val Blue10 = Color(0xFF0A1A33)
    val Blue40 = Color(0xFF1A5FD0)
    val Blue75 = Color(0xFF7FAEFF)

    // Tour y sus lugares: verde
    val Green10 = Color(0xFF00240F)
    val Green35 = Color(0xFF11703A)
    val Green75 = Color(0xFF5ED68A)

    // Aviso (sin red, GPS débil): amarillo; en claro baja a ocre para llegar a 4.5:1 sobre blanco
    val Yellow35 = Color(0xFF7D5D00)
    val Yellow85 = Color(0xFFFFD34D)

    // Problema (fuera de ruta, fallo): rojo
    val Red40 = Color(0xFFC2261B)
    val Red70 = Color(0xFFFF6E5E)
}

/**
 * Colores semánticos: lo único que consumen los componentes. Cada esquema (claro/oscuro) asigna un color de
 * [BasePalette] a cada rol, y cada color tiene un solo significado: azul = tu ruta (marca, Focus, la línea, el destino
 * y la llegada), verde = Tour y sus lugares, amarillo = aviso, rojo = problema (fuera de ruta o un fallo).
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
    val warning: Color,
    val error: Color,
)
