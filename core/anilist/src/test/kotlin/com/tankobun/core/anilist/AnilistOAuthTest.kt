package com.tankobun.core.anilist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnilistOAuthTest {
    @Test
    fun authorizationUrlUsesAniListImplicitGrantParameters() {
        val url = AnilistOAuth.authorizationUrl(
            clientId = "40942",
            redirectUri = "tankobun://auth/anilist",
            state = "csrf-state",
        )

        assertEquals(
            "https://anilist.co/api/v2/oauth/authorize?client_id=40942&response_type=token&state=csrf-state",
            url,
        )
    }

    @Test
    fun parsesImplicitGrantFragment() {
        val token = AnilistOAuth.parseRedirect(
            "tankobun://auth/anilist#access_token=abc123&token_type=Bearer&expires_in=31536000&state=csrf-state",
        )

        assertEquals("abc123", token?.accessToken)
        assertEquals("Bearer", token?.tokenType)
        assertEquals(31_536_000L, token?.expiresInSeconds)
        assertEquals("csrf-state", token?.state)
    }

    @Test
    fun returnsNullWhenNoTokenExists() {
        assertNull(AnilistOAuth.parseRedirect("tankobun://auth/anilist?error=access_denied"))
    }

    @Test
    fun rejectsMalformedOrUnexpectedCallbacksWithoutThrowing() {
        listOf(
            "tankobun://auth/anilist#access_token=%",
            "tankobun://auth/anilist#access_token=%GG",
            "tankobun://auth/anilist#access_token=",
            "tankobun://auth/other#access_token=abc&state=csrf-state",
            "tankobun://other/anilist#access_token=abc&state=csrf-state",
            "https://auth/anilist#access_token=abc&state=csrf-state",
            "tankobun://user@auth/anilist#access_token=abc",
            "tankobun://auth:123/anilist#access_token=abc",
            "tankobun://auth/anilist#access_token=" + "a".repeat(16 * 1024),
        ).forEach { assertNull(it, AnilistOAuth.parseRedirect(it)) }
    }
}
