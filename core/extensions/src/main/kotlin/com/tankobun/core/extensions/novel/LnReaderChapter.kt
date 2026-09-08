package com.tankobun.core.extensions.novel

import eu.kanade.tachiyomi.source.model.SChapter
import org.json.JSONArray
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal fun JSONArray.lnReaderChapters(): List<SChapter> =
    // LNReader lists start with the first chapter; the manga source contract starts with the latest.
    // Preserve unnumbered chapters without fabricating metadata or changing their source paths.
    (length() - 1 downTo 0).map { index ->
        val row = getJSONObject(index)
        SChapter.create().apply {
            url = row.getString("path")
            name = row.getString("name")
            chapter_number = row.optDouble("chapterNumber", -1.0).toFloat()
                .takeIf { it.isFinite() } ?: -1f
            scanlator = when (val value = row.opt("scanlator")) {
                is JSONArray -> (0 until value.length()).mapNotNull { value.optString(it).takeIf(String::isNotBlank) }.joinToString(", ")
                is String -> value
                else -> null
            }
            date_upload = row.optString("releaseTime").let { date ->
                runCatching { Instant.parse(date).toEpochMilli() }
                    .recoverCatching { LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
                    .getOrDefault(0L)
            }
        }
    }
