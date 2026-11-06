package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver.CONTINUE
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.RouteResult
import com.xanderjs.haptimap.core.model.Transport
import com.xanderjs.haptimap.core.model.offset
import com.xanderjs.haptimap.core.navigation.NavigationEvent as E
import com.xanderjs.haptimap.core.navigation.NavigationSession as S
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// Paso 08: la política de recálculo con tiempo virtual. Con los umbrales por defecto, un usuario 40 m
// fuera desde t = 0 se declara desviado a los 5 s y la primera petición sale a los 15 s (10 s de espera).
@OptIn(ExperimentalCoroutinesApi::class)
class RouteRecalculatorTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val store = NavigationSessionStore()
    private val engine = NavigationEngine(store)
    private val recalculator = RouteRecalculator(store, engine)
    private val connectivity = MutableStateFlow<ConnectivityStatus>(ConnectivityStatus.Connected(Transport.WIFI))
    private val route = routeOf(Leg(CONTINUE, 90.0, 200.0))
    private val away = ORIGIN.offset(0.0, 40.0)

    /** Dónde está el usuario: el GPS da un fix bueno ahí cada segundo mientras [gps]. */
    private var here = away
    private var gps = true

    private val fixes = flow {
        var millis = 0L
        while (true) {
            if (gps) emit(fix(here, millis))
            delay(1.seconds)
            millis += 1000
        }
    }

    private val rerouted = routeFrom(away, Leg(CONTINUE, 90.0, 120.0))

    /** Las peticiones: en qué segundo, desde dónde y hacia qué destino. */
    private val requests = mutableListOf<Triple<Long, GeoPoint, Place>>()
    private var results: (Int) -> RouteResult = { RouteResult.Success(rerouted) }
    private var latency = Duration.ZERO
    private var cancelled = 0

    private suspend fun compute(origin: GeoPoint, destination: Place, now: () -> Long): RouteResult {
        requests += Triple(now() / 1000, origin, destination)
        val result = results(requests.size)
        try {
            delay(latency)
        } catch (e: CancellationException) {
            cancelled++
            throw e
        }
        return result
    }

    private fun TestScope.start(
        thresholds: NavigationThresholds = NavigationThresholds(),
        computeRoute: suspend (GeoPoint, Place) -> RouteResult = { o, d -> compute(o, d) { currentTime } },
    ) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.follow(fixes) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            recalculator.follow(computeRoute, connectivity, thresholds, testScheduler.timeSource)
        }
        navigate()
    }

    private fun navigate() {
        store.dispatch(E.RestoreRoute(route))
        store.dispatch(E.StartNavigation)
    }

    private val requestSeconds get() = requests.map { it.first }

    @Test
    fun `asks once 10 s after leaving, from the last good fix to the same destination`() = runTest {
        val file = folder.root.resolve("active_route.json")
        val cache = RouteCache(file)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { cache.follow(store.state) }
        start()
        advanceTimeBy(5.seconds + 1.milliseconds)
        assertEquals(S.OffRoute(route), store.state.value)
        advanceTimeBy(9.seconds)
        assertEquals(emptyList<Long>(), requestSeconds)
        advanceTimeBy(1.seconds)
        assertEquals(listOf(Triple(15L, away, route.destination)), requests)
        assertEquals(S.NavigationActive(rerouted), store.state.value)
        // la caché guarda la polilínea con precisión 1e5: se compara lo que no se redondea
        assertEquals(rerouted.distanceMeters to rerouted.points.size, cache.load()!!.route.let { it.distanceMeters to it.points.size })
    }

    @Test
    fun `failures back off at 15, 30 and 60 s and stop after 4 automatic requests`() = runTest {
        results = { RouteResult.Failure(RouteError.API_ERROR) }
        start()
        advanceTimeBy(10.minutes)
        assertEquals(listOf(15L, 30L, 60L, 120L), requestSeconds)
        assertEquals(S.OffRoute(route, SessionError.ROUTE_UNAVAILABLE), store.state.value)
    }

    @Test
    fun `retry right after a failure waits for the 15 s gap`() = runTest {
        results = { if (it == 1) RouteResult.Failure(RouteError.NO_ROUTE) else RouteResult.Success(rerouted) }
        start()
        advanceTimeBy(16.seconds)
        store.dispatch(E.Recalculate)
        assertEquals(S.Recalculating(route), store.state.value)
        advanceTimeBy(13.seconds)
        assertEquals(listOf(15L), requestSeconds)
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(listOf(15L, 30L), requestSeconds)
        assertEquals(S.NavigationActive(rerouted), store.state.value)
    }

    @Test
    fun `missing or invalid key and quota stop automatic requests but retry still asks`() = runTest {
        start()
        for (error in listOf(RouteError.MISSING_KEY, RouteError.API_INVALID_KEY, RouteError.API_QUOTA)) {
            results = { RouteResult.Failure(error) }
            requests.clear()
            store.dispatch(E.Cancel)
            val base = currentTime / 1000
            navigate()
            advanceTimeBy(5.minutes)
            assertEquals("$error", listOf(base + 15), requestSeconds)
            assertEquals(S.OffRoute(route, SessionError.ROUTE_UNAVAILABLE), store.state.value)
            store.dispatch(E.Recalculate)
            runCurrent()
            assertEquals(2, requests.size)
            assertEquals(route, store.state.value.activeRoute)
        }
    }

    @Test
    fun `offline it stays off route with the old route and asks as soon as it reconnects`() = runTest {
        connectivity.value = ConnectivityStatus.Disconnected
        start()
        advanceTimeBy(3.minutes)
        assertEquals(emptyList<Long>(), requestSeconds)
        assertEquals(S.OffRoute(route), store.state.value)
        connectivity.value = ConnectivityStatus.Connected(Transport.CELLULAR)
        runCurrent()
        assertEquals(listOf(180L), requestSeconds)
        assertEquals(S.NavigationActive(rerouted), store.state.value)
    }

    @Test
    fun `retry while offline is not sent and the automatic request still comes on reconnect`() = runTest {
        connectivity.value = ConnectivityStatus.Disconnected
        start()
        advanceTimeBy(20.seconds)
        store.dispatch(E.Recalculate)
        runCurrent()
        assertEquals(S.OffRoute(route, SessionError.NO_CONNECTION), store.state.value)
        advanceTimeBy(30.seconds)
        connectivity.value = ConnectivityStatus.Connected(Transport.WIFI)
        runCurrent()
        assertEquals(1, requests.size)
        assertEquals(S.NavigationActive(rerouted), store.state.value)
    }

    @Test
    fun `no request while the GPS is weak`() = runTest {
        // espera de 20 s: la señal débil (10 s sin fix desde los 6 s) llega antes de que acabe
        start(NavigationThresholds(recalculationDelay = 20.seconds))
        advanceTimeBy(7.seconds)
        gps = false
        advanceTimeBy(60.seconds)
        assertEquals(emptyList<Long>(), requestSeconds)
        gps = true
        advanceTimeBy(1.seconds)
        assertEquals(1, requests.size)
    }

    @Test
    fun `rejoining before the delay ends sends no request`() = runTest {
        val cues = mutableListOf<NavigationCue>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.cues.toList(cues) }
        start()
        advanceTimeBy(8.seconds)
        here = ORIGIN
        advanceTimeBy(2.minutes)
        assertEquals(emptyList<Long>(), requestSeconds)
        assertEquals(S.NavigationActive(route), store.state.value)
        assertEquals(listOf(NavigationCue.OffRoute, NavigationCue.BackOnRoute), cues)
    }

    @Test
    fun `rejoining late cancels the request in flight and a late result is ignored`() = runTest {
        // como el modo "volver tarde" del simulador: vuelve con la petición de 2 s en curso
        val cues = mutableListOf<NavigationCue>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.cues.toList(cues) }
        latency = 3.seconds
        start()
        advanceTimeBy(15.seconds + 1.milliseconds)
        assertEquals(S.Recalculating(route), store.state.value)
        here = ORIGIN
        advanceTimeBy(2.seconds)
        assertEquals(S.NavigationActive(route), store.state.value)
        assertEquals(1, cancelled)
        advanceTimeBy(1.minutes)
        assertEquals(S.NavigationActive(route), store.state.value)
        assertEquals(1, requests.size)
        assertEquals(listOf(NavigationCue.OffRoute, NavigationCue.BackOnRoute), cues)
    }

    @Test
    fun `a response that could not be cancelled arrives after rejoining and is ignored`() = runTest {
        val cues = mutableListOf<NavigationCue>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.cues.toList(cues) }
        start { origin, destination ->
            withContext(NonCancellable) {
                requests += Triple(currentTime / 1000, origin, destination)
                delay(3.seconds)
            }
            RouteResult.Success(rerouted)
        }
        advanceTimeBy(15.seconds + 1.milliseconds)
        here = ORIGIN
        advanceTimeBy(1.minutes)
        assertEquals(S.NavigationActive(route), store.state.value)
        assertTrue(NavigationCue.Rerouted !in cues)
        assertEquals(listOf(NavigationCue.OffRoute, NavigationCue.BackOnRoute), cues)
    }

    @Test
    fun `cues of a rerouted episode come once and in order`() = runTest {
        val cues = mutableListOf<NavigationCue>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.cues.toList(cues) }
        start()
        advanceTimeBy(1.minutes)
        assertEquals(listOf(NavigationCue.OffRoute, NavigationCue.Rerouted), cues)
    }
}
