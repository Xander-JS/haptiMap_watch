package com.xanderjs.haptimap.presentation.catalog

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.R
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.designsystem.atoms.ActiveRouteButton
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.CategoryChip
import com.xanderjs.haptimap.designsystem.atoms.DirectionArrow
import com.xanderjs.haptimap.designsystem.atoms.DistanceText
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HaptiIcon
import com.xanderjs.haptimap.designsystem.atoms.HaptiIconSize
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.atoms.VoiceSearchButton
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.molecules.samplePlaceRows
import com.xanderjs.haptimap.designsystem.organisms.PlaceResultsList
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import com.xanderjs.haptimap.designsystem.theme.ThemeMode
import com.xanderjs.haptimap.feature.navigationui.RouteOfflineNotice
import com.xanderjs.haptimap.feature.navigationui.RouteStatus
import com.xanderjs.haptimap.feature.navigationui.RouteStatusMessage
import com.xanderjs.haptimap.presentation.debug.SimulationMode
import com.xanderjs.haptimap.feature.search.SearchStatus
import com.xanderjs.haptimap.feature.search.SearchStatusMessage
import com.xanderjs.haptimap.feature.search.icon
import com.xanderjs.haptimap.feature.search.label
import com.xanderjs.haptimap.designsystem.R as DsR

/** Pantalla temporal (paso 00): muestra todos los tokens y componentes. Desde el paso 11 se abre desde la búsqueda, solo en debug. */
@Composable
fun DesignSystemCatalog(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenLocationDebug: () -> Unit,
    onOpenSessionDebug: () -> Unit,
    onLoadSampleRoute: (() -> Unit)? = null,
    onOpenHaptics: (() -> Unit)? = null,
    simulationMode: SimulationMode? = null,
    onSimulationModeChange: (SimulationMode) -> Unit = {},
) {
    val colors = HaptiMapTheme.colors
    val typography = HaptiMapTheme.typography
    val spacing = HaptiMapTheme.spacing
    val shapes = HaptiMapTheme.shapes
    var selectedChip by remember { mutableIntStateOf(0) }
    // muestra el aviso de sin conexión sin cortar la red; la conexión real sigue mandando si está caída
    var forceOffline by rememberSaveable { mutableStateOf(false) }

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

    CompositionLocalProvider(LocalOffline provides (forceOffline || LocalOffline.current)) {
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
            item {
                HaptiButton(
                    text = stringResource(R.string.location_debug_title),
                    onClick = onOpenLocationDebug,
                    modifier = Modifier.fillMaxWidth(),
                    variant = HaptiButtonVariant.Secondary,
                )
            }
            item {
                HaptiButton(
                    text = stringResource(R.string.session_debug_title),
                    onClick = onOpenSessionDebug,
                    modifier = Modifier.fillMaxWidth(),
                    variant = HaptiButtonVariant.Secondary,
                )
            }

            // solo en debug (null en release): ruta de ejemplo sin clave de API → resumen de ruta
            onLoadSampleRoute?.let { load ->
                item {
                    HaptiButton(
                        text = stringResource(R.string.catalog_sample_route),
                        onClick = load,
                        modifier = Modifier.fillMaxWidth(),
                        variant = HaptiButtonVariant.Secondary,
                    )
                }
            }
            // solo en debug (null en release): banco de pruebas háptico (paso 09)
            onOpenHaptics?.let { open ->
                item {
                    HaptiButton(
                        text = stringResource(R.string.catalog_section_haptics),
                        onClick = open,
                        modifier = Modifier.fillMaxWidth(),
                        variant = HaptiButtonVariant.Secondary,
                    )
                }
            }
            // solo en debug (null en release): el recorrido simulado sustituye al GPS mientras se navega
            simulationMode?.let { mode ->
                item {
                    HaptiButton(
                        text = stringResource(R.string.simulation_label, stringResource(mode.label)),
                        onClick = { onSimulationModeChange(mode.next()) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = HaptiButtonVariant.Secondary,
                    )
                }
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
                HaptiButton(
                    text = stringResource(if (forceOffline) R.string.catalog_offline_stop else R.string.catalog_offline_simulate),
                    onClick = { forceOffline = !forceOffline },
                    modifier = Modifier.fillMaxWidth(),
                    variant = HaptiButtonVariant.Secondary,
                )
            }
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
                item {
                    CategoryChip(
                        label = stringResource(category.label),
                        icon = painterResource(category.icon),
                        selected = selectedChip == index,
                        onClick = { selectedChip = index },
                    )
                }
            }
            item { VoiceSearchButton(label = stringResource(DsR.string.sample_voice_search), onClick = {}) }
            item { ActiveRouteButton(text = stringResource(DsR.string.sample_place_museum), navigating = true, onClick = {}) }
            item { PlaceResultsList(items = samplePlaceRows(), onItemClick = {}) }
            item { SearchStatusMessage(SearchStatus.Loading) }
            item { SearchStatusMessage(SearchStatus.Failed(PlaceSearchError.NO_RESULTS)) }
            item { SearchStatusMessage(SearchStatus.Failed(PlaceSearchError.NO_CONNECTION)) }
            item { SearchStatusMessage(SearchStatus.Failed(PlaceSearchError.API_INVALID_KEY)) }
            item { HeadingText(stringResource(DsR.string.sample_place_museum)) }
            item { RouteStatusMessage(RouteStatus.Loading) }
            item { RouteStatusMessage(RouteStatus.Failed(RouteError.NO_ROUTE)) }
            item { RouteStatusMessage(RouteStatus.Failed(RouteError.MISSING_KEY)) }
            item { RouteOfflineNotice() }

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

@HaptiPreviews
@Composable
private fun DesignSystemCatalogPreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { DesignSystemCatalog(themeMode = ThemeMode.SYSTEM, onThemeModeChange = {}, onOpenLocationDebug = {}, onOpenSessionDebug = {}) }
}
