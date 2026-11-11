package com.xanderjs.haptimap.feature.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.designsystem.R as DsR
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection

/** Lo que enseña una lección: el mismo dibujo que la navegación pone para ese aviso. */
sealed interface LessonVisual {
    data class Arrow(val direction: ArrowDirection) : LessonVisual
    /** En el color del aviso (HapticEvent.tint), como en la navegación. */
    data class Icon(@param:DrawableRes val icon: Int) : LessonVisual

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
    HapticLesson(HapticEvent.TURN_LEFT, R.string.haptic_turn_left, R.string.lesson_turn_left_meaning, LessonVisual.Arrow(ArrowDirection.LEFT)),
    HapticLesson(HapticEvent.TURN_RIGHT, R.string.haptic_turn_right, R.string.lesson_turn_right_meaning, LessonVisual.Arrow(ArrowDirection.RIGHT)),
    HapticLesson(HapticEvent.CONTINUE, R.string.haptic_continue, R.string.lesson_continue_meaning, LessonVisual.Arrow(ArrowDirection.STRAIGHT)),
    HapticLesson(HapticEvent.TURN_IMMINENT, R.string.haptic_turn_imminent, R.string.lesson_imminent_meaning, LessonVisual.Distance(5)),
    HapticLesson(HapticEvent.OFF_ROUTE, R.string.haptic_off_route, R.string.lesson_off_route_meaning, LessonVisual.Icon(DsR.drawable.ic_off_route)),
    // llegada a un POI del desvío del Tour (paso 20): a mitad del trayecto, antes de la llegada al destino
    HapticLesson(HapticEvent.POI_ARRIVAL, R.string.haptic_poi_arrival, R.string.lesson_poi_arrival_meaning, LessonVisual.Icon(DsR.drawable.ic_exploration_landmark)),
    HapticLesson(HapticEvent.ARRIVAL, R.string.haptic_arrival, R.string.lesson_arrival_meaning, LessonVisual.Arrow(ArrowDirection.ARRIVE)),
)
