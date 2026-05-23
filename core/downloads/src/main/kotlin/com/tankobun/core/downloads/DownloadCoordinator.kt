package com.tankobun.core.downloads

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tankobun.core.database.DownloadDao
import com.tankobun.core.database.DownloadPageDao
import com.tankobun.core.database.toEntity
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.DownloadJob

class DownloadCoordinator(
    context: Context,
    private val downloadDao: DownloadDao,
    private val downloadPageDao: DownloadPageDao,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val workManager = WorkManager.getInstance(context)

    suspend fun enqueue(job: DownloadJob) {
        downloadDao.upsertDownload(job.toEntity())
        schedule(job.id)
    }

    suspend fun schedulePending() {
        downloadDao.pendingDownloads().forEach { schedule(it.id) }
    }

    suspend fun pause(jobId: String) {
        downloadDao.updateState(jobId, DownloadState.PAUSED, nowMillis())
        workManager.cancelUniqueWork(workName(jobId))
    }

    suspend fun resume(jobId: String) {
        val job = downloadDao.getDownload(jobId) ?: return
        downloadDao.upsertDownload(
            job.copy(
                state = DownloadState.QUEUED,
                updatedAtEpochMillis = nowMillis(),
            ),
        )
        schedule(jobId)
    }

    suspend fun retry(jobId: String) {
        val job = downloadDao.getDownload(jobId) ?: return
        downloadPageDao.deletePagesForJob(jobId)
        downloadDao.upsertDownload(
            job.copy(
                state = DownloadState.QUEUED,
                pageCount = 0,
                completedPages = 0,
                updatedAtEpochMillis = nowMillis(),
            ),
        )
        schedule(jobId)
    }

    suspend fun remove(jobId: String) {
        workManager.cancelUniqueWork(workName(jobId))
        downloadPageDao.deletePagesForJob(jobId)
        downloadDao.deleteDownload(jobId)
    }

    private fun schedule(jobId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(DownloadWorker.JobIdKey to jobId))
            .build()
        workManager.enqueueUniqueWork(
            workName(jobId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun workName(jobId: String): String = "download-$jobId"
}
