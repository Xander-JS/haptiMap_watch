package com.xanderjs.haptimap.core.location

/** Prioridad neutra del muestreo; [FusedLocationSource] la traduce a com.google.android.gms.location.Priority. */
enum class SamplingPriority { HIGH_ACCURACY, BALANCED }

/** Cómo se pide la ubicación. Lo decide [AdaptiveLocationPolicy] (paso 23); nadie más fija estos valores. */
data class LocationSampling(
    val intervalMillis: Long,
    val minUpdateMeters: Float,
    val priority: SamplingPriority,
) {
    companion object {
        /** Reserva segura: crucero a pie con alta precisión. La usan firstPoint y el primer fix antes de haber contexto. */
        val Cruise = LocationSampling(2_000L, 0f, SamplingPriority.HIGH_ACCURACY)
    }
}

/**
 * El contexto de navegación que decide el muestreo, en primitivos: la política es pura y no conoce el Navigation
 * Engine. El llamador lo compone desde NavigationProgress y el estado del desvío/sugerencia (paso 19/20).
 */
data class SamplingContext(
    /** Distancia a lo largo de la ruta hasta la próxima maniobra; null si aún no se navega. */
    val distanceToManeuverMeters: Double?,
    /** Velocidad filtrada a lo largo de la ruta (m/s). */
    val speedMetersPerSecond: Double,
    /** Desvío en curso o sugerencia de POI en pantalla (paso 19/20): puede haber una decisión cerca. */
    val detourOrSuggestionActive: Boolean,
    /** Desvío ya declarado (paso 08). */
    val offRoute: Boolean,
    /** Distancia a la ruta del último fix bueno; si se acerca al umbral de desvío, sube el ritmo. null si no hay fix. */
    val crossTrackMeters: Double?,
    /** Precisión del último fix bueno (m); null si no hay. */
    val accuracyMeters: Float?,
)

/** El régimen de muestreo elegido; cada uno es un [LocationSampling] distinto (ver [AdaptiveLocationPolicy.samplingFor]). */
enum class SamplingRegime { FAST, CRUISE, RELAXED, STATIONARY }

/**
 * Política de muestreo adaptativo (paso 23): el ÚNICO sitio donde se deciden intervalo, distancia mínima y prioridad
 * del GPS, a partir del contexto de navegación. Ahorra batería bajando el ritmo en rectas largas y con el usuario
 * parado, y lo sube cerca de maniobras, desvíos/sugerencias, con riesgo de salirse o con mala precisión, sin que los
 * avisos hápticos lleguen tarde.
 *
 * Fórmula del ritmo (docs/steps/23-gps-adaptativo.md): el aviso más ajustado es el inminente, a [imminentMeters]
 * antes de la maniobra. Para que al menos [fixesInZone] fixes caigan en esa zona antes de llegar al giro, el avance
 * por muestra v·T no puede superarla; a la velocidad máxima a pie:
 *     T_fast = imminentMeters / (fixesInZone · maxWalkingSpeed)      (6 / (2·3) = 1 s)
 * Y para estar ya en modo rápido antes del primer aviso (a [approachMeters]), se entra en rápido una muestra de
 * crucero antes de esa zona:
 *     armMeters = approachMeters + maxWalkingSpeed · T_cruise        (18 + 3·2 = 24 m → se usa con margen)
 *
 * Histéresis (evitar el parpadeo entre ritmos) y tope de cambios: acelerar (intervalo menor) es inmediato —nunca se
 * retrasa un aviso—; frenar (intervalo mayor) exige que pasen [minChangeIntervalMillis] desde el último cambio, y los
 * bordes de régimen usan umbrales de entrada/salida distintos.
 */
