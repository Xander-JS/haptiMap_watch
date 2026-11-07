package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep

/** Motivo por el que una ruta alternativa queda descartada frente a la más rápida/corta (paso 21). */
enum class RouteDiscardReason {
    /** Supera [RouteEvaluationConfig.maxDurationRatioToFastest] respecto a la alternativa más rápida. */
    TOO_SLOW,

    /** Supera [RouteEvaluationConfig.maxDistanceRatioToShortest] respecto a la alternativa más corta. */
    TOO_LONG,
}

/** Por qué [RouteEvaluator] eligió la ruta ganadora entre las candidatas (paso 21). */
enum class RouteSelectionReason {
    /** Solo llegó una ruta candidata: no había alternativa que comparar. */
    SINGLE_CANDIDATE,

    /** La elegida es a la vez la más rápida y la de menor complejidad/maniobras. */
    FASTEST_AND_SIMPLEST,

    /** En modo Focus se eligió por ser más rápida/directa. */
    FASTER_FOR_FOCUS,

    /** En modo Tour se eligió una alternativa más tranquila y fácil de seguir por vibración. */
    SIMPLER_FOR_TOUR,

    /** Mejor equilibrio global de tiempo, distancia, maniobras y complejidad para el perfil activo. */
    BEST_BALANCE,
}

/**
 * Resultado de evaluar una alternativa concreta (paso 21). Todos los sub-scores están en `0.0..1.0`
 * (mayor = mejor), mientras que [complexity] está en `0.0..1.0` (menor = más simple para guiar por vibración).
 */
data class RouteEvaluation(
    val index: Int,
    val route: Route,
    val score: Double,
    val timeScore: Double,
    val distanceScore: Double,
    val maneuverScore: Double,
    val simplicityScore: Double,
    val turnCount: Int,
    val sharpTurnCount: Int,
    val shortStepCount: Int,
    val turnsPerKm: Double,
    val complexity: Double,
    val discardReason: RouteDiscardReason? = null,
)

/** Decisión final de [RouteEvaluator.select]: la alternativa [chosen], todas las [evaluations] y el [reason]. */
data class RouteSelection(
    val chosen: RouteEvaluation,
    val evaluations: List<RouteEvaluation>,
    val reason: RouteSelectionReason,
)

/**
 * Capa de inteligencia (paso 21): puntúa las rutas alternativas devueltas en la llamada inicial de Routes y elige
 * la mejor para el [NavigationProfile] activo, sin clases de Google ni llamadas extra a Places/Routes.
 *
 * Criterios (cada uno normalizado en `0..1` dentro del margen permitido, mayor = mejor, igual que [PoiEvaluator]):
 * - Tiempo estimado (`1 - exceso / (maxDurationRatioToFastest - 1)`)
 * - Distancia (`1 - exceso / (maxDistanceRatioToShortest - 1)`)
 * - Número de maniobras de giro (`TURN_LEFT` / `TURN_RIGHT` respecto al mínimo del conjunto)
 * - Simplicidad háptica (`1 - complexity`), donde `complexity` combina tramos cortos (`< shortStepThresholdMeters`,
 *   que apilan los avisos hápticos de 18 m y 6 m), giros cerrados (`SHARP` / `UTURN`) y densidad de giros por km.
 */
