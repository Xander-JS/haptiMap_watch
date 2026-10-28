package com.xanderjs.haptimap.core.model

import kotlin.time.Duration

/**
 * Ruta a pie hasta [destination] (paso 05). [points] es la geometría completa; [steps] van en orden
 * de recorrido y el último es siempre un paso [Maneuver.ARRIVAL] de 0 m en el final.
 */
data class Route(
    val destination: Place,
    val distanceMeters: Int,
    val duration: Duration,
    val points: List<GeoPoint>,
    val steps: List<RouteStep>,
)

/**
 * Un tramo: [maneuver] se hace al empezarlo y después se caminan [distanceMeters]. [instruction] es
 * el texto del proveedor en el idioma pedido; [sourceManeuver] es su maniobra original
 * (p. ej. TURN_SHARP_LEFT), que [maneuver] simplifica, guardada para afinar los giros más adelante.
 */
data class RouteStep(
    val maneuver: Maneuver,
    val distanceMeters: Int,
    val duration: Duration,
    val points: List<GeoPoint>,
    val start: GeoPoint,
    val end: GeoPoint,
    val instruction: String?,
    val sourceManeuver: String?,
)

/** Maniobra semántica, independiente del proveedor. Los giros suaves y cerrados cuentan como giros. */
enum class Maneuver { TURN_LEFT, TURN_RIGHT, CONTINUE, ARRIVAL }

/** Resultado de pedir una ruta: la ruta o un error explícito. */
sealed interface RouteResult {
    data class Success(val route: Route) : RouteResult
    data class Failure(val error: RouteError) : RouteResult
}

/** Por qué no hay ruta. MISSING_KEY: la app se compiló sin clave y no se llega a pedir. Los API_* son fallos de Google. */
enum class RouteError {
    NO_CONNECTION,
    NO_LOCATION,
    NO_ROUTE,
    MISSING_KEY,
    API_QUOTA,
    API_INVALID_KEY,
    API_ERROR,
}
