package com.tankobun.app

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tankobun.app.backup.AppSettingsBackupDataSource
import com.tankobun.app.backup.buildMyAnimeListBackupXml
import com.tankobun.app.backup.createDocumentInTree
import com.tankobun.app.state.LibraryItem
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
            val content = settings.backupContent()
            val folderUri = settings.backupFolderUri()?.let(Uri::parse)
            if (schedule == BackupSchedule.OFF || folderUri == null) {
                Result.success()
            } else {
                val libraryWritten = if (content.includesLibrary()) {
                    val items = backupLibraryItems(container)
                    if (items.isEmpty()) {
                        false
                    } else {
                        writeScheduledLibraryBackup(container, folderUri, items)
                        true
                    }
                } else {
                    false
                }
                val settingsWritten = if (content.includesSettings()) {
                    AppSettingsBackupDataSource(container).writeScheduledBackupFromCurrentSettings(folderUri)
                    true
                } else {
                    false
                }
                if (!libraryWritten && !settingsWritten) {
                    Result.success()
                } else {
                    settings.saveLastScheduledBackupAtEpochMillis(System.currentTimeMillis())
                    Result.success()
                }
            }
        }.getOrElse { error ->
            android.util.Log.w("ScheduledBackupWorker", "Scheduled backup failed", error)
            Result.retry()
        }
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
    val titleLanguage = container.settingsStore.anilistTitleLanguage()
    val media = container.database.mediaDao().cachedMedia().associateBy { it.id }
    return container.database.listEntryDao().cachedEntries()
        .mapNotNull { entry ->
            media[entry.mediaId]?.toModel(titleLanguage)?.let { cachedMedia ->
                LibraryItem(cachedMedia, entry.toModel())
            }
        }
        .distinctBy { it.media.id }
        .sortedBy { it.media.title.userPreferred.lowercase(Locale.ROOT) }
}

private fun writeScheduledLibraryBackup(
    container: AppContainer,
    folderUri: Uri,
    items: List<LibraryItem>,
) {
    val fileUri = createDocumentInTree(
        contentResolver = container.application.contentResolver,
        treeUri = folderUri,
        mimeType = "text/xml",
        displayName = scheduledBackupFileName(container.settingsStore.viewerName()),
    )
    val xml = buildMyAnimeListBackupXml(
        items = items,
        viewerName = container.settingsStore.viewerName(),
        scoreFormat = container.settingsStore.anilistScoreFormat(),
    )
    val output = container.application.contentResolver.openOutputStream(fileUri)
        ?: error("Could not open backup file")
    OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
        writer.write(xml)
    }
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
