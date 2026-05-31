package com.tankobun.core.anilist

import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistMediaDetails
import com.tankobun.core.model.AnilistMediaPage
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistRecommendationPage
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.MediaStatus
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object AnilistJsonMapper {
    fun media(element: JsonElement): AnilistMedia {
        val obj = element.jsonObject
        val title = obj["title"]?.jsonObject ?: JsonObject(emptyMap())
        val cover = obj["coverImage"]?.jsonObject
        val startDate = obj["startDate"]?.jsonObject
        val endDate = obj["endDate"]?.jsonObject
        return AnilistMedia(
            id = obj.int("id"),
            idMal = obj.intOrNull("idMal"),
            title = AnilistTitle(
                romaji = title.stringOrNull("romaji"),
                english = title.stringOrNull("english"),
                native = title.stringOrNull("native"),
                userPreferred = title.stringOrNull("userPreferred")
                    ?: title.stringOrNull("romaji")
                    ?: "Untitled",
            ),
            description = obj.stringOrNull("description"),
            coverImage = cover?.stringOrNull("extraLarge") ?: cover?.stringOrNull("large"),
            bannerImage = obj.stringOrNull("bannerImage"),
            chapters = obj.intOrNull("chapters"),
            volumes = obj.intOrNull("volumes"),
            format = obj.stringOrNull("format"),
            status = obj.stringOrNull("status"),
            averageScore = obj.intOrNull("averageScore"),
            popularity = obj.intOrNull("popularity"),
            startDateYear = startDate?.intOrNull("year"),
            endDateYear = endDate?.intOrNull("year"),
            siteUrl = obj.stringOrNull("siteUrl"),
            genres = obj.stringArray("genres"),
            synonyms = obj.stringArray("synonyms"),
            isAdult = obj.booleanOrFalse("isAdult"),
            updatedAtEpochSeconds = obj.longOrNull("updatedAt"),
            staff = obj.staffNames(),
            tags = obj.mediaTagNames(),
            countryOfOrigin = obj.stringOrNull("countryOfOrigin"),
        )
    }

    fun listEntry(element: JsonElement): AnilistListEntry {
        val obj = element.jsonObject
        return AnilistListEntry(
            id = obj.int("id"),
            mediaId = obj.int("mediaId"),
            status = obj.stringOrNull("status").toMediaStatus(),
            progress = obj.intOrNull("progress") ?: 0,
            score = obj.doubleOrNull("score"),
            notes = obj.stringOrNull("notes"),
            private = obj.booleanOrFalse("private"),
            customLists = obj.stringArray("customLists"),
            updatedAtEpochSeconds = obj.longOrNull("updatedAt"),
        )
    }

    fun mediaDetails(data: JsonObject): AnilistMediaDetails {
        val mediaElement = requireNotNull(data["Media"])
        val mediaObj = mediaElement.jsonObject
        val entry = mediaObj["mediaListEntry"]
            ?.takeUnless { it is JsonNull }
            ?.let(::listEntry)
        return AnilistMediaDetails(
            media = media(mediaElement),
            listEntry = entry,
            recommendationPage = recommendationPage(mediaObj),
        )
    }

    fun mediaRecommendations(data: JsonObject): AnilistRecommendationPage {
        val mediaObj = data["Media"]
            ?.takeUnless { it is JsonNull }
            ?.jsonObject
            ?: JsonObject(emptyMap())
        return recommendationPage(mediaObj)
    }

    fun listCollection(data: JsonObject): List<Pair<AnilistMedia, AnilistListEntry>> {
        val lists = data["MediaListCollection"]
            ?.jsonObject
            ?.get("lists")
            ?.jsonArray
            ?: JsonArray(emptyList())

        return lists.flatMap { list ->
            list.jsonObject["entries"]?.jsonArray.orEmpty().mapNotNull { entry ->
                val entryObj = entry.jsonObject
                val media = entryObj["media"] ?: return@mapNotNull null
                media(media) to listEntry(entry)
            }
        }
    }

    fun searchPage(data: JsonObject): List<AnilistMedia> {
        return searchMediaPage(data).media
    }

    fun searchMediaPage(data: JsonObject): AnilistMediaPage {
        val page = data["Page"]?.jsonObject ?: JsonObject(emptyMap())
        val pageInfo = page["pageInfo"]?.jsonObject
        return AnilistMediaPage(
            media = page["media"]?.jsonArray.orEmpty().map(::media),
            currentPage = pageInfo?.intOrNull("currentPage") ?: 1,
            hasNextPage = pageInfo?.booleanOrFalse("hasNextPage") ?: false,
        )
    }

    fun staffMedia(data: JsonObject): List<AnilistMedia> {
        return staffMediaPage(data).media
    }

    fun staffMediaPage(data: JsonObject): AnilistMediaPage {
        val staffMedia = data["Staff"]
            ?.takeUnless { it is JsonNull }
            ?.jsonObject
            ?.get("staffMedia")
            ?.takeUnless { it is JsonNull }
            ?.jsonObject
            ?: JsonObject(emptyMap())
        val pageInfo = staffMedia["pageInfo"]?.jsonObject
        val media = staffMedia["edges"]
            ?.jsonArray
            .orEmpty()
            .mapNotNull { edge ->
                val obj = edge.jsonObject
                if (!obj.stringOrNull("staffRole").isCreatorRole()) return@mapNotNull null
                obj["node"]?.takeUnless { it is JsonNull }?.let(::media)
            }
            .distinctBy { it.id }
        return AnilistMediaPage(
            media = media,
            currentPage = pageInfo?.intOrNull("currentPage") ?: 1,
            hasNextPage = pageInfo?.booleanOrFalse("hasNextPage") ?: false,
        )
    }

    fun mediaTags(data: JsonObject): List<AnilistMediaTag> {
        return data["MediaTagCollection"]
            ?.jsonArray
            .orEmpty()
            .mapNotNull { tag ->
                val obj = tag.jsonObject
                val name = obj.stringOrNull("name") ?: return@mapNotNull null
                AnilistMediaTag(
                    name = name,
                    category = obj.stringOrNull("category"),
                    isAdult = obj.booleanOrFalse("isAdult"),
                )
            }
    }

    private fun recommendationPage(mediaObj: JsonObject): AnilistRecommendationPage {
        val recommendationObj = mediaObj["recommendations"]
            ?.takeUnless { it is JsonNull }
            ?.jsonObject
            ?: JsonObject(emptyMap())
        val pageInfo = recommendationObj["pageInfo"]?.jsonObject
        val recommendations = recommendationObj
            .get("nodes")
            ?.jsonArray
            .orEmpty()
            .mapNotNull { node ->
                val obj = node.jsonObject
                val mediaRecommendation = obj["mediaRecommendation"]
                    ?.takeUnless { it is JsonNull }
                    ?: return@mapNotNull null
                AnilistRecommendation(
                    media = media(mediaRecommendation),
                    rating = obj.intOrNull("rating"),
                )
            }
        return AnilistRecommendationPage(
            recommendations = recommendations,
            currentPage = pageInfo?.intOrNull("currentPage") ?: 1,
            hasNextPage = pageInfo?.booleanOrFalse("hasNextPage") ?: false,
        )
    }

    private fun String?.toMediaStatus(): MediaStatus = when (this) {
        "CURRENT" -> MediaStatus.CURRENT
        "PLANNING" -> MediaStatus.PLANNING
        "COMPLETED" -> MediaStatus.COMPLETED
        "DROPPED" -> MediaStatus.DROPPED
        "PAUSED" -> MediaStatus.PAUSED
        "REPEATING" -> MediaStatus.REPEATING
        else -> MediaStatus.UNKNOWN
    }
}

