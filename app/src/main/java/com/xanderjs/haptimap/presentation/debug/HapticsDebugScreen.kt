package com.xanderjs.haptimap.presentation.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.R
import com.xanderjs.haptimap.feature.onboarding.R as OnboardingR
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticTokens
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.molecules.HapticPatternCard
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate
import com.xanderjs.haptimap.feature.onboarding.hapticScaleMs
import com.xanderjs.haptimap.feature.onboarding.label
import com.xanderjs.haptimap.feature.onboarding.playbackProgress
import com.xanderjs.haptimap.feature.onboarding.tint
import com.xanderjs.haptimap.feature.onboarding.toSegments

/**
 * Banco de pruebas háptico (paso 09, solo debug): cada patrón con su forma y un toque para reproducirlo.
 * En el emulador no vibra: el log HaptiHaptics y la barra de progreso muestran lo que sonaría. Base del tutorial del paso 13.
 */
@Composable
fun HapticsDebugScreen(viewModel: HapticsDebugViewModel = hiltViewModel()) {
    val played by viewModel.played.collectAsStateWithLifecycle()
    HapticsDebugContent(playing = played?.event, progress = playbackProgress(played), onPlay = viewModel::play)
}

/** [progress] es el de [playing]; null cuando no suena nada. */
@Composable
internal fun HapticsDebugContent(playing: HapticEvent?, progress: Float?, onPlay: (HapticEvent) -> Unit) {
    RoundScreenTemplate {
        item { HeadingText(stringResource(R.string.catalog_section_haptics)) }
        HapticEvent.entries.forEach { event ->
            item {
                val pattern = HapticTokens.patternFor(event)
                HapticPatternCard(
                    title = stringResource(event.label),
                    caption = pluralStringResource(OnboardingR.plurals.haptic_pattern_summary, pattern.pulseCount, pattern.pulseCount, pattern.totalDurationMs.toInt()),
                    segments = pattern.toSegments(),
                    onPlay = { onPlay(event) },
                    scaleMs = hapticScaleMs,
                    progress = progress.takeIf { event == playing },
                    tint = event.tint,
                )
            }
        }
    }
}
