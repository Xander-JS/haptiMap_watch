package com.xanderjs.haptimap.presentation.catalog

import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import com.xanderjs.haptimap.designsystem.atoms.StepIndicator
import com.xanderjs.haptimap.designsystem.atoms.BodyText
import com.xanderjs.haptimap.designsystem.atoms.HaptiEdgeButton
import com.xanderjs.haptimap.designsystem.atoms.VoiceSearchButton
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.molecules.samplePlaceRows
import com.xanderjs.haptimap.designsystem.organisms.PlaceResultsList
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.templates.PlaceDetailPager
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate
import com.xanderjs.haptimap.designsystem.templates.samplePlaceDetails
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
import androidx.annotation.DrawableRes
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.LanguageOption
import com.xanderjs.haptimap.designsystem.atoms.ChoiceChip
import com.xanderjs.haptimap.designsystem.atoms.HapticPulse
import com.xanderjs.haptimap.designsystem.atoms.HapticWaveform
import com.xanderjs.haptimap.designsystem.atoms.IconDisc
import com.xanderjs.haptimap.designsystem.atoms.InstructionText
import com.xanderjs.haptimap.designsystem.atoms.MapControlButton
import com.xanderjs.haptimap.designsystem.atoms.ModeTone
import com.xanderjs.haptimap.designsystem.atoms.NoticeLabel
import com.xanderjs.haptimap.designsystem.atoms.PulseRings
import com.xanderjs.haptimap.designsystem.atoms.RouteStatsText
import com.xanderjs.haptimap.designsystem.atoms.SectionLabel
import com.xanderjs.haptimap.designsystem.atoms.StatusTone
import com.xanderjs.haptimap.designsystem.atoms.color
import com.xanderjs.haptimap.designsystem.atoms.onColor
import com.xanderjs.haptimap.designsystem.atoms.sampleLeftSegments
import com.xanderjs.haptimap.designsystem.atoms.sampleRampSegments
import com.xanderjs.haptimap.designsystem.molecules.ChoiceOption
import com.xanderjs.haptimap.designsystem.molecules.HapticCueBadge
import com.xanderjs.haptimap.designsystem.molecules.HapticPatternCard
import com.xanderjs.haptimap.designsystem.molecules.MapManeuverCard
import com.xanderjs.haptimap.designsystem.molecules.PlaceRow
import com.xanderjs.haptimap.designsystem.molecules.ProfileOption
import com.xanderjs.haptimap.designsystem.molecules.SettingChoiceGroup
import com.xanderjs.haptimap.designsystem.molecules.SettingStepperRow
import com.xanderjs.haptimap.designsystem.molecules.SuggestionCountdown
import com.xanderjs.haptimap.designsystem.organisms.ConfirmDialog
import com.xanderjs.haptimap.feature.onboarding.label
import com.xanderjs.haptimap.presentation.ProvideAppLanguage
import kotlin.time.Duration.Companion.minutes
import com.xanderjs.haptimap.feature.settings.R as SettingsR

