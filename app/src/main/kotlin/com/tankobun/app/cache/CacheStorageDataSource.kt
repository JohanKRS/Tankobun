package com.tankobun.app.cache

import com.tankobun.app.AppContainer
import com.tankobun.app.ReaderPageCache
import com.tankobun.app.state.CacheStorageSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal enum class CacheClearTarget {
    ANILIST_IMAGES,
    SOURCE_NETWORK,
    READER_PAGES,
    TEMPORARY_FILES,
    ALL,
}

internal class CacheStorageDataSource(
    private val container: AppContainer,
) {
    suspend fun summary(): CacheStorageSummary = withContext(Dispatchers.IO) {
        val appCache = container.application.cacheDir
        CacheStorageSummary(
            anilistAndImageBytes = anilistAndImageCacheDirs(appCache).sumOf { it.safeSizeBytes() },
            sourceNetworkBytes = sourceNetworkCacheDir(appCache).safeSizeBytes(),
            readerPageBytes = ReaderPageCache.sizeBytes(container.application),
            temporaryBytes = temporaryCacheDir(appCache).safeSizeBytes(),
        )
    }

    suspend fun clear(target: CacheClearTarget) = withContext(Dispatchers.IO) {
        when (target) {
            CacheClearTarget.ANILIST_IMAGES -> {
                container.okHttpClient.cache?.evictAll()
                anilistAndImageCacheDirs(container.application.cacheDir).forEach { it.recreate() }
            }

            CacheClearTarget.SOURCE_NETWORK -> sourceNetworkCacheDir(container.application.cacheDir).recreate()
            CacheClearTarget.READER_PAGES -> ReaderPageCache.clear(container.application)
            CacheClearTarget.TEMPORARY_FILES -> temporaryCacheDir(container.application.cacheDir).recreate()
            CacheClearTarget.ALL -> {
                container.okHttpClient.cache?.evictAll()
                anilistAndImageCacheDirs(container.application.cacheDir).forEach { it.recreate() }
                sourceNetworkCacheDir(container.application.cacheDir).recreate()
                temporaryCacheDir(container.application.cacheDir).recreate()
                ReaderPageCache.clear(container.application)
            }
        }
    }

    private fun anilistAndImageCacheDirs(appCache: File): List<File> =
        appCache.listFiles()
            .orEmpty()
            .filter { file -> file.name !in RESERVED_CACHE_DIRS }
            .plus(File(appCache, "http"))
            .distinctBy { it.absolutePath }

    private fun sourceNetworkCacheDir(appCache: File): File =
        File(appCache, "source-http")

    private fun temporaryCacheDir(appCache: File): File =
        File(appCache, "extension_apks")

    private companion object {
        val RESERVED_CACHE_DIRS = setOf("source-http", "extension_apks")
    }
}

private fun File.recreate() {
    deleteRecursively()
    mkdirs()
}

private fun File.safeSizeBytes(): Long {
    if (!exists()) return 0L
    if (isFile) return length().coerceAtLeast(0L)
    return walkTopDown()
        .filter { it.isFile }
        .sumOf { file -> runCatching { file.length().coerceAtLeast(0L) }.getOrDefault(0L) }
}
