package com.tankobun.app.catalog

import com.tankobun.core.model.*
import com.tankobun.core.database.toEntity
import com.tankobun.core.database.toModel
import com.tankobun.app.home.withHomeTrendingLimit
import com.tankobun.app.logic.browseCacheKey
import com.tankobun.app.state.TankobunUiState
import org.junit.Assert.*
import org.junit.Test

class CatalogTaxonomyTest {
    private val martial = CatalogTag("mb:29", "Martial Arts", "Themes", isGenre = true, mangaBakaId = 29, parentId = 542)
    private val root = CatalogTag("mb:542", "Themes", mangaBakaId = 542)
    private fun taxonomy() = CatalogTaxonomy.merge(
        listOf(AnilistMediaTag("Martial Arts", "Theme-Action", false), AnilistMediaTag("Boys' Love", "Theme-Romance", false),
            AnilistMediaTag("AniList exclusive", "Theme", false)),
        listOf("Action", "Fantasy"),
        listOf(root, martial, CatalogTag("mb:39", "Action", "Themes", isGenre = true, mangaBakaId = 39, parentId = 542),
            CatalogTag("mb:180", "Boys Love", "Themes", isGenre = true, mangaBakaId = 180, parentId = 542),
            CatalogTag("mb:7000", "MangaBaka exclusive", "Themes", mangaBakaId = 7000, parentId = 542)),
    )

    @Test fun mangaBakaGenresMapToAniListGenresOrTagsWithoutChangingTheirMeaning() {
        val taxonomy = taxonomy()
        val plan = taxonomy.plan(setOf("mb:29", "mb:39"), setOf("mb:180"))
        assertTrue(plan.canQueryAniList)
        assertTrue(plan.canQueryMangaBaka)
        assertEquals(setOf("Action"), plan.anilistGenres)
        assertEquals(setOf("Martial Arts", "Boys' Love"), plan.anilistTags)
        assertEquals(setOf(29, 39, 180), plan.mangaBakaTagIds)
        assertEquals("mb:29", taxonomy.resolve("Martial Arts")?.key)
        assertEquals("mb:180", taxonomy.resolve("Boys’ Love")?.key)
    }

    @Test fun unsupportedFiltersNeverTurnIntoAnUnfilteredProviderQuery() {
        val taxonomy = taxonomy()
        assertFalse(taxonomy.plan(emptySet(), setOf("mb:7000")).canQueryAniList)
        assertTrue(taxonomy.plan(emptySet(), setOf("mb:7000")).canQueryMangaBaka)
        assertFalse(taxonomy.plan(emptySet(), setOf("AniList exclusive")).canQueryMangaBaka)
        assertTrue(taxonomy.plan(emptySet(), setOf("AniList exclusive")).canQueryAniList)
        val unknown = taxonomy.plan(emptySet(), setOf("not in either catalog"))
        assertFalse(unknown.canQueryAniList)
        assertFalse(unknown.canQueryMangaBaka)
    }

    @Test fun homonymsKeepIndependentIdsAndAreNotGuessedAsAniListEquivalents() {
        val taxonomy = CatalogTaxonomy.merge(listOf(AnilistMediaTag("Chibi", "Technical", false)), emptyList(), listOf(
            CatalogTag("mb:515", "Chibi", "Character Archetype > Moe", mangaBakaId = 515),
            CatalogTag("mb:1287", "Chibi", "Work Info > Art Style", mangaBakaId = 1287),
        ))
        assertEquals(3, taxonomy.tags.size)
        assertFalse(taxonomy.plan(emptySet(), setOf("mb:515")).canQueryAniList)
        val keys = taxonomy.mediaFilterKeys(media().copy(mangaBakaTagIds = listOf(515)))
        assertTrue(taxonomy.matchesAny(keys, setOf("mb:515")))
        assertFalse(taxonomy.matchesAny(keys, setOf("mb:1287")))
    }

    @Test fun libraryUsesSavedAniListTagsForMappedGenresAndMangaBakaAncestors() {
        val taxonomy = taxonomy()
        val keys = taxonomy.mediaFilterKeys(media().copy(tags = listOf("Martial Arts")))
        assertTrue(taxonomy.matchesAny(keys, setOf("mb:29")))
        assertTrue(taxonomy.matchesAny(keys, setOf("mb:542")))
        assertFalse(taxonomy.matchesAny(keys, setOf("mb:7000")))
        assertTrue(taxonomy.matchesAny(taxonomy.mediaFilterKeys(media().copy(mangaBakaTagIds = listOf(7000))), setOf("mb:542")))
    }

