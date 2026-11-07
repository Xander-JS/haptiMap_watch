package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.NavigationProfile

/**
 * Pesos de los cuatro criterios de [RouteEvaluator] para un perfil de navegación (paso 21).
 * Todos son `>= 0` y su suma es `> 0`; el evaluador normaliza por [totalWeight].
 */
data class ProfileRouteWeights(
    val weightTime: Double,
    val weightDistance: Double,
    val weightManeuvers: Double,
    val weightComplexity: Double,
) {
    val totalWeight: Double get() = weightTime + weightDistance + weightManeuvers + weightComplexity
    val efficiencyWeight: Double get() = weightTime + weightDistance
    val simplicityWeight: Double get() = weightManeuvers + weightComplexity

    init {
        require(weightTime >= 0 && weightDistance >= 0 && weightManeuvers >= 0 && weightComplexity >= 0) {
            "los pesos del perfil no pueden ser negativos: $this"
        }
        require(totalWeight > 0) { "la suma de pesos del perfil debe ser > 0: $this" }
    }
}

/**
 * Todos los umbrales y pesos de la selección de ruta alternativa en un solo sitio (paso 21), con la misma
 * filosofía que [PoiEvaluationConfig]: valores por defecto razonables y validación al construir.
 *
 * Como HaptiMap se sigue por vibración, una ruta con tramos muy cortos (< [shortStepThresholdMeters], donde el
 * preaviso a 18 m y el giro inminente a 6 m se solapan), giros cerrados o alta densidad de giros por kilómetro
 * penaliza la complejidad háptica. [focusWeights] prioriza llegar antes y directo; [tourWeights] acepta un pequeño
 * rodeo si la ruta es más simple y tranquila de seguir por tacto, siempre dentro de [maxDurationRatioToFastest]
 * y [maxDistanceRatioToShortest].
 */
data class RouteEvaluationConfig(
    /** Regla "nunca elegir una ruta más de un X% más lenta que la más rápida" (1.25 = máx. +25% de tiempo). */
    val maxDurationRatioToFastest: Double = 1.25,
    /** Regla "nunca elegir una ruta más de un X% más larga que la más corta" (1.35 = máx. +35% de distancia). */
    val maxDistanceRatioToShortest: Double = 1.35,
    /** Tramos entre maniobras por debajo de estos metros apilan los avisos hápticos (18 m y 6 m). */
    val shortStepThresholdMeters: Int = 25,
    /** Densidad de referencia (giros/km) a partir de la cual la penalización por densidad llega al máximo (1.0). */
    val referenceTurnsPerKm: Double = 8.0,
    /** Escala de maniobras extra sobre la ruta con menos giros para normalizar el criterio de maniobras (0..1). */
    val referenceManeuverCount: Int = 12,
    /** Peso de la proporción de tramos cortos dentro de la complejidad háptica. */
    val shortStepPenaltyWeight: Double = 0.45,
    /** Peso de la proporción de giros cerrados (`SHARP` / `UTURN`) dentro de la complejidad háptica. */
    val sharpTurnPenaltyWeight: Double = 0.35,
    /** Peso de la densidad de giros por km dentro de la complejidad háptica. */
    val turnDensityPenaltyWeight: Double = 0.20,
    /** Focus prioriza tiempo y distancia, manteniendo preferencia por rutas hápticamente limpias a igualdad de tiempo. */
    val focusWeights: ProfileRouteWeights = ProfileRouteWeights(
        weightTime = 0.45,
        weightDistance = 0.25,
        weightManeuvers = 0.15,
        weightComplexity = 0.15,
    ),
    /** Tour acepta rutas algo más largas (dentro del límite) a cambio de menos giros y menor complejidad háptica. */
    val tourWeights: ProfileRouteWeights = ProfileRouteWeights(
        weightTime = 0.15,
        weightDistance = 0.15,
        weightManeuvers = 0.35,
        weightComplexity = 0.35,
    ),
) {
    val totalComplexityWeight: Double
        get() = shortStepPenaltyWeight + sharpTurnPenaltyWeight + turnDensityPenaltyWeight

    init {
        validate()
    }

    fun weightsFor(profile: NavigationProfile): ProfileRouteWeights = when (profile) {
        NavigationProfile.FOCUS -> focusWeights
        NavigationProfile.TOUR -> tourWeights
    }

    private fun validate() {
        require(maxDurationRatioToFastest > 1.0) {
            "maxDurationRatioToFastest debe ser > 1.0: $maxDurationRatioToFastest"
        }
        require(maxDistanceRatioToShortest > 1.0) {
            "maxDistanceRatioToShortest debe ser > 1.0: $maxDistanceRatioToShortest"
        }
        require(shortStepThresholdMeters > 0) {
            "shortStepThresholdMeters debe ser > 0: $shortStepThresholdMeters"
        }
        require(referenceTurnsPerKm > 0.0) {
            "referenceTurnsPerKm debe ser > 0: $referenceTurnsPerKm"
        }
        require(referenceManeuverCount > 0) {
            "referenceManeuverCount debe ser > 0: $referenceManeuverCount"
        }
        require(shortStepPenaltyWeight >= 0.0 && sharpTurnPenaltyWeight >= 0.0 && turnDensityPenaltyWeight >= 0.0) {
            "los pesos de complejidad no pueden ser negativos"
        }
        require(totalComplexityWeight > 0.0) {
            "la suma de pesos de complejidad debe ser > 0"
        }
        require(focusWeights.efficiencyWeight > focusWeights.simplicityWeight) {
            "focusWeights debe priorizar tiempo/distancia sobre simplicidad: $focusWeights"
        }
        require(tourWeights.simplicityWeight > tourWeights.efficiencyWeight) {
            "tourWeights debe priorizar maniobras/complejidad sobre tiempo/distancia: $tourWeights"
        }
    }
}
