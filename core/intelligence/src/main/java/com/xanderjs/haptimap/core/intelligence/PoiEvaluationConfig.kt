package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.PlaceCategory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Todos los umbrales y pesos de la evaluación de POIs en un solo sitio (sección 6, paso 18), sin
 * números mágicos repartidos por la lógica. Son valores iniciales razonables, pensados para exponerse
 * en la pantalla de ajustes (paso 25) sin tocar la capa de inteligencia.
 *
 * La clave de DeepSeek para un [RelevanceProvider] con IA en el futuro NO vive aquí: sigue en
 * BuildConfig.DEEPSEEK_API_KEY / local.properties (marcador = no se usa), como la de Google.
 */
data class PoiEvaluationConfig(
    /** Descarte "demasiado lejos de la ruta": distancia en línea recta del corredor de la ruta al POI. */
    val maxDistanceFromRouteMeters: Double = 100.0,
    /** Descarte "desvío demasiado grande": metros extra de ir y volver que estima [DetourEstimator]. */
    val maxDetourMeters: Double = 150.0,
    /** Descarte "añade demasiado tiempo": tiempo extra del desvío a paso de peatón. */
    val maxAddedTime: Duration = 3.minutes,
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
)

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
