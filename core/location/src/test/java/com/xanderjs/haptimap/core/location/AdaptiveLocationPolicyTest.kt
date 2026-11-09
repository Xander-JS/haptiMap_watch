package com.xanderjs.haptimap.core.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Decisiones de la política de muestreo adaptativo (paso 23): por escenario, histéresis, tope y garantía de avisos. */
class AdaptiveLocationPolicyTest {

    private val policy = AdaptiveLocationPolicy()

    // valores por defecto que usan los escenarios; los cue-timing cuentan con estos
    private val imminentMeters = 6.0
    private val approachMeters = 18.0
    private val maxWalkingSpeed = 3.0

    private fun ctx(
        distance: Double? = 200.0,
        speed: Double = 1.4,
        suggestion: Boolean = false,
        distanceToPoi: Double? = null,
        offRoute: Boolean = false,
        crossTrack: Double? = 0.0,
        accuracy: Float? = 5f,
    ) = SamplingContext(distance, speed, suggestion, distanceToPoi, offRoute, crossTrack, accuracy)

    private fun regime(context: SamplingContext, previous: SamplingRegime? = null) = policy.desiredRegime(context, previous)

    // --- un régimen por escenario ------------------------------------------

    @Test fun `long straight relaxes`() {
        assertEquals(SamplingRegime.RELAXED, regime(ctx(distance = 300.0, speed = 1.4)))
    }

    @Test fun `moderate distance cruises`() {
        assertEquals(SamplingRegime.CRUISE, regime(ctx(distance = 80.0, speed = 1.4)))
    }

    @Test fun `approaching a turn goes fast`() {
        assertEquals(SamplingRegime.FAST, regime(ctx(distance = 25.0, speed = 1.4)))
    }

    @Test fun `imminent turn goes fast`() {
        assertEquals(SamplingRegime.FAST, regime(ctx(distance = 3.0, speed = 1.4)))
    }

    @Test fun `active suggestion goes fast even far from a turn`() {
        assertEquals(SamplingRegime.FAST, regime(ctx(distance = 300.0, suggestion = true)))
    }

    @Test fun `a detour far from the POI and any turn uses the normal distance regimes`() {
        // yendo al POI pero aún lejos (y en recta): NO fuerza rápido, se relaja como una ruta normal larga
        assertEquals(SamplingRegime.RELAXED, regime(ctx(distance = 300.0, distanceToPoi = 300.0)))
        assertEquals(SamplingRegime.CRUISE, regime(ctx(distance = 80.0, distanceToPoi = 80.0)))
    }

    @Test fun `approaching the detour POI goes fast with the same arm distance`() {
        // cerca del POI (dentro de la distancia de armado) aunque la próxima maniobra esté lejos: rápido a tiempo
        assertEquals(SamplingRegime.FAST, regime(ctx(distance = 300.0, distanceToPoi = 25.0)))
    }

    @Test fun `low accuracy goes fast`() {
        assertEquals(SamplingRegime.FAST, regime(ctx(distance = 300.0, accuracy = 45f)))
    }

    @Test fun `off-route risk goes fast`() {
        assertEquals(SamplingRegime.FAST, regime(ctx(distance = 300.0, crossTrack = 15.0)))
        assertEquals(SamplingRegime.FAST, regime(ctx(distance = 300.0, offRoute = true)))
    }

    @Test fun `stationary user slows down and uses balanced power`() {
        assertEquals(SamplingRegime.STATIONARY, regime(ctx(distance = 300.0, speed = 0.1)))
        assertEquals(SamplingPriority.BALANCED, policy.samplingFor(SamplingRegime.STATIONARY).priority)
    }

    @Test fun `no maneuver distance yet cruises when moving`() {
        assertEquals(SamplingRegime.CRUISE, regime(ctx(distance = null, speed = 1.4)))
    }

    // --- histéresis en los bordes -----------------------------------------

    @Test fun `fast has a wider exit than entry near a turn`() {
        // a 35 m: entrando desde crucero NO es rápido (umbral 30); ya en rápido SÍ lo sigue (umbral de salida 40)
        assertEquals(SamplingRegime.CRUISE, regime(ctx(distance = 35.0), previous = SamplingRegime.CRUISE))
        assertEquals(SamplingRegime.FAST, regime(ctx(distance = 35.0), previous = SamplingRegime.FAST))
    }

    @Test fun `stationary has a higher exit speed than entry`() {
        // a 0,5 m/s: ya parado sigue parado (sale al superar 0,6); en marcha no entra en parado (entra bajo 0,3)
        assertEquals(SamplingRegime.STATIONARY, regime(ctx(distance = 300.0, speed = 0.5), previous = SamplingRegime.STATIONARY))
        assertEquals(SamplingRegime.RELAXED, regime(ctx(distance = 300.0, speed = 0.5), previous = SamplingRegime.RELAXED))
    }

    // --- tope de cambios (next) -------------------------------------------

    @Test fun `speeding up is immediate`() {
        val relaxed = policy.next(null, ctx(distance = 300.0), nowMillis = 0)
        assertEquals(SamplingRegime.RELAXED, relaxed.regime)
        val fast = policy.next(relaxed, ctx(distance = 10.0), nowMillis = 500)
        assertEquals(SamplingRegime.FAST, fast.regime) // acelerar no espera al tope
    }

    @Test fun `slowing down waits for the change cap`() {
        val fast = policy.next(null, ctx(distance = 10.0), nowMillis = 0)
        assertEquals(SamplingRegime.FAST, fast.regime)
        // quiere relajarse, pero aún no han pasado 12 s: se mantiene rápido
        val stillFast = policy.next(fast, ctx(distance = 300.0), nowMillis = 5_000)
        assertEquals(SamplingRegime.FAST, stillFast.regime)
        // pasado el tope, ya baja
        val relaxed = policy.next(fast, ctx(distance = 300.0), nowMillis = 12_000)
        assertEquals(SamplingRegime.RELAXED, relaxed.regime)
    }

    // --- garantía de avisos a tiempo --------------------------------------

    @Test fun `fast rate samples inside the imminent zone before the turn`() {
        // el aviso inminente está a imminentMeters; a ritmo rápido el avance por muestra no supera esa zona (de hecho
        // deja 2 fixes dentro), así el cruce se muestrea antes de llegar al giro, a la velocidad máxima a pie
        val fastIntervalSeconds = policy.samplingFor(SamplingRegime.FAST).intervalMillis / 1000.0
        val metersPerSample = maxWalkingSpeed * fastIntervalSeconds
        assertTrue("avance por muestra $metersPerSample m > zona inminente $imminentMeters m", metersPerSample <= imminentMeters)
    }

    @Test fun `near a maneuver the regime is always fast`() {
        // tanto en el primer aviso (approachMeters) como en el inminente (imminentMeters) el régimen es rápido,
        // así que aplica el intervalo que garantiza el aviso a tiempo
        assertEquals(SamplingRegime.FAST, regime(ctx(distance = approachMeters)))
        assertEquals(SamplingRegime.FAST, regime(ctx(distance = imminentMeters)))
    }

    @Test fun `faster regimes have shorter intervals`() {
        val fast = policy.samplingFor(SamplingRegime.FAST).intervalMillis
        val cruise = policy.samplingFor(SamplingRegime.CRUISE).intervalMillis
        val relaxed = policy.samplingFor(SamplingRegime.RELAXED).intervalMillis
        val stationary = policy.samplingFor(SamplingRegime.STATIONARY).intervalMillis
        assertTrue(fast < cruise && cruise < relaxed && relaxed < stationary)
    }
}
