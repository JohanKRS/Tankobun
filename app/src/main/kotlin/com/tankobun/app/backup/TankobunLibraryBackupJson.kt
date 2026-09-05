package com.tankobun.app.backup

import com.tankobun.app.state.LibraryItem
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceBinding
import org.json.JSONArray
import org.json.JSONObject

internal data class TankobunLibraryBackup(
    val scoreFormat: AnilistScoreFormat,
    val titleLanguage: AnilistTitleLanguage,
    val customLists: List<String>,
    val items: List<TankobunLibraryBackupItem>,
)

internal data class TankobunLibraryBackupItem(
    val media: AnilistMedia,
    val entry: AnilistListEntry,
    val sourceBinding: SourceBinding?,
    val progress: List<ReadingProgress>,
)

internal fun buildTankobunLibraryBackupJson(
    items: List<LibraryItem>,
    scoreFormat: AnilistScoreFormat,
    titleLanguage: AnilistTitleLanguage,
    customLists: List<String>,
    sourceBindings: List<SourceBinding>,
    progress: List<ReadingProgress>,
): String {
    val bindingsByMediaId = sourceBindings.associateBy { it.mediaId }
    val progressByMediaId = progress.groupBy { it.mediaId }
    return JSONObject()
        .put("type", TANKOBUN_LIBRARY_BACKUP_TYPE)
        .put("version", TANKOBUN_LIBRARY_BACKUP_VERSION)
        .put("createdAtEpochMillis", System.currentTimeMillis())
        .put(
            "preferences",
            JSONObject()
                .put("scoreFormat", scoreFormat.name)
                .put("titleLanguage", titleLanguage.name)
                .put("customLists", customLists.toJsonArray()),
        )
        .put(
            "items",
            items.sortedBy { it.media.title.userPreferred.lowercase() }
                .map { item ->
                    JSONObject()
                        .put("media", item.media.toJson())
                        .put("entry", item.entry.toJson())
                        .put("sourceBinding", bindingsByMediaId[item.media.id]?.toJson())
                        .put("progress", progressByMediaId[item.media.id].orEmpty().map { it.toJson() }.toJsonArray())
                }
                .toJsonArray(),
        )
        .toString(JSON_INDENT)
}

internal fun parseTankobunLibraryBackupJson(text: String): TankobunLibraryBackup {
    val root = JSONObject(text)
    check(root.optString("type") == TANKOBUN_LIBRARY_BACKUP_TYPE) { "Unsupported Tankobun library backup" }
    val preferences = root.optJSONObject("preferences") ?: JSONObject()
    return TankobunLibraryBackup(
        scoreFormat = preferences.enumOrDefault("scoreFormat", AnilistScoreFormat.POINT_100),
        titleLanguage = preferences.enumOrDefault("titleLanguage", AnilistTitleLanguage.ROMAJI),
        customLists = preferences.optJSONArray("customLists").stringValues(),
        items = root.optJSONArray("items")
            .objectValues()
            .map { item ->
                TankobunLibraryBackupItem(
                    media = requireNotNull(item.optJSONObject("media")) { "Missing media" }.toMedia(),
                    entry = requireNotNull(item.optJSONObject("entry")) { "Missing entry" }.toEntry(),
                    sourceBinding = item.optJSONObject("sourceBinding")?.toSourceBinding(),
                    progress = item.optJSONArray("progress").objectValues().map { it.toProgress() },
                )
            },
    )
}

internal fun isTankobunLibraryBackupJson(text: String): Boolean =
    runCatching { JSONObject(text).optString("type") == TANKOBUN_LIBRARY_BACKUP_TYPE }.getOrDefault(false)

private fun AnilistMedia.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .putNullable("idMal", idMal)
        .put(
            "title",
            JSONObject()
                .putNullable("romaji", title.romaji)
                .putNullable("english", title.english)
                .putNullable("native", title.native)
                .put("userPreferred", title.userPreferred),
        )
        .putNullable("description", description)
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
        id = getInt("id"),
        idMal = optIntOrNull("idMal"),
        title = AnilistTitle(
            romaji = title.optStringOrNull("romaji"),
            english = title.optStringOrNull("english"),
            native = title.optStringOrNull("native"),
            userPreferred = title.optString("userPreferred").takeIf { it.isNotBlank() }.orEmpty(),
        ),
        description = optStringOrNull("description"),
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

