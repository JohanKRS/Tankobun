package com.tankobun.core.anilist

import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
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
        return AnilistJsonMapper.searchPage(data)
    }

    suspend fun mediaDetails(mediaId: Int, accessToken: String?): AnilistMedia {
        val data = graphQlClient.execute(
            query = AnilistQueries.MediaDetails,
            variables = buildJsonObject { put("id", mediaId) },
            accessToken = accessToken,
        )
        return AnilistJsonMapper.media(requireNotNull(data["Media"]))
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
