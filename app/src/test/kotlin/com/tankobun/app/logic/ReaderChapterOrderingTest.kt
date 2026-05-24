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
