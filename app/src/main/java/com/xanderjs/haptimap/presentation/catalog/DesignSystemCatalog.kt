package com.xanderjs.haptimap.presentation.catalog

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material3.AppScaffold
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.R
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticTokens
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.CategoryChip
import com.xanderjs.haptimap.designsystem.atoms.DirectionArrow
import com.xanderjs.haptimap.designsystem.atoms.DistanceText
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HaptiIcon
import com.xanderjs.haptimap.designsystem.atoms.HaptiIconSize
import com.xanderjs.haptimap.designsystem.atoms.label
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import com.xanderjs.haptimap.designsystem.theme.ThemeMode
import com.xanderjs.haptimap.designsystem.R as DsR

/** Pantalla temporal (paso 00): muestra todos los tokens y componentes. Se elimina cuando existan las features. */
@Composable
fun DesignSystemCatalog(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val colors = HaptiMapTheme.colors
    val typography = HaptiMapTheme.typography
    val spacing = HaptiMapTheme.spacing
    val shapes = HaptiMapTheme.shapes
    var selectedChip by remember { mutableIntStateOf(0) }

    val swatches = listOf(
        R.string.token_background to colors.background,
        R.string.token_on_background to colors.onBackground,
        R.string.token_surface to colors.surface,
        R.string.token_on_surface to colors.onSurface,
        R.string.token_on_surface_variant to colors.onSurfaceVariant,
        R.string.token_primary to colors.primary,
        R.string.token_on_primary to colors.onPrimary,
        R.string.token_accent to colors.accent,
        R.string.token_on_accent to colors.onAccent,
        R.string.token_navigation_arrow to colors.navigationArrow,
        R.string.token_off_route to colors.offRoute,
        R.string.token_poi_highlight to colors.poiHighlight,
        R.string.token_success to colors.success,
        R.string.token_warning to colors.warning,
        R.string.token_error to colors.error,
    )
    val textStyles = listOf(
        R.string.token_distance to typography.distance,
        R.string.token_title to typography.title,
        R.string.token_body to typography.body,
        R.string.token_label to typography.label,
        R.string.token_caption to typography.caption,
    )
    val spacings = listOf(
        R.string.token_xs to spacing.xs,
        R.string.token_s to spacing.s,
        R.string.token_m to spacing.m,
        R.string.token_l to spacing.l,
        R.string.token_xl to spacing.xl,
    )
    val shapeTokens = listOf(
        R.string.token_small to shapes.small,
        R.string.token_medium to shapes.medium,
        R.string.token_large to shapes.large,
        R.string.token_full to shapes.full,
    )

    RoundScreenTemplate {
        item { Text(stringResource(R.string.catalog_title), style = typography.title, color = colors.onBackground) }
        item {
            HaptiButton(
                text = stringResource(R.string.theme_mode_label, stringResource(themeMode.label)),
                onClick = { onThemeModeChange(themeMode.next()) },
                modifier = Modifier.fillMaxWidth(),
                variant = HaptiButtonVariant.Secondary,
            )
        }

        item { SectionHeader(R.string.catalog_section_components) }
        item { ManeuverIndicator(direction = ArrowDirection.LEFT, distanceMeters = 20) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
                ArrowDirection.entries.forEach { DirectionArrow(direction = it, size = HaptiIconSize.Medium) }
            }
        }
        item { DistanceText(meters = 1500) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.m), verticalAlignment = Alignment.CenterVertically) {
                HaptiIconSize.entries.forEach {
                    HaptiIcon(painter = painterResource(DsR.drawable.ic_straight), contentDescription = null, size = it)
                }
            }
        }
        item {
            HaptiButton(text = stringResource(DsR.string.sample_button_primary), onClick = {}, modifier = Modifier.fillMaxWidth())
        }
        item {
            HaptiButton(
                text = stringResource(DsR.string.sample_button_secondary),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = HaptiButtonVariant.Secondary,
            )
        }
        PlaceCategory.entries.forEachIndexed { index, category ->
            item { CategoryChip(label = stringResource(category.label), selected = selectedChip == index, onClick = { selectedChip = index }) }
        }

        item { SectionHeader(R.string.catalog_section_colors) }
        swatches.forEach { (name, color) ->
            item {
                TokenRow(name) {
                    Box(
                        Modifier.size(HaptiMapTheme.dimens.iconMedium)
                            .background(color, shapes.full)
                            .border(HaptiMapTheme.dimens.borderWidth, colors.onSurfaceVariant, shapes.full),
                    )
                }
            }
        }

        item { SectionHeader(R.string.catalog_section_typography) }
        textStyles.forEach { (name, style) -> item { StyleSample(name, style) } }

        item { SectionHeader(R.string.catalog_section_spacing) }
        spacings.forEach { (name, size) -> item { TokenRow(name) { SpacingBar(size) } } }

        item { SectionHeader(R.string.catalog_section_shapes) }
        shapeTokens.forEach { (name, shape) -> item { TokenRow(name) { ShapeSample(shape) } } }

        item { SectionHeader(R.string.catalog_section_haptics) }
        HapticEvent.entries.forEach { event ->
            item {
                val pattern = HapticTokens.patternFor(event)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(event.label), style = typography.body, color = colors.onSurface)
                    Text(
                        pluralStringResource(
                            R.plurals.haptic_pattern_summary,
                            pattern.pulseCount,
                            pattern.pulseCount,
                            pattern.totalDurationMs.toInt(),
                        ),
                        style = typography.caption,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(@StringRes title: Int) {
    Text(stringResource(title), style = HaptiMapTheme.typography.title, color = HaptiMapTheme.colors.primary)
}

@Composable
private fun TokenRow(@StringRes name: Int, sample: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m), verticalAlignment = Alignment.CenterVertically) {
        sample()
        Text(stringResource(name), style = HaptiMapTheme.typography.caption, color = HaptiMapTheme.colors.onSurface)
    }
}

@Composable
private fun StyleSample(@StringRes name: Int, style: TextStyle) {
    Text(stringResource(name), style = style, color = HaptiMapTheme.colors.onBackground, maxLines = 1)
}

@Composable
private fun SpacingBar(size: Dp) {
    Box(Modifier.width(size).height(HaptiMapTheme.dimens.iconSmall).background(HaptiMapTheme.colors.primary))
}

@Composable
private fun ShapeSample(shape: Shape) {
    Box(Modifier.size(HaptiMapTheme.dimens.iconMedium).background(HaptiMapTheme.colors.accent, shape))
}

@get:StringRes
private val ThemeMode.label: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

private fun ThemeMode.next(): ThemeMode = ThemeMode.entries[(ordinal + 1) % ThemeMode.entries.size]

@get:StringRes
private val HapticEvent.label: Int
    get() = when (this) {
        HapticEvent.TURN_LEFT -> R.string.haptic_turn_left
        HapticEvent.TURN_RIGHT -> R.string.haptic_turn_right
        HapticEvent.CONTINUE -> R.string.haptic_continue
        HapticEvent.TURN_IMMINENT -> R.string.haptic_turn_imminent
        HapticEvent.ARRIVAL -> R.string.haptic_arrival
        HapticEvent.OFF_ROUTE -> R.string.haptic_off_route
        HapticEvent.POI_SUGGESTION -> R.string.haptic_poi_suggestion
    }

@HaptiPreviews
@Composable
private fun DesignSystemCatalogPreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { DesignSystemCatalog(themeMode = ThemeMode.SYSTEM, onThemeModeChange = {}) }
}
