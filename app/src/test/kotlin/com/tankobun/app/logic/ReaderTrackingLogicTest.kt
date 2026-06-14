package com.tankobun.app.logic

import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.SourceChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTrackingLogicTest {
    @Test
    fun moreThanHalfRequiresStrictlyPastTheHalfwayPoint() {
        assertFalse(hasReadMoreThanHalfChapter(pageIndex = 4, totalPages = 10))
        assertTrue(hasReadMoreThanHalfChapter(pageIndex = 5, totalPages = 10))
        assertTrue(hasReadMoreThanHalfChapter(pageIndex = 2, totalPages = 5))
    }

    @Test
    fun halfChapterPromotesUntrackedMangaToReadingOnly() {
        val chapters = listOf(chapter("1", 1f), chapter("2", 2f))

        assertEquals(
            MediaStatus.CURRENT,
            automaticStatusForReaderPosition(
                pageIndex = 5,
                totalPages = 10,
                mediaStatus = "RELEASING",
                chapter = chapters.first(),
                sourceChapters = chapters,
                currentStatus = MediaStatus.PLANNING,
                enabled = true,
            ),
        )
    }

    @Test
    fun halfChapterDoesNotOverrideReadingOrCompleted() {
        val chapters = listOf(chapter("1", 1f), chapter("2", 2f))

        assertNull(
            automaticStatusForReaderPosition(
                pageIndex = 5,
                totalPages = 10,
                mediaStatus = "RELEASING",
                chapter = chapters.first(),
                sourceChapters = chapters,
                currentStatus = MediaStatus.CURRENT,
                enabled = true,
            ),
        )
        assertNull(
            automaticStatusForReaderPosition(
                pageIndex = 5,
                totalPages = 10,
                mediaStatus = "FINISHED",
                chapter = chapters.first(),
                sourceChapters = chapters,
                currentStatus = MediaStatus.COMPLETED,
                enabled = true,
            ),
        )
    }

    @Test
    fun finalPageOfLastChapterPromotesToCompleted() {
        val chapters = listOf(chapter("1", 1f), chapter("2", 2f))

        assertEquals(
            MediaStatus.COMPLETED,
            automaticStatusForReaderPosition(
                pageIndex = 9,
                totalPages = 10,
                mediaStatus = "FINISHED",
                chapter = chapters.last(),
                sourceChapters = chapters,
                currentStatus = MediaStatus.CURRENT,
                enabled = true,
            ),
        )
    }

    @Test
    fun finalPageOfUnfinishedMangaAlreadyReadingNeedsNoChange() {
        val chapters = listOf(chapter("1", 1f), chapter("2", 2f))

        assertNull(
            automaticStatusForReaderPosition(
                pageIndex = 9,
                totalPages = 10,
                mediaStatus = "RELEASING",
                chapter = chapters.last(),
                sourceChapters = chapters,
                currentStatus = MediaStatus.CURRENT,
                enabled = true,
            ),
        )
    }

    @Test
    fun finalPageOfUnfinishedMangaPromotesPlanningToReading() {
        val chapters = listOf(chapter("1", 1f), chapter("2", 2f))

        assertEquals(
            MediaStatus.CURRENT,
            automaticStatusForReaderPosition(
                pageIndex = 9,
                totalPages = 10,
                mediaStatus = "RELEASING",
                chapter = chapters.last(),
                sourceChapters = chapters,
                currentStatus = MediaStatus.PLANNING,
                enabled = true,
            ),
        )
    }

    @Test
    fun finalPageOfUnfinishedMangaCanMoveCompletedBackToReading() {
        val chapters = listOf(chapter("1", 1f))

        assertEquals(
            MediaStatus.CURRENT,
            automaticStatusForReaderPosition(
                pageIndex = 0,
                totalPages = 1,
                mediaStatus = "RELEASING",
                chapter = chapters.first(),
                sourceChapters = chapters,
                currentStatus = MediaStatus.COMPLETED,
                enabled = true,
            ),
        )
    }

    @Test
    fun disabledAutomationDoesNothing() {
        val chapters = listOf(chapter("1", 1f))

        assertNull(
            automaticStatusForReaderPosition(
                pageIndex = 9,
                totalPages = 10,
                mediaStatus = "FINISHED",
                chapter = chapters.first(),
                sourceChapters = chapters,
                currentStatus = MediaStatus.PLANNING,
                enabled = false,
            ),
        )
    }

    private fun chapter(url: String, number: Float): SourceChapter =
        SourceChapter(
            sourceId = 1L,
            mangaUrl = "manga",
            url = url,
            name = url,
            chapterNumber = number,
            scanlator = null,
            uploadedAtEpochMillis = null,
        )
}
