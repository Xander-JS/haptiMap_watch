package com.xanderjs.haptimap.feature.settings

import androidx.compose.runtime.Composable
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.core.model.DetourTolerance
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.HapticIntensity
import com.xanderjs.haptimap.core.model.ThemeOption
import com.xanderjs.haptimap.core.model.UserSettings
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiLargeFontPreviews
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// La pantalla de ajustes (paso 25) en las 12 combinaciones de @HaptiPreviews: redonda pequeña/grande y cuadrada,
// claro/oscuro, inglés/español. Referencias: ./gradlew :feature:settings:updateDebugScreenshotTest;
// comprobar: :feature:settings:validateDebugScreenshotTest.

@Composable
private fun Screen(settings: UserSettings, anchorItem: Int = 0, navigating: Boolean = false) = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        SettingsContent(
            settings = settings,
            navigating = navigating,
            onHapticIntensity = {}, onSuggestionTimeout = {}, onToggleFavorite = {}, onDetourTolerance = {},
            onDisplayMode = {}, onTheme = {}, onLanguage = {}, onReplayTutorial = {},
            listState = rememberTransformingLazyColumnState(initialAnchorItemIndex = anchorItem),
        )
    }
}

// arriba: título, intensidad háptica (Media marcada) y la espera de sugerencias
@PreviewTest @HaptiPreviews @Composable
fun SettingsTop() = Screen(UserSettings())

// desplazada a favoritas, con varias marcadas y otros ajustes cambiados: se ven selección múltiple y única
@PreviewTest @HaptiPreviews @Composable
fun SettingsFavorites() = Screen(
    UserSettings(
        hapticIntensity = HapticIntensity.HIGH,
        favoriteCategories = setOf(ExplorationCategory.MUSEUM, ExplorationCategory.VIEWPOINT),
        detourTolerance = DetourTolerance.LONG,
        theme = ThemeOption.DARK,
    ),
    anchorItem = 3,
)

// durante un trayecto: al final, el botón de repetir el tutorial desactivado y la nota que lo explica
@PreviewTest @HaptiPreviews @Composable
fun SettingsDuringTrip() = Screen(UserSettings(), anchorItem = 8, navigating = true)

@PreviewTest @HaptiLargeFontPreviews @Composable
fun LargeFontSettingsTop() = SettingsTop()
