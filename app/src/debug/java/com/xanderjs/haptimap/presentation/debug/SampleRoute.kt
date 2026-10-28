package com.xanderjs.haptimap.presentation.debug

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.offset
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Ritmo de las duraciones de ejemplo, como el de Google a pie. */
private const val SAMPLE_WALK_SPEED = 1.4

/** Un tramo: la maniobra al empezarlo, rumbo, metros y la instrucción en inglés y en español. */
private class SampleLeg(val maneuver: Maneuver, val bearing: Double, val meters: Double, val en: String, val es: String)

private val SOL = GeoPoint(40.41693, -3.70354)

// Sol → Museo del Prado por el Barrio de las Letras: tres giros a la derecha, dos a la izquierda y un
// cruce. Calles reales; la geometría son tramos rectos con rumbos aproximados, no una polilínea de Google.
private val LEGS = listOf(
    SampleLeg(Maneuver.CONTINUE, 100.0, 230.0, "Head east on Carrera de San Jerónimo", "Dirígete al este por la Carrera de San Jerónimo"),
    SampleLeg(Maneuver.TURN_RIGHT, 190.0, 120.0, "Turn right onto Calle del Príncipe", "Gira a la derecha por la Calle del Príncipe"),
    SampleLeg(Maneuver.TURN_LEFT, 105.0, 260.0, "Turn left onto Calle del Prado", "Gira a la izquierda por la Calle del Prado"),
    SampleLeg(Maneuver.TURN_RIGHT, 195.0, 90.0, "Turn right onto Calle de Jesús", "Gira a la derecha por la Calle de Jesús"),
    SampleLeg(Maneuver.TURN_LEFT, 100.0, 280.0, "Turn left onto Calle de las Huertas", "Gira a la izquierda por la Calle de las Huertas"),
    SampleLeg(
        Maneuver.CONTINUE,
        100.0,
        60.0,
        "Cross Paseo del Prado at the crosswalk and continue toward the Murillo entrance of the museum",
        "Cruza el Paseo del Prado por el paso de peatones y sigue hacia la puerta de Murillo del museo",
    ),
)

/**
 * Ruta de ejemplo para enseñar el flujo completo sin clave de API (solo debug: en release esta función
 * devuelve null y el catálogo no muestra la entrada). Un vértice cada 10 m, como una polilínea real.
 */
fun sampleRoute(language: String): Route? {
    val points = mutableListOf(SOL)
    val steps = mutableListOf<RouteStep>()
    var start = SOL
    for (leg in LEGS) {
        val pieces = ceil(leg.meters / 10).toInt()
        val stepPoints = (0..pieces).map { start.offset(leg.bearing, leg.meters * it / pieces) }
        points += stepPoints.drop(1)
        val instruction = if (language == "es") leg.es else leg.en
        steps += RouteStep(leg.maneuver, leg.meters.toInt(), (leg.meters / SAMPLE_WALK_SPEED).seconds, stepPoints, start, stepPoints.last(), instruction, null)
        start = stepPoints.last()
    }
    steps += RouteStep(Maneuver.ARRIVAL, 0, Duration.ZERO, listOf(start), start, start, null, null)
    val meters = LEGS.sumOf { it.meters }
    val prado = Place("sample-prado", "Museo del Prado", start, null, "Calle de Ruiz de Alarcón 23", meters.toInt())
    return Route(prado, meters.toInt(), (meters / SAMPLE_WALK_SPEED).seconds, points, steps)
}
