package com.xanderjs.haptimap.core.intelligence

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertThrows
import org.junit.Test

class PoiEvaluationConfigTest {

    @Test
    fun `the default configuration is coherent`() {
        PoiEvaluationConfig() // no lanza
    }

    @Test
    fun `a time budget the max detour can never reach is rejected`() {
        // a 1,3 m/s un desvío de 200 m tarda ~154 s, así que 5 min dejaría TOO_MUCH_TIME inalcanzable
        assertThrows(IllegalArgumentException::class.java) {
            PoiEvaluationConfig(maxDetourMeters = 200.0, maxAddedTime = 5.minutes, walkingSpeedMetersPerSecond = 1.3)
        }
    }

    @Test
    fun `a detour larger than the round trip to the max distance is rejected`() {
        // 2 × 120 = 240; un desvío de 300 m exigiría más distancia que maxDistance, enmascarando DETOUR_TOO_LARGE
        assertThrows(IllegalArgumentException::class.java) {
            PoiEvaluationConfig(maxDistanceFromRouteMeters = 120.0, maxDetourMeters = 300.0, maxAddedTime = 30.seconds)
        }
    }

    @Test
    fun `out-of-range scalars are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { PoiEvaluationConfig(minRelevance = 1.5) }
        assertThrows(IllegalArgumentException::class.java) { PoiEvaluationConfig(favoriteBoost = 0.5) }
        assertThrows(IllegalArgumentException::class.java) { PoiEvaluationConfig(walkingSpeedMetersPerSecond = 0.0) }
    }
}
