package com.xanderjs.haptimap.presentation.debug

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.RouteResult
import com.xanderjs.haptimap.core.network.RoutesRepository

/** En release el recálculo es siempre el de Google: no hay ruta falsa (src/debug). Sin clave, MISSING_KEY. */
@Suppress("UNUSED_PARAMETER")
fun rerouteRequest(routes: RoutesRepository, language: () -> String): suspend (GeoPoint, Place) -> RouteResult =
    { origin, destination -> routes.computeRoute(origin, destination) }
