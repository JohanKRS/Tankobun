package com.tankobun.app.logic

import com.tankobun.app.state.ReaderChapterSegment
import com.tankobun.app.state.ReaderLoadError
import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderStateLogicTest {
    @Test
    fun readerLoadErrorIgnoresStaleChapter() {
        val activeChapter = chapter("active")
        val staleChapter = chapter("stale")
        val state = TankobunUiState(activeChapter = activeChapter, busy = true)

        val next = state.withReaderLoadError(
            chapter = staleChapter,
            readerError = ReaderLoadError("No pages", "Stale load"),
        )

        assertSame(state, next)
    }

    @Test
    fun readerPagesLoadedUpdatesOnlyMatchingActiveChapter() {
        val activeChapter = chapter("active")
        val pages = listOf(page(0), page(1))

        val next = TankobunUiState(
            activeChapter = activeChapter,
            readerError = ReaderLoadError("Old", "Old error"),
            busy = true,
        ).withReaderPagesLoaded(
            chapter = activeChapter,
            pages = pages,
            pageIndex = 1,
            pageScrollOffset = 24,
        )

        assertEquals(activeChapter, next.activeChapter)
        assertEquals(pages, next.readerPages)
        assertNull(next.readerError)
        assertEquals(1, next.currentPageIndex)
        assertEquals(24, next.currentPageScrollOffset)
        assertFalse(next.busy)
    }

    @Test
    fun activatedNextSegmentKeepsOldChapterAsPreviousSegment() {
        val activeChapter = chapter("active")
        val nextChapter = chapter("next")
        val activeSegment = ReaderChapterSegment(activeChapter, listOf(page(0)))
        val nextSegment = ReaderChapterSegment(nextChapter, listOf(page(0), page(1)))

        val next = TankobunUiState(
            activeChapter = activeChapter,
            readerPages = activeSegment.pages,
        ).withActivatedReaderSegment(
            targetSegment = nextSegment,
            oldActiveSegment = activeSegment,
            pageIndex = 1,
            pageScrollOffset = 8,
            direction = ReaderSegmentDirection.NEXT,
        )

        assertEquals(nextChapter, next.activeChapter)
        assertEquals(nextSegment.pages, next.readerPages)
        assertEquals(activeSegment, next.readerPreviousSegment)
        assertNull(next.readerNextSegment)
        assertEquals(1, next.currentPageIndex)
        assertEquals(8, next.currentPageScrollOffset)
    }

    @Test
    fun adjacentSegmentUpdateKeepsOppositeSide() {
        val activeChapter = chapter("active")
        val previousSegment = ReaderChapterSegment(chapter("previous"), listOf(page(0)))
        val nextSegment = ReaderChapterSegment(chapter("next"), listOf(page(0), page(1)))

        val next = TankobunUiState(
            activeChapter = activeChapter,
            readerPreviousSegment = previousSegment,
        ).withReaderAdjacentSegment(
            chapter = activeChapter,
            segment = nextSegment,
            direction = ReaderSegmentDirection.NEXT,
        )

        assertEquals(previousSegment, next.readerPreviousSegment)
        assertEquals(nextSegment, next.readerNextSegment)
    }

    @Test
    fun recentProgressOpenedResetsReaderAndUsesExistingEntry() {
        val media = media(42, "Manga")
        val entry = entry(mediaId = 42, progress = 12, customLists = listOf("Favorites"))
        val progress = progress(chapterUrl = "chapter-1")
        val chapter = chapter("chapter-1")

        val next = TankobunUiState(
            activeChapter = chapter("old"),
            readerPages = listOf(page(0)),
            selectedSourceId = 5,
            anilistScoreFormat = AnilistScoreFormat.POINT_100,
        ).withRecentProgressOpened(
            item = RecentReadingProgress(media = media, progress = progress, chapter = chapter),
            existingEntry = entry,
        )

        assertEquals(media, next.selectedMedia)
        assertEquals(entry, next.selectedListEntry)
        assertEquals(1L, next.selectedSourceId)
        assertEquals(progress, next.latestProgress)
        assertEquals(mapOf("chapter-1" to progress), next.chapterProgress)
        assertNull(next.activeChapter)
        assertTrue(next.readerPages.isEmpty())
        assertEquals("12", next.trackingProgress)
        assertEquals(setOf("Favorites"), next.trackingCustomLists)
    }

    private fun media(id: Int, title: String): AnilistMedia =
        AnilistMedia(
            id = id,
            idMal = null,
            title = AnilistTitle(
                romaji = title,
                english = null,
                native = null,
                userPreferred = title,
            ),
            description = null,
            coverImage = null,
            bannerImage = null,
            chapters = null,
            volumes = null,
            format = null,
            status = null,
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

    private fun entry(
        mediaId: Int,
        progress: Int = 0,
        customLists: List<String> = emptyList(),
    ): AnilistListEntry =
        AnilistListEntry(
            id = mediaId * 10,
            mediaId = mediaId,
            status = MediaStatus.CURRENT,
            progress = progress,
            score = null,
            notes = null,
            private = false,
            customLists = customLists,
            updatedAtEpochSeconds = null,
        )

    private fun chapter(url: String): SourceChapter =
        SourceChapter(
            sourceId = 1L,
            mangaUrl = "manga",
            url = url,
            name = url,
            chapterNumber = 1f,
            scanlator = null,
            uploadedAtEpochMillis = null,
        )

    private fun page(index: Int): ReaderPage =
        ReaderPage(index = index, imageUrl = "https://example.com/$index.jpg", cachedFilePath = null)

    private fun progress(chapterUrl: String): ReadingProgress =
        ReadingProgress(
            mediaId = 42,
            chapterUrl = chapterUrl,
            chapterNumber = 1f,
            pageIndex = 0,
            pageScrollOffset = 0,
            totalPages = 2,
            readerMode = ReaderMode.PAGED,
            completed = false,
            updatedAtEpochMillis = 1L,
        )
}
