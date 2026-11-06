package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.DetourTolerance
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.Transport
import com.xanderjs.haptimap.core.model.UserSettings
import com.xanderjs.haptimap.core.model.distanceTo
import com.xanderjs.haptimap.core.model.offset
import com.xanderjs.haptimap.core.intelligence.toPoiEvaluationConfig
import com.xanderjs.haptimap.core.navigation.NavigationProgress
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.network.ApiCallBudget
import com.xanderjs.haptimap.core.network.ApiKind
import com.xanderjs.haptimap.core.network.LatLng
import com.xanderjs.haptimap.core.network.LocalizedText
import com.xanderjs.haptimap.core.network.PlaceDto
import com.xanderjs.haptimap.core.network.PoiFetch
import com.xanderjs.haptimap.core.network.PoiSource
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TourPoiPipelineTest {

    private val start = GeoPoint(40.0, -3.0)
    private val end = start.offset(bearingDegrees = 90.0, meters = 400.0)
    private val route = Route(
        destination = Place("d", "Destino", end, null, null, 0),
        distanceMeters = start.distanceTo(end).toInt(),
        duration = Duration.ZERO,
        points = listOf(start, end),
        steps = listOf(RouteStep(Maneuver.ARRIVAL, 0, Duration.ZERO, emptyList(), start, end, null, null)),
    )
    private val pathLength = start.distanceTo(end)

    // los POIs de prueba van sobre la ruta, 200 m por delante: siguen cerca (y SUGGEST) en todas las consultas
    private val onRouteAhead = start.offset(bearingDegrees = 90.0, meters = 200.0)

    private class FakePoiSource(var result: PoiFetch) : PoiSource {
        val calls = mutableListOf<Triple<GeoPoint, Double, String>>()
        override suspend fun searchAhead(center: GeoPoint, radiusMeters: Double, languageCode: String): PoiFetch {
            calls += Triple(center, radiusMeters, languageCode)
            return result
        }
    }

    private fun dto(id: String, type: String) =
        PlaceDto(id = id, displayName = LocalizedText(id), location = LatLng(onRouteAhead.latitude, onRouteAhead.longitude), primaryType = type)

    private fun progressAt(alongMeters: Double, stepIndex: Int = 0) = NavigationProgress(
        stepIndex = stepIndex,
        nextManeuver = route.steps[0],
        distanceToManeuverMeters = 0.0,
        remainingMeters = pathLength - alongMeters,
        remainingTime = Duration.ZERO,
        crossTrackMeters = null,
        fixAccuracyMeters = null,
        weakSignal = false,
        position = start,
    )

    private inner class Harness(val source: FakePoiSource, tour: Boolean = true) {
        val sessions = MutableStateFlow<NavigationSession>(
            NavigationSession.NavigationActive(route, if (tour) NavigationProfile.TOUR else NavigationProfile.FOCUS),
        )
        val progress = MutableStateFlow<NavigationProgress?>(null)
        val connectivity = MutableStateFlow<ConnectivityStatus>(ConnectivityStatus.Connected(Transport.WIFI))
        val settings = MutableStateFlow(UserSettings())
        val budget = ApiCallBudget()
    }

    private fun TestBody(tour: Boolean = true, result: PoiFetch = PoiFetch.Found(listOf()), block: suspend TestScope.(FakePoiSource, Harness, TourPoiPipeline) -> Unit) = runTest {
        val source = FakePoiSource(result)
        val h = Harness(source, tour)
        val pipeline = TourPoiPipeline(source, h.sessions, h.progress, h.connectivity, h.settings, h.budget, { "en" })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { pipeline.run() }
        advanceUntilIdle()
        block(source, h, pipeline)
    }

    @Test
    fun `the first advance on a Tour trip triggers one query ahead on the route`() = TestBody(result = PoiFetch.Found(listOf(dto("a", "museum")))) { source, h, pipeline ->
        h.progress.value = progressAt(0.0)
        advanceUntilIdle()
        assertEquals(1, source.calls.size)
        // el centro va por delante de la posición actual (look-ahead), no en el inicio
        assertTrue(source.calls[0].first.distanceTo(start) > 100.0)
        assertEquals("a", pipeline.suggestions.value.single().candidate.id)
    }

    @Test
    fun `small advances within the threshold do not query again`() = TestBody(result = PoiFetch.Found(listOf(dto("a", "museum")))) { source, h, _ ->
        h.progress.value = progressAt(0.0); advanceUntilIdle()
        h.progress.value = progressAt(50.0); advanceUntilIdle()
        h.progress.value = progressAt(100.0); advanceUntilIdle()
        assertEquals(1, source.calls.size) // solo la primera; 50 y 100 m no llegan al umbral de 120 m
    }

    @Test
    fun `advancing past the threshold queries again`() = TestBody(result = PoiFetch.Found(listOf(dto("a", "museum")))) { source, h, _ ->
        h.progress.value = progressAt(0.0); advanceUntilIdle()
        h.progress.value = progressAt(130.0); advanceUntilIdle()
        assertEquals(2, source.calls.size)
    }

    @Test
    fun `the radius comes from the detour tolerance in settings`() = TestBody(result = PoiFetch.Found(listOf(dto("a", "museum")))) { source, h, _ ->
        h.settings.value = UserSettings(detourTolerance = DetourTolerance.SHORT)
        h.progress.value = progressAt(0.0); advanceUntilIdle()
        assertEquals(DetourTolerance.SHORT.toPoiEvaluationConfig().maxDistanceFromRouteMeters, source.calls[0].second, 0.0)
    }

    @Test
    fun `results are cached and deduplicated by place id across queries`() = TestBody { source, h, pipeline ->
        source.result = PoiFetch.Found(listOf(dto("a", "museum"), dto("b", "park")))
        h.progress.value = progressAt(0.0); advanceUntilIdle()
        source.result = PoiFetch.Found(listOf(dto("b", "park"), dto("c", "monument")))
        h.progress.value = progressAt(130.0); advanceUntilIdle()
        // a, b, c -> tres distintas pese a repetir b
        assertEquals(setOf("a", "b", "c"), pipeline.suggestions.value.map { it.candidate.id }.toSet())
    }

    @Test
    fun `places whose type maps to no exploration category are discarded`() = TestBody { source, h, pipeline ->
        source.result = PoiFetch.Found(listOf(dto("museo", "art_museum"), dto("iglesia", "church")))
        h.progress.value = progressAt(0.0); advanceUntilIdle()
        assertEquals(listOf("museo"), pipeline.suggestions.value.map { it.candidate.id })
    }

    @Test
    fun `no query is made while disconnected, and it resumes once connected`() = TestBody(result = PoiFetch.Found(listOf(dto("a", "museum")))) { source, h, _ ->
        h.connectivity.value = ConnectivityStatus.Disconnected
        h.progress.value = progressAt(0.0); advanceUntilIdle()
        assertEquals(0, source.calls.size) // suspendido sin red
        h.connectivity.value = ConnectivityStatus.Connected(Transport.CELLULAR)
        advanceUntilIdle()
        assertEquals(1, source.calls.size) // reanuda al reconectar, sin avanzar más
    }

    @Test
    fun `FOCUS trips never query`() = TestBody(tour = false, result = PoiFetch.Found(listOf(dto("a", "museum")))) { source, h, _ ->
        h.progress.value = progressAt(0.0); advanceUntilIdle()
        h.progress.value = progressAt(200.0); advanceUntilIdle()
        assertEquals(0, source.calls.size)
    }

    @Test
    fun `starting a Tour trip resets the per-trip budget`() = TestBody(result = PoiFetch.Found(listOf(dto("a", "museum")))) { source, h, _ ->
        // termina el trayecto y agota el tope por trayecto
        h.sessions.value = NavigationSession.Idle; advanceUntilIdle()
        repeat(ApiCallBudget.DEFAULT_MAX_PER_TRIP) { h.budget.record(ApiKind.ROUTES) }
        // empieza un trayecto Tour nuevo: startTrip() reinicia el tope
        h.sessions.value = NavigationSession.NavigationActive(route, NavigationProfile.TOUR); advanceUntilIdle()
        h.progress.value = progressAt(0.0); advanceUntilIdle()
        assertEquals(1, source.calls.size)
    }
}
