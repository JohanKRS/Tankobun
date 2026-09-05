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
import dalvik.system.PathClassLoader
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.TankobunInjektRegistry
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

class TachiyomiSourceHost(
    private val context: Context,
    private val trustStore: ExtensionTrustStore = ExtensionTrustStore(context),
) {
    private val appContext = context.applicationContext
    private val sourceCache = mutableMapOf<String, CachedSources>()
    private val imageFetchSemaphores = ConcurrentHashMap<String, Semaphore>()
    private val imageFetchRateLimiters = ConcurrentHashMap<String, RespectfulRateLimiter>()

    init {
        TankobunInjektRegistry.applicationOrNull()?.let { registered ->
            eu.kanade.tachiyomi.network.NetworkHelper.configure(registered)
        }
        ensureHttpAgent()
    }

    fun loadSources(packageName: String): List<Source> {
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
        val catalogueSource = findCatalogueSource(source)
            ?: return@withContext emptyList()

        runSourceAction(source, catalogueSource, "search") {
            val filters = catalogueSource.getFilterList()
            runCatching {
                catalogueSource.searchManga(page, query, filters)
            }.getOrElse { error ->
                if (filters.isEmpty() || !error.isFilterCompatibilityError()) {
                    throw error
                }
                logSourceFailure(
                    action = "searchWithDefaultFilters",
                    packageName = source.packageName,
                    source = catalogueSource,
                    error = error,
                )
                catalogueSource.searchManga(page, query, FilterList())
            }
        }
    }

    suspend fun mangaDetails(source: SourceDescriptor, manga: SourceManga): SourceManga? = withContext(Dispatchers.IO) {
        val sourceInstance = findSource(source) ?: return@withContext null
        runSourceAction(source, sourceInstance, "mangaDetails") {
            val sourceManga = manga.toSManga()
            val details = runCatching {
                sourceInstance.fetchMangaDetails(sourceManga).toBlocking().first()
            }.getOrElse {
                sourceInstance.getMangaUpdate(
                    manga = sourceManga,
                    chapters = emptyList(),
                    fetchDetails = true,
                    fetchChapters = false,
                ).manga
            }
            details.toSourceManga(sourceInstance.id).let { resolved ->
                resolved.copy(
                    url = resolved.url.ifBlank { manga.url },
                    title = resolved.title.ifBlank { manga.title },
                    thumbnailUrl = resolved.thumbnailUrl ?: manga.thumbnailUrl,
                    description = resolved.description ?: manga.description,
                    author = resolved.author ?: manga.author,
                    artist = resolved.artist ?: manga.artist,
                    status = resolved.status ?: manga.status,
                )
            }
        }
    }

    suspend fun chapters(source: SourceDescriptor, manga: SourceManga): List<SourceChapter> = withContext(Dispatchers.IO) {
        val sourceInstance = findSource(source) ?: return@withContext emptyList()
        runSourceAction(source, sourceInstance, "chapters") {
            val sourceManga = manga.toSManga()
            val chapters = runCatching {
                sourceInstance.fetchChapterList(sourceManga).toBlocking().first()
            }.getOrElse {
                sourceInstance.getMangaUpdate(
                    manga = sourceManga,
                    chapters = emptyList(),
                    fetchDetails = false,
                    fetchChapters = true,
                ).chapters
            }.ifEmpty {
                runCatching {
                    sourceInstance.getMangaUpdate(
                        manga = sourceManga,
                        chapters = emptyList(),
                        fetchDetails = false,
                        fetchChapters = true,
                    ).chapters
                }.getOrDefault(emptyList())
            }
            chapters.map { chapter ->
                chapter.toSourceChapter(sourceInstance.id, manga.url)
            }
        }
    }

    suspend fun pages(source: SourceDescriptor, chapter: SourceChapter): List<ReaderPage> = withContext(Dispatchers.IO) {
        val sourceInstance = findSource(source) ?: return@withContext emptyList()
        runSourceAction(source, sourceInstance, "pages") {
            val sourceChapter = chapter.toSChapter()
            val pages = sourceInstance.fetchPageListWithTimeout(sourceChapter)
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
        val semaphore = imageFetchSemaphores.getOrPut(fetchKey) { Semaphore(SOURCE_IMAGE_FETCH_CONCURRENCY) }
        val rateLimiter = imageFetchRateLimiters.getOrPut(fetchKey) {
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
        val resolutionError = if (sourcePage.imageUrl == null) {
            sourceInstance.resolveImageUrl(sourcePage)
        } else {
            null
        }
        if (sourcePage.imageUrl == null) {
            sourcePage.imageUrl = sourcePageUrl.takeIf { it.isHttpUrl() }
        }
        val usingDirectFallback = sourcePage.imageUrl == sourcePageUrl && resolutionError != null
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
                        usingDirectFallback = usingDirectFallback,
                        resolutionError = resolutionError,
                    )
                }
            } catch (error: SourceImageTimeoutException) {
                throw error
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
        usingDirectFallback: Boolean,
        resolutionError: Throwable?,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            val future = SOURCE_IMAGE_EXECUTOR.submit<ByteArray> {
                fetchImageBytesBlocking(
                    sourceInstance = sourceInstance,
                    sourcePage = sourcePage,
                    page = page,
                    rateLimiter = rateLimiter,
                    usingDirectFallback = usingDirectFallback,
                    resolutionError = resolutionError,
                )
            }
            try {
                future.get(SOURCE_IMAGE_ATTEMPT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            } catch (error: TimeoutException) {
                future.cancel(true)
                throw SourceImageTimeoutException(page.index, error)
            } catch (error: ExecutionException) {
                throw error.cause ?: error
            } catch (error: InterruptedException) {
                future.cancel(true)
                Thread.currentThread().interrupt()
                throw CancellationException("Image fetch interrupted").apply { initCause(error) }
            }
        }

    private fun fetchImageBytesBlocking(
        sourceInstance: HttpSource,
        sourcePage: Page,
        page: ReaderPage,
        rateLimiter: RespectfulRateLimiter,
        usingDirectFallback: Boolean,
        resolutionError: Throwable?,
    ): ByteArray =
        runCatching {
            sourceInstance.fetchImage(sourcePage).toBlocking().first()
        }.getOrElse { error ->
            if (usingDirectFallback && resolutionError != null) {
                error.addSuppressed(resolutionError)
            }
            throw error
        }.use { response ->
            rateLimiter.recordResponse(response.headers, response.code)
            if (!response.isSuccessful) {
                throw SourceImageHttpException(page.index, response.code).also { error ->
                    if (usingDirectFallback && resolutionError != null) {
                        error.addSuppressed(resolutionError)
                    }
                }
            }
            val contentType = response.body.contentType()?.toString().orEmpty()
            response.body.bytes().also { bytes ->
                if (!bytes.looksLikeImage() && !contentType.startsWith("image/", ignoreCase = true)) {
                    throw IllegalStateException("Page ${page.index + 1} did not return an image").also { error ->
                        if (usingDirectFallback && resolutionError != null) {
                            error.addSuppressed(resolutionError)
                        }
                    }
                }
            }
        }

    private fun SourceDescriptor.imageFetchKey(): String = "$packageName:$id"

    private fun Source.fetchPageListWithTimeout(sourceChapter: SChapter): List<Page> {
        val future = SOURCE_PAGE_LIST_EXECUTOR.submit<List<Page>> {
            runCatching {
                fetchPageList(sourceChapter).toBlocking().first()
            }.getOrElse { error ->
                if (error.isSourcePageListTimeout()) {
                    throw SourcePageListTimeoutException(name, error)
                }
                runBlocking { getPageList(sourceChapter) }
            }
        }
        return try {
            future.get(SOURCE_PAGE_LIST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        } catch (error: TimeoutException) {
            future.cancel(true)
            throw SourcePageListTimeoutException(name, error)
        } catch (error: ExecutionException) {
            throw error.cause ?: error
        } catch (error: InterruptedException) {
            future.cancel(true)
            Thread.currentThread().interrupt()
            throw CancellationException("Page list fetch interrupted").apply { initCause(error) }
        }
    }

    companion object {
        private const val TAG = "TankobunSources"
        private const val SOURCE_PAGE_LIST_TIMEOUT_MILLIS = 8_000L
        private const val SOURCE_IMAGE_FETCH_CONCURRENCY = 5
        private const val SOURCE_IMAGE_REQUEST_SPACING_MILLIS = 200L
        private const val SOURCE_IMAGE_RETRY_ATTEMPTS = 3
        private const val SOURCE_IMAGE_ATTEMPT_TIMEOUT_MILLIS = 25_000L
        private const val SOURCE_IMAGE_RETRY_INITIAL_DELAY_MILLIS = 2_500L
        private const val SOURCE_IMAGE_RETRY_MAX_DELAY_MILLIS = 15_000L
        private val SOURCE_PAGE_LIST_THREAD_COUNT = AtomicInteger()
        private val SOURCE_PAGE_LIST_EXECUTOR = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "TankobunSourcePages-${SOURCE_PAGE_LIST_THREAD_COUNT.incrementAndGet()}").apply {
                isDaemon = true
            }
        }
        private val SOURCE_IMAGE_THREAD_COUNT = AtomicInteger()
        private val SOURCE_IMAGE_EXECUTOR = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "TankobunSourceImage-${SOURCE_IMAGE_THREAD_COUNT.incrementAndGet()}").apply {
                isDaemon = true
            }
        }
    }

    private fun findCatalogueSource(source: SourceDescriptor): CatalogueSource? =
        loadSources(source.packageName)
            .filterIsInstance<CatalogueSource>()
            .bestMatch(source)

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

