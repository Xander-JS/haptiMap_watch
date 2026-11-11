package com.xanderjs.haptimap.designsystem.molecules

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.IconDisc
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Cuenta atrás de una sugerencia de POI (paso 19): un anillo en el color del Tour que se vacía con [progress] (1 → 0)
 * alrededor de [content] (el disco del POI). Sin número: el anillo ya dice cuánto queda y la pantalla tiene menos que
 * leer; [contentDescription] da los segundos a TalkBack. Dibujado a mano (arco), no un indicador animado, para que
 * las capturas sean deterministas. El mismo tamaño que el disco de la maniobra con su anillo.
 */
@Composable
fun SuggestionCountdown(
    progress: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = HaptiMapTheme.colors
    val dimens = HaptiMapTheme.dimens
    Box(
        modifier = modifier
            .size(dimens.maneuverDisc + dimens.maneuverRingGap * 2)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val strokePx = dimens.outlineWidth.toPx() * 2
            val topLeft = Offset(strokePx / 2, strokePx / 2)
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            drawArc(colors.outline, 0f, 360f, false, topLeft, arcSize, style = Stroke(width = dimens.outlineWidth.toPx()))
            drawArc(colors.tour, -90f, 360f * progress.coerceIn(0f, 1f), false, topLeft, arcSize, style = Stroke(width = strokePx, cap = StrokeCap.Round))
        }
        content()
    }
}

@HaptiPreviews
@Composable
private fun SuggestionCountdownPreview() = PreviewSurface {
    val colors = HaptiMapTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m), verticalAlignment = Alignment.CenterVertically) {
        listOf(1f, 0.4f).forEach { progress ->
            SuggestionCountdown(progress = progress, contentDescription = "") {
                IconDisc(painterResource(R.drawable.ic_exploration_museum), null, colors.tour, colors.onTour, size = HaptiMapTheme.dimens.poiDisc)
            }
        }
    }
}
