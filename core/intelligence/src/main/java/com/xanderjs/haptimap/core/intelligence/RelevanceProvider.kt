package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.PlaceCategory

/**
 * Cuánto encaja un POI con el paseo, en 0..1 (paso 18). Es la costura para enchufar más adelante un
 * proveedor con IA (p. ej. DeepSeek, con la clave ya reservada en BuildConfig.DEEPSEEK_API_KEY) sin
 * tocar el evaluador: bastaría otra implementación de esta interfaz. Hoy no se llama a ninguna IA.
 */
fun interface RelevanceProvider {
    fun relevance(candidate: PoiCandidate): Double
}

/**
 * Relevancia local por reglas (sin red): el peso base de la categoría ([PoiEvaluationConfig.categoryWeights],
 * o [PoiEvaluationConfig.defaultCategoryWeight] si no hay o la categoría es nula) multiplicado por
 * [PoiEvaluationConfig.favoriteBoost] cuando es una categoría favorita del usuario, recortado a 0..1.
 *
 * [favoriteCategories] llega vacío mientras no exista el ajuste de favoritas (paso 25).
 */
class RuleBasedRelevanceProvider(
    private val config: PoiEvaluationConfig = PoiEvaluationConfig(),
    private val favoriteCategories: Set<PlaceCategory> = emptySet(),
) : RelevanceProvider {
    override fun relevance(candidate: PoiCandidate): Double {
        val base = candidate.category?.let { config.categoryWeights[it] } ?: config.defaultCategoryWeight
        val boosted = if (candidate.category != null && candidate.category in favoriteCategories) base * config.favoriteBoost else base
        return boosted.coerceIn(0.0, 1.0)
    }
}
