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
            tags.visibleTags(query = "", includeAdult = true).map { it.name },
        )
    }

    @Test
    fun visibleTagsHidesAdultTagsWhenDisabled() {
        val tags = listOf(
            AnilistMediaTag(name = "Ahegao", category = "Adult", isAdult = true),
            AnilistMediaTag(name = "Coming of Age", category = "Theme", isAdult = false),
        )

        assertEquals(
            listOf("Coming of Age"),
            tags.visibleTags(query = "", includeAdult = false).map { it.name },
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
            tags.visibleTags(query = "sports", includeAdult = false).map { it.name },
        )
    }
}
