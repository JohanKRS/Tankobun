package com.tankobun.app.logic

import com.tankobun.core.database.DownloadPageEntity
import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadLogicTest {
    @Test
    fun buildsStorageSummaryFromJobsAndPages() {
        val downloads = listOf(
            downloadJob(
                id = "complete",
                mediaId = 1,
                sourceId = 10,
                chapterUrl = "chapter-1",
                state = DownloadState.COMPLETE,
            ),
            downloadJob(
                id = "queued",
                mediaId = 1,
                sourceId = 10,
                chapterUrl = "chapter-2",
                state = DownloadState.QUEUED,
            ),
            downloadJob(
                id = "other",
                mediaId = 2,
                sourceId = 20,
                chapterUrl = "chapter-3",
                state = DownloadState.FAILED,
            ),
        )
        val pages = listOf(
            downloadPage(mediaId = 1, sourceId = 10, chapterUrl = "chapter-1", filePath = "a"),
            downloadPage(mediaId = 1, sourceId = 10, chapterUrl = "chapter-1", filePath = "b"),
            downloadPage(mediaId = 2, sourceId = 20, chapterUrl = "chapter-3", filePath = "c"),
        )

        val summary = buildDownloadStorageSummary(
            downloads = downloads,
            pages = pages,
            fileSize = { path -> mapOf("a" to 100L, "b" to 50L, "c" to 200L).getValue(path) },
        )

        assertEquals(350L, summary.totalBytes)
        assertEquals(2, summary.items.size)
        assertEquals(2, summary.items[0].mediaId)
        assertEquals(200L, summary.items[0].bytes)
        assertEquals(1, summary.items[0].chapterCount)
        assertEquals(1, summary.items[1].mediaId)
        assertEquals(150L, summary.items[1].bytes)
        assertEquals(2, summary.items[1].chapterCount)
        assertEquals(1, summary.items[1].completedChapterCount)
        assertEquals(1, summary.items[1].activeChapterCount)
        assertEquals(2, summary.items[1].pageCount)
    }

    private fun downloadJob(
        id: String,
        mediaId: Int,
        sourceId: Long,
        chapterUrl: String,
        state: DownloadState,
    ): DownloadJob = DownloadJob(
        id = id,
        mediaId = mediaId,
        sourceId = sourceId,
        mangaUrl = "manga",
        chapterUrl = chapterUrl,
        chapterName = chapterUrl,
        state = state,
        pageCount = 0,
        completedPages = 0,
        retryCount = 0,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L,
    )

    private fun downloadPage(
        mediaId: Int,
        sourceId: Long,
        chapterUrl: String,
        filePath: String,
    ): DownloadPageEntity = DownloadPageEntity(
        jobId = "$mediaId-$sourceId-$chapterUrl-$filePath",
        mediaId = mediaId,
        sourceId = sourceId,
        mangaUrl = "manga",
        chapterUrl = chapterUrl,
        pageIndex = 0,
        imageUrl = "https://example.invalid/$filePath",
        filePath = filePath,
        updatedAtEpochMillis = 0L,
    )
}