private class SourceImageTimeoutException(
    pageIndex: Int,
    cause: Throwable,
) : IOException("Page ${pageIndex + 1} timed out while loading", cause)

private class SourcePageListTimeoutException(
    sourceName: String,
    cause: Throwable,
) : IOException("$sourceName took too long to open the chapter", cause)

private fun Throwable.isSourcePageListTimeout(): Boolean =
    causeChain().any { error ->
        error is TimeoutException ||
            error.message?.contains("timed out", ignoreCase = true) == true ||
            error.message?.contains("timeout", ignoreCase = true) == true
    }

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

private fun CatalogueSource.searchManga(
    page: Int,
    query: String,
    filters: FilterList,
): List<SourceManga> =
    fetchSearchManga(page, query, filters).toBlocking().first().mangas.map { manga ->
        manga.toSourceManga(id)
    }

private fun Throwable.isFilterCompatibilityError(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current.message?.contains("Unknown filter", ignoreCase = true) == true) {
            return true
        }
        current = current.cause
    }
    return false
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

private suspend fun HttpSource.resolveImageUrl(page: Page): Throwable? =
    runCatching {
        getImageUrl(page)
    }.onSuccess { resolvedUrl ->
        if (resolvedUrl.isNotBlank()) {
            page.imageUrl = resolvedUrl
        }
    }.exceptionOrNull()
        ?.takeUnless { it.hasUnsupportedOperationCause() }

