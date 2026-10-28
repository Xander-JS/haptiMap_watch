package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.LOW_ACCURACY_THRESHOLD_METERS
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Todos los umbrales del Navigation Engine en un sitio (pasos 07 y 08). Son valores iniciales: el paso 14
 * los ajusta con pruebas reales y puede hacerlos depender de la velocidad o del tipo de maniobra.
 * Ver docs/steps/07-motor-de-navegacion.md y docs/steps/08-desvios-y-recalculo.md.
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
    /** Ganancias del filtro alfa-beta del avance: cuánto corrige cada fix la posición y la velocidad. */
    val filterAlpha: Double = 0.3,
    val filterBeta: Double = 0.05,
    /** Un fix bueno está fuera de la ruta a más de esto, más su precisión hasta [offRouteAccuracyAllowanceMeters] (paso 08). */
    val offRouteMeters: Double = 20.0,
    val offRouteAccuracyAllowanceMeters: Double = 10.0,
    /** Fuera de ruta se declara con fixes fuera seguidos durante esto y al menos [offRouteMinFixes]. */
    val offRouteAfter: Duration = 5.seconds,
    val offRouteMinFixes: Int = 3,
    /** Vuelta a la ruta (histéresis, más cerca que la salida): a menos de esto más la precisión, [backOnRouteFixes] fixes seguidos. */
    val backOnRouteMeters: Double = 12.0,
    val backOnRouteFixes: Int = 2,
    /** Espera entre declarar el desvío y pedir otra ruta: quien se da cuenta al notar el aviso vuelve sin gastar una petición. */
    val recalculationDelay: Duration = 10.seconds,
    /** Separación mínima entre dos peticiones, y primer escalón del backoff tras un fallo (se dobla hasta [recalculationMaxBackoff]). */
    val recalculationMinInterval: Duration = 15.seconds,
    val recalculationMaxBackoff: Duration = 60.seconds,
    /** Peticiones automáticas por desvío; después solo Reintentar. */
    val maxAutoRecalculations: Int = 4,
)
