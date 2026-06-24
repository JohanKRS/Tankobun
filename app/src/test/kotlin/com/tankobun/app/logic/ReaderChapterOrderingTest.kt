package com.tankobun.app.logic

import com.tankobun.core.model.SourceChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderChapterOrderingTest {
    @Test
    fun nextAndPreviousUseChapterNumbersWhenAvailable() {
        val chapters = listOf(chapter("c1", 1f), chapter("c2", 2f), chapter("c3", 3f))

        assertEquals(chapters[2], chapters.nextInReadingOrderAfter(chapters[1]))
        assertEquals(chapters[0], chapters.previousInReadingOrderBefore(chapters[1]))
        assertNull(chapters.nextInReadingOrderAfter(chapters[2]))
        assertNull(chapters.previousInReadingOrderBefore(chapters[0]))
    }

    @Test
    fun nextAndPreviousFallBackToSourceOrderForUnknownChapterNumbers() {
        val chapters = listOf(chapter("newest", 0f), chapter("middle", 0f), chapter("oldest", 0f))

        assertEquals(chapters[0], chapters.nextInReadingOrderAfter(chapters[1]))
        assertEquals(chapters[2], chapters.previousInReadingOrderBefore(chapters[1]))
    }

    @Test
    fun trackerProgressUsesParsedChapterNumberWhenAvailable() {
        val chapters = listOf(chapter("c1", 1f), chapter("c2", 2.5f), chapter("c3", 3f))

        assertEquals(2, chapters.trackerProgressForChapter(chapters[1]))
    }

    @Test
    fun trackerProgressFallsBackToReadingOrderForUnknownChapterNumbers() {
        val chapters = listOf(chapter("newest", 0f), chapter("middle", 0f), chapter("oldest", 0f))

        assertEquals(1, chapters.trackerProgressForChapter(chapters[2]))
        assertEquals(2, chapters.trackerProgressForChapter(chapters[1]))
        assertEquals(3, chapters.trackerProgressForChapter(chapters[0]))
    }

    @Test
    fun trackerProgressIgnoresUnknownSpecialsWhenNormalChaptersHaveNumbers() {
        val chapters = listOf(chapter("c13", 13f), chapter("omake", 0f), chapter("c12", 12f))

        assertNull(chapters.trackerProgressForChapter(chapters[1]))
    }

    @Test
    fun trackerProgressIsNullWhenChapterIsNotInSourceListAndNumberIsUnknown() {
        val chapters = listOf(chapter("newest", 0f), chapter("oldest", 0f))

        assertNull(chapters.trackerProgressForChapter(chapter("missing", 0f)))
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