/**
 * Catálogo del design system (solo debug, desde la búsqueda): todos los tokens y cada átomo, molécula y organismo, con
 * tema claro/oscuro, idioma inglés/español y cada aviso háptico con su anillo sincronizado. [onPlayCue] null = sin motor (preview).
 */
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
    noVibrator: Boolean? = null,
    onNoVibratorChange: (Boolean) -> Unit = {},
    pulse: Pair<HapticEvent, HapticPulse>? = null,
    onPlayCue: ((HapticEvent) -> Unit)? = null,
) {
    val colors = HaptiMapTheme.colors
    val typography = HaptiMapTheme.typography
    val spacing = HaptiMapTheme.spacing
    val shapes = HaptiMapTheme.shapes
    var selectedChip by remember { mutableIntStateOf(0) }
    // muestra el aviso de sin conexión sin cortar la red; la conexión real sigue mandando si está caída
    var forceOffline by rememberSaveable { mutableStateOf(false) }
    // idioma solo del catálogo, para enseñarlo en los dos sin tocar el ajuste guardado; SYSTEM = el de la app
    var language by rememberSaveable { mutableStateOf(LanguageOption.SYSTEM) }
    var confirming by remember { mutableStateOf(false) }
    var placeDetail by remember { mutableStateOf(false) }
    var selectedChoice by remember { mutableIntStateOf(1) }
    var stepperValue by remember { mutableIntStateOf(7) }

    val swatches = listOf(
        R.string.token_background to colors.background,
        R.string.token_on_background to colors.onBackground,
        R.string.token_surface to colors.surface,
        R.string.token_on_surface to colors.onSurface,
        R.string.token_on_surface_variant to colors.onSurfaceVariant,
        R.string.token_primary to colors.primary,
        R.string.token_on_primary to colors.onPrimary,
        R.string.token_outline to colors.outline,
        R.string.token_tour to colors.tour,
        R.string.token_on_tour to colors.onTour,
        R.string.token_navigation_arrow to colors.navigationArrow,
        R.string.token_off_route to colors.offRoute,
        R.string.token_warning to colors.warning,
        R.string.token_error to colors.error,
    )
    val textStyles = listOf(
        R.string.token_distance to typography.distance,
        R.string.token_distance_compact to typography.distanceCompact,
        R.string.token_distance_medium to typography.distanceMedium,
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

    ProvideAppLanguage(language) { CompositionLocalProvider(LocalOffline provides (forceOffline || LocalOffline.current)) {
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
                    text = stringResource(R.string.catalog_language_label, stringResource(language.label)),
                    onClick = { language = language.next() },
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
            // solo en debug: el tutorial háptico (paso 13) como en un reloj sin vibrador
            noVibrator?.let { on ->
                item {
                    HaptiButton(
                        text = stringResource(if (on) R.string.catalog_vibrator_restore else R.string.catalog_vibrator_simulate_none),
                        onClick = { onNoVibratorChange(!on) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = HaptiButtonVariant.Secondary,
                    )
                }
            }

            // Atomic Design de abajo arriba: cada componente de :core:designsystem al menos una vez (los tonos y variantes, juntos)
            item { SectionHeader(R.string.catalog_section_atoms) }
            item {
                HaptiButton(
                    text = stringResource(if (forceOffline) R.string.catalog_offline_stop else R.string.catalog_offline_simulate),
                    onClick = { forceOffline = !forceOffline },
                    modifier = Modifier.fillMaxWidth(),
                    variant = HaptiButtonVariant.Secondary,
                )
            }
            item { HaptiButton(text = stringResource(DsR.string.sample_button_primary), onClick = {}, modifier = Modifier.fillMaxWidth()) }
            item {
                HaptiButton(
                    text = stringResource(DsR.string.sample_button_secondary),
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    variant = HaptiButtonVariant.Secondary,
                )
            }
            // la acción principal en el borde; aquí suelta, en una pantalla va en GlanceScreenTemplate(edgeButton)
            ModeTone.entries.forEach { tone ->
                item { HaptiEdgeButton(text = stringResource(DsR.string.sample_button_primary), onClick = {}, tone = tone) }
            }
            item { VoiceSearchButton(label = stringResource(DsR.string.sample_voice_search), onClick = {}) }
            item { ActiveRouteButton(text = stringResource(DsR.string.sample_place_museum), navigating = true, onClick = {}) }
            item { MapControlButton(iconRes = DsR.drawable.ic_recenter, contentDescription = stringResource(DsR.string.map_recenter), onClick = {}) }
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
            item { ChoiceChip(label = stringResource(DsR.string.sample_choice_selected), selected = true, onClick = {}) }
            item { ChoiceChip(label = stringResource(DsR.string.sample_choice_disabled), selected = false, onClick = {}, enabled = false) }
            item { HeadingText(stringResource(DsR.string.sample_place_museum)) }
            item { BodyText(stringResource(DsR.string.sample_status_error)) }
            item { InstructionText(stringResource(DsR.string.sample_instruction)) }
            item { SectionLabel(stringResource(DsR.string.sample_section_label)) }
            item { DistanceText(meters = 1500) }
            item { DistanceText(meters = 5, compact = true) }
            item { RouteStatsText(distanceMeters = 1200, duration = 15.minutes) }
            StatusTone.entries.forEach { tone -> item { NoticeLabel(stringResource(DsR.string.sample_notice), tone) } }
            item { StepIndicator(current = 2, total = 6) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.m), verticalAlignment = Alignment.CenterVertically) {
                    HaptiIconSize.entries.forEach {
                        HaptiIcon(painter = painterResource(DsR.drawable.ic_straight), contentDescription = null, size = it)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
                    ArrowDirection.entries.forEach { DirectionArrow(direction = it, size = HaptiIconSize.Medium) }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                    ModeTone.entries.forEach { IconDisc(painterResource(DsR.drawable.ic_turn_left), null, it.color, it.onColor) }
                }
            }
            item { HapticWaveform(segments = sampleRampSegments, modifier = Modifier.fillMaxWidth()) }

            item { SectionHeader(R.string.catalog_section_molecules) }
            ModeTone.entries.forEach { tone -> item { ManeuverIndicator(direction = ArrowDirection.LEFT, distanceMeters = 20, tone = tone) } }
            item { MapManeuverCard(direction = ArrowDirection.RIGHT, distanceMeters = 150) }
            item { PlaceRow(item = samplePlaceRows().first(), onClick = {}) }
            ModeTone.entries.forEach { tone ->
                item {
                    val focus = tone == ModeTone.Focus
                    ProfileOption(
                        icon = painterResource(if (focus) DsR.drawable.ic_profile_focus else DsR.drawable.ic_profile_tour),
                        title = stringResource(if (focus) DsR.string.sample_profile_focus else DsR.string.sample_profile_tour),
                        subtitle = stringResource(if (focus) DsR.string.sample_profile_focus_subtitle else DsR.string.sample_profile_tour_subtitle),
                        selected = focus,
                        onClick = {},
                        tone = tone,
                    )
                }
            }
            item {
                SettingChoiceGroup(
                    title = stringResource(DsR.string.sample_section_label),
                    options = listOf(
                        ChoiceOption(stringResource(DsR.string.sample_choice_unselected), selected = selectedChoice == 0, onClick = { selectedChoice = 0 }),
                        ChoiceOption(stringResource(DsR.string.sample_choice_selected), selected = selectedChoice == 1, onClick = { selectedChoice = 1 }),
                        ChoiceOption(stringResource(DsR.string.sample_choice_disabled), selected = false, onClick = {}, enabled = false),
                    ),
                    note = stringResource(DsR.string.sample_choice_note),
                )
            }
            item {
                SettingStepperRow(
                    title = stringResource(DsR.string.sample_section_label),
                    value = stepperValue.toString(),
                    onDecrement = { stepperValue-- },
                    onIncrement = { stepperValue++ },
                    decrementDescription = stringResource(DsR.string.sample_step_less),
                    incrementDescription = stringResource(DsR.string.sample_step_more),
                    decrementEnabled = stepperValue > 1,
                    incrementEnabled = stepperValue < 10,
                )
            }
            item {
                SuggestionCountdown(progress = 0.6f, contentDescription = "") {
                    IconDisc(painterResource(DsR.drawable.ic_exploration_museum), null, colors.tour, colors.onTour, size = HaptiMapTheme.dimens.poiDisc)
                }
            }
            item { HapticCueBadge(stringResource(DsR.string.direction_left), sampleLeftSegments, progress = 0.4f) }
            item {
                HapticPatternCard(
                    title = stringResource(DsR.string.direction_left),
                    caption = stringResource(DsR.string.sample_haptic_caption),
                    segments = sampleLeftSegments,
                    onPlay = { onPlayCue?.invoke(HapticEvent.TURN_LEFT) },
                )
            }

            item { SectionHeader(R.string.catalog_section_organisms) }
            item { PlaceResultsList(items = samplePlaceRows(), onItemClick = {}) }
            item { SearchStatusMessage(SearchStatus.Loading) }
            item { SearchStatusMessage(SearchStatus.Failed(PlaceSearchError.NO_RESULTS)) }
            item { SearchStatusMessage(SearchStatus.Failed(PlaceSearchError.NO_CONNECTION)) }
            item { SearchStatusMessage(SearchStatus.Failed(PlaceSearchError.API_INVALID_KEY)) }
            item { RouteStatusMessage(RouteStatus.Loading) }
            item { RouteStatusMessage(RouteStatus.Failed(RouteError.NO_ROUTE)) }
            item { RouteStatusMessage(RouteStatus.Failed(RouteError.MISSING_KEY)) }
            item { RouteOfflineNotice() }
            // las plantillas son pantalla completa: se abren encima del catálogo
            item {
                HaptiButton(
                    text = stringResource(R.string.catalog_place_detail_open),
                    onClick = { placeDetail = true },
                    modifier = Modifier.fillMaxWidth(),
                    variant = HaptiButtonVariant.Secondary,
                )
            }
            item {
                HaptiButton(
                    text = stringResource(R.string.catalog_confirm_open),
                    onClick = { confirming = true },
                    modifier = Modifier.fillMaxWidth(),
                    variant = HaptiButtonVariant.Secondary,
                )
            }

            // cada aviso suena en el motor de verdad y su disco late al mismo ritmo (PulseRings con el HapticPulse del motor)
            onPlayCue?.let { play ->
                item { SectionHeader(R.string.catalog_section_cues) }
                HapticEvent.entries.forEach { event ->
                    item { CueDemo(event, pulse?.takeIf { it.first == event }?.second, onPlay = { play(event) }) }
                }
            }

            // paso 12: los mismos componentes dentro de HaptiMapTheme(ambient = true), como los pinta AmbientScreenTemplate
            item { SectionHeader(R.string.catalog_section_ambient) }
            item { AmbientSample() }

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
        if (placeDetail) {
            BackHandler { placeDetail = false }
            Box(Modifier.fillMaxSize().background(colors.background)) {
                PlaceDetailPager(
                    items = samplePlaceDetails(),
                    initialPage = 0,
                    goAction = { HaptiEdgeButton(text = stringResource(DsR.string.sample_button_go), onClick = { placeDetail = false }) },
                )
            }
        }
        ConfirmDialog(
            visible = confirming,
            title = stringResource(DsR.string.sample_confirm_title),
            confirmLabel = stringResource(DsR.string.sample_confirm_yes),
            dismissLabel = stringResource(DsR.string.sample_confirm_no),
            onConfirm = { confirming = false },
            onDismiss = { confirming = false },
        )
    } }
}

/** Un aviso: su disco (como el de la maniobra, con los anillos de [PulseRings]) y el botón que lo hace sonar. */
@Composable
private fun CueDemo(event: HapticEvent, pulse: HapticPulse?, onPlay: () -> Unit) {
    val dimens = HaptiMapTheme.dimens
    val tone = if (event == HapticEvent.POI_SUGGESTION || event == HapticEvent.POI_ARRIVAL) ModeTone.Tour else ModeTone.Focus
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        Box(Modifier.size(dimens.maneuverDisc + dimens.maneuverRingGap * 2), contentAlignment = Alignment.Center) {
            PulseRings(pulse, tone.color, Modifier.matchParentSize())
            IconDisc(painterResource(event.icon), null, tone.color, tone.onColor)
        }
        HaptiButton(text = stringResource(event.label), onClick = onPlay, modifier = Modifier.fillMaxWidth(), icon = painterResource(DsR.drawable.ic_play))
    }
}

