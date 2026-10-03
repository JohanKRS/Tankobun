package com.tankobun.app.ui.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import com.tankobun.app.AppLanguage
import com.tankobun.app.qa.LayoutQaScreens
import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter
import org.junit.Test

class ChapterRowQaScreens(language: AppLanguage) : LayoutQaScreens(language) {
    private val day = 24L * 60 * 60 * 1000
    private val now = System.currentTimeMillis()
    private val actions = ChapterRowActions({}, { _, _ -> }, {}, {}, {}, {}, {})

    private fun chapter(number: Float, name: String, uploadedAgo: Long?, scanlator: String? = "Lua Scans") = SourceChapter(
        sourceId = 1L,
        mangaUrl = "/manga",
        url = "/ch/$number",
        name = name,
        chapterNumber = number,
        scanlator = scanlator,
        uploadedAtEpochMillis = uploadedAgo?.let { now - it },
    )

    private fun download(chapter: SourceChapter, state: DownloadState) = DownloadJob(
        id = chapter.url,
        mediaId = 1,
        sourceId = 1L,
        mangaUrl = "/manga",
        chapterUrl = chapter.url,
        chapterName = chapter.name,
        state = state,
        pageCount = 20,
        completedPages = 6,
        retryCount = 0,
        createdAtEpochMillis = now,
        updatedAtEpochMillis = now,
    )

    @Test
    fun chapterRows() = capture("chapter-rows") {
        val today = chapter(131f, "Chapter 131", 2 * 60 * 60 * 1000L)
        val yesterday = chapter(130f, "Chapter 130 - The Long Way Back to the Harbor Town", day)
        val week = chapter(129f, "Chapter 129", 4 * day)
        val month = chapter(128f, "Chapter 128", 40 * day, scanlator = null)
        val old = chapter(12f, "Vol. 2 Chapter 12", 400 * day)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ChapterRow(today, actions, read = false, download = null, selectingForDownload = false, selectedForDownload = false, onToggleDownloadSelection = {})
            ChapterRow(
                yesterday, actions, read = false, download = download(yesterday, DownloadState.RUNNING),
                selectingForDownload = false, selectedForDownload = false, onToggleDownloadSelection = {},
                progress = ReadingProgress(1, yesterday.url, 130f, 4, 0, 20, ReaderMode.PAGED, false, now),
            )
            ChapterRow(week, actions, read = true, download = download(week, DownloadState.COMPLETE), selectingForDownload = false, selectedForDownload = false, onToggleDownloadSelection = {})
            ChapterRow(month, actions, read = true, download = null, selectingForDownload = false, selectedForDownload = false, onToggleDownloadSelection = {})
            ChapterRow(old, actions, read = false, download = download(old, DownloadState.FAILED), selectingForDownload = false, selectedForDownload = false, onToggleDownloadSelection = {})
            ChapterRow(old, actions, read = false, download = null, selectingForDownload = true, selectedForDownload = true, onToggleDownloadSelection = {})
        }
    }
}
