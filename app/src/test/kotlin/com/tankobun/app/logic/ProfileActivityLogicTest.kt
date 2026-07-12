package com.tankobun.app.logic

import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReadingProgress
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileActivityLogicTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.of(2026, 7, 12)
    private val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `computes periods and streak from completed chapters only`() {
        val progress = listOf(
            completed(0),
            completed(0, chapter = 2f),
            completed(1),
            completed(2),
            completed(8),
            completed(40),
            completed(0, chapter = 99f).copy(completed = false),
        )

        val result = progress.toLocalReadingActivity(nowEpochMillis = now, zoneId = zone)

        assertEquals(2, result.chaptersToday)
        assertEquals(4, result.chaptersLast7Days)
        assertEquals(5, result.chaptersLast30Days)
        assertEquals(3, result.currentStreakDays)
        assertEquals(3, result.longestStreakDays)
        assertEquals(5, result.totalReadingDays)
        assertEquals(6, result.chaptersTracked)
        assertEquals(2, result.last14Days.last())
    }

    private fun completed(daysAgo: Long, chapter: Float = daysAgo.toFloat() + 1f): ReadingProgress =
        ReadingProgress(
            mediaId = 1,
            chapterUrl = "chapter-$chapter",
            chapterNumber = chapter,
            pageIndex = 9,
            pageScrollOffset = 0,
            totalPages = 10,
            readerMode = ReaderMode.PAGED,
            completed = true,
            updatedAtEpochMillis = today.minusDays(daysAgo).atTime(20, 0).atZone(zone).toInstant().toEpochMilli(),
        )
}
