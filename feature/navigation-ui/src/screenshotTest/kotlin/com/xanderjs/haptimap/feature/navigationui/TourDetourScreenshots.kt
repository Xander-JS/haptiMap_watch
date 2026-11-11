package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.designsystem.R as DsR
import com.xanderjs.haptimap.designsystem.atoms.HaptiEdgeButton
import com.xanderjs.haptimap.designsystem.atoms.ModeTone
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.templates.DetourCard
import com.xanderjs.haptimap.designsystem.preview.HaptiLargeFontPreviews
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Paso 20: la tarjeta del desvío en las 12 combinaciones de @HaptiPreviews (formas, temas, idiomas) y con la letra
// más grande. Referencias: ./gradlew :feature:navigation-ui:updateDebugScreenshotTest.

@Composable
private fun Card(title: String, category: ExplorationCategory, status: String) = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        DetourCard(
            title = title,
            categoryIcon = painterResource(category.explorationIcon),
            categoryName = stringResource(category.labelRes),
            status = status,
            cancelAction = { HaptiEdgeButton(text = stringResource(R.string.tour_detour_cancel), onClick = {}, tone = ModeTone.Tour) },
        )
    }
}

/** De camino al POI: nombre del café y la distancia que falta (lo que pide el paso 20). */
@PreviewTest @HaptiPreviews @Composable
fun DetourToPoi() = Card(stringResource(DsR.string.sample_place_cafe), ExplorationCategory.CAFE_RESTAURANT, "120 m")

/** Nombre largo (una línea con puntos suspensivos) y el estado de vuelta al destino. */
@PreviewTest @HaptiPreviews @Composable
fun DetourReturning() = Card("Museo Nacional Centro de Arte Reina Sofía", ExplorationCategory.CULTURAL_SITE, stringResource(R.string.tour_detour_returning))

/** El mismo caso de ida con la letra más grande del sistema (paso 15A, accesibilidad). */
@PreviewTest @HaptiLargeFontPreviews @Composable
fun LargeFontDetourToPoi() = DetourToPoi()
