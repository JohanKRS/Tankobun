package com.tankobun.core.mangabaka

import com.tankobun.core.model.*
import kotlinx.serialization.json.*
import kotlin.math.roundToInt

internal fun JsonObject.obj(key: String) = this[key] as? JsonObject ?: JsonObject(emptyMap())
internal fun JsonObject.array(key: String) = this[key] as? JsonArray ?: JsonArray(emptyList())
internal fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
internal fun JsonObject.number(key: String) = text(key)?.toDoubleOrNull()
internal fun JsonObject.strings(key: String) = array(key).mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
internal fun String?.imageUrl() = this?.takeIf { it.startsWith("https://") }

internal object MangaBakaMapper {
    fun media(data: JsonObject): AnilistMedia? {
        val id = data.number("id")?.toInt()?.takeIf { it > 0 } ?: return null
        if (data.text("state") != "active") return null
        val anilist = data.obj("source").obj("anilist").number("id")?.toInt()?.takeIf { it > 0 }
        val titles = data.array("titles").mapNotNull { it as? JsonObject }
            .sortedByDescending { it["is_primary"] == JsonPrimitive(true) }
        fun title(vararg languages: String) = titles.firstOrNull { it.text("language") in languages }?.text("title")
        val english = title("en") ?: data.text("title")
        val romaji = title("ja-Latn", "ko-Latn", "zh-Latn") ?: data.text("romanized_title")
        val native = title("ja", "ko", "zh", "zh-Hant", "zh-Hans") ?: data.text("native_title")
        val preferred = english ?: romaji ?: native ?: titles.firstNotNullOfOrNull { it.text("title") } ?: return null
        val tags = data.array("tags").mapNotNull { it as? JsonObject }.filter { it["is_spoiler"] != JsonPrimitive(true) }
        val type = data.text("type")
        val cover = data.obj("cover")
        val coverUrl = cover.text("x350") ?: cover.obj("x350").text("x2") ?: cover.text("raw") ?: cover.obj("raw").text("url")
        return AnilistMedia(
            id = anilist ?: -id, anilistId = anilist, mangaBakaId = id,
            idMal = data.obj("source").obj("my_anime_list").number("id")?.toInt(),
            title = AnilistTitle(romaji, english, native, preferred),
            description = data.text("description"), coverImage = coverUrl.imageUrl(), bannerImage = null,
            chapters = data.number("total_chapters")?.toInt(), volumes = data.number("final_volume")?.toInt(),
            format = if (type == "novel") "NOVEL" else "MANGA",
            countryOfOrigin = when (type) { "manhwa" -> "KR"; "manhua" -> "CN"; "manga", "novel" -> "JP"; else -> null },
            status = when (data.text("status")) { "completed" -> "FINISHED"; "cancelled" -> "CANCELLED"; "releasing" -> "RELEASING"; "hiatus" -> "HIATUS"; "upcoming" -> "NOT_YET_RELEASED"; else -> null },
            averageScore = data.number("rating")?.roundToInt(),
            // MangaBaka popularity is a rank, not AniList's reader count.
            popularity = null,
            startDateYear = data.obj("published").text("start_date")?.take(4)?.toIntOrNull(),
            endDateYear = data.obj("published").text("end_date")?.take(4)?.toIntOrNull(),
            siteUrl = data.text("canonical_url") ?: "https://mangabaka.org/$id",
            genres = tags.filter { it["is_genre"] == JsonPrimitive(true) }.mapNotNull { it.text("name") },
            tags = tags.mapNotNull { it.text("name") },
            staff = (data.strings("authors") + data.strings("artists")).distinct(),
            synonyms = titles.mapNotNull { it.text("title") }.distinct(),
            isAdult = data.text("content_rating") !in setOf("safe", "suggestive"),
            updatedAtEpochSeconds = null,
        )
    }

    fun banner(data: JsonObject, includeAdult: Boolean): String? = data.array("data")
        .mapNotNull { it as? JsonObject }
        .filter { it.text("type") == "banner" && (includeAdult || it.text("content_rating") in setOf("safe", "suggestive")) }
        .map { it.obj("image") }
        .firstOrNull { it.obj("raw").number("width") ?: 0.0 >= (it.obj("raw").number("height") ?: Double.MAX_VALUE) * 1.5 }
        ?.let { it.obj("x350").text("x3") ?: it.obj("raw").text("url") }.imageUrl()
}
