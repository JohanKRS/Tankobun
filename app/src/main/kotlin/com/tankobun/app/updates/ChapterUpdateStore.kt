package com.tankobun.app.updates

import com.tankobun.app.AppContainer
import com.tankobun.core.database.ChapterUpdateEntity
import com.tankobun.core.model.SourceChapter
import kotlinx.coroutines.flow.Flow

/** Chapters that showed up for library manga, kept for the Library "Updates" sheet. */
internal class ChapterUpdateStore(private val container: AppContainer) {
    private val dao get() = container.database.chapterUpdateDao()

    fun observe(): Flow<List<ChapterUpdateEntity>> = dao.observeUpdates()

    suspend fun record(mediaId: Int, chapters: List<SourceChapter>, foundAtEpochMillis: Long) {
        if (chapters.isEmpty()) return
        dao.insertUpdates(chapters.map { it.toUpdateEntity(mediaId, foundAtEpochMillis) })
        dao.deleteOlderThan(foundAtEpochMillis - RETENTION_MILLIS)
    }

    suspend fun clear() = dao.clear()

    suspend fun forget(mediaIds: Collection<Int>) {
        if (mediaIds.isNotEmpty()) dao.deleteForMedia(mediaIds.toList())
    }

    companion object {
        /** Updates older than a month are no longer news. */
        const val RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}

internal fun SourceChapter.toUpdateEntity(mediaId: Int, foundAtEpochMillis: Long) = ChapterUpdateEntity(
    mediaId = mediaId,
    sourceId = sourceId,
    mangaUrl = mangaUrl,
    chapterUrl = url,
    name = name,
    chapterNumber = chapterNumber,
    chapterNumberText = chapterNumberText,
    volume = volume,
    scanlator = scanlator,
    uploadedAtEpochMillis = uploadedAtEpochMillis,
    foundAtEpochMillis = foundAtEpochMillis,
)

internal fun ChapterUpdateEntity.toSourceChapter() = SourceChapter(
    sourceId = sourceId,
    mangaUrl = mangaUrl,
    url = chapterUrl,
    name = name,
    chapterNumber = chapterNumber,
    scanlator = scanlator,
    uploadedAtEpochMillis = uploadedAtEpochMillis,
    volume = volume,
    chapterNumberText = chapterNumberText,
)
