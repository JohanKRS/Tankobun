package com.tankobun.core.extensions

import com.tankobun.core.model.SourceManga
import com.tankobun.core.model.SourceMangaUpdate
import com.tankobun.core.model.SourceChapter
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SChapter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal suspend fun Source.updateManga(
    manga: SourceManga,
    chapters: List<SourceChapter>,
    fetchDetails: Boolean,
    fetchChapters: Boolean,
): SourceMangaUpdate {
    val update = getMangaUpdate(manga.toSManga(), chapters.map { it.toSChapter() }, fetchDetails, fetchChapters)
    val resolved = update.manga.toSourceManga(manga.sourceId).let { details ->
        details.copy(
            url = details.url.ifBlank { manga.url },
            title = details.title.ifBlank { manga.title },
            thumbnailUrl = details.thumbnailUrl ?: manga.thumbnailUrl,
            description = details.description ?: manga.description,
            author = details.author ?: manga.author,
            artist = details.artist ?: manga.artist,
            status = details.status ?: manga.status,
        )
    }
    val existingUrls = chapters.mapTo(hashSetOf()) { it.url }
    return SourceMangaUpdate(resolved, update.chapters.map { chapter ->
        if (this is HttpSource && chapter.url !in existingUrls) prepareNewChapter(chapter, update.manga)
        chapter.toSourceChapter(manga.sourceId, resolved.url)
    })
}

internal fun SourceManga.toSManga(): SManga =
    SManga.create().also {
        it.url = url
        it.title = title
        it.thumbnail_url = thumbnailUrl
        it.description = description
        it.author = author
        it.artist = artist
        it.status = status?.toIntOrNull() ?: SManga.UNKNOWN
        it.memo = memoJson.toSourceMemo()
    }

internal fun SManga.toSourceManga(sourceId: Long): SourceManga =
    SourceManga(
        sourceId = sourceId,
        url = url,
        title = title,
        thumbnailUrl = thumbnail_url,
        description = description,
        author = author,
        artist = artist,
        status = status.toString(),
        memoJson = memo.takeIf { it.isNotEmpty() }?.toString(),
    )

internal fun SChapter.toSourceChapter(sourceId: Long, mangaUrl: String): SourceChapter =
    SourceChapter(
        sourceId = sourceId,
        mangaUrl = mangaUrl,
        url = url,
        name = name,
        chapterNumber = chapter_number,
        scanlator = scanlator,
        uploadedAtEpochMillis = date_upload.takeIf { it > 0 },
        memoJson = memo.takeIf { it.isNotEmpty() }?.toString(),
    )

internal fun SourceChapter.toSChapter(): SChapter =
    SChapter.create().also {
        it.url = url
        it.name = name
        it.chapter_number = chapterNumber
        it.scanlator = scanlator
        it.date_upload = uploadedAtEpochMillis ?: 0L
        it.memo = memoJson.toSourceMemo()
    }

private fun String?.toSourceMemo(): JsonObject =
    this?.let { runCatching { Json.parseToJsonElement(it) as? JsonObject }.getOrNull() }
        ?: JsonObject(emptyMap())
