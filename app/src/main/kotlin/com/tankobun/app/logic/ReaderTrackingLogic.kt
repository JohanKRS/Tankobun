package com.tankobun.app.logic

import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceChapter

internal fun hasReadMoreThanHalfChapter(
    pageIndex: Int,
    totalPages: Int,
): Boolean {
    if (totalPages <= 0) return false
    val pagesRead = pageIndex.coerceIn(0, totalPages - 1) + 1
    return pagesRead * 2 > totalPages
}

internal fun automaticStatusForReaderPosition(
    pageIndex: Int,
    totalPages: Int,
    mediaStatus: String?,
    chapter: SourceChapter,
    sourceChapters: List<SourceChapter>,
    currentStatus: MediaStatus?,
    enabled: Boolean,
): MediaStatus? {
    if (!enabled || totalPages <= 0) return null
    val reachedLastPage = pageIndex.coerceAtLeast(0) >= totalPages - 1
    val reachedLastChapter = sourceChapters.nextInReadingOrderAfter(chapter) == null
    if (
        reachedLastPage &&
        reachedLastChapter &&
        mediaStatus.isFinishedPublicationStatus() &&
        currentStatus != MediaStatus.COMPLETED
    ) {
        return MediaStatus.COMPLETED
    }
    if (
        currentStatus != MediaStatus.CURRENT &&
        (!mediaStatus.isFinishedPublicationStatus() || currentStatus != MediaStatus.COMPLETED) &&
        hasReadMoreThanHalfChapter(pageIndex, totalPages)
    ) {
        return MediaStatus.CURRENT
    }
    return null
}

internal fun automaticStatusForReaderPosition(
    pageIndex: Int,
    pages: List<ReaderPage>,
    mediaStatus: String?,
    chapter: SourceChapter,
    sourceChapters: List<SourceChapter>,
    currentStatus: MediaStatus?,
    enabled: Boolean,
): MediaStatus? =
    automaticStatusForReaderPosition(
        pageIndex = pageIndex,
        totalPages = pages.size,
        mediaStatus = mediaStatus,
        chapter = chapter,
        sourceChapters = sourceChapters,
        currentStatus = currentStatus,
        enabled = enabled,
    )

internal fun String?.isFinishedPublicationStatus(): Boolean =
    equals("FINISHED", ignoreCase = true)
