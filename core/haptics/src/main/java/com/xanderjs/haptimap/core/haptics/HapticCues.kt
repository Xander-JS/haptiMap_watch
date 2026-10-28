package com.xanderjs.haptimap.core.haptics

import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.navigation.NavigationCue
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.navigatingRoute

/**
 * Adaptador de los avisos de navegación a eventos hápticos (paso 09). Es lo único de :core:haptics que conoce
 * la navegación. [playCount] es HapticEngine.playCount: con él sabe si la dirección de una maniobra llegó a sonar.
 *
 * La dirección nunca se pierde: si llega ManeuverImminent de un giro cuya dirección no ha sonado (los dos umbrales
 * en un fix, o el Approaching se fundió con algo más prioritario), suena la dirección en lugar de TURN_IMMINENT.
 * No es seguro entre hilos: un solo recolector.
 */
class HapticCues(private val playCount: (HapticEvent) -> Int) {
    // por maniobra (stepIndex): la dirección pedida y cuántas veces había sonado ese patrón al pedirla
    private val directions = mutableMapOf<Int, Pair<HapticEvent, Int>>()

    fun eventFor(cue: NavigationCue): HapticEvent? = when (cue) {
        is NavigationCue.ManeuverApproaching -> when (cue.maneuver) {
            Maneuver.TURN_LEFT, Maneuver.TURN_RIGHT -> direction(cue.stepIndex, cue.maneuver)
            Maneuver.CONTINUE -> HapticEvent.CONTINUE
            // la llegada la avisa Arrived; dos patrones seguidos al final serían ruido
            Maneuver.ARRIVAL -> null
        }
        is NavigationCue.ManeuverImminent -> when (cue.maneuver) {
            Maneuver.TURN_LEFT, Maneuver.TURN_RIGHT ->
                if (directionPlayed(cue.stepIndex)) HapticEvent.TURN_IMMINENT else direction(cue.stepIndex, cue.maneuver)
            // seguir recto no necesita urgencia: basta el pulso suave
            Maneuver.CONTINUE, Maneuver.ARRIVAL -> null
        }
        // el silencio confirma el giro
        is NavigationCue.ManeuverPassed -> null
        NavigationCue.Arrived -> HapticEvent.ARRIVAL
        NavigationCue.OffRoute -> HapticEvent.OFF_ROUTE
        NavigationCue.BackOnRoute -> HapticEvent.BACK_ON_ROUTE
        NavigationCue.Rerouted -> {
            // la ruta nueva numera sus pasos desde cero
            directions.clear()
            HapticEvent.BACK_ON_ROUTE
        }
    }

    private fun direction(stepIndex: Int, maneuver: Maneuver): HapticEvent {
        val event = if (maneuver == Maneuver.TURN_LEFT) HapticEvent.TURN_LEFT else HapticEvent.TURN_RIGHT
        directions[stepIndex] = event to playCount(event)
        return event
    }

    private fun directionPlayed(stepIndex: Int): Boolean =
        directions[stepIndex]?.let { (event, before) -> playCount(event) > before } ?: false
}

/** Lo háptico sigue mientras se navega y en la llegada (que su patrón no se corte); se corta al detener o cerrar. */
val NavigationSession.hapticsActive: Boolean
    get() = navigatingRoute != null || this is NavigationSession.Arrived