    @Test fun enrichmentKeepsPrimaryArtworkAndPersistsBothTaxonomiesThroughTheDatabase() {
        val primary = media().copy(genres = listOf("Action"), tags = listOf("School"), coverImage = "primary")
        val secondary = media().copy(mangaBakaId = 9, genres = listOf("Action", "Martial Arts"),
            tags = listOf("School", "MangaBaka exclusive"), mangaBakaTagIds = listOf(29, 7000), coverImage = "secondary")
        val combined = primary.withFallbackDetails(secondary)
        assertEquals(listOf("Action", "Martial Arts"), combined.genres)
        assertEquals(listOf("School", "MangaBaka exclusive"), combined.tags)
        assertEquals("primary", combined.coverImage)
        assertEquals(combined, combined.toEntity(123).toModel())
        assertEquals(combined.mangaBakaTagIds, primary.withFallbackDetails(combined).mangaBakaTagIds)
    }

    @Test fun mangaBakaFallbackAndOlderHomeCacheKeepFiveHighlightsWithoutLosingGenreCandidates() {
        val candidates = (1..30).map { media().copy(id = it) }
        val genre = AnilistGenreHighlight("Martial Arts", candidates.last())
        val feed = AnilistHomeFeed(candidates, listOf(genre)).withHomeTrendingLimit()
        assertEquals((1..5).toList(), feed.trending.map { it.id })
        assertEquals(listOf(genre), feed.genreHighlights)
    }

    @Test fun libraryFiltersCombineAlternativesWithInclusiveAndOpenYearRanges() {
        val filters = CatalogSearchFilters(formats = setOf("MANGA", "ONE_SHOT"), countries = setOf("JP", "KR"),
            statuses = setOf("FINISHED", "RELEASING"), years = PublicationYears(2000, 2010))
        val base = media().copy(format = "MANGA", countryOfOrigin = "JP", status = "FINISHED", startDateYear = 2000)
        assertTrue(filters.matches(base))
        assertTrue(filters.matches(base.copy(format = "ONE_SHOT", countryOfOrigin = "KR", status = "RELEASING", startDateYear = 2010)))
        assertFalse(filters.matches(base.copy(format = "NOVEL")))
        assertFalse(filters.matches(base.copy(countryOfOrigin = "CN")))
        assertFalse(filters.matches(base.copy(startDateYear = null)))
        assertFalse(filters.matches(base.copy(startDateYear = 1999)))
        assertFalse(filters.matches(base.copy(startDateYear = 2011)))
        assertTrue(filters.copy(years = PublicationYears(to = 2010)).matches(base.copy(startDateYear = 1950)))
        assertTrue(filters.copy(years = PublicationYears(from = 2000)).matches(base.copy(startDateYear = 2026)))
    }

    @Test fun filterCacheKeysIgnoreSelectionOrderButSeparateRangesAndValues() {
        val base = TankobunUiState(browseSelection = CatalogSearchFilters(countries = linkedSetOf("JP", "KR"), years = PublicationYears(2000, 2010)))
        assertEquals(base.browseCacheKey(), base.copy(browseSelection = base.browseSelection.copy(countries = linkedSetOf("KR", "JP"))).browseCacheKey())
        assertNotEquals(base.browseCacheKey(), base.copy(browseSelection = base.browseSelection.copy(years = PublicationYears(2000, 2011))).browseCacheKey())
        assertNotEquals(base.browseCacheKey(), base.copy(browseSelection = base.browseSelection.copy(countries = setOf("JP"))).browseCacheKey())
    }

    private fun media() = AnilistMedia(
        id = 1, idMal = null, title = AnilistTitle(null, null, null, "Fictional"), description = null,
        coverImage = null, bannerImage = null, chapters = null, volumes = null, format = "MANGA", status = null,
        averageScore = null, popularity = null, startDateYear = null, endDateYear = null, siteUrl = null,
        genres = emptyList(), synonyms = emptyList(), isAdult = false, updatedAtEpochSeconds = null,
    )
}
