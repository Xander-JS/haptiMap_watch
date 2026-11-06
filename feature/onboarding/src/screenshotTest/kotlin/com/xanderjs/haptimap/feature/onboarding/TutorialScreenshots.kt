package com.xanderjs.haptimap.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.core.haptics.PlayedHaptic
import com.xanderjs.haptimap.core.model.HapticTokens
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiLargeFontPreviews
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Paso 13: cada pantalla del tutorial háptico en las 12 combinaciones de @HaptiPreviews (formas, temas, idiomas).

@Composable
private fun Screen(step: TutorialStep, canVibrate: Boolean = true, progress: Float? = null) = HaptiMapTheme {
    val played = (step as? TutorialStep.Lesson)?.let { hapticLessons[it.index].event }?.let { PlayedHaptic(it, HapticTokens.patternFor(it)) }
    AppScaffold(timeText = { HaptiTimeText() }) {
        TutorialContent(TutorialUiState(step, canVibrate), hapticLessons, progress, played, {}, {}, {}, {})
    }
}

@PreviewTest @HaptiPreviews @Composable
fun TutorialWelcome() = Screen(TutorialStep.Welcome)

/** Sin vibrador: lo dice antes de empezar, y sin la pista de los ajustes. */
@PreviewTest @HaptiPreviews @Composable
fun TutorialWelcomeNoVibrator() = Screen(TutorialStep.Welcome, canVibrate = false)

@PreviewTest @HaptiPreviews @Composable
fun TutorialTurnLeft() = Screen(TutorialStep.Lesson(0))

@PreviewTest @HaptiPreviews @Composable
fun TutorialTurnRight() = Screen(TutorialStep.Lesson(1))

@PreviewTest @HaptiPreviews @Composable
fun TutorialContinue() = Screen(TutorialStep.Lesson(2))

@PreviewTest @HaptiPreviews @Composable
fun TutorialImminent() = Screen(TutorialStep.Lesson(3))

@PreviewTest @HaptiPreviews @Composable
fun TutorialOffRoute() = Screen(TutorialStep.Lesson(4))

@PreviewTest @HaptiPreviews @Composable
fun TutorialArrival() = Screen(TutorialStep.Lesson(5))

/** Sonando: la tarjeta con el borde y la parte ya sonada en accent. */
@PreviewTest @HaptiPreviews @Composable
fun TutorialTurnLeftPlaying() = Screen(TutorialStep.Lesson(0), progress = 0.5f)

@PreviewTest @HaptiPreviews @Composable
fun TutorialLessonNoVibrator() = Screen(TutorialStep.Lesson(0), canVibrate = false)

@PreviewTest @HaptiPreviews @Composable
fun TutorialDone() = Screen(TutorialStep.Done)

// Paso 15A: el flujo principal con la letra más grande del sistema (1.24), 12 combinaciones

@PreviewTest @HaptiLargeFontPreviews @Composable
fun LargeFontTutorialWelcome() = TutorialWelcome()

@PreviewTest @HaptiLargeFontPreviews @Composable
fun LargeFontTutorialTurnLeft() = TutorialTurnLeft()

@PreviewTest @HaptiLargeFontPreviews @Composable
fun LargeFontTutorialImminent() = TutorialImminent()

@PreviewTest @HaptiLargeFontPreviews @Composable
fun LargeFontTutorialDone() = TutorialDone()
