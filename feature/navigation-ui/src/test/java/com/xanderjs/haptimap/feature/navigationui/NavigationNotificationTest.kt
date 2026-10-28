package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.xanderjs.haptimap.designsystem.R as DsR

// Paso 10: el contenido de la notificación del servicio y cuándo se reconstruye
class NavigationNotificationTest {

    private fun navigating(direction: ArrowDirection, meters: Int) =
        NavigationUiState.Navigating(GuidanceUi(direction, meters, "Turn left onto Calle Mayor", 900, 12.minutes), notice = null)

    @Test
    fun `navigating shows the maneuver icon, its label and the distance to it`() {
        ArrowDirection.entries.forEach { direction ->
            assertEquals(
                NavigationNotificationContent(direction.icon, direction.description, NotificationDetail.Distance(120)),
                navigating(direction, 120).toNotificationContent(),
            )
        }
    }

    @Test
    fun `off route shows the off-route icon, the same status line as the screen and the distance from the route`() {
        val expected = mapOf(
            OffRouteStatus.RECALCULATING to R.string.navigation_rerouting,
            OffRouteStatus.OFFLINE to R.string.navigation_reroute_offline,
            OffRouteStatus.FAILED to R.string.navigation_reroute_failed,
            OffRouteStatus.HEAD_BACK to R.string.navigation_head_back,
        )
        expected.forEach { (status, title) ->
            assertEquals(
                NavigationNotificationContent(DsR.drawable.ic_off_route, title, NotificationDetail.FromRoute(40)),
                NavigationUiState.OffRoute(status, metersFromRoute = 40, notice = null).toNotificationContent(),
            )
        }
        assertNull(NavigationUiState.OffRoute(OffRouteStatus.HEAD_BACK, metersFromRoute = null, notice = null).toNotificationContent()!!.detail)
    }

    @Test
    fun `arrived shows the arrival icon and the destination, ended shows nothing`() {
        assertEquals(
            NavigationNotificationContent(DsR.drawable.ic_arrive, R.string.navigation_arrived, NotificationDetail.Destination("Museo del Prado")),
            NavigationUiState.Arrived("Museo del Prado").toNotificationContent(),
        )
        assertNull(NavigationUiState.Ended.toNotificationContent())
    }

    @Test
    fun `the stop dialog and notices don't change the notification`() {
        val plain = navigating(ArrowDirection.LEFT, 80)
        assertEquals(plain.toNotificationContent(), plain.copy(confirmingStop = true, notice = NavigationNotice.WEAK_GPS).toNotificationContent())
    }

    @Test
    fun `the first content and every headline change post at once`() {
        val throttle = NotificationThrottle()
        assertTrue(throttle.shouldPost(navigating(ArrowDirection.STRAIGHT, 120).toNotificationContent()!!, 0))
        assertTrue(throttle.shouldPost(navigating(ArrowDirection.LEFT, 18).toNotificationContent()!!, 100))
        assertTrue(throttle.shouldPost(NavigationUiState.OffRoute(OffRouteStatus.HEAD_BACK, 25, null).toNotificationContent()!!, 200))
        assertTrue(throttle.shouldPost(NavigationUiState.OffRoute(OffRouteStatus.RECALCULATING, 25, null).toNotificationContent()!!, 300))
        assertTrue(throttle.shouldPost(NavigationUiState.Arrived("Prado").toNotificationContent()!!, 400))
        assertTrue(throttle.shouldPost(NavigationUiState.Arrived("Sol").toNotificationContent()!!, 500))
    }

    @Test
    fun `distance-only changes post at most every interval, identical content never`() {
        val throttle = NotificationThrottle(5.seconds)
        fun post(meters: Int, seconds: Int) = throttle.shouldPost(navigating(ArrowDirection.LEFT, meters).toNotificationContent()!!, seconds * 1000L)
        assertTrue(post(100, 0))
        assertFalse(post(100, 10))
        assertFalse(post(99, 1))
        assertFalse(post(95, 4))
        assertTrue(post(93, 5))
        assertFalse(post(92, 6))
        assertTrue(post(85, 10))
        assertFalse(post(85, 20))
    }

    @Test
    fun `distance from the route is throttled the same way`() {
        val throttle = NotificationThrottle(5.seconds)
        fun post(meters: Int?, seconds: Int) =
            throttle.shouldPost(NavigationUiState.OffRoute(OffRouteStatus.HEAD_BACK, meters, null).toNotificationContent()!!, seconds * 1000L)
        assertTrue(post(30, 0))
        assertFalse(post(32, 2))
        assertTrue(post(35, 5))
        // perder la distancia (sin fix) cambia lo que se ve: sale al momento
        assertTrue(post(null, 6))
    }
}
