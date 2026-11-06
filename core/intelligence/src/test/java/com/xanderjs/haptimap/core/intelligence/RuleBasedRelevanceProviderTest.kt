package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.PlaceCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleBasedRelevanceProviderTest {

    private val config = PoiEvaluationConfig()

    private fun poi(category: PlaceCategory?) = PoiCandidate("id", "POI", category, GeoPoint(0.0, 0.0))

    @Test
    fun `a known category uses its configured weight`() {
        val provider = RuleBasedRelevanceProvider(config)
        assertEquals(config.categoryWeights.getValue(PlaceCategory.CAFE), provider.relevance(poi(PlaceCategory.CAFE)), 1e-9)
    }

    @Test
    fun `a null category uses the default weight`() {
        val provider = RuleBasedRelevanceProvider(config)
        assertEquals(config.defaultCategoryWeight, provider.relevance(poi(null)), 1e-9)
    }

    @Test
    fun `a favorite category is boosted and clamped to one`() {
        val provider = RuleBasedRelevanceProvider(config, favoriteCategories = setOf(PlaceCategory.CAFE, PlaceCategory.PHARMACY))
        // pharmacy 0.3 * 1.5 = 0.45
        assertEquals(config.categoryWeights.getValue(PlaceCategory.PHARMACY) * config.favoriteBoost, provider.relevance(poi(PlaceCategory.PHARMACY)), 1e-9)
        // cafe 0.9 * 1.5 = 1.35 -> recortado a 1.0
        assertEquals(1.0, provider.relevance(poi(PlaceCategory.CAFE)), 1e-9)
    }

    @Test
    fun `favorites do not affect a null category`() {
        val provider = RuleBasedRelevanceProvider(config, favoriteCategories = setOf(PlaceCategory.CAFE))
        assertEquals(config.defaultCategoryWeight, provider.relevance(poi(null)), 1e-9)
    }
}
