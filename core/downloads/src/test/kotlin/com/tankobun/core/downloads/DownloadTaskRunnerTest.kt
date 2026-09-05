package com.tankobun.core.downloads

import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.ReaderPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

class DownloadTaskRunnerTest {
    @Test
    fun neverMarksAnEmptyChapterAsDownloaded() = runBlocking {
        val stateStore = RecordingDownloadStateStore()
        val fetcher = object : DownloadPageFetcher {
            override suspend fun pages(job: DownloadJob) = emptyList<ReaderPage>()
            override suspend fun bytes(page: ReaderPage): ByteArray = error("No pages expected")
        }
        runner(fetcher, RecordingPageStorage(), stateStore, 1).run(testJob())
        assertEquals(DownloadState.FAILED, stateStore.state)
    }

    @Test
    fun pauseDuringFetchDoesNotWriteOrMarkComplete() = runBlocking {
        val stateStore = RecordingDownloadStateStore()
        val storage = RecordingPageStorage()
        val fetcher = object : DownloadPageFetcher {
            override suspend fun pages(job: DownloadJob) = listOf(ReaderPage(0, "https://example.test/page.jpg", null))
            override suspend fun bytes(page: ReaderPage): ByteArray {
                stateStore.state = DownloadState.PAUSED
                return byteArrayOf(1)
            }
        }
        runner(fetcher, storage, stateStore, 1).run(testJob())
        assertEquals(DownloadState.PAUSED, stateStore.state)
        assertTrue(storage.writtenPageIndexes.isEmpty())
    }

    @Test
    fun progressCannotGoBackwardsWhenPersistenceSuspends() = runBlocking {
        val stateStore = RecordingDownloadStateStore(delayFirstProgress = true)
        val fetcher = object : DownloadPageFetcher {
            override suspend fun pages(job: DownloadJob) = (0 until 12).map { ReaderPage(it, "https://example.test/$it.jpg", null) }
            override suspend fun bytes(page: ReaderPage) = byteArrayOf(1)
        }
        runner(fetcher, RecordingPageStorage(), stateStore, 1).run(testJob())
        assertEquals((0..12).toList(), stateStore.progress.map { it.first })
        assertEquals(DownloadState.COMPLETE, stateStore.state)
    }

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

    @Test
    fun fetchesUpToFivePagesConcurrently() = runBlocking {
        val activeFetches = AtomicInteger()
        val maxActiveFetches = AtomicInteger()
        val fetcher = object : DownloadPageFetcher {
            override suspend fun pages(job: DownloadJob): List<ReaderPage> =
                (0 until 10).map { index ->
                    ReaderPage(index, "https://example.test/$index.jpg", null)
                }

            override suspend fun bytes(page: ReaderPage): ByteArray {
                val active = activeFetches.incrementAndGet()
                maxActiveFetches.updateAndGet { maxOf(it, active) }
                delay(20L)
                activeFetches.decrementAndGet()
                return byteArrayOf(1)
            }
        }

        runner(fetcher, RecordingPageStorage(), RecordingDownloadStateStore(), maxAttempts = 1)
            .run(testJob())

        assertEquals(5, maxActiveFetches.get())
    }

    @Test
    fun resumesWithoutRefetchingPagesAlreadyStored() = runBlocking {
        val fetchedIndexes = mutableListOf<Int>()
        val fetcher = object : DownloadPageFetcher {
            override suspend fun pages(job: DownloadJob): List<ReaderPage> =
                (0 until 3).map { index ->
                    ReaderPage(index, "https://example.test/$index.jpg", null)
                }

            override suspend fun bytes(page: ReaderPage): ByteArray {
                fetchedIndexes += page.index
                return byteArrayOf(1)
            }
        }
        val storage = RecordingPageStorage(storedIndexes = setOf(0, 2))
        val stateStore = RecordingDownloadStateStore()

        runner(fetcher, storage, stateStore, maxAttempts = 1).run(testJob())

        assertEquals(listOf(1), fetchedIndexes)
        assertEquals(listOf(2 to 3, 3 to 3), stateStore.progress)
        assertEquals(DownloadState.COMPLETE, stateStore.state)
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

    private class RecordingPageStorage(
        private val storedIndexes: Set<Int> = emptySet(),
    ) : DownloadPageStorage {
        val writtenPageIndexes = Collections.synchronizedList(mutableListOf<Int>())

        override suspend fun storedPageIndexes(job: DownloadJob): Set<Int> = storedIndexes

        override suspend fun writePage(job: DownloadJob, page: ReaderPage, bytes: ByteArray): String {
            writtenPageIndexes += page.index
            return "page-${page.index}"
        }
    }

    private class RecordingDownloadStateStore(private val delayFirstProgress: Boolean = false) : DownloadStateStore {
        var state = DownloadState.QUEUED
        val progress = mutableListOf<Pair<Int, Int>>()
        val failedMessages = mutableListOf<String>()

        override suspend fun markRunning(jobId: String) {
            state = DownloadState.RUNNING
        }

        override suspend fun markPageComplete(jobId: String, completedPages: Int, pageCount: Int) {
            if (delayFirstProgress && completedPages == 1) delay(30)
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
