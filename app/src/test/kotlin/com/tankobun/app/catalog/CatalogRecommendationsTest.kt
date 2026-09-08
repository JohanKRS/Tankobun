package com.tankobun.app.catalog

import com.tankobun.core.model.*
import org.junit.Assert.*
import org.junit.Test

class CatalogRecommendationsTest {
    @Test fun aniListKeepsOrderArtworkAndVotesWhileSupplementAddsMissingWorks() {
        val primaryMedia = media(1).copy(chapters = null)
        val primary = AnilistRecommendationPage(listOf(AnilistRecommendation(primaryMedia, 50)), 1, true)
        val supplements = listOf(
            AnilistRecommendation(primaryMedia.copy(coverImage = "secondary", chapters = 25, mangaBakaId = 99), null),
            AnilistRecommendation(media(-2), null),
            AnilistRecommendation(media(42), null),
            AnilistRecommendation(media(-3).copy(isAdult = true), null),
        )
        val page = mergeRecommendations(42, 1, primary, supplements, false)
        assertEquals(listOf(1, -2), page.recommendations.map { it.media.id })
        assertEquals(primaryMedia.coverImage, page.recommendations.first().media.coverImage)
        assertEquals(25, page.recommendations.first().media.chapters)
        assertEquals(99, page.recommendations.first().media.mangaBakaId)
        assertEquals(50, page.recommendations.first().rating)
        assertTrue(page.hasNextPage)
    }

    @Test fun supplementalCountNeverInventsAnotherAniListPage() {
        val supplements = (1..36).map { AnilistRecommendation(media(-it), null) }
        val page = mergeRecommendations(42, 1, null, supplements, false)
        assertEquals(36, page.recommendations.size)
        assertFalse(page.hasNextPage)
    }

    private fun media(id: Int) = AnilistMedia(
        id = id, idMal = null, title = AnilistTitle(null, null, null, "Fictional story"),
        description = null, coverImage = "https://example.test/cover.jpg", bannerImage = null,
        chapters = 12, volumes = 2, format = "MANGA", status = "FINISHED", averageScore = 80,
        popularity = null, startDateYear = null, endDateYear = null, siteUrl = null,
        genres = emptyList(), synonyms = emptyList(), isAdult = false, updatedAtEpochSeconds = null,
    )
}
