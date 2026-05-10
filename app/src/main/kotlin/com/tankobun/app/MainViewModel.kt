package com.tankobun.app

import android.util.Log
import com.tankobun.core.anilist.AnilistGraphQlException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tankobun.core.anilist.AnilistOAuth
import com.tankobun.core.database.toEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.reader.ReaderProgressCalculator
import com.tankobun.core.reader.ReaderSession
import com.tankobun.core.sync.SyncMutationFactory
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.CachePolicy
import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceBinding
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceManga
import com.tankobun.core.model.SourceSearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID

data class TankobunUiState(
    val loggedIn: Boolean = false,
    val clientConfigured: Boolean = false,
    val themeMode: TankobunThemeMode = TankobunThemeMode.SYSTEM,
    val viewerName: String? = null,
    val library: List<AnilistMedia> = emptyList(),
    val libraryItems: List<LibraryItem> = emptyList(),
    val librarySyncedAtEpochMillis: Long = 0L,
    val libraryViewMode: MediaViewMode = MediaViewMode.COVER_GRID,
    val browseViewMode: MediaViewMode = MediaViewMode.COVER_GRID,
    val searchQuery: String = "",
    val searchResults: List<AnilistMedia> = emptyList(),
    val selectedMedia: AnilistMedia? = null,
    val allInstalledSources: List<SourceDescriptor> = emptyList(),
    val installedSources: List<SourceDescriptor> = emptyList(),
    val sourceLanguages: Set<String> = defaultSourceLanguages(),
    val extensionRepositoryUrl: String = "",
    val availableExtensions: List<ExtensionIndexEntry> = emptyList(),
    val sourceMatches: List<SourceSearchResult> = emptyList(),
    val sourceMatchChapterCounts: Map<String, Int> = emptyMap(),
    val sourcePickerOpen: Boolean = false,
    val sourcePickerLoading: Boolean = false,
    val selectedSourceManga: SourceManga? = null,
    val sourceChapters: List<SourceChapter> = emptyList(),
    val latestProgress: ReadingProgress? = null,
    val activeChapter: SourceChapter? = null,
    val readerPages: List<ReaderPage> = emptyList(),
    val currentPageIndex: Int = 0,
    val downloads: List<DownloadJob> = emptyList(),
    val selectedSourceId: Long? = null,
    val readerMode: ReaderMode = ReaderMode.PAGED,
    val busy: Boolean = false,
    val message: String? = null,
) {
    val selectedSource: SourceDescriptor?
        get() = installedSources.firstOrNull { it.id == selectedSourceId }
            ?: allInstalledSources.firstOrNull { it.id == selectedSourceId }

    val librarySections: List<LibrarySection>
        get() = libraryItems.toLibrarySections()
}

data class LibraryItem(
    val media: AnilistMedia,
    val entry: AnilistListEntry,
)

data class LibrarySection(
    val key: String,
    val title: String,
    val items: List<LibraryItem>,
)

private data class VerifiedSourceMatches(
    val matches: List<SourceSearchResult>,
    val chapterCounts: Map<String, Int>,
)

class MainViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val progressCalculator = ReaderProgressCalculator()
    private val syncMutationFactory = SyncMutationFactory()
    private val cachePolicy = CachePolicy()
    private val _state = MutableStateFlow(
        TankobunUiState(
            loggedIn = container.tokenStore.accessToken() != null,
            clientConfigured = BuildConfig.ANILIST_CLIENT_ID.isNotBlank(),
            viewerName = container.settingsStore.viewerName(),
            librarySyncedAtEpochMillis = container.settingsStore.librarySyncedAtEpochMillis(),
            libraryViewMode = container.settingsStore.libraryViewMode(),
            browseViewMode = container.settingsStore.browseViewMode(),
            sourceLanguages = container.settingsStore.sourceLanguages(),
            extensionRepositoryUrl = container.settingsStore.extensionRepositoryUrl(),
            themeMode = container.settingsStore.themeMode(),
        ),
    )
    val state: StateFlow<TankobunUiState> = _state

    init {
        refreshInstalledSources()
        if (_state.value.loggedIn) {
            loadCachedLibrary(syncIfEmpty = true)
        }
    }

    fun loginUrl(): String? {
        if (BuildConfig.ANILIST_CLIENT_ID.isBlank()) return null
        return AnilistOAuth.authorizationUrl(
            clientId = BuildConfig.ANILIST_CLIENT_ID,
            redirectUri = BuildConfig.ANILIST_REDIRECT_URI,
        )
    }

    fun handleOAuthRedirect(uri: String) {
        val token = AnilistOAuth.parseRedirect(uri) ?: return
        container.tokenStore.saveAccessToken(token.accessToken)
        _state.update { it.copy(loggedIn = true, message = "AniList connected") }
        refreshLibrary()
    }

    fun signOut() {
        container.tokenStore.clear()
        container.settingsStore.saveViewerName(null)
        container.settingsStore.saveLibrarySyncedAtEpochMillis(0L)
        _state.update {
            it.copy(
                loggedIn = false,
                viewerName = null,
                library = emptyList(),
                libraryItems = emptyList(),
                librarySyncedAtEpochMillis = 0L,
                message = "Signed out",
            )
        }
    }

    fun refreshInstalledSources() {
        viewModelScope.launch {
            val packages = container.extensionScanner.installedExtensions()
            container.sourceHost.retainInstalledPackages(packages.map { it.packageName }.toSet())
            val discoveredSources = packages.flatMap { descriptor ->
                runCatching {
                    container.sourceHost.loadSources(descriptor.packageName).map { source ->
                        descriptor.copy(
                            id = source.id,
                            name = source.name,
                            lang = source.lang,
                        )
                    }
                }.getOrDefault(emptyList()).ifEmpty { listOf(descriptor) }
            }
            val allSources = discoveredSources.visibleSources()
            val sources = allSources.preferredVisibleSources(_state.value.sourceLanguages)
            val selectedSourceId = _state.value.selectedSourceId
            _state.update {
                it.copy(
                    allInstalledSources = allSources,
                    installedSources = sources,
                    selectedSourceId = selectedSourceId
                        ?.takeIf { current -> sources.any { source -> source.id == current } }
                        ?: sources.firstOrNull()?.id,
                )
            }
        }
    }

    fun setExtensionRepositoryUrl(url: String) {
        container.settingsStore.saveExtensionRepositoryUrl(url)
        _state.update { it.copy(extensionRepositoryUrl = url) }
    }

    fun setThemeMode(mode: TankobunThemeMode) {
        container.settingsStore.saveThemeMode(mode)
        _state.update { it.copy(themeMode = mode) }
    }

    fun setLibraryViewMode(mode: MediaViewMode) {
        container.settingsStore.saveLibraryViewMode(mode)
        _state.update { it.copy(libraryViewMode = mode) }
    }

    fun setBrowseViewMode(mode: MediaViewMode) {
        container.settingsStore.saveBrowseViewMode(mode)
        _state.update { it.copy(browseViewMode = mode) }
    }

    fun setSourceLanguageEnabled(language: String, enabled: Boolean) {
        val normalized = language.trim().lowercase(Locale.ROOT).replace('_', '-')
        val selectedLanguages = if (enabled) {
            _state.value.sourceLanguages + normalized
        } else {
            _state.value.sourceLanguages - normalized
        }
        val next = selectedLanguages.ifEmpty { defaultSourceLanguages() }
        container.settingsStore.saveSourceLanguages(next)
        _state.update {
            val sources = it.allInstalledSources.preferredVisibleSources(next)
            it.copy(
                sourceLanguages = next,
                installedSources = sources,
                selectedSourceId = it.selectedSourceId
                    ?.takeIf { current -> sources.any { source -> source.id == current } }
                    ?: sources.firstOrNull()?.id,
            )
        }
    }

    fun refreshExtensionIndex() {
        val repositoryUrl = _state.value.extensionRepositoryUrl.trim()
        if (repositoryUrl.isBlank()) {
            _state.update { it.copy(message = "Paste an extension repository index URL first") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                container.extensionRepository.fetchIndex(repositoryUrl)
            }.onSuccess { extensions ->
                _state.update {
                    it.copy(
                        availableExtensions = extensions,
                        busy = false,
                        message = "Loaded ${extensions.size} extensions",
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(busy = false, message = error.message ?: "Extension index failed")
                }
            }
        }
    }

    fun extensionApkUrl(entry: ExtensionIndexEntry): String =
        container.extensionRepository.apkUrl(_state.value.extensionRepositoryUrl.trim(), entry)

    private fun loadCachedLibrary(syncIfEmpty: Boolean = false) {
        viewModelScope.launch {
            val media = container.database.mediaDao().cachedMedia().associateBy { it.id }
            val entries = container.database.listEntryDao().cachedEntries()
            val items = entries.mapNotNull { entry ->
                media[entry.mediaId]?.toModel()?.let { cachedMedia ->
                    LibraryItem(cachedMedia, entry.toModel())
                }
            }.distinctBy { it.media.id }
                .sortedBy { it.media.title.userPreferred.lowercase(Locale.ROOT) }

            if (items.isNotEmpty()) {
                _state.update {
                    it.copy(
                        library = items.map { item -> item.media },
                        libraryItems = items,
                        librarySyncedAtEpochMillis = container.settingsStore.librarySyncedAtEpochMillis(),
                    )
                }
            } else if (syncIfEmpty) {
                refreshLibrary()
            }
        }
    }

    fun refreshLibrary() {
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = "Connect AniList to sync your library") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                val viewer = container.anilistRepository.viewer(token)
                val entries = container.anilistRepository.mangaList(token, userId = viewer.id)
                val now = System.currentTimeMillis()
                container.database.mediaDao().upsertMedia(entries.map { it.first.toEntity(now) })
                container.database.listEntryDao().upsertEntries(entries.map { it.second.toEntity(now) })
                val entryIds = entries.map { it.second.id }
                if (entryIds.isEmpty()) {
                    container.database.listEntryDao().deleteAllEntries()
                } else {
                    container.database.listEntryDao().deleteEntriesNotIn(entryIds)
                }
                container.settingsStore.saveViewerName(viewer.name)
                container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
                _state.update {
                    it.copy(
                        viewerName = viewer.name,
                        library = entries.map { pair -> pair.first },
                        libraryItems = entries.map { (media, entry) -> LibraryItem(media, entry) },
                        librarySyncedAtEpochMillis = now,
                        busy = false,
                        message = "Library synced",
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList library sync failed", error)
                _state.update {
                    it.copy(busy = false, message = error.userMessage("Library sync failed"))
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun searchAniList() {
        val query = _state.value.searchQuery.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                container.anilistRepository.searchManga(query)
            }.onSuccess { results ->
                _state.update { it.copy(searchResults = results, busy = false) }
            }.onFailure { error ->
                _state.update { it.copy(busy = false, message = error.message ?: "Search failed") }
            }
        }
    }

    fun selectMedia(media: AnilistMedia) {
        _state.update {
            it.copy(
                selectedMedia = media,
                sourceMatches = emptyList(),
                sourceMatchChapterCounts = emptyMap(),
                sourcePickerOpen = false,
                sourcePickerLoading = false,
                selectedSourceManga = null,
                sourceChapters = emptyList(),
                latestProgress = null,
                activeChapter = null,
                readerPages = emptyList(),
                currentPageIndex = 0,
                message = null,
            )
        }
        loadCachedSourceState(media.id)
    }

    fun selectSource(sourceId: Long) {
        _state.update {
            val match = it.sourceMatches.firstOrNull { match -> match.source.id == sourceId }
            val sameSource = it.selectedSourceId == sourceId
            it.copy(
                selectedSourceId = sourceId,
                selectedSourceManga = match?.manga ?: it.selectedSourceManga?.takeIf { manga ->
                    sameSource && manga.sourceId == sourceId
                },
                sourceChapters = it.sourceChapters.takeIf { sameSource }.orEmpty(),
                activeChapter = null,
                readerPages = emptyList(),
                currentPageIndex = 0,
            )
        }
    }

    fun clearSelectedMedia() {
        _state.update {
            it.copy(
                selectedMedia = null,
                sourceMatches = emptyList(),
                sourceMatchChapterCounts = emptyMap(),
                sourcePickerOpen = false,
                sourcePickerLoading = false,
                selectedSourceManga = null,
                sourceChapters = emptyList(),
                latestProgress = null,
                activeChapter = null,
                readerPages = emptyList(),
                currentPageIndex = 0,
                message = null,
            )
        }
    }

    fun setReaderMode(mode: ReaderMode) {
        _state.update { it.copy(readerMode = mode) }
    }

    private fun loadCachedSourceState(mediaId: Int) {
        viewModelScope.launch {
            val binding = container.database.sourceBindingDao().bindingForMedia(mediaId)
            val cachedResults = container.database.sourceSearchDao().cachedSearchResults(mediaId)
            val sources = _state.value.allInstalledSources.ifEmpty { _state.value.installedSources }
            val cachedMatches = cachedResults.mapNotNull { result ->
                val source = sources.firstOrNull { it.id == result.sourceId || it.packageName == result.sourcePackageName }
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
                SourceSearchResult(
                    mediaId = result.mediaId,
                    source = source,
                    manga = SourceManga(
                        sourceId = result.sourceId,
                        url = result.mangaUrl,
                        title = result.mangaTitle,
                        thumbnailUrl = result.mangaThumbnailUrl,
                        description = null,
                        author = null,
                        artist = null,
                        status = null,
                    ),
                    score = result.score,
                    reasons = result.reasons,
                    searchedAtEpochMillis = result.searchedAtEpochMillis,
                )
            }.distinctBy { "${it.source.id}:${it.manga.url}:${it.manga.title}" }
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

            val boundSource = binding?.let { cached ->
                sources.firstOrNull { it.id == cached.sourceId || it.packageName == cached.sourcePackageName }
            }
            val boundManga = binding?.let { cached ->
                SourceManga(
                    sourceId = cached.sourceId,
                    url = cached.mangaUrl,
                    title = cached.mangaTitle,
                    thumbnailUrl = cached.thumbnailUrl,
                    description = null,
                    author = null,
                    artist = null,
                    status = null,
                )
            }
            val chapters = if (boundSource != null && boundManga != null) {
                container.database.chapterDao()
                    .cachedChapters(boundSource.id, boundManga.url)
                    .map { it.toModel() }
            } else {
                emptyList()
            }
            val latestProgress = container.database.progressDao().latestProgress(mediaId)?.toModel()
            val visibleMatches = matches.toMutableList()
            if (boundSource != null && boundManga != null && chapters.isNotEmpty()) {
                val selectedMatch = SourceSearchResult(
                    mediaId = mediaId,
                    source = boundSource,
                    manga = boundManga,
                    score = 1.0,
                    reasons = listOf("selected source"),
                    searchedAtEpochMillis = binding.selectedAtEpochMillis,
                )
                if (visibleMatches.none { it.source.id == selectedMatch.source.id && it.manga.url == selectedMatch.manga.url }) {
                    visibleMatches.add(0, selectedMatch)
                }
                chapterCounts[selectedMatch.sourceMatchKey()] = chapters.size
            }

            _state.update {
                if (it.selectedMedia?.id != mediaId) {
                    it
                } else {
                    it.copy(
                        sourceMatches = visibleMatches,
                        sourceMatchChapterCounts = chapterCounts,
                        selectedSourceId = boundSource?.id ?: it.selectedSourceId,
                        selectedSourceManga = boundManga,
                        sourceChapters = chapters,
                        latestProgress = latestProgress,
                    )
                }
            }
        }
    }

    fun openSourcePicker() {
        _state.value.selectedMedia ?: return
        val sources = sourcePickerSources()
        _state.update { it.copy(sourcePickerOpen = true, message = null) }
        if (sources.isEmpty()) {
            _state.update { it.copy(message = "Install a source extension first") }
            return
        }
        if (!_state.value.sourcePickerLoading) {
            findSourceMatches()
        }
    }

    fun closeSourcePicker() {
        _state.update { it.copy(sourcePickerOpen = false, sourcePickerLoading = false) }
    }

    fun bindSelectedSource() {
        val media = _state.value.selectedMedia ?: return
        val source = _state.value.selectedSource ?: run {
            _state.update { it.copy(message = "Install a source extension first") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                val now = System.currentTimeMillis()
                val matches = searchSourceMatches(media, source, now)
                val match = matches.firstOrNull()
                    ?: error("No manga found on ${source.name}")
                val resolved = match.withResolvedManga()
                saveSourceBinding(resolved)
                resolved
            }.onSuccess { match ->
                _state.update {
                    it.copy(
                        sourceMatches = (listOf(match) + it.sourceMatches).distinctBy { result ->
                            "${result.source.id}:${result.manga.url}"
                        },
                        selectedSourceId = match.source.id,
                        selectedSourceManga = match.manga,
                        message = "Source selected for ${match.manga.title}",
                    )
                }
                loadChapters(match.source, match.manga)
            }.onFailure { error ->
                Log.w(TAG, "Selected source binding failed for ${source.name}", error)
                _state.update { it.copy(busy = false, message = error.message ?: "Source selection failed") }
            }
        }
    }

    private suspend fun searchSourceMatches(
        media: AnilistMedia,
        source: SourceDescriptor,
        now: Long,
    ): List<SourceSearchResult> {
        val queries = sourceSearchQueries(media)
        val candidates = queries
            .flatMap { query ->
                runCatching {
                    container.sourceHost.search(source, query)
                }.onFailure { error ->
                    Log.w(TAG, "Source search failed for ${source.name} with '$query'", error)
                }.getOrDefault(emptyList())
            }
            .distinctBy { "${it.sourceId}:${it.url}:${it.title}" }

        val ranked = container.sourceMatcher.rank(media, source, candidates, now)
        Log.i(
            TAG,
            "Source search ${source.name}: queries=${queries.size}, candidates=${candidates.size}, ranked=${ranked.size}",
        )
        return ranked.ifEmpty {
            candidates.take(3).map { sourceManga ->
                        SourceSearchResult(
                            mediaId = media.id,
                            source = source,
                            manga = sourceManga,
                            score = 0.0,
                            reasons = listOf("manual source"),
                            searchedAtEpochMillis = now,
                        )
                    }
        }
    }

    private fun sourceSearchQueries(media: AnilistMedia): List<String> {
        val rawTitles = buildList {
            add(media.title.userPreferred)
            media.title.romaji?.let(::add)
            media.title.english?.let(::add)
            media.title.native?.let(::add)
            addAll(media.synonyms)
        }

        return rawTitles
            .flatMap { title ->
                listOf(
                    title,
                    title.replace(Regex("<[^>]*>"), ""),
                    title.replace(Regex("\\bNo\\.?(\\d)", RegexOption.IGNORE_CASE), "No. $1"),
                    title.replace(Regex("[^\\p{L}\\p{N}\\s]+"), " "),
                    title.replace(Regex("\\bNo\\.?\\s*", RegexOption.IGNORE_CASE), ""),
                )
            }
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.length >= 2 }
            .distinctBy { it.lowercase() }
            .take(8)
        }

    fun findSourceMatches(forceRefresh: Boolean = false) {
        val media = _state.value.selectedMedia ?: return
        val sources = sourcePickerSources()
        if (sources.isEmpty()) {
            _state.update { it.copy(message = "Install a source extension first") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, sourcePickerLoading = true, message = null) }
            runCatching {
                val now = System.currentTimeMillis()
                cachedVerifiedMatches(media.id, sources, now)
                    .takeIf { !forceRefresh && it.matches.isNotEmpty() }
                    ?: searchVerifiedMatches(media, sources, now).also { verified ->
                        container.database.sourceSearchDao().clearForMedia(media.id)
                        container.database.sourceSearchDao().upsertResults(verified.matches.map { it.toEntity() })
                    }
            }.onSuccess { verified ->
                _state.update {
                    val selectedMatches = it.sourceMatches.filter { match ->
                        it.selectedSourceId == match.source.id &&
                            it.selectedSourceManga?.url == match.manga.url
                    }
                    val nextMatches = (selectedMatches + verified.matches)
                        .distinctBy { match -> "${match.source.id}:${match.manga.url}" }
                        .sortedByDescending { match -> match.score }
                    it.copy(
                        sourceMatches = nextMatches,
                        sourceMatchChapterCounts = it.sourceMatchChapterCounts + verified.chapterCounts,
                        busy = false,
                        sourcePickerLoading = false,
                        message = if (nextMatches.isEmpty()) {
                            "No sources with chapters found"
                        } else {
                            "Found ${nextMatches.size} readable sources"
                        },
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        busy = false,
                        sourcePickerLoading = false,
                        message = error.message ?: "Source search failed",
                    )
                }
            }
        }
    }

    private fun sourcePickerSources(): List<SourceDescriptor> =
        _state.value.allInstalledSources
            .ifEmpty { _state.value.installedSources }
            .distinctBy { "${it.packageName}:${it.id}" }

    private suspend fun cachedVerifiedMatches(
        mediaId: Int,
        sources: List<SourceDescriptor>,
        now: Long,
    ): VerifiedSourceMatches {
        val cachedResults = container.database.sourceSearchDao().cachedSearchResults(mediaId)
            .filter { now - it.searchedAtEpochMillis <= cachePolicy.sourceSearchTtlMillis }
        val matches = mutableListOf<SourceSearchResult>()
        val counts = mutableMapOf<String, Int>()

        cachedResults.forEach { result ->
            val source = sources.firstOrNull { it.id == result.sourceId || it.packageName == result.sourcePackageName }
                ?: return@forEach
            val match = SourceSearchResult(
                mediaId = result.mediaId,
                source = source,
                manga = SourceManga(
                    sourceId = result.sourceId,
                    url = result.mangaUrl,
                    title = result.mangaTitle,
                    thumbnailUrl = result.mangaThumbnailUrl,
                    description = null,
                    author = null,
                    artist = null,
                    status = null,
                ),
                score = result.score,
                reasons = result.reasons,
                searchedAtEpochMillis = result.searchedAtEpochMillis,
            )
            val chapters = cachedChapters(source, match.manga, now, requireFresh = true)
            if (chapters != null && chapters.isNotEmpty()) {
                matches += match
                counts[match.sourceMatchKey()] = chapters.size
            }
        }

        return VerifiedSourceMatches(
            matches = matches.distinctBy { "${it.source.id}:${it.manga.url}" }.sortedByDescending { it.score },
            chapterCounts = counts,
        )
    }

    private suspend fun searchVerifiedMatches(
        media: AnilistMedia,
        sources: List<SourceDescriptor>,
        now: Long,
    ): VerifiedSourceMatches {
        val matches = mutableListOf<SourceSearchResult>()
        val counts = mutableMapOf<String, Int>()

        sources.forEach { source ->
            val candidates = runCatching {
                withTimeoutOrNull(SOURCE_MATCH_TIMEOUT_MILLIS) {
                    searchSourceMatches(media, source, now).take(4)
                }.orEmpty()
            }.onFailure { error ->
                Log.w(TAG, "Source search failed for ${source.name}", error)
            }.getOrDefault(emptyList())

            for (candidate in candidates) {
                val resolved = candidate.withResolvedManga()
                val chapters = withTimeoutOrNull(SOURCE_MATCH_TIMEOUT_MILLIS) {
                    cachedChapters(source, resolved.manga, now, requireFresh = true)
                        ?: fetchAndCacheChapters(source, resolved.manga, now)
                }.orEmpty()
                if (chapters.isNotEmpty()) {
                    matches += resolved
                    counts[resolved.sourceMatchKey()] = chapters.size
                    publishSourcePickerMatch(media.id, resolved, chapters.size)
                    break
                }
            }
        }

        return VerifiedSourceMatches(
            matches = matches.distinctBy { "${it.source.id}:${it.manga.url}" }.sortedByDescending { it.score },
            chapterCounts = counts,
        )
    }

    private fun publishSourcePickerMatch(mediaId: Int, match: SourceSearchResult, chapterCount: Int) {
        _state.update {
            if (it.selectedMedia?.id != mediaId) {
                it
            } else {
                val nextMatches = (it.sourceMatches + match)
                    .distinctBy { result -> "${result.source.id}:${result.manga.url}" }
                    .sortedByDescending { result -> result.score }
                it.copy(
                    sourceMatches = nextMatches,
                    sourceMatchChapterCounts = it.sourceMatchChapterCounts + (match.sourceMatchKey() to chapterCount),
                    message = "Found ${nextMatches.size} readable sources",
                )
            }
        }
    }

    private suspend fun cachedChapters(
        source: SourceDescriptor,
        manga: SourceManga,
        now: Long,
        requireFresh: Boolean,
    ): List<SourceChapter>? {
        val cached = container.database.chapterDao().cachedChapters(source.id, manga.url)
        if (cached.isEmpty()) return null
        val freshest = cached.maxOf { it.fetchedAtEpochMillis }
        if (requireFresh && now - freshest > cachePolicy.sourceChapterTtlMillis) return null
        return cached.map { it.toModel() }
    }

    private suspend fun fetchAndCacheChapters(
        source: SourceDescriptor,
        manga: SourceManga,
        now: Long,
    ): List<SourceChapter> {
        val chapters = container.sourceHost.chapters(source, manga)
        container.database.chapterDao().upsertChapters(chapters.map { it.toEntity(now) })
        return chapters
    }

    fun bindSourceMatch(match: SourceSearchResult) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, sourcePickerLoading = true, message = null) }
            runCatching {
                val resolved = match.withResolvedManga()
                saveSourceBinding(resolved)
                resolved
            }.onSuccess { resolved ->
                _state.update {
                    it.copy(
                        selectedSourceId = resolved.source.id,
                        selectedSourceManga = resolved.manga,
                        sourcePickerOpen = false,
                        sourcePickerLoading = false,
                        message = "Source selected for ${resolved.manga.title}",
                    )
                }
                loadChapters(resolved.source, resolved.manga)
            }.onFailure { error ->
                Log.w(TAG, "Source binding failed for ${match.source.name}/${match.manga.title}", error)
                _state.update {
                    it.copy(
                        busy = false,
                        sourcePickerLoading = false,
                        message = error.message ?: "Source selection failed",
                    )
                }
            }
        }
    }

    private suspend fun SourceSearchResult.withResolvedManga(): SourceSearchResult {
        val resolved = runCatching {
            container.sourceHost.mangaDetails(source, manga)
        }.onFailure { error ->
            Log.w(TAG, "Manga details failed for ${source.name}/${manga.title}; using search result", error)
        }.getOrNull()

        return if (resolved == null) this else copy(manga = resolved)
    }

    fun loadChaptersForCurrentMatch() {
        val state = _state.value
        val selectedSourceId = state.selectedSourceId
        val selectedManga = state.selectedSourceManga
        val match = state.sourceMatches.firstOrNull { result ->
            result.source.id == selectedSourceId &&
                selectedManga != null &&
                result.manga.sourceId == selectedManga.sourceId &&
                result.manga.url == selectedManga.url
        } ?: state.sourceMatches.firstOrNull { result ->
            selectedManga != null &&
                result.manga.sourceId == selectedManga.sourceId &&
                result.manga.url == selectedManga.url
        } ?: state.sourceMatches.firstOrNull { it.source.id == selectedSourceId }
        val manga = match?.manga ?: selectedManga?.takeIf { manga ->
            selectedSourceId == null || manga.sourceId == selectedSourceId
        }
        val source = match?.source
            ?: manga?.let { selected ->
                state.installedSources.firstOrNull { it.id == selected.sourceId }
                    ?: state.allInstalledSources.firstOrNull { it.id == selected.sourceId }
            }
            ?: state.selectedSource
        if (source == null || manga == null) {
            _state.update { it.copy(message = "Find and choose a source match first") }
            return
        }
        loadChapters(source, manga)
    }

    private suspend fun saveSourceBinding(match: SourceSearchResult) {
        val binding = SourceBinding(
            mediaId = match.mediaId,
            sourceId = match.source.id,
            sourcePackageName = match.source.packageName,
            mangaUrl = match.manga.url,
            mangaTitle = match.manga.title,
            thumbnailUrl = match.manga.thumbnailUrl,
            selectedAtEpochMillis = System.currentTimeMillis(),
        )
        container.database.sourceBindingDao().upsertBinding(binding.toEntity())
    }

    private fun loadChapters(source: SourceDescriptor, manga: com.tankobun.core.model.SourceManga) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                val now = System.currentTimeMillis()
                val detailedManga = runCatching {
                    container.sourceHost.mangaDetails(source, manga)
                }.onFailure { error ->
                    Log.w(TAG, "Chapter details prefetch failed for ${source.name}/${manga.title}", error)
                }.getOrNull() ?: manga
                val chapters = cachedChapters(source, detailedManga, now, requireFresh = false)
                    ?: fetchAndCacheChapters(source, detailedManga, now)
                detailedManga to chapters
            }.onSuccess { (detailedManga, chapters) ->
                Log.i(TAG, "Chapter load ${source.name}/${detailedManga.title}: chapters=${chapters.size}")
                _state.update {
                    it.copy(
                        selectedSourceManga = detailedManga,
                        sourceChapters = chapters,
                        busy = false,
                        sourceMatchChapterCounts = it.sourceMatchChapterCounts + (sourceMatchKey(source.id, detailedManga.url) to chapters.size),
                        message = if (chapters.isEmpty()) "No chapters found" else "Loaded ${chapters.size} chapters",
                    )
                }
            }.onFailure { error ->
                Log.w(TAG, "Chapter load failed for ${source.name}/${manga.title}", error)
                _state.update { it.copy(busy = false, message = error.message ?: "Chapter load failed") }
            }
        }
    }

    fun openChapter(chapter: SourceChapter) {
        val state = _state.value
        val media = state.selectedMedia ?: return
        val source = state.installedSources.firstOrNull { it.id == chapter.sourceId }
            ?: state.allInstalledSources.firstOrNull { it.id == chapter.sourceId }
            ?: state.selectedSource?.takeIf { it.id == chapter.sourceId }
            ?: run {
                _state.update { it.copy(message = "Source is not installed") }
                return
            }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                container.sourceHost.pages(source, chapter)
            }.onSuccess { pages ->
                val savedProgress = container.database.progressDao().progressForChapter(media.id, chapter.url)
                val startPageIndex = savedProgress?.pageIndex?.coerceIn(0, pages.lastIndex.coerceAtLeast(0)) ?: 0
                Log.i(TAG, "Page load ${source.name}/${chapter.name}: pages=${pages.size}")
                _state.update {
                    it.copy(
                        activeChapter = chapter,
                        readerPages = pages,
                        currentPageIndex = startPageIndex,
                        readerMode = savedProgress?.readerMode ?: it.readerMode,
                        busy = false,
                        message = if (pages.isEmpty()) "No pages found" else "Opened ${chapter.name}",
                    )
                }
                saveReaderProgress()
            }.onFailure { error ->
                Log.w(TAG, "Page load failed for ${source.name}/${chapter.name}", error)
                _state.update { it.copy(busy = false, message = error.message ?: "Reader failed") }
            }
        }
    }

    fun closeReader() {
        saveReaderProgress()
        _state.update {
            it.copy(
                activeChapter = null,
                readerPages = emptyList(),
                currentPageIndex = 0,
            )
        }
    }

    fun moveReaderPage(delta: Int) {
        val pages = _state.value.readerPages
        if (pages.isEmpty()) return
        _state.update {
            it.copy(currentPageIndex = (it.currentPageIndex + delta).coerceIn(0, pages.lastIndex))
        }
        saveReaderProgress()
    }

    fun enqueueDownload(chapter: SourceChapter) {
        val media = _state.value.selectedMedia ?: return
        val now = System.currentTimeMillis()
        val job = DownloadJob(
            id = UUID.randomUUID().toString(),
            mediaId = media.id,
            sourceId = chapter.sourceId,
            mangaUrl = chapter.mangaUrl,
            chapterUrl = chapter.url,
            chapterName = chapter.name,
            state = DownloadState.QUEUED,
            pageCount = 0,
            completedPages = 0,
            retryCount = 0,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        viewModelScope.launch {
            container.database.downloadDao().upsertDownload(job.toEntity())
            _state.update { it.copy(downloads = it.downloads + job, message = "Queued ${chapter.name}") }
        }
    }

    private fun saveReaderProgress() {
        val media = _state.value.selectedMedia ?: return
        val chapter = _state.value.activeChapter ?: return
        val pages = _state.value.readerPages
        if (pages.isEmpty()) return
        val session = ReaderSession(
            mediaId = media.id,
            chapter = chapter,
            pages = pages,
            mode = _state.value.readerMode,
            currentPageIndex = _state.value.currentPageIndex,
        )
        val progress = progressCalculator.progressFor(session, System.currentTimeMillis())
        viewModelScope.launch {
            container.database.progressDao().upsertProgress(progress.toEntity())
            _state.update {
                if (it.selectedMedia?.id == media.id) it.copy(latestProgress = progress) else it
            }
            if (progress.completed && chapter.chapterNumber > 0) {
                val mutation = syncMutationFactory.saveMediaListEntry(
                    mediaId = media.id,
                    progress = chapter.chapterNumber.toInt(),
                    nowMillis = System.currentTimeMillis(),
                )
                container.database.syncMutationDao().upsertMutation(mutation.toEntity())
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(container) as T
        }
    }

    companion object {
        private const val TAG = "TankobunMain"
        private const val SOURCE_MATCH_TIMEOUT_MILLIS = 20_000L
    }
}

private fun Throwable.userMessage(fallback: String): String = when (this) {
    is AnilistGraphQlException -> when (statusCode) {
        401 -> "AniList session expired. Sign in again."
        429 -> "AniList is rate limiting requests. Try again in a minute."
        500 -> "AniList returned a server error while syncing. Try again in a moment."
        else -> "AniList request failed${statusCode?.let { " ($it)" }.orEmpty()}."
    }
    else -> message ?: fallback
}

private fun SourceSearchResult.sourceMatchKey(): String =
    sourceMatchKey(source.id, manga.url)

private fun sourceMatchKey(sourceId: Long, mangaUrl: String): String =
    "$sourceId:$mangaUrl"

private fun List<LibraryItem>.toLibrarySections(): List<LibrarySection> {
    val statusSections = listOf(
        MediaStatus.CURRENT to "Reading",
        MediaStatus.PLANNING to "Plan to Read",
        MediaStatus.COMPLETED to "Completed",
        MediaStatus.PAUSED to "Paused",
        MediaStatus.DROPPED to "Dropped",
        MediaStatus.REPEATING to "Rereading",
        MediaStatus.UNKNOWN to "Other",
    ).mapNotNull { (status, title) ->
        val items = filter { it.entry.status == status }
        if (items.isEmpty()) null else LibrarySection(status.name, title, items)
    }

    val customSections = flatMap { item ->
        item.entry.customLists.map { customList -> customList to item }
    }
        .groupBy({ it.first }, { it.second })
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        .map { (name, items) -> LibrarySection("custom:$name", name, items.distinctBy { it.media.id }) }

    return statusSections + customSections
}

private fun List<SourceDescriptor>.visibleSources(): List<SourceDescriptor> =
    distinctBy { "${it.packageName}:${it.id}:${it.name}:${it.lang}" }
        .sortedWith(compareBy<SourceDescriptor> { it.normalizedLanguage() }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.lang })

private fun List<SourceDescriptor>.preferredVisibleSources(preferredLanguages: Set<String>): List<SourceDescriptor> {
    val preferredSources = filter { it.normalizedLanguage() in preferredLanguages }
    return (preferredSources.ifEmpty { this })
        .distinctBy { "${it.packageName}:${it.id}:${it.name}:${it.lang}" }
        .sortedWith(compareBy<SourceDescriptor> { it.languageSortPriority(preferredLanguages) }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.lang })
}

private fun SourceDescriptor.languageSortPriority(preferredLanguages: Set<String>): Int =
    when (normalizedLanguage()) {
        "en" -> 0
        Locale.getDefault().language.lowercase(Locale.ROOT) -> 1
        Locale.getDefault().toLanguageTag().lowercase(Locale.ROOT) -> 1
        "all" -> 2
        else -> if (normalizedLanguage() in preferredLanguages) 3 else 4
    }

private fun SourceDescriptor.normalizedLanguage(): String =
    lang.lowercase(Locale.ROOT).replace('_', '-')
