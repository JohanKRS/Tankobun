package com.tankobun.app.sharing

import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitle
import org.json.JSONArray
import org.json.JSONObject

internal const val RECOMMENDATION_SHARE_MIME_TYPE = "application/vnd.tankobun.recommendations+json"
internal const val RECOMMENDATION_SHARE_EXTENSION = "tankobun-recs"

internal data class RecommendationSharePayload(
    val suggestedListName: String,
    val createdAtEpochMillis: Long,
    val items: List<AnilistMedia>,
)

data class RecommendationImportPreview(
    val suggestedListName: String,
    val items: List<RecommendationImportPreviewItem>,
)

data class RecommendationImportPreviewItem(
    val media: AnilistMedia,
    val alreadyInLibrary: Boolean,
)

internal fun buildRecommendationShareJson(
    suggestedListName: String,
    items: List<AnilistMedia>,
    createdAtEpochMillis: Long = System.currentTimeMillis(),
): String =
    JSONObject()
        .put("type", RECOMMENDATION_SHARE_TYPE)
        .put("version", RECOMMENDATION_SHARE_VERSION)
        .put("createdAtEpochMillis", createdAtEpochMillis)
        .put("suggestedListName", suggestedListName.trim().ifBlank { DEFAULT_RECOMMENDATION_LIST_NAME })
        .put("items", items.distinctBy { it.id }.sortedBy { it.title.userPreferred.lowercase() }.map { it.toJson() }.toJsonArray())
        .toString(JSON_INDENT)

internal fun parseRecommendationShareJson(text: String): RecommendationSharePayload {
    val root = JSONObject(text)
    check(root.optString("type") == RECOMMENDATION_SHARE_TYPE) { "Unsupported Tankobun recommendations file" }
    check(root.optInt("version") == RECOMMENDATION_SHARE_VERSION) { "Unsupported Tankobun recommendations version" }
    val items = root.optJSONArray("items").objectValues().map { it.toMedia() }.distinctBy { it.id }
    check(items.isNotEmpty()) { "No recommendations found" }
    return RecommendationSharePayload(
        suggestedListName = root.optString("suggestedListName")
            .takeIf { it.isNotBlank() }
            ?: DEFAULT_RECOMMENDATION_LIST_NAME,
        createdAtEpochMillis = root.optLong("createdAtEpochMillis", 0L),
        items = items,
    )
}

internal fun RecommendationSharePayload.toImportPreview(existingMediaIds: Set<Int>): RecommendationImportPreview =
    RecommendationImportPreview(
        suggestedListName = suggestedListName,
        items = items.map { media ->
            RecommendationImportPreviewItem(media = media, alreadyInLibrary = media.id in existingMediaIds)
        },
    )

private fun AnilistMedia.toJson(): JSONObject =
    JSONObject()
        .put("mediaId", id)
        .putNullable("idMal", idMal)
        .put(
            "title",
            JSONObject()
                .putNullable("romaji", title.romaji)
                .putNullable("english", title.english)
                .putNullable("native", title.native)
                .put("userPreferred", title.userPreferred),
        )
        .putNullable("coverImage", coverImage)
        .putNullable("bannerImage", bannerImage)
        .putNullable("chapters", chapters)
        .putNullable("volumes", volumes)
        .putNullable("format", format)
        .putNullable("countryOfOrigin", countryOfOrigin)
        .putNullable("status", status)
        .putNullable("averageScore", averageScore)
        .putNullable("popularity", popularity)
        .putNullable("startDateYear", startDateYear)
        .putNullable("endDateYear", endDateYear)
        .putNullable("siteUrl", siteUrl)
        .put("genres", genres.toJsonArray())
        .put("synonyms", synonyms.toJsonArray())
        .put("isAdult", isAdult)
        .putNullable("updatedAtEpochSeconds", updatedAtEpochSeconds)
        .put("staff", staff.toJsonArray())
        .put("tags", tags.toJsonArray())

private fun JSONObject.toMedia(): AnilistMedia {
    val title = optJSONObject("title") ?: JSONObject()
    return AnilistMedia(
        id = optInt("mediaId").takeIf { it != 0 } ?: getInt("id"),
        idMal = optIntOrNull("idMal"),
        title = AnilistTitle(
            romaji = title.optStringOrNull("romaji"),
            english = title.optStringOrNull("english"),
            native = title.optStringOrNull("native"),
            userPreferred = title.optString("userPreferred").takeIf { it.isNotBlank() }
                ?: title.optStringOrNull("romaji")
                ?: "Untitled",
        ),
        description = null,
        coverImage = optStringOrNull("coverImage"),
        bannerImage = optStringOrNull("bannerImage"),
        chapters = optIntOrNull("chapters"),
        volumes = optIntOrNull("volumes"),
        format = optStringOrNull("format"),
        countryOfOrigin = optStringOrNull("countryOfOrigin"),
        status = optStringOrNull("status"),
        averageScore = optIntOrNull("averageScore"),
        popularity = optIntOrNull("popularity"),
        startDateYear = optIntOrNull("startDateYear"),
        endDateYear = optIntOrNull("endDateYear"),
        siteUrl = optStringOrNull("siteUrl"),
        genres = optJSONArray("genres").stringValues(),
        synonyms = optJSONArray("synonyms").stringValues(),
        isAdult = optBoolean("isAdult", false),
        updatedAtEpochSeconds = optLongOrNull("updatedAtEpochSeconds"),
        staff = optJSONArray("staff").stringValues(),
        tags = optJSONArray("tags").stringValues(),
    )
}

private fun JSONObject.putNullable(name: String, value: Any?): JSONObject =
    if (value == null) put(name, JSONObject.NULL) else put(name, value)

private fun Iterable<Any>.toJsonArray(): JSONArray =
    JSONArray().also { array -> forEach { array.put(it) } }

private fun JSONArray?.stringValues(): List<String> =
    objectValuesAndValues().mapNotNull { it as? String }

private fun JSONArray?.objectValues(): List<JSONObject> =
    objectValuesAndValues().mapNotNull { it as? JSONObject }

private fun JSONArray?.objectValuesAndValues(): List<Any> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> opt(index) }
}

private fun JSONObject.optStringOrNull(name: String): String? =
    if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null

private fun JSONObject.optIntOrNull(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null

private fun JSONObject.optLongOrNull(name: String): Long? =
    if (has(name) && !isNull(name)) optLong(name) else null

private const val RECOMMENDATION_SHARE_TYPE = "tankobun.recommendations"
private const val RECOMMENDATION_SHARE_VERSION = 1
private const val DEFAULT_RECOMMENDATION_LIST_NAME = "Tankobun recommendations"
private const val JSON_INDENT = 2
