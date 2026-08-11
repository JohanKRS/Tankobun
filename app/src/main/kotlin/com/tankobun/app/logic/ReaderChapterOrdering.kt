package com.tankobun.app.logic

import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter
import kotlin.math.abs

internal const val NEXT_DOWNLOAD_WINDOW_SIZE = 10

internal fun List<SourceChapter>.nextInReadingOrderAfter(chapter: SourceChapter): SourceChapter? {
    if (chapter.chapterNumber > 0f) {
        return filter { it.chapterNumber > chapter.chapterNumber }
            .minByOrNull { it.chapterNumber }
    }

    val currentIndex = indexOfFirst { it.sourceId == chapter.sourceId && it.url == chapter.url }
    return if (currentIndex > 0) this[currentIndex - 1] else null
}

internal fun List<SourceChapter>.previousInReadingOrderBefore(chapter: SourceChapter): SourceChapter? {
    if (chapter.chapterNumber > 0f) {
        return filter { it.chapterNumber < chapter.chapterNumber }
            .maxByOrNull { it.chapterNumber }
    }

    val currentIndex = indexOfFirst { it.sourceId == chapter.sourceId && it.url == chapter.url }
    return if (currentIndex >= 0 && currentIndex < lastIndex) this[currentIndex + 1] else null
}

internal fun List<SourceChapter>.chapterNearProgress(progress: ReadingProgress): SourceChapter? {
    val chapterNumber = progress.chapterNumber
    if (chapterNumber > 0f) {
        val nextChapter = if (progress.completed) {
            filter { it.chapterNumber > chapterNumber }.minByOrNull { it.chapterNumber }
        } else {
            filter { it.chapterNumber >= chapterNumber }.minByOrNull { it.chapterNumber }
        }
        if (nextChapter != null) return nextChapter
        return minByOrNull {
            abs((it.chapterNumber.takeIf { number -> number > 0f } ?: chapterNumber) - chapterNumber)
        }
    }
    return firstInReadingOrder()
}

internal fun List<SourceChapter>.firstInReadingOrder(): SourceChapter? =
    filter { it.chapterNumber > 0f }
        .minByOrNull { it.chapterNumber }
        ?: lastOrNull()

internal fun nextTenDownloadCandidates(state: TankobunUiState): List<SourceChapter> {
    val chapters = state.sourceChapters.readingOrder()
    if (chapters.isEmpty()) return emptyList()
    val progress = state.latestProgress
    val startIndex = if (progress == null) {
        0
    } else {
        val exactIndex = chapters.indexOfFirst { it.url == progress.chapterUrl }
        when {
            exactIndex >= 0 && progress.completed -> exactIndex + 1
            exactIndex >= 0 -> exactIndex
            progress.chapterNumber > 0f -> chapters.indexOfFirst { it.chapterNumber >= progress.chapterNumber }
                .takeIf { it >= 0 } ?: 0
            else -> 0
        }
    }.coerceIn(0, chapters.size)

    return chapters
        .drop(startIndex)
        .filterNot { state.chapterProgress[it.url]?.completed == true }
        .take(NEXT_DOWNLOAD_WINDOW_SIZE)
}

internal fun List<SourceChapter>.trackerProgressForChapter(chapter: SourceChapter): Int? {
    chapter.chapterNumber.toInt().takeIf { it > 0 }?.let { return it }
    if (any { it.chapterNumber > 0f }) return null
    val readingIndex = readingOrder().indexOfFirst { item ->
        item.sourceId == chapter.sourceId && item.url == chapter.url
    }
    return (readingIndex + 1).takeIf { readingIndex >= 0 }
}

internal fun List<SourceChapter>.readingOrder(): List<SourceChapter> =
    if (any { it.chapterNumber > 0f }) {
        sortedWith(compareBy<SourceChapter> { it.chapterNumber.takeIf { number -> number > 0f } ?: Float.MAX_VALUE }
            .thenBy { it.name })
    } else {
        asReversed()
    }
