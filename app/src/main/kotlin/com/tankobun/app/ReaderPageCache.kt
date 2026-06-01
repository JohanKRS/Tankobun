package com.tankobun.app

import android.content.Context
import android.net.Uri
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Locale

object ReaderPageCache {
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

    fun withCachedPaths(
        context: Context,
        mediaId: Int,
        chapter: SourceChapter,
        pages: List<ReaderPage>,
    ): List<ReaderPage> =
        pages.map { page ->
            page.cachedFilePath?.let { return@map page }
            cachedFile(context, mediaId, chapter, page)
                ?.absolutePath
                ?.let { page.copy(cachedFilePath = it) }
                ?: page
        }

    suspend fun prune(context: Context, maxChapterDirs: Int = 16) = withContext(Dispatchers.IO) {
        val root = rootDir(context)
        val chapterDirs = root
            .walkTopDown()
            .maxDepth(4)
            .filter { it.isDirectory && it.parentFile?.parentFile?.parentFile == root }
            .sortedByDescending { it.lastModified() }
            .toList()
        chapterDirs.drop(maxChapterDirs).forEach { it.deleteRecursively() }
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
