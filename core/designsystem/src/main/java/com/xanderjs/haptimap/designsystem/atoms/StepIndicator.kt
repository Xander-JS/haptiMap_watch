package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * En qué paso de un recorrido lineal se está (tutorial háptico, paso 13): [current] de [total], desde 1.
 * Se lee sin color: hechos, punto relleno; el actual, más ancho; los que faltan, solo el contorno.
 * TalkBack lee "Paso 2 de 6".
 */
@Composable
fun StepIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    val dimens = HaptiMapTheme.dimens
    val colors = HaptiMapTheme.colors
    val description = stringResource(R.string.step_indicator, current, total)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s),
    ) {
        for (step in 1..total) {
            val shape = HaptiMapTheme.shapes.full
            val dot = Modifier.size(width = if (step == current) dimens.stepDotCurrentWidth else dimens.stepDot, height = dimens.stepDot)
            Box(
                if (step <= current) {
                    dot.background(colors.primary, shape)
                } else {
                    dot.border(dimens.borderWidth, colors.onSurfaceVariant, shape)
                },
            )
        }
    }
}

@HaptiPreviews
@Composable
private fun StepIndicatorPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m)) {
        StepIndicator(current = 1, total = 6)
        StepIndicator(current = 4, total = 6)
        StepIndicator(current = 6, total = 6)
    }
}
