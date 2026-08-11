package com.tankobun.app.logic

import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.MediaStatus
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
    fun currentSourceReplacesChapterFromOldSourceByChapterNumber() {
        val oldProgress = progress(chapterNumber = 12f)
        val item = com.tankobun.app.state.RecentReadingProgress(
            media = media(status = "RELEASING"),
            progress = oldProgress,
            chapter = chapter(12f, sourceId = 1L, mangaUrl = "old-manga"),
            sourcePackageName = "old.package",
        )
        val currentChapters = listOf(
            chapter(13f, sourceId = 2L, mangaUrl = "current-manga", url = "current-13"),
            chapter(12f, sourceId = 2L, mangaUrl = "current-manga", url = "current-12"),
        )

        val resolved = item.withCurrentSourceChapters("current.package", currentChapters)

        assertEquals(2L, resolved.chapter?.sourceId)
        assertEquals("current-manga", resolved.chapter?.mangaUrl)
        assertEquals(12f, resolved.chapter?.chapterNumber)
        assertEquals("current.package", resolved.sourcePackageName)
    }

    @Test
    fun completedProgressContinuesAtNextChapterFromCurrentSource() {
        val item = com.tankobun.app.state.RecentReadingProgress(
            media = media(status = "RELEASING"),
            progress = progress(chapterNumber = 12f, completed = true),
            chapter = chapter(12f, sourceId = 1L, mangaUrl = "old-manga"),
        )
        val currentChapters = listOf(
            chapter(12f, sourceId = 2L, mangaUrl = "current-manga", url = "current-12"),
            chapter(13f, sourceId = 2L, mangaUrl = "current-manga", url = "current-13"),
        )

        val resolved = item.withCurrentSourceChapters("current.package", currentChapters)

        assertEquals(13f, resolved.chapter?.chapterNumber)
        assertEquals(2L, resolved.chapter?.sourceId)
    }

    @Test
    fun currentBindingWithoutCachedChaptersNeverFallsBackToOldSource() {
        val item = com.tankobun.app.state.RecentReadingProgress(
            media = media(status = "RELEASING"),
            progress = progress(chapterNumber = 12f),
            chapter = chapter(12f, sourceId = 1L, mangaUrl = "old-manga"),
            sourcePackageName = "old.package",
        )

        val resolved = item.withCurrentSourceChapters("current.package", emptyList())

        assertNull(resolved.chapter)
        assertEquals("current.package", resolved.sourcePackageName)
    }

    @Test
    fun currentSourcePrefersExactChapterUrlEvenWhenNumberIsUnknown() {
        val unknownProgress = progress(chapterNumber = 0f).copy(chapterUrl = "shared-chapter-url")
        val item = com.tankobun.app.state.RecentReadingProgress(
            media = media(status = "RELEASING"),
            progress = unknownProgress,
            chapter = chapter(
                chapterNumber = 0f,
                sourceId = 1L,
                mangaUrl = "old-manga",
                url = "shared-chapter-url",
            ),
        )
        val expected = chapter(
            chapterNumber = 34f,
            sourceId = 2L,
            mangaUrl = "current-manga",
            url = "shared-chapter-url",
        )

        val resolved = item.withCurrentSourceChapters(
            sourcePackageName = "current.package",
            chapters = listOf(chapter(1f, sourceId = 2L), expected),
        )

        assertEquals(expected, resolved.chapter)
        assertEquals("current.package", resolved.sourcePackageName)
    }

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

        assertFalse(media(status = "FINISHED").shouldShowInContinueReading(entry(), completedProgress, metrics))
    }

    @Test
    fun releasingMangaAtLastAvailableChapterStaysInContinueReading() {
        val completedProgress = progress(chapterNumber = 58f, completed = true)
        val metrics = recentReadingMetrics(
            progress = completedProgress,
            chapter = chapter(58f),
            availableChapters = listOf(1f, 58f).map(::chapter),
        )

        assertTrue(media(status = "RELEASING").shouldShowInContinueReading(entry(), completedProgress, metrics))
    }

    @Test
    fun mangaOutsideReadingCategoryLeavesContinueReading() {
        val progress = progress(chapterNumber = 12f)
        val metrics = recentReadingMetrics(
            progress = progress,
            chapter = chapter(12f),
            availableChapters = listOf(12f, 20f).map(::chapter),
        )

        assertFalse(
            media(status = "RELEASING").shouldShowInContinueReading(
                entry(status = MediaStatus.COMPLETED),
                progress,
                metrics,
            ),
        )
        assertFalse(media(status = "RELEASING").shouldShowInContinueReading(null, progress, metrics))
    }

    @Test
    fun entryHiddenFromReadingCategoryLeavesContinueReading() {
        assertFalse(entry(hiddenFromStatusLists = true).isInReadingCategory())
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

    private fun entry(
        status: MediaStatus = MediaStatus.CURRENT,
        hiddenFromStatusLists: Boolean = false,
    ): AnilistListEntry =
        AnilistListEntry(
            id = 1,
            mediaId = 1,
            status = status,
            progress = 0,
            score = null,
            notes = null,
            private = false,
            customLists = emptyList(),
            updatedAtEpochSeconds = null,
            hiddenFromStatusLists = hiddenFromStatusLists,
        )

    private fun chapter(
        chapterNumber: Float,
        sourceId: Long = 1L,
        mangaUrl: String = "manga",
        url: String = "chapter-$chapterNumber",
    ): SourceChapter =
        SourceChapter(
            sourceId = sourceId,
            mangaUrl = mangaUrl,
            url = url,
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
