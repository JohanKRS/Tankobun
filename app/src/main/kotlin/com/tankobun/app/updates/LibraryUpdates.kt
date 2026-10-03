package com.tankobun.app.updates

import com.tankobun.app.AppContainer
import com.tankobun.app.logic.ChapterStanding
import com.tankobun.app.logic.chapterStanding
import com.tankobun.app.logic.distinctChapterNumbers
import com.tankobun.app.state.LibraryUpdateChapter
import com.tankobun.app.state.LibraryUpdateGroup
import com.tankobun.core.database.toModel
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.SourceChapter

internal data class FoundChapter(
    val mediaId: Int,
    val chapter: SourceChapter,
    val foundAtEpochMillis: Long,
)

/**
 * Groups found chapters by manga, newest manga first. Manga that left the library are dropped,
 * and duplicate scanlations of one chapter number are shown once.
 */
internal fun buildLibraryUpdateGroups(
    found: List<FoundChapter>,
    mediaById: Map<Int, AnilistMedia>,
    readUrlsByMedia: Map<Int, Set<String>>,
): List<LibraryUpdateGroup> =
    found
        .groupBy { it.mediaId }
        .mapNotNull { (mediaId, items) ->
            val media = mediaById[mediaId] ?: return@mapNotNull null
            val readUrls = readUrlsByMedia[mediaId].orEmpty()
            val seenNumbers = mutableSetOf<String>()
            val chapters = items
                .sortedWith(
                    compareByDescending<FoundChapter> { it.chapter.chapterNumber }
                        .thenByDescending { it.foundAtEpochMillis },
                )
                .filter { item ->
                    val number = item.chapter.distinctNumberKey() ?: return@filter true
                    seenNumbers.add(number)
                }
                .map { item -> LibraryUpdateChapter(item.chapter, item.foundAtEpochMillis, item.chapter.url in readUrls) }
            LibraryUpdateGroup(
                media = media,
                chapters = chapters,
                newestFoundAtEpochMillis = items.maxOf { it.foundAtEpochMillis },
                newChapterCount = chapters.map { it.chapter }.distinctChapterNumbers().size,
            )
        }
        .sortedByDescending { it.newestFoundAtEpochMillis }

private fun SourceChapter.distinctNumberKey(): String? =
    listOf(this).distinctChapterNumbers().firstOrNull()?.stripTrailingZeros()?.toPlainString()

/** Unread chapters found after [seenAtEpochMillis], counted by chapter number. */
internal fun List<LibraryUpdateGroup>.unseenChapterCount(seenAtEpochMillis: Long): Int =
    sumOf { group ->
        group.chapters
            .filter { !it.read && it.foundAtEpochMillis > seenAtEpochMillis }
            .map { it.chapter }
            .distinctChapterNumbers()
            .size
    }

/** Where the reader stands in each started library manga, using its selected source's cached chapters. */
internal suspend fun AppContainer.libraryChapterStandings(mediaIds: Set<Int>): Map<Int, ChapterStanding> {
    if (mediaIds.isEmpty()) return emptyMap()
    val chapterDao = database.chapterDao()
    val bindingDao = database.sourceBindingDao()
    return buildMap {
        database.progressDao().latestReadingProgress()
            .asSequence()
            .map { it.toModel() }
            .filter { it.mediaId in mediaIds }
            .forEach { progress ->
                val binding = bindingDao.bindingForMedia(progress.mediaId) ?: return@forEach
                val chapters = chapterDao.cachedChapters(binding.sourceId, binding.mangaUrl).map { it.toModel() }
                val chapter = chapters.firstOrNull { it.url == progress.chapterUrl }
                chapterStanding(progress, chapter, chapters)?.let { put(progress.mediaId, it) }
            }
    }
}
