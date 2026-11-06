package com.xanderjs.haptimap.core.model

/**
 * Categorías de exploración del Modo Tour (paso 18): lo que invita a parar en un paseo, no los
 * mandados del Modo Búsqueda ([PlaceCategory], que no se tocan). Un POI del Tour trae una de estas
 * (o ninguna); el paso 17 mapea los tipos de Google Places a ellas, y los ajustes (paso 25) dejan al
 * usuario marcar sus favoritas. Los textos visibles están en recursos.
 */
enum class ExplorationCategory {
    MUSEUM,
    PARK,

    /** Monumento o punto de referencia. */
    LANDMARK,

    /** Mirador. */
    VIEWPOINT,

    /** Sitio cultural: galería, centro cultural, teatro. */
    CULTURAL_SITE,

    /** Café o restaurante: paradas opcionales durante el paseo. */
    CAFE_RESTAURANT,
}
