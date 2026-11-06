package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleBasedRelevanceProviderTest {

    private val config = PoiEvaluationConfig()

    private fun poi(category: ExplorationCategory?) = PoiCandidate("id", "POI", category, GeoPoint(0.0, 0.0))

    @Test
    fun `a known category uses its configured weight`() {
        val provider = RuleBasedRelevanceProvider(config)
        assertEquals(config.categoryWeights.getValue(ExplorationCategory.MUSEUM), provider.relevance(poi(ExplorationCategory.MUSEUM)), 1e-9)
    }

    @Test
    fun `a null category uses the default weight`() {
        val provider = RuleBasedRelevanceProvider(config)
        assertEquals(config.defaultCategoryWeight, provider.relevance(poi(null)), 1e-9)
    }

    @Test
    fun `a favorite category is boosted and clamped to one`() {
        val provider = RuleBasedRelevanceProvider(config, favoriteCategories = setOf(ExplorationCategory.VIEWPOINT, ExplorationCategory.CAFE_RESTAURANT))
        // café/restaurante 0.6 * 1.5 = 0.9
        assertEquals(config.categoryWeights.getValue(ExplorationCategory.CAFE_RESTAURANT) * config.favoriteBoost, provider.relevance(poi(ExplorationCategory.CAFE_RESTAURANT)), 1e-9)
        // mirador 0.9 * 1.5 = 1.35 -> recortado a 1.0
        assertEquals(1.0, provider.relevance(poi(ExplorationCategory.VIEWPOINT)), 1e-9)
    }

    @Test
    fun `favorites do not affect a null category`() {
        val provider = RuleBasedRelevanceProvider(config, favoriteCategories = setOf(ExplorationCategory.VIEWPOINT))
        assertEquals(config.defaultCategoryWeight, provider.relevance(poi(null)), 1e-9)
    }
}
