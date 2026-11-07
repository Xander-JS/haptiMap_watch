package com.xanderjs.haptimap.designsystem.molecules

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Cuenta atrás de una sugerencia de POI (paso 19): un anillo que se vacía con [progress] (1 → 0) alrededor de los
 * [secondsRemaining]. Dibujado a mano (arco), no un indicador animado, para que las capturas sean deterministas.
 * El anillo usa el color de POI del tema ([com.xanderjs.haptimap.designsystem.tokens.HaptiColors.poiHighlight]), el
 * mismo que resalta los POIs en el resto de la app. [contentDescription] lo lee TalkBack como una sola frase.
 */
@Composable
fun SuggestionCountdown(
    secondsRemaining: Int,
    progress: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = HaptiMapTheme.colors
    val strokePx = with(LocalDensity.current) { 4.dp.toPx() }
    Box(
        modifier = modifier
            .size(48.dp)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val topLeft = Offset(strokePx / 2, strokePx / 2)
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
            drawArc(colors.surface, 0f, 360f, false, topLeft, arcSize, style = stroke)
            drawArc(colors.poiHighlight, -90f, 360f * progress.coerceIn(0f, 1f), false, topLeft, arcSize, style = stroke)
        }
        Text(
            text = secondsRemaining.toString(),
            style = HaptiMapTheme.typography.title,
            color = colors.onBackground,
        )
    }
}

@HaptiPreviews
@Composable
private fun SuggestionCountdownPreview() = PreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m), verticalAlignment = Alignment.CenterVertically) {
        SuggestionCountdown(secondsRemaining = 7, progress = 1f, contentDescription = "7 s")
        SuggestionCountdown(secondsRemaining = 3, progress = 0.4f, contentDescription = "3 s")
    }
}
