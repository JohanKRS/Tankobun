package com.tankobun.app

import android.content.Context
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceChapter
import com.tankobun.app.cache.PageDiskStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object ReaderPageCache {
    private val stores = ConcurrentHashMap<String, PageDiskStore>()
    private fun store(context: Context): PageDiskStore = stores.getOrPut(rootDir(context).absolutePath) {
        val settings = SettingsStore(context.applicationContext)
        PageDiskStore(rootDir(context), { settings.cachePreferences().readerLimitBytes })
    }

    private val pageLocks = ConcurrentHashMap<String, PageLock>()

    private data class PageLock(
        val mutex: Mutex = Mutex(),
        val references: AtomicInteger = AtomicInteger(0),
    )

    data class PageBytes(
        val bytes: ByteArray,
        val fromCache: Boolean,
    )

    suspend fun cachedOrFetch(
        context: Context,
        mediaId: Int,
        chapter: SourceChapter,
        page: ReaderPage,
        allowCachedRead: Boolean = true,
        failOnCacheWrite: Boolean = false,
        onCacheWriteFailure: (Throwable) -> Unit = {},
        fetchBytes: suspend () -> ByteArray,
    ): PageBytes {
        val key = pageCacheKey(mediaId, chapter, page)
        val pageLock = pageLocks.compute(key) { _, existing ->
            (existing ?: PageLock()).also { it.references.incrementAndGet() }
        } ?: error("Could not create reader page lock")
        return try {
            pageLock.mutex.withLock {
                if (allowCachedRead) {
                    cachedBytes(context, mediaId, chapter, page)?.let { bytes ->
                        return@withLock PageBytes(bytes = bytes, fromCache = true)
                    }
                }
                val diskStore = store(context)
                val generation = diskStore.generation()
                val bytes = try {
                    fetchBytes()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    if (allowCachedRead) {
                        withContext(Dispatchers.IO) { diskStore.read(key, allowStale = true) }?.let {
                            return@withLock PageBytes(it, fromCache = true)
                        }
                    }
                    throw error
                }
                runCatching {
                    withContext(Dispatchers.IO) { diskStore.write(key, bytes, generation) }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    onCacheWriteFailure(error)
                    if (failOnCacheWrite) {
                        throw error
                    }
                }
                PageBytes(bytes = bytes, fromCache = false)
            }
        } finally {
            pageLocks.computeIfPresent(key) { _, existing ->
                if (existing !== pageLock) {
                    existing
                } else if (existing.references.decrementAndGet() == 0) {
                    null
                } else {
                    existing
                }
            }
        }
    }

    suspend fun cachedBytes(
        context: Context,
        mediaId: Int,
        chapter: SourceChapter,
        page: ReaderPage,
    ): ByteArray? = withContext(Dispatchers.IO) {
        store(context).read(pageCacheKey(mediaId, chapter, page))
    }

    // Downloads have their own paths. Automatic cache files always go through the
    // fetcher so an eviction between composing and loading is a recoverable miss.
    suspend fun withCachedPaths(
        context: Context,
        mediaId: Int,
        chapter: SourceChapter,
        pages: List<ReaderPage>,
    ): List<ReaderPage> = pages.map { page ->
        if (page.cachedFilePath?.let { isManagedPath(context, it) } == true) page.copy(cachedFilePath = null) else page
    }

    fun isManagedPath(context: Context, path: String): Boolean =
        File(path).absolutePath.startsWith(rootDir(context).absolutePath + File.separator)

    suspend fun prune(context: Context) = withContext(Dispatchers.IO) { store(context).trim() }
    suspend fun sizeBytes(context: Context): Long = withContext(Dispatchers.IO) { store(context).sizeBytes() }
    suspend fun clear(context: Context) = withContext(Dispatchers.IO) { store(context).clear() }

    private fun rootDir(context: Context): File = File(context.filesDir, "reader_page_cache_v2")

    private fun pageCacheKey(mediaId: Int, chapter: SourceChapter, page: ReaderPage): String =
        "$mediaId/${chapter.sourceId}/${stableFileKey(chapter.url)}/${page.index.toString().padStart(4, '0')}-${stableFileKey(page.sourcePageUrl.ifBlank { page.imageUrl })}"

    private fun stableFileKey(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }.take(24)
    }

}
