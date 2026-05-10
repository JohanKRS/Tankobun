package com.tankobun.core.anilist

import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.MediaStatus
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
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
            status = obj.stringOrNull("status"),
            siteUrl = obj.stringOrNull("siteUrl"),
            genres = obj.stringArray("genres"),
            synonyms = obj.stringArray("synonyms"),
            isAdult = obj.booleanOrFalse("isAdult"),
            updatedAtEpochSeconds = obj.longOrNull("updatedAt"),
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
        return data["Page"]
            ?.jsonObject
            ?.get("media")
            ?.jsonArray
            .orEmpty()
            .map(::media)
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
    this[name]?.takeUnless { it is JsonNull }?.jsonArray?.mapNotNull {
        it.takeUnless { value -> value is JsonNull }?.jsonPrimitive?.content
    }.orEmpty()
