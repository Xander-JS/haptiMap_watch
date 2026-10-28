package com.xanderjs.haptimap.core.model

/**
 * Eventos semánticos que el Haptic Engine traduce a vibración (sección 11).
 * El resto de la app solo conoce estos eventos, nunca tiempos ni amplitudes.
 */
enum class HapticEvent {
    TURN_LEFT,
    TURN_RIGHT,
    CONTINUE,
    TURN_IMMINENT,
    ARRIVAL,
    OFF_ROUTE,

    /** De vuelta en la ruta, por su cuenta o con una recalculada (paso 09). */
    BACK_ON_ROUTE,
    POI_SUGGESTION,
}
