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
)
