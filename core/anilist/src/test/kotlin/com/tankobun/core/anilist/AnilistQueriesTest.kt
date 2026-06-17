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
}
