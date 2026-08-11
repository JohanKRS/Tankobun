package com.tankobun.app.logic

import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter

internal data class RecentReadingMetrics(
    val currentChapterNumber: Float?,
    val lastAvailableChapterNumber: Float?,
    val overallProgress: Float?,
    val reachedLastAvailableChapter: Boolean,
)

internal fun RecentReadingProgress.withCurrentSourceChapters(
    sourcePackageName: String,
    chapters: List<SourceChapter>,
): RecentReadingProgress {
    val previousChapter = chapter
    val effectiveProgress = previousChapter
        ?.chapterNumber
        ?.takeIf { progress.chapterNumber <= 0f && it > 0f }
        ?.let { previousNumber -> progress.copy(chapterNumber = previousNumber) }
        ?: progress
    val previousName = previousChapter?.name?.trim().orEmpty()
    val currentChapter = chapters.firstOrNull { it.url == progress.chapterUrl }
        ?: if (effectiveProgress.chapterNumber > 0f) {
            chapters.chapterNearProgress(effectiveProgress)
        } else {
            chapters.firstOrNull { current ->
                previousName.isNotEmpty() && current.name.trim().equals(previousName, ignoreCase = true)
            } ?: chapters.firstInReadingOrder()
        }
    return copy(
        chapter = currentChapter,
        sourcePackageName = sourcePackageName,
    )
}

internal fun recentReadingMetrics(
    progress: ReadingProgress,
    chapter: SourceChapter?,
    availableChapters: List<SourceChapter>,
): RecentReadingMetrics {
    val currentChapterNumber = progress.chapterNumber.takeIf { it > 0f }
        ?: chapter?.chapterNumber?.takeIf { it > 0f }
    val lastAvailableChapterNumber = availableChapters
        .asSequence()
        .map(SourceChapter::chapterNumber)
        .filter { it > 0f }
        .maxOrNull()
    val overallProgress = if (currentChapterNumber != null && lastAvailableChapterNumber != null) {
        (currentChapterNumber / lastAvailableChapterNumber).coerceIn(0f, 1f)
    } else {
        null
    }
    return RecentReadingMetrics(
        currentChapterNumber = currentChapterNumber,
        lastAvailableChapterNumber = lastAvailableChapterNumber,
        overallProgress = overallProgress,
        reachedLastAvailableChapter = currentChapterNumber != null &&
            lastAvailableChapterNumber != null &&
            currentChapterNumber >= lastAvailableChapterNumber,
    )
}

internal fun AnilistMedia.shouldShowInContinueReading(
    entry: AnilistListEntry?,
    progress: ReadingProgress,
    metrics: RecentReadingMetrics,
): Boolean =
    entry?.isInReadingCategory() == true &&
        (!status.equals("FINISHED", ignoreCase = true) ||
            !progress.completed ||
            !metrics.reachedLastAvailableChapter)

internal fun AnilistListEntry.isInReadingCategory(): Boolean =
    status == MediaStatus.CURRENT && !hiddenFromStatusLists
