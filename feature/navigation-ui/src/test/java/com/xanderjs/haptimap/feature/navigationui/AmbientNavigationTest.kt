package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmbientNavigationTest {

    private fun navigating(meters: Int, notice: NavigationNotice? = null, instruction: String? = "Turn left", remaining: Int = 900) =
        NavigationUiState.Navigating(GuidanceUi(ArrowDirection.LEFT, meters, instruction, remaining, 10.minutes), notice)

    @Test
    fun `navigating keeps the arrow, the notice and an approximate distance`() {
        assertEquals(
            AmbientNavigationUi.Guidance(ArrowDirection.LEFT, 50, NavigationNotice.OFFLINE),
            ambientNavigationUi(navigating(47, NavigationNotice.OFFLINE)),
        )
    }

    @Test
    fun `fixes inside the same step give the same value, so ambient does not redraw`() {
        val a = ambientNavigationUi(navigating(312, instruction = "Turn left", remaining = 900).copy(confirmingStop = true))
        val b = ambientNavigationUi(navigating(288, instruction = "Turn left", remaining = 870))
        assertEquals(a, b)
    }

    @Test
    fun `a maneuver change is a new value`() {
        val turn = ambientNavigationUi(navigating(20))
        val next = ambientNavigationUi(
            NavigationUiState.Navigating(GuidanceUi(ArrowDirection.RIGHT, 20, null, 500, 5.minutes), null),
        )
        assertEquals(AmbientNavigationUi.Guidance(ArrowDirection.RIGHT, 20, null), next)
        assertNotEquals(turn, next)
    }

    @Test
    fun `off route, recalculating, offline and failed keep their status but not the distance to the route`() {
        OffRouteStatus.entries.forEach { status ->
            assertEquals(
                AmbientNavigationUi.OffRoute(status, NavigationNotice.WEAK_GPS),
                ambientNavigationUi(NavigationUiState.OffRoute(status, metersFromRoute = 32, notice = NavigationNotice.WEAK_GPS)),
            )
        }
        assertEquals(
            ambientNavigationUi(NavigationUiState.OffRoute(OffRouteStatus.HEAD_BACK, 32, null)),
            ambientNavigationUi(NavigationUiState.OffRoute(OffRouteStatus.HEAD_BACK, 41, null)),
        )
    }

    @Test
    fun `arrival and ended`() {
        assertEquals(AmbientNavigationUi.Arrived, ambientNavigationUi(NavigationUiState.Arrived("Museo del Prado")))
        assertNull(ambientNavigationUi(NavigationUiState.Ended))
    }

    @Test
    fun `approximate distance steps`() {
        val cases = mapOf(
            0 to 0, 4 to 0, 5 to 10, 47 to 50, 94 to 90, 95 to 100, 99 to 100,
            100 to 100, 124 to 100, 125 to 150, 474 to 450, 499 to 500,
            500 to 500, 549 to 500, 550 to 600, 949 to 900, 950 to 1000,
            1000 to 1000, 1249 to 1000, 1250 to 1500, 12_400 to 12_500,
        )
        cases.forEach { (meters, expected) -> assertEquals("$meters m", expected, approximateMeters(meters)) }
    }

}
