package com.xanderjs.haptimap.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import androidx.compose.ui.res.stringResource
import com.xanderjs.haptimap.designsystem.atoms.BodyText
import com.xanderjs.haptimap.designsystem.atoms.StepIndicator
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Paso 13: las piezas nuevas del tutorial háptico en las 12 combinaciones: el indicador de paso (al principio, a
// medias y al final) y el párrafo BodyText.

@PreviewTest @HaptiPreviews @Composable
fun StepIndicators() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m)) {
        StepIndicator(current = 1, total = 6)
        StepIndicator(current = 4, total = 6)
        StepIndicator(current = 6, total = 6)
    }
}

@PreviewTest @HaptiPreviews @Composable
fun StepIndicatorAmbient() = PreviewSurface(ambient = true) { StepIndicator(current = 3, total = 6) }

@PreviewTest @HaptiPreviews @Composable
fun BodyTextSample() = PreviewSurface { BodyText(stringResource(R.string.sample_status_error)) }
