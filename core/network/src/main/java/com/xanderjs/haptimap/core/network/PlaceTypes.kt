package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.PlaceCategory

/** Único sitio donde cada categoría rápida se traduce a tipos de Places API (New). */
val PlaceCategory.placeTypes: List<String>
    get() = when (this) {
        PlaceCategory.STORE -> listOf("store")
        PlaceCategory.HARDWARE_STORE -> listOf("hardware_store")
        PlaceCategory.RESTAURANT -> listOf("restaurant")
        PlaceCategory.CAFE -> listOf("cafe")
        PlaceCategory.PHARMACY -> listOf("pharmacy")
        PlaceCategory.TRANSIT -> listOf("transit_station", "bus_station", "subway_station", "train_station", "light_rail_station")
        PlaceCategory.SUPERMARKET -> listOf("supermarket")
    }

/** Inversa de [placeTypes] para los resultados de texto; null si el tipo no es de ninguna categoría. */
fun categoryForPlaceType(type: String?): PlaceCategory? =
    PlaceCategory.entries.firstOrNull { type in it.placeTypes }
