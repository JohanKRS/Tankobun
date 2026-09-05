package com.tankobun.core.extensions

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import com.tankobun.core.network.RespectfulRateLimiter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceManga
import com.tankobun.core.model.SourceMangaUpdate
import dalvik.system.PathClassLoader
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.TankobunInjektRegistry
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class TachiyomiSourceHost(
    private val context: Context,
    private val trustStore: ExtensionTrustStore = ExtensionTrustStore(context),
) {
    private val appContext = context.applicationContext
    private val sourceCache = mutableMapOf<String, CachedSources>()
    private val sourceLoadLocks = Array(32) { Any() }
    private val updateGate = SourceUpdateGate()
    private val imageFetchSemaphores = ConcurrentHashMap<String, Semaphore>()
    private val imageFetchRateLimiters = ConcurrentHashMap<String, RespectfulRateLimiter>()

    init {
        TankobunInjektRegistry.applicationOrNull()?.let { registered ->
            eu.kanade.tachiyomi.network.NetworkHelper.configure(registered)
        }
        ensureHttpAgent()
    }

    fun loadSources(packageName: String): List<Source> =
        synchronized(sourceLoadLocks[(packageName.hashCode() and Int.MAX_VALUE) % sourceLoadLocks.size]) {
            loadSourcesLocked(packageName)
        }

    private fun loadSourcesLocked(packageName: String): List<Source> {
        ensureHttpAgent()
        val packageInfo = packageInfo(packageName) ?: return emptyList()
        // Gate every entrypoint, including background work and cached source instances.
        if (!trustStore.isTrusted(packageInfo)) return emptyList()
        val key = packageInfo.cacheKey()
        synchronized(sourceCache) {
            sourceCache[packageName]?.takeIf { it.cacheKey == key }?.let { return it.sources }
        }

        val sources = loadDeclaredSourceClasses(packageInfo).flatMap { instance ->
            runCatching {
                when (instance) {
                    is SourceFactory -> instance.createSources()
                    is Source -> listOf(instance)
                    else -> emptyList()
                }
            }.onFailure { error ->
                logSourceFailure(
                    action = "createSources",
                    packageName = packageName,
                    error = error,
                )
            }.getOrDefault(emptyList())
        }.distinctBy { "${it.id}:${it.name}:${it.lang}" }

        synchronized(sourceCache) {
            sourceCache[packageName] = CachedSources(key, sources)
        }
        return sources
    }

    fun retainInstalledPackages(packageNames: Set<String>) {
        synchronized(sourceCache) {
            sourceCache.keys.retainAll(packageNames)
        }
    }

    fun clearCache(packageName: String? = null) {
        synchronized(sourceCache) {
            if (packageName == null) {
                sourceCache.clear()
            } else {
                sourceCache.remove(packageName)
            }
        }
    }

    suspend fun search(
        source: SourceDescriptor,
        query: String,
        page: Int = 1,
    ): List<SourceManga> = withContext(Dispatchers.IO) {
        val catalogueSource = findSource(source)
            ?: return@withContext emptyList()

        runSourceAction(source, catalogueSource, "search") {
            catalogueSource.getSearchManga(page, query, catalogueSource.getFilterList())
                .mangas.map { it.toSourceManga(source.id) }
        }
    }

    suspend fun mangaDetails(source: SourceDescriptor, manga: SourceManga): SourceManga? =
        mangaUpdate(source, manga, fetchDetails = true, fetchChapters = false)?.manga

    suspend fun chapters(source: SourceDescriptor, manga: SourceManga): List<SourceChapter> =
        mangaUpdate(source, manga, fetchDetails = false, fetchChapters = true)?.chapters.orEmpty()

    suspend fun mangaUpdate(
        source: SourceDescriptor,
        manga: SourceManga,
        chapters: List<SourceChapter> = emptyList(),
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SourceMangaUpdate? = withContext(Dispatchers.IO) {
        val instance = findSource(source) ?: return@withContext null
        updateGate.run(instance, manga.url) {
            runSourceAction(source, instance, "mangaUpdate") {
                instance.updateManga(manga.copy(sourceId = source.id), chapters, fetchDetails, fetchChapters)
            }
        }
    }

    suspend fun pages(source: SourceDescriptor, chapter: SourceChapter): List<ReaderPage> = withContext(Dispatchers.IO) {
        val sourceInstance = findSource(source) ?: return@withContext emptyList()
        runSourceAction(source, sourceInstance, "pages") {
            val sourceChapter = chapter.toSChapter()
            val pages = sourceInstance.getPageList(sourceChapter)
            pages.map { page ->
                val imageRequest = if (sourceInstance is HttpSource && page.imageUrl != null) {
                    runCatching {
                        sourceInstance.imageRequest(page)
                    }.onFailure { error ->
                        logSourceFailure(
                            action = "imageRequest",
                            packageName = source.packageName,
                            source = sourceInstance,
                            error = error,
                        )
                    }.getOrNull()
                } else {
                    null
                }
                val imageUrl = imageRequest?.url?.toString()
                    ?: page.imageUrl
                    ?: page.uri?.toString()
                    ?: page.url
                val imageUrlResolved = imageRequest != null || page.imageUrl != null || page.uri != null
                val headers = imageRequest?.headers?.names()
                    ?.associateWith { name -> imageRequest.headers[name].orEmpty() }
                    .orEmpty()
                ReaderPage(
                    index = page.index,
                    imageUrl = imageUrl,
                    cachedFilePath = null,
                    headers = headers,
                    sourcePageUrl = page.url,
                    imageUrlResolved = imageUrlResolved,
                )
            }
        }
    }

    suspend fun imageBytes(
        source: SourceDescriptor,
        page: ReaderPage,
        maxAttempts: Int = SOURCE_IMAGE_RETRY_ATTEMPTS,
    ): ByteArray = withContext(Dispatchers.IO) {
        val sourceInstance = findSource(source) ?: error("Source is not installed")
        if (sourceInstance !is HttpSource) {
            error("${sourceInstance.name} does not support HTTP image loading")
        }
        val fetchKey = source.imageFetchKey()
        val semaphore = imageFetchSemaphores.computeIfAbsent(fetchKey) { Semaphore(SOURCE_IMAGE_FETCH_CONCURRENCY) }
        val rateLimiter = imageFetchRateLimiters.computeIfAbsent(fetchKey) {
            RespectfulRateLimiter(minSpacingMillis = SOURCE_IMAGE_REQUEST_SPACING_MILLIS)
        }
        semaphore.withPermit {
            runSourceAction(source, sourceInstance, "image") {
                fetchImageBytesWithRetries(
                    sourceInstance = sourceInstance,
                    page = page,
                    rateLimiter = rateLimiter,
                    maxAttempts = maxAttempts,
                )
            }
        }
    }

    private suspend fun fetchImageBytesWithRetries(
        sourceInstance: HttpSource,
        page: ReaderPage,
        rateLimiter: RespectfulRateLimiter,
        maxAttempts: Int,
    ): ByteArray {
        val sourcePageUrl = page.sourcePageUrl.ifBlank { page.imageUrl }
        val imageUrlCandidate = page.imageUrl.takeIf { it.isNotBlank() }
        val sourcePage = Page(
            index = page.index,
            url = sourcePageUrl,
            imageUrl = imageUrlCandidate.takeIf {
                page.imageUrlResolved || it != sourcePageUrl || it.looksLikeImageUrl()
            },
        )
        if (sourcePage.imageUrl == null) {
            sourcePage.imageUrl = sourceInstance.getImageUrl(sourcePage).takeIf { it.isNotBlank() }
                ?: error("Page ${page.index + 1} did not return an image")
        }
        var attempt = 1
        val attempts = maxAttempts.coerceAtLeast(1)
        var delayMillis = SOURCE_IMAGE_RETRY_INITIAL_DELAY_MILLIS
        var lastError: Throwable? = null

        while (attempt <= attempts) {
            try {
                return rateLimiter.run {
                    fetchImageBytesOnce(
                        sourceInstance = sourceInstance,
                        sourcePage = sourcePage,
                        page = page,
                        rateLimiter = rateLimiter,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastError = error
                if (attempt >= attempts || !error.isTransientSourceImageFailure()) {
                    throw error
                }
                delay(delayMillis)
                delayMillis = (delayMillis * 2).coerceAtMost(SOURCE_IMAGE_RETRY_MAX_DELAY_MILLIS)
                attempt += 1
            }
        }

        throw lastError ?: IllegalStateException("Page ${page.index + 1} failed")
    }

    private suspend fun fetchImageBytesOnce(
        sourceInstance: HttpSource,
        sourcePage: Page,
        page: ReaderPage,
        rateLimiter: RespectfulRateLimiter,
    ): ByteArray = sourceInstance.fetchImage(sourcePage).map { response ->
        // Consume the body within the subscription: cancelling it cancels the
        // transport even while reading a slow or large image.
        response.use {
            rateLimiter.recordResponse(it.headers, it.code)
            if (!it.isSuccessful) throw SourceImageHttpException(page.index, it.code)
            val contentType = it.body.contentType()?.toString().orEmpty()
            it.body.bytes().also { bytes ->
                check(bytes.looksLikeImage() || contentType.startsWith("image/", ignoreCase = true)) {
                    "Page ${page.index + 1} did not return an image"
                }
            }
        }
    }.awaitSourceValue()

    private fun SourceDescriptor.imageFetchKey(): String = "$packageName:$id"

    companion object {
        private const val SOURCE_IMAGE_FETCH_CONCURRENCY = 5
        private const val SOURCE_IMAGE_REQUEST_SPACING_MILLIS = 200L
        private const val SOURCE_IMAGE_RETRY_ATTEMPTS = 3
        private const val SOURCE_IMAGE_RETRY_INITIAL_DELAY_MILLIS = 2_500L
        private const val SOURCE_IMAGE_RETRY_MAX_DELAY_MILLIS = 15_000L
    }

    private fun findSource(source: SourceDescriptor): Source? =
        loadSources(source.packageName)
            .bestMatch(source)

    private fun loadDeclaredSourceClasses(packageInfo: PackageInfo): List<Any> {
        ensureHttpAgent()
        val packageName = packageInfo.packageName
        val appInfo = packageInfo.applicationInfo ?: return emptyList()
        // Use the same package snapshot that passed the signer check.
        val classLoader = PathClassLoader(appInfo.sourceDir, appContext.classLoader)
        val metadata = appInfo.metaData

        val declared = metadata?.getString("tachiyomi.extension.class")
            ?.split(';', ',')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.map { it.toFullyQualifiedSourceClassName(packageName) }
            .orEmpty()

        return declared.mapNotNull { className ->
            runCatching {
                classLoader.loadClass(className).getDeclaredConstructor().newInstance()
            }.onFailure { error ->
                logSourceFailure(
                    action = "loadClass",
                    packageName = packageName,
                    className = className,
                    error = error,
                )
            }.getOrNull()
        }
    }

    private fun packageInfo(packageName: String): PackageInfo? =
        runCatching {
            @Suppress("DEPRECATION")
            appContext.packageManager.getPackageInfo(packageName, EXTENSION_PACKAGE_FLAGS)
        }.onFailure { error ->
            logSourceFailure(
                action = "packageInfo",
                packageName = packageName,
                error = error,
            )
        }.getOrNull()

    private fun PackageInfo.cacheKey(): String {
        val version = if (android.os.Build.VERSION.SDK_INT >= 28) longVersionCode else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }
        val sourceDir = applicationInfo?.sourceDir.orEmpty()
        val updatedAt = lastUpdateTime
        return "$version:$updatedAt:$sourceDir"
    }
}

private data class CachedSources(
    val cacheKey: String,
    val sources: List<Source>,
)

class SourceActionException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)

private class SourceImageHttpException(
    pageIndex: Int,
    val statusCode: Int,
) : IllegalStateException("Page ${pageIndex + 1} failed: HTTP $statusCode")

private fun Throwable.isTransientSourceImageFailure(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        when (current) {
            is SourceImageHttpException -> return current.statusCode in TRANSIENT_SOURCE_IMAGE_STATUS_CODES
            is IOException -> return true
        }
        current = current.cause
    }
    return false
}

