package com.xanderjs.haptimap.presentation.debug

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Paso 09: el banco de pruebas háptico con los patrones reales, en las 12 combinaciones de @HaptiPreviews.
// Referencias: ./gradlew :app:updateDebugScreenshotTest.

@Composable
private fun Bench(playing: HapticEvent?, progress: Float?) = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { HapticsDebugContent(playing, progress, onPlay = {}) }
}

@PreviewTest @HaptiPreviews @Composable
fun HapticsBench() = Bench(null, null)

/** Girar a la izquierda a medio sonar. */
@PreviewTest @HaptiPreviews @Composable
fun HapticsBenchPlaying() = Bench(HapticEvent.TURN_LEFT, 0.5f)
