package com.tankobun.app.logic

import com.tankobun.app.updates.FoundChapter
import com.tankobun.app.updates.buildLibraryUpdateGroups
import com.tankobun.app.updates.unseenChapterCount
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

class ChapterStandingLogicTest {
    @Test
    fun countsEachChapterNumberOnceAndSkipsSpecials() {
        val chapters = listOf(
            chapter(1f, "Chapter 1"),
            chapter(2f, "Chapter 2", url = "/a/2"),
            chapter(2f, "Chapter 2", url = "/b/2"),
            chapter(2.5f, "Chapter 2.5"),
            chapter(3f, "Chapter 3 Extra"),
            chapter(-1f, "Oneshot"),
        )
        assertEquals(listOf("1", "2", "2.5"), chapters.distinctChapterNumbers().map { it.toPlainString() })
    }

    @Test
    fun remainingCountsNumbersAfterTheCurrentChapter() {
        val chapters = (1..7).map { chapter(it.toFloat(), "Chapter $it") } + chapter(7f, "Chapter 7", url = "/dup/7")
        val standing = chapterStanding(progress(4f, completed = false), null, chapters)!!

        assertEquals(4f, standing.currentNumber)
        assertEquals(3, standing.remaining)
        assertFalse(standing.caughtUp)
    }

    @Test
    fun finishingTheLastChapterIsCaughtUpButStartingItIsNot() {
        val chapters = (1..3).map { chapter(it.toFloat(), "Chapter $it") }

        assertTrue(chapterStanding(progress(3f, completed = true), null, chapters)!!.caughtUp)
        val started = chapterStanding(progress(3f, completed = false), null, chapters)!!
        assertEquals(0, started.remaining)
        assertFalse(started.caughtUp)
    }

    @Test
    fun standingNeedsANumberAndChapters() {
        assertNull(chapterStanding(progress(0f, completed = false), null, listOf(chapter(1f, "Chapter 1"))))
        assertNull(chapterStanding(progress(2f, completed = false), null, emptyList()))
        val fromChapter = chapterStanding(progress(0f, completed = false), chapter(2f, "Chapter 2"), listOf(chapter(3f, "Chapter 3")))
        assertEquals(2f, fromChapter!!.currentNumber)
    }

    @Test
    fun chapterNumberLabelsAreShort() {
        assertEquals("12", 12f.chapterNumberLabel())
        assertEquals("12.5", 12.5f.chapterNumberLabel())
    }

    @Test
    fun updateGroupsCollapseDuplicatesAndDropMangaOutsideTheLibrary() {
        val found = listOf(
            FoundChapter(1, chapter(10f, "Chapter 10", url = "/a/10"), foundAtEpochMillis = 100),
            FoundChapter(1, chapter(10f, "Chapter 10", url = "/b/10"), foundAtEpochMillis = 100),
            FoundChapter(1, chapter(11f, "Chapter 11"), foundAtEpochMillis = 200),
            FoundChapter(1, chapter(11.5f, "Chapter 11.5 Special"), foundAtEpochMillis = 200),
            FoundChapter(2, chapter(5f, "Chapter 5"), foundAtEpochMillis = 300),
            FoundChapter(3, chapter(1f, "Chapter 1"), foundAtEpochMillis = 400),
        )
        val groups = buildLibraryUpdateGroups(
            found = found,
            mediaById = mapOf(1 to media(1), 2 to media(2)),
            readUrlsByMedia = mapOf(1 to setOf("/ch/11.0")),
        )

        assertEquals(listOf(2, 1), groups.map { it.media.id })
        val first = groups.single { it.media.id == 1 }
        assertEquals(listOf("Chapter 11.5 Special", "Chapter 11", "Chapter 10"), first.chapters.map { it.chapter.name })
        assertEquals(2, first.newChapterCount)
        assertTrue(first.chapters.single { it.chapter.name == "Chapter 11" }.read)
        assertEquals(200L, first.newestFoundAtEpochMillis)
    }

    @Test
    fun unseenCountSkipsReadAndAlreadySeenChapters() {
        val groups = buildLibraryUpdateGroups(
            found = listOf(
                FoundChapter(1, chapter(10f, "Chapter 10"), foundAtEpochMillis = 100),
                FoundChapter(1, chapter(11f, "Chapter 11"), foundAtEpochMillis = 200),
                FoundChapter(1, chapter(12f, "Chapter 12"), foundAtEpochMillis = 200),
                FoundChapter(2, chapter(5f, "Chapter 5"), foundAtEpochMillis = 300),
            ),
            mediaById = mapOf(1 to media(1), 2 to media(2)),
            readUrlsByMedia = mapOf(1 to setOf("/ch/12.0")),
        )

        assertEquals(3, groups.unseenChapterCount(seenAtEpochMillis = 0))
        assertEquals(2, groups.unseenChapterCount(seenAtEpochMillis = 150))
        assertEquals(0, groups.unseenChapterCount(seenAtEpochMillis = 300))
    }

    private fun chapter(number: Float, name: String, url: String = "/ch/$number") = SourceChapter(
        sourceId = 1L,
        mangaUrl = "/manga",
        url = url,
        name = name,
        chapterNumber = number,
        scanlator = null,
        uploadedAtEpochMillis = null,
    )

    private fun progress(number: Float, completed: Boolean) = ReadingProgress(
        mediaId = 1,
        chapterUrl = "/ch/$number",
        chapterNumber = number,
        pageIndex = 0,
        pageScrollOffset = 0,
        totalPages = 10,
        readerMode = ReaderMode.PAGED,
        completed = completed,
        updatedAtEpochMillis = 0L,
    )

    private fun media(id: Int) = AnilistMedia(
        id = id,
        idMal = null,
        title = AnilistTitle(romaji = null, english = null, native = null, userPreferred = "Manga $id"),
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
}
