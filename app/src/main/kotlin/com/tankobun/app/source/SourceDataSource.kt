package com.tankobun.app.source

import android.util.Log
import com.tankobun.app.AppContainer
import com.tankobun.app.BuildConfig
import com.tankobun.app.logic.SOURCE_CANDIDATES_TO_VERIFY
import com.tankobun.app.logic.SourceQueryTimeoutException
import com.tankobun.app.logic.VerifiedReadableMatch
import com.tankobun.app.logic.VerifiedSourceMatches
import com.tankobun.app.logic.isFatalSourceSearchError
import com.tankobun.app.logic.isReadableMatchCandidate
import com.tankobun.app.logic.sourceMatchKey
import com.tankobun.app.logic.sourcePickerDiagnosticDetail
import com.tankobun.app.logic.sourceSearchQueries
import com.tankobun.app.logic.sourceSearchRankTitleVariants
import com.tankobun.app.logic.withCurrentSourceChapters
import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.core.database.SourceSearchResultEntity
import com.tankobun.core.database.toEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.CachePolicy
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceBinding
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceManga
import com.tankobun.core.model.SourceMangaUpdate
import androidx.room.withTransaction
import com.tankobun.core.model.SourceSearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

internal data class LoadedSourceChapters(
    val manga: SourceManga,
    val chapters: List<SourceChapter>,
    val chapterProgress: Map<String, ReadingProgress>,
)

internal data class CachedSourceState(
    val sourceMatches: List<SourceSearchResult>,
    val sourceMatchChapterCounts: Map<String, Int>,
    val boundSourceId: Long?,
    val boundSourcePackageName: String?,
    val boundSource: SourceDescriptor?,
    val boundManga: SourceManga?,
    val sourceChapters: List<SourceChapter>,
    val chaptersFresh: Boolean,
    val latestProgress: ReadingProgress?,
    val chapterProgress: Map<String, ReadingProgress>,
)

internal sealed class SourcePickerSearchUpdate {
    data class Match(val match: SourceSearchResult, val chapterCount: Int) : SourcePickerSearchUpdate()
    data class Diagnostic(val source: SourceDescriptor, val detail: String) : SourcePickerSearchUpdate()
}

