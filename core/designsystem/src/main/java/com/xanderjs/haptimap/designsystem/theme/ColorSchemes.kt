package com.xanderjs.haptimap.designsystem.theme

import androidx.wear.compose.material3.ColorScheme
import com.xanderjs.haptimap.designsystem.tokens.BasePalette
import com.xanderjs.haptimap.designsystem.tokens.HaptiColors

/** Oscuro: fondo negro puro (OLED, ahorra batería). */
internal val DarkHaptiColors = HaptiColors(
    background = BasePalette.Black,
    onBackground = BasePalette.Neutral92,
    surface = BasePalette.Neutral15,
    onSurface = BasePalette.Neutral92,
    onSurfaceVariant = BasePalette.Neutral75,
    primary = BasePalette.Teal80,
    onPrimary = BasePalette.Teal10,
    accent = BasePalette.Amber80,
    onAccent = BasePalette.Amber10,
    navigationArrow = BasePalette.Teal80,
    offRoute = BasePalette.Red80,
    poiHighlight = BasePalette.Amber80,
    success = BasePalette.Green80,
    warning = BasePalette.Amber80,
    error = BasePalette.Red80,
)

/** Claro: tonos oscuros y saturados sobre blanco para leerse a pleno sol (todos ≥ 4.5:1, WCAG AA). */
internal val LightHaptiColors = HaptiColors(
    background = BasePalette.White,
    onBackground = BasePalette.Neutral10,
    surface = BasePalette.Neutral95,
    onSurface = BasePalette.Neutral10,
    onSurfaceVariant = BasePalette.Neutral30,
    primary = BasePalette.Teal40,
    onPrimary = BasePalette.White,
    accent = BasePalette.Amber40,
    onAccent = BasePalette.White,
    navigationArrow = BasePalette.Teal40,
    offRoute = BasePalette.Red40,
    poiHighlight = BasePalette.Amber40,
    success = BasePalette.Green40,
    warning = BasePalette.Amber40,
    error = BasePalette.Red40,
)

/** Traduce los roles propios al ColorScheme de Wear M3, para que los componentes estándar (TimeText, scaffold) también sigan los tokens. */
internal fun HaptiColors.toWearColorScheme() = ColorScheme(
    primary = primary,
    primaryDim = primary,
    primaryContainer = primary,
    onPrimary = onPrimary,
    onPrimaryContainer = onPrimary,
    secondary = accent,
    secondaryDim = accent,
    secondaryContainer = surface,
    onSecondary = onAccent,
    onSecondaryContainer = onSurface,
    tertiary = success,
    surfaceContainerLow = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surface,
    onSurface = onSurface,
    onSurfaceVariant = onSurfaceVariant,
    outline = onSurfaceVariant,
    outlineVariant = surface,
    background = background,
    onBackground = onBackground,
    error = error,
    errorDim = error,
    onError = background,
)
