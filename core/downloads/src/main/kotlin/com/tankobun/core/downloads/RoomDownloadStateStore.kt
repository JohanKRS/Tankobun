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
        downloadDao.updateProgress(jobId, completedPages, pageCount, nowMillis())
    }

    override suspend fun markComplete(jobId: String, pageCount: Int) {
        downloadDao.markComplete(jobId, pageCount, nowMillis())
    }

    override suspend fun markFailed(jobId: String, message: String) {
        downloadDao.markFailed(jobId, nowMillis())
    }

    override suspend fun shouldContinue(jobId: String): Boolean {
        val state = downloadDao.getDownload(jobId)?.state ?: return false
        return state == DownloadState.QUEUED || state == DownloadState.RUNNING
    }
}
