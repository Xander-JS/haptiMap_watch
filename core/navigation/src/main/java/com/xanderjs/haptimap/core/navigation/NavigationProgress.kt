package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.RouteStep
import kotlin.time.Duration

/**
 * Dónde va el usuario en la ruta activa (paso 07). Distancias a lo largo de la ruta, no en línea recta.
 * [crossTrackMeters] es la distancia a la ruta del último fix bueno (null hasta el primero): el paso 08
 * decide con ella si se ha salido. [weakSignal]: ningún fix bueno en NavigationThresholds.weakSignalAfter.
 * [offRoute]: desvío declarado (paso 08), hasta volver a la ruta. [position]: el último fix bueno, origen del recálculo.
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
    val offRoute: Boolean = false,
    val position: GeoPoint? = null,
    /** Velocidad filtrada a lo largo de la ruta (m/s), del filtro alfa-beta del tracker; 0 hasta el primer match. La usa el muestreo adaptativo (paso 23). */
    val speedMetersPerSecond: Double = 0.0,
)

/**
 * Avisos semánticos para el Haptic Engine (paso 09): cada uno una vez por maniobra y umbral, en orden.
 * No es NavigationEvent: esos mueven la sesión; estos solo dicen qué ha pasado en la ruta.
 * [stepIndex] es el paso cuya maniobra se avisa. [OffRoute], [BackOnRoute] y [Rerouted], una vez por desvío (paso 08).
 */
sealed interface NavigationCue {
    data class ManeuverApproaching(val stepIndex: Int, val maneuver: Maneuver) : NavigationCue
    data class ManeuverImminent(val stepIndex: Int, val maneuver: Maneuver) : NavigationCue
    data class ManeuverPassed(val stepIndex: Int, val maneuver: Maneuver) : NavigationCue
    data object Arrived : NavigationCue

    /** Desvío declarado. Documento de producto §12: pulso largo intenso. */
    data object OffRoute : NavigationCue

    /** Ha vuelto a la ruta por su cuenta, sin ruta nueva. */
    data object BackOnRoute : NavigationCue

    /** Se sigue una ruta recalculada desde donde estaba. */
    data object Rerouted : NavigationCue
}
