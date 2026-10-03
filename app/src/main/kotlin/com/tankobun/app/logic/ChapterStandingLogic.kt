package com.tankobun.app.logic

import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter
import java.math.BigDecimal
import java.util.TreeSet

/**
 * Chapter numbers a reader would count. Scanlator duplicates of one number collapse, and
 * specials, extras or unnumbered entries are left out, following [editionKey].
 */
internal fun Iterable<SourceChapter>.distinctChapterNumbers(): TreeSet<BigDecimal> =
    mapNotNullTo(TreeSet()) { chapter -> chapter.editionKey()?.number?.toBigDecimalOrNull() }

/** Where the reader stands in a manga: the chapter they are on and how many numbers come after it. */
data class ChapterStanding(
    val currentNumber: Float,
    val remaining: Int,
    val currentCompleted: Boolean,
) {
    val caughtUp: Boolean get() = currentCompleted && remaining == 0
}

internal fun chapterStanding(
    progress: ReadingProgress,
    chapter: SourceChapter?,
    chapters: List<SourceChapter>,
): ChapterStanding? {
    val current = progress.chapterNumber.takeIf { it > 0f && it.isFinite() }
        ?: chapter?.chapterNumber?.takeIf { it > 0f && it.isFinite() }
        ?: return null
    if (chapters.isEmpty()) return null
    val currentNumber = BigDecimal(current.toString())
    return ChapterStanding(
        currentNumber = current,
        remaining = chapters.distinctChapterNumbers().tailSet(currentNumber, false).size,
        currentCompleted = progress.completed,
    )
}

/** "12", "12.5": the shortest form of a chapter number for labels. */
internal fun Float.chapterNumberLabel(): String =
    BigDecimal(toString()).stripTrailingZeros().toPlainString()
