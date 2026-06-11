package com.tankobun.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tankobun.app.source.SourceDataSource
import com.tankobun.core.database.SourceBindingEntity
import com.tankobun.core.database.SourceChapterEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.CachePolicy
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.SourceBinding
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceManga
import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit

class NewChapterCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val app = applicationContext as? TankobunApplication ?: error("Tankobun application not available")
            val container = app.container
            if (!container.settingsStore.newChapterChecksEnabled()) {
                Result.success()
            } else {
                val result = NewChapterChecker(container).check()
                container.settingsStore.saveLastNewChapterCheckAtEpochMillis(result.checkedAtEpochMillis)
                if (result.updates.isNotEmpty()) {
                    NewChapterNotifier.show(applicationContext, container, result.updates)
                }
                Result.success()
            }
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "New chapter check failed", error)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "TankobunChapterCheck"
    }
}

object NewChapterCheckWork {
    private const val UNIQUE_NAME = "tankobun-new-chapter-check"
    private const val RUN_NOW_UNIQUE_NAME = "tankobun-new-chapter-check-now"
    private const val INITIAL_DELAY_HOURS = 1L
    private const val INTERVAL_MILLIS = 24L * 60L * 60L * 1_000L

    fun sync(context: Context, enabled: Boolean) {
        if (!enabled) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
            return
        }
        enqueue(context, ExistingPeriodicWorkPolicy.KEEP)
    }

    fun update(context: Context, enabled: Boolean) {
        if (!enabled) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
            return
        }
        enqueue(context, ExistingPeriodicWorkPolicy.UPDATE)
    }

    fun runOnce(context: Context) {
        val request = OneTimeWorkRequestBuilder<NewChapterCheckWorker>()
            .setConstraints(workConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(RUN_NOW_UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private fun enqueue(context: Context, policy: ExistingPeriodicWorkPolicy) {
        val request = PeriodicWorkRequestBuilder<NewChapterCheckWorker>(INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
            .setInitialDelay(INITIAL_DELAY_HOURS, TimeUnit.HOURS)
            .setConstraints(workConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_NAME, policy, request)
    }

    private fun workConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
}

internal data class NewChapterCheckResult(
    val checkedAtEpochMillis: Long,
    val checkedManga: Int,
    val skippedManga: Int,
    val updates: List<NewChapterUpdate>,
)

internal data class NewChapterUpdate(
    val mediaTitle: String,
    val sourceName: String,
    val chapters: List<SourceChapter>,
)

internal class NewChapterChecker(
    private val container: AppContainer,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val sourceDataSource = SourceDataSource(container, CachePolicy())
    private val sourceLimiters = mutableMapOf<Long, RespectfulRateLimiter>()

    suspend fun check(): NewChapterCheckResult {
        val checkedAt = now()
        val candidates = readingCandidates()
        var checkedManga = 0
        var skippedManga = 0
        val updates = mutableListOf<NewChapterUpdate>()
        val sourceCache = mutableMapOf<String, SourceDescriptor?>()

        for (candidate in candidates) {
            val bindingRow = container.database.sourceBindingDao().bindingForMedia(candidate.media.id)
            val binding = bindingRow?.toModel()
            if (binding == null) {
                skippedManga += 1
                continue
            }

            val source = sourceCache.getOrPut(binding.sourceCacheKey()) {
                resolveInstalledSource(bindingRow)
            }
            if (source == null) {
                skippedManga += 1
                continue
            }

            val cachedRows = container.database.chapterDao().cachedChapters(source.id, binding.mangaUrl)
            val manga = binding.toSourceManga(source)
            val fetched = runCatching {
                sourceLimiters.getOrPut(source.id) {
                    RespectfulRateLimiter(minSpacingMillis = SOURCE_REQUEST_SPACING_MILLIS)
                }.run {
                    sourceDataSource.fetchAndCacheChapters(source, manga, checkedAt)
                }
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Could not check ${candidate.media.title.userPreferred} on ${source.name}", error)
                skippedManga += 1
                null
            } ?: continue

            checkedManga += 1
            if (cachedRows.isEmpty() || fetched.isEmpty()) continue

            val newChapters = fetched.newSince(cachedRows)
            if (newChapters.isNotEmpty()) {
                updates += NewChapterUpdate(
                    mediaTitle = candidate.media.title.userPreferred,
                    sourceName = source.name,
                    chapters = newChapters,
                )
            }
        }

        return NewChapterCheckResult(
            checkedAtEpochMillis = checkedAt,
            checkedManga = checkedManga,
            skippedManga = skippedManga,
            updates = updates,
        )
    }

    private suspend fun readingCandidates(): List<ReadingCandidate> {
        val titleLanguage = container.settingsStore.anilistTitleLanguage()
        val mediaById = container.database.mediaDao().cachedMedia().associateBy { it.id }
        return container.database.listEntryDao().cachedEntries()
            .mapNotNull { entry ->
                if (entry.status != MediaStatus.CURRENT) return@mapNotNull null
                val media = mediaById[entry.mediaId]?.toModel(titleLanguage) ?: return@mapNotNull null
                if (media.isFinishedPublishing()) return@mapNotNull null
                ReadingCandidate(media)
            }
            .distinctBy { it.media.id }
            .sortedBy { it.media.title.userPreferred.lowercase(Locale.ROOT) }
    }

    private fun resolveInstalledSource(binding: SourceBindingEntity): SourceDescriptor? {
        val extensions = container.extensionScanner.installedExtensions()
            .sortedBy { descriptor ->
                if (descriptor.packageName == binding.sourcePackageName) 0 else 1
            }
        extensions.forEach { extension ->
            val source = runCatching {
                container.sourceHost.loadSources(extension.packageName)
                    .firstOrNull { it.id == binding.sourceId }
            }.getOrNull() ?: return@forEach
            return extension.copy(
                id = source.id,
                name = source.name,
                lang = source.lang,
            )
        }
        return null
    }

    private fun List<SourceChapter>.newSince(cachedRows: List<SourceChapterEntity>): List<SourceChapter> {
        val cachedUrls = cachedRows.map { it.chapterUrl }.toHashSet()
        return filterNot { it.url in cachedUrls }
            .sortedWith(
                compareByDescending<SourceChapter> { it.chapterNumber }
                    .thenByDescending { it.uploadedAtEpochMillis ?: 0L }
                    .thenBy { it.name.lowercase(Locale.ROOT) },
            )
    }

    private fun AnilistMedia.isFinishedPublishing(): Boolean =
        status.equals("FINISHED", ignoreCase = true)

    private fun SourceBinding.toSourceManga(source: SourceDescriptor): SourceManga =
        SourceManga(
            sourceId = source.id,
            url = mangaUrl,
            title = mangaTitle,
            thumbnailUrl = thumbnailUrl,
            description = null,
            author = null,
            artist = null,
            status = null,
        )

    private fun SourceBinding.sourceCacheKey(): String =
        "$sourcePackageName:$sourceId"

    private data class ReadingCandidate(
        val media: AnilistMedia,
    )

    private companion object {
        private const val TAG = "TankobunChapterCheck"
        private const val SOURCE_REQUEST_SPACING_MILLIS = 2_500L
    }
}

private object NewChapterNotifier {
    private const val CHANNEL_ID = "new_chapters"
    private const val NOTIFICATION_ID = 4007

    fun show(context: Context, container: AppContainer, updates: List<NewChapterUpdate>) {
        if (!context.canPostNotifications()) return
        ensureChannel(context, container)

        val chapterCount = updates.sumOf { it.chapters.size }
        val mangaCount = updates.size
        val newChapterLabel = container.quantityString(R.plurals.new_chapter_count, chapterCount, chapterCount)
        val text = if (mangaCount == 1) {
            container.string(R.string.notification_new_chapters_text_single, newChapterLabel, updates.first().mediaTitle)
        } else {
            val mangaLabel = container.quantityString(R.plurals.manga_count, mangaCount, mangaCount)
            container.string(R.string.notification_new_chapters_text_multi, mangaLabel, newChapterLabel)
        }
        val details = updates.take(NOTIFICATION_DETAIL_MAX_MANGA).joinToString("\n") { update ->
            val chapterNames = update.chapters
                .take(NOTIFICATION_DETAIL_MAX_CHAPTERS_PER_MANGA)
                .joinToString(", ") { it.name }
            "${update.mediaTitle}: $chapterNames"
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tankobun)
            .setContentTitle(container.string(R.string.notification_new_chapters_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(listOf(text, details).filter { it.isNotBlank() }.joinToString("\n\n")))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setNumber(chapterCount)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context, container: AppContainer) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            container.string(R.string.notification_channel_new_chapters_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = container.string(R.string.notification_channel_new_chapters_desc)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun Context.canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private const val NOTIFICATION_DETAIL_MAX_MANGA = 4
    private const val NOTIFICATION_DETAIL_MAX_CHAPTERS_PER_MANGA = 2
}
