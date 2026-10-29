package com.xanderjs.haptimap.presentation.debug

import com.xanderjs.haptimap.core.network.RoutesRepository

/** En release las rutas son siempre las de Google: no hay ruta falsa (src/debug). Sin clave, MISSING_KEY. */
@Suppress("UNUSED_PARAMETER")
fun debugRoutes(routes: RoutesRepository, language: () -> String): RoutesRepository = routes
