package com.tankobun.core.extensions.novel

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class LnReaderChapterTest {
    @Test fun unnumberedChaptersKeepTheirReadingSequenceAndSourceIdentity() {
        val chapters = JSONArray("""[{"path":"dawn","name":"Dawn"},{"path":"night","name":"Night"}]""").lnReaderChapters()
        assertEquals(listOf("night", "dawn"), chapters.map { it.url })
        assertEquals(listOf(-1f, -1f), chapters.map { it.chapter_number })
        assertEquals("dawn", chapters.last().url)
    }

    @Test fun metadataAcceptsDateOnlyIsoDatesAndMissingDates() {
        val chapters = JSONArray("""[
            {"path":"a","name":"First","chapterNumber":1,"releaseTime":"2026-01-02","scanlator":["A","B"]},
            {"path":"b","name":"Second","chapterNumber":2.5,"releaseTime":"2026-01-03T12:30:00Z"},
            {"path":"c","name":"Epilogue"}
        ]""").lnReaderChapters()
        assertEquals(0L, chapters[0].date_upload)
        assertEquals(Instant.parse("2026-01-03T12:30:00Z").toEpochMilli(), chapters[1].date_upload)
        assertEquals(2.5f, chapters[1].chapter_number)
        assertEquals(Instant.parse("2026-01-02T00:00:00Z").toEpochMilli(), chapters[2].date_upload)
        assertEquals("A, B", chapters[2].scanlator)
    }
}
