package com.tankobun.core.anilist

import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.MediaStatus
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AnilistRepository(
    private val graphQlClient: AnilistGraphQlClient,
) {
    suspend fun viewer(accessToken: String): AnilistViewer {
        val data = graphQlClient.execute(
            query = AnilistQueries.Viewer,
            accessToken = accessToken,
        )
        val viewer = requireNotNull(data["Viewer"]).jsonObject
        return AnilistViewer(
            id = requireNotNull(viewer["id"]?.jsonPrimitive?.intOrNull),
            name = requireNotNull(viewer["name"]?.jsonPrimitive?.content),
            avatarUrl = viewer["avatar"]?.jsonObject?.get("large")?.jsonPrimitive?.content,
        )
    }

    suspend fun searchManga(query: String, page: Int = 1): List<AnilistMedia> {
        val data = graphQlClient.execute(
            query = AnilistQueries.SearchManga,
            variables = buildJsonObject {
                put("search", query)
                put("page", page)
            },
        )
        val directResults = AnilistJsonMapper.searchPage(data)
        return directResults.ifEmpty {
            fallbackSearchManga(query)
        }
    }

    private suspend fun fallbackSearchManga(query: String): List<AnilistMedia> {
        val normalizedQuery = query.normalizedSearchTokens()
        if (normalizedQuery.isEmpty()) return emptyList()

        return (1..3)
            .flatMap { page ->
                val data = graphQlClient.execute(
                    query = AnilistQueries.PopularMangaPage,
                    variables = buildJsonObject { put("page", page) },
                )
                AnilistJsonMapper.searchPage(data)
            }
            .distinctBy { it.id }
            .mapNotNull { media ->
                val score = media.searchFallbackScore(normalizedQuery)
                if (score <= 0) null else media to score
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    suspend fun mediaDetails(mediaId: Int, accessToken: String?): AnilistMedia {
        return mediaDetailsWithEntry(mediaId, accessToken).first
    }

    suspend fun mediaDetailsWithEntry(
        mediaId: Int,
        accessToken: String?,
    ): Triple<AnilistMedia, AnilistListEntry?, List<AnilistRecommendation>> {
        val data = graphQlClient.execute(
            query = AnilistQueries.MediaDetails,
            variables = buildJsonObject { put("id", mediaId) },
            accessToken = accessToken,
        )
        return AnilistJsonMapper.mediaDetails(data)
    }

    suspend fun mangaList(
        accessToken: String,
        userId: Int? = null,
        userName: String? = null,
    ): List<Pair<AnilistMedia, AnilistListEntry>> {
        val (query, variables) = when {
            userId != null -> AnilistQueries.MangaListCollectionByUserId to buildJsonObject {
                put("userId", userId)
            }
            !userName.isNullOrBlank() -> AnilistQueries.MangaListCollectionByUserName to buildJsonObject {
                put("userName", userName)
            }
            else -> error("AniList manga list needs a user id or username")
        }

        val data = graphQlClient.execute(
            query = query,
            variables = variables,
            accessToken = accessToken,
        )
        return AnilistJsonMapper.listCollection(data)
    }

    suspend fun saveListEntry(
        accessToken: String,
        mediaId: Int,
        status: MediaStatus?,
        progress: Int?,
        score: Double?,
        notes: String?,
        private: Boolean?,
        customLists: List<String>?,
    ): AnilistListEntry {
        val data = graphQlClient.execute(
            query = AnilistQueries.SaveMediaListEntry,
            variables = buildJsonObject {
                put("mediaId", mediaId)
                if (status != null) put("status", status.name) else put("status", JsonNull)
                if (progress != null) put("progress", progress) else put("progress", JsonNull)
                if (score != null) put("score", score) else put("score", JsonNull)
                if (notes != null) put("notes", notes) else put("notes", JsonNull)
                if (private != null) put("private", private) else put("private", JsonNull)
                if (customLists != null) {
                    put("customLists", buildJsonArray { customLists.forEach { add(it) } })
                } else {
                    put("customLists", JsonNull)
                }
            },
            accessToken = accessToken,
        )
        return AnilistJsonMapper.listEntry(requireNotNull(data["SaveMediaListEntry"]))
    }
}

private fun String.normalizedSearchTokens(): List<String> =
    lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.length >= 2 }

private fun AnilistMedia.searchFallbackScore(tokens: List<String>): Int {
    val fields = listOfNotNull(
        title.userPreferred,
        title.romaji,
        title.english,
        title.native,
    ) + synonyms
    val normalizedFields = fields.map { it.lowercase() }
    var score = 0
    tokens.forEach { token ->
        normalizedFields.forEach { field ->
            when {
                field == token -> score += 100
                field.startsWith(token) -> score += 40
                field.contains(token) -> score += 20
            }
        }
    }
    return score
}
