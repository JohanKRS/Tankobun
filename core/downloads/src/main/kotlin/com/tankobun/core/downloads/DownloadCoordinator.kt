package com.tankobun.core.downloads

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tankobun.core.database.DownloadDao
import com.tankobun.core.database.toEntity
import com.tankobun.core.model.DownloadJob

class DownloadCoordinator(
    context: Context,
    private val downloadDao: DownloadDao,
) {
    private val workManager = WorkManager.getInstance(context)

    suspend fun enqueue(job: DownloadJob) {
        downloadDao.upsertDownload(job.toEntity())
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.JobIdKey to job.id))
            .build()
        workManager.enqueueUniqueWork(
            "download-${job.id}",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
