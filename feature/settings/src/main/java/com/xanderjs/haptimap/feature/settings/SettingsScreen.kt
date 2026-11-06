package com.xanderjs.haptimap.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import com.xanderjs.haptimap.core.model.DetourTolerance
import com.xanderjs.haptimap.core.model.DisplayMode
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.HapticIntensity
import com.xanderjs.haptimap.core.model.LanguageOption
import com.xanderjs.haptimap.core.model.SUGGESTION_TIMEOUT_SECONDS
import com.xanderjs.haptimap.core.model.ThemeOption
import com.xanderjs.haptimap.core.model.UserSettings
import com.xanderjs.haptimap.designsystem.atoms.BodyText
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.molecules.ChoiceOption
import com.xanderjs.haptimap.designsystem.molecules.SettingChoiceGroup
import com.xanderjs.haptimap.designsystem.molecules.SettingStepperRow
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate

/**
 * Ajustes de la app (paso 25). Una lista redonda con scroll por corona: intensidad háptica (con preview al
 * cambiarla), espera de sugerencias (5–10 s), categorías de exploración favoritas, tolerancia al desvío, modo
 * de pantalla (el mapa continuo aún no existe, paso 22), tema, idioma y repetir el tutorial háptico (paso 13).
 */
@Composable
fun SettingsScreen(onReplayTutorial: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val navigating by viewModel.navigating.collectAsStateWithLifecycle()
    SettingsContent(
        settings = settings,
        navigating = navigating,
        onHapticIntensity = viewModel::setHapticIntensity,
        onSuggestionTimeout = viewModel::setSuggestionTimeout,
        onToggleFavorite = viewModel::toggleFavorite,
        onDetourTolerance = viewModel::setDetourTolerance,
        onDisplayMode = viewModel::setDisplayMode,
        onTheme = viewModel::setTheme,
        onLanguage = viewModel::setLanguage,
        onReplayTutorial = onReplayTutorial,
    )
}

/** La pantalla en un estado dado; las previews y capturas están en src/screenshotTest. */
@Composable
internal fun SettingsContent(
    settings: UserSettings,
    navigating: Boolean,
    onHapticIntensity: (HapticIntensity) -> Unit,
    onSuggestionTimeout: (Int) -> Unit,
    onToggleFavorite: (ExplorationCategory) -> Unit,
    onDetourTolerance: (DetourTolerance) -> Unit,
    onDisplayMode: (DisplayMode) -> Unit,
    onTheme: (ThemeOption) -> Unit,
    onLanguage: (LanguageOption) -> Unit,
    onReplayTutorial: () -> Unit,
    listState: TransformingLazyColumnState = rememberTransformingLazyColumnState(),
) {
    RoundScreenTemplate(listState = listState) {
        item { HeadingText(stringResource(R.string.settings_title)) }

        item {
            SettingChoiceGroup(
                title = stringResource(R.string.settings_haptic_intensity),
                options = HapticIntensity.entries.map { option ->
                    ChoiceOption(stringResource(option.labelRes), selected = settings.hapticIntensity == option, onClick = { onHapticIntensity(option) })
                },
            )
        }

        item {
            SettingStepperRow(
                title = stringResource(R.string.settings_suggestion_timeout),
                value = stringResource(R.string.settings_seconds, settings.suggestionTimeoutSeconds),
                onDecrement = { onSuggestionTimeout(settings.suggestionTimeoutSeconds - 1) },
                onIncrement = { onSuggestionTimeout(settings.suggestionTimeoutSeconds + 1) },
                decrementDescription = stringResource(R.string.settings_timeout_less),
                incrementDescription = stringResource(R.string.settings_timeout_more),
                decrementEnabled = settings.suggestionTimeoutSeconds > SUGGESTION_TIMEOUT_SECONDS.first,
                incrementEnabled = settings.suggestionTimeoutSeconds < SUGGESTION_TIMEOUT_SECONDS.last,
            )
        }

        item {
            SettingChoiceGroup(
                title = stringResource(R.string.settings_favorites),
                options = ExplorationCategory.entries.map { category ->
                    ChoiceOption(stringResource(category.labelRes), selected = category in settings.favoriteCategories, onClick = { onToggleFavorite(category) })
                },
                note = stringResource(R.string.settings_favorites_note),
            )
        }

        item {
            SettingChoiceGroup(
                title = stringResource(R.string.settings_detour),
                options = DetourTolerance.entries.map { option ->
                    ChoiceOption(stringResource(option.labelRes), selected = settings.detourTolerance == option, onClick = { onDetourTolerance(option) })
                },
            )
        }

        item {
            // el mapa continuo llega en el paso 22: se muestra apagado y con una nota, sin romper nada
            SettingChoiceGroup(
                title = stringResource(R.string.settings_display),
                options = DisplayMode.entries.map { option ->
                    ChoiceOption(
                        label = stringResource(option.labelRes),
                        selected = settings.displayMode == option,
                        onClick = { onDisplayMode(option) },
                        enabled = option != DisplayMode.CONTINUOUS_MAP,
                    )
                },
                note = stringResource(R.string.settings_display_note),
            )
        }

        item {
            SettingChoiceGroup(
                title = stringResource(R.string.settings_theme),
                options = ThemeOption.entries.map { option ->
                    ChoiceOption(stringResource(option.labelRes), selected = settings.theme == option, onClick = { onTheme(option) })
                },
            )
        }

        item {
            SettingChoiceGroup(
                title = stringResource(R.string.settings_language),
                options = LanguageOption.entries.map { option ->
                    ChoiceOption(stringResource(option.labelRes), selected = settings.language == option, onClick = { onLanguage(option) })
                },
            )
        }

        // durante un trayecto se puede entrar a ajustes, pero repetir el tutorial se desactiva: sus patrones
        // chocarían con los avisos de la ruta. Se explica con una nota debajo.
        item {
            HaptiButton(
                text = stringResource(R.string.settings_replay_tutorial),
                onClick = onReplayTutorial,
                modifier = Modifier.fillMaxWidth(),
                variant = HaptiButtonVariant.Secondary,
                enabled = !navigating,
            )
        }
        if (navigating) item { BodyText(stringResource(R.string.settings_replay_tutorial_navigating)) }
    }
}

