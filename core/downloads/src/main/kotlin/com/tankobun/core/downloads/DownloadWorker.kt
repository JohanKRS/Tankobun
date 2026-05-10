package com.tankobun.core.downloads

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val jobId = inputData.getString(JobIdKey) ?: return Result.failure()
        val delegate = DownloadWorkerDelegateRegistry.delegate ?: return Result.retry()
        return if (delegate.run(jobId)) Result.success() else Result.retry()
    }

    companion object {
        const val JobIdKey = "download.job.id"
    }
}

fun interface DownloadWorkerDelegate {
    suspend fun run(jobId: String): Boolean
}

object DownloadWorkerDelegateRegistry {
    @Volatile
    var delegate: DownloadWorkerDelegate? = null
}
