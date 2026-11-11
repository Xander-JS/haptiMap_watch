package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/** Un tramo de vibración: [durationMs] y su fuerza [level] de 0 (pausa) a 1 (máxima). */
@Immutable
data class WaveformSegment(val durationMs: Long, val level: Float)

/** Lo más bajo que se dibuja un pulso: uno suave tiene que verse aunque sea poco. */
private const val MIN_BAR_FRACTION = 0.25f

/**
 * Un patrón háptico dibujado: el ancho es el tiempo, la altura la fuerza. Los pulsos son barras centradas;
 * las pausas, la línea base sola. [scaleMs] es cuánto tiempo cabe en el ancho: con el mismo en toda una
 * lista, los patrones se comparan de un vistazo (por defecto, el propio patrón). [progress] (0..1) pinta en
 * [playedTint] lo que ya ha sonado. [tint] debe ser un color semántico de HaptiMapTheme.colors.
 * Decorativo: quien lo usa da el texto para TalkBack.
 */
@Composable
fun HapticWaveform(
    segments: List<WaveformSegment>,
    modifier: Modifier = Modifier,
    scaleMs: Long = segments.sumOf { it.durationMs },
    progress: Float? = null,
    tint: Color = HaptiMapTheme.colors.primary,
    playedTint: Color = HaptiMapTheme.colors.onSurface,
) {
    val baseline = HaptiMapTheme.colors.onSurfaceVariant
    val dimens = HaptiMapTheme.dimens
    Canvas(modifier.fillMaxWidth().height(dimens.waveformHeight)) {
        val msToPx = size.width / scaleMs.coerceAtLeast(1)
        val totalPx = segments.sumOf { it.durationMs } * msToPx
        val stroke = dimens.borderWidth.toPx()
        drawLine(baseline, Offset(0f, center.y), Offset(totalPx, center.y), stroke)
        bars(segments, msToPx, tint)
        if (progress != null) clipRect(right = totalPx * progress.coerceIn(0f, 1f)) { bars(segments, msToPx, playedTint) }
    }
}

// cada pulso es una píldora: se lee como un toque, no como un bloque
private fun DrawScope.bars(segments: List<WaveformSegment>, msToPx: Float, color: Color) {
    var x = 0f
    segments.forEach { segment ->
        val width = segment.durationMs * msToPx
        if (segment.level > 0f) {
            val height = size.height * (MIN_BAR_FRACTION + (1 - MIN_BAR_FRACTION) * segment.level.coerceAtMost(1f))
            val radius = minOf(width, height) / 2
            drawRoundRect(color, Offset(x, center.y - height / 2), Size(width, height), CornerRadius(radius))
        }
        x += width
    }
}

@HaptiPreviews
@Composable
private fun HapticWaveformPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m)) {
        HapticWaveform(sampleLeftSegments, scaleMs = 900)
        HapticWaveform(sampleRampSegments, scaleMs = 900, progress = 0.6f)
    }
}

/** Datos de ejemplo para previews y capturas: el patrón real sale de HapticTokens en :core:model. */
val sampleLeftSegments = listOf(WaveformSegment(200, 0.63f), WaveformSegment(250, 0f), WaveformSegment(200, 0.63f))
val sampleRampSegments = listOf(
    WaveformSegment(40, 0.31f), WaveformSegment(150, 0f), WaveformSegment(70, 0.63f), WaveformSegment(110, 0f),
    WaveformSegment(100, 1f), WaveformSegment(70, 0f), WaveformSegment(150, 1f),
)
