package com.tankobun.core.reader

import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceChapter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderProgressCalculatorTest {
    @Test
    fun marksLastPageAsCompleted() {
        val chapter = SourceChapter(1, "/manga", "/chapter-1", "Chapter 1", 1f, null, null)
        val session = ReaderSession(
            mediaId = 10,
            chapter = chapter,
            pages = listOf(
                ReaderPage(0, "https://example.test/1.jpg", null),
                ReaderPage(1, "https://example.test/2.jpg", null),
            ),
            mode = ReaderMode.PAGED,
            currentPageIndex = 1,
        )

        assertTrue(ReaderProgressCalculator().progressFor(session, nowMillis = 100).completed)
    }

    @Test
    fun keepsMiddlePageIncomplete() {
        val chapter = SourceChapter(1, "/manga", "/chapter-1", "Chapter 1", 1f, null, null)
        val session = ReaderSession(
            mediaId = 10,
            chapter = chapter,
            pages = listOf(
                ReaderPage(0, "https://example.test/1.jpg", null),
                ReaderPage(1, "https://example.test/2.jpg", null),
            ),
            mode = ReaderMode.WEBTOON,
            currentPageIndex = 0,
        )

        assertFalse(ReaderProgressCalculator().progressFor(session, nowMillis = 100).completed)
    }
}
