package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * El modo del trayecto, en color (rediseño Pulso): Focus en volt (la marca) y Tour en orquídea. Colorea la maniobra,
 * el selector de modo y las acciones del Tour, así el modo se reconoce de un vistazo sin leer nada.
 */
enum class ModeTone { Focus, Tour }

val ModeTone.color: Color
    @Composable @ReadOnlyComposable get() = if (this == ModeTone.Tour) HaptiMapTheme.colors.tour else HaptiMapTheme.colors.navigationArrow

val ModeTone.onColor: Color
    @Composable @ReadOnlyComposable get() = if (this == ModeTone.Tour) HaptiMapTheme.colors.onTour else HaptiMapTheme.colors.onPrimary

/** El contorno de tarjetas y botones sobre surface: ≥ 3:1 contra el fondo (WCAG 1.4.11). */
@Composable
@ReadOnlyComposable
internal fun outlineBorder() = BorderStroke(HaptiMapTheme.dimens.outlineWidth, HaptiMapTheme.colors.outline)
