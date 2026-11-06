package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.GeoPoint

/**
 * Un POI candidato a sugerir en Modo Tour (paso 18), independiente de las clases de Google Places.
 * [category] es una categoría de exploración ([ExplorationCategory]), o null si la fuente no lo mapea a
 * ninguna; [rating] (0..[PoiEvaluationConfig.ratingScaleMax]) y [popularity] (nº de valoraciones) son
 * señales opcionales que pueden faltar.
 */
data class PoiCandidate(
    val id: String,
    val name: String,
    val category: ExplorationCategory?,
    val location: GeoPoint,
    val rating: Double? = null,
    val popularity: Int? = null,
)

/**
 * Contrato para que el paso 17 convierta sus resultados (Google Places) en [PoiCandidate] sin que esta
 * capa conozca Google. El paso 17 implementa esto (mapeando los tipos de Places a [ExplorationCategory]);
 * la capa de inteligencia solo ve [PoiCandidate].
 */
fun interface PoiMapper<in T> {
    fun toCandidate(source: T): PoiCandidate
}
