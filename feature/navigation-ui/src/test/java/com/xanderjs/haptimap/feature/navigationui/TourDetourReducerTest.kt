package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.intelligence.PoiCandidate
import com.xanderjs.haptimap.core.intelligence.PoiDecision
import com.xanderjs.haptimap.core.intelligence.PoiEvaluation
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import kotlin.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TourDetourReducerTest {

    private val here = GeoPoint(40.0, -3.0)
    private val poi = PoiEvaluation(
        candidate = PoiCandidate("poi", "Café", ExplorationCategory.CAFE_RESTAURANT, here),
        score = 1.0, detourMeters = 40.0, addedTime = Duration.ZERO, decision = PoiDecision.SUGGEST, discardReason = null,
    )
    private fun route(name: String) = Route(Place(name, name, here, null, null, 0), 0, Duration.ZERO, listOf(here, here), emptyList())
    private val original = route("Destino")
    private val detour = route("Desvío")

    private val loading = TourDetour.Loading(poi, original)
    private val toPoi = TourDetour.ToPoi(poi, original, detour)
    private val poiReached = TourDetour.PoiReached(poi, original, detour)
    private val returning = TourDetour.Returning(poi, original, detour)

    @Test
    fun `the full detour path, accept to return to inactive`() {
        var s: TourDetour = TourDetour.Inactive
        s = reduce(s, TourDetourEvent.Start(poi, original)); assertEquals(loading, s)
        s = reduce(s, TourDetourEvent.Loaded(detour)); assertEquals(toPoi, s)
        s = reduce(s, TourDetourEvent.ReachPoi); assertEquals(poiReached, s)
        s = reduce(s, TourDetourEvent.Resume); assertEquals(returning, s)
        s = reduce(s, TourDetourEvent.Finish); assertEquals(TourDetour.Inactive, s)
    }

    @Test
    fun `cancel returns to inactive from every detour state, and does nothing when inactive`() {
        listOf(loading, toPoi, poiReached, returning).forEach {
            assertEquals("cancel from $it", TourDetour.Inactive, reduce(it, TourDetourEvent.Cancel))
        }
        assertSame(TourDetour.Inactive, reduce(TourDetour.Inactive, TourDetourEvent.Cancel))
    }

    @Test
    fun `a failed load falls back to inactive, only from loading`() {
        assertEquals(TourDetour.Inactive, reduce(loading, TourDetourEvent.LoadFailed))
        assertSame(toPoi, reduce(toPoi, TourDetourEvent.LoadFailed))
    }

    @Test
    fun `start is ignored while a detour is already in progress`() {
        assertSame(loading, reduce(loading, TourDetourEvent.Start(poi, original)))
        assertSame(toPoi, reduce(toPoi, TourDetourEvent.Start(poi, original)))
    }

    @Test
    fun `events that do not apply leave the state untouched`() {
        assertSame(TourDetour.Inactive, reduce(TourDetour.Inactive, TourDetourEvent.Loaded(detour)))
        assertSame(TourDetour.Inactive, reduce(TourDetour.Inactive, TourDetourEvent.ReachPoi))
        assertSame(loading, reduce(loading, TourDetourEvent.ReachPoi))
        assertSame(toPoi, reduce(toPoi, TourDetourEvent.Resume))
        assertSame(toPoi, reduce(toPoi, TourDetourEvent.Finish))
        assertSame(poiReached, reduce(poiReached, TourDetourEvent.Finish))
    }

    @Test
    fun `accessors expose the poi and routes per state`() {
        assertFalse(TourDetour.Inactive.active)
        assertNull(TourDetour.Inactive.poi)
        assertNull(TourDetour.Inactive.detourRoute)
        assertTrue(loading.active)
        assertEquals(poi, loading.poi)
        assertEquals(original, loading.originalRoute)
        assertNull("aún no hay ruta de desvío", loading.detourRoute)
        assertEquals(detour, toPoi.detourRoute)
        assertEquals(detour, returning.detourRoute)
    }
}
