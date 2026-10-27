package com.xanderjs.haptimap.core.model

/**
 * Lugar elegible como destino. [category] es null si el tipo de Places no corresponde a ninguna
 * categoría rápida (resultados de búsqueda por texto). [distanceMeters] es desde la posición del usuario al buscar.
 */
data class Place(
    val id: String,
    val name: String,
    val location: GeoPoint,
    val category: PlaceCategory?,
    val address: String?,
    val distanceMeters: Int,
)
