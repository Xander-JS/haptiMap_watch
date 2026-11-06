package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.PlaceCategory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

/**
 * Todos los umbrales y pesos de la evaluación de POIs en un solo sitio (sección 6, paso 18), sin
 * números mágicos repartidos por la lógica. Son valores iniciales razonables, pensados para exponerse
 * en la pantalla de ajustes (paso 25) sin tocar la capa de inteligencia.
 *
 * Los tres umbrales de desvío son coherentes entre sí: con el estimador geométrico (ida y vuelta,
 * `detour = 2 × distancia`, `tiempo = detour / velocidad`) cada regla se reduce a un umbral de distancia
 * `d_tiempo < d_desvío < d_lejos`, de modo que cada motivo puede llegar a ser el que decide. [validate]
 * lo comprueba para que no se configure una combinación en la que una regla queda enmascarada (p. ej. un
 * tiempo máximo tan alto que el desvío máximo lo hace inalcanzable).
 *
 * La clave de DeepSeek para un [RelevanceProvider] con IA en el futuro NO vive aquí: sigue en
 * BuildConfig.DEEPSEEK_API_KEY / local.properties (marcador = no se usa), como la de Google.
 */
data class PoiEvaluationConfig(
    /** Descarte "demasiado lejos de la ruta": distancia en línea recta del corredor de la ruta al POI. */
    val maxDistanceFromRouteMeters: Double = 120.0,
    /** Descarte "desvío demasiado grande": metros extra de ir y volver que estima [DetourEstimator]. */
    val maxDetourMeters: Double = 200.0,
    /** Descarte "añade demasiado tiempo": tiempo extra del desvío a paso de peatón. */
    val maxAddedTime: Duration = 2.minutes,
    /** Descarte "poca relevancia": relevancia (0..1) por debajo de esto. */
    val minRelevance: Double = 0.3,
    /** Velocidad peatonal para pasar el desvío (m) a tiempo (~4,7 km/h). */
    val walkingSpeedMetersPerSecond: Double = 1.3,

    /** Peso base por categoría (0..1) de [RuleBasedRelevanceProvider]; lo que falte usa [defaultCategoryWeight]. */
    val categoryWeights: Map<PlaceCategory, Double> = DEFAULT_CATEGORY_WEIGHTS,
    /** Peso de una categoría sin entrada o nula (p. ej. una atracción que Places no mapea a una categoría rápida). */
    val defaultCategoryWeight: Double = 0.6,
    /** Multiplica la relevancia de una categoría favorita del usuario (recortada a 1.0). */
    val favoriteBoost: Double = 1.5,

    // Pesos del score (mayor = se sugiere antes). Ordenan los POIs que superan las reglas.
    val weightRelevance: Double = 1.0,
    /** Cercanía: premia el desvío pequeño. */
    val weightProximity: Double = 0.6,
    val weightRating: Double = 0.4,
    /** Valoración máxima de la fuente (Google: 5 estrellas) para normalizar [PoiCandidate.rating] a 0..1. */
    val ratingScaleMax: Double = 5.0,
) {
    init { validate() }

    /**
     * Rechaza combinaciones incoherentes (sección 6, paso 18). Además de rangos básicos, impone que los tres
     * umbrales de desvío estén ordenados como distancias para el estimador geométrico de ida y vuelta, así cada
     * regla puede ser la que decide y ninguna queda enmascarada por otra:
     *
     *   velocidad × tiempoMáx  <  desvíoMáx  <  2 × distanciaMáx
     *
     * (equivale a `d_tiempo < d_desvío < d_lejos`). Lanza [IllegalArgumentException] con el porqué.
     */
    private fun validate() {
        require(maxDistanceFromRouteMeters > 0) { "maxDistanceFromRouteMeters debe ser > 0: $maxDistanceFromRouteMeters" }
        require(maxDetourMeters > 0) { "maxDetourMeters debe ser > 0: $maxDetourMeters" }
        require(maxAddedTime.isPositive()) { "maxAddedTime debe ser > 0: $maxAddedTime" }
        require(walkingSpeedMetersPerSecond > 0) { "walkingSpeedMetersPerSecond debe ser > 0: $walkingSpeedMetersPerSecond" }
        require(minRelevance in 0.0..1.0) { "minRelevance debe estar en 0..1: $minRelevance" }
        require(defaultCategoryWeight in 0.0..1.0) { "defaultCategoryWeight debe estar en 0..1: $defaultCategoryWeight" }
        require(categoryWeights.values.all { it in 0.0..1.0 }) { "categoryWeights deben estar en 0..1: $categoryWeights" }
        require(favoriteBoost >= 1.0) { "favoriteBoost debe ser >= 1: $favoriteBoost" }
        require(weightRelevance >= 0 && weightProximity >= 0 && weightRating >= 0) { "los pesos del score no pueden ser negativos" }
        require(ratingScaleMax > 0) { "ratingScaleMax debe ser > 0: $ratingScaleMax" }

        // coherencia con el estimador geométrico: cada umbral, llevado a distancia, por debajo del siguiente
        val detourFromTimeMeters = walkingSpeedMetersPerSecond * maxAddedTime.toDouble(DurationUnit.SECONDS)
        require(detourFromTimeMeters < maxDetourMeters) {
            "maxAddedTime demasiado alto: a $walkingSpeedMetersPerSecond m/s el desvío máximo ($maxDetourMeters m) tarda " +
                "menos de $maxAddedTime, así que TOO_MUCH_TIME nunca decide"
        }
        require(maxDetourMeters < 2 * maxDistanceFromRouteMeters) {
            "maxDetourMeters demasiado alto: con ida y vuelta (2 × distancia) un desvío de $maxDetourMeters m exige más de " +
                "$maxDistanceFromRouteMeters m de distancia, así que DETOUR_TOO_LARGE nunca decide antes que TOO_FAR"
        }
    }
}

/** Modo Tour: un café o un restaurante invitan a parar; una ferretería o el transporte, menos (paso 18). */
val DEFAULT_CATEGORY_WEIGHTS: Map<PlaceCategory, Double> = mapOf(
    PlaceCategory.CAFE to 0.9,
    PlaceCategory.RESTAURANT to 0.8,
    PlaceCategory.STORE to 0.6,
    PlaceCategory.SUPERMARKET to 0.4,
    PlaceCategory.TRANSIT to 0.3,
    PlaceCategory.PHARMACY to 0.3,
    PlaceCategory.HARDWARE_STORE to 0.2,
)