@get:DrawableRes
private val HapticEvent.icon: Int
    get() = when (this) {
        HapticEvent.TURN_LEFT -> DsR.drawable.ic_turn_left
        HapticEvent.TURN_RIGHT -> DsR.drawable.ic_turn_right
        HapticEvent.ARRIVAL, HapticEvent.POI_ARRIVAL -> DsR.drawable.ic_arrive
        HapticEvent.OFF_ROUTE -> DsR.drawable.ic_off_route
        HapticEvent.POI_SUGGESTION -> DsR.drawable.ic_exploration_museum
        HapticEvent.PROFILE_SELECTED -> DsR.drawable.ic_profile_focus
        HapticEvent.CONTINUE, HapticEvent.TURN_IMMINENT, HapticEvent.BACK_ON_ROUTE -> DsR.drawable.ic_straight
    }

@get:StringRes
private val LanguageOption.label: Int
    get() = when (this) {
        LanguageOption.SYSTEM -> SettingsR.string.settings_language_system
        LanguageOption.ENGLISH -> SettingsR.string.settings_language_english
        LanguageOption.SPANISH -> SettingsR.string.settings_language_spanish
    }

private fun LanguageOption.next(): LanguageOption = LanguageOption.entries[(ordinal + 1) % LanguageOption.entries.size]

/** Flechas de contorno, distancia fina, aviso sin píldora y carga sin spinner, sobre negro con cualquier tema. */
@Composable
private fun AmbientSample() = HaptiMapTheme(ambient = true) {
    Column(
        modifier = Modifier.fillMaxWidth().background(HaptiMapTheme.colors.background, HaptiMapTheme.shapes.large).padding(HaptiMapTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s),
    ) {
        ManeuverIndicator(direction = ArrowDirection.RIGHT, distanceMeters = 150)
        Row(horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m)) {
            ArrowDirection.entries.forEach { DirectionArrow(direction = it, size = HaptiIconSize.Medium) }
        }
        RouteOfflineNotice()
        RouteStatusMessage(RouteStatus.Loading)
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
    Box(Modifier.size(HaptiMapTheme.dimens.iconMedium).background(HaptiMapTheme.colors.primary, shape))
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
