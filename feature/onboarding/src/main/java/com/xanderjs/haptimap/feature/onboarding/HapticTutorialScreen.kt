package com.xanderjs.haptimap.feature.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.core.haptics.PlayedHaptic
import com.xanderjs.haptimap.core.model.HapticTokens
import com.xanderjs.haptimap.designsystem.atoms.BodyText
import com.xanderjs.haptimap.designsystem.atoms.DirectionArrow
import com.xanderjs.haptimap.designsystem.atoms.DistanceText
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HaptiIcon
import com.xanderjs.haptimap.designsystem.atoms.HaptiIconSize
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.atoms.NoticeLabel
import com.xanderjs.haptimap.designsystem.atoms.StatusTone
import com.xanderjs.haptimap.designsystem.atoms.StepIndicator
import com.xanderjs.haptimap.designsystem.atoms.color
import com.xanderjs.haptimap.designsystem.molecules.HapticPatternCard
import com.xanderjs.haptimap.designsystem.templates.GlanceScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Tutorial háptico (paso 13, sección 19): bienvenida, una lección por patrón (suena sola una vez; "Repetir" y
 * "Siguiente") y fin. "Saltar" en todas, debajo del borde. [onDone] al terminar o saltar: ya está guardado como visto.
 */
@Composable
fun HapticTutorialScreen(onDone: () -> Unit, viewModel: TutorialViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val played by viewModel.played.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }
    TutorialContent(
        state = state,
        lessons = viewModel.lessons,
        progress = playbackProgress(played),
        played = played,
        onNext = viewModel::next,
        onPlayAgain = viewModel::playAgain,
        onSkip = viewModel::skip,
        onFinish = viewModel::finish,
    )
}

@Composable
internal fun TutorialContent(
    state: TutorialUiState,
    lessons: List<HapticLesson>,
    progress: Float?,
    played: PlayedHaptic?,
    onNext: () -> Unit,
    onPlayAgain: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
) = when (val step = state.step) {
    TutorialStep.Welcome -> WelcomeContent(lessons.size, state.canVibrate, onStart = onNext, onSkip = onSkip)
    is TutorialStep.Lesson -> {
        val lesson = lessons[step.index]
        LessonContent(
            lesson = lesson,
            number = step.index + 1,
            total = lessons.size,
            canVibrate = state.canVibrate,
            // la tarjeta solo se anima con su patrón; en ambiente nada se anima
            progress = progress.takeIf { played?.event == lesson.event && !HaptiMapTheme.ambient },
            onPlayAgain = onPlayAgain,
            onNext = onNext,
            onSkip = onSkip,
        )
    }
    TutorialStep.Done -> DoneContent(onFinish)
}

@Composable
private fun WelcomeContent(lessonCount: Int, canVibrate: Boolean, onStart: () -> Unit, onSkip: () -> Unit) = GlanceScreenTemplate(
    glance = { HeadingText(stringResource(R.string.tutorial_welcome_title)) },
    details = listOfNotNull<@Composable () -> Unit>(
        @Composable { BodyText(pluralStringResource(R.plurals.tutorial_welcome_body, lessonCount, lessonCount)) },
        if (canVibrate) null else ({ NoticeLabel(stringResource(R.string.tutorial_no_vibrator), StatusTone.Error) }),
        @Composable { HaptiButton(text = stringResource(R.string.tutorial_start), onClick = onStart, modifier = Modifier.fillMaxWidth()) },
        // Android no dice si el usuario apagó la vibración: se avisa siempre, después de la acción
        if (canVibrate) ({ BodyText(stringResource(R.string.tutorial_vibration_hint)) }) else null,
    ),
    belowFold = { SkipButton(onSkip) },
)

/**
 * Primera vista: el paso, el dibujo de la navegación para ese aviso y su nombre. Debajo, qué se siente y qué hacer,
 * la tarjeta del patrón (es el botón Repetir y enseña su forma mientras suena) y Siguiente.
 * El dibujo no se lee con TalkBack: el título y el significado ya lo dicen.
 */
@Composable
private fun LessonContent(
    lesson: HapticLesson,
    number: Int,
    total: Int,
    canVibrate: Boolean,
    progress: Float?,
    onPlayAgain: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val pattern = HapticTokens.patternFor(lesson.event)
    GlanceScreenTemplate(
        glance = {
            StepIndicator(current = number, total = total)
            LessonVisualView(lesson.visual, Modifier.clearAndSetSemantics {})
            HeadingText(stringResource(lesson.title))
        },
        details = listOf(
            @Composable { BodyText(stringResource(lesson.meaning)) },
            if (canVibrate) {
                @Composable {
                    HapticPatternCard(
                        title = stringResource(R.string.tutorial_play_again),
                        caption = pluralStringResource(R.plurals.haptic_pattern_summary, pattern.pulseCount, pattern.pulseCount, pattern.totalDurationMs.toInt()),
                        segments = pattern.toSegments(),
                        onPlay = onPlayAgain,
                        scaleMs = hapticScaleMs,
                        progress = progress,
                        tint = lesson.event.tint,
                    )
                }
            } else {
                @Composable { NoticeLabel(stringResource(R.string.tutorial_no_vibrator), StatusTone.Error) }
            },
            @Composable { HaptiButton(text = stringResource(R.string.tutorial_next), onClick = onNext, modifier = Modifier.fillMaxWidth()) },
        ),
        belowFold = { SkipButton(onSkip) },
    )
}

/**
 * Los mismos átomos e iconos que la navegación: DirectionArrow, el icono de fuera de ruta con su tono, DistanceText.
 * Medianos, como el pin de la llegada: con los grandes, el significado no cabía en la primera vista.
 */
@Composable
private fun LessonVisualView(visual: LessonVisual, modifier: Modifier) = when (visual) {
    is LessonVisual.Arrow -> DirectionArrow(direction = visual.direction, modifier = modifier, size = HaptiIconSize.Medium)
    is LessonVisual.Icon -> HaptiIcon(painterResource(visual.icon), contentDescription = null, modifier = modifier, size = HaptiIconSize.Medium, tint = visual.tone.color)
    is LessonVisual.Distance -> DistanceText(meters = visual.meters, modifier = modifier)
}

@Composable
private fun DoneContent(onFinish: () -> Unit) = GlanceScreenTemplate(
    glance = { HeadingText(stringResource(R.string.tutorial_done_title)) },
    details = listOf(
        @Composable { BodyText(stringResource(R.string.tutorial_done_body)) },
        @Composable { HaptiButton(text = stringResource(R.string.tutorial_done_action), onClick = onFinish, modifier = Modifier.fillMaxWidth()) },
    ),
)

@Composable
private fun SkipButton(onSkip: () -> Unit) = HaptiButton(
    text = stringResource(R.string.tutorial_skip),
    onClick = onSkip,
    modifier = Modifier.fillMaxWidth(),
    variant = HaptiButtonVariant.Secondary,
)
