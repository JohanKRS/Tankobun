package com.tankobun.core.reader

import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter

data class ReaderSession(
    val mediaId: Int,
    val chapter: SourceChapter,
    val pages: List<ReaderPage>,
    val mode: ReaderMode,
    val currentPageIndex: Int,
) {
    val canGoForward: Boolean get() = currentPageIndex < pages.lastIndex
    val canGoBack: Boolean get() = currentPageIndex > 0

    fun moveTo(index: Int): ReaderSession =
        copy(currentPageIndex = index.coerceIn(0, pages.lastIndex.coerceAtLeast(0)))

    fun next(): ReaderSession = moveTo(currentPageIndex + 1)

    fun previous(): ReaderSession = moveTo(currentPageIndex - 1)
}

class ReaderProgressCalculator {
    fun progressFor(
        session: ReaderSession,
        nowMillis: Long,
    ): ReadingProgress {
        val completed = session.pages.isNotEmpty() && session.currentPageIndex >= session.pages.lastIndex
        return ReadingProgress(
            mediaId = session.mediaId,
            chapterUrl = session.chapter.url,
            chapterNumber = session.chapter.chapterNumber,
            pageIndex = session.currentPageIndex,
            totalPages = session.pages.size,
            readerMode = session.mode,
            completed = completed,
            updatedAtEpochMillis = nowMillis,
        )
    }
}

class PagePrefetchPlanner(
    private val radius: Int = 3,
) {
    fun pagesToPrefetch(currentPageIndex: Int, totalPages: Int): IntRange {
        if (totalPages <= 0) return IntRange.EMPTY
        val start = (currentPageIndex - radius).coerceAtLeast(0)
        val end = (currentPageIndex + radius).coerceAtMost(totalPages - 1)
        return start..end
    }
}