private val HapticIntensity.labelRes: Int
    @StringRes get() = when (this) {
        HapticIntensity.LOW -> R.string.settings_intensity_low
        HapticIntensity.MEDIUM -> R.string.settings_intensity_medium
        HapticIntensity.HIGH -> R.string.settings_intensity_high
    }

private val DetourTolerance.labelRes: Int
    @StringRes get() = when (this) {
        DetourTolerance.SHORT -> R.string.settings_detour_short
        DetourTolerance.MEDIUM -> R.string.settings_detour_medium
        DetourTolerance.LONG -> R.string.settings_detour_long
    }

private val DisplayMode.labelRes: Int
    @StringRes get() = when (this) {
        DisplayMode.ECO_AOD -> R.string.settings_display_eco
        DisplayMode.CONTINUOUS_MAP -> R.string.settings_display_map
    }

private val ThemeOption.labelRes: Int
    @StringRes get() = when (this) {
        ThemeOption.SYSTEM -> R.string.settings_theme_system
        ThemeOption.LIGHT -> R.string.settings_theme_light
        ThemeOption.DARK -> R.string.settings_theme_dark
    }

private val LanguageOption.labelRes: Int
    @StringRes get() = when (this) {
        LanguageOption.SYSTEM -> R.string.settings_language_system
        LanguageOption.ENGLISH -> R.string.settings_language_english
        LanguageOption.SPANISH -> R.string.settings_language_spanish
    }

private val ExplorationCategory.labelRes: Int
    @StringRes get() = when (this) {
        ExplorationCategory.MUSEUM -> R.string.settings_category_museum
        ExplorationCategory.PARK -> R.string.settings_category_park
        ExplorationCategory.LANDMARK -> R.string.settings_category_landmark
        ExplorationCategory.VIEWPOINT -> R.string.settings_category_viewpoint
        ExplorationCategory.CULTURAL_SITE -> R.string.settings_category_cultural
        ExplorationCategory.CAFE_RESTAURANT -> R.string.settings_category_cafe_restaurant
    }
