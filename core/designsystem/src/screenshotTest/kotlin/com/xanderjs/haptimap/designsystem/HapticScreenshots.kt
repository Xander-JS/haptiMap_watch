package com.xanderjs.haptimap.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.designsystem.atoms.HapticWaveform
import com.xanderjs.haptimap.designsystem.atoms.sampleLeftSegments
import com.xanderjs.haptimap.designsystem.atoms.sampleRampSegments
import com.xanderjs.haptimap.designsystem.molecules.HapticCueBadge
import com.xanderjs.haptimap.designsystem.molecules.HapticPatternCard
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Paso 09: los componentes hápticos en las 12 combinaciones de @HaptiPreviews. Con datos reales, en :app (HapticsDebugScreenshots).

/** En reposo y a medio sonar, con la misma escala: el ancho es tiempo. */
@PreviewTest @HaptiPreviews @Composable
fun HapticWaveforms() = PreviewSurface {
    Column(Modifier.padding(horizontal = HaptiMapTheme.spacing.xl), verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m)) {
        HapticWaveform(sampleLeftSegments, scaleMs = 900)
        HapticWaveform(sampleRampSegments, scaleMs = 900, progress = 0.6f)
    }
}

@PreviewTest @HaptiPreviews @Composable
fun HapticPatternCards() = PreviewSurface {
    Column(Modifier.padding(horizontal = HaptiMapTheme.spacing.xl), verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        val title = stringResource(R.string.direction_left)
        val caption = stringResource(R.string.sample_haptic_caption)
        HapticPatternCard(title, caption, sampleLeftSegments, onPlay = {}, scaleMs = 900)
        HapticPatternCard(title, caption, sampleLeftSegments, onPlay = {}, scaleMs = 900, progress = 0.5f)
    }
}

@PreviewTest @HaptiPreviews @Composable
fun HapticCueBadgeSample() = PreviewSurface {
    HapticCueBadge(stringResource(R.string.direction_left), sampleLeftSegments, progress = 0.4f)
}
