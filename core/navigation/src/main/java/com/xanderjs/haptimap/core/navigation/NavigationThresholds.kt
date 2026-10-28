package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.LOW_ACCURACY_THRESHOLD_METERS
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Todos los umbrales del Navigation Engine en un sitio (paso 07). Son valores iniciales: el paso 14 los
 * ajusta con pruebas reales y puede hacerlos depender de la velocidad o del tipo de maniobra.
 * Ver docs/steps/07-motor-de-navegacion.md.
 */
data class NavigationThresholds(
    /** Primer aviso de una maniobra (documento de producto: 15–20 m antes). */
    val approachMeters: Double = 18.0,
    /** Maniobra inminente: ≈4 s a paso de peatón, cerca del retraso del GPS. El más probable de ajustar. */
    val imminentMeters: Double = 6.0,
    /** Maniobra hecha y paso siguiente cuando el avance la supera en esto. */
    val passedMeters: Double = 5.0,
    /** Llegada: a menos de esto del final, más la precisión del fix hasta [arrivalAccuracyAllowanceMeters]. */
    val arrivalRadiusMeters: Double = 10.0,
    val arrivalAccuracyAllowanceMeters: Double = 10.0,
    /** Fixes con más error que esto no mueven el avance ni disparan avisos. */
    val maxAccuracyMeters: Float = LOW_ACCURACY_THRESHOLD_METERS,
    /** Sin un fix bueno durante esto, señal GPS débil. */
    val weakSignalAfter: Duration = 10.seconds,
    /** Ventana de map matching alrededor del avance actual: hacia atrás, hacia delante y su crecimiento con el tiempo. */
    val searchBehindMeters: Double = 15.0,
    val searchAheadMeters: Double = 30.0,
    val maxWalkingSpeedMetersPerSecond: Double = 3.0,
    val searchAheadMaxMeters: Double = 100.0,
)
