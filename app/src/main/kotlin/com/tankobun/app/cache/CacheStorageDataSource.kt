package com.tankobun.app.cache

import coil3.SingletonImageLoader
import eu.kanade.tachiyomi.network.NetworkHelper
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
            anilistAndImageBytes = (container.okHttpClient.cache?.size() ?: 0L) + (SingletonImageLoader.get(container.application).diskCache?.size ?: 0L),
            sourceNetworkBytes = NetworkHelper.cacheSizeBytes(),
            readerPageBytes = ReaderPageCache.sizeBytes(container.application),
            temporaryBytes = temporaryCacheDir(appCache).safeSizeBytes(),
        )
    }

    suspend fun clear(target: CacheClearTarget) = withContext(Dispatchers.IO) {
        when (target) {
            CacheClearTarget.ANILIST_IMAGES -> {
                clearAnilistAndImages()
            }

            CacheClearTarget.SOURCE_NETWORK -> NetworkHelper.clearCache()
            CacheClearTarget.READER_PAGES -> ReaderPageCache.clear(container.application)
            CacheClearTarget.TEMPORARY_FILES -> temporaryCacheDir(container.application.cacheDir).recreate()
            CacheClearTarget.ALL -> {
                clearAnilistAndImages()
                NetworkHelper.clearCache()
                temporaryCacheDir(container.application.cacheDir).recreate()
                ReaderPageCache.clear(container.application)
            }
        }
    }

    private fun clearAnilistAndImages() {
        container.okHttpClient.cache?.evictAll()
        val images = SingletonImageLoader.get(container.application)
        images.diskCache?.clear()
        images.memoryCache?.clear()
    }

    private fun temporaryCacheDir(appCache: File): File = File(appCache, "extension_apks")

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
