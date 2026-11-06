package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ExplorationCategory

/**
 * Único sitio donde los tipos de Places API (New) se traducen a una [ExplorationCategory] del Modo Tour.
 * Es la costura del paso 17: el PoiMapper usa [explorationCategoryForPlaceType] para convertir cada resultado
 * de Places en un PoiCandidate con su categoría, sin que la capa de inteligencia conozca Google. Independiente
 * de [PlaceCategory.placeTypes] (Modo Búsqueda), que no se toca.
 */
val ExplorationCategory.placeTypes: List<String>
    get() = when (this) {
        ExplorationCategory.MUSEUM -> listOf("museum")
        ExplorationCategory.PARK -> listOf("park", "national_park")
        ExplorationCategory.LANDMARK -> listOf("historical_landmark", "monument")
        ExplorationCategory.VIEWPOINT -> listOf("tourist_attraction")
        ExplorationCategory.CULTURAL_SITE -> listOf("art_gallery", "cultural_center", "performing_arts_theater")
        ExplorationCategory.CAFE_RESTAURANT -> listOf("cafe", "restaurant")
    }

/**
 * Subtipos que Places (New) devuelve en primaryType, más específicos que los tipos pedidos en includedTypes
 * (visto con datos reales: el Prado llega como "art_museum", no "museum"). La inversa los reconoce para no
 * descartar lugares válidos; en las consultas se siguen pidiendo los tipos base de [placeTypes] (Tabla A).
 */
private val PRIMARY_TYPE_ALIASES: Map<String, ExplorationCategory> = mapOf(
    "art_museum" to ExplorationCategory.MUSEUM,
    "history_museum" to ExplorationCategory.MUSEUM,
    "science_museum" to ExplorationCategory.MUSEUM,
    "national_park" to ExplorationCategory.PARK,
    "wildlife_park" to ExplorationCategory.PARK,
    "garden" to ExplorationCategory.PARK,
    "monument" to ExplorationCategory.LANDMARK,
    "historical_place" to ExplorationCategory.LANDMARK,
    "cultural_landmark" to ExplorationCategory.LANDMARK,
    "observation_deck" to ExplorationCategory.VIEWPOINT,
    "performing_arts_theater" to ExplorationCategory.CULTURAL_SITE,
    "cultural_center" to ExplorationCategory.CULTURAL_SITE,
    "coffee_shop" to ExplorationCategory.CAFE_RESTAURANT,
)

private val PRIMARY_TYPE_CATEGORY: Map<String, ExplorationCategory> =
    ExplorationCategory.entries.flatMap { c -> c.placeTypes.map { it to c } }.toMap() + PRIMARY_TYPE_ALIASES

/** La categoría de exploración de un primaryType de Places (tipo base o subtipo); null si no es de ninguna. */
fun explorationCategoryForPlaceType(type: String?): ExplorationCategory? = type?.let { PRIMARY_TYPE_CATEGORY[it] }
