package com.xanderjs.haptimap.presentation.debug

import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.network.PlacesRepository

/** En release Places es siempre el de Google: no hay lugares falsos (src/debug). Sin clave, API_INVALID_KEY. */
@Suppress("UNUSED_PARAMETER")
fun debugPlaces(places: PlacesRepository, hasApiKey: Boolean, label: (PlaceCategory) -> String): PlacesRepository = places
