package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ExplorationCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExplorationTypesTest {

    @Test
    fun `every exploration category maps to at least one place type and back`() {
        for (category in ExplorationCategory.entries) {
            val types = category.placeTypes
            assert(types.isNotEmpty()) { "$category sin tipos de Places" }
            assertEquals(category, explorationCategoryForPlaceType(types.first()))
        }
    }

    @Test
    fun `an unknown or null type maps to no category`() {
        assertNull(explorationCategoryForPlaceType("hardware_store"))
        assertNull(explorationCategoryForPlaceType(null))
    }
}
