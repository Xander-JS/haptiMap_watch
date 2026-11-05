package com.xanderjs.haptimap.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.designsystem.atoms.StepIndicator
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Paso 13: el indicador de paso del tutorial háptico, al principio, a medias y al final, en las 12 combinaciones.

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
