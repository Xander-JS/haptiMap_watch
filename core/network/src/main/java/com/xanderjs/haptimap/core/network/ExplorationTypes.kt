package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ExplorationCategory

/**
 * Único sitio donde los tipos de Places API (New) se traducen a una [ExplorationCategory] del Modo Tour.
 * Es la costura del paso 17: su PoiMapper usará [explorationCategoryForPlaceType] para convertir cada
 * resultado de Places en un PoiCandidate con su categoría de exploración, sin que la capa de inteligencia
 * conozca Google. Independiente de [placeTypes] (Modo Búsqueda), que no se toca.
 *
 * shortcut: mapeo aproximado a los tipos de Places New; afinarlo con resultados reales en el paso 17.
 * Places New no expone "mirador", que suele llegar como tourist_attraction.
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

/** Inversa de [placeTypes]; null si el tipo no es de ninguna categoría de exploración. */
fun explorationCategoryForPlaceType(type: String?): ExplorationCategory? =
    ExplorationCategory.entries.firstOrNull { type in it.placeTypes }
