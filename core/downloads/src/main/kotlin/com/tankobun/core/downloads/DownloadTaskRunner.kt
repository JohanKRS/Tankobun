package com.tankobun.core.downloads

import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.ReaderPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

interface DownloadPageFetcher {
    suspend fun pages(job: DownloadJob): List<ReaderPage>
    suspend fun bytes(page: ReaderPage): ByteArray
}

interface DownloadPageStorage {
    suspend fun storedPageIndexes(job: DownloadJob): Set<Int> = emptySet()
    suspend fun writePage(job: DownloadJob, page: ReaderPage, bytes: ByteArray): String
}

interface DownloadStateStore {
    suspend fun markRunning(jobId: String)
    suspend fun markPageComplete(jobId: String, completedPages: Int, pageCount: Int)
    suspend fun markComplete(jobId: String, pageCount: Int)
    suspend fun markFailed(jobId: String, message: String)
    suspend fun shouldContinue(jobId: String): Boolean
}

data class DownloadRetryPolicy(
    val maxAttempts: Int = 4,
    val initialDelayMillis: Long = 5_000L,
    val maxDelayMillis: Long = 20_000L,
)

class DownloadTaskRunner(
    private val pageFetcher: DownloadPageFetcher,
    private val pageStorage: DownloadPageStorage,
    private val stateStore: DownloadStateStore,
    private val retryPolicy: DownloadRetryPolicy = DownloadRetryPolicy(),
    private val pageConcurrency: Int = DEFAULT_PAGE_CONCURRENCY,
) {
    suspend fun run(job: DownloadJob) {
        try {
            if (!stateStore.shouldContinue(job.id)) return
            stateStore.markRunning(job.id)
            if (!stateStore.shouldContinue(job.id)) return
            val pages = runWithRetries(job.id, "Chapter pages") {
                pageFetcher.pages(job)
            }
            val validPageIndexes = pages.asSequence().map { it.index }.toSet()
            val storedPageIndexes = pageStorage.storedPageIndexes(job)
                .filterTo(mutableSetOf()) { it in validPageIndexes }
            val completedPages = AtomicInteger(storedPageIndexes.size)
            stateStore.markPageComplete(job.id, completedPages.get(), pages.size)

            val semaphore = Semaphore(pageConcurrency.coerceAtLeast(1))
            coroutineScope {
                pages.filterNot { it.index in storedPageIndexes }.map { page ->
                    async {
                        semaphore.withPermit {
                            if (!stateStore.shouldContinue(job.id)) throw DownloadStoppedException()
                            val bytes = runWithRetries(job.id, "Page ${page.index + 1}") {
                                pageFetcher.bytes(page)
                            }
                            pageStorage.writePage(job, page, bytes)
                            stateStore.markPageComplete(
                                jobId = job.id,
                                completedPages = completedPages.incrementAndGet(),
                                pageCount = pages.size,
                            )
                        }
                    }
                }.awaitAll()
            }
            stateStore.markComplete(job.id, pages.size)
        } catch (_: DownloadStoppedException) {
            return
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stateStore.markFailed(job.id, error.message ?: "Download failed")
        }
    }

    private suspend fun <T> runWithRetries(
        jobId: String,
        operation: String,
        block: suspend () -> T,
    ): T {
        val attempts = retryPolicy.maxAttempts.coerceAtLeast(1)
        var attempt = 1
        var delayMillis = retryPolicy.initialDelayMillis.coerceAtLeast(0)
        val maxDelayMillis = retryPolicy.maxDelayMillis.coerceAtLeast(0)
        var lastError: Throwable? = null

        while (attempt <= attempts) {
            if (!stateStore.shouldContinue(jobId)) throw DownloadStoppedException()
            try {
                return block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastError = error
                if (attempt == attempts) break
                if (!stateStore.shouldContinue(jobId)) throw DownloadStoppedException()
                if (delayMillis > 0L) {
                    delay(delayMillis)
                }
                delayMillis = nextRetryDelay(delayMillis, maxDelayMillis)
                attempt += 1
            }
        }

        throw DownloadRetryExhaustedException(
            operation = operation,
            attempts = attempts,
            cause = lastError ?: IllegalStateException("Unknown download error"),
        )
    }

    private fun nextRetryDelay(currentDelayMillis: Long, maxDelayMillis: Long): Long {
        if (currentDelayMillis <= 0L || maxDelayMillis <= 0L) return currentDelayMillis
        return (currentDelayMillis * 2).coerceAtMost(maxDelayMillis)
    }

    private companion object {
        const val DEFAULT_PAGE_CONCURRENCY = 5
    }
}

private class DownloadStoppedException : Exception()

private class DownloadRetryExhaustedException(
    operation: String,
    attempts: Int,
    cause: Throwable,
) : IllegalStateException(
    "$operation failed after $attempts attempts: ${cause.message ?: cause.javaClass.simpleName}",
    cause,
)
