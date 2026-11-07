package com.xanderjs.haptimap.core.intelligence

import org.junit.Assert.assertThrows
import org.junit.Test

class RouteEvaluationConfigTest {

    @Test
    fun `the default configuration is coherent`() {
        RouteEvaluationConfig() // no lanza
    }

    @Test
    fun `max duration and distance ratios must be strictly greater than one`() {
        assertThrows(IllegalArgumentException::class.java) {
            RouteEvaluationConfig(maxDurationRatioToFastest = 1.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RouteEvaluationConfig(maxDistanceRatioToShortest = 0.95)
        }
    }

    @Test
    fun `thresholds and reference scales must be positive`() {
        assertThrows(IllegalArgumentException::class.java) {
            RouteEvaluationConfig(shortStepThresholdMeters = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RouteEvaluationConfig(referenceTurnsPerKm = 0.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RouteEvaluationConfig(referenceManeuverCount = 0)
        }
    }

    @Test
    fun `negative or all-zero weights are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProfileRouteWeights(weightTime = -0.1, weightDistance = 0.5, weightManeuvers = 0.3, weightComplexity = 0.3)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ProfileRouteWeights(weightTime = 0.0, weightDistance = 0.0, weightManeuvers = 0.0, weightComplexity = 0.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RouteEvaluationConfig(
                shortStepPenaltyWeight = 0.0,
                sharpTurnPenaltyWeight = 0.0,
                turnDensityPenaltyWeight = 0.0,
            )
        }
    }

    @Test
    fun `focus must prioritize efficiency and tour must prioritize haptic simplicity`() {
        val inverted = ProfileRouteWeights(
            weightTime = 0.1,
            weightDistance = 0.1,
            weightManeuvers = 0.4,
            weightComplexity = 0.4,
        )
        assertThrows(IllegalArgumentException::class.java) {
            RouteEvaluationConfig(focusWeights = inverted)
        }
        val efficiencyHeavy = ProfileRouteWeights(
            weightTime = 0.5,
            weightDistance = 0.3,
            weightManeuvers = 0.1,
            weightComplexity = 0.1,
        )
        assertThrows(IllegalArgumentException::class.java) {
            RouteEvaluationConfig(tourWeights = efficiencyHeavy)
        }
    }
}
