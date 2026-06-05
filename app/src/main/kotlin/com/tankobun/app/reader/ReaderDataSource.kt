package com.tankobun.app.reader

import android.util.Log
import com.tankobun.app.AppContainer
import com.tankobun.app.ReaderPageCache
import com.tankobun.core.database.toEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.database.toReaderPage
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.reader.ReaderProgressCalculator
import com.tankobun.core.reader.ReaderSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.File

internal data class ChapterReadUpdate(
    val latestProgress: ReadingProgress?,
    val chapterProgress: Map<String, ReadingProgress>,
    val syncProgress: Int?,
)

internal class ReaderDataSource(
    private val container: AppContainer,
) {
    private val progressCalculator = ReaderProgressCalculator()

    suspend fun loadPagesForChapter(
        mediaId: Int,
        chapter: SourceChapter,
        source: SourceDescriptor?,
    ): List<ReaderPage> {
        val cachedPages = cachedDownloadedPages(mediaId, chapter)
        if (cachedPages.isNotEmpty()) return cachedPages
        if (source == null) return emptyList()
        return ReaderPageCache.withCachedPaths(
            context = container.application,
            mediaId = mediaId,
            chapter = chapter,
            pages = container.sourceHost.pages(source, chapter),
        )
    }

    fun cacheWindowPages(
        pages: List<ReaderPage>,
        pageIndex: Int,
        preferredDirection: Int,
    ): List<ReaderPage> {
        if (pages.isEmpty()) return emptyList()
        val normalizedPageIndex = pageIndex.coerceIn(0, pages.lastIndex)
        val start = (normalizedPageIndex - READER_CACHE_BACK_PAGES).coerceAtLeast(0)
        val end = (normalizedPageIndex + READER_CACHE_FORWARD_PAGES).coerceAtMost(pages.lastIndex)
        return orderedCacheWindow(
            pages = pages,
            pageIndex = normalizedPageIndex,
            start = start,
            end = end,
            preferredDirection = preferredDirection,
        )
    }

    fun adjacentTailPages(pages: List<ReaderPage>): List<ReaderPage> =
        pages.takeLast(READER_ADJACENT_CACHE_PAGE_COUNT)

    fun adjacentHeadPages(pages: List<ReaderPage>): List<ReaderPage> =
        pages.take(READER_ADJACENT_CACHE_PAGE_COUNT)

    suspend fun cachePages(
        source: SourceDescriptor,
        mediaId: Int,
        chapter: SourceChapter,
        pages: List<ReaderPage>,
        initialDelayMillis: Long,
    ) {
        val pagesToCache = pages.filter { it.cachedFilePath == null }
        if (pagesToCache.isEmpty()) return
        if (initialDelayMillis > 0L) {
            delay(initialDelayMillis)
        }
        pagesToCache.forEachIndexed { index, page ->
            if (index > 0) delay(READER_CACHE_REQUEST_SPACING_MILLIS)
            runCatching {
                ReaderPageCache.cachedOrFetch(
                    context = container.application,
                    mediaId = mediaId,
                    chapter = chapter,
                    page = page,
                ) {
                    container.sourceHost.imageBytes(source, page)
                }
            }.onFailure { error ->
                if (error !is CancellationException) {
                    Log.w(TAG, "Reader cache failed for ${chapter.name} page ${page.index + 1}", error)
                }
            }
        }
        ReaderPageCache.prune(container.application)
    }

    suspend fun cachedProgressForChapter(mediaId: Int, chapterUrl: String): ReadingProgress? =
        container.database.progressDao().progressForChapter(mediaId, chapterUrl)?.toModel()

    suspend fun markChapterRead(
        mediaId: Int,
        chapter: SourceChapter,
        read: Boolean,
        readerMode: ReaderMode,
        nowMillis: Long,
    ): ChapterReadUpdate {
        val progressDao = container.database.progressDao()
        val syncProgress = if (read) {
            val existing = progressDao.progressForChapter(mediaId, chapter.url)?.toModel()
            val totalPages = existing?.totalPages?.takeIf { it > 0 } ?: 1
            val progress = ReadingProgress(
                mediaId = mediaId,
                chapterUrl = chapter.url,
                chapterNumber = chapter.chapterNumber,
                pageIndex = totalPages - 1,
                pageScrollOffset = 0,
                totalPages = totalPages,
                readerMode = existing?.readerMode ?: readerMode,
                completed = true,
                updatedAtEpochMillis = nowMillis,
            )
            progressDao.upsertProgress(progress.toEntity())
            chapter.chapterNumber.toInt().takeIf { it > 0 }
        } else {
            progressDao.deleteProgressForChapter(mediaId, chapter.url)
            null
        }

        return ChapterReadUpdate(
            latestProgress = progressDao.latestProgress(mediaId)?.toModel(),
            chapterProgress = progressByChapter(mediaId),
            syncProgress = syncProgress,
        )
    }

    suspend fun saveProgress(
        mediaId: Int,
        chapter: SourceChapter,
        pages: List<ReaderPage>,
        readerMode: ReaderMode,
        pageIndex: Int,
        pageScrollOffset: Int,
        updatedAtMillis: Long,
    ): ReadingProgress {
        val normalizedPageIndex = pageIndex.coerceIn(0, pages.lastIndex)
        val session = ReaderSession(
            mediaId = mediaId,
            chapter = chapter,
            pages = pages,
            mode = readerMode,
            currentPageIndex = normalizedPageIndex,
            currentPageScrollOffset = pageScrollOffset.coerceAtLeast(0),
        )
        val progress = progressCalculator.progressFor(session, updatedAtMillis)
        container.database.progressDao().upsertProgress(progress.toEntity())
        return progress
    }

    private suspend fun progressByChapter(mediaId: Int): Map<String, ReadingProgress> =
        container.database.progressDao()
            .progressForMedia(mediaId)
            .map { it.toModel() }
            .associateBy { it.chapterUrl }

    private suspend fun cachedDownloadedPages(mediaId: Int, chapter: SourceChapter): List<ReaderPage> {
        container.database.downloadDao().completedForChapter(mediaId, chapter.url) ?: return emptyList()
        return container.database.downloadPageDao()
            .pagesForChapter(mediaId, chapter.url)
            .filter { File(it.filePath).isFile }
            .map { it.toReaderPage() }
    }

    private fun orderedCacheWindow(
        pages: List<ReaderPage>,
        pageIndex: Int,
        start: Int,
        end: Int,
        preferredDirection: Int,
    ): List<ReaderPage> {
        val forwardFirst = preferredDirection >= 0
        val orderedIndexes = buildList {
            add(pageIndex)
            val radius = maxOf(pageIndex - start, end - pageIndex)
            for (offset in 1..radius) {
                val forward = pageIndex + offset
                val backward = pageIndex - offset
                if (forwardFirst) {
                    if (forward <= end) add(forward)
                    if (backward >= start) add(backward)
                } else {
                    if (backward >= start) add(backward)
                    if (forward <= end) add(forward)
                }
            }
        }
        return orderedIndexes.distinct().map { pages[it] }
    }

    companion object {
        const val CACHE_INITIAL_DELAY_MILLIS = 500L
        const val ADJACENT_CACHE_INITIAL_DELAY_MILLIS = 12_000L

        private const val TAG = "TankobunReaderData"
        private const val READER_CACHE_BACK_PAGES = 5
        private const val READER_CACHE_FORWARD_PAGES = 10
        private const val READER_ADJACENT_CACHE_PAGE_COUNT = 2
        private const val READER_CACHE_REQUEST_SPACING_MILLIS = 250L
    }
}
