package com.xanderjs.haptimap.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Shapes
import androidx.wear.compose.material3.Typography
import com.xanderjs.haptimap.designsystem.tokens.AmbientTypography
import com.xanderjs.haptimap.designsystem.tokens.DefaultTypography
import com.xanderjs.haptimap.designsystem.tokens.HaptiColors
import com.xanderjs.haptimap.designsystem.tokens.HaptiDimens
import com.xanderjs.haptimap.designsystem.tokens.HaptiShapes
import com.xanderjs.haptimap.designsystem.tokens.HaptiSpacing
import com.xanderjs.haptimap.designsystem.tokens.HaptiTypography
import com.xanderjs.haptimap.designsystem.tokens.haptiDimens

private val LocalHaptiColors = staticCompositionLocalOf { DarkHaptiColors }
private val LocalHaptiTypography = staticCompositionLocalOf { DefaultTypography }
private val LocalHaptiSpacing = staticCompositionLocalOf { HaptiSpacing() }
private val LocalHaptiShapes = staticCompositionLocalOf { HaptiShapes() }
private val LocalHaptiDimens = staticCompositionLocalOf { HaptiDimens() }
private val LocalHaptiAmbient = staticCompositionLocalOf { false }

/**
 * Con [ambient] (paso 12), los roles y la tipografía de ambiente, sea cual sea [themeMode]: AOD es oscuro siempre.
 * Los componentes lo leen en [HaptiMapTheme.ambient] para quitar lo que no va en ambiente (rellenos, animaciones).
 */
@Composable
fun HaptiMapTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    ambient: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = when {
        ambient -> AmbientHaptiColors
        dark -> DarkHaptiColors
        else -> LightHaptiColors
    }
    val typography = if (ambient) AmbientTypography else DefaultTypography
    val shapes = HaptiShapes()

    CompositionLocalProvider(
        LocalHaptiColors provides colors,
        LocalHaptiTypography provides typography,
        LocalHaptiSpacing provides HaptiSpacing(),
        LocalHaptiShapes provides shapes,
        LocalHaptiDimens provides haptiDimens(round = LocalConfiguration.current.isScreenRound, fontScale = LocalDensity.current.fontScale),
        LocalHaptiAmbient provides ambient,
    ) {
        MaterialTheme(
            colorScheme = colors.toWearColorScheme(),
            typography = Typography(
                titleMedium = typography.title,
                bodyMedium = typography.body,
                bodySmall = typography.caption,
                labelMedium = typography.label,
                numeralLarge = typography.distance,
            ),
            shapes = Shapes(small = shapes.small, medium = shapes.medium, large = shapes.large),
            content = content,
        )
    }
}

/** Punto de acceso único a los tokens: `HaptiMapTheme.colors.primary`, `HaptiMapTheme.spacing.m`... */
object HaptiMapTheme {
    val colors: HaptiColors
        @Composable @ReadOnlyComposable get() = LocalHaptiColors.current
    val typography: HaptiTypography
        @Composable @ReadOnlyComposable get() = LocalHaptiTypography.current
    val spacing: HaptiSpacing
        @Composable @ReadOnlyComposable get() = LocalHaptiSpacing.current
    val shapes: HaptiShapes
        @Composable @ReadOnlyComposable get() = LocalHaptiShapes.current
    val dimens: HaptiDimens
        @Composable @ReadOnlyComposable get() = LocalHaptiDimens.current

    /** Dentro de un tema de ambiente (AOD): sin rellenos grandes ni animaciones. */
    val ambient: Boolean
        @Composable @ReadOnlyComposable get() = LocalHaptiAmbient.current
}
