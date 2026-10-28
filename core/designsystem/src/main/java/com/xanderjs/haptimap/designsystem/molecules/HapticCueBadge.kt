package com.xanderjs.haptimap.designsystem.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.HapticWaveform
import com.xanderjs.haptimap.designsystem.atoms.WaveformSegment
import com.xanderjs.haptimap.designsystem.atoms.sampleLeftSegments
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * El patrón que acaba de sonar, en pequeño: su forma y su nombre en una píldora. No es interactivo;
 * TalkBack lo anuncia al aparecer. Con poco ancho, el nombre se corta con elipsis y la forma se mantiene. Lo usa el indicador de debug mientras se navega (paso 09).
 */
@Composable
fun HapticCueBadge(
    title: String,
    segments: List<WaveformSegment>,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    tint: Color = HaptiMapTheme.colors.primary,
) {
    val dimens = HaptiMapTheme.dimens
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .background(HaptiMapTheme.colors.surface, HaptiMapTheme.shapes.full)
            .padding(horizontal = HaptiMapTheme.spacing.l, vertical = HaptiMapTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m),
    ) {
        HapticWaveform(
            segments,
            modifier = Modifier.width(dimens.waveformCompactWidth).height(dimens.waveformCompactHeight),
            progress = progress,
            tint = tint,
        )
        Text(
            title,
            modifier = Modifier.weight(1f, fill = false),
            style = HaptiMapTheme.typography.caption,
            color = HaptiMapTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@HaptiPreviews
@Composable
private fun HapticCueBadgePreview() = PreviewSurface {
    HapticCueBadge(stringResource(R.string.direction_left), sampleLeftSegments, progress = 0.4f)
}
