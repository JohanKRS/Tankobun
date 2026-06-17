package com.tankobun.app.logic

import com.tankobun.app.state.ReaderChapterSegment
import com.tankobun.app.state.ReaderLoadError
import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceChapter

internal enum class ReaderSegmentDirection {
    PREVIOUS,
    NEXT,
}

internal fun TankobunUiState.withReaderLoading(chapter: SourceChapter): TankobunUiState =
    copy(
        busy = true,
        message = null,
        activeChapter = chapter,
        readerPages = emptyList(),
        readerPreviousSegment = null,
        readerNextSegment = null,
        readerError = null,
        currentPageIndex = 0,
        currentPageScrollOffset = 0,
    )

internal fun TankobunUiState.withReaderLoadError(
    chapter: SourceChapter,
    readerError: ReaderLoadError,
): TankobunUiState =
    if (activeChapter?.url == chapter.url) {
        copy(
            readerPages = emptyList(),
            readerPreviousSegment = null,
            readerNextSegment = null,
            readerError = readerError,
            busy = false,
            message = readerError.title,
        )
    } else {
        this
    }

internal fun TankobunUiState.withReaderPagesLoaded(
    chapter: SourceChapter,
    pages: List<ReaderPage>,
    pageIndex: Int,
    pageScrollOffset: Int,
): TankobunUiState =
    if (activeChapter?.url == chapter.url) {
        copy(
            activeChapter = chapter,
            readerPages = pages,
            readerPreviousSegment = null,
            readerNextSegment = null,
            readerError = null,
            currentPageIndex = pageIndex,
            currentPageScrollOffset = pageScrollOffset,
            busy = false,
            message = null,
        )
    } else {
        this
    }

internal fun TankobunUiState.withAdjacentReaderSegments(
    chapter: SourceChapter,
    previousSegment: ReaderChapterSegment?,
    nextSegment: ReaderChapterSegment?,
): TankobunUiState =
    if (activeChapter?.url == chapter.url) {
        copy(
            readerPreviousSegment = previousSegment,
            readerNextSegment = nextSegment,
        )
    } else {
        this
    }

internal fun TankobunUiState.withRecentProgressOpened(
    item: RecentReadingProgress,
    existingEntry: AnilistListEntry?,
): TankobunUiState =
    copy(
        selectedMedia = item.media,
        selectedListEntry = existingEntry,
        sourceMatches = emptyList(),
        sourceMatchChapterCounts = emptyMap(),
        sourcePickerOpen = false,
        sourcePickerLoading = false,
        sourcePickerMessage = null,
        sourcePickerDiagnostics = emptyList(),
        selectedRecommendations = emptyList(),
        selectedRecommendationsPage = 0,
        selectedRecommendationsHasMore = false,
        recommendationsLoading = false,
        trackingStatus = existingEntry.trackingStatusForForm(MediaStatus.CURRENT),
        trackingProgress = (existingEntry?.progress ?: 0).toString(),
        trackingScore = existingEntry?.score.formatTrackingScore(anilistScoreFormat),
        trackingNotes = existingEntry?.notes.orEmpty(),
        trackingPrivate = existingEntry?.private ?: false,
        trackingCustomLists = existingEntry?.customLists.orEmpty().toSet(),
        selectedSourceId = item.chapter?.sourceId ?: selectedSourceId,
        selectedSourceManga = null,
        sourceChapters = emptyList(),
        latestProgress = item.progress,
        chapterProgress = mapOf(item.progress.chapterUrl to item.progress),
        activeChapter = null,
        readerPages = emptyList(),
        readerPreviousSegment = null,
        readerNextSegment = null,
        readerError = null,
        currentPageIndex = 0,
        currentPageScrollOffset = 0,
        message = null,
    )

internal fun TankobunUiState.withReaderClosed(): TankobunUiState =
    copy(
        activeChapter = null,
        readerPages = emptyList(),
        readerPreviousSegment = null,
        readerNextSegment = null,
        readerError = null,
        currentPageIndex = 0,
        currentPageScrollOffset = 0,
        busy = false,
    )

internal fun TankobunUiState.withReaderPagePosition(
    pageIndex: Int,
    pageScrollOffset: Int,
): TankobunUiState =
    copy(
        currentPageIndex = pageIndex,
        currentPageScrollOffset = pageScrollOffset,
    )

internal fun TankobunUiState.withActivatedReaderSegment(
    targetSegment: ReaderChapterSegment,
    oldActiveSegment: ReaderChapterSegment,
    pageIndex: Int,
    pageScrollOffset: Int,
    direction: ReaderSegmentDirection,
): TankobunUiState =
    copy(
        activeChapter = targetSegment.chapter,
        readerPages = targetSegment.pages,
        readerPreviousSegment = if (direction == ReaderSegmentDirection.NEXT) oldActiveSegment else null,
        readerNextSegment = if (direction == ReaderSegmentDirection.PREVIOUS) oldActiveSegment else null,
        currentPageIndex = pageIndex,
        currentPageScrollOffset = pageScrollOffset.coerceAtLeast(0),
    )
