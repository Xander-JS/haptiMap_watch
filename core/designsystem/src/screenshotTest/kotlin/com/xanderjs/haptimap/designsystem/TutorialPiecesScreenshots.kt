package com.xanderjs.haptimap.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AppScaffold
import com.xanderjs.haptimap.designsystem.atoms.DistanceText
import com.xanderjs.haptimap.designsystem.atoms.HaptiEdgeButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.templates.GlanceScreenTemplate
import com.xanderjs.haptimap.designsystem.atoms.BodyText
import com.xanderjs.haptimap.designsystem.atoms.StepIndicator
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Paso 13: las piezas nuevas del tutorial háptico en las 12 combinaciones: el indicador de paso (al principio, a
// medias y al final), el párrafo BodyText, la distancia compacta y la acción principal en el borde
// (GlanceScreenTemplate(edgeButton)): siempre en la primera vista, con mucho texto encima.

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

@PreviewTest @HaptiPreviews @Composable
fun DistanceCompact() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m)) {
        DistanceText(meters = 5, compact = true)
        DistanceText(meters = 1500, compact = true)
    }
}

@PreviewTest @HaptiPreviews @Composable
fun GlanceWithEdgeButton() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        GlanceScreenTemplate(
            glance = { HeadingText(stringResource(R.string.sample_place_museum)) },
            details = listOf(
                @Composable { BodyText(stringResource(R.string.sample_instruction_long)) },
                @Composable { BodyText(stringResource(R.string.sample_instruction_long)) },
            ),
            edgeButton = { HaptiEdgeButton(text = stringResource(R.string.sample_button_primary), onClick = {}) },
        )
    }
}
