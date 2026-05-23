package com.tankobun.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tankobun.core.database.toModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.util.Locale
import java.util.concurrent.TimeUnit

class ScheduledBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val app = applicationContext as? TankobunApplication ?: error("Tankobun application not available")
            val container = app.container
            val settings = container.settingsStore
            val schedule = settings.backupSchedule()
            val folderUri = settings.backupFolderUri()?.let(Uri::parse)
            if (schedule == BackupSchedule.OFF || folderUri == null) {
                Result.success()
            } else {
                val items = backupLibraryItems(container)
                if (items.isEmpty()) {
                    Result.success()
                } else {
                    val fileUri = DocumentsContract.createDocument(
                        applicationContext.contentResolver,
                        folderUri,
                        "text/xml",
                        scheduledBackupFileName(settings.viewerName()),
                    ) ?: error("Could not create backup file")
                    val xml = buildMyAnimeListBackupXml(
                        items = items,
                        viewerName = settings.viewerName(),
                        scoreFormat = settings.anilistScoreFormat(),
                    )
                    val output = applicationContext.contentResolver.openOutputStream(fileUri)
                        ?: error("Could not open backup file")
                    OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                        writer.write(xml)
                    }
                    settings.saveLastScheduledBackupAtEpochMillis(System.currentTimeMillis())
                    Result.success()
                }
            }
        }.getOrElse { Result.retry() }
    }
}

object ScheduledBackupWork {
    private const val UNIQUE_NAME = "tankobun-scheduled-anilist-backup"

    fun sync(context: Context, schedule: BackupSchedule) {
        if (schedule == BackupSchedule.OFF) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
            return
        }
        enqueue(context, schedule, ExistingPeriodicWorkPolicy.KEEP)
    }

    fun update(context: Context, schedule: BackupSchedule) {
        if (schedule == BackupSchedule.OFF) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
            return
        }
        enqueue(context, schedule, ExistingPeriodicWorkPolicy.UPDATE)
    }

    private fun enqueue(
        context: Context,
        schedule: BackupSchedule,
        policy: ExistingPeriodicWorkPolicy,
    ) {
        val intervalMillis = schedule.intervalMillis()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<ScheduledBackupWorker>(intervalMillis, TimeUnit.MILLISECONDS)
            .setInitialDelay(intervalMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_NAME, policy, request)
    }
}

private suspend fun backupLibraryItems(container: AppContainer): List<LibraryItem> {
    val media = container.database.mediaDao().cachedMedia().associateBy { it.id }
    return container.database.listEntryDao().cachedEntries()
        .mapNotNull { entry ->
            media[entry.mediaId]?.toModel()?.let { cachedMedia ->
                LibraryItem(cachedMedia, entry.toModel())
            }
        }
        .distinctBy { it.media.id }
        .sortedBy { it.media.title.userPreferred.lowercase(Locale.ROOT) }
}

private fun scheduledBackupFileName(viewerName: String?): String {
    val userPart = viewerName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
        ?: "user"
    return "tankobun_anilist_backup_${userPart}_${System.currentTimeMillis()}.xml"
}

private fun BackupSchedule.intervalMillis(): Long =
    when (this) {
        BackupSchedule.OFF -> Long.MAX_VALUE
        BackupSchedule.DAILY -> 24L * 60L * 60L * 1_000L
        BackupSchedule.WEEKLY -> 7L * 24L * 60L * 60L * 1_000L
        BackupSchedule.MONTHLY -> 30L * 24L * 60L * 60L * 1_000L
    }
