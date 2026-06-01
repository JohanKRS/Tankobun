package com.tankobun.core.extensions

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.TankobunInjektRegistry

class TachiyomiSourceHost(
    private val context: Context,
) {
    private val appContext = context.applicationContext
    private val sourceCache = mutableMapOf<String, CachedSources>()

    init {
        TankobunInjektRegistry.applicationOrNull()?.let { registered ->
            eu.kanade.tachiyomi.network.NetworkHelper.configure(registered)
        }
        ensureHttpAgent()
    }

    fun loadSources(packageName: String): List<Source> {
        ensureHttpAgent()
        val packageInfo = packageInfo(packageName) ?: return emptyList()
        val key = packageInfo.cacheKey()
        synchronized(sourceCache) {
            sourceCache[packageName]?.takeIf { it.cacheKey == key }?.let { return it.sources }
        }

        val sources = loadDeclaredSourceClasses(packageName).flatMap { instance ->
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
            val pages = runCatching {
                sourceInstance.fetchPageList(sourceChapter).toBlocking().first()
            }.getOrElse {
                sourceInstance.getPageList(sourceChapter)
            }
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
                val headers = imageRequest?.headers?.names()
                    ?.associateWith { name -> imageRequest.headers[name].orEmpty() }
                    .orEmpty()
                ReaderPage(
                    index = page.index,
                    imageUrl = imageUrl,
                    cachedFilePath = null,
                    headers = headers,
                    sourcePageUrl = page.url,
                )
            }
        }
    }

    suspend fun imageBytes(source: SourceDescriptor, page: ReaderPage): ByteArray = withContext(Dispatchers.IO) {
        val sourceInstance = findSource(source) ?: error("Source is not installed")
        if (sourceInstance !is HttpSource) {
            error("${sourceInstance.name} does not support HTTP image loading")
        }
        runSourceAction(source, sourceInstance, "image") {
            val sourcePageUrl = page.sourcePageUrl.ifBlank { page.imageUrl }
            val imageUrlCandidate = page.imageUrl.takeIf { it.isNotBlank() }
            val sourcePage = Page(
                index = page.index,
                url = sourcePageUrl,
                imageUrl = imageUrlCandidate.takeIf { it != sourcePageUrl || it.looksLikeImageUrl() },
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
            runCatching {
                sourceInstance.fetchImage(sourcePage).toBlocking().first()
            }.getOrElse { error ->
                if (usingDirectFallback) {
                    resolutionError?.let(error::addSuppressed)
                }
                throw error
            }.use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Page ${page.index + 1} failed: HTTP ${response.code}").also { error ->
                        if (usingDirectFallback) {
                            resolutionError?.let(error::addSuppressed)
                        }
                    }
                }
                val contentType = response.body.contentType()?.toString().orEmpty()
                response.body.bytes().also { bytes ->
                    if (!bytes.looksLikeImage() && !contentType.startsWith("image/", ignoreCase = true)) {
                        throw IllegalStateException("Page ${page.index + 1} did not return an image").also { error ->
                            if (usingDirectFallback) {
                                resolutionError?.let(error::addSuppressed)
                            }
                        }
                    }
                }
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

    private fun loadDeclaredSourceClasses(packageName: String): List<Any> {
        ensureHttpAgent()
        val extensionContext = appContext.createPackageContext(
            packageName,
            Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY,
        )
        val classLoader = PathClassLoader(extensionContext.applicationInfo.sourceDir, appContext.classLoader)
        val metadata = extensionContext.packageManager
            .getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
            .metaData

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
            appContext.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
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

    companion object {
        private const val TAG = "TankobunSources"
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

private suspend fun <T> runSourceAction(
    descriptor: SourceDescriptor,
    source: Source,
    action: String,
    block: suspend () -> T,
): T =
    runCatching {
        block()
    }.onFailure { error ->
        logSourceFailure(
            action = action,
            packageName = descriptor.packageName,
            source = source,
            error = error,
        )
    }.getOrElse { error ->
        throw SourceActionException(
            "${source.name} $action failed: ${error.javaClass.simpleName}",
            error,
        )
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
