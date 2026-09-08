package com.tankobun.app.catalog

import com.tankobun.app.home.withHomeTrendingLimit
import com.tankobun.core.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class HomeCatalogSelectionTest {
    @Test fun combinedKeepsAlternativesUntilEveryGenreClaimsAUniqueWork() {
        val genres = listOf("Action", "Adventure", "Comedy")
        val al = HomeGenreCandidates(listOf(media(1)), mapOf(
            "Action" to listOf(media(1), media(2)), "Adventure" to listOf(media(1), media(3)),
            "Comedy" to listOf(media(1), media(2), media(3)),
        ))
        val mb = HomeGenreCandidates(listOf(media(-4)), mapOf(
            "Action" to listOf(media(-4)), "Adventure" to listOf(media(1), media(-4)),
            "Comedy" to listOf(media(1), media(-4), media(-5)),
        ))
        assertEquals(listOf(1, -4, 2), mergeHomeCandidates(genres, al, mb, CatalogMode.COMBINED).feed(genres).genreHighlights.map { it.media.id })
        assertEquals(listOf(1, 3, 2), mergeHomeCandidates(genres, al, mb, CatalogMode.ANILIST).feed(genres).genreHighlights.map { it.media.id })
        assertEquals(listOf(-4, 1, -5), mergeHomeCandidates(genres, al, mb, CatalogMode.MANGABAKA).feed(genres).genreHighlights.map { it.media.id })
    }

    @Test fun mangaBakaTagsAndTheirAncestorsCoverGenresEvenBeyondTheFirstThirtyWorks() {
        val genres = listOf("Action", "Mecha", "Music", "Historical")
        val taxonomy = CatalogTaxonomy(listOf(
            CatalogTag("mb:1", "Action", mangaBakaId = 1, isGenre = true),
            CatalogTag("mb:2", "Mecha", mangaBakaId = 2),
            CatalogTag("mb:3", "Music", mangaBakaId = 3),
            CatalogTag("mb:4", "Historical", mangaBakaId = 4),
            CatalogTag("mb:5", "Period", mangaBakaId = 5, parentId = 4),
        ))
        val pool = (1..50).map { media(it).copy(mangaBakaTagIds = listOf(1)) } + listOf(
            media(51).copy(mangaBakaTagIds = listOf(2)), media(52).copy(tags = listOf("Music")),
            media(53).copy(mangaBakaTagIds = listOf(5)),
        )
        val feed = mangaBakaHomeCandidates(genres, pool, taxonomy).feed(genres).withHomeTrendingLimit()
        assertEquals(genres, feed.genreHighlights.map { it.genre })
        assertEquals(listOf(1, 51, 52, 53), feed.genreHighlights.map { it.media.id })
        assertEquals(5, feed.trending.size)
    }

    @Test fun sparsePoolFetchesOnlyMissingGenresAndAdvancesPastAlreadyUsedWorks() = runBlocking {
        val genres = listOf("Action", "Adventure", "Music")
        val calls = mutableListOf<String>()
        val snapshots = mutableListOf<List<AnilistGenreHighlight>>()
        val initial = HomeGenreCandidates(emptyList(), mapOf("Action" to listOf(media(1)), "Music" to listOf(media(2))))
        val feed = completeHomeGenres(genres, initial, fetch = { genre ->
            calls += genre
            if (genre == "Adventure") listOf(media(1), media(2)) else listOf(media(2), media(3))
        }, onLoaded = { snapshots += it }).feed(genres)
        assertEquals(listOf("Adventure", "Music"), calls)
        assertEquals(genres, feed.genreHighlights.map { it.genre })
        assertEquals(listOf(1, 2, 3), feed.genreHighlights.map { it.media.id })
        assertTrue(snapshots.all { highlights -> highlights.map { it.media.id }.distinct().size == highlights.size })
    }

    @Test fun completePoolMakesNoExtraRequestsAndCatalogFailureDoesNotCauseDuplicates() = runBlocking {
        val genres = listOf("Action", "Music", "Mecha", "Thriller")
        val candidates = HomeGenreCandidates(emptyList(), genres.mapIndexed { index, genre -> genre to listOf(media(index)) }.toMap())
        completeHomeGenres(genres, candidates, fetch = { error("No request needed") }, onLoaded = {})
        var calls = 0
        val sparse = candidates.copy(byGenre = mapOf("Action" to listOf(media(1))))
        val feed = completeHomeGenres(genres, sparse, fetch = { calls++; null }, onLoaded = {}).feed(genres)
        assertEquals(2, calls)
        assertEquals(listOf(1), feed.genreHighlights.map { it.media.id })
    }

    private fun media(id: Int) = AnilistMedia(
        id = id, idMal = null, title = AnilistTitle(null, null, null, "Fictional story"),
        description = null, coverImage = null, bannerImage = null, chapters = null, volumes = null,
        format = "MANGA", status = "RELEASING", averageScore = null, popularity = null,
        startDateYear = null, endDateYear = null, siteUrl = null, genres = emptyList(), synonyms = emptyList(),
        isAdult = false, updatedAtEpochSeconds = null,
    )
}