private fun String.isHttpUrl(): Boolean =
    startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)

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

private fun Throwable.hasUnsupportedOperationCause(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is UnsupportedOperationException) {
            return true
        }
        current = current.cause
    }
    return false
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

private fun SourceManga.toSManga(): SManga =
    SManga.create().also {
        it.url = url
        it.title = title
        it.thumbnail_url = thumbnailUrl
        it.description = description
        it.author = author
        it.artist = artist
    }

private fun SManga.toSourceManga(sourceId: Long): SourceManga =
    SourceManga(
        sourceId = sourceId,
        url = url,
        title = title,
        thumbnailUrl = thumbnail_url,
        description = description,
        author = author,
        artist = artist,
        status = status.toString(),
    )

private fun SChapter.toSourceChapter(sourceId: Long, mangaUrl: String): SourceChapter =
    SourceChapter(
        sourceId = sourceId,
        mangaUrl = mangaUrl,
        url = url,
        name = name,
        chapterNumber = chapter_number,
        scanlator = scanlator,
        uploadedAtEpochMillis = date_upload.takeIf { it > 0 },
    )

private fun SourceChapter.toSChapter(): SChapter =
    SChapter.create().also {
        it.url = url
        it.name = name
        it.chapter_number = chapterNumber
        it.scanlator = scanlator
        it.date_upload = uploadedAtEpochMillis ?: 0L
    }