private fun AnilistListEntry.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("mediaId", mediaId)
        .put("status", status.name)
        .put("progress", progress)
        .putNullable("score", score)
        .putNullable("notes", notes)
        .put("private", private)
        .put("customLists", customLists.toJsonArray())
        .put("hiddenFromStatusLists", hiddenFromStatusLists)
        .putNullable("updatedAtEpochSeconds", updatedAtEpochSeconds)

private fun JSONObject.toEntry(): AnilistListEntry =
    AnilistListEntry(
        id = optInt("id", -getInt("mediaId")),
        mediaId = getInt("mediaId"),
        status = enumOrDefault("status", MediaStatus.PLANNING),
        progress = optInt("progress", 0).coerceAtLeast(0),
        score = optDoubleOrNull("score"),
        notes = optStringOrNull("notes"),
        private = optBoolean("private", false),
        customLists = optJSONArray("customLists").stringValues(),
        updatedAtEpochSeconds = optLongOrNull("updatedAtEpochSeconds"),
        hiddenFromStatusLists = optBoolean("hiddenFromStatusLists", false),
    )

private fun SourceBinding.toJson(): JSONObject =
    JSONObject()
        .put("mediaId", mediaId)
        .put("sourceId", sourceId)
        .put("sourcePackageName", sourcePackageName)
        .put("mangaUrl", mangaUrl)
        .put("mangaTitle", mangaTitle)
        .putNullable("thumbnailUrl", thumbnailUrl)
        .put("selectedAtEpochMillis", selectedAtEpochMillis)
        .putNullable("memoJson", memoJson)

private fun JSONObject.toSourceBinding(): SourceBinding =
    SourceBinding(
        mediaId = getInt("mediaId"),
        sourceId = getLong("sourceId"),
        sourcePackageName = optString("sourcePackageName"),
        mangaUrl = optString("mangaUrl"),
        mangaTitle = optString("mangaTitle"),
        thumbnailUrl = optStringOrNull("thumbnailUrl"),
        selectedAtEpochMillis = optLong("selectedAtEpochMillis", 0L),
        memoJson = optStringOrNull("memoJson"),
    )

private fun ReadingProgress.toJson(): JSONObject =
    JSONObject()
        .put("mediaId", mediaId)
        .put("chapterUrl", chapterUrl)
        .put("chapterNumber", chapterNumber.toDouble())
        .put("pageIndex", pageIndex)
        .put("pageScrollOffset", pageScrollOffset)
        .put("totalPages", totalPages)
        .put("readerMode", readerMode.name)
        .put("completed", completed)
        .put("updatedAtEpochMillis", updatedAtEpochMillis)

private fun JSONObject.toProgress(): ReadingProgress =
    ReadingProgress(
        mediaId = getInt("mediaId"),
        chapterUrl = optString("chapterUrl"),
        chapterNumber = optDouble("chapterNumber", 0.0).toFloat(),
        pageIndex = optInt("pageIndex", 0).coerceAtLeast(0),
        pageScrollOffset = optInt("pageScrollOffset", 0).coerceAtLeast(0),
        totalPages = optInt("totalPages", 0).coerceAtLeast(0),
        readerMode = enumOrDefault("readerMode", ReaderMode.PAGED),
        completed = optBoolean("completed", false),
        updatedAtEpochMillis = optLong("updatedAtEpochMillis", 0L),
    )

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

private fun JSONObject.optDoubleOrNull(name: String): Double? =
    if (has(name) && !isNull(name)) optDouble(name) else null

private inline fun <reified T : Enum<T>> JSONObject.enumOrDefault(name: String, default: T): T =
    optStringOrNull(name)?.let { value -> runCatching { enumValueOf<T>(value) }.getOrNull() } ?: default

private const val TANKOBUN_LIBRARY_BACKUP_TYPE = "tankobun.library"
private const val TANKOBUN_LIBRARY_BACKUP_VERSION = 1
private const val JSON_INDENT = 2
