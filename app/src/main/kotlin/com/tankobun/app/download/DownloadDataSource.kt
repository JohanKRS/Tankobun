package com.tankobun.app.download

import com.tankobun.app.AppContainer
import com.tankobun.app.logic.BulkDownloadResult
import com.tankobun.app.logic.buildDownloadStorageSummary
import com.tankobun.app.state.DownloadStorageSummary
import com.tankobun.core.database.toModel
import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.SourceChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

internal class DownloadDataSource(
    private val container: AppContainer,
) {
    fun observeDownloads(): Flow<List<DownloadJob>> =
        container.database.downloadDao().observeDownloads()
            .map { rows -> rows.map { row -> row.toModel() } }

    suspend fun allDownloads(): List<DownloadJob> =
        container.database.downloadDao().allDownloads().map { it.toModel() }

    suspend fun latestForChapter(mediaId: Int, chapterUrl: String): DownloadJob? =
        container.database.downloadDao().latestForChapter(mediaId, chapterUrl)?.toModel()

    suspend fun enqueueChapters(
        mediaId: Int,
        chapters: List<SourceChapter>,
        retryFailed: Boolean = true,
    ): BulkDownloadResult {
        var queued = 0
        var resumed = 0
        var retried = 0
        var skipped = 0
        chapters.distinctBy { "${it.sourceId}:${it.url}" }.forEach { chapter ->
            val existing = latestForChapter(mediaId, chapter.url)
            when (existing?.state) {
                DownloadState.QUEUED,
                DownloadState.RUNNING,
                DownloadState.COMPLETE -> skipped += 1

                DownloadState.PAUSED -> {
                    container.downloadCoordinator.resume(existing.id)
                    resumed += 1
                }

                DownloadState.FAILED -> {
                    if (retryFailed) {
                        container.downloadCoordinator.retry(existing.id)
                        retried += 1
                    } else {
                        skipped += 1
                    }
                }

                null -> {
                    val now = System.currentTimeMillis()
                    container.downloadCoordinator.enqueue(
                        DownloadJob(
                            id = UUID.randomUUID().toString(),
                            mediaId = mediaId,
                            sourceId = chapter.sourceId,
                            mangaUrl = chapter.mangaUrl,
                            chapterUrl = chapter.url,
                            chapterName = chapter.name,
                            state = DownloadState.QUEUED,
                            pageCount = 0,
                            completedPages = 0,
                            retryCount = 0,
                            createdAtEpochMillis = now,
                            updatedAtEpochMillis = now,
                        ),
                    )
                    queued += 1
                }
            }
        }
        return BulkDownloadResult(
            queued = queued,
            resumed = resumed,
            retried = retried,
            skipped = skipped,
        )
    }

    suspend fun pause(jobId: String) {
        container.downloadCoordinator.pause(jobId)
    }

    suspend fun resume(jobId: String) {
        container.downloadCoordinator.resume(jobId)
    }

    suspend fun retry(jobId: String) {
        container.downloadCoordinator.retry(jobId)
    }

    suspend fun remove(jobId: String) {
        container.downloadCoordinator.remove(jobId)
    }

    suspend fun removeMedia(mediaId: Int) {
        container.downloadCoordinator.removeMedia(mediaId)
    }

    suspend fun removeMediaSource(mediaId: Int, sourceId: Long) {
        container.downloadCoordinator.removeMediaSource(mediaId, sourceId)
    }

    suspend fun removeAll() {
        container.downloadCoordinator.removeAll()
    }

    suspend fun storageSummary(downloads: List<DownloadJob>): DownloadStorageSummary =
        withContext(Dispatchers.IO) {
            buildDownloadStorageSummary(
                downloads = downloads,
                pages = container.database.downloadPageDao().allPages(),
            )
        }
}