internal class SourceDataSource(
    private val container: AppContainer,
    private val cachePolicy: CachePolicy,
) {
    suspend fun cachedSourceState(mediaId: Int, sources: List<SourceDescriptor>): CachedSourceState {
        val binding = container.database.sourceBindingDao().bindingForMedia(mediaId)
        val cachedResults = container.database.sourceSearchDao().cachedSearchResults(mediaId)
        val cachedMatches = cachedResults.map { result ->
            val source = sources.sourceFor(
                sourceId = result.sourceId,
                packageName = result.sourcePackageName,
                sourceName = result.sourceName,
                sourceLang = result.sourceLang,
            )
                ?: SourceDescriptor(
                    id = result.sourceId,
                    name = result.sourceName,
                    lang = result.sourceLang,
                    packageName = result.sourcePackageName,
                    versionName = null,
                    versionCode = null,
                    isNsfw = false,
                    installed = false,
                )
            result.toSearchResult(source)
        }.distinctBy { "${it.source.packageName}:${it.source.id}:${it.manga.url}:${it.manga.title}" }

        val chapterCounts = mutableMapOf<String, Int>()
        val matches = buildList {
            cachedMatches.forEach { match ->
                val chapters = container.database.chapterDao().cachedChapters(match.source.id, match.manga.url)
                if (chapters.isNotEmpty()) {
                    chapterCounts[match.sourceMatchKey()] = chapters.size
                    add(match)
                }
            }
        }

        val boundSourceHint = binding?.let { cached ->
            cachedResults.firstOrNull { result ->
                result.sourcePackageName == cached.sourcePackageName &&
                    (result.sourceId == cached.sourceId || result.mangaUrl == cached.mangaUrl)
            }
        }
        val boundSource = binding?.let { cached ->
            sources.sourceFor(
                sourceId = cached.sourceId,
                packageName = cached.sourcePackageName,
                sourceName = boundSourceHint?.sourceName,
                sourceLang = boundSourceHint?.sourceLang,
            )
        }
        val effectiveBinding = if (binding != null && boundSource != null && (
                binding.sourceId != boundSource.id || binding.sourcePackageName != boundSource.packageName
            )
        ) {
            binding.copy(
                sourceId = boundSource.id,
                sourcePackageName = boundSource.packageName,
            ).also { migrated ->
                container.database.sourceBindingDao().upsertBinding(migrated)
            }
        } else {
            binding
        }
        val boundManga = binding?.let { cached ->
            SourceManga(
                sourceId = boundSource?.id ?: cached.sourceId,
                url = cached.mangaUrl,
                title = cached.mangaTitle,
                thumbnailUrl = cached.thumbnailUrl,
                memoJson = cached.memoJson,
                description = null,
                author = null,
                artist = null,
                status = null,
            )
        }
        val chapterRows = if (boundSource != null && boundManga != null) {
            container.database.chapterDao()
                .cachedChapters(boundSource.id, boundManga.url)
        } else {
            emptyList()
        }

        val chapters = chapterRows.map { it.toModel() }
        val freshness = com.tankobun.core.network.CacheFreshness()
        val chaptersFresh = chapterRows.isNotEmpty() && chapterRows.all { freshness.isFresh(it.fetchedAtEpochMillis, cachePolicy.sourceChapterTtlMillis) }

        val visibleMatches = matches.toMutableList()
        if (binding != null && boundSource != null && boundManga != null && chapters.isNotEmpty()) {
            val selectedMatch = SourceSearchResult(
                mediaId = mediaId,
                source = boundSource,
                manga = boundManga,
                score = 1.0,
                reasons = listOf("selected source"),
                searchedAtEpochMillis = binding.selectedAtEpochMillis,
            )
            if (
                visibleMatches.none {
                    it.source.id == selectedMatch.source.id &&
                        it.source.packageName == selectedMatch.source.packageName &&
                        it.manga.url == selectedMatch.manga.url
                }
            ) {
                visibleMatches.add(0, selectedMatch)
            }
            chapterCounts[selectedMatch.sourceMatchKey()] = chapters.size
        }

        return CachedSourceState(
            sourceMatches = visibleMatches,
            sourceMatchChapterCounts = chapterCounts,
            boundSourceId = effectiveBinding?.sourceId,
            boundSourcePackageName = effectiveBinding?.sourcePackageName,
            boundSource = boundSource,
            boundManga = boundManga,
            sourceChapters = chapters,
            chaptersFresh = chaptersFresh,
            latestProgress = container.database.progressDao().latestProgress(mediaId)?.toModel(),
            chapterProgress = progressByChapter(mediaId),
        )
    }

    suspend fun searchVerifiedMatches(
        media: AnilistMedia,
        sources: List<SourceDescriptor>,
        now: Long,
        titleOverride: String? = null,
        diagnosticSearchTimedOut: String = "search timed out",
        diagnosticNoSearchResults: String = "no search results",
        diagnosticNoConfidentTitleMatch: String = "no confident title match",
        diagnosticNoReadableChapters: String = "no readable chapters",
        diagnosticDetail: (Throwable) -> String = ::sourcePickerDiagnosticDetail,
        onUpdate: suspend (SourcePickerSearchUpdate) -> Unit = {},
    ): VerifiedSourceMatches = supervisorScope {
        val semaphore = Semaphore(SOURCE_SEARCH_CONCURRENCY)
        val verified = sources
            .map { source ->
                async {
                    semaphore.withPermit {
                        try {
                            val searchResults = withTimeoutOrNull(SOURCE_MATCH_TIMEOUT_MILLIS) {
                                searchSourceMatches(media, source, now, titleOverride)
                            }
                            val candidates = searchResults
                                .orEmpty()
                                .filter { it.isReadableMatchCandidate() }
                                .take(SOURCE_CANDIDATES_TO_VERIFY)
                            when {
                                searchResults == null -> {
                                    onUpdate(SourcePickerSearchUpdate.Diagnostic(source, diagnosticSearchTimedOut))
                                    null
                                }
                                searchResults.isEmpty() -> {
                                    onUpdate(SourcePickerSearchUpdate.Diagnostic(source, diagnosticNoSearchResults))
                                    null
                                }
                                candidates.isEmpty() -> {
                                    onUpdate(SourcePickerSearchUpdate.Diagnostic(source, diagnosticNoConfidentTitleMatch))
                                    null
                                }
                                else -> {
                                    val readable = withTimeoutOrNull(SOURCE_VERIFY_TIMEOUT_MILLIS) {
                                        firstReadableMatch(source, candidates, now)
                                    }
                                    if (readable == null) {
                                        onUpdate(SourcePickerSearchUpdate.Diagnostic(source, diagnosticNoReadableChapters))
                                    } else {
                                        onUpdate(SourcePickerSearchUpdate.Match(readable.match, readable.chapterCount))
                                    }
                                    readable
                                }
                            }
                        } catch (error: Throwable) {
                            if (error is CancellationException) throw error
                            Log.w(TAG, "Source verification failed for ${source.name}", error)
                            onUpdate(SourcePickerSearchUpdate.Diagnostic(source, diagnosticDetail(error)))
                            null
                        }
                    }
                }
            }
            .awaitAll()
            .filterNotNull()

        val matches = verified.map { it.match }
        val counts = verified.associate { it.match.sourceMatchKey() to it.chapterCount }

        VerifiedSourceMatches(
            matches = matches.distinctBy { "${it.source.packageName}:${it.source.id}:${it.manga.url}" }
                .sortedByDescending { it.score },
            chapterCounts = counts,
        )
    }

    suspend fun progressByChapter(mediaId: Int): Map<String, ReadingProgress> =
        container.database.progressDao()
            .progressForMedia(mediaId)
            .map { it.toModel() }
            .associateBy { it.chapterUrl }

    suspend fun resolveRecentReadingProgress(
        item: RecentReadingProgress,
        sources: List<SourceDescriptor>,
        now: Long,
    ): RecentReadingProgress {
        val cached = cachedSourceState(item.media.id, sources)
        val boundPackageName = cached.boundSource?.packageName ?: cached.boundSourcePackageName
            ?: return item
        val source = cached.boundSource
        val manga = cached.boundManga
        if (source == null || manga == null) {
            return item.withCurrentSourceChapters(boundPackageName, emptyList())
        }
        val chapters = cached.sourceChapters.ifEmpty {
            runCatching {
                loadChapters(
                    source = source,
                    manga = manga,
                    mediaId = item.media.id,
                    now = now,
                ).chapters
            }.onFailure { error ->
                Log.w(TAG, "Current source chapter load failed for ${item.media.id}/${source.name}", error)
            }.getOrDefault(emptyList())
        }
        return item.withCurrentSourceChapters(source.packageName, chapters)
    }

    suspend fun searchSourceMatches(
        media: AnilistMedia,
        source: SourceDescriptor,
        now: Long,
        titleOverride: String? = null,
    ): List<SourceSearchResult> {
        val queries = sourceSearchQueries(media, titleOverride)
        val titleOverrides = titleOverride?.let(::sourceSearchRankTitleVariants).orEmpty()
        val candidates = mutableListOf<SourceManga>()
        var searchedQueries = 0
        var failedQueries = 0
        var lastSearchError: Throwable? = null

        for (query in queries) {
            var stopSourceSearch = false
            val results = runCatching {
                withTimeoutOrNull(SOURCE_QUERY_TIMEOUT_MILLIS) {
                    container.sourceHost.search(source, query)
                } ?: throw SourceQueryTimeoutException(query)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                failedQueries += 1
                lastSearchError = error
                stopSourceSearch = isFatalSourceSearchError(error)
                Log.w(TAG, "Source search failed for ${source.name} with '$query'", error)
            }.getOrDefault(emptyList())

            searchedQueries += 1
            if (results.isNotEmpty()) {
                candidates += results
            }
            if (stopSourceSearch) {
                break
            }

            val rankedSoFar = container.sourceMatcher.rank(
                media = media,
                source = source,
                candidates = candidates.distinctBy { "${it.sourceId}:${it.url}:${it.title}" },
                searchedAtEpochMillis = now,
                titleOverrides = titleOverrides,
            )
            if ((rankedSoFar.firstOrNull()?.score ?: 0.0) >= SOURCE_STRONG_MATCH_SCORE) {
                break
            }
            if (candidates.size >= SOURCE_MAX_CANDIDATES_PER_SOURCE) {
                break
            }
        }

        val distinctCandidates = candidates.distinctBy { "${it.sourceId}:${it.url}:${it.title}" }
        val searchError = lastSearchError
        if (distinctCandidates.isEmpty() && searchError != null) {
            throw searchError
        }

        val ranked = container.sourceMatcher.rank(media, source, distinctCandidates, now, titleOverrides)
        if (BuildConfig.DEBUG) {
            Log.i(
                TAG,
                "Source search ${source.name}: queries=$searchedQueries/${queries.size}, failed=$failedQueries, candidates=${distinctCandidates.size}, ranked=${ranked.size}",
            )
        }
        return ranked.ifEmpty {
            distinctCandidates.take(SOURCE_FALLBACK_CANDIDATES_PER_SOURCE).map { sourceManga ->
                SourceSearchResult(
                    mediaId = media.id,
                    source = source,
                    manga = sourceManga,
                    score = 0.0,
                    reasons = listOf("search result"),
                    searchedAtEpochMillis = now,
                )
            }
        }
    }

    suspend fun readableSourceMatch(
        media: AnilistMedia,
        source: SourceDescriptor,
        now: Long,
    ): SourceSearchResult {
        val matches = searchSourceMatches(media, source, now)
            .filter { it.isReadableMatchCandidate() }
        val verified = firstReadableMatch(source, matches, now)
            ?: error("No readable manga found on ${source.name}")
        return verified.match
    }

    suspend fun cachedVerifiedMatches(
        mediaId: Int,
        sources: List<SourceDescriptor>,
        now: Long,
    ): VerifiedSourceMatches {
        val cachedResults = container.database.sourceSearchDao().cachedSearchResults(mediaId)
            .filter { now - it.searchedAtEpochMillis <= cachePolicy.sourceSearchTtlMillis }
        val matches = mutableListOf<SourceSearchResult>()
        val counts = mutableMapOf<String, Int>()

        cachedResults.forEach { result ->
            val source = sources.sourceFor(
                sourceId = result.sourceId,
                packageName = result.sourcePackageName,
                sourceName = result.sourceName,
                sourceLang = result.sourceLang,
            )
                ?: return@forEach
            val match = result.toSearchResult(source)
            val chapters = cachedChapters(source, match.manga, now, requireFresh = false)
            if (chapters != null && chapters.isNotEmpty()) {
                matches += match
                counts[match.sourceMatchKey()] = chapters.size
            }
        }

        return VerifiedSourceMatches(
            matches = matches.distinctBy { "${it.source.packageName}:${it.source.id}:${it.manga.url}" }
                .sortedByDescending { it.score },
            chapterCounts = counts,
        )
    }

    suspend fun cacheVerifiedMatches(mediaId: Int, matches: List<SourceSearchResult>) {
        container.database.sourceSearchDao().clearForMedia(mediaId)
        if (matches.isNotEmpty()) {
            container.database.sourceSearchDao().upsertResults(matches.map { it.toEntity() })
        }
    }

    suspend fun firstReadableMatch(
        source: SourceDescriptor,
        candidates: List<SourceSearchResult>,
        now: Long,
    ): VerifiedReadableMatch? {
        candidates.take(SOURCE_CANDIDATES_TO_VERIFY).forEach { candidate ->
            verifyReadableMatch(source, candidate, now)?.let { return it }
        }
        return null
    }

    suspend fun resolveMangaDetails(match: SourceSearchResult): SourceSearchResult =
        match.copy(manga = resolveMangaDetails(match.source, match.manga))

    suspend fun resolveMangaDetails(source: SourceDescriptor, manga: SourceManga): SourceManga =
        runCatching {
            container.sourceHost.mangaDetails(source, manga)
        }.onFailure { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Manga details failed for ${source.name}/${manga.title}; using search result", error)
        }.getOrNull() ?: manga

    suspend fun cachedChapters(
        source: SourceDescriptor,
        manga: SourceManga,
        now: Long,
        requireFresh: Boolean,
    ): List<SourceChapter>? {
        val cached = container.database.chapterDao().cachedChapters(source.id, manga.url)
        if (cached.isEmpty()) return null
        val freshness = com.tankobun.core.network.CacheFreshness { now }
        if (requireFresh && cached.any { !freshness.isFresh(it.fetchedAtEpochMillis, cachePolicy.sourceChapterTtlMillis) }) return null
        return cached.map { it.toModel() }
    }

    suspend fun fetchAndCacheChapters(
        source: SourceDescriptor,
        manga: SourceManga,
        now: Long,
    ): List<SourceChapter> = fetchAndCacheUpdate(source, manga, now).chapters

    private suspend fun fetchAndCacheUpdate(
        source: SourceDescriptor,
        manga: SourceManga,
        now: Long,
        fetchDetails: Boolean = false,
    ): SourceMangaUpdate {
        val existing = container.database.chapterDao().cachedChapters(source.id, manga.url).map { it.toModel() }
        val update = container.sourceHost.mangaUpdate(source, manga, existing, fetchDetails, fetchChapters = true)
            ?: error(container.string(com.tankobun.app.R.string.source_picker_chapter_load_failed))
        container.database.withTransaction {
            container.database.chapterDao().deleteChaptersForSourceManga(source.id, manga.url)
            container.database.chapterDao().upsertChapters(update.chapters.map { it.toEntity(now).copy(mangaUrl = manga.url) })
            container.database.sourceBindingDao().updateMemo(source.id, manga.url, update.manga.memoJson)
            container.database.sourceSearchDao().updateMemo(source.id, manga.url, update.manga.memoJson)
        }
        return update
    }

    suspend fun saveSourceBinding(match: SourceSearchResult) {
        val binding = SourceBinding(
            mediaId = match.mediaId,
            sourceId = match.source.id,
            sourcePackageName = match.source.packageName,
            mangaUrl = match.manga.url,
            mangaTitle = match.manga.title,
            thumbnailUrl = match.manga.thumbnailUrl,
            memoJson = match.manga.memoJson,
            selectedAtEpochMillis = System.currentTimeMillis(),
        )
        container.database.sourceBindingDao().upsertBinding(binding.toEntity())
    }

    suspend fun loadChapters(
        source: SourceDescriptor,
        manga: SourceManga,
        mediaId: Int?,
        now: Long,
        forceRefresh: Boolean = false,
    ): LoadedSourceChapters {
        val cached = if (forceRefresh) null else cachedChapters(source, manga, now, requireFresh = true)
        val update = if (cached != null) {
            SourceMangaUpdate(manga, cached)
        } else {
            fetchAndCacheUpdate(source, manga, now, fetchDetails = true)
        }
        val chapterProgress = mediaId?.let { progressByChapter(it) }.orEmpty()
        return LoadedSourceChapters(
            manga = update.manga,
            chapters = update.chapters,
            chapterProgress = chapterProgress,
        )
    }

    private suspend fun verifyReadableMatch(
        source: SourceDescriptor,
        candidate: SourceSearchResult,
        now: Long,
    ): VerifiedReadableMatch? {
        readableChaptersForVerification(source, candidate, now)?.let { (manga, chapterCount) ->
            return VerifiedReadableMatch(candidate.copy(manga = manga), chapterCount)
        }

        val resolved = withTimeoutOrNull(SOURCE_DETAILS_TIMEOUT_MILLIS) {
            resolveMangaDetails(candidate)
        } ?: return null
        if (resolved.manga.url == candidate.manga.url && resolved.manga.title == candidate.manga.title &&
            resolved.manga.memoJson == candidate.manga.memoJson
        ) {
            return null
        }
        return readableChaptersForVerification(source, resolved, now)?.let { (manga, chapterCount) ->
            VerifiedReadableMatch(resolved.copy(manga = manga), chapterCount)
        }
    }

    private suspend fun readableChaptersForVerification(
        source: SourceDescriptor,
        match: SourceSearchResult,
        now: Long,
    ): Pair<SourceManga, Int>? {
        if (match.manga.url.isBlank()) return null
        return try {
            withTimeoutOrNull(SOURCE_CHAPTER_TIMEOUT_MILLIS) {
                val cached = cachedChapters(source, match.manga, now, requireFresh = false)
                val update = if (cached != null) SourceMangaUpdate(match.manga, cached)
                    else fetchAndCacheUpdate(source, match.manga, now)
                update.takeIf { it.chapters.isNotEmpty() }?.let { it.manga to it.chapters.size }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Log.d(TAG, "Chapter verification failed for ${source.name}/${match.manga.title}", error)
            null
        }
    }

    private fun SourceSearchResultEntity.toSearchResult(source: SourceDescriptor): SourceSearchResult =
        SourceSearchResult(
            mediaId = mediaId,
            source = source,
            manga = SourceManga(
                sourceId = sourceId,
                url = mangaUrl,
                title = mangaTitle,
                thumbnailUrl = mangaThumbnailUrl,
                memoJson = memoJson,
                description = null,
                author = null,
                artist = null,
                status = null,
            ),
            score = score,
            reasons = reasons,
            searchedAtEpochMillis = searchedAtEpochMillis,
        )

    private fun List<SourceDescriptor>.sourceFor(
        sourceId: Long,
        packageName: String,
        sourceName: String? = null,
        sourceLang: String? = null,
    ): SourceDescriptor? =
        firstOrNull { source ->
            source.id == sourceId && source.packageName == packageName
        } ?: firstOrNull { source ->
            source.packageName == packageName &&
                sourceName != null &&
                sourceLang != null &&
                source.name.equals(sourceName, ignoreCase = true) &&
                source.lang.equals(sourceLang, ignoreCase = true)
        } ?: filter { source ->
            source.packageName == packageName
        }.singleOrNull() ?: firstOrNull { source ->
            source.id == sourceId &&
                sourceName != null &&
                source.name.equals(sourceName, ignoreCase = true)
        } ?: firstOrNull { source ->
            source.id == sourceId
        }

    private companion object {
        private const val TAG = "TankobunSourceData"
        private const val SOURCE_QUERY_TIMEOUT_MILLIS = 6_000L
        private const val SOURCE_DETAILS_TIMEOUT_MILLIS = 6_000L
        private const val SOURCE_CHAPTER_TIMEOUT_MILLIS = 10_000L
        private const val SOURCE_MATCH_TIMEOUT_MILLIS = 20_000L
        private const val SOURCE_VERIFY_TIMEOUT_MILLIS = 20_000L
        private const val SOURCE_SEARCH_CONCURRENCY = 4
        private const val SOURCE_FALLBACK_CANDIDATES_PER_SOURCE = 5
        private const val SOURCE_MAX_CANDIDATES_PER_SOURCE = 40
        private const val SOURCE_STRONG_MATCH_SCORE = 0.9
    }
}
