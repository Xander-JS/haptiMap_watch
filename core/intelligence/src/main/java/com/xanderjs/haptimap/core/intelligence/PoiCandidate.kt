package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.PlaceCategory

/**
 * Un POI candidato a sugerir en Modo Tour (paso 18), independiente de las clases de Google Places.
 * [category] es null si la fuente no lo mapea a una categoría rápida (p. ej. un museo o un mirador);
 * [rating] (0..[PoiEvaluationConfig.ratingScaleMax]) y [popularity] (nº de valoraciones) son señales
 * opcionales que puede faltar.
 */
data class PoiCandidate(
    val id: String,
    val name: String,
    val category: PlaceCategory?,
    val location: GeoPoint,
    val rating: Double? = null,
    val popularity: Int? = null,
)

/**
 * Contrato para que el paso 17 convierta sus resultados (Google Places) en [PoiCandidate] sin que esta
 * capa conozca Google. El paso 17 implementa esto; la capa de inteligencia solo ve [PoiCandidate].
 */
fun interface PoiMapper<in T> {
    fun toCandidate(source: T): PoiCandidate
}
