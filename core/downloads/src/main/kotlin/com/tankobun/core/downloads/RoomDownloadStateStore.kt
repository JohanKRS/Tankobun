package com.tankobun.core.downloads

import com.tankobun.core.database.DownloadDao
import com.tankobun.core.model.DownloadState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomDownloadStateStore(
    private val downloadDao: DownloadDao,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : DownloadStateStore {
    private val continuationLock = Mutex()
    @Volatile
    private var cachedShouldContinue: Boolean? = null
    @Volatile
    private var continuationCheckedAtMillis: Long = Long.MIN_VALUE

    override suspend fun markRunning(jobId: String) {
        cacheContinuation(downloadDao.markRunningIfActive(jobId, nowMillis()) > 0)
    }

    override suspend fun markPageComplete(jobId: String, completedPages: Int, pageCount: Int) {
        downloadDao.updateProgress(jobId, completedPages, pageCount, nowMillis())
    }

    override suspend fun markComplete(jobId: String, pageCount: Int) {
        downloadDao.markComplete(jobId, pageCount, nowMillis())
        cacheContinuation(false)
    }

    override suspend fun markFailed(jobId: String, message: String) {
        downloadDao.markFailed(jobId, nowMillis())
        cacheContinuation(false)
    }

    override suspend fun shouldContinue(jobId: String): Boolean = continuationLock.withLock {
        val now = nowMillis()
        cachedShouldContinue?.takeIf {
            now - continuationCheckedAtMillis in 0 until CONTINUATION_CACHE_MILLIS
        }?.let { return@withLock it }

        val state = downloadDao.getDownload(jobId)?.state
        (state == DownloadState.QUEUED || state == DownloadState.RUNNING).also { shouldContinue ->
            cachedShouldContinue = shouldContinue
            continuationCheckedAtMillis = now
        }
    }

    private fun cacheContinuation(shouldContinue: Boolean) {
        cachedShouldContinue = shouldContinue
        continuationCheckedAtMillis = nowMillis()
    }

    private companion object {
        const val CONTINUATION_CACHE_MILLIS = 250L
    }
}
