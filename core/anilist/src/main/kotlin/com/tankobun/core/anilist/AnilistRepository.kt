package com.tankobun.core.anilist

import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistMediaDetails
import com.tankobun.core.model.AnilistMediaPage
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.AnilistRecommendationPage
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.MediaStatus
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonNull
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
        val options = viewer["options"]?.jsonObject
        val mediaListOptions = viewer["mediaListOptions"]?.jsonObject
        val mangaListOptions = mediaListOptions?.get("mangaList")?.jsonObject
        return AnilistViewer(
            id = requireNotNull(viewer["id"]?.jsonPrimitive?.intOrNull),
            name = requireNotNull(viewer["name"]?.jsonPrimitive?.content),
            avatarUrl = viewer["avatar"]?.jsonObject?.get("large")?.jsonPrimitive?.content,
            scoreFormat = mediaListOptions
                ?.stringOrNull("scoreFormat")
                ?.toAnilistScoreFormat()
                ?: AnilistScoreFormat.POINT_100,
            titleLanguage = options
                ?.stringOrNull("titleLanguage")
                ?.toAnilistTitleLanguage()
                ?: AnilistTitleLanguage.ROMAJI,
            mangaCustomLists = mangaListOptions?.stringArray("customLists").orEmpty(),
        )
    }

    suspend fun searchManga(
        query: String,
        page: Int = 1,
        perPage: Int = 50,
        accessToken: String? = null,
    ): List<AnilistMedia> =
        searchMangaPage(query = query, page = page, perPage = perPage, accessToken = accessToken).media

    suspend fun searchMangaPage(
        query: String,
        page: Int = 1,
        perPage: Int = 50,
        accessToken: String? = null,
    ): AnilistMediaPage {
        query.extractAniListMangaId()?.let { mediaId ->
            val media = runCatching { listOf(mediaDetailsWithEntry(mediaId, accessToken = accessToken).media) }
                .getOrDefault(emptyList())
            return AnilistMediaPage(media = media, currentPage = 1, hasNextPage = false)
        }

        val data = graphQlClient.execute(
            query = AnilistQueries.SearchManga,
            variables = buildJsonObject {
                put("search", query)
                put("page", page)
                put("perPage", perPage)
            },
            accessToken = accessToken,
        )
        val directResults = AnilistJsonMapper.searchMediaPage(data)
        return if (page == 1 && directResults.media.isEmpty()) {
            AnilistMediaPage(
                media = fallbackSearchManga(query, accessToken = accessToken),
                currentPage = 1,
                hasNextPage = false,
            )
        } else {
            directResults
        }
    }

    suspend fun browseManga(
        search: String? = null,
        genres: Set<String> = emptySet(),
        tags: Set<String> = emptySet(),
        format: String? = null,
        status: String? = null,
        countryOfOrigin: String? = null,
        year: Int? = null,
        sort: String = "TRENDING_DESC",
        page: Int = 1,
        perPage: Int = 50,
        accessToken: String? = null,
    ): List<AnilistMedia> =
        browseMangaPage(
            search = search,
            genres = genres,
            tags = tags,
            format = format,
            status = status,
            countryOfOrigin = countryOfOrigin,
            year = year,
            sort = sort,
            page = page,
            perPage = perPage,
            accessToken = accessToken,
        ).media

    suspend fun browseMangaPage(
        search: String? = null,
        genres: Set<String> = emptySet(),
        tags: Set<String> = emptySet(),
        format: String? = null,
        status: String? = null,
        countryOfOrigin: String? = null,
        year: Int? = null,
        sort: String = "TRENDING_DESC",
        page: Int = 1,
        perPage: Int = 50,
        accessToken: String? = null,
    ): AnilistMediaPage {
        val normalizedSearch = search?.trim().orEmpty()
        if (
            genres.isEmpty() &&
            tags.isEmpty() &&
            format == null &&
            status == null &&
            countryOfOrigin == null &&
            year == null
        ) {
            normalizedSearch.extractAniListMangaId()?.let { mediaId ->
                val media = runCatching { listOf(mediaDetailsWithEntry(mediaId, accessToken = accessToken).media) }
                    .getOrDefault(emptyList())
                return AnilistMediaPage(media = media, currentPage = 1, hasNextPage = false)
            }
        }

        val data = graphQlClient.execute(
            query = AnilistQueries.BrowseManga,
            variables = buildJsonObject {
                put("page", page)
                put("perPage", perPage)
                if (normalizedSearch.isNotBlank()) put("search", normalizedSearch)
                if (genres.isNotEmpty()) {
                    put("genres", buildJsonArray { genres.sorted().forEach { add(it) } })
                }
                if (tags.isNotEmpty()) {
                    put("tags", buildJsonArray { tags.sorted().forEach { add(it) } })
                }
                if (!format.isNullOrBlank()) put("format", format)
                if (!status.isNullOrBlank()) put("status", status)
                if (!countryOfOrigin.isNullOrBlank()) put("countryOfOrigin", countryOfOrigin)
                if (year != null) {
                    put("startDateGreater", year * 10_000 + 101)
                    put("startDateLesser", year * 10_000 + 12_31)
                }
                put("sort", buildJsonArray { add(sort) })
            },
            accessToken = accessToken,
        )
        return AnilistJsonMapper.searchMediaPage(data)
    }

    suspend fun staffManga(
        staffName: String,
        sort: String = "POPULARITY_DESC",
        page: Int = 1,
        perPage: Int = 50,
        accessToken: String? = null,
    ): List<AnilistMedia> =
        staffMangaPage(
            staffName = staffName,
            sort = sort,
            page = page,
            perPage = perPage,
            accessToken = accessToken,
        ).media

    suspend fun staffMangaPage(
        staffName: String,
        sort: String = "POPULARITY_DESC",
        page: Int = 1,
        perPage: Int = 50,
        accessToken: String? = null,
    ): AnilistMediaPage {
        val normalizedStaffName = staffName.trim()
        if (normalizedStaffName.isBlank()) return AnilistMediaPage(emptyList(), currentPage = 1, hasNextPage = false)
        val data = graphQlClient.execute(
            query = AnilistQueries.StaffManga,
            variables = buildJsonObject {
                put("search", normalizedStaffName)
                put("page", page)
                put("perPage", perPage)
                put("sort", buildJsonArray { add(sort) })
            },
            accessToken = accessToken,
        )
        return AnilistJsonMapper.staffMediaPage(data)
    }

    suspend fun mediaTags(): List<AnilistMediaTag> {
        val data = graphQlClient.execute(query = AnilistQueries.MediaTags)
        return AnilistJsonMapper.mediaTags(data)
            .distinctBy { it.name.lowercase() }
            .sortedWith(compareBy<AnilistMediaTag> { it.category.orEmpty() }.thenBy { it.name })
    }

    suspend fun mangaById(mediaId: Int, accessToken: String? = null): AnilistMedia? {
        val data = graphQlClient.execute(
            query = AnilistQueries.MangaById,
            variables = buildJsonObject {
                put("id", mediaId)
            },
            accessToken = accessToken,
        )
        val media = data["Media"] ?: return null
        if (media is JsonNull) return null
        return AnilistJsonMapper.media(media)
    }

    suspend fun mangaByMalId(idMal: Int, accessToken: String? = null): AnilistMedia? {
        val data = graphQlClient.execute(
            query = AnilistQueries.MangaByMalId,
            variables = buildJsonObject {
                put("idMal", idMal)
            },
            accessToken = accessToken,
        )
        val media = data["Media"] ?: return null
        if (media is JsonNull) return null
        return AnilistJsonMapper.media(media)
    }

    private suspend fun fallbackSearchManga(query: String, accessToken: String?): List<AnilistMedia> {
        val normalizedQuery = query.normalizedSearchTokens()
        if (normalizedQuery.isEmpty()) return emptyList()

        val candidates = mutableListOf<AnilistMedia>()
        SEARCH_FALLBACK_SORTS.forEach { sort ->
            for (page in 1..SEARCH_FALLBACK_MAX_PAGES_PER_SORT) {
                candidates += AnilistJsonMapper.searchPage(
                    graphQlClient.execute(
                        query = AnilistQueries.SearchFallbackMangaPage,
                        variables = buildJsonObject {
                            put("page", page)
                            put("sort", buildJsonArray { add(sort) })
                        },
                        accessToken = accessToken,
                    ),
                )
                val rankedCount = candidates
                    .distinctBy { it.id }
                    .count { it.searchFallbackScore(normalizedQuery) > 0 }
                if (rankedCount >= SEARCH_FALLBACK_TARGET_RESULTS) break
            }
        }

        return candidates
            .distinctBy { it.id }
            .mapNotNull { media ->
                val score = media.searchFallbackScore(normalizedQuery)
                if (score <= 0) null else media to score
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    suspend fun mediaDetails(mediaId: Int, accessToken: String?): AnilistMedia {
        return mediaDetailsWithEntry(mediaId, accessToken).media
    }

    suspend fun mediaDetailsWithEntry(
        mediaId: Int,
        accessToken: String?,
        scoreFormat: AnilistScoreFormat = AnilistScoreFormat.POINT_100,
        recommendationsPage: Int = 1,
        recommendationsPerPage: Int = DEFAULT_RECOMMENDATIONS_PER_PAGE,
    ): AnilistMediaDetails {
        val data = graphQlClient.execute(
            query = AnilistQueries.MediaDetails,
            variables = buildJsonObject {
                put("id", mediaId)
                put("scoreFormat", scoreFormat.name)
                put("recommendationsPage", recommendationsPage)
                put("recommendationsPerPage", recommendationsPerPage)
            },
            accessToken = accessToken,
        )
        return AnilistJsonMapper.mediaDetails(data)
    }

    suspend fun mediaRecommendations(
        mediaId: Int,
        page: Int,
        perPage: Int = DEFAULT_RECOMMENDATIONS_PER_PAGE,
        accessToken: String?,
    ): AnilistRecommendationPage {
        val data = graphQlClient.execute(
            query = AnilistQueries.MediaRecommendations,
            variables = buildJsonObject {
                put("id", mediaId)
                put("page", page)
                put("perPage", perPage)
            },
            accessToken = accessToken,
        )
        return AnilistJsonMapper.mediaRecommendations(data)
    }

    suspend fun mangaList(
        accessToken: String,
        userId: Int? = null,
        userName: String? = null,
        scoreFormat: AnilistScoreFormat = AnilistScoreFormat.POINT_100,
    ): List<Pair<AnilistMedia, AnilistListEntry>> {
        val (query, variables) = when {
            userId != null -> AnilistQueries.MangaListCollectionByUserId to buildJsonObject {
                put("userId", userId)
                put("scoreFormat", scoreFormat.name)
            }
            !userName.isNullOrBlank() -> AnilistQueries.MangaListCollectionByUserName to buildJsonObject {
                put("userName", userName)
                put("scoreFormat", scoreFormat.name)
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
        scoreFormat: AnilistScoreFormat = AnilistScoreFormat.POINT_100,
    ): AnilistListEntry {
        val data = graphQlClient.execute(
            query = AnilistQueries.SaveMediaListEntry,
            variables = buildJsonObject {
                put("mediaId", mediaId)
                if (status != null) put("status", status.name)
                if (progress != null) put("progress", progress)
                if (score != null) put("score", score)
                if (notes != null) put("notes", notes)
                if (private != null) put("private", private)
                if (customLists != null) {
                    put("customLists", buildJsonArray { customLists.forEach { add(it) } })
                }
                put("scoreFormat", scoreFormat.name)
            },
            accessToken = accessToken,
        )
        return AnilistJsonMapper.listEntry(requireNotNull(data["SaveMediaListEntry"]))
    }

    suspend fun updateMangaCustomLists(
        accessToken: String,
        customLists: List<String>,
    ): List<String> {
        val data = graphQlClient.execute(
            query = AnilistQueries.UpdateMangaCustomLists,
            variables = buildJsonObject {
                put("customLists", buildJsonArray { customLists.forEach { add(it) } })
            },
            accessToken = accessToken,
        )
        return data["UpdateUser"]
            ?.jsonObject
            ?.get("mediaListOptions")
            ?.jsonObject
            ?.get("mangaList")
            ?.jsonObject
            ?.stringArray("customLists")
            .orEmpty()
    }
}

private val SEARCH_FALLBACK_SORTS = listOf(
    "FAVOURITES_DESC",
    "POPULARITY_DESC",
    "TRENDING_DESC",
    "UPDATED_AT_DESC",
)

private const val SEARCH_FALLBACK_MAX_PAGES_PER_SORT = 10
private const val SEARCH_FALLBACK_TARGET_RESULTS = 20
private const val DEFAULT_RECOMMENDATIONS_PER_PAGE = 18

private fun String.extractAniListMangaId(): Int? {
    trim().toIntOrNull()?.let { return it }
    return Regex("""anilist\.co/manga/(\d+)""", RegexOption.IGNORE_CASE)
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
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

private fun String.toAnilistScoreFormat(): AnilistScoreFormat =
    runCatching { AnilistScoreFormat.valueOf(this) }.getOrDefault(AnilistScoreFormat.POINT_100)

private fun String.toAnilistTitleLanguage(): AnilistTitleLanguage =
    runCatching { AnilistTitleLanguage.valueOf(this) }.getOrDefault(AnilistTitleLanguage.ROMAJI)

private fun kotlinx.serialization.json.JsonObject.stringOrNull(name: String): String? =
    this[name]?.jsonPrimitive?.content

private fun kotlinx.serialization.json.JsonObject.stringArray(name: String): List<String> =
    this[name]?.let { value ->
        value.takeIf { it !is kotlinx.serialization.json.JsonNull }
            ?.let { it as? kotlinx.serialization.json.JsonArray }
            ?.mapNotNull { it.jsonPrimitive.content }
    }.orEmpty()
