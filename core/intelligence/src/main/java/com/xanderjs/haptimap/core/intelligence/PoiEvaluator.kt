package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Route

/**
 * Capa de inteligencia (sección 6, paso 18): puntúa y filtra los POIs candidatos del Modo Tour. Decide
 * cuáles merece la pena sugerir; no los obtiene (paso 17), no vibra ni toca la UI. Función pura: mismas
 * entradas, mismo resultado.
 *
 * Solo evalúa con el perfil TOUR; en FOCUS devuelve lista vacía (sin sugerencias). Un POI se descarta
 * si (en este orden) ya fue rechazado en el trayecto, está demasiado lejos de la ruta, el desvío es
 * demasiado grande, añade demasiado tiempo o tiene poca relevancia. Los que pasan se ordenan por score
 * (relevancia + cercanía + valoración), de mayor a menor; los descartados van al final con su motivo.
 */
class PoiEvaluator(
    private val config: PoiEvaluationConfig = PoiEvaluationConfig(),
    private val detourEstimator: DetourEstimator = GeometricDetourEstimator(config),
    private val relevanceProvider: RelevanceProvider = RuleBasedRelevanceProvider(config),
) {
    fun evaluate(
        candidates: List<PoiCandidate>,
        route: Route,
        fromAlongMeters: Double,
        profile: NavigationProfile,
        rejected: RejectedPoiMemory,
    ): List<PoiEvaluation> {
        if (profile != NavigationProfile.TOUR) return emptyList()
        return candidates
            .map { evaluateOne(it, route, fromAlongMeters, rejected) }
            .sortedWith(compareBy({ it.decision != PoiDecision.SUGGEST }, { -it.score }))
    }

    private fun evaluateOne(candidate: PoiCandidate, route: Route, fromAlongMeters: Double, rejected: RejectedPoiMemory): PoiEvaluation {
        val detour = detourEstimator.estimate(candidate, route, fromAlongMeters)
        val relevance = relevanceProvider.relevance(candidate)
        val reason = discardReason(candidate, detour, relevance, rejected)
        val score = if (reason == null) score(candidate, detour, relevance) else 0.0
        return PoiEvaluation(
            candidate = candidate,
            score = score,
            detourMeters = detour.detourMeters,
            addedTime = detour.addedTime,
            decision = if (reason == null) PoiDecision.SUGGEST else PoiDecision.DISCARD,
            discardReason = reason,
        )
    }

    // el primer motivo que aplica, en el orden de DiscardReason; null = se sugiere
    private fun discardReason(candidate: PoiCandidate, detour: DetourEstimate, relevance: Double, rejected: RejectedPoiMemory): DiscardReason? = when {
        candidate.id in rejected -> DiscardReason.ALREADY_REJECTED
        detour.distanceFromRouteMeters > config.maxDistanceFromRouteMeters -> DiscardReason.TOO_FAR
        detour.detourMeters > config.maxDetourMeters -> DiscardReason.DETOUR_TOO_LARGE
        detour.addedTime > config.maxAddedTime -> DiscardReason.TOO_MUCH_TIME
        relevance < config.minRelevance -> DiscardReason.LOW_RELEVANCE
        else -> null
    }

    // mayor = se sugiere antes: más relevante, menos desvío y mejor valorado
    private fun score(candidate: PoiCandidate, detour: DetourEstimate, relevance: Double): Double {
        val proximity = (1 - detour.detourMeters / config.maxDetourMeters).coerceIn(0.0, 1.0)
        val rating = ((candidate.rating ?: 0.0) / config.ratingScaleMax).coerceIn(0.0, 1.0)
        return config.weightRelevance * relevance + config.weightProximity * proximity + config.weightRating * rating
    }
}

/** Solo los POIs a sugerir, ya ordenados. */
fun List<PoiEvaluation>.suggestions(): List<PoiEvaluation> = filter { it.decision == PoiDecision.SUGGEST }
