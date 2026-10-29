package com.xanderjs.haptimap.presentation.debug

import com.xanderjs.haptimap.core.location.simulatedWalk
import com.xanderjs.haptimap.core.location.withDetour
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.MeasuredPath
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.RouteResult
import com.xanderjs.haptimap.core.model.Transport
import com.xanderjs.haptimap.core.model.distanceTo
import com.xanderjs.haptimap.core.navigation.NavigationCue
import com.xanderjs.haptimap.core.navigation.NavigationEngine
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.RouteRecalculator
import com.xanderjs.haptimap.core.navigation.RouteTracker
import com.xanderjs.haptimap.core.network.FakeRoutesRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Paso 08: los modos de desvío del simulador, con su configuración real, por el motor y el recálculo
// en tiempo virtual. Comprueba que "Salir y volver" y "Volver tarde" caen en su ventana.
@OptIn(ExperimentalCoroutinesApi::class)
class OffRouteSimulationTest {

    private val route = sampleRoute("en")!!
    private val store = NavigationSessionStore()
    private val engine = NavigationEngine(store)
    private val connectivity = MutableStateFlow<ConnectivityStatus>(ConnectivityStatus.Connected(Transport.WIFI))
    private val cues = mutableListOf<NavigationCue>()
    private val states = mutableListOf<NavigationSession>()
    private val requestTimes = mutableListOf<Long>()

    /** Anda el modo sobre la ruta de ejemplo durante [duration] con la petición falsa de debug (sin clave). */
    private fun TestScope.simulate(mode: SimulationMode, duration: Duration = 2.minutes) {
        val noKey = debugRoutes(FakeRoutesRepository(RouteResult.Failure(RouteError.MISSING_KEY), hasApiKey = false)) { "en" }
        val counted: suspend (GeoPoint, Place) -> RouteResult = { origin, destination ->
            requestTimes += currentTime
            noKey.computeRoute(origin, destination)
        }
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        backgroundScope.launch(dispatcher) { engine.cues.toList(cues) }
        backgroundScope.launch(dispatcher) { store.state.toList(states) }
        backgroundScope.launch(dispatcher) { engine.follow(simulatedWalk(route.points.withDetour(mode.detour!!), mode.walk!!, nowMillis = { currentTime })) }
        backgroundScope.launch(dispatcher) { RouteRecalculator(store, engine).follow(counted, connectivity, timeSource = testScheduler.timeSource) }
        store.dispatch(NavigationEvent.RestoreRoute(route))
        store.dispatch(NavigationEvent.StartNavigation)
        advanceTimeBy(duration)
    }

    private fun timeOf(cue: NavigationCue): Long = cueTimes.getValue(cue)
    private val cueTimes = mutableMapOf<NavigationCue, Long>()

    private fun TestScope.timeCues() = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        engine.cues.collect { cueTimes.putIfAbsent(it, currentTime) }
    }

    @Test
    fun `leave and return rejoins before the request, with no request at all`() = runTest {
        timeCues()
        simulate(SimulationMode.LEAVE_AND_RETURN)
        assertEquals(NavigationCue.OffRoute, cues.first())
        assertEquals(1, cues.count { it == NavigationCue.BackOnRoute })
        assertEquals(emptyList<Long>(), requestTimes)
        // vuelve antes de los 10 s de espera
        assertTrue(timeOf(NavigationCue.BackOnRoute) - timeOf(NavigationCue.OffRoute) < 10_000)
        assertTrue(states.none { it is NavigationSession.Recalculating })
    }

    @Test
    fun `return late rejoins with the request in flight and the late route is ignored`() = runTest {
        timeCues()
        simulate(SimulationMode.RETURN_LATE)
        assertEquals(1, requestTimes.size)
        val rejoined = timeOf(NavigationCue.BackOnRoute)
        // la petición falsa tarda 2 s: vuelve con ella en curso
        assertTrue("request at ${requestTimes[0]}, rejoined at $rejoined", rejoined - requestTimes[0] in 1..1_999)
        assertEquals(1, cues.count { it == NavigationCue.BackOnRoute })
        assertTrue(NavigationCue.Rerouted !in cues)
        // Recalculating → NavigationActive con la ruta de siempre; el RouteRecalculated tardío no cambia nada
        assertEquals(NavigationSession.NavigationActive(route), states.last { it !is NavigationSession.Arrived })
        assertTrue(states.none { it is NavigationSession.NavigationActive && it.route != route })
    }

    @Test
    fun `leave route reroutes once to a route from where the walker is`() = runTest {
        // hasta justo después del recálculo: aquí el paseo no vuelve a empezar en la ruta nueva, como en la app
        simulate(SimulationMode.LEAVE_ROUTE, 80.seconds)
        assertEquals(listOf(NavigationCue.OffRoute, NavigationCue.Rerouted), cues.filter { it !is NavigationCue.ManeuverApproaching })
        assertEquals(1, requestTimes.size)
        val rerouted = (store.state.value as NavigationSession.NavigationActive).route
        assertEquals(route.destination, rerouted.destination)
        assertTrue(rerouted.points.last().distanceTo(route.destination.location) < 1)
    }

    @Test
    fun `the fake reroute is a walkable route that arrives`() {
        val origin = MeasuredPath(route.points).pointAt(60.0).let { GeoPoint(it.latitude + 0.0005, it.longitude) }
        val fake = debugReroute(origin, route.destination, "es")
        assertEquals(origin, fake.points.first())
        assertEquals(Maneuver.ARRIVAL, fake.steps.last().maneuver)
        assertTrue(fake.steps.any { it.maneuver == Maneuver.TURN_LEFT || it.maneuver == Maneuver.TURN_RIGHT })
        assertTrue(fake.steps.first().instruction!!.startsWith("Ve hacia"))
        val path = MeasuredPath(fake.points)
        val tracker = RouteTracker(fake)
        val updates = (0..path.length.toInt() / 2).map { tracker.update(LocationFix(path.pointAt(it * 2.0), 5f, null, null, it * 1000L)) }
        assertTrue(updates.flatMap { it.cues }.contains(NavigationCue.Arrived))
    }
}
