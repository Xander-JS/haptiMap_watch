package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.RouteStep
import kotlin.time.Duration

/**
 * Dónde va el usuario en la ruta activa (paso 07). Distancias a lo largo de la ruta, no en línea recta.
 * [crossTrackMeters] es la distancia a la ruta del último fix bueno (null hasta el primero): el paso 08
 * decide con ella si se ha salido. [weakSignal]: ningún fix bueno en NavigationThresholds.weakSignalAfter.
 */
data class NavigationProgress(
    val stepIndex: Int,
    val nextManeuver: RouteStep,
    val distanceToManeuverMeters: Double,
    val remainingMeters: Double,
    val remainingTime: Duration,
    val crossTrackMeters: Double?,
    val fixAccuracyMeters: Float?,
    val weakSignal: Boolean,
)

/**
 * Avisos semánticos para el Haptic Engine (paso 09): cada uno una vez por maniobra y umbral, en orden.
 * No es NavigationEvent: esos mueven la sesión; estos solo dicen qué ha pasado en la ruta.
 * [stepIndex] es el paso cuya maniobra se avisa.
 */
sealed interface NavigationCue {
    data class ManeuverApproaching(val stepIndex: Int, val maneuver: Maneuver) : NavigationCue
    data class ManeuverImminent(val stepIndex: Int, val maneuver: Maneuver) : NavigationCue
    data class ManeuverPassed(val stepIndex: Int, val maneuver: Maneuver) : NavigationCue
    data object Arrived : NavigationCue
}