class RouteEvaluator(
    private val config: RouteEvaluationConfig = RouteEvaluationConfig(),
) {
    fun select(candidates: List<Route>, profile: NavigationProfile): RouteSelection {
        require(candidates.isNotEmpty()) { "candidates no puede estar vacío" }
        val evaluations = evaluate(candidates, profile)
        val chosen = evaluations
            .filter { it.discardReason == null }
            .maxByOrNull { it.score }
            ?: evaluations.minBy { it.route.duration }
        return RouteSelection(
            chosen = chosen,
            evaluations = evaluations,
            reason = reasonFor(chosen, evaluations, profile),
        )
    }

    fun evaluate(candidates: List<Route>, profile: NavigationProfile): List<RouteEvaluation> {
        require(candidates.isNotEmpty()) { "candidates no puede estar vacío" }
        val fastestSeconds = candidates.minOf { it.durationSeconds() }
        val shortestMeters = candidates.minOf { it.distanceMeters.coerceAtLeast(1) }.toDouble()
        val turnCounts = candidates.map { it.countTurns() }
        val minTurns = turnCounts.min()
        val weights = config.weightsFor(profile)

        return candidates.mapIndexed { index, route ->
            val durationSeconds = route.durationSeconds()
            val distanceMeters = route.distanceMeters.coerceAtLeast(1).toDouble()
            val durationRatio = durationSeconds / fastestSeconds
            val distanceRatio = distanceMeters / shortestMeters
            val turnSteps = route.steps.filter { it.maneuver == Maneuver.TURN_LEFT || it.maneuver == Maneuver.TURN_RIGHT }
            val turnCount = turnSteps.size
            val sharpTurnCount = turnSteps.count { it.isSharpTurn() }
            val nonArrivalSteps = route.steps.filter { it.maneuver != Maneuver.ARRIVAL }
            val shortStepCount = nonArrivalSteps.count { it.distanceMeters in 1 until config.shortStepThresholdMeters }
            val km = (route.distanceMeters / 1000.0).coerceAtLeast(0.1)
            val turnsPerKm = turnCount / km

            val shortStepRatio = if (nonArrivalSteps.isEmpty()) 0.0 else (shortStepCount.toDouble() / nonArrivalSteps.size).coerceIn(0.0, 1.0)
            val sharpTurnRatio = if (turnCount == 0) 0.0 else (sharpTurnCount.toDouble() / turnCount).coerceIn(0.0, 1.0)
            val densityRatio = (turnsPerKm / config.referenceTurnsPerKm).coerceIn(0.0, 1.0)
            val complexity = (
                (
                    config.shortStepPenaltyWeight * shortStepRatio +
                        config.sharpTurnPenaltyWeight * sharpTurnRatio +
                        config.turnDensityPenaltyWeight * densityRatio
                    ) / config.totalComplexityWeight
                ).coerceIn(0.0, 1.0)

            val timeScore = (1.0 - (durationRatio - 1.0) / (config.maxDurationRatioToFastest - 1.0)).coerceIn(0.0, 1.0)
            val distanceScore = (1.0 - (distanceRatio - 1.0) / (config.maxDistanceRatioToShortest - 1.0)).coerceIn(0.0, 1.0)
            val maneuverScore = (1.0 - (turnCount - minTurns).toDouble() / config.referenceManeuverCount).coerceIn(0.0, 1.0)
            val simplicityScore = 1.0 - complexity

            val discardReason = when {
                durationRatio > config.maxDurationRatioToFastest -> RouteDiscardReason.TOO_SLOW
                distanceRatio > config.maxDistanceRatioToShortest -> RouteDiscardReason.TOO_LONG
                else -> null
            }
            val rawScore = (
                weights.weightTime * timeScore +
                    weights.weightDistance * distanceScore +
                    weights.weightManeuvers * maneuverScore +
                    weights.weightComplexity * simplicityScore
                ) / weights.totalWeight

            RouteEvaluation(
                index = index,
                route = route,
                score = if (discardReason == null) rawScore else 0.0,
                timeScore = timeScore,
                distanceScore = distanceScore,
                maneuverScore = maneuverScore,
                simplicityScore = simplicityScore,
                turnCount = turnCount,
                sharpTurnCount = sharpTurnCount,
                shortStepCount = shortStepCount,
                turnsPerKm = turnsPerKm,
                complexity = complexity,
                discardReason = discardReason,
            )
        }
    }

    private fun reasonFor(
        chosen: RouteEvaluation,
        evaluations: List<RouteEvaluation>,
        profile: NavigationProfile,
    ): RouteSelectionReason {
        if (evaluations.size == 1) return RouteSelectionReason.SINGLE_CANDIDATE
        val fastestDuration = evaluations.minOf { it.route.duration }
        val minComplexity = evaluations.minOf { it.complexity }
        val minTurns = evaluations.minOf { it.turnCount }
        val isFastest = chosen.route.duration <= fastestDuration
        val isSimplest = chosen.complexity <= minComplexity && chosen.turnCount <= minTurns
        return when {
            isFastest && isSimplest -> RouteSelectionReason.FASTEST_AND_SIMPLEST
            profile == NavigationProfile.FOCUS && isFastest -> RouteSelectionReason.FASTER_FOR_FOCUS
            profile == NavigationProfile.TOUR && (chosen.complexity < evaluations.first().complexity || chosen.turnCount < evaluations.first().turnCount) ->
                RouteSelectionReason.SIMPLER_FOR_TOUR
            else -> RouteSelectionReason.BEST_BALANCE
        }
    }
}

private fun Route.durationSeconds(): Double = duration.inWholeSeconds.coerceAtLeast(1L).toDouble()

private fun Route.countTurns(): Int =
    steps.count { it.maneuver == Maneuver.TURN_LEFT || it.maneuver == Maneuver.TURN_RIGHT }

private fun RouteStep.isSharpTurn(): Boolean {
    val raw = sourceManeuver ?: return false
    return raw.contains("SHARP") || raw.contains("UTURN")
}