private val TRANSIENT_SOURCE_IMAGE_STATUS_CODES = setOf(
    408,
    425,
    429,
    500,
    502,
    503,
    504,
    520,
    521,
    522,
    523,
    524,
)

private fun Throwable.causeChain(): Sequence<Throwable> = sequence {
    var current: Throwable? = this@causeChain
    val seen = mutableSetOf<Throwable>()
    while (current != null && seen.add(current)) {
        yield(current)
        current = current.cause
    }
}

private suspend fun <T> runSourceAction(
    descriptor: SourceDescriptor,
    source: Source,
    action: String,
    block: suspend () -> T,
): T {
    try {
        return block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        logSourceFailure(
            action = action,
            packageName = descriptor.packageName,
            source = source,
            error = error,
        )
        throw SourceActionException(
            "${source.name} $action failed: ${error.javaClass.simpleName}",
            error,
        )
    }
}

private fun logSourceFailure(
    action: String,
    packageName: String,
    source: Source? = null,
    className: String? = null,
    error: Throwable,
) {
    val sourcePart = source?.let { " source=${it.name}/${it.lang} id=${it.id}" }.orEmpty()
    val classPart = className?.let { " class=$it" }.orEmpty()
    Log.w(
        "TankobunSources",
        "Extension action failed action=$action package=$packageName$sourcePart$classPart error=${error.javaClass.name}",
        error,
    )
}

