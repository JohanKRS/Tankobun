package com.tankobun.core.downloads

import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.network.RespectfulRateLimiter

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
}

class DownloadTaskRunner(
    private val pageFetcher: DownloadPageFetcher,
    private val pageStorage: DownloadPageStorage,
    private val stateStore: DownloadStateStore,
    private val sourceRateLimiter: RespectfulRateLimiter,
) {
    suspend fun run(job: DownloadJob) {
        runCatching {
            stateStore.markRunning(job.id)
            val pages = sourceRateLimiter.run { pageFetcher.pages(job) }
            pages.forEachIndexed { index, page ->
                val bytes = sourceRateLimiter.run { pageFetcher.bytes(page) }
                pageStorage.writePage(job, page, bytes)
                stateStore.markPageComplete(job.id, index + 1, pages.size)
            }
            stateStore.markComplete(job.id, pages.size)
        }.onFailure { error ->
            stateStore.markFailed(job.id, error.message ?: "Download failed")
        }
    }
}