private fun JsonObject.int(name: String): Int =
    requireNotNull(this[name]?.jsonPrimitive?.intOrNull) { "Missing integer field $name" }

private fun JsonObject.intOrNull(name: String): Int? =
    this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull

private fun JsonObject.longOrNull(name: String): Long? =
    this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.longOrNull

private fun JsonObject.doubleOrNull(name: String): Double? =
    this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.doubleOrNull

private fun JsonObject.stringOrNull(name: String): String? =
    this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content

private fun JsonObject.booleanOrFalse(name: String): Boolean =
    this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.booleanOrNull ?: false

private fun JsonObject.stringArray(name: String): List<String> =
    this[name]?.takeUnless { it is JsonNull }?.stringValues().orEmpty()

private fun JsonElement.stringValues(): List<String> =
    when (this) {
        is JsonArray -> flatMap { it.stringValues() }
        is JsonPrimitive -> listOf(content.trim()).filter { it.isNotBlank() }
        is JsonObject -> objectStringValues()
        else -> emptyList()
    }

private fun JsonObject.objectStringValues(): List<String> {
    stringOrNull("name")?.trim()?.takeIf { it.isNotBlank() }?.let { name ->
        return if (namedObjectEnabled()) listOf(name) else emptyList()
    }
    return entries.mapNotNull { (key, value) ->
        when (value) {
            is JsonPrimitive -> when (value.booleanOrNull) {
                true -> key
                false -> null
                null -> value.content.trim().takeIf { it.isNotBlank() }
            }
            is JsonObject -> value.stringOrNull("name")?.trim()?.takeIf { it.isNotBlank() }
            else -> null
        }
    }
}

