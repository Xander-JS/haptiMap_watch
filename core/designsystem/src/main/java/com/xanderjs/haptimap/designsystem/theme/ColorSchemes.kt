package com.xanderjs.haptimap.designsystem.theme

import androidx.wear.compose.material3.ColorScheme
import com.xanderjs.haptimap.designsystem.tokens.BasePalette
import com.xanderjs.haptimap.designsystem.tokens.HaptiColors

/** Oscuro: fondo negro puro (OLED, ahorra batería) y acentos muy luminosos para leerse de un vistazo. */
internal val DarkHaptiColors = HaptiColors(
    background = BasePalette.Black,
    onBackground = BasePalette.Neutral96,
    surface = BasePalette.Neutral12,
    onSurface = BasePalette.Neutral96,
    onSurfaceVariant = BasePalette.Neutral75,
    outline = BasePalette.Neutral50,
    primary = BasePalette.Blue75,
    onPrimary = BasePalette.Blue10,
    tour = BasePalette.Green75,
    onTour = BasePalette.Green10,
    navigationArrow = BasePalette.Blue75,
    offRoute = BasePalette.Red70,
    warning = BasePalette.Yellow85,
    error = BasePalette.Red70,
)

/** Claro: los mismos roles en tonos oscuros sobre un blanco frío, para leerse a pleno sol (texto ≥ 4.5:1). */
internal val LightHaptiColors = HaptiColors(
    background = BasePalette.Neutral97,
    onBackground = BasePalette.Neutral8,
    surface = BasePalette.Neutral92,
    onSurface = BasePalette.Neutral8,
    onSurfaceVariant = BasePalette.Neutral35,
    outline = BasePalette.Neutral55,
    primary = BasePalette.Blue40,
    onPrimary = BasePalette.White,
    tour = BasePalette.Green35,
    onTour = BasePalette.White,
    navigationArrow = BasePalette.Blue40,
    offRoute = BasePalette.Red40,
    warning = BasePalette.Yellow35,
    error = BasePalette.Red40,
)

/**
 * Ambiente / AOD (paso 12), igual con el tema claro y el oscuro: Wear pide fondo negro y pocos píxeles encendidos.
 * Un solo gris claro para todo el contenido (no blanco puro, contra el quemado; y sin colores, que en pantallas
 * low-bit no se ven igual). Surface y outline negros: las píldoras se quedan en texto, sin relleno ni borde.
 */
internal val AmbientHaptiColors = HaptiColors(
    background = BasePalette.Black,
    onBackground = BasePalette.Neutral75,
    surface = BasePalette.Black,
    onSurface = BasePalette.Neutral75,
    onSurfaceVariant = BasePalette.Neutral75,
    outline = BasePalette.Black,
    primary = BasePalette.Neutral75,
    onPrimary = BasePalette.Black,
    tour = BasePalette.Neutral75,
    onTour = BasePalette.Black,
    navigationArrow = BasePalette.Neutral75,
    offRoute = BasePalette.Neutral75,
    warning = BasePalette.Neutral75,
    error = BasePalette.Neutral75,
)

/** Traduce los roles propios al ColorScheme de Wear M3, para que los componentes estándar (TimeText, scaffold) también sigan los tokens. */
internal fun HaptiColors.toWearColorScheme() = ColorScheme(
    primary = primary,
    primaryDim = primary,
    primaryContainer = primary,
    onPrimary = onPrimary,
    onPrimaryContainer = onPrimary,
    secondary = tour,
    secondaryDim = tour,
    secondaryContainer = surface,
    onSecondary = onTour,
    onSecondaryContainer = onSurface,
    tertiary = primary,
    surfaceContainerLow = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surface,
    onSurface = onSurface,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline,
    outlineVariant = outline,
    background = background,
    onBackground = onBackground,
    error = error,
    errorDim = error,
    onError = background,
)
