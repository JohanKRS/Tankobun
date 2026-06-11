package com.tankobun.app.state

import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.MediaStatus

data class ExtensionInstallRequest(
    val packageName: String,
    val name: String,
    val apkUri: String,
    val expectedVersionCode: Int,
    val expectedVersionName: String,
)

data class LibraryItem(
    val media: AnilistMedia,
    val entry: AnilistListEntry,
)

data class LibrarySection(
    val key: String,
    val title: String,
    val items: List<LibraryItem>,
    val status: MediaStatus? = null,
)

data class RecentReadingProgress(
    val media: AnilistMedia,
    val progress: ReadingProgress,
    val chapter: SourceChapter?,
)

data class ReaderChapterSegment(
    val chapter: SourceChapter,
    val pages: List<ReaderPage>,
)

data class ReaderLoadError(
    val title: String,
    val message: String,
)

data class DownloadStorageSummary(
    val totalBytes: Long = 0L,
    val items: List<DownloadStorageItem> = emptyList(),
)

data class DownloadStorageItem(
    val mediaId: Int,
    val sourceId: Long,
    val bytes: Long,
    val chapterCount: Int,
    val completedChapterCount: Int,
    val activeChapterCount: Int,
    val pageCount: Int,
)

data class CacheStorageSummary(
    val anilistAndImageBytes: Long = 0L,
    val sourceNetworkBytes: Long = 0L,
    val readerPageBytes: Long = 0L,
    val temporaryBytes: Long = 0L,
) {
    val totalBytes: Long
        get() = anilistAndImageBytes + sourceNetworkBytes + readerPageBytes + temporaryBytes
}

data class BackupMissingSource(
    val packageName: String,
    val name: String,
    val lang: String,
    val versionName: String?,
    val sourceNames: List<String> = emptyList(),
)
