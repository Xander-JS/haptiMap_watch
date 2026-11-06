package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.DetourTolerance
import com.xanderjs.haptimap.core.model.UserSettings
import kotlin.time.Duration.Companion.seconds

/**
 * Traduce los ajustes del usuario (paso 25) a la capa de inteligencia (paso 18), única fuente de estos valores.
 * El preset de tolerancia al desvío se convierte en los tres umbrales de [PoiEvaluationConfig], siempre coherentes
 * (pasan su [PoiEvaluationConfig.validate]); las favoritas alimentan el plus de [RuleBasedRelevanceProvider].
 */
fun DetourTolerance.toPoiEvaluationConfig(): PoiEvaluationConfig = when (this) {
    // corto, medio y largo mantienen velocidad×tiempo < desvío < 2×distancia (ver PoiEvaluationConfig)
    DetourTolerance.SHORT -> PoiEvaluationConfig(maxDistanceFromRouteMeters = 60.0, maxDetourMeters = 90.0, maxAddedTime = 50.seconds)
    DetourTolerance.MEDIUM -> PoiEvaluationConfig(maxDistanceFromRouteMeters = 120.0, maxDetourMeters = 200.0, maxAddedTime = 120.seconds)
    DetourTolerance.LONG -> PoiEvaluationConfig(maxDistanceFromRouteMeters = 200.0, maxDetourMeters = 340.0, maxAddedTime = 220.seconds)
}

/**
 * El evaluador de POIs del Modo Tour configurado con los ajustes (paso 25): desvío por preset y favoritas al
 * proveedor de relevancia. Lo que el paso 19 usará para puntuar y filtrar sugerencias.
 */
fun poiEvaluator(settings: UserSettings): PoiEvaluator {
    val config = settings.detourTolerance.toPoiEvaluationConfig()
    return PoiEvaluator(
        config = config,
        detourEstimator = GeometricDetourEstimator(config),
        relevanceProvider = RuleBasedRelevanceProvider(config, settings.favoriteCategories),
    )
}
