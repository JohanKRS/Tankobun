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
}
