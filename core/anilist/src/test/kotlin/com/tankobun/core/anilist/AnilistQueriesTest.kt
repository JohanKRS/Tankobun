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
        assertTrue(AnilistQueries.SaveMediaListEntry.contains("\$hiddenFromStatusLists: Boolean"))
        assertTrue(AnilistQueries.SaveMediaListEntry.contains("hiddenFromStatusLists: \$hiddenFromStatusLists"))
    }

    @Test
    fun deleteMediaListEntryMutationUsesEntryId() {
        assertTrue(AnilistQueries.DeleteMediaListEntry.contains("mutation DeleteMediaListEntry(\$id: Int!)"))
        assertTrue(AnilistQueries.DeleteMediaListEntry.contains("DeleteMediaListEntry(id: \$id)"))
        assertTrue(AnilistQueries.DeleteMediaListEntry.contains("deleted"))
    }

    @Test
    fun homeGenreCandidatesBatchGenresAcrossRequestedPages() {
        val query = AnilistQueries.homeGenreCandidates(
            genres = listOf("Fantasy", "Slice of Life"),
            pages = 1..3,
            perPage = 50,
        )

        assertTrue(query.contains("genre0Page1: Page(page: 1, perPage: 50)"))
        assertTrue(query.contains("genre0Page3: Page(page: 3, perPage: 50)"))
        assertTrue(query.contains("genre1Page2: Page(page: 2, perPage: 50)"))
        assertTrue(query.contains("genre: \"Slice of Life\""))
        assertTrue(query.contains("sort: TRENDING_DESC"))
        assertFalse(query.contains("sort: POPULARITY_DESC"))
        assertFalse(query.contains("Page4"))
    }

    @Test
    fun mediaGenresUsesAniListGenreCollection() {
        assertTrue(AnilistQueries.MediaGenres.contains("GenreCollection"))
    }
}
