package com.xanderjs.haptimap.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tamaños de componentes. */
@Immutable
data class HaptiDimens(
    /** Área táctil mínima global (accesibilidad): ningún elemento interactivo baja de esto. */
    val minTouchTarget: Dp = 48.dp,
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 56.dp,
    val borderWidth: Dp = 1.dp,
    /** HapticWaveform: alto en la tarjeta del banco de pruebas y tamaño compacto del indicador de debug. */
    val waveformHeight: Dp = 24.dp,
    val waveformCompactHeight: Dp = 14.dp,
    val waveformCompactWidth: Dp = 36.dp,
    /** Margen extra bajo la franja de la hora: el aviso curvo "Sin red" baja por los lados y tapaba el primer elemento. */
    val clockBandSafeTop: Dp = 8.dp,
    /**
     * Ancho del contenido centrado (CenteredScreenTemplate) respecto a la pantalla. Con 0,8 un bloque de
     * hasta ~0,6 veces el alto (mensaje de 3 líneas + botón) cabe entero dentro del círculo.
     */
    val centeredContentWidthFraction: Float = 0.8f,
)
