package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.intelligence.PoiCandidate
import com.xanderjs.haptimap.core.intelligence.PoiDecision
import com.xanderjs.haptimap.core.intelligence.PoiEvaluation
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteResult
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.offset
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationProgress
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.network.ApiCallBudget
import java.io.File
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TourDetourControllerTest {

    @get:Rule val folder = TemporaryFolder()

    private val here = GeoPoint(40.0, -3.0)
    private val destPoint = here.offset(90.0, 1000.0)
    private val dest = Place("dest", "Destino", destPoint, null, null, 0)
    private val poiPoint = here.offset(90.0, 500.0).offset(0.0, 60.0) // 60 m al norte de la ruta (fuera de ella)
    private val nearPoint = here.offset(90.0, 400.0) // sobre la ruta este-oeste
    private val farPoint = here.offset(0.0, 500.0) // 500 m al norte: lejos de la ruta

    private val poi = PoiEvaluation(
        candidate = PoiCandidate("poi", "Café", ExplorationCategory.CAFE_RESTAURANT, poiPoint),
        score = 1.0, detourMeters = 120.0, addedTime = Duration.ZERO, decision = PoiDecision.SUGGEST, discardReason = null,
    )
    private fun route(points: List<GeoPoint>) = Route(
        dest, 0, Duration.ZERO, points,
        listOf(RouteStep(Maneuver.ARRIVAL, 0, Duration.ZERO, listOf(points.last()), points.last(), points.last(), null, null)),
    )
    private val original = route(listOf(here, destPoint))
    private val detour = route(listOf(here, poiPoint, destPoint))
    private val freshRoute = route(listOf(farPoint, destPoint))

    private fun progressAt(position: GeoPoint?) = NavigationProgress(
        stepIndex = 0,
        nextManeuver = RouteStep(Maneuver.CONTINUE, 0, Duration.ZERO, emptyList(), here, here, null, null),
        distanceToManeuverMeters = 100.0, remainingMeters = 500.0, remainingTime = Duration.ZERO,
        crossTrackMeters = 0.0, fixAccuracyMeters = 5f, weakSignal = false, position = position,
    )

    private inner class Fixture(
        budget: ApiCallBudget = ApiCallBudget.UNLIMITED,
        session: NavigationSession = NavigationSession.NavigationActive(original, NavigationProfile.TOUR),
    ) {
        val sessions = MutableStateFlow(session)
        val progress = MutableStateFlow<NavigationProgress?>(progressAt(here))
        var detourResult: RouteResult = RouteResult.Success(detour)
        var cancelResult: RouteResult = RouteResult.Success(freshRoute)
        val computeCalls = mutableListOf<Triple<GeoPoint, Place, List<Place>>>()
        val dispatched = mutableListOf<NavigationEvent>()
        val haptics = mutableListOf<HapticEvent>()
        val store = DetourStore(folder.root.resolve("detour.json"))
        val controller = TourDetourController(
            sessions = sessions,
            progress = progress,
            computeRoute = { o, d, i -> computeCalls += Triple(o, d, i); if (i.isEmpty()) cancelResult else detourResult },
            dispatch = { e -> dispatched += e; if (e is NavigationEvent.FollowRoute) sessions.value = NavigationSession.NavigationActive(e.route, NavigationProfile.TOUR) },
            playHaptic = { haptics += it },
            budget = budget,
            store = store,
        )
        val followed get() = dispatched.filterIsInstance<NavigationEvent.FollowRoute>().map { it.route }
    }

    private fun TestScope.start(f: Fixture) {
        backgroundScope.launch { f.controller.run() }
        runCurrent()
    }

    @Test
    fun `accept requests one route via the POI, navigates it and goes to ToPoi`() = runTest {
        val f = Fixture()
        start(f)
        f.controller.accept(poi)
        runCurrent()
        assertEquals(1, f.computeCalls.size)
        assertEquals(listOf("poi"), f.computeCalls[0].third.map { it.id }) // el POI como parada intermedia
        assertEquals(dest, f.computeCalls[0].second)
        assertEquals(listOf(detour), f.followed) // navega el desvío
        assertTrue(f.controller.state.value is TourDetour.ToPoi)
    }

    @Test
    fun `reaching the POI fires its haptic, then continues on its own to returning`() = runTest {
        val f = Fixture()
        start(f)
        f.controller.accept(poi); runCurrent()

        f.progress.value = progressAt(poiPoint) // se alcanza el POI
        runCurrent()
        assertEquals(listOf(HapticEvent.POI_ARRIVAL), f.haptics)
        assertTrue(f.controller.state.value is TourDetour.PoiReached)

        advanceTimeBy(TourDetourController.POI_PAUSE.inWholeMilliseconds + 100); runCurrent()
        assertTrue(f.controller.state.value is TourDetour.Returning)
    }

    @Test
    fun `arriving at the destination ends the detour`() = runTest {
        val f = Fixture()
        start(f)
        f.controller.accept(poi); runCurrent()
        f.progress.value = progressAt(poiPoint); runCurrent()
        advanceTimeBy(TourDetourController.POI_PAUSE.inWholeMilliseconds + 100); runCurrent() // -> Returning

        // la ruta del desvío se acaba: llegada (sin ruta en curso)
        f.sessions.value = NavigationSession.Arrived(detour, NavigationProfile.TOUR)
        runCurrent()
        assertEquals(TourDetour.Inactive, f.controller.state.value)
    }

    @Test
    fun `cancel near the original route reuses it with no new call`() = runTest {
        val f = Fixture()
        start(f)
        f.controller.accept(poi); runCurrent()
        f.progress.value = progressAt(nearPoint) // cerca de la ruta original
        f.controller.cancel(); runCurrent()

        assertEquals("ninguna llamada nueva", 1, f.computeCalls.size) // solo la del accept
        assertEquals(original, f.followed.last()) // repone la ruta cacheada
        assertEquals(TourDetour.Inactive, f.controller.state.value)
    }

    @Test
    fun `cancel far from the original route requests one new route to the destination`() = runTest {
        val f = Fixture()
        start(f)
        f.controller.accept(poi); runCurrent()
        f.progress.value = progressAt(farPoint) // lejos de la ruta original
        f.controller.cancel(); runCurrent()

        assertEquals("una llamada nueva", 2, f.computeCalls.size)
        assertEquals(emptyList<String>(), f.computeCalls[1].third.map { it.id }) // sin parada intermedia
        assertEquals(freshRoute, f.followed.last())
        assertEquals(TourDetour.Inactive, f.controller.state.value)
    }

    @Test
    fun `with no budget the detour never starts`() = runTest {
        val f = Fixture(budget = ApiCallBudget(maxPerTrip = 0))
        start(f)
        f.controller.accept(poi); runCurrent()
        assertTrue(f.computeCalls.isEmpty())
        assertTrue(f.followed.isEmpty())
        assertEquals(TourDetour.Inactive, f.controller.state.value)
    }

    @Test
    fun `restores an in-progress detour after process death`() = runTest {
        // se guarda un desvío en curso (como antes de morir el proceso) y la ruta del desvío ya está activa al reanudar
        DetourStore(folder.root.resolve("detour.json")).save(TourDetour.ToPoi(poi, original, detour))
        val f = Fixture(session = NavigationSession.NavigationActive(detour, NavigationProfile.TOUR))
        start(f)
        runCurrent()
        val state = f.controller.state.value
        assertTrue("restaura el desvío", state is TourDetour.ToPoi)
        state as TourDetour.ToPoi
        assertEquals("Café", state.poi.candidate.name)
        assertEquals(detour, state.detourRoute)
        assertEquals(dest, state.originalRoute.destination)
    }
}
