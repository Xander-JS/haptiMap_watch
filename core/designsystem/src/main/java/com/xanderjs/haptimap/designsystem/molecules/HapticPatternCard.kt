package com.xanderjs.haptimap.designsystem.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.HaptiIcon
import com.xanderjs.haptimap.designsystem.atoms.HaptiIconSize
import com.xanderjs.haptimap.designsystem.atoms.HapticWaveform
import com.xanderjs.haptimap.designsystem.atoms.WaveformSegment
import com.xanderjs.haptimap.designsystem.atoms.sampleLeftSegments
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Un patrón háptico que se puede probar: nombre, su forma ([HapticWaveform]) y un resumen. Toda la tarjeta es
 * el botón de reproducir. Mientras suena ([progress] no null) el borde y la parte ya sonada van en accent.
 * Base del banco de pruebas (paso 09) y del tutorial háptico (paso 13). TalkBack lee título y resumen y
 * ofrece "Reproducir".
 */
@Composable
fun HapticPatternCard(
    title: String,
    caption: String,
    segments: List<WaveformSegment>,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    scaleMs: Long = segments.sumOf { it.durationMs },
    progress: Float? = null,
    tint: Color = HaptiMapTheme.colors.primary,
) {
    val colors = HaptiMapTheme.colors
    val shape = HaptiMapTheme.shapes.large
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HaptiMapTheme.dimens.minTouchTarget)
            .clip(shape)
            .background(colors.surface)
            .border(HaptiMapTheme.dimens.borderWidth, if (progress != null) colors.accent else colors.surface, shape)
            .clickable(onClickLabel = stringResource(R.string.haptic_play), role = Role.Button, onClick = onPlay)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = HaptiMapTheme.spacing.xl, vertical = HaptiMapTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
            Text(title, modifier = Modifier.weight(1f), style = HaptiMapTheme.typography.label, color = colors.onSurface, maxLines = 2)
            HaptiIcon(painterResource(R.drawable.ic_play), contentDescription = null, size = HaptiIconSize.Small, tint = tint)
        }
        HapticWaveform(segments, scaleMs = scaleMs, progress = progress, tint = tint)
        Text(caption, style = HaptiMapTheme.typography.caption, color = colors.onSurfaceVariant, maxLines = 1)
    }
}

@HaptiPreviews
@Composable
private fun HapticPatternCardPreview() = PreviewSurface {
    Column(Modifier.padding(horizontal = HaptiMapTheme.spacing.xl), verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        HapticPatternCard(stringResource(R.string.direction_left), stringResource(R.string.sample_haptic_caption), sampleLeftSegments, onPlay = {}, scaleMs = 900)
        HapticPatternCard(stringResource(R.string.direction_left), stringResource(R.string.sample_haptic_caption), sampleLeftSegments, onPlay = {}, scaleMs = 900, progress = 0.5f)
    }
}
