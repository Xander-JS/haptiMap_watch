package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.intelligence.PoiCandidate
import com.xanderjs.haptimap.core.intelligence.PoiDecision
import com.xanderjs.haptimap.core.intelligence.PoiEvaluation
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.navigation.NavigationProgress
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationThresholds
import kotlin.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TourSuggestionReducerTest {

    private val here = GeoPoint(40.0, -3.0)
    private val dest = Place("dest", "Destino", here, null, null, 0)
    private val route = Route(dest, 0, Duration.ZERO, listOf(here, here), emptyList())
    private val thresholds = NavigationThresholds()

    private fun poi(id: String) = PoiEvaluation(
        candidate = PoiCandidate(id, "POI $id", ExplorationCategory.MUSEUM, here),
        score = 1.0,
        detourMeters = 40.0,
        addedTime = Duration.ZERO,
        decision = PoiDecision.SUGGEST,
        discardReason = null,
    )

    private val a = poi("a")
    private val b = poi("b")

    private fun progressAt(distanceToManeuver: Double) = NavigationProgress(
        stepIndex = 0,
        nextManeuver = RouteStep(Maneuver.TURN_LEFT, 0, Duration.ZERO, emptyList(), here, here, null, null),
        distanceToManeuverMeters = distanceToManeuver,
        remainingMeters = 500.0,
        remainingTime = Duration.ZERO,
        crossTrackMeters = 0.0,
        fixAccuracyMeters = 5f,
        weakSignal = false,
    )

    // --- transiciones del reductor ---

    @Test
    fun `offer shows the poi only from inactive`() {
        assertEquals(TourSuggestion.Showing(a), reduce(TourSuggestion.Inactive, TourSuggestionEvent.Offer(a)))
    }

    @Test
    fun `a second offer while showing is ignored (one at a time)`() {
        val showing = TourSuggestion.Showing(a)
        assertSame(showing, reduce(showing, TourSuggestionEvent.Offer(b)))
    }

    @Test
    fun `accept moves showing to accepted keeping the poi`() {
        assertEquals(TourSuggestion.Accepted(a), reduce(TourSuggestion.Showing(a), TourSuggestionEvent.Accept))
    }

    @Test
    fun `timeout dismiss and maneuver all return showing to inactive`() {
        val showing = TourSuggestion.Showing(a)
        assertEquals(TourSuggestion.Inactive, reduce(showing, TourSuggestionEvent.Timeout))
        assertEquals(TourSuggestion.Inactive, reduce(showing, TourSuggestionEvent.Dismiss))
        assertEquals(TourSuggestion.Inactive, reduce(showing, TourSuggestionEvent.ManeuverWins))
    }

    @Test
    fun `consumed returns accepted to inactive, the step 20 handoff`() {
        assertEquals(TourSuggestion.Inactive, reduce(TourSuggestion.Accepted(a), TourSuggestionEvent.Consumed))
    }

    @Test
    fun `events that do not apply keep the same state`() {
        assertSame(TourSuggestion.Inactive, reduce(TourSuggestion.Inactive, TourSuggestionEvent.Accept))
        assertSame(TourSuggestion.Inactive, reduce(TourSuggestion.Inactive, TourSuggestionEvent.Timeout))
        assertSame(TourSuggestion.Inactive, reduce(TourSuggestion.Inactive, TourSuggestionEvent.Consumed))
        val showing = TourSuggestion.Showing(a)
        assertSame(showing, reduce(showing, TourSuggestionEvent.Consumed))
        val accepted = TourSuggestion.Accepted(a)
        assertSame(accepted, reduce(accepted, TourSuggestionEvent.Accept))
        assertSame(accepted, reduce(accepted, TourSuggestionEvent.Timeout))
    }

    // --- puerta: perfil y colisión con maniobra ---

    private val tour = NavigationSession.NavigationActive(route, NavigationProfile.TOUR)
    private val focus = NavigationSession.NavigationActive(route, NavigationProfile.FOCUS)

    @Test
    fun `can offer while navigating in tour with no imminent maneuver`() {
        assertTrue(canOfferSuggestion(TourSuggestion.Inactive, tour, progressAt(500.0), thresholds))
    }

    @Test
    fun `cannot offer in focus`() {
        assertFalse(canOfferSuggestion(TourSuggestion.Inactive, focus, progressAt(500.0), thresholds))
    }

    @Test
    fun `cannot offer when not navigating`() {
        assertFalse(canOfferSuggestion(TourSuggestion.Inactive, NavigationSession.Idle, null, thresholds))
    }

    @Test
    fun `cannot offer when a maneuver is within the approach ring`() {
        // navegación manda: nada en el radio de aproximación (18 m por defecto); el borde mismo cuenta como inminente
        assertFalse(canOfferSuggestion(TourSuggestion.Inactive, tour, progressAt(thresholds.approachMeters), thresholds))
        assertFalse(canOfferSuggestion(TourSuggestion.Inactive, tour, progressAt(thresholds.approachMeters - 0.1), thresholds))
        assertTrue(canOfferSuggestion(TourSuggestion.Inactive, tour, progressAt(thresholds.approachMeters + 0.1), thresholds))
    }

    @Test
    fun `cannot offer when one is already showing`() {
        assertFalse(canOfferSuggestion(TourSuggestion.Showing(a), tour, progressAt(500.0), thresholds))
    }

    @Test
    fun `maneuver imminent uses the approach ring`() {
        assertTrue(maneuverImminent(progressAt(thresholds.approachMeters), thresholds))
        assertFalse(maneuverImminent(progressAt(thresholds.approachMeters + 1), thresholds))
        assertFalse(maneuverImminent(null, thresholds))
    }
}
