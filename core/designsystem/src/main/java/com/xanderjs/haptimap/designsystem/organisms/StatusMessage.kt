package com.xanderjs.haptimap.designsystem.organisms

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Tono de un [StatusMessage]: Default (texto normal, p. ej. cargando), Muted (no es un fallo, p. ej. sin
 * resultados), Warning (una condición que el usuario puede esperar, como sin red) o Error.
 */
enum class StatusTone { Default, Muted, Warning, Error }

/**
 * Estado de una carga en el centro de la pantalla: [text] ya resuelto por la pantalla, con un spinner
 * si [loading]. TalkBack lo anuncia al cambiar. Qué error da qué texto y tono lo decide cada pantalla.
 */
@Composable
fun StatusMessage(
    text: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Default,
    loading: Boolean = false,
) {
    val colors = HaptiMapTheme.colors
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m),
    ) {
        if (loading) {
            val spinnerModifier = Modifier.size(HaptiMapTheme.dimens.iconMedium)
            // previews y capturas pintan un fotograma de la animación (vacío o a medias, según el momento):
            // ahí un arco fijo con el mismo trazo, para que la referencia sea estable
            if (LocalInspectionMode.current) {
                PreviewSpinner(colors.primary, spinnerModifier)
            } else {
                CircularProgressIndicator(
                    modifier = spinnerModifier,
                    colors = ProgressIndicatorDefaults.colors(indicatorColor = colors.primary, trackColor = Color.Transparent),
                )
            }
        }
        val color = when (tone) {
            StatusTone.Default -> colors.onBackground
            StatusTone.Muted -> colors.onSurfaceVariant
            StatusTone.Warning -> colors.warning
            StatusTone.Error -> colors.error
        }
        Text(text, style = HaptiMapTheme.typography.body, color = color, textAlign = TextAlign.Center)
    }
}

/** Un fotograma fijo del spinner: arco de 270° desde arriba, con el trazo del indeterminado. */
@Composable
private fun PreviewSpinner(color: Color, modifier: Modifier) {
    val stroke = with(LocalDensity.current) { CircularProgressIndicatorDefaults.IndeterminateStrokeWidth.toPx() }
    Canvas(modifier) {
        val inset = stroke / 2
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@HaptiPreviews
@Composable
private fun StatusMessageLoadingPreview() = PreviewSurface { StatusMessage(stringResource(R.string.sample_status_loading), loading = true) }

@HaptiPreviews
@Composable
private fun StatusMessageErrorPreview() = PreviewSurface { StatusMessage(stringResource(R.string.sample_status_error), tone = StatusTone.Error) }
