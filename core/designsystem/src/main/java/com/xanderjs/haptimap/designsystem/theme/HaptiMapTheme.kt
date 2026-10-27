package com.xanderjs.haptimap.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Shapes
import androidx.wear.compose.material3.Typography
import com.xanderjs.haptimap.designsystem.tokens.DefaultTypography
import com.xanderjs.haptimap.designsystem.tokens.HaptiColors
import com.xanderjs.haptimap.designsystem.tokens.HaptiDimens
import com.xanderjs.haptimap.designsystem.tokens.HaptiShapes
import com.xanderjs.haptimap.designsystem.tokens.HaptiSpacing
import com.xanderjs.haptimap.designsystem.tokens.HaptiTypography

private val LocalHaptiColors = staticCompositionLocalOf { DarkHaptiColors }
private val LocalHaptiTypography = staticCompositionLocalOf { DefaultTypography }
private val LocalHaptiSpacing = staticCompositionLocalOf { HaptiSpacing() }
private val LocalHaptiShapes = staticCompositionLocalOf { HaptiShapes() }
private val LocalHaptiDimens = staticCompositionLocalOf { HaptiDimens() }

@Composable
fun HaptiMapTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkHaptiColors else LightHaptiColors
    val typography = DefaultTypography
    val shapes = HaptiShapes()

    CompositionLocalProvider(
        LocalHaptiColors provides colors,
        LocalHaptiTypography provides typography,
        LocalHaptiSpacing provides HaptiSpacing(),
        LocalHaptiShapes provides shapes,
        LocalHaptiDimens provides HaptiDimens(),
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
}
