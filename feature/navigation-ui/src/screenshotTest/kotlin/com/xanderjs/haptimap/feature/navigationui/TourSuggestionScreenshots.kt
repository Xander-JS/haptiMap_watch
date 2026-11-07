package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.designsystem.R as DsR
import com.xanderjs.haptimap.designsystem.atoms.HaptiEdgeButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.organisms.SuggestionCard
import com.xanderjs.haptimap.designsystem.preview.HaptiLargeFontPreviews
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Paso 19: la tarjeta de sugerencia de POI en las 12 combinaciones de @HaptiPreviews (redonda pequeña/grande/cuadrada
// × claro/oscuro × inglés/español). Referencias: ./gradlew :feature:navigation-ui:updateDebugScreenshotTest.

@Composable
private fun Card(
    title: String,
    category: ExplorationCategory,
    addedTime: String,
    seconds: Int,
    progress: Float,
) = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        SuggestionCard(
            title = title,
            categoryIcon = painterResource(category.explorationIcon),
            categoryName = stringResource(category.labelRes),
            addedTime = addedTime,
            secondsRemaining = seconds,
            progress = progress,
            countdownDescription = pluralStringResource(R.plurals.tour_suggestion_countdown, seconds, seconds),
            dismissLabel = stringResource(R.string.tour_suggestion_dismiss),
            onDismiss = {},
            acceptAction = { HaptiEdgeButton(text = stringResource(R.string.tour_suggestion_accept), onClick = {}) },
        )
    }
}

/** Caso típico: un museo a 3 min, cuenta atrás a media. */
@PreviewTest @HaptiPreviews @Composable
fun TourSuggestionMuseum() = Card(stringResource(DsR.string.sample_place_museum), ExplorationCategory.MUSEUM, "+3 min", 5, 0.6f)

/** Nombre largo (dos líneas + puntos suspensivos), al empezar la cuenta atrás (anillo lleno). */
@PreviewTest @HaptiPreviews @Composable
fun TourSuggestionLongName() = Card("Museo Nacional Centro de Arte Reina Sofía", ExplorationCategory.CULTURAL_SITE, "+2 min", 10, 1f)

/** Café/restaurante con el desvío en segundos y la cuenta atrás casi agotada. */
@PreviewTest @HaptiPreviews @Composable
fun TourSuggestionCafeSeconds() = Card(stringResource(DsR.string.sample_place_cafe), ExplorationCategory.CAFE_RESTAURANT, "+45 s", 2, 0.2f)

/** El mismo caso típico con la letra más grande del sistema (paso 15A, accesibilidad). */
@PreviewTest @HaptiLargeFontPreviews @Composable
fun LargeFontTourSuggestionMuseum() = TourSuggestionMuseum()
