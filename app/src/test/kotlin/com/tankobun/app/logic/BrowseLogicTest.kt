package com.tankobun.app.logic

import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseLogicTest {
    @Test
    fun detectsBrowseLandingContent() {
        assertFalse(BrowseLandingData(emptyList(), emptyList(), emptyList(), emptyList()).hasContent())
        assertTrue(BrowseLandingData(listOf(media(1)), emptyList(), emptyList(), emptyList()).hasContent())
    }

    @Test
    fun reconstructsCachedPageMetadata() {
        val shortPage = cachedBrowsePageFromMedia(List(10) { media(it) })
        val fullPage = cachedBrowsePageFromMedia(List(BROWSE_RESULTS_PAGE_SIZE) { media(it) })
        val secondPage = cachedBrowsePageFromMedia(List(BROWSE_RESULTS_PAGE_SIZE + 1) { media(it) })

        assertEquals(0, shortPage.currentPage)
        assertTrue(shortPage.hasNextPage)
        assertEquals(1, fullPage.currentPage)
        assertTrue(fullPage.hasNextPage)
        assertEquals(2, secondPage.currentPage)
        assertFalse(secondPage.hasNextPage)
    }

    private fun media(id: Int): AnilistMedia =
        AnilistMedia(
            id = id,
            idMal = null,
            title = AnilistTitle(
                romaji = null,
                english = null,
                native = null,
                userPreferred = "Manga $id",
            ),
            description = null,
            coverImage = null,
            bannerImage = null,
            chapters = null,
            volumes = null,
            format = null,
            status = null,
            averageScore = null,
            popularity = null,
            startDateYear = null,
            endDateYear = null,
            siteUrl = null,
            genres = emptyList(),
            synonyms = emptyList(),
            isAdult = false,
            updatedAtEpochSeconds = null,
        )
}
