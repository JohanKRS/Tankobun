package com.tankobun.app.logic

import com.tankobun.app.state.DownloadStorageItem
import com.tankobun.app.state.DownloadStorageSummary
import com.tankobun.core.database.DownloadPageEntity
import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.DownloadState
import java.io.File

internal data class BulkDownloadResult(
    val queued: Int = 0,
    val resumed: Int = 0,
    val retried: Int = 0,
    val skipped: Int = 0,
) {
    val changed: Int
        get() = queued + resumed + retried
}

internal fun bulkDownloadMessage(label: String, result: BulkDownloadResult): String {
    if (result.changed == 0) return "No new $label to download"
    val parts = buildList {
        if (result.queued > 0) add("queued ${result.queued}")
        if (result.resumed > 0) add("resumed ${result.resumed}")
        if (result.retried > 0) add("retrying ${result.retried}")
    }
    return "${parts.joinToString(" / ")} $label"
}

internal fun buildDownloadStorageSummary(
    downloads: List<DownloadJob>,
    pages: List<DownloadPageEntity>,
    fileSize: (String) -> Long = ::downloadedFileSize,
): DownloadStorageSummary {
    val bytesByGroup = mutableMapOf<DownloadStorageGroupKey, Long>()
    val pageCountByGroup = mutableMapOf<DownloadStorageGroupKey, Int>()
    val pageChapterUrlsByGroup = mutableMapOf<DownloadStorageGroupKey, MutableSet<String>>()
    pages.forEach { page ->
        val key = DownloadStorageGroupKey(page.mediaId, page.sourceId)
        val bytes = fileSize(page.filePath).coerceAtLeast(0L)
        bytesByGroup[key] = bytesByGroup.getOrDefault(key, 0L) + bytes
        pageCountByGroup[key] = pageCountByGroup.getOrDefault(key, 0) + 1
        pageChapterUrlsByGroup.getOrPut(key) { mutableSetOf() }.add(page.chapterUrl)
    }

    val jobsByGroup = downloads.groupBy { DownloadStorageGroupKey(it.mediaId, it.sourceId) }
    val groupKeys = (bytesByGroup.keys + jobsByGroup.keys + pageChapterUrlsByGroup.keys).toSet()
    val items = groupKeys
        .map { key ->
            val jobs = jobsByGroup[key].orEmpty()
            val chapterUrls = (jobs.map { it.chapterUrl } + pageChapterUrlsByGroup[key].orEmpty()).distinct()
            DownloadStorageItem(
                mediaId = key.mediaId,
                sourceId = key.sourceId,
                bytes = bytesByGroup.getOrDefault(key, 0L),
                chapterCount = chapterUrls.size,
                completedChapterCount = jobs
                    .filter { it.state == DownloadState.COMPLETE }
                    .map { it.chapterUrl }
                    .distinct()
                    .size,
                activeChapterCount = jobs
                    .filter { it.state == DownloadState.QUEUED || it.state == DownloadState.RUNNING || it.state == DownloadState.PAUSED }
                    .map { it.chapterUrl }
                    .distinct()
                    .size,
                pageCount = pageCountByGroup.getOrDefault(key, 0),
            )
        }
        .filter { it.bytes > 0L || it.chapterCount > 0 }
        .sortedWith(
            compareByDescending<DownloadStorageItem> { it.bytes }
                .thenBy { it.mediaId }
                .thenBy { it.sourceId },
        )
    return DownloadStorageSummary(
        totalBytes = items.sumOf { it.bytes },
        items = items,
    )
}

private fun downloadedFileSize(path: String): Long {
    val file = File(path)
    return if (file.exists()) file.length() else 0L
}

private data class DownloadStorageGroupKey(
    val mediaId: Int,
    val sourceId: Long,
)
