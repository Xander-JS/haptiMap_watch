package com.xanderjs.haptimap.core.haptics

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.HapticEvent.ARRIVAL
import com.xanderjs.haptimap.core.model.HapticEvent.BACK_ON_ROUTE
import com.xanderjs.haptimap.core.model.HapticEvent.OFF_ROUTE
import com.xanderjs.haptimap.core.model.HapticEvent.TURN_IMMINENT
import com.xanderjs.haptimap.core.model.HapticEvent.TURN_LEFT
import com.xanderjs.haptimap.core.model.HapticEvent.TURN_RIGHT
import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Maneuver.CONTINUE
import com.xanderjs.haptimap.core.model.MeasuredPath
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.offset
import com.xanderjs.haptimap.core.navigation.NavigationEngine
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.navigatingRoute
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Un paseo simulado entero por el Navigation Engine real, cableado como en :app: giro a la izquierda, un desvío
 * y vuelta, giro a la derecha, otro desvío que acaba en recálculo, y la ruta nueva hasta la llegada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HapticWalkTest {

    private val store = NavigationSessionStore()
    private val navigation = NavigationEngine(store)

    // 60 m al este, izquierda 100 m al norte, derecha 80 m al este
    private val route = routeFrom(ORIGIN, Leg(CONTINUE, 90.0, 60.0), Leg(Maneuver.TURN_LEFT, 0.0, 100.0), Leg(Maneuver.TURN_RIGHT, 90.0, 80.0))
    private var millis = 0L

    private fun fix(point: GeoPoint) = LocationFix(point, 5f, SPEED.toFloat(), null, (++millis) * 1000)

    private fun along(route: Route, from: Double, to: Double): List<LocationFix> {
        val path = MeasuredPath(route.points)
        return (0..((to - from) / SPEED).toInt()).map { fix(path.pointAt(from + it * SPEED)) }
    }

    /** Sale de la ruta en [at] metros hacia [bearing] durante 28 fixes (39 m) y, si [back], vuelve. */
    private fun detour(at: Double, bearing: Double, back: Boolean): List<LocationFix> {
        val base = MeasuredPath(route.points).pointAt(at)
        val out = (1..28).map { fix(base.offset(bearing, it * SPEED)) }
        return if (back) out + (1..28).map { fix(base.offset(bearing, (28 - it) * SPEED)) } else out
    }

    private lateinit var rerouted: Route

    @Test
    fun `a full walk with a detour and a reroute plays the exact haptic sequence`() = runTest {
        val player = FakeHapticPlayer { currentTime }
        val haptics = HapticEngine(player)
        val adapter = HapticCues(haptics::playCount)
        // un fix por segundo de la ruta que se navega; el motor vuelve a recolectar con cada ruta nueva
        val fixes = flow {
            val plan = when (store.state.value.navigatingRoute) {
                route -> along(route, 0.0, 110.0) + detour(110.0, 270.0, back = true) + along(route, 110.0, 190.0) +
                    detour(190.0, 0.0, back = false)
                else -> along(rerouted, 0.0, MeasuredPath(rerouted.points).length)
            }
            plan.forEach {
                delay(1.seconds)
                emit(it)
            }
            awaitCancellation()
        }
        val unconfined = UnconfinedTestDispatcher(testScheduler)
        backgroundScope.launch(unconfined) { navigation.follow(fixes) }
        backgroundScope.launch(unconfined) { haptics.follow(navigation.cues.mapNotNull(adapter::eventFor), store.state.map { it.hapticsActive }) }

        store.dispatch(NavigationEvent.RestoreRoute(route))
        store.dispatch(NavigationEvent.StartNavigation)
        advanceTimeBy(260.seconds)
        assertEquals(NavigationSession.OffRoute(route), store.state.value)

        // el RouteRecalculator, a mano: una ruta nueva desde donde está
        rerouted = routeFrom(MeasuredPath(route.points).pointAt(190.0).offset(0.0, 28 * SPEED), Leg(CONTINUE, 0.0, 30.0), Leg(Maneuver.TURN_LEFT, 270.0, 40.0))
        store.dispatch(NavigationEvent.Recalculate)
        store.dispatch(NavigationEvent.RouteRecalculated(rerouted))
        advanceTimeBy(70.seconds)

        assertEquals(NavigationSession.Arrived(rerouted), store.state.value)
        assertEquals(
            listOf(
                TURN_LEFT, TURN_IMMINENT,
                OFF_ROUTE, BACK_ON_ROUTE,
                TURN_RIGHT, TURN_IMMINENT,
                OFF_ROUTE, BACK_ON_ROUTE, // el segundo, por Rerouted
                TURN_LEFT, TURN_IMMINENT,
                ARRIVAL,
            ),
            player.events,
        )
    }

    private data class Leg(val maneuver: Maneuver, val bearing: Double, val meters: Double)

    // shortcut: copia mínima de routeFrom de core/navigation/src/test/.../Replay.kt; si crece, pasarla a testFixtures
    private fun routeFrom(origin: GeoPoint, vararg legs: Leg): Route {
        val points = mutableListOf(origin)
        val steps = mutableListOf<RouteStep>()
        var start = origin
        for (leg in legs) {
            val pieces = ceil(leg.meters / 10).toInt()
            val stepPoints = (0..pieces).map { start.offset(leg.bearing, leg.meters * it / pieces) }
            points += stepPoints.drop(1)
            steps += RouteStep(leg.maneuver, leg.meters.toInt(), (leg.meters / SPEED).seconds, stepPoints, start, stepPoints.last(), "${leg.maneuver}", null)
            start = stepPoints.last()
        }
        steps += RouteStep(Maneuver.ARRIVAL, 0, Duration.ZERO, listOf(start), start, start, null, null)
        val total = legs.sumOf { it.meters }
        return Route(Place("dest", "Destination", start, null, null, total.toInt()), total.toInt(), (total / SPEED).seconds, points, steps)
    }

    private companion object {
        val ORIGIN = GeoPoint(40.4168, -3.7038)
        const val SPEED = 1.4
    }
}
