package com.tankobun.app.logic

import com.tankobun.app.state.LocalReadingActivity
import com.tankobun.core.model.ReadingProgress
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal fun List<ReadingProgress>.toLocalReadingActivity(
    nowEpochMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): LocalReadingActivity {
    val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate()
    val completedDates = asSequence()
        .filter { it.completed && it.updatedAtEpochMillis > 0L }
        .map { progress -> Instant.ofEpochMilli(progress.updatedAtEpochMillis).atZone(zoneId).toLocalDate() }
        .toList()
    val countsByDate = completedDates.groupingBy { it }.eachCount()
    val activeDates = countsByDate.keys

    fun chaptersSince(start: LocalDate): Int =
        countsByDate.entries.sumOf { (date, count) -> if (!date.isBefore(start) && !date.isAfter(today)) count else 0 }

    val last7Start = today.minusDays(6)
    val last30Start = today.minusDays(29)
    val chaptersLast30Days = chaptersSince(last30Start)
    val activeDaysLast30 = activeDates.count { !it.isBefore(last30Start) && !it.isAfter(today) }

    val streakStart = when {
        today in activeDates -> today
        today.minusDays(1) in activeDates -> today.minusDays(1)
        else -> null
    }
    var currentStreak = 0
    var cursor = streakStart
    while (cursor != null && cursor in activeDates) {
        currentStreak += 1
        cursor = cursor.minusDays(1)
    }

    var longestStreak = 0
    var runningStreak = 0
    var previous: LocalDate? = null
    activeDates.sorted().forEach { date ->
        runningStreak = if (previous != null && date == previous.plusDays(1)) runningStreak + 1 else 1
        longestStreak = maxOf(longestStreak, runningStreak)
        previous = date
    }

    return LocalReadingActivity(
        generatedAtEpochMillis = nowEpochMillis,
        chaptersTracked = completedDates.size,
        chaptersToday = countsByDate[today] ?: 0,
        chaptersLast7Days = chaptersSince(last7Start),
        chaptersLast30Days = chaptersLast30Days,
        averagePerActiveDay30 = if (activeDaysLast30 > 0) chaptersLast30Days.toDouble() / activeDaysLast30 else 0.0,
        currentStreakDays = currentStreak,
        longestStreakDays = longestStreak,
        totalReadingDays = activeDates.size,
        last14Days = (13L downTo 0L).map { daysAgo -> countsByDate[today.minusDays(daysAgo)] ?: 0 },
    )
}
