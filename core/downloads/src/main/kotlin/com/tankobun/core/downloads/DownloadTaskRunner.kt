package com.tankobun.core.downloads

import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.CancellationException

interface DownloadPageFetcher {
    suspend fun pages(job: DownloadJob): List<ReaderPage>
    suspend fun bytes(page: ReaderPage): ByteArray
}

interface DownloadPageStorage {
    suspend fun writePage(job: DownloadJob, page: ReaderPage, bytes: ByteArray): String
}

interface DownloadStateStore {
    suspend fun markRunning(jobId: String)
    suspend fun markPageComplete(jobId: String, completedPages: Int, pageCount: Int)
    suspend fun markComplete(jobId: String, pageCount: Int)
    suspend fun markFailed(jobId: String, message: String)
    suspend fun shouldContinue(jobId: String): Boolean
}

class DownloadTaskRunner(
    private val pageFetcher: DownloadPageFetcher,
    private val pageStorage: DownloadPageStorage,
    private val stateStore: DownloadStateStore,
    private val sourceRateLimiter: RespectfulRateLimiter,
) {
    suspend fun run(job: DownloadJob) {
        try {
            if (!stateStore.shouldContinue(job.id)) return
            stateStore.markRunning(job.id)
            if (!stateStore.shouldContinue(job.id)) return
            val pages = sourceRateLimiter.run { pageFetcher.pages(job) }
            stateStore.markPageComplete(job.id, 0, pages.size)
            pages.forEachIndexed { index, page ->
                if (!stateStore.shouldContinue(job.id)) return
                val bytes = sourceRateLimiter.run { pageFetcher.bytes(page) }
                pageStorage.writePage(job, page, bytes)
                stateStore.markPageComplete(job.id, index + 1, pages.size)
            }
            stateStore.markComplete(job.id, pages.size)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stateStore.markFailed(job.id, error.message ?: "Download failed")
        }
    }
}
