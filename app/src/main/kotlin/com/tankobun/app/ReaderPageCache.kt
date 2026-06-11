package com.tankobun.app

import android.content.Context
import android.net.Uri
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object ReaderPageCache {
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
                val bytes = fetchBytes()
                runCatching {
                    writePage(
                        context = context,
                        mediaId = mediaId,
                        chapter = chapter,
                        page = page,
                        bytes = bytes,
                    )
                }.onFailure { error ->
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
        val file = cachedFile(context, mediaId, chapter, page) ?: return@withContext null
        file.readBytes()
    }

    suspend fun writePage(
        context: Context,
        mediaId: Int,
        chapter: SourceChapter,
        page: ReaderPage,
        bytes: ByteArray,
    ): String = withContext(Dispatchers.IO) {
        val file = pageFile(context, mediaId, chapter, page)
        val parent = requireNotNull(file.parentFile)
        parent.mkdirs()
        val partial = File(parent, "${file.name}.part")
        partial.writeBytes(bytes)
        if (file.exists()) file.delete()
        check(partial.renameTo(file)) { "Could not cache page ${page.index + 1}" }
        parent.setLastModified(System.currentTimeMillis())
        file.absolutePath
    }

    suspend fun withCachedPaths(
        context: Context,
        mediaId: Int,
        chapter: SourceChapter,
        pages: List<ReaderPage>,
    ): List<ReaderPage> = withContext(Dispatchers.IO) {
        pages.map { page ->
            val cachedFile = page.cachedFilePath
                ?.let(::File)
                ?.takeIf { it.isFile && it.length() > 0L }
                ?: cachedFile(context, mediaId, chapter, page)
            cachedFile?.let { file ->
                page.copy(cachedFilePath = file.absolutePath)
            } ?: page
        }
    }

    suspend fun prune(
        context: Context,
        maxChapterDirs: Int = 32,
        minRecentChapterDirsPerMedia: Int = 4,
    ) = withContext(Dispatchers.IO) {
        val root = rootDir(context)
        val chapterDirs = root
            .walkTopDown()
            .maxDepth(4)
            .filter { it.isDirectory && it.parentFile?.parentFile?.parentFile == root }
            .sortedByDescending { it.lastModified() }
            .toList()
        val protectedChapterDirs = chapterDirs
            .groupBy { it.mediaCacheDir()?.absolutePath.orEmpty() }
            .values
            .flatMap { dirs -> dirs.take(minRecentChapterDirsPerMedia.coerceAtLeast(0)) }
            .toSet()
        val unprotectedToKeep = (maxChapterDirs - protectedChapterDirs.size).coerceAtLeast(0)
        chapterDirs
            .filterNot { it in protectedChapterDirs }
            .drop(unprotectedToKeep)
            .forEach { it.deleteRecursively() }
    }

    suspend fun sizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        rootDir(context).safeSizeBytes()
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        rootDir(context).deleteRecursively()
        rootDir(context).mkdirs()
    }

    private fun cachedFile(
        context: Context,
        mediaId: Int,
        chapter: SourceChapter,
        page: ReaderPage,
    ): File? =
        pageFile(context, mediaId, chapter, page)
            .takeIf { it.isFile && it.length() > 0L }
            ?.also { it.parentFile?.setLastModified(System.currentTimeMillis()) }

    private fun pageFile(
        context: Context,
        mediaId: Int,
        chapter: SourceChapter,
        page: ReaderPage,
    ): File =
        File(chapterDir(context, mediaId, chapter), pageFileName(page))

    private fun chapterDir(context: Context, mediaId: Int, chapter: SourceChapter): File =
        File(
            rootDir(context),
            "$mediaId/${chapter.sourceId}/${stableFileKey(chapter.url)}",
        )

    private fun rootDir(context: Context): File =
        File(context.filesDir, "reader_page_cache_v2").also { it.mkdirs() }

    private fun File.mediaCacheDir(): File? = parentFile?.parentFile

    private fun pageCacheKey(mediaId: Int, chapter: SourceChapter, page: ReaderPage): String =
        "$mediaId:${chapter.sourceId}:${stableFileKey(chapter.url)}:${pageFileName(page)}"

    private fun pageFileName(page: ReaderPage): String =
        "${page.index.toString().padStart(4, '0')}.${pageFileExtension(page.imageUrl)}"

    private fun stableFileKey(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }.take(24)
    }

    private fun pageFileExtension(url: String): String {
        val extension = Uri.parse(url).lastPathSegment
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.ROOT)
        return extension?.takeIf { it in IMAGE_EXTENSIONS } ?: "jpg"
    }

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")
}

private fun File.safeSizeBytes(): Long {
    if (!exists()) return 0L
    if (isFile) return length().coerceAtLeast(0L)
    return walkTopDown()
        .filter { it.isFile }
        .sumOf { file -> runCatching { file.length().coerceAtLeast(0L) }.getOrDefault(0L) }
}
