package com.tankobun.core.anilist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnilistQueriesTest {
    @Test
    fun mangaListByUserIdDoesNotSendUnusedUsernameArgument() {
        assertTrue(AnilistQueries.MangaListCollectionByUserId.contains("userId: \$userId"))
        assertFalse(AnilistQueries.MangaListCollectionByUserId.contains("userName"))
    }

    @Test
    fun mangaListByUsernameDoesNotSendUnusedUserIdArgument() {
        assertTrue(AnilistQueries.MangaListCollectionByUserName.contains("userName: \$userName"))
        assertFalse(AnilistQueries.MangaListCollectionByUserName.contains("userId"))
    }

    @Test
    fun listEntryQueriesIncludeHiddenFromStatusLists() {
        assertTrue(AnilistQueries.MangaListCollectionByUserId.contains("hiddenFromStatusLists"))
        assertTrue(AnilistQueries.MangaListCollectionByUserName.contains("hiddenFromStatusLists"))
        assertTrue(AnilistQueries.MediaDetails.contains("hiddenFromStatusLists"))
        assertTrue(AnilistQueries.MediaListEntry.contains("hiddenFromStatusLists"))
        assertTrue(AnilistQueries.SaveMediaListEntry.contains("\$hiddenFromStatusLists: Boolean"))
        assertTrue(AnilistQueries.SaveMediaListEntry.contains("hiddenFromStatusLists: \$hiddenFromStatusLists"))
    }

    @Test
    fun mediaListEntryRefreshIsScopedToOneMangaAndIncludesScoreFormat() {
        assertTrue(AnilistQueries.MediaListEntry.contains("Media(id: \$id, type: MANGA)"))
        assertTrue(AnilistQueries.MediaListEntry.contains("score(format: \$scoreFormat)"))
        assertFalse(AnilistQueries.MediaListEntry.contains("recommendations"))
    }

    @Test
    fun deleteMediaListEntryMutationUsesEntryId() {
        assertTrue(AnilistQueries.DeleteMediaListEntry.contains("mutation DeleteMediaListEntry(\$id: Int!)"))
        assertTrue(AnilistQueries.DeleteMediaListEntry.contains("DeleteMediaListEntry(id: \$id)"))
        assertTrue(AnilistQueries.DeleteMediaListEntry.contains("deleted"))
    }

    @Test
    fun homeInitialBatchesAllGenresWithCompactHighlightFields() {
        val query = AnilistQueries.homeInitial(
            genres = listOf("Fantasy", "Slice of Life"),
            perPage = 3,
        )

        assertTrue(query.contains("trending: Page(page: 1, perPage: 5)"))
        assertTrue(query.contains("genre0Page1: Page(page: 1, perPage: 3)"))
        assertTrue(query.contains("genre1Page1: Page(page: 1, perPage: 3)"))
        assertTrue(query.contains("genre: \"Slice of Life\""))
        assertTrue(query.contains("sort: TRENDING_DESC"))
        assertFalse(query.contains("characters(role: MAIN"))
        assertFalse(query.contains("sort: POPULARITY_DESC"))
        assertFalse(query.contains("Page2"))
    }

    @Test
    fun homeGenreCandidatesDoesNotRepeatTrendingPayload() {
        val query = AnilistQueries.homeGenreCandidates(listOf("Fantasy"), perPage = 3)

        assertTrue(query.contains("genre0Page1: Page(page: 1, perPage: 3)"))
        assertFalse(query.contains("trending:"))
        assertFalse(query.contains("HomeTrendingMedia"))
    }

    @Test
    fun mediaGenresUsesAniListGenreCollection() {
        assertTrue(AnilistQueries.MediaGenres.contains("GenreCollection"))
    }

    @Test
    fun browseLandingBatchesAllFourSections() {
        assertTrue(AnilistQueries.BrowseLanding.contains("trending: Page"))
        assertTrue(AnilistQueries.BrowseLanding.contains("popular: Page"))
        assertTrue(AnilistQueries.BrowseLanding.contains("popularManhwa: Page"))
        assertTrue(AnilistQueries.BrowseLanding.contains("topManga: Page"))
    }

    @Test
    fun fallbackSearchIsBoundedToTwoSmallCandidateSets() {
        assertTrue(AnilistQueries.SearchFallbackManga.contains("popular: Page(page: 1, perPage: 25)"))
        assertTrue(AnilistQueries.SearchFallbackManga.contains("trending: Page(page: 1, perPage: 25)"))
        assertFalse(AnilistQueries.SearchFallbackManga.contains("${'$'}page"))
    }

    @Test
    fun mangaBatchUsesVariablesInsteadOfEmbeddingIds() {
        val query = AnilistQueries.mangaByIds(3)

        assertTrue(query.contains("${'$'}id0: Int!"))
        assertTrue(query.contains("media2: Media(id: ${'$'}id2, type: MANGA)"))
    }
}
