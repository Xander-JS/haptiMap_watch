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
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.UserSettings
import com.xanderjs.haptimap.core.navigation.NavigationProgress
import com.xanderjs.haptimap.core.navigation.NavigationSession
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TourSuggestionControllerTest {

    private val here = GeoPoint(40.0, -3.0)
    private val dest = Place("dest", "Destino", here, null, null, 0)
    private val route = Route(dest, 0, Duration.ZERO, listOf(here, here), emptyList())
    private val tour = NavigationSession.NavigationActive(route, NavigationProfile.TOUR)
    private val focus = NavigationSession.NavigationActive(route, NavigationProfile.FOCUS)

    private fun poi(id: String) = PoiEvaluation(
        candidate = PoiCandidate(id, "POI $id", ExplorationCategory.MUSEUM, here),
        score = 1.0, detourMeters = 40.0, addedTime = Duration.ZERO,
        decision = PoiDecision.SUGGEST, discardReason = null,
    )

    private fun progressAt(distanceToManeuver: Double) = NavigationProgress(
        stepIndex = 0,
        nextManeuver = RouteStep(Maneuver.TURN_LEFT, 0, Duration.ZERO, emptyList(), here, here, null, null),
        distanceToManeuverMeters = distanceToManeuver,
        remainingMeters = 500.0, remainingTime = Duration.ZERO,
        crossTrackMeters = 0.0, fixAccuracyMeters = 5f, weakSignal = false,
    )

    private class Fixture(
        timeoutSeconds: Int = 6,
        session: NavigationSession,
        cooldown: Duration = 30.seconds,
        progressDistance: Double = 500.0,
        progressFactory: (Double) -> NavigationProgress,
    ) {
        val sessions = MutableStateFlow(session)
        val progress = MutableStateFlow<NavigationProgress?>(progressFactory(progressDistance))
        val settings = MutableStateFlow(UserSettings(suggestionTimeoutSeconds = timeoutSeconds))
        val suggestions = MutableStateFlow<List<PoiEvaluation>>(emptyList())
        val rejected = mutableListOf<String>()
        val haptics = mutableListOf<HapticEvent>()
        val controller = TourSuggestionController(
            suggestions = suggestions, sessions = sessions, progress = progress, settings = settings,
            reject = { rejected += it }, playHaptic = { haptics += it }, cooldown = cooldown,
        )
    }

    private fun fixture(
        timeoutSeconds: Int = 6,
        session: NavigationSession = tour,
        cooldown: Duration = 30.seconds,
    ) = Fixture(timeoutSeconds, session, cooldown, progressFactory = ::progressAt)

    @Test
    fun `shows the best suggestion with a haptic and times out from settings, rejecting it`() = runTest {
        val f = fixture(timeoutSeconds = 6)
        backgroundScope.launch { f.controller.run() }
        runCurrent()
        assertEquals(TourSuggestion.Inactive, f.controller.state.value)

        f.suggestions.value = listOf(poi("a"), poi("b")) // "a" es la mejor (primera, ya ordenada)
        runCurrent()
        assertEquals(TourSuggestion.Showing(poi("a")), f.controller.state.value)
        assertEquals(listOf(HapticEvent.POI_SUGGESTION), f.haptics)

        advanceTimeBy(5_000); runCurrent()
        assertEquals(TourSuggestion.Showing(poi("a")), f.controller.state.value) // aún no expira

        advanceTimeBy(1_500); runCurrent() // pasados los 6 s
        assertEquals(TourSuggestion.Inactive, f.controller.state.value)
        assertEquals(listOf("a"), f.rejected)
    }

    @Test
    fun `accept closes the suggestion and rejects the poi without waiting for the timeout`() = runTest {
        val f = fixture(timeoutSeconds = 10)
        backgroundScope.launch { f.controller.run() }
        runCurrent()
        f.suggestions.value = listOf(poi("a"))
        runCurrent()
        assertEquals(TourSuggestion.Showing(poi("a")), f.controller.state.value)

        f.controller.accept()
        runCurrent() // sin avanzar el tiempo: aceptar gana al timeout
        assertEquals(TourSuggestion.Inactive, f.controller.state.value)
        assertEquals(listOf("a"), f.rejected)
    }

    @Test
    fun `an imminent maneuver while showing retires the suggestion`() = runTest {
        val f = fixture()
        backgroundScope.launch { f.controller.run() }
        runCurrent()
        f.suggestions.value = listOf(poi("a"))
        runCurrent()
        assertEquals(TourSuggestion.Showing(poi("a")), f.controller.state.value)

        f.progress.value = progressAt(5.0) // dentro del radio de aproximación
        runCurrent()
        assertEquals(TourSuggestion.Inactive, f.controller.state.value)
        assertEquals(listOf("a"), f.rejected)
    }

    @Test
    fun `no suggestion in focus`() = runTest {
        val f = fixture(session = focus)
        backgroundScope.launch { f.controller.run() }
        runCurrent()
        f.suggestions.value = listOf(poi("a"))
        advanceTimeBy(60_000); runCurrent()
        assertEquals(TourSuggestion.Inactive, f.controller.state.value)
        assertTrue(f.haptics.isEmpty())
    }

    @Test
    fun `a cooldown passes between suggestions`() = runTest {
        val f = fixture(timeoutSeconds = 5, cooldown = 10.seconds)
        backgroundScope.launch { f.controller.run() }
        runCurrent()
        f.suggestions.value = listOf(poi("a"))
        runCurrent()
        assertEquals(TourSuggestion.Showing(poi("a")), f.controller.state.value)

        advanceTimeBy(5_100); runCurrent() // expira "a" (t≈5 s), empieza el cooldown de 10 s (termina t≈15 s)
        assertEquals(TourSuggestion.Inactive, f.controller.state.value)
        f.suggestions.value = listOf(poi("b")) // el pipeline deja de proponer "a" y ofrece "b"

        advanceTimeBy(4_000); runCurrent() // t≈9 s, aún en cooldown
        assertEquals(TourSuggestion.Inactive, f.controller.state.value)

        advanceTimeBy(6_100); runCurrent() // t≈15 s, pasado el cooldown
        assertEquals(TourSuggestion.Showing(poi("b")), f.controller.state.value)
    }
}