private fun ensureHttpAgent() {
    if (System.getProperty("http.agent").isNullOrBlank()) {
        System.setProperty("http.agent", eu.kanade.tachiyomi.network.NetworkHelper.defaultUserAgent())
    }
}

private fun String.looksLikeImageUrl(): Boolean {
    val path = substringBefore('#').substringBefore('?').lowercase()
    return path.endsWith(".jpg") ||
        path.endsWith(".jpeg") ||
        path.endsWith(".png") ||
        path.endsWith(".webp") ||
        path.endsWith(".gif") ||
        path.endsWith(".avif") ||
        path.endsWith(".bmp")
}

private fun ByteArray.looksLikeImage(): Boolean =
    startsWith(0xFF, 0xD8, 0xFF) ||
        startsWith(0x89, 0x50, 0x4E, 0x47) ||
        startsWith(0x47, 0x49, 0x46) ||
        (
            size >= 12 &&
                this[0] == 0x52.toByte() &&
                this[1] == 0x49.toByte() &&
                this[2] == 0x46.toByte() &&
                this[3] == 0x46.toByte() &&
                this[8] == 0x57.toByte() &&
                this[9] == 0x45.toByte() &&
                this[10] == 0x42.toByte() &&
                this[11] == 0x50.toByte()
            )

private fun ByteArray.startsWith(vararg values: Int): Boolean =
    size >= values.size && values.indices.all { index -> this[index] == values[index].toByte() }

private fun <T : Source> List<T>.bestMatch(source: SourceDescriptor): T? {
    firstOrNull { it.id == source.id }?.let { return it }
    firstOrNull { it.name == source.name && it.lang == source.lang }?.let { return it }
    firstOrNull {
        it.name.equals(source.name, ignoreCase = true) &&
            it.lang.equals(source.lang, ignoreCase = true)
    }?.let { return it }
    firstOrNull { it.name == source.name }?.let { return it }
    val sameLanguage = filter { it.lang == source.lang }
    return sameLanguage.singleOrNull() ?: singleOrNull()
}

internal fun String.toFullyQualifiedSourceClassName(packageName: String): String =
    when {
        startsWith('.') -> packageName + this
        '.' in this -> this
        else -> "$packageName.$this"
    }