private fun JsonObject.namedObjectEnabled(): Boolean {
    val explicitState = listOf("enabled", "selected", "value", "checked")
        .firstNotNullOfOrNull { field -> (this[field]?.takeUnless { it is JsonNull } as? JsonPrimitive)?.booleanOrNull }
    return explicitState != false
}

private fun JsonObject.staffNames(): List<String> =
    preferredCreatorNames().ifEmpty {
        this["staff"]
            ?.takeUnless { it is JsonNull }
            ?.jsonObject
            ?.get("nodes")
            ?.jsonArray
            .orEmpty()
            .mapNotNull { node ->
                node.jsonObject["name"]
                    ?.takeUnless { it is JsonNull }
                    ?.jsonObject
                    ?.stringOrNull("userPreferred")
            }
            .distinct()
    }

private fun JsonObject.preferredCreatorNames(): List<String> =
    this["staff"]
        ?.takeUnless { it is JsonNull }
        ?.jsonObject
        ?.get("edges")
        ?.jsonArray
        .orEmpty()
        .mapNotNull { edge ->
            val obj = edge.jsonObject
            val role = obj.stringOrNull("role")
            if (!role.isCreatorRole()) return@mapNotNull null
            obj["node"]
                ?.takeUnless { it is JsonNull }
                ?.jsonObject
                ?.get("name")
                ?.takeUnless { it is JsonNull }
                ?.jsonObject
                ?.stringOrNull("userPreferred")
        }
        .distinct()

private fun String?.isCreatorRole(): Boolean {
    val normalized = this?.lowercase().orEmpty()
    return normalized.contains("story") ||
        normalized.contains("art") ||
        normalized.contains("author") ||
        normalized.contains("creator") ||
        normalized.contains("writer")
}

private fun JsonObject.mediaTagNames(): List<String> =
    this["tags"]
        ?.takeUnless { it is JsonNull }
        ?.jsonArray
        .orEmpty()
        .mapNotNull { tag ->
            val obj = tag.jsonObject
            if (obj.booleanOrFalse("isMediaSpoiler") || obj.booleanOrFalse("isGeneralSpoiler")) return@mapNotNull null
            obj.stringOrNull("name")?.let { name -> obj.intOrNull("rank").orZero() to name }
        }
        .sortedByDescending { (rank, _) -> rank }
        .map { (_, name) -> name }
        .distinct()

private fun Int?.orZero(): Int = this ?: 0
