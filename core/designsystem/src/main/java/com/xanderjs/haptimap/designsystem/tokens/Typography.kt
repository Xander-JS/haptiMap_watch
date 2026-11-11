package com.xanderjs.haptimap.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.sp

/** Escala tipográfica para pantallas pequeñas y redondas. */
@Immutable
data class HaptiTypography(
    /** Distancia a la próxima maniobra: debe leerse de un vistazo mientras se camina. */
    val distance: TextStyle,
    /** La misma distancia en pequeño, donde acompaña a un texto (tutorial háptico, paso 13, y el mapa). */
    val distanceCompact: TextStyle,
    /** Entre las dos: la distancia de un lugar en su detalle, junto a su disco y sobre su nombre, encima del botón "Ir". */
    val distanceMedium: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

/**
 * Roboto Condensed del sistema (sin fuente empaquetada): numerales y títulos de "instrumento". En el mismo ancho
 * caben más cifras ("1,2 km" grande en la pantalla pequeña). Lo demás (cuerpo, botones) sigue en Roboto, más legible.
 */
private fun condensed(weight: Int) = FontFamily(
    Typeface(android.graphics.Typeface.create(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL), weight, false)),
)

// una familia por peso: con una familia de un solo Typeface, Compose no aplica el fontWeight
private val CondensedBold = condensed(700)
private val CondensedRegular = condensed(400)

internal val DefaultTypography = HaptiTypography(
    // tnum: dígitos de ancho fijo para que el número no "baile" al actualizarse
    distance = TextStyle(fontFamily = CondensedBold, fontWeight = FontWeight.Bold, fontSize = 50.sp, lineHeight = 50.sp, fontFeatureSettings = "tnum"),
    distanceCompact = TextStyle(fontFamily = CondensedBold, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 22.sp, fontFeatureSettings = "tnum"),
    distanceMedium = TextStyle(fontFamily = CondensedBold, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 34.sp, fontFeatureSettings = "tnum"),
    title = TextStyle(fontFamily = CondensedBold, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp, letterSpacing = 0.2.sp),
    body = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp),
    label = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    caption = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
)

/** Ambiente (paso 12): los mismos tamaños, para que nada se mueva al entrar, con trazo fino: menos píxeles encendidos. */
internal val AmbientTypography = DefaultTypography.copy(
    distance = DefaultTypography.distance.copy(fontFamily = CondensedRegular, fontWeight = FontWeight.Normal),
    distanceCompact = DefaultTypography.distanceCompact.copy(fontFamily = CondensedRegular, fontWeight = FontWeight.Normal),
    distanceMedium = DefaultTypography.distanceMedium.copy(fontFamily = CondensedRegular, fontWeight = FontWeight.Normal),
    title = DefaultTypography.title.copy(fontFamily = CondensedRegular, fontWeight = FontWeight.Normal),
    label = DefaultTypography.label.copy(fontWeight = FontWeight.Normal),
)
