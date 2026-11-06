package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PoiEvaluatorTest {

    private val config = PoiEvaluationConfig()
    private val here = GeoPoint(40.0, -3.0)
    private val dest = Place("dest", "Destino", here, null, null, 0)
    // el estimador falso ignora la ruta: aquí basta una mínima válida
    private val route = Route(dest, 0, Duration.ZERO, listOf(here, here), listOf(RouteStep(Maneuver.ARRIVAL, 0, Duration.ZERO, emptyList(), here, here, null, null)))
    private val rejected = RejectedPoiMemory()

    private fun poi(id: String, category: PlaceCategory? = PlaceCategory.CAFE, rating: Double? = null) =
        PoiCandidate(id, "POI $id", category, here, rating)

    // desvío a medida por POI, para tocar cada umbral sin depender de la geometría
    private fun estimatorOf(vararg byId: Pair<String, DetourEstimate>): DetourEstimator {
        val map = byId.toMap()
        return DetourEstimator { c, _, _ -> map[c.id] ?: DetourEstimate(10.0, 20.0, 15.seconds) }
    }

    private fun evaluatorWith(
        estimator: DetourEstimator = estimatorOf(),
        relevance: RelevanceProvider = RelevanceProvider { 1.0 },
    ) = PoiEvaluator(config, estimator, relevance)

    private fun PoiEvaluator.evaluate(candidates: List<PoiCandidate>, profile: NavigationProfile = NavigationProfile.TOUR) =
        evaluate(candidates, route, fromAlongMeters = 0.0, profile = profile, rejected = rejected)

    @Test
    fun `FOCUS returns no suggestions at all`() {
        val result = evaluatorWith().evaluate(listOf(poi("a")), profile = NavigationProfile.FOCUS)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `TOUR suggests a close, relevant candidate`() {
        val result = evaluatorWith().evaluate(listOf(poi("a")))
        assertEquals(PoiDecision.SUGGEST, result.single().decision)
        assertNull(result.single().discardReason)
    }

    @Test
    fun `too far from the route is discarded, and the threshold itself is kept`() {
        val estimator = estimatorOf(
            "far" to DetourEstimate(config.maxDistanceFromRouteMeters + 0.1, 20.0, 15.seconds),
            "edge" to DetourEstimate(config.maxDistanceFromRouteMeters, 20.0, 15.seconds),
        )
        val result = evaluatorWith(estimator).evaluate(listOf(poi("far"), poi("edge"))).associateBy { it.candidate.id }
        assertEquals(DiscardReason.TOO_FAR, result.getValue("far").discardReason)
        assertEquals(PoiDecision.SUGGEST, result.getValue("edge").decision)
    }

    @Test
    fun `a detour that is too large is discarded, and the threshold itself is kept`() {
        val estimator = estimatorOf(
            "big" to DetourEstimate(50.0, config.maxDetourMeters + 0.1, 15.seconds),
            "edge" to DetourEstimate(50.0, config.maxDetourMeters, 15.seconds),
        )
        val result = evaluatorWith(estimator).evaluate(listOf(poi("big"), poi("edge"))).associateBy { it.candidate.id }
        assertEquals(DiscardReason.DETOUR_TOO_LARGE, result.getValue("big").discardReason)
        assertEquals(PoiDecision.SUGGEST, result.getValue("edge").decision)
    }

    @Test
    fun `too much added time is discarded, and the threshold itself is kept`() {
        val estimator = estimatorOf(
            "slow" to DetourEstimate(50.0, 100.0, config.maxAddedTime + 1.seconds),
            "edge" to DetourEstimate(50.0, 100.0, config.maxAddedTime),
        )
        val result = evaluatorWith(estimator).evaluate(listOf(poi("slow"), poi("edge"))).associateBy { it.candidate.id }
        assertEquals(DiscardReason.TOO_MUCH_TIME, result.getValue("slow").discardReason)
        assertEquals(PoiDecision.SUGGEST, result.getValue("edge").decision)
    }

    @Test
    fun `low relevance is discarded, and the minimum itself is kept`() {
        val relevance = RelevanceProvider { if (it.id == "dull") config.minRelevance - 0.01 else config.minRelevance }
        val result = evaluatorWith(relevance = relevance).evaluate(listOf(poi("dull"), poi("edge"))).associateBy { it.candidate.id }
        assertEquals(DiscardReason.LOW_RELEVANCE, result.getValue("dull").discardReason)
        assertEquals(PoiDecision.SUGGEST, result.getValue("edge").decision)
    }

    @Test
    fun `a rejected POI is discarded even with perfect metrics, and rejection wins over other reasons`() {
        rejected.reject("ignored")
        rejected.reject("ignored-and-far")
        val estimator = estimatorOf("ignored-and-far" to DetourEstimate(999.0, 999.0, 10.minutes))
        val result = evaluatorWith(estimator).evaluate(listOf(poi("ignored"), poi("ignored-and-far"))).associateBy { it.candidate.id }
        assertEquals(DiscardReason.ALREADY_REJECTED, result.getValue("ignored").discardReason)
        assertEquals(DiscardReason.ALREADY_REJECTED, result.getValue("ignored-and-far").discardReason)
    }

    @Test
    fun `rejecting a POI stops it being suggested, and clearing the memory brings it back`() {
        val evaluator = evaluatorWith()
        assertEquals(PoiDecision.SUGGEST, evaluator.evaluate(listOf(poi("a"))).single().decision)

        rejected.reject("a")
        assertEquals(DiscardReason.ALREADY_REJECTED, evaluator.evaluate(listOf(poi("a"))).single().discardReason)

        rejected.clear()
        assertEquals(PoiDecision.SUGGEST, evaluator.evaluate(listOf(poi("a"))).single().decision)
    }

    @Test
    fun `suggestions are ranked by score and discarded POIs come last`() {
        // mismo relevance y rating: ordena la cercanía (desvío menor, score mayor)
        val estimator = estimatorOf(
            "near" to DetourEstimate(10.0, 20.0, 15.seconds),
            "mid" to DetourEstimate(40.0, 80.0, 60.seconds),
            "far" to DetourEstimate(config.maxDistanceFromRouteMeters + 1, 20.0, 15.seconds),
        )
        val result = evaluatorWith(estimator).evaluate(listOf(poi("mid"), poi("far"), poi("near")))
        assertEquals(listOf("near", "mid", "far"), result.map { it.candidate.id })
        assertEquals(listOf("near", "mid"), result.suggestions().map { it.candidate.id })
        assertTrue(result[0].score > result[1].score)
    }

    @Test
    fun `a realistic mixed scenario keeps only the worthwhile POIs, best first`() {
        val estimator = estimatorOf(
            "museo" to DetourEstimate(30.0, 60.0, 46.seconds),
            "cafe" to DetourEstimate(15.0, 30.0, 23.seconds),
            "farmacia-lejos" to DetourEstimate(config.maxDistanceFromRouteMeters + 20, 240.0, 3.minutes),
            "tienda-rodeo" to DetourEstimate(90.0, config.maxDetourMeters + 50, 2.minutes),
        )
        val relevance = RelevanceProvider {
            when (it.id) {
                "museo" -> 0.9
                "cafe" -> 0.8
                else -> 0.5
            }
        }
        val pois = listOf(
            poi("museo", category = null, rating = 4.6),
            poi("cafe", category = PlaceCategory.CAFE, rating = 4.2),
            poi("farmacia-lejos", category = PlaceCategory.PHARMACY),
            poi("tienda-rodeo", category = PlaceCategory.STORE),
        )
        val result = evaluatorWith(estimator, relevance).evaluate(pois)

        assertEquals(listOf("museo", "cafe"), result.suggestions().map { it.candidate.id })
        val byId = result.associateBy { it.candidate.id }
        assertEquals(DiscardReason.TOO_FAR, byId.getValue("farmacia-lejos").discardReason)
        assertEquals(DiscardReason.DETOUR_TOO_LARGE, byId.getValue("tienda-rodeo").discardReason)
    }
}
