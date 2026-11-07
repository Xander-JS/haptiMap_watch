package com.xanderjs.haptimap.presentation.debug

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.bearingTo
import com.xanderjs.haptimap.core.model.distanceTo

// tramos de menos de esto no merecen paso propio
private const val MIN_LEG_METERS = 5.0

/**
 * Sintetiza una ruta plausible de [origin] a [destination] para el **simulador de ubicación** y sus tests (no es
 * un dato de Google ni un repositorio): en L, primero este-oeste y después norte-sur, con un giro (o recta si un
 * tramo es muy corto). No sigue calles. La app nunca la enseña: sin clave, Places y Routes dan su error de clave.
 */
fun debugReroute(origin: GeoPoint, destination: Place, language: String): Route {
    val corner = GeoPoint(origin.latitude, destination.location.longitude)
    val targets = listOf(corner, destination.location).filterIndexed { i, target ->
        val from = if (i == 0) origin else corner
        from.distanceTo(target) >= MIN_LEG_METERS
    }.ifEmpty { listOf(destination.location) }
    var from = origin
    var previousBearing: Double? = null
    val legs = targets.map { target ->
        val bearing = from.bearingTo(target)
        val maneuver = when (val turn = previousBearing?.let { (bearing - it + 360) % 360 }) {
            null -> Maneuver.CONTINUE
            else -> if (turn < 180) Maneuver.TURN_RIGHT else Maneuver.TURN_LEFT
        }
        val leg = SampleLeg(maneuver, bearing, from.distanceTo(target), english(maneuver, destination.name), spanish(maneuver, destination.name))
        from = target
        previousBearing = bearing
        leg
    }
    return routeAlong(origin, legs, language) { _, _ -> destination }
}

private fun english(maneuver: Maneuver, name: String) = when (maneuver) {
    Maneuver.TURN_LEFT -> "Turn left toward $name"
    Maneuver.TURN_RIGHT -> "Turn right toward $name"
    else -> "Head toward $name"
}

private fun spanish(maneuver: Maneuver, name: String) = when (maneuver) {
    Maneuver.TURN_LEFT -> "Gira a la izquierda hacia $name"
    Maneuver.TURN_RIGHT -> "Gira a la derecha hacia $name"
    else -> "Ve hacia $name"
}
