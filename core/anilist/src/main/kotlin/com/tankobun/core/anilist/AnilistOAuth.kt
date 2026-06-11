package com.tankobun.core.anilist

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class OAuthToken(
    val accessToken: String,
    val tokenType: String?,
    val expiresInSeconds: Long?,
    val state: String?,
)

object AnilistOAuth {
    const val AuthorizationEndpoint = "https://anilist.co/api/v2/oauth/authorize"

    fun authorizationUrl(
        clientId: String,
        @Suppress("UNUSED_PARAMETER") redirectUri: String,
        state: String? = null,
    ): String {
        val params = buildList {
            add("client_id=${clientId.encodeUrl()}")
            add("response_type=token")
            state?.takeIf { it.isNotBlank() }?.let { value -> add("state=${value.encodeUrl()}") }
        }
        return "$AuthorizationEndpoint?${params.joinToString("&")}"
    }

    fun parseRedirect(uri: String): OAuthToken? {
        val parsed = URI(uri)
        val params = parseParams(parsed.rawFragment.orEmpty()) + parseParams(parsed.rawQuery.orEmpty())
        val token = params["access_token"] ?: return null
        return OAuthToken(
            accessToken = token,
            tokenType = params["token_type"],
            expiresInSeconds = params["expires_in"]?.toLongOrNull(),
            state = params["state"],
        )
    }

    private fun parseParams(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split('&')
            .mapNotNull { part ->
                val pieces = part.split('=', limit = 2)
                if (pieces.size != 2) return@mapNotNull null
                pieces[0].decodeUrl() to pieces[1].decodeUrl()
            }
            .toMap()
    }

    private fun String.decodeUrl(): String =
        java.net.URLDecoder.decode(this, StandardCharsets.UTF_8.name())

    private fun String.encodeUrl(): String =
        URLEncoder.encode(this, StandardCharsets.UTF_8.name())
}
