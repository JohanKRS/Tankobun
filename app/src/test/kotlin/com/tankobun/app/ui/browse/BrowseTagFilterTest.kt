package com.tankobun.app.ui.browse

import com.tankobun.core.model.AnilistMediaTag
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowseTagFilterTest {
    @Test
    fun visibleTagsIncludesAdultTagsAndSortsByName() {
        val tags = listOf(
            AnilistMediaTag(name = "Zombie", category = "Theme", isAdult = false),
            AnilistMediaTag(name = "Ahegao", category = "Adult", isAdult = true),
            AnilistMediaTag(name = "Coming of Age", category = "Theme", isAdult = false),
        )

        assertEquals(
            listOf("Ahegao", "Coming of Age", "Zombie"),
            tags.visibleTags(query = "").map { it.name },
        )
    }

    @Test
    fun visibleTagsSearchesNameAndCategory() {
        val tags = listOf(
            AnilistMediaTag(name = "Archery", category = "Sports", isAdult = false),
            AnilistMediaTag(name = "Aviation", category = "Vehicles", isAdult = false),
            AnilistMediaTag(name = "Baseball", category = "Sports", isAdult = false),
        )

        assertEquals(
            listOf("Archery", "Baseball"),
            tags.visibleTags(query = "sports").map { it.name },
        )
    }
}
