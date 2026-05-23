package com.tankobun.core.downloads

import com.tankobun.core.database.DownloadDao
import com.tankobun.core.model.DownloadState

class RoomDownloadStateStore(
    private val downloadDao: DownloadDao,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : DownloadStateStore {
    override suspend fun markRunning(jobId: String) {
        downloadDao.updateState(jobId, DownloadState.RUNNING, nowMillis())
    }

    override suspend fun markPageComplete(jobId: String, completedPages: Int, pageCount: Int) {
        val job = downloadDao.getDownload(jobId) ?: return
        downloadDao.upsertDownload(
            job.copy(
                pageCount = pageCount,
                completedPages = completedPages,
                updatedAtEpochMillis = nowMillis(),
            ),
        )
    }

    override suspend fun markComplete(jobId: String, pageCount: Int) {
        val job = downloadDao.getDownload(jobId) ?: return
        downloadDao.upsertDownload(
            job.copy(
                state = DownloadState.COMPLETE,
                pageCount = pageCount,
                completedPages = pageCount,
                updatedAtEpochMillis = nowMillis(),
            ),
        )
    }

    override suspend fun markFailed(jobId: String, message: String) {
        val job = downloadDao.getDownload(jobId) ?: return
        downloadDao.upsertDownload(
            job.copy(
                state = DownloadState.FAILED,
                retryCount = job.retryCount + 1,
                updatedAtEpochMillis = nowMillis(),
            ),
        )
    }

    override suspend fun shouldContinue(jobId: String): Boolean {
        val state = downloadDao.getDownload(jobId)?.state ?: return false
        return state == DownloadState.QUEUED || state == DownloadState.RUNNING
    }
}
