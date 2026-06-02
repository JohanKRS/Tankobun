package com.tankobun.core.downloads

import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadTaskRunnerTest {
    @Test
    fun retriesPageBytesBeforeCompletingDownload() = runBlocking {
        val fetcher = FakePageFetcher(failPageAttempts = 2)
        val storage = RecordingPageStorage()
        val stateStore = RecordingDownloadStateStore()

        runner(fetcher, storage, stateStore, maxAttempts = 3).run(testJob())

        assertEquals(3, fetcher.pageBytesAttempts)
        assertEquals(listOf(0), storage.writtenPageIndexes)
        assertEquals(listOf(0 to 1, 1 to 1), stateStore.progress)
        assertEquals(DownloadState.COMPLETE, stateStore.state)
    }

    @Test
    fun marksDownloadFailedAfterPageRetriesAreExhausted() = runBlocking {
        val fetcher = FakePageFetcher(failPageAttempts = Int.MAX_VALUE)
        val storage = RecordingPageStorage()
        val stateStore = RecordingDownloadStateStore()

        runner(fetcher, storage, stateStore, maxAttempts = 2).run(testJob())

        assertEquals(2, fetcher.pageBytesAttempts)
        assertTrue(storage.writtenPageIndexes.isEmpty())
        assertEquals(DownloadState.FAILED, stateStore.state)
        assertTrue(stateStore.failedMessages.single().contains("Page 1 failed after 2 attempts"))
    }

    private fun runner(
        fetcher: DownloadPageFetcher,
        storage: DownloadPageStorage,
        stateStore: DownloadStateStore,
        maxAttempts: Int,
    ): DownloadTaskRunner =
        DownloadTaskRunner(
            pageFetcher = fetcher,
            pageStorage = storage,
            stateStore = stateStore,
            sourceRateLimiter = RespectfulRateLimiter(minSpacingMillis = 0),
            retryPolicy = DownloadRetryPolicy(
                maxAttempts = maxAttempts,
                initialDelayMillis = 0,
                maxDelayMillis = 0,
            ),
        )

    private fun testJob(): DownloadJob =
        DownloadJob(
            id = "job-1",
            mediaId = 10,
            sourceId = 20,
            mangaUrl = "/manga",
            chapterUrl = "/chapter",
            chapterName = "Chapter 1",
            state = DownloadState.QUEUED,
            pageCount = 0,
            completedPages = 0,
            retryCount = 0,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
        )

    private class FakePageFetcher(
        private val failPageAttempts: Int,
    ) : DownloadPageFetcher {
        var pageBytesAttempts = 0

        override suspend fun pages(job: DownloadJob): List<ReaderPage> =
            listOf(ReaderPage(index = 0, imageUrl = "https://example.test/page.jpg", cachedFilePath = null))

        override suspend fun bytes(page: ReaderPage): ByteArray {
            pageBytesAttempts += 1
            if (pageBytesAttempts <= failPageAttempts) {
                throw IllegalStateException("HTTP 522")
            }
            return byteArrayOf(1)
        }
    }

    private class RecordingPageStorage : DownloadPageStorage {
        val writtenPageIndexes = mutableListOf<Int>()

        override suspend fun writePage(job: DownloadJob, page: ReaderPage, bytes: ByteArray): String {
            writtenPageIndexes += page.index
            return "page-${page.index}"
        }
    }

    private class RecordingDownloadStateStore : DownloadStateStore {
        var state = DownloadState.QUEUED
        val progress = mutableListOf<Pair<Int, Int>>()
        val failedMessages = mutableListOf<String>()

        override suspend fun markRunning(jobId: String) {
            state = DownloadState.RUNNING
        }

        override suspend fun markPageComplete(jobId: String, completedPages: Int, pageCount: Int) {
            progress += completedPages to pageCount
        }

        override suspend fun markComplete(jobId: String, pageCount: Int) {
            state = DownloadState.COMPLETE
        }

        override suspend fun markFailed(jobId: String, message: String) {
            state = DownloadState.FAILED
            failedMessages += message
        }

        override suspend fun shouldContinue(jobId: String): Boolean =
            state == DownloadState.QUEUED || state == DownloadState.RUNNING
    }
}
