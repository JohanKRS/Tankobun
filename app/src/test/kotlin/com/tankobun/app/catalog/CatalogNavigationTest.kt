package com.tankobun.app.catalog

import com.tankobun.app.LibraryMode
import com.tankobun.app.logic.browseCacheKey
import com.tankobun.app.logic.browseLandingCacheKey
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CatalogNavigationTest {
    @Test fun priorityChangesOrderWithoutLosingExclusiveWorks() {
        val al = listOf(media(1), media(2))
        val mb = listOf(media(-3), media(2))
        assertEquals(listOf(1, 2, -3), mergeCatalogMedia(al, mb, CatalogMode.ANILIST).map { it.id })
        assertEquals(listOf(-3, 2, 1), mergeCatalogMedia(al, mb, CatalogMode.MANGABAKA).map { it.id })
    }

    @Test fun combinedAlternatesUniqueWorksEvenWhenProviderRankingsOverlap() {
        val al = listOf(media(1), media(2), media(3), media(4))
        val mb = listOf(media(1), media(-5), media(2), media(-6))
        assertEquals(listOf(1, -5, 2, -6, 3, 4), mergeCatalogMedia(al, mb, CatalogMode.COMBINED).map { it.id })
        // Identical display titles do not prove that two works are the same.
        assertEquals(6, mergeCatalogMedia(al, mb, CatalogMode.COMBINED).size)
    }

    @Test fun artworkAndEnrichmentAreIndependentOfNavigationPriority() {
        val al = media(1).copy(bannerImage = "anilist-banner", chapters = null)
        val mb = media(1).copy(coverImage = "other-cover", bannerImage = null, chapters = 45, mangaBakaId = 12)
        CatalogMode.entries.forEach { mode ->
            val merged = mergeCatalogMedia(listOf(al), listOf(mb), mode).single()
            assertEquals(al.coverImage, merged.coverImage)
            assertEquals(al.bannerImage, merged.bannerImage)
            assertEquals(45, merged.chapters)
            assertEquals(12, merged.mangaBakaId)
            assertEquals(al.id, merged.id)
        }
    }

    @Test fun aMissingCatalogDoesNotHideTheOtherCatalog() {
        CatalogMode.entries.forEach { mode ->
            assertEquals(listOf(media(1)), mergeCatalogMedia(listOf(media(1)), emptyList(), mode))
            assertEquals(listOf(media(-1)), mergeCatalogMedia(emptyList(), listOf(media(-1)), mode))
        }
    }

    @Test fun modeChangePreservesQueryFiltersLibraryAndPersonalizedShelf() {
        val original = TankobunUiState(
            libraryMode = LibraryMode.ANILIST, loggedIn = true, mangaBakaAccountName = "Reader",
            library = listOf(media(9)), browseForYou = listOf(media(-3)),
            searchQuery = "Example", browseGenres = setOf("Action"), browseTags = setOf("Magic"),
            browseSelection = CatalogSearchFilters(countries = setOf("JP", "KR"), years = PublicationYears(2000, 2020)),
            searchResults = listOf(media(1)), browseResultsPage = 4, browseResultsHasMore = true,
            homeTrending = listOf(media(1)), browseTrending = listOf(media(1)),
        )
        val changed = original.withCatalogMode(CatalogMode.MANGABAKA)
        assertEquals(original.library, changed.library)
        assertEquals(original.libraryMode, changed.libraryMode)
        assertEquals(original.loggedIn, changed.loggedIn)
        assertEquals(original.mangaBakaAccountName, changed.mangaBakaAccountName)
        assertEquals(original.browseForYou, changed.browseForYou)
        assertEquals(original.searchQuery, changed.searchQuery)
        assertEquals(original.browseGenres, changed.browseGenres)
        assertEquals(original.browseTags, changed.browseTags)
        assertEquals(original.browseSelection, changed.browseSelection)
        assertTrue(changed.searchResults.isEmpty())
        assertTrue(changed.browseTrending.isEmpty())
        assertTrue(changed.homeTrending.isEmpty())
        assertEquals(0, changed.browseResultsPage)
        assertFalse(changed.browseResultsHasMore)
    }

    @Test fun searchAndShelvesCannotReuseAnotherModeOrAdultCache() {
        val states = CatalogMode.entries.flatMap { mode ->
            listOf(false, true).map { adult -> TankobunUiState(catalogMode = mode, showNsfwContent = adult, searchQuery = "Title") }
        }
        assertEquals(6, states.map { it.browseCacheKey() }.distinct().size)
        assertEquals(6, states.map { it.browseLandingCacheKey("trending") }.distinct().size)
        assertEquals(TankobunUiState().browseCacheKey(), TankobunUiState(catalogMode = CatalogMode.ANILIST).browseCacheKey())
    }

    @Test fun olderAndUnknownStoredPreferencesDefaultToAniList() {
        assertEquals(CatalogMode.ANILIST, CatalogMode.fromStored(null))
        assertEquals(CatalogMode.ANILIST, CatalogMode.fromStored("future-mode"))
        CatalogMode.entries.forEach { assertEquals(it, CatalogMode.fromStored(it.name)) }
    }

    @Test fun preferredCatalogDoesNotWaitForTheOtherFullTimeout() = runBlocking {
        for (mode in listOf(CatalogMode.ANILIST, CatalogMode.MANGABAKA)) {
            var cancelled = false
            val slow: suspend () -> String? = { try { delay(10_000); "slow" } finally { cancelled = true } }
            val fast: suspend () -> String? = { "ready" }
            val result = withTimeout(2_000) {
                navigationCatalogs(mode,
                    aniList = if (mode == CatalogMode.ANILIST) fast else slow,
                    mangaBaka = if (mode == CatalogMode.MANGABAKA) fast else slow,
                    supplementWaitMillis = 20,
                )
            }
            assertEquals(if (mode == CatalogMode.ANILIST) "ready" to null else null to "ready", result)
            assertTrue(cancelled)
        }
    }

    @Test fun combinedCanStartWithEitherHealthyCatalog() = runBlocking {
        for (aniListFast in listOf(false, true)) {
            val fast: suspend () -> String? = { "ready" }
            val slow: suspend () -> String? = { delay(10_000); "slow" }
            val result = withTimeout(2_000) {
                navigationCatalogs(CatalogMode.COMBINED,
                    aniList = if (aniListFast) fast else slow,
                    mangaBaka = if (aniListFast) slow else fast,
                    supplementWaitMillis = 20,
                )
            }
            assertEquals(if (aniListFast) "ready" to null else null to "ready", result)
        }
    }

    @Test fun failedPreferredCatalogStillWaitsForAvailableFallback() = runBlocking {
        val result = navigationCatalogs(CatalogMode.MANGABAKA,
            aniList = { delay(30); "fallback" }, mangaBaka = { null }, supplementWaitMillis = 1)
        assertEquals("fallback" to null, result)
    }

    @Test fun aQueuedSupplementIsIncludedWithinItsAllowedBudget() = runBlocking {
        for (mode in CatalogMode.entries) {
            val result = navigationCatalogs(mode,
                aniList = { "aniList" }, mangaBaka = { delay(30); "mangaBaka" }, supplementWaitMillis = 1_000)
            assertEquals("aniList" to "mangaBaka", result)
        }
    }

    @Test fun anEmptyPreferredSearchDoesNotHideASlowerMatch() = runBlocking {
        val result = navigationCatalogs(CatalogMode.MANGABAKA,
            aniList = { delay(30); listOf(media(1)) }, mangaBaka = { emptyList<AnilistMedia>() },
            supplementWaitMillis = 1, hasContent = { it.isNotEmpty() })
        assertEquals(listOf(media(1)) to emptyList<AnilistMedia>(), result)
    }

    @Test fun modeSwitchCancellationStopsBothRequests() = runBlocking {
        val alStarted = CompletableDeferred<Unit>()
        val mbStarted = CompletableDeferred<Unit>()
        var cancelled = 0
        val job = launch {
            navigationCatalogs(CatalogMode.COMBINED,
                aniList = { try { alStarted.complete(Unit); awaitCancellation() } finally { cancelled++ } },
                mangaBaka = { try { mbStarted.complete(Unit); awaitCancellation() } finally { cancelled++ } },
            )
        }
        withTimeout(2_000) { alStarted.await(); mbStarted.await() }
        job.cancelAndJoin()
        assertEquals(2, cancelled)
    }

    private fun media(id: Int) = AnilistMedia(
        id = id, idMal = null, title = AnilistTitle(null, null, null, "Fictional story"),
        description = null, coverImage = "https://example.test/cover.jpg", bannerImage = null,
        chapters = 12, volumes = 2, format = "MANGA", status = "FINISHED", averageScore = 80,
        popularity = null, startDateYear = null, endDateYear = null, siteUrl = null,
        genres = emptyList(), synonyms = emptyList(), isAdult = false, updatedAtEpochSeconds = null,
    )
}