class AdaptiveLocationPolicy(
    private val imminentMeters: Double = 6.0,                 // = NavigationThresholds.imminentMeters
    private val approachMeters: Double = 18.0,                // = NavigationThresholds.approachMeters
    private val maxWalkingSpeed: Double = 3.0,                // = NavigationThresholds.maxWalkingSpeedMetersPerSecond
    private val offRouteMeters: Double = 20.0,                // = NavigationThresholds.offRouteMeters
    private val fixesInZone: Int = 2,
    private val relaxedAboveMeters: Double = 120.0,
    private val stationarySpeed: Double = 0.3,
    private val movingSpeed: Double = 0.6,                    // histéresis: para salir de PARADO hay que moverse de verdad
    private val poorAccuracyMeters: Float = 30f,
    private val minChangeIntervalMillis: Long = 12_000L,
) {
    private val fastInterval = Math.round(imminentMeters / (fixesInZone * maxWalkingSpeed) * 1000.0).coerceAtLeast(1)
    private val cruiseInterval = 2_000L
    private val relaxedInterval = 4_000L
    private val stationaryInterval = 6_000L

    /** Distancia a la maniobra para entrar en rápido; con margen de una muestra de crucero sobre la zona de aviso. */
    private val armMeters = approachMeters + maxWalkingSpeed * (cruiseInterval / 1000.0) + 6.0
    private val armExitMeters = armMeters + 10.0             // histéresis: salir de rápido algo más lejos
    private val relaxedExitMeters = relaxedAboveMeters - 20.0
    /** A partir de esta distancia a la ruta, riesgo de salirse (antes de declararlo a offRouteMeters). */
    private val offRouteRiskMeters = offRouteMeters * 0.6

    /** El régimen crudo para un contexto, con histéresis en los bordes respecto a [previous]. */
    fun desiredRegime(context: SamplingContext, previous: SamplingRegime?): SamplingRegime {
        // 1) seguridad y precisión: siempre rápido
        if (context.offRoute || (context.crossTrackMeters ?: 0.0) > offRouteRiskMeters) return SamplingRegime.FAST
        if (context.detourOrSuggestionActive) return SamplingRegime.FAST
        if (context.accuracyMeters != null && context.accuracyMeters > poorAccuracyMeters) return SamplingRegime.FAST
        // 2) cerca de una maniobra
        val d = context.distanceToManeuverMeters
        if (d != null) {
            val enterFast = if (previous == SamplingRegime.FAST) armExitMeters else armMeters
            if (d <= enterFast) return SamplingRegime.FAST
        }
        // 3) parado (no cerca de un giro): bajar mucho el ritmo
        val stoppedBelow = if (previous == SamplingRegime.STATIONARY) movingSpeed else stationarySpeed
        if (context.speedMetersPerSecond < stoppedBelow) return SamplingRegime.STATIONARY
        // 4) recta larga
        if (d != null) {
            val enterRelaxed = if (previous == SamplingRegime.RELAXED) relaxedExitMeters else relaxedAboveMeters
            if (d > enterRelaxed) return SamplingRegime.RELAXED
        }
        return SamplingRegime.CRUISE
    }

    fun samplingFor(regime: SamplingRegime): LocationSampling = when (regime) {
        // minUpdateMeters 0 cerca del giro (avisos a tiempo) y al arrancar parado (detectar el primer paso); mayor en
        // recta para saltarse el ruido del GPS parado. BALANCED parado: sin moverse no hace falta la precisión del GPS.
        SamplingRegime.FAST -> LocationSampling(fastInterval, 0f, SamplingPriority.HIGH_ACCURACY)
        SamplingRegime.CRUISE -> LocationSampling(cruiseInterval, 3f, SamplingPriority.HIGH_ACCURACY)
        SamplingRegime.RELAXED -> LocationSampling(relaxedInterval, 8f, SamplingPriority.HIGH_ACCURACY)
        SamplingRegime.STATIONARY -> LocationSampling(stationaryInterval, 0f, SamplingPriority.BALANCED)
    }

    /**
     * La decisión siguiente dado el contexto y [nowMillis]. Acelerar es inmediato; frenar espera al tope de cambios.
     * [previous] null en el primer fix y tras reiniciar el proceso (la política no persiste: se recompone del contexto).
     */
    fun next(previous: Decision?, context: SamplingContext, nowMillis: Long): Decision {
        val desired = desiredRegime(context, previous?.regime)
        if (previous == null) return Decision(desired, samplingFor(desired), nowMillis)
        if (desired == previous.regime) return previous
        val faster = samplingFor(desired).intervalMillis < previous.sampling.intervalMillis
        val allowed = faster || nowMillis - previous.changedAtMillis >= minChangeIntervalMillis
        return if (allowed) Decision(desired, samplingFor(desired), nowMillis) else previous
    }

    /** Una decisión y cuándo se tomó (para el tope de cambios). */
    data class Decision(val regime: SamplingRegime, val sampling: LocationSampling, val changedAtMillis: Long)
}
