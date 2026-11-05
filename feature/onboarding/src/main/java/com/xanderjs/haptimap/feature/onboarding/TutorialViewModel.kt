package com.xanderjs.haptimap.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.haptics.PlayedHaptic
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Bienvenida, una lección por patrón y fin. */
sealed interface TutorialStep {
    data object Welcome : TutorialStep
    data class Lesson(val index: Int) : TutorialStep
    data object Done : TutorialStep
}

fun TutorialStep.next(lessonCount: Int): TutorialStep = when (this) {
    TutorialStep.Welcome -> if (lessonCount > 0) TutorialStep.Lesson(0) else TutorialStep.Done
    is TutorialStep.Lesson -> if (index + 1 < lessonCount) TutorialStep.Lesson(index + 1) else TutorialStep.Done
    TutorialStep.Done -> TutorialStep.Done
}

/** [finished]: terminado o saltado; la pantalla se va. */
data class TutorialUiState(val step: TutorialStep, val canVibrate: Boolean, val finished: Boolean = false)

/**
 * El tutorial háptico (paso 13). Cada lección suena una vez al entrar, desde la acción que la abre y no al
 * componerse: volver del ambiente, de la pantalla apagada o de la muerte del proceso no la repite.
 * Suena por [HapticEngine.play], con las mismas reglas y patrones que la navegación.
 */
@HiltViewModel
class TutorialViewModel @Inject constructor(
    private val engine: HapticEngine,
    private val store: TutorialStore,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    val lessons: List<HapticLesson> = hapticLessons

    private val _uiState = MutableStateFlow(TutorialUiState(restoredStep(), engine.canVibrate))
    val uiState: StateFlow<TutorialUiState> = _uiState.asStateFlow()

    /** Lo último que ha sonado: la tarjeta lo pinta mientras suena si es su patrón. */
    val played: StateFlow<PlayedHaptic?> = engine.played

    fun next() {
        val step = _uiState.value.step.next(lessons.size)
        moveTo(step)
        if (step is TutorialStep.Lesson) play(step)
        // llegar al final cuenta como visto aunque luego se deslice fuera
        if (step == TutorialStep.Done) store.markSeen()
    }

    fun playAgain() {
        (_uiState.value.step as? TutorialStep.Lesson)?.let(::play)
    }

    fun skip() {
        store.markSeen()
        _uiState.update { it.copy(finished = true) }
    }

    fun finish() = skip()

    private fun play(step: TutorialStep.Lesson) {
        if (_uiState.value.canVibrate) engine.play(lessons[step.index].event)
    }

    private fun moveTo(step: TutorialStep) {
        savedState[KEY_STEP] = when (step) {
            TutorialStep.Welcome -> WELCOME
            is TutorialStep.Lesson -> step.index
            TutorialStep.Done -> DONE
        }
        _uiState.update { it.copy(step = step) }
    }

    private fun restoredStep(): TutorialStep = when (val saved = savedState.get<Int>(KEY_STEP) ?: WELCOME) {
        WELCOME -> TutorialStep.Welcome
        DONE -> TutorialStep.Done
        else -> TutorialStep.Lesson(saved.coerceIn(0, lessons.lastIndex))
    }

    private companion object {
        const val KEY_STEP = "tutorial_step"
        const val WELCOME = -1
        const val DONE = -2
    }
}
