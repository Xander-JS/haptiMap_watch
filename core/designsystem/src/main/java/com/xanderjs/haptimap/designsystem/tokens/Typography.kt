package com.xanderjs.haptimap.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Escala tipográfica para pantallas pequeñas y redondas. */
@Immutable
data class HaptiTypography(
    /** Distancia a la próxima maniobra: debe leerse de un vistazo mientras se camina. */
    val distance: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

internal val DefaultTypography = HaptiTypography(
    // tnum: dígitos de ancho fijo para que el número no "baile" al actualizarse
    distance = TextStyle(fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, fontFeatureSettings = "tnum"),
    title = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    body = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp),
    label = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    caption = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
)
