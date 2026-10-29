package com.xanderjs.haptimap.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.designsystem.atoms.CategoryChip
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.VoiceSearchButton
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Piezas del Modo Búsqueda (paso 11) en las 12 combinaciones de @HaptiPreviews. La pantalla entera, con sus
// textos reales, se captura en :feature:search. Referencias: ./gradlew :core:designsystem:updateDebugScreenshotTest.

@PreviewTest @HaptiPreviews @Composable
fun SearchCategoryChips() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        CategoryChip(stringResource(R.string.sample_category_pharmacy), painterResource(R.drawable.ic_category_pharmacy), selected = true, onClick = {})
        CategoryChip(stringResource(R.string.sample_category_cafe), painterResource(R.drawable.ic_category_cafe), selected = false, onClick = {})
    }
}

@PreviewTest @HaptiPreviews @Composable
fun SearchVoiceButton() = PreviewSurface { VoiceSearchButton(label = stringResource(R.string.sample_voice_search), onClick = {}) }

@PreviewTest @HaptiPreviews @Composable
fun SearchIconButton() = PreviewSurface {
    HaptiButton(
        text = stringResource(R.string.sample_place_museum),
        onClick = {},
        variant = HaptiButtonVariant.Secondary,
        icon = painterResource(R.drawable.ic_straight),
    )
}
