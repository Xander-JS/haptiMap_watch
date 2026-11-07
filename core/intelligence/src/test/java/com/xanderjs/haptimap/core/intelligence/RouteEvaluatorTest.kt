package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteEvaluatorTest {

    private val sol = GeoPoint(40.416775, -3.703790)
    private val prado = Place("ChIJ7aLYZp0oQg0RWoitk33wlBA", "Museo del Prado", GeoPoint(40.413782, -3.692127), null, null, 1000)
    private val evaluator = RouteEvaluator()

    private fun step(
        maneuver: Maneuver,
        distanceMeters: Int = 120,
        sourceManeuver: String? = when (maneuver) {
            Maneuver.TURN_LEFT -> "TURN_LEFT"
            Maneuver.TURN_RIGHT -> "TURN_RIGHT"
            Maneuver.CONTINUE -> "STRAIGHT"
            Maneuver.ARRIVAL -> null
        },
    ) = RouteStep(
        maneuver = maneuver,
        distanceMeters = distanceMeters,
        duration = (distanceMeters / 1.3).seconds,
        points = listOf(sol, prado.location),
        start = sol,
        end = prado.location,
        instruction = null,
        sourceManeuver = sourceManeuver,
    )

    private fun route(
        distanceMeters: Int,
        duration: Duration,
        steps: List<RouteStep>,
    ) = Route(
        destination = prado,
        distanceMeters = distanceMeters,
        duration = duration,
        points = listOf(sol, prado.location),
        steps = steps + step(Maneuver.ARRIVAL, distanceMeters = 0),
    )

    @Test
    fun `single candidate returns SINGLE_CANDIDATE without discarding`() {
        val single = route(1089, 814.seconds, listOf(step(Maneuver.CONTINUE, 500), step(Maneuver.TURN_RIGHT, 589)))
        val selection = evaluator.select(listOf(single), NavigationProfile.FOCUS)
        assertEquals(single, selection.chosen.route)
        assertEquals(RouteSelectionReason.SINGLE_CANDIDATE, selection.reason)
        assertNull(selection.chosen.discardReason)
    }

    @Test
    fun `scores distance higher for the shorter route when other criteria match`() {
        val steps = listOf(step(Maneuver.CONTINUE, 400), step(Maneuver.TURN_RIGHT, 400))
        val shorter = route(800, 600.seconds, steps)
        val longer = route(960, 600.seconds, steps)
        val evals = evaluator.evaluate(listOf(shorter, longer), NavigationProfile.FOCUS)
        assertEquals(1.0, evals[0].distanceScore, 0.001)
        assertTrue(evals[0].distanceScore > evals[1].distanceScore)
        assertTrue(evals[0].score > evals[1].score)
    }

    @Test
    fun `scores time higher for the faster route when other criteria match`() {
        val steps = listOf(step(Maneuver.CONTINUE, 400), step(Maneuver.TURN_RIGHT, 400))
        val faster = route(800, 550.seconds, steps)
        val slower = route(800, 650.seconds, steps)
        val evals = evaluator.evaluate(listOf(faster, slower), NavigationProfile.FOCUS)
        assertEquals(1.0, evals[0].timeScore, 0.001)
        assertTrue(evals[0].timeScore > evals[1].timeScore)
        assertTrue(evals[0].score > evals[1].score)
    }

    @Test
    fun `scores maneuvers and turn density higher for the route with fewer turns`() {
        val fewTurns = route(
            distanceMeters = 900,
            duration = 680.seconds,
            steps = listOf(step(Maneuver.CONTINUE, 450), step(Maneuver.TURN_RIGHT, 450)),
        )
        val manyTurns = route(
            distanceMeters = 900,
            duration = 680.seconds,
            steps = listOf(
                step(Maneuver.CONTINUE, 150),
                step(Maneuver.TURN_LEFT, 150),
                step(Maneuver.TURN_RIGHT, 150),
                step(Maneuver.TURN_LEFT, 150),
                step(Maneuver.TURN_RIGHT, 150),
                step(Maneuver.TURN_LEFT, 150),
            ),
        )
        val evals = evaluator.evaluate(listOf(fewTurns, manyTurns), NavigationProfile.TOUR)
        assertEquals(1, evals[0].turnCount)
        assertEquals(5, evals[1].turnCount)
        assertTrue(evals[0].maneuverScore > evals[1].maneuverScore)
        assertTrue(evals[0].turnsPerKm < evals[1].turnsPerKm)
        assertTrue(evals[0].score > evals[1].score)
    }

    @Test
    fun `short segments and sharp turns increase haptic complexity and lower simplicity score`() {
        val calm = route(
            distanceMeters = 800,
            duration = 600.seconds,
            steps = listOf(
                step(Maneuver.CONTINUE, 300),
                step(Maneuver.TURN_RIGHT, 250, "TURN_SLIGHT_RIGHT"),
                step(Maneuver.TURN_LEFT, 250, "TURN_LEFT"),
            ),
        )
        val hapticPileUp = route(
            distanceMeters = 800,
            duration = 600.seconds,
            steps = listOf(
                step(Maneuver.CONTINUE, 300),
                step(Maneuver.TURN_RIGHT, 12, "TURN_SHARP_RIGHT"),
                step(Maneuver.TURN_LEFT, 15, "UTURN_LEFT"),
            ),
        )
        val evals = evaluator.evaluate(listOf(calm, hapticPileUp), NavigationProfile.FOCUS)
        assertEquals(0, evals[0].shortStepCount)
        assertEquals(0, evals[0].sharpTurnCount)
        assertEquals(2, evals[1].shortStepCount)
        assertEquals(2, evals[1].sharpTurnCount)
        assertTrue(evals[1].complexity > evals[0].complexity)
        assertTrue(evals[0].simplicityScore > evals[1].simplicityScore)
        assertTrue(evals[0].score > evals[1].score)
    }

    @Test
    fun `profile weights make FOCUS pick the faster route and TOUR pick the calmer slightly longer route`() {
        // Ruta 0: más rápida y corta (800 m, 600 s), pero con muchos giros, tramos cortos (<25 m) y giros cerrados
        val fastAndComplex = route(
            distanceMeters = 800,
            duration = 600.seconds,
            steps = listOf(
                step(Maneuver.CONTINUE, 200),
                step(Maneuver.TURN_LEFT, 12, "TURN_SHARP_LEFT"),
                step(Maneuver.TURN_RIGHT, 14, "TURN_SHARP_RIGHT"),
                step(Maneuver.TURN_LEFT, 18, "TURN_LEFT"),
                step(Maneuver.TURN_RIGHT, 180, "TURN_SHARP_RIGHT"),
                step(Maneuver.TURN_LEFT, 190, "TURN_LEFT"),
                step(Maneuver.TURN_RIGHT, 186, "TURN_RIGHT"),
            ),
        )
        // Ruta 1: +10% en tiempo y distancia (880 m, 660 s, dentro del +25%), pero con 1 solo giro suave y sin tramos cortos
        val calmForTour = route(
            distanceMeters = 880,
            duration = 660.seconds,
            steps = listOf(
                step(Maneuver.CONTINUE, 440),
                step(Maneuver.TURN_RIGHT, 440, "TURN_SLIGHT_RIGHT"),
            ),
        )

        val focusSelection = evaluator.select(listOf(fastAndComplex, calmForTour), NavigationProfile.FOCUS)
        assertEquals(0, focusSelection.chosen.index)
        assertEquals(RouteSelectionReason.FASTER_FOR_FOCUS, focusSelection.reason)

        val tourSelection = evaluator.select(listOf(fastAndComplex, calmForTour), NavigationProfile.TOUR)
        assertEquals(1, tourSelection.chosen.index)
        assertEquals(RouteSelectionReason.SIMPLER_FOR_TOUR, tourSelection.reason)
    }

    @Test
    fun `max-longer duration rule discards a route slower than maxDurationRatioToFastest even in TOUR`() {
        val fastest = route(
            distanceMeters = 1086,
            duration = 882.seconds,
            steps = listOf(
                step(Maneuver.CONTINUE, 310),
                step(Maneuver.TURN_LEFT, 10, "TURN_LEFT"),
                step(Maneuver.TURN_RIGHT, 23, "TURN_SHARP_RIGHT"),
                step(Maneuver.TURN_LEFT, 13, "TURN_LEFT"),
                step(Maneuver.TURN_RIGHT, 730, "TURN_RIGHT"),
            ),
        )
        // Como la 2ª alternativa real de two_routes.json (1378 m, 1125 s = +27.5% de tiempo > +25% permitido)
        val tooSlow = route(
            distanceMeters = 1378,
            duration = 1125.seconds,
            steps = listOf(step(Maneuver.CONTINUE, 700), step(Maneuver.TURN_RIGHT, 678)),
        )
        // En el límite exacto (882 * 1.25 = 1102.5 s -> 1102 s <= 1.25x): no se descarta
        val atEdge = route(
            distanceMeters = 1200,
            duration = 1102.seconds,
            steps = listOf(step(Maneuver.CONTINUE, 600), step(Maneuver.TURN_RIGHT, 600)),
        )

        val slowSelection = evaluator.select(listOf(fastest, tooSlow), NavigationProfile.TOUR)
        assertEquals(RouteDiscardReason.TOO_SLOW, slowSelection.evaluations[1].discardReason)
        assertEquals(0.0, slowSelection.evaluations[1].score, 0.0)
        assertEquals(0, slowSelection.chosen.index)

        val edgeSelection = evaluator.select(listOf(fastest, atEdge), NavigationProfile.TOUR)
        assertNull(edgeSelection.evaluations[1].discardReason)
        assertEquals(1, edgeSelection.chosen.index)
    }

    @Test
    fun `max-longer distance rule discards a route longer than maxDistanceRatioToShortest even in TOUR`() {
        val shortest = route(
            distanceMeters = 1000,
            duration = 800.seconds,
            steps = listOf(
                step(Maneuver.TURN_LEFT, 12, "TURN_SHARP_LEFT"),
                step(Maneuver.TURN_RIGHT, 14, "TURN_SHARP_RIGHT"),
                step(Maneuver.TURN_LEFT, 974, "TURN_LEFT"),
            ),
        )
        // +36% de distancia (> 1.35), aunque el tiempo (+15%) esté dentro del límite
        val tooLong = route(
            distanceMeters = 1360,
            duration = 920.seconds,
            steps = listOf(step(Maneuver.CONTINUE, 1360)),
        )
        val atEdge = route(
            distanceMeters = 1350,
            duration = 920.seconds,
            steps = listOf(step(Maneuver.CONTINUE, 1350)),
        )

        val evals = evaluator.evaluate(listOf(shortest, tooLong, atEdge), NavigationProfile.TOUR)
        assertEquals(RouteDiscardReason.TOO_LONG, evals[1].discardReason)
        assertNull(evals[2].discardReason)
    }
}
