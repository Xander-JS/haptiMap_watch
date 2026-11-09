package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.intelligence.PoiCandidate
import com.xanderjs.haptimap.core.intelligence.PoiDecision
import com.xanderjs.haptimap.core.intelligence.PoiEvaluation
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.offset
import com.xanderjs.haptimap.core.navigation.NavigationProgress
import kotlin.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Lógica pura del mapa continuo (paso 22): decisión de vista, seguimiento de cámara, rumbo y mapeo del estado. */
class NavigationMapStateTest {

    private val here = GeoPoint(40.0, -3.0)
    private val east = here.offset(90.0, 200.0)
    private val dest = Place("dest", "Destino", here.offset(90.0, 1000.0), null, null, 0)
    private val poiPoint = here.offset(90.0, 500.0).offset(0.0, 60.0)
    private val poi = PoiEvaluation(
        candidate = PoiCandidate("poi", "Café", ExplorationCategory.CAFE_RESTAURANT, poiPoint),
        score = 1.0, detourMeters = 120.0, addedTime = Duration.ZERO, decision = PoiDecision.SUGGEST, discardReason = null,
    )

    private fun route(points: List<GeoPoint>) = Route(
        dest, 0, Duration.ZERO, points,
        listOf(RouteStep(Maneuver.ARRIVAL, 0, Duration.ZERO, listOf(points.last()), points.last(), points.last(), null, null)),
    )
    private val original = route(listOf(here, dest.location))
    private val detourRoute = route(listOf(here, poiPoint, dest.location))

    private fun progressAt(position: GeoPoint?, maneuverEnd: GeoPoint = east) = NavigationProgress(
        stepIndex = 0,
        nextManeuver = RouteStep(Maneuver.CONTINUE, 0, Duration.ZERO, emptyList(), here, maneuverEnd, null, null),
        distanceToManeuverMeters = 100.0, remainingMeters = 500.0, remainingTime = Duration.ZERO,
        crossTrackMeters = 0.0, fixAccuracyMeters = 5f, weakSignal = false, position = position,
    )

    // --- seguimiento de cámara ---------------------------------------------

    @Test fun `panning stops following and recenter resumes it`() {
        assertEquals(false, following(currentlyFollowing = true, gesture = MapGesture.USER_PANNED))
        assertEquals(true, following(currentlyFollowing = false, gesture = MapGesture.RECENTER))
    }

    // --- rumbo -------------------------------------------------------------

    @Test fun `bearing east is about 90 degrees`() {
        assertEquals(90.0, bearingBetween(here, east).toDouble(), 1.0)
    }

    @Test fun `bearing north is about 0 degrees`() {
        assertEquals(0.0, bearingBetween(here, here.offset(0.0, 200.0)).toDouble(), 1.0)
    }

    // --- mapeo del estado --------------------------------------------------

    @Test fun `no map state without an active route`() {
        assertNull(navigationMapState(active = null, progress = progressAt(here), detour = TourDetour.Inactive, suggestion = TourSuggestion.Inactive))
    }

    @Test fun `maps route, destination, position and bearing`() {
        val state = navigationMapState(ActiveRoute(original, offline = false), progressAt(here), TourDetour.Inactive, TourSuggestion.Inactive)!!
        assertEquals(original.points, state.route)
        assertEquals(dest.location, state.destination)
        assertEquals(here, state.userPosition)
        assertEquals(90.0, state.userBearingDegrees!!.toDouble(), 1.0)
        assertNull(state.detourLeg)
        assertNull(state.poi)
    }

    @Test fun `active detour adds the detour leg and marks its poi`() {
        val detour = TourDetour.ToPoi(poi, original, detourRoute)
        val state = navigationMapState(ActiveRoute(original, offline = false), progressAt(here), detour, TourSuggestion.Inactive)!!
        assertEquals(detourRoute.points, state.detourLeg)
        assertEquals(poiPoint, state.poi!!.position)
        assertEquals("Café", state.poi!!.title)
    }

    @Test fun `a showing suggestion marks its poi`() {
        val state = navigationMapState(ActiveRoute(original, offline = false), progressAt(here), TourDetour.Inactive, TourSuggestion.Showing(poi))!!
        assertEquals(poiPoint, state.poi!!.position)
    }
}
