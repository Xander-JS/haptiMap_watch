package com.xanderjs.haptimap.feature.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.designsystem.R as DsR
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.StatusTone

/** Lo que enseña una lección: el mismo dibujo que la navegación pone para ese aviso. */
sealed interface LessonVisual {
    data class Arrow(val direction: ArrowDirection) : LessonVisual
    data class Icon(@param:DrawableRes val icon: Int, val tone: StatusTone) : LessonVisual

    /** La distancia corta que se ve al llegar al giro: el aviso inminente no dice dirección, ya la dijo el anterior. */
    data class Distance(val meters: Int) : LessonVisual
}

/** Patrón → significado → acción (sección 19): el evento suena por el Haptic Engine, como al navegar. */
data class HapticLesson(
    val event: HapticEvent,
    @param:StringRes val title: Int,
    @param:StringRes val meaning: Int,
    val visual: LessonVisual,
)

/**
 * Las lecciones de la versión 1, en orden. La sugerencia de POI (versión 2, paso 19) es una entrada más aquí.
 * El orden sigue un recorrido: dirección, seguir, el giro ya, salirse y llegar.
 */
val hapticLessons: List<HapticLesson> = listOf(
    HapticLesson(HapticEvent.TURN_LEFT, DsR.string.direction_left, R.string.lesson_turn_left_meaning, LessonVisual.Arrow(ArrowDirection.LEFT)),
    HapticLesson(HapticEvent.TURN_RIGHT, DsR.string.direction_right, R.string.lesson_turn_right_meaning, LessonVisual.Arrow(ArrowDirection.RIGHT)),
    HapticLesson(HapticEvent.CONTINUE, DsR.string.direction_straight, R.string.lesson_continue_meaning, LessonVisual.Arrow(ArrowDirection.STRAIGHT)),
    HapticLesson(HapticEvent.TURN_IMMINENT, R.string.lesson_imminent, R.string.lesson_imminent_meaning, LessonVisual.Distance(5)),
    HapticLesson(HapticEvent.OFF_ROUTE, R.string.lesson_off_route, R.string.lesson_off_route_meaning, LessonVisual.Icon(DsR.drawable.ic_off_route, StatusTone.Warning)),
    HapticLesson(HapticEvent.ARRIVAL, DsR.string.direction_arrive, R.string.lesson_arrival_meaning, LessonVisual.Arrow(ArrowDirection.ARRIVE)),
)
