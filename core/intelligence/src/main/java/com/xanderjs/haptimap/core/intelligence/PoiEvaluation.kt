package com.xanderjs.haptimap.core.intelligence

import kotlin.time.Duration

/**
 * El veredicto sobre un [PoiCandidate] (paso 18): su [score] para ordenar, el desvío estimado
 * ([detourMeters] de ir y volver, [addedTime] a paso de peatón) y la [decision]. [discardReason] es
 * no nulo si y solo si [decision] es [PoiDecision.DISCARD]. El score (relevancia + cercanía) es 0 en los descartados.
 */
data class PoiEvaluation(
    val candidate: PoiCandidate,
    val score: Double,
    val detourMeters: Double,
    val addedTime: Duration,
    val decision: PoiDecision,
    val discardReason: DiscardReason?,
)

enum class PoiDecision { SUGGEST, DISCARD }

/** Por qué se descarta un POI. Orden de precedencia el de [PoiEvaluator]: el primero que falla manda. */
enum class DiscardReason {
    /** Ya ignorado o caducado en este trayecto: no se vuelve a sugerir. */
    ALREADY_REJECTED,

    /** Demasiado lejos del corredor de la ruta. */
    TOO_FAR,

    /** El desvío de ir y volver es demasiado largo. */
    DETOUR_TOO_LARGE,

    /** El desvío añade demasiado tiempo. */
    TOO_MUCH_TIME,

    /** Poca relevancia para el contexto del paseo. */
    LOW_RELEVANCE,
}
