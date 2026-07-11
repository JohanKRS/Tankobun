package com.tankobun.app.logic

import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentReadingLogicTest {
    @Test
    fun overallProgressUsesHighestChapterNumberInsteadOfDuplicateCount() {
        val metrics = recentReadingMetrics(
            progress = progress(chapterNumber = 20f),
            chapter = chapter(20f),
            availableChapters = listOf(1f, 20f, 20f, 20f, 58f).map(::chapter),
        )

        assertEquals(20f, metrics.currentChapterNumber)
        assertEquals(58f, metrics.lastAvailableChapterNumber)
        assertEquals(20f / 58f, metrics.overallProgress)
        assertFalse(metrics.reachedLastAvailableChapter)
    }

    @Test
    fun finishedMangaAtCompletedLastChapterLeavesContinueReading() {
        val completedProgress = progress(chapterNumber = 58f, completed = true)
        val metrics = recentReadingMetrics(
            progress = completedProgress,
            chapter = chapter(58f),
            availableChapters = listOf(1f, 58f, 58f).map(::chapter),
        )

        assertFalse(media(status = "FINISHED").shouldShowInContinueReading(completedProgress, metrics))
    }

    @Test
    fun releasingMangaAtLastAvailableChapterStaysInContinueReading() {
        val completedProgress = progress(chapterNumber = 58f, completed = true)
        val metrics = recentReadingMetrics(
            progress = completedProgress,
            chapter = chapter(58f),
            availableChapters = listOf(1f, 58f).map(::chapter),
        )

        assertTrue(media(status = "RELEASING").shouldShowInContinueReading(completedProgress, metrics))
    }

    @Test
    fun unnumberedChapterDoesNotExposeNegativeProgress() {
        val metrics = recentReadingMetrics(
            progress = progress(chapterNumber = -1f),
            chapter = chapter(-1f),
            availableChapters = listOf(-1f, -1f).map(::chapter),
        )

        assertNull(metrics.currentChapterNumber)
        assertNull(metrics.lastAvailableChapterNumber)
        assertNull(metrics.overallProgress)
        assertFalse(metrics.reachedLastAvailableChapter)
    }

    private fun progress(chapterNumber: Float, completed: Boolean = false): ReadingProgress =
        ReadingProgress(
            mediaId = 1,
            chapterUrl = "chapter-$chapterNumber",
            chapterNumber = chapterNumber,
            pageIndex = 0,
            pageScrollOffset = 0,
            totalPages = 10,
            readerMode = ReaderMode.PAGED,
            completed = completed,
            updatedAtEpochMillis = 1L,
        )

    private fun chapter(chapterNumber: Float): SourceChapter =
        SourceChapter(
            sourceId = 1L,
            mangaUrl = "manga",
            url = "chapter-$chapterNumber",
            name = "Chapter $chapterNumber",
            chapterNumber = chapterNumber,
            scanlator = null,
            uploadedAtEpochMillis = null,
        )

    private fun media(status: String): AnilistMedia =
        AnilistMedia(
            id = 1,
            idMal = null,
            title = AnilistTitle("Manga", null, null, "Manga"),
            description = null,
            coverImage = null,
            bannerImage = null,
            chapters = null,
            volumes = null,
            format = null,
            status = status,
            averageScore = null,
            popularity = null,
            startDateYear = null,
            endDateYear = null,
            siteUrl = null,
            genres = emptyList(),
            synonyms = emptyList(),
            isAdult = false,
            updatedAtEpochSeconds = null,
        )
}
