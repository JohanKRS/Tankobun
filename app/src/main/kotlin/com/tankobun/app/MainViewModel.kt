package com.tankobun.app

import android.net.Uri
import android.util.Log
import com.tankobun.core.anilist.AnilistGraphQlException
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tankobun.core.anilist.AnilistOAuth
import com.tankobun.core.database.toEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.database.AnilistSearchResultEntity
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.AnilistRecommendation
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import java.io.File
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
    val browseSearched: Boolean = false,
    val browseGenres: Set<String> = emptySet(),
    val browseTags: Set<String> = emptySet(),
    val browseAvailableTags: List<AnilistMediaTag> = emptyList(),
    val browseFormat: String? = null,
    val browsePublishingStatus: String? = null,
    val browseCountryOfOrigin: String? = null,
    val browseYear: Int? = null,
    val browseSort: String = BROWSE_SORT_SEARCH_MATCH,
    val browseTrending: List<AnilistMedia> = emptyList(),
    val browsePopular: List<AnilistMedia> = emptyList(),
    val browsePopularManhwa: List<AnilistMedia> = emptyList(),
    val browseTopManga: List<AnilistMedia> = emptyList(),
    val browseLandingLoaded: Boolean = false,
    val selectedMedia: AnilistMedia? = null,
    val selectedListEntry: AnilistListEntry? = null,
    val selectedRecommendations: List<AnilistRecommendation> = emptyList(),
    val selectedRecommendationsPage: Int = 0,
    val selectedRecommendationsHasMore: Boolean = false,
    val recommendationsLoading: Boolean = false,
    val trackingStatus: MediaStatus = MediaStatus.PLANNING,
    val trackingProgress: String = "0",
    val trackingScore: String = "",
    val trackingNotes: String = "",
    val trackingPrivate: Boolean = false,
    val trackingCustomLists: String = "",
    val allInstalledSources: List<SourceDescriptor> = emptyList(),
    val installedSources: List<SourceDescriptor> = emptyList(),
    val sourceLanguages: Set<String> = defaultSourceLanguages(),
    val disabledSourceKeys: Set<String> = emptySet(),
    val extensionRepositoryUrl: String = "",
    val availableExtensions: List<ExtensionIndexEntry> = emptyList(),
    val installingExtensionPackageName: String? = null,
    val extensionInstallRequest: ExtensionInstallRequest? = null,
    val sourceMatches: List<SourceSearchResult> = emptyList(),
    val sourceMatchChapterCounts: Map<String, Int> = emptyMap(),
    val sourcePickerOpen: Boolean = false,
    val sourcePickerLoading: Boolean = false,
    val sourcePickerMessage: String? = null,
    val sourcePickerDiagnostics: List<String> = emptyList(),
    val selectedSourceManga: SourceManga? = null,
    val sourceChapters: List<SourceChapter> = emptyList(),
    val latestProgress: ReadingProgress? = null,
    val activeChapter: SourceChapter? = null,
    val readerPages: List<ReaderPage> = emptyList(),
    val currentPageIndex: Int = 0,
    val downloads: List<DownloadJob> = emptyList(),
    val selectedSourceId: Long? = null,
    val readerMode: ReaderMode = ReaderMode.PAGED,
    val readerPageGapLevel: Int = 0,
    val readerFitWidth: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
) {
    val selectedSource: SourceDescriptor?
        get() = installedSources.firstOrNull { it.id == selectedSourceId }
            ?: allInstalledSources.firstOrNull { it.id == selectedSourceId }

    val librarySections: List<LibrarySection>
        get() = libraryItems.toLibrarySections()
}

data class ExtensionInstallRequest(
    val packageName: String,
    val name: String,
    val apkUri: String,
    val expectedVersionCode: Int,
    val expectedVersionName: String,
)

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

private data class VerifiedReadableMatch(
    val match: SourceSearchResult,
    val chapterCount: Int,
)

private class SourceQueryTimeoutException(query: String) : RuntimeException("Search timed out for '$query'")

private data class BrowseLandingData(
    val trending: List<AnilistMedia>,
    val popular: List<AnilistMedia>,
    val popularManhwa: List<AnilistMedia>,
    val topManga: List<AnilistMedia>,
)

private data class InstalledExtensionVersion(
    val versionCode: Int,
    val versionName: String,
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
            browseAvailableTags = container.settingsStore.anilistTags(),
            sourceLanguages = container.settingsStore.sourceLanguages(),
            disabledSourceKeys = container.settingsStore.disabledSourceKeys(),
            extensionRepositoryUrl = container.settingsStore.extensionRepositoryUrl(),
            themeMode = container.settingsStore.themeMode(),
            readerMode = container.settingsStore.readerMode(),
            readerPageGapLevel = container.settingsStore.readerPageGapLevel(),
            readerFitWidth = container.settingsStore.readerFitWidth(),
        ),
    )
    val state: StateFlow<TankobunUiState> = _state

    init {
        refreshInstalledSources()
        if (_state.value.extensionRepositoryUrl.isNotBlank()) {
            refreshExtensionIndex(silent = true)
        }
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
            val sources = allSources.preferredVisibleSources(
                preferredLanguages = _state.value.sourceLanguages,
                disabledSourceKeys = _state.value.disabledSourceKeys,
            )
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
            .plus(UNIVERSAL_SOURCE_LANGUAGE)
        container.settingsStore.saveSourceLanguages(next)
        _state.update {
            val sources = it.allInstalledSources.preferredVisibleSources(next, it.disabledSourceKeys)
            it.copy(
                sourceLanguages = next,
                installedSources = sources,
                selectedSourceId = it.selectedSourceId
                    ?.takeIf { current -> sources.any { source -> source.id == current } }
                    ?: sources.firstOrNull()?.id,
            )
        }
    }

    fun setSourceEnabled(source: SourceDescriptor, enabled: Boolean) {
        setSourcesEnabled(listOf(source), enabled)
    }

    fun setSourcesEnabled(sources: Collection<SourceDescriptor>, enabled: Boolean) {
        val keys = sources.mapTo(mutableSetOf()) { it.sourceSettingsKey() }
        if (keys.isEmpty()) return
        val languages = sources.mapTo(mutableSetOf()) { it.normalizedLanguage() }
            .filterTo(mutableSetOf()) { it.isNotBlank() }
        val nextLanguages = if (enabled) {
            _state.value.sourceLanguages + languages + UNIVERSAL_SOURCE_LANGUAGE
        } else {
            _state.value.sourceLanguages + UNIVERSAL_SOURCE_LANGUAGE
        }
        val nextDisabledKeys = if (enabled) {
            _state.value.disabledSourceKeys - keys
        } else {
            _state.value.disabledSourceKeys + keys
        }

        container.settingsStore.saveSourceLanguages(nextLanguages)
        container.settingsStore.saveDisabledSourceKeys(nextDisabledKeys)
        _state.update { current ->
            val visibleSources = current.allInstalledSources.preferredVisibleSources(nextLanguages, nextDisabledKeys)
            current.copy(
                sourceLanguages = nextLanguages,
                disabledSourceKeys = nextDisabledKeys,
                installedSources = visibleSources,
                selectedSourceId = current.selectedSourceId
                    ?.takeIf { selected -> visibleSources.any { source -> source.id == selected } }
                    ?: visibleSources.firstOrNull()?.id,
            )
        }
    }

    fun refreshExtensionIndex(silent: Boolean = false) {
        val repositoryUrl = _state.value.extensionRepositoryUrl.trim()
        if (repositoryUrl.isBlank()) {
            if (!silent) {
                _state.update { it.copy(message = "Paste an extension repository index URL first") }
            }
            return
        }
        viewModelScope.launch {
            if (!silent) {
                _state.update { it.copy(busy = true, message = null) }
            }
            runCatching {
                container.extensionRepository.fetchIndex(repositoryUrl)
            }.onSuccess { extensions ->
                _state.update {
                    it.copy(
                        availableExtensions = extensions,
                        busy = if (silent) it.busy else false,
                        message = if (silent) it.message else "Loaded ${extensions.size} extensions",
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        busy = if (silent) it.busy else false,
                        message = if (silent) it.message else error.message ?: "Extension index failed",
                    )
                }
            }
        }
    }

    fun extensionApkUrl(entry: ExtensionIndexEntry): String =
        container.extensionRepository.apkUrl(_state.value.extensionRepositoryUrl.trim(), entry)

    fun extensionIconUrl(entry: ExtensionIndexEntry): String? =
        _state.value.extensionRepositoryUrl.trim()
            .takeIf { it.isNotBlank() }
            ?.let { container.extensionRepository.iconUrl(it, entry) }

    fun installExtension(entry: ExtensionIndexEntry) {
        val apkUrl = extensionApkUrl(entry)
        viewModelScope.launch {
            _state.update {
                it.copy(
                    installingExtensionPackageName = entry.packageName,
                    extensionInstallRequest = null,
                    message = "Downloading ${entry.name}",
                )
            }
            runCatching {
                downloadExtensionApk(apkUrl, entry)
            }.onSuccess { apkUri ->
                _state.update {
                    it.copy(
                        installingExtensionPackageName = null,
                        extensionInstallRequest = ExtensionInstallRequest(
                            packageName = entry.packageName,
                            name = entry.name,
                            apkUri = apkUri.toString(),
                            expectedVersionCode = entry.versionCode,
                            expectedVersionName = entry.versionName,
                        ),
                        message = "Ready to install ${entry.name}",
                    )
                }
            }.onFailure { error ->
                Log.w(TAG, "Extension APK download failed for ${entry.packageName}", error)
                _state.update {
                    it.copy(
                        installingExtensionPackageName = null,
                        message = error.message ?: "Extension download failed",
                    )
                }
            }
        }
    }

    fun requireExtensionInstallPermission() {
        _state.update {
            it.copy(message = "Allow Tankobun to install extensions, then tap Install again")
        }
    }

    fun consumeExtensionInstallRequest() {
        _state.update { it.copy(extensionInstallRequest = null) }
    }

    fun refreshInstalledSourcesAfterExtensionInstall(request: ExtensionInstallRequest) {
        viewModelScope.launch {
            var installedVersion: InstalledExtensionVersion? = null
            for (attempt in 0 until 5) {
                refreshInstalledSources()
                installedVersion = installedExtensionVersion(request.packageName)
                if ((installedVersion?.versionCode ?: -1) >= request.expectedVersionCode) {
                    break
                }
                delay(1_000L * (attempt + 1))
            }
            refreshInstalledSources()
            val version = installedVersion
            val message = when {
                version == null -> "Installer returned before ${request.name} was installed"
                version.versionCode >= request.expectedVersionCode -> "Updated ${request.name} to v${version.versionName}"
                else -> "${request.name} is still v${version.versionName}; Android did not finish the update"
            }
            _state.update { it.copy(message = message) }
            Log.i(
                TAG,
                "Installer returned for ${request.packageName}; installed=${version?.versionCode}, expected=${request.expectedVersionCode}",
            )
        }
    }

    private fun installedExtensionVersion(packageName: String): InstalledExtensionVersion? =
        runCatching {
            val packageInfo = container.application.packageManager.getPackageInfo(packageName, 0)
            InstalledExtensionVersion(
                versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                },
                versionName = packageInfo.versionName.orEmpty(),
            )
        }.getOrNull()

    private suspend fun downloadExtensionApk(apkUrl: String, entry: ExtensionIndexEntry): Uri =
        withContext(Dispatchers.IO) {
            val cacheDir = File(container.application.cacheDir, "extension_apks").also { it.mkdirs() }
            val safeName = "${entry.packageName}-${entry.versionCode}.apk"
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
            val apkFile = File(cacheDir, safeName)
            val partialFile = File(cacheDir, "$safeName.part")

            cacheDir.listFiles()
                ?.filter { it.name.startsWith(entry.packageName) && it.name != apkFile.name }
                ?.forEach { it.delete() }

            val request = Request.Builder().url(apkUrl).build()
            container.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("APK download failed: HTTP ${response.code}")
                }
                val body = response.body
                partialFile.outputStream().use { output ->
                    body.byteStream().use { input -> input.copyTo(output) }
                }
            }

            if (partialFile.length() <= 0L) {
                partialFile.delete()
                error("APK download failed: empty file")
            }
            if (apkFile.exists()) apkFile.delete()
            check(partialFile.renameTo(apkFile)) { "APK download failed: could not finalize file" }

            FileProvider.getUriForFile(
                container.application,
                "${container.application.packageName}.fileprovider",
                apkFile,
            )
        }

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

    fun setBrowseGenre(genre: String, selected: Boolean) {
        _state.update {
            it.copy(
                browseGenres = if (selected) {
                    it.browseGenres + genre
                } else {
                    it.browseGenres - genre
                },
            )
        }
    }

    fun setBrowseTag(tag: String, selected: Boolean) {
        _state.update {
            it.copy(
                browseTags = if (selected) {
                    it.browseTags + tag
                } else {
                    it.browseTags - tag
                },
            )
        }
    }

    fun setBrowseFormat(format: String?) {
        _state.update { it.copy(browseFormat = format) }
    }

    fun setBrowsePublishingStatus(status: String?) {
        _state.update { it.copy(browsePublishingStatus = status) }
    }

    fun setBrowseCountryOfOrigin(country: String?) {
        _state.update { it.copy(browseCountryOfOrigin = country) }
    }

    fun setBrowseYear(year: Int?) {
        _state.update { it.copy(browseYear = year) }
    }

    fun setBrowseSort(sort: String) {
        _state.update { it.copy(browseSort = sort) }
    }

    fun resetBrowseFilters() {
        _state.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                browseSearched = false,
                browseGenres = emptySet(),
                browseTags = emptySet(),
                browseFormat = null,
                browsePublishingStatus = null,
                browseCountryOfOrigin = null,
                browseYear = null,
                browseSort = BROWSE_SORT_SEARCH_MATCH,
                message = null,
            )
        }
        loadBrowseLanding()
    }

    fun viewAllBrowseSection(sort: String) {
        _state.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                browseSearched = false,
                browseGenres = emptySet(),
                browseTags = emptySet(),
                browseFormat = null,
                browsePublishingStatus = null,
                browseCountryOfOrigin = null,
                browseYear = null,
                browseSort = sort,
                message = null,
            )
        }
        searchAniList()
    }

    fun viewAllPopularManhwa() {
        _state.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                browseSearched = false,
                browseGenres = emptySet(),
                browseTags = emptySet(),
                browseFormat = null,
                browsePublishingStatus = null,
                browseCountryOfOrigin = "KR",
                browseYear = null,
                browseSort = "POPULARITY_DESC",
                message = null,
            )
        }
        searchAniList()
    }

    fun loadBrowseLanding(force: Boolean = false) {
        if (!force && _state.value.browseLandingLoaded) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                val trending = cachedAnilistBrowseMedia(BROWSE_TRENDING_CACHE_KEY) {
                    container.anilistRepository.browseManga(sort = "TRENDING_DESC", perPage = 12)
                }
                val popular = cachedAnilistBrowseMedia(BROWSE_POPULAR_CACHE_KEY) {
                    container.anilistRepository.browseManga(sort = "POPULARITY_DESC", perPage = 12)
                }
                val popularManhwa = cachedAnilistBrowseMedia(BROWSE_MANHWA_CACHE_KEY) {
                    container.anilistRepository.browseManga(
                        countryOfOrigin = "KR",
                        sort = "POPULARITY_DESC",
                        perPage = 12,
                    )
                }
                val topManga = cachedAnilistBrowseMedia(BROWSE_TOP_MANGA_CACHE_KEY) {
                    fetchTopManga()
                }
                BrowseLandingData(trending, popular, popularManhwa, topManga)
            }.onSuccess { landing ->
                _state.update {
                    it.copy(
                        browseTrending = landing.trending,
                        browsePopular = landing.popular,
                        browsePopularManhwa = landing.popularManhwa,
                        browseTopManga = landing.topManga,
                        browseLandingLoaded = true,
                        busy = false,
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList browse landing failed", error)
                _state.update {
                    it.copy(busy = false, message = error.userMessage("Browse failed"))
                }
            }
        }
    }

    fun loadBrowseTags(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val cachedTags = container.settingsStore.anilistTags()
        val cachedAt = container.settingsStore.anilistTagsCachedAtEpochMillis()
        if (!force && cachedTags.isNotEmpty() && now - cachedAt <= cachePolicy.anilistTagsTtlMillis) {
            _state.update { it.copy(browseAvailableTags = cachedTags) }
            return
        }
        viewModelScope.launch {
            runCatching {
                container.anilistRepository.mediaTags()
            }.onSuccess { tags ->
                container.settingsStore.saveAnilistTags(tags, System.currentTimeMillis())
                _state.update { it.copy(browseAvailableTags = tags) }
            }.onFailure { error ->
                Log.w(TAG, "AniList tag collection failed", error)
            }
        }
    }

    fun searchAniList() {
        val snapshot = _state.value
        val query = snapshot.searchQuery.trim()
        if (!snapshot.hasBrowseQueryOrFilters()) {
            _state.update { it.copy(searchResults = emptyList(), browseSearched = false, message = null) }
            loadBrowseLanding()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, browseSearched = true, message = null) }
            runCatching {
                val cacheKey = snapshot.browseCacheKey()
                cachedAnilistBrowseMedia(cacheKey) {
                    if (!snapshot.hasBrowseFilters() && snapshot.browseSort == BROWSE_SORT_SEARCH_MATCH) {
                        container.anilistRepository.searchManga(query)
                    } else {
                        container.anilistRepository.browseManga(
                            search = query.takeIf { it.isNotBlank() },
                            genres = snapshot.browseGenres,
                            tags = snapshot.browseTags,
                            format = snapshot.browseFormat,
                            status = snapshot.browsePublishingStatus,
                            countryOfOrigin = snapshot.browseCountryOfOrigin,
                            year = snapshot.browseYear,
                            sort = snapshot.effectiveBrowseSort(),
                        )
                    }
                }
            }.onSuccess { results ->
                _state.update { it.copy(searchResults = results, busy = false) }
            }.onFailure { error ->
                Log.e(TAG, "AniList search failed for $query", error)
                _state.update { it.copy(busy = false, message = error.userMessage("Search failed")) }
            }
        }
    }

    private suspend fun cachedAnilistBrowseMedia(
        cacheKey: String,
        fetch: suspend () -> List<AnilistMedia>,
    ): List<AnilistMedia> {
        val now = System.currentTimeMillis()
        val cachedRows = container.database.searchResultDao().cachedSearchRows(cacheKey)
        val cachedIsFresh = cachedRows.isNotEmpty() &&
            cachedRows.all { now - it.fetchedAtEpochMillis <= cachePolicy.anilistSearchTtlMillis }
        if (cachedIsFresh) {
            return container.database.searchResultDao().cachedSearchMedia(cacheKey).map { it.toModel() }
        }
        val results = fetch()
        container.database.mediaDao().upsertMedia(results.map { it.toEntity(now) })
        container.database.searchResultDao().deleteForQuery(cacheKey)
        container.database.searchResultDao().upsertResults(
            results.mapIndexed { index, media ->
                AnilistSearchResultEntity(
                    query = cacheKey,
                    mediaId = media.id,
                    orderIndex = index,
                    fetchedAtEpochMillis = now,
                )
            },
        )
        return results
    }

    private suspend fun fetchTopManga(): List<AnilistMedia> {
        val firstPage = container.anilistRepository.browseManga(sort = "SCORE_DESC", page = 1, perPage = 50)
        val secondPage = container.anilistRepository.browseManga(sort = "SCORE_DESC", page = 2, perPage = 50)
        return (firstPage + secondPage).distinctBy { it.id }.take(100)
    }

    fun selectMedia(media: AnilistMedia) {
        val existingEntry = _state.value.libraryItems.firstOrNull { item -> item.media.id == media.id }?.entry
        _state.update {
            it.copy(
                selectedMedia = media,
                sourceMatches = emptyList(),
                sourceMatchChapterCounts = emptyMap(),
                sourcePickerOpen = false,
                sourcePickerLoading = false,
                sourcePickerMessage = null,
                sourcePickerDiagnostics = emptyList(),
                selectedListEntry = existingEntry,
                selectedRecommendations = emptyList(),
                selectedRecommendationsPage = 0,
                selectedRecommendationsHasMore = false,
                recommendationsLoading = false,
                trackingStatus = existingEntry?.status ?: MediaStatus.PLANNING,
                trackingProgress = (existingEntry?.progress ?: 0).toString(),
                trackingScore = existingEntry?.score?.toString().orEmpty(),
                trackingNotes = existingEntry?.notes.orEmpty(),
                trackingPrivate = existingEntry?.private ?: false,
                trackingCustomLists = existingEntry?.customLists?.joinToString(", ").orEmpty(),
                selectedSourceManga = null,
                sourceChapters = emptyList(),
                latestProgress = null,
                activeChapter = null,
                readerPages = emptyList(),
                currentPageIndex = 0,
                message = null,
            )
        }
        loadAnilistDetails(media.id)
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
                sourcePickerMessage = null,
                sourcePickerDiagnostics = emptyList(),
                selectedListEntry = null,
                selectedRecommendations = emptyList(),
                selectedRecommendationsPage = 0,
                selectedRecommendationsHasMore = false,
                recommendationsLoading = false,
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
        container.settingsStore.saveReaderMode(mode)
        _state.update { it.copy(readerMode = mode) }
        saveReaderProgress()
    }

    fun setReaderPageGapLevel(level: Int) {
        val normalized = level.coerceIn(0, 3)
        container.settingsStore.saveReaderPageGapLevel(normalized)
        _state.update { it.copy(readerPageGapLevel = normalized) }
    }

    fun setReaderFitWidth(enabled: Boolean) {
        container.settingsStore.saveReaderFitWidth(enabled)
        _state.update { it.copy(readerFitWidth = enabled) }
    }

    fun setTrackingStatus(status: MediaStatus) {
        _state.update { it.copy(trackingStatus = status) }
    }

    fun setTrackingProgress(progress: String) {
        _state.update { it.copy(trackingProgress = progress.filter { char -> char.isDigit() }.take(5)) }
    }

    fun setTrackingScore(score: String) {
        _state.update { it.copy(trackingScore = score.filter { char -> char.isDigit() || char == '.' }.take(5)) }
    }

    fun setTrackingNotes(notes: String) {
        _state.update { it.copy(trackingNotes = notes) }
    }

    fun setTrackingPrivate(private: Boolean) {
        _state.update { it.copy(trackingPrivate = private) }
    }

    fun setTrackingCustomLists(customLists: String) {
        _state.update { it.copy(trackingCustomLists = customLists) }
    }

    fun saveTracking() {
        val media = _state.value.selectedMedia ?: return
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = "Connect AniList to track this manga") }
            return
        }

        val snapshot = _state.value
        val progress = snapshot.trackingProgress.toIntOrNull()?.coerceAtLeast(0)
        val score = snapshot.trackingScore.toDoubleOrNull()?.coerceIn(0.0, 100.0)
        val notes = snapshot.trackingNotes.trim().ifBlank { null }
        val customLists = snapshot.trackingCustomLists
            .split(',', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                val entry = container.anilistRepository.saveListEntry(
                    accessToken = token,
                    mediaId = media.id,
                    status = snapshot.trackingStatus,
                    progress = progress,
                    score = score,
                    notes = notes,
                    private = snapshot.trackingPrivate,
                    customLists = customLists,
                )
                val now = System.currentTimeMillis()
                container.database.mediaDao().upsertMedia(media.toEntity(now))
                container.database.listEntryDao().upsertEntry(entry.toEntity(now))
                entry
            }.onSuccess { entry ->
                _state.update {
                    val nextItem = LibraryItem(media, entry)
                    val nextItems = (it.libraryItems.filterNot { item -> item.media.id == media.id } + nextItem)
                        .sortedBy { item -> item.media.title.userPreferred.lowercase(Locale.ROOT) }
                    it.copy(
                        library = nextItems.map { item -> item.media },
                        libraryItems = nextItems,
                        selectedListEntry = entry,
                        trackingStatus = entry.status,
                        trackingProgress = entry.progress.toString(),
                        trackingScore = entry.score?.toString().orEmpty(),
                        trackingNotes = entry.notes.orEmpty(),
                        trackingPrivate = entry.private,
                        trackingCustomLists = entry.customLists.joinToString(", "),
                        busy = false,
                        message = "AniList tracking saved",
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList tracking save failed for ${media.id}", error)
                _state.update { it.copy(busy = false, message = error.userMessage("Tracking save failed")) }
            }
        }
    }

    private fun loadAnilistDetails(mediaId: Int) {
        viewModelScope.launch {
            val token = container.tokenStore.accessToken()
            val now = System.currentTimeMillis()
            val cachedMedia = container.database.mediaDao().cachedMedia(mediaId)
            val cachedEntry = container.database.listEntryDao().cachedEntry(mediaId)?.toModel()
            val cachedRecommendations = cachedRecommendations(mediaId)
            if (cachedMedia != null || cachedEntry != null || cachedRecommendations.isNotEmpty()) {
                _state.update {
                    if (it.selectedMedia?.id != mediaId) {
                        it
                    } else {
                        it.copy(
                            selectedMedia = cachedMedia?.toModel() ?: it.selectedMedia,
                            selectedListEntry = cachedEntry,
                            selectedRecommendations = cachedRecommendations,
                            selectedRecommendationsPage = cachedRecommendations.recommendationPageCount(),
                            selectedRecommendationsHasMore = cachedRecommendations.size >= RECOMMENDATIONS_PAGE_SIZE,
                            trackingStatus = cachedEntry?.status ?: it.trackingStatus,
                            trackingProgress = cachedEntry?.progress?.toString() ?: it.trackingProgress,
                            trackingScore = cachedEntry?.score?.toString() ?: it.trackingScore,
                            trackingNotes = cachedEntry?.notes ?: it.trackingNotes,
                            trackingPrivate = cachedEntry?.private ?: it.trackingPrivate,
                            trackingCustomLists = cachedEntry?.customLists?.joinToString(", ") ?: it.trackingCustomLists,
                        )
                    }
                }
            }

            val cachedMediaIsFresh = cachedMedia != null && now - cachedMedia.fetchedAtEpochMillis <= cachePolicy.mediaDetailsTtlMillis
            val cachedRecommendationsAreFresh = container.database.recommendationDao()
                .cachedRecommendations(mediaId)
                .firstOrNull()
                ?.let { now - it.fetchedAtEpochMillis <= cachePolicy.mediaDetailsTtlMillis }
                ?: false
            if (cachedMediaIsFresh && cachedRecommendationsAreFresh) {
                return@launch
            }

            runCatching {
                container.anilistRepository.mediaDetailsWithEntry(
                    mediaId = mediaId,
                    accessToken = token,
                    recommendationsPage = 1,
                    recommendationsPerPage = RECOMMENDATIONS_PAGE_SIZE,
                )
            }.onSuccess { result ->
                val details = result.media
                val entry = result.listEntry
                val recommendationPage = result.recommendationPage
                val recommendations = recommendationPage.recommendations
                container.database.mediaDao().upsertMedia(details.toEntity(now))
                container.database.mediaDao().upsertMedia(recommendations.map { it.media.toEntity(now) })
                container.database.recommendationDao().deleteForMedia(mediaId)
                container.database.recommendationDao().upsertRecommendations(
                    recommendations.map { it.toEntity(mediaId, now) },
                )
                if (entry != null) {
                    container.database.listEntryDao().upsertEntry(entry.toEntity(now))
                }
                _state.update {
                    if (it.selectedMedia?.id != mediaId) {
                        it
                    } else {
                        val effectiveEntry = entry ?: cachedEntry
                        val nextItems = if (effectiveEntry == null) {
                            it.libraryItems
                        } else {
                            (it.libraryItems.filterNot { item -> item.media.id == mediaId } + LibraryItem(details, effectiveEntry))
                                .sortedBy { item -> item.media.title.userPreferred.lowercase(Locale.ROOT) }
                        }
                        it.copy(
                            selectedMedia = details,
                            selectedListEntry = effectiveEntry,
                            selectedRecommendations = recommendations,
                            selectedRecommendationsPage = recommendationPage.currentPage,
                            selectedRecommendationsHasMore = recommendationPage.hasNextPage,
                            recommendationsLoading = false,
                            library = nextItems.map { item -> item.media },
                            libraryItems = nextItems,
                            trackingStatus = effectiveEntry?.status ?: it.trackingStatus,
                            trackingProgress = effectiveEntry?.progress?.toString() ?: it.trackingProgress,
                            trackingScore = effectiveEntry?.score?.toString() ?: it.trackingScore,
                            trackingNotes = effectiveEntry?.notes ?: it.trackingNotes,
                            trackingPrivate = effectiveEntry?.private ?: it.trackingPrivate,
                            trackingCustomLists = effectiveEntry?.customLists?.joinToString(", ") ?: it.trackingCustomLists,
                        )
                    }
                }
            }.onFailure { error ->
                Log.w(TAG, "AniList details failed for $mediaId", error)
                _state.update {
                    if (it.selectedMedia?.id == mediaId) it.copy(recommendationsLoading = false) else it
                }
            }
        }
    }

    fun loadMoreRecommendations() {
        val snapshot = _state.value
        val mediaId = snapshot.selectedMedia?.id ?: return
        if (!snapshot.selectedRecommendationsHasMore || snapshot.recommendationsLoading) return
        val nextPage = snapshot.selectedRecommendationsPage.coerceAtLeast(1) + 1

        viewModelScope.launch {
            val token = container.tokenStore.accessToken()
            _state.update { it.copy(recommendationsLoading = true, message = null) }
            runCatching {
                container.anilistRepository.mediaRecommendations(
                    mediaId = mediaId,
                    page = nextPage,
                    perPage = RECOMMENDATIONS_PAGE_SIZE,
                    accessToken = token,
                )
            }.onSuccess { recommendationPage ->
                val now = System.currentTimeMillis()
                container.database.mediaDao().upsertMedia(recommendationPage.recommendations.map { it.media.toEntity(now) })
                container.database.recommendationDao().upsertRecommendations(
                    recommendationPage.recommendations.map { it.toEntity(mediaId, now) },
                )
                _state.update {
                    if (it.selectedMedia?.id != mediaId) {
                        it
                    } else {
                        val combined = (it.selectedRecommendations + recommendationPage.recommendations)
                            .distinctBy { recommendation -> recommendation.media.id }
                        it.copy(
                            selectedRecommendations = combined,
                            selectedRecommendationsPage = recommendationPage.currentPage,
                            selectedRecommendationsHasMore = recommendationPage.hasNextPage,
                            recommendationsLoading = false,
                        )
                    }
                }
            }.onFailure { error ->
                Log.w(TAG, "AniList recommendations failed for $mediaId page $nextPage", error)
                _state.update {
                    if (it.selectedMedia?.id == mediaId) {
                        it.copy(
                            recommendationsLoading = false,
                            message = error.userMessage("Recommendations failed"),
                        )
                    } else {
                        it
                    }
                }
            }
        }
    }

    private suspend fun cachedRecommendations(mediaId: Int): List<AnilistRecommendation> {
        val recommendationEntities = container.database.recommendationDao().cachedRecommendations(mediaId)
        if (recommendationEntities.isEmpty()) return emptyList()
        val mediaById = container.database.recommendationDao()
            .cachedRecommendationMedia(mediaId)
            .associateBy { it.id }
        return recommendationEntities.mapNotNull { recommendation ->
            mediaById[recommendation.recommendationMediaId]?.toModel()?.let { media ->
                AnilistRecommendation(media = media, rating = recommendation.rating)
            }
        }
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
        _state.update {
            it.copy(
                sourcePickerOpen = true,
                sourcePickerMessage = null,
                sourcePickerDiagnostics = emptyList(),
                message = null,
            )
        }
        if (sources.isEmpty()) {
            _state.update { it.copy(sourcePickerMessage = "Enable or install a source extension first") }
            return
        }
        if (!_state.value.sourcePickerLoading) {
            findSourceMatches(forceRefresh = true)
        }
    }

    fun closeSourcePicker() {
        _state.update {
            it.copy(
                sourcePickerOpen = false,
                sourcePickerLoading = false,
                sourcePickerMessage = null,
                sourcePickerDiagnostics = emptyList(),
            )
        }
    }

    fun bindSelectedSource() {
        val media = _state.value.selectedMedia ?: return
        val source = _state.value.selectedSource ?: run {
            _state.update { it.copy(message = "Enable or install a source extension first") }
            return
        }
        bindSource(source)
    }

    fun bindSource(source: SourceDescriptor) {
        val media = _state.value.selectedMedia ?: return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    busy = true,
                    sourcePickerLoading = true,
                    sourcePickerMessage = "Searching ${source.name}...",
                    sourcePickerDiagnostics = emptyList(),
                    message = null,
                )
            }
            runCatching {
                val now = System.currentTimeMillis()
                val matches = searchSourceMatches(media, source, now)
                    .filter { it.isReadableMatchCandidate() }
                val verified = firstReadableMatch(source, matches, now)
                    ?: error("No readable manga found on ${source.name}")
                saveSourceBinding(verified.match)
                verified.match
            }.onSuccess { match ->
                _state.update {
                    it.copy(
                        sourceMatches = (listOf(match) + it.sourceMatches).distinctBy { result ->
                            "${result.source.id}:${result.manga.url}"
                        },
                        selectedSourceId = match.source.id,
                        selectedSourceManga = match.manga,
                        sourcePickerOpen = false,
                        sourcePickerLoading = false,
                        sourcePickerMessage = null,
                        message = "Source selected for ${match.manga.title}",
                    )
                }
                loadChapters(match.source, match.manga)
            }.onFailure { error ->
                Log.w(TAG, "Selected source binding failed for ${source.name}", error)
                _state.update {
                    it.copy(
                        busy = false,
                        sourcePickerLoading = false,
                        sourcePickerMessage = sourcePickerErrorMessage(source.name, error),
                        message = null,
                    )
                }
            }
        }
    }

    private suspend fun searchSourceMatches(
        media: AnilistMedia,
        source: SourceDescriptor,
        now: Long,
    ): List<SourceSearchResult> {
        val queries = sourceSearchQueries(media)
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

        val ranked = container.sourceMatcher.rank(media, source, distinctCandidates, now)
        Log.i(
            TAG,
            "Source search ${source.name}: queries=$searchedQueries/${queries.size}, failed=$failedQueries, candidates=${distinctCandidates.size}, ranked=${ranked.size}",
        )
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

    private fun sourceSearchQueries(media: AnilistMedia): List<String> {
        val rawTitles = buildList {
            add(media.title.userPreferred)
            media.title.romaji?.let(::add)
            media.title.english?.let(::add)
            media.title.native?.let(::add)
            addAll(media.synonyms)
        }

        return rawTitles
            .flatMap(::sourceSearchQueryVariants)
            .map(::cleanSourceSearchQuery)
            .filter { it.length >= 2 }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(SOURCE_SEARCH_QUERY_LIMIT)
    }

    private fun sourceSearchQueryVariants(title: String): List<String> {
        val withoutHtml = title.withoutHtmlTags()
        val withoutParenthetical = withoutHtml.withoutBracketedText()
        val subtitlePrefix = withoutHtml.mainTitlePrefix()
        val spacedNumber = withoutHtml.withSpacedNoNumber()
        val withoutNumberPrefix = withoutHtml.withoutNoNumberPrefix()
        val words = cleanSourceSearchQuery(withoutHtml)
            .split(' ')
            .filter { it.isNotBlank() }
        val acronym = words
            .takeIf { it.size >= 3 }
            ?.joinToString(separator = "") { it.first().uppercaseChar().toString() }
        val leadingWords = words
            .takeIf { it.size >= 4 }
            ?.take(4)
            ?.joinToString(" ")

        return buildList {
            add(withoutHtml)
            add(withoutParenthetical)
            add(subtitlePrefix)
            add(spacedNumber)
            add(withoutNumberPrefix)
            leadingWords?.let(::add)
            acronym?.let(::add)
        }.distinctBy { it.lowercase(Locale.ROOT) }
    }

    private fun cleanSourceSearchQuery(query: String): String =
        buildString {
            var previousWasSpace = true
            query.forEach { char ->
                val replacement = when {
                    char == '&' -> " and "
                    char.isLetterOrDigit() -> char.toString()
                    char.isWhitespace() -> " "
                    else -> " "
                }
                replacement.forEach { next ->
                    if (next.isWhitespace()) {
                        if (!previousWasSpace) append(' ')
                        previousWasSpace = true
                    } else {
                        append(next)
                        previousWasSpace = false
                    }
                }
            }
        }.trim()

    private fun String.withoutHtmlTags(): String = buildString {
        var insideTag = false
        this@withoutHtmlTags.forEach { char ->
            when (char) {
                '<' -> {
                    insideTag = true
                    append(' ')
                }
                '>' -> {
                    insideTag = false
                    append(' ')
                }
                else -> if (!insideTag) append(char)
            }
        }
    }

    private fun String.withoutBracketedText(): String = buildString {
        var closingBracket: Char? = null
        this@withoutBracketedText.forEach { char ->
            when {
                closingBracket == null && char == '(' -> {
                    closingBracket = ')'
                    append(' ')
                }
                closingBracket == null && char == '[' -> {
                    closingBracket = ']'
                    append(' ')
                }
                closingBracket == null && char == '{' -> {
                    closingBracket = '}'
                    append(' ')
                }
                closingBracket != null && char == closingBracket -> {
                    closingBracket = null
                    append(' ')
                }
                closingBracket == null -> append(char)
            }
        }
    }

    private fun String.mainTitlePrefix(): String {
        val separators = listOf(
            ":",
            "\uFF1A",
            " - ",
            " \u2010 ",
            " \u2011 ",
            " \u2012 ",
            " \u2013 ",
            " \u2014 ",
            " \u2015 ",
        )
        val splitAt = separators.mapNotNull { separator ->
            indexOf(separator).takeIf { it >= 0 }
        }.minOrNull()
        return splitAt?.let { take(it) }.orEmpty()
    }

    private fun String.withSpacedNoNumber(): String = buildString {
        var index = 0
        while (index < this@withSpacedNoNumber.length) {
            val char = this@withSpacedNoNumber[index]
            val next = this@withSpacedNoNumber.getOrNull(index + 1)
            if ((char == 'N' || char == 'n') && (next == 'O' || next == 'o')) {
                var cursor = index + 2
                if (this@withSpacedNoNumber.getOrNull(cursor) == '.') cursor += 1
                if (this@withSpacedNoNumber.getOrNull(cursor)?.isDigit() == true) {
                    append("No. ")
                    index = cursor
                    continue
                }
            }
            append(char)
            index += 1
        }
    }

    private fun String.withoutNoNumberPrefix(): String = buildString {
        var index = 0
        while (index < this@withoutNoNumberPrefix.length) {
            val char = this@withoutNoNumberPrefix[index]
            val next = this@withoutNoNumberPrefix.getOrNull(index + 1)
            if ((char == 'N' || char == 'n') && (next == 'O' || next == 'o')) {
                var cursor = index + 2
                if (this@withoutNoNumberPrefix.getOrNull(cursor) == '.') cursor += 1
                while (this@withoutNoNumberPrefix.getOrNull(cursor)?.isWhitespace() == true) {
                    cursor += 1
                }
                if (this@withoutNoNumberPrefix.getOrNull(cursor)?.isDigit() == true) {
                    index = cursor
                    continue
                }
            }
            append(char)
            index += 1
        }
    }

    private fun sourcePickerErrorMessage(sourceName: String, error: Throwable): String {
        val detail = errorDetail(error)
            ?: error.javaClass.simpleName
        return when {
            isMissingSourceCompatibilityClass(error) ->
                "$sourceName needs a compatibility library that was missing from the app. Update the app and try again."
            detail.contains("syntax error in regexp pattern", ignoreCase = true) ->
                "$sourceName failed while parsing source data. The extension reported a regexp error; try another source or update that extension."
            detail.contains("timeout", ignoreCase = true) || detail.contains("timed out", ignoreCase = true) ->
                "$sourceName took too long to respond. Try again or choose another source."
            detail.contains("No readable manga found", ignoreCase = true) ->
                detail
            else -> "$sourceName failed: $detail"
        }
    }

    private fun sourcePickerDiagnosticDetail(error: Throwable): String {
        val detail = errorDetail(error) ?: error.javaClass.simpleName
        return when {
            isMissingSourceCompatibilityClass(error) -> "missing compatibility class"
            detail.contains("HTTP error", ignoreCase = true) -> detail
            detail.contains("syntax error in regexp pattern", ignoreCase = true) -> "regexp parse error"
            detail.contains("timeout", ignoreCase = true) -> "timed out"
            else -> detail.take(120)
        }
    }

    private fun errorDetail(error: Throwable): String? =
        errorDetails(error)
            .firstOrNull { it.contains("HTTP error", ignoreCase = true) }
            ?: errorDetails(error).firstOrNull()

    private fun errorDetails(error: Throwable): List<String> =
        generateSequence(error) { it.cause }
            .mapNotNull { it.message?.takeIf(String::isNotBlank) }
            .toList()

    private fun isMissingSourceCompatibilityClass(error: Throwable): Boolean =
        generateSequence(error) { it.cause }.any { cause ->
            cause is NoClassDefFoundError ||
                cause.message?.contains("OkioStreamsKt", ignoreCase = true) == true
        }

    private fun isFatalSourceSearchError(error: Throwable): Boolean {
        val details = errorDetails(error).joinToString(separator = "\n")
        return isMissingSourceCompatibilityClass(error) ||
            details.contains("HTTP error 401", ignoreCase = true) ||
            details.contains("HTTP error 403", ignoreCase = true)
    }

    fun findSourceMatches(forceRefresh: Boolean = false) {
        val media = _state.value.selectedMedia ?: return
        val sources = sourcePickerSources()
        if (sources.isEmpty()) {
            _state.update { it.copy(sourcePickerMessage = "Enable or install a source extension first") }
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    busy = true,
                    sourcePickerLoading = true,
                    sourcePickerMessage = "Searching enabled sources...",
                    sourcePickerDiagnostics = emptyList(),
                    message = null,
                )
            }
            runCatching {
                val now = System.currentTimeMillis()
                cachedVerifiedMatches(media.id, sources, now)
                    .takeIf { !forceRefresh && it.matches.isNotEmpty() }
                    ?: searchVerifiedMatches(media, sources, now).also { verified ->
                        container.database.sourceSearchDao().clearForMedia(media.id)
                        if (verified.matches.isNotEmpty()) {
                            container.database.sourceSearchDao().upsertResults(verified.matches.map { it.toEntity() })
                        }
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
                        sourcePickerMessage = if (nextMatches.isEmpty()) {
                            "No readable matches found automatically. Tap a source below to try it directly."
                        } else {
                            "Found ${nextMatches.size} readable sources"
                        },
                        message = null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        busy = false,
                        sourcePickerLoading = false,
                        sourcePickerMessage = sourcePickerErrorMessage("source search", error),
                        message = null,
                    )
                }
            }
        }
    }

    private fun sourcePickerSources(): List<SourceDescriptor> {
        val snapshot = _state.value
        val selectedSourceId = snapshot.selectedSourceId
        return snapshot.installedSources
            .distinctBy { "${it.packageName}:${it.id}" }
            .sortedWith(
                compareBy<SourceDescriptor> { if (it.id == selectedSourceId) 0 else 1 }
                    .thenBy { it.languageSortPriority(snapshot.sourceLanguages) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.lang },
            )
    }

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
            val chapters = cachedChapters(source, match.manga, now, requireFresh = false)
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
    ): VerifiedSourceMatches = supervisorScope {
        val semaphore = Semaphore(SOURCE_SEARCH_CONCURRENCY)
        val verified = sources
            .map { source ->
                async {
                    semaphore.withPermit {
                        try {
                            val searchResults = withTimeoutOrNull(SOURCE_MATCH_TIMEOUT_MILLIS) {
                                searchSourceMatches(media, source, now)
                            }
                            val candidates = searchResults
                                .orEmpty()
                                .filter { it.isReadableMatchCandidate() }
                                .take(SOURCE_CANDIDATES_TO_VERIFY)
                            when {
                                searchResults == null -> {
                                    publishSourcePickerDiagnostic(media.id, source, "search timed out")
                                    null
                                }
                                searchResults.isEmpty() -> {
                                    publishSourcePickerDiagnostic(media.id, source, "no search results")
                                    null
                                }
                                candidates.isEmpty() -> {
                                    publishSourcePickerDiagnostic(media.id, source, "no confident title match")
                                    null
                                }
                                else -> {
                                    val readable = withTimeoutOrNull(SOURCE_VERIFY_TIMEOUT_MILLIS) {
                                        firstReadableMatch(source, candidates, now)
                                    }
                                    if (readable == null) {
                                        publishSourcePickerDiagnostic(media.id, source, "no readable chapters")
                                    } else {
                                        publishSourcePickerMatch(media.id, readable.match, readable.chapterCount)
                                    }
                                    readable
                                }
                            }
                        } catch (error: Throwable) {
                            if (error is CancellationException) throw error
                            Log.w(TAG, "Source verification failed for ${source.name}", error)
                            publishSourcePickerDiagnostic(media.id, source, sourcePickerDiagnosticDetail(error))
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
            matches = matches.distinctBy { "${it.source.id}:${it.manga.url}" }.sortedByDescending { it.score },
            chapterCounts = counts,
        )
    }

    private suspend fun firstReadableMatch(
        source: SourceDescriptor,
        candidates: List<SourceSearchResult>,
        now: Long,
    ): VerifiedReadableMatch? {
        candidates.take(SOURCE_CANDIDATES_TO_VERIFY).forEach { candidate ->
            verifyReadableMatch(source, candidate, now)?.let { return it }
        }
        return null
    }

    private suspend fun verifyReadableMatch(
        source: SourceDescriptor,
        candidate: SourceSearchResult,
        now: Long,
    ): VerifiedReadableMatch? {
        val resolved = withTimeoutOrNull(SOURCE_DETAILS_TIMEOUT_MILLIS) {
            candidate.withResolvedManga()
        } ?: candidate
        val attempts = listOf(resolved, candidate)
            .filter { it.manga.url.isNotBlank() }
            .distinctBy { "${it.manga.url}:${it.manga.title}" }

        attempts.forEach { match ->
            val chapters = withTimeoutOrNull(SOURCE_CHAPTER_TIMEOUT_MILLIS) {
                cachedChapters(source, match.manga, now, requireFresh = false)
                    ?: fetchAndCacheChapters(source, match.manga, now)
            }.orEmpty()
            if (chapters.isNotEmpty()) {
                return VerifiedReadableMatch(match, chapters.size)
            }
        }
        return null
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
                    sourcePickerMessage = "Found ${nextMatches.size} readable sources",
                    message = null,
                )
            }
        }
    }

    private fun publishSourcePickerDiagnostic(mediaId: Int, source: SourceDescriptor, detail: String) {
        val diagnostic = "${source.name}: $detail"
        _state.update {
            if (it.selectedMedia?.id != mediaId) {
                it
            } else if (diagnostic in it.sourcePickerDiagnostics) {
                it
            } else {
                it.copy(sourcePickerDiagnostics = it.sourcePickerDiagnostics + diagnostic)
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
            _state.update {
                it.copy(
                    busy = true,
                    sourcePickerLoading = true,
                    sourcePickerMessage = "Opening ${match.manga.title} from ${match.source.name}...",
                    message = null,
                )
            }
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
                        sourcePickerMessage = null,
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
                        sourcePickerMessage = sourcePickerErrorMessage(match.source.name, error),
                        message = null,
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
        private const val SOURCE_VERIFY_TIMEOUT_MILLIS = 20_000L
        private const val SOURCE_QUERY_TIMEOUT_MILLIS = 6_000L
        private const val SOURCE_DETAILS_TIMEOUT_MILLIS = 6_000L
        private const val SOURCE_CHAPTER_TIMEOUT_MILLIS = 10_000L
        private const val SOURCE_SEARCH_CONCURRENCY = 4
        private const val SOURCE_SEARCH_QUERY_LIMIT = 12
        private const val SOURCE_CANDIDATES_TO_VERIFY = 5
        private const val SOURCE_FALLBACK_CANDIDATES_PER_SOURCE = 5
        private const val SOURCE_MAX_CANDIDATES_PER_SOURCE = 40
        private const val SOURCE_STRONG_MATCH_SCORE = 0.9
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

private fun SourceSearchResult.isReadableMatchCandidate(): Boolean =
    score >= SOURCE_READABLE_MATCH_SCORE

private fun sourceMatchKey(sourceId: Long, mangaUrl: String): String =
    "$sourceId:$mangaUrl"

private const val SOURCE_READABLE_MATCH_SCORE = 0.9
private const val BROWSE_SORT_SEARCH_MATCH = "SEARCH_MATCH"
private const val BROWSE_TRENDING_CACHE_KEY = "browse:section:trending"
private const val BROWSE_POPULAR_CACHE_KEY = "browse:section:popular"
private const val BROWSE_MANHWA_CACHE_KEY = "browse:section:popular-manhwa"
private const val BROWSE_TOP_MANGA_CACHE_KEY = "browse:section:top-100:v2"
private const val RECOMMENDATIONS_PAGE_SIZE = 18

private fun TankobunUiState.hasBrowseFilters(): Boolean =
    browseGenres.isNotEmpty() ||
        browseTags.isNotEmpty() ||
        browseFormat != null ||
        browsePublishingStatus != null ||
        browseCountryOfOrigin != null ||
        browseYear != null

private fun TankobunUiState.hasBrowseQueryOrFilters(): Boolean =
    searchQuery.trim().isNotBlank() ||
        hasBrowseFilters() ||
        browseSort != BROWSE_SORT_SEARCH_MATCH

private fun TankobunUiState.effectiveBrowseSort(): String =
    if (browseSort == BROWSE_SORT_SEARCH_MATCH && searchQuery.isBlank()) {
        "TRENDING_DESC"
    } else {
        browseSort
    }

private fun TankobunUiState.browseCacheKey(): String = buildString {
    append("browse:")
    append("q=").append(searchQuery.normalizedSearchKey())
    append("|genres=").append(browseGenres.sorted().joinToString(",") { it.normalizedSearchKey() })
    append("|tags=").append(browseTags.sorted().joinToString(",") { it.normalizedSearchKey() })
    append("|format=").append(browseFormat.orEmpty())
    append("|status=").append(browsePublishingStatus.orEmpty())
    append("|country=").append(browseCountryOfOrigin.orEmpty())
    append("|year=").append(browseYear?.toString().orEmpty())
    append("|sort=").append(effectiveBrowseSort())
}

private fun String.normalizedSearchKey(): String =
    trim().lowercase(Locale.ROOT)

private fun List<AnilistRecommendation>.recommendationPageCount(): Int =
    if (isEmpty()) 0 else ((size - 1) / RECOMMENDATIONS_PAGE_SIZE) + 1

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

private fun List<SourceDescriptor>.preferredVisibleSources(
    preferredLanguages: Set<String>,
    disabledSourceKeys: Set<String> = emptySet(),
): List<SourceDescriptor> {
    val preferredSources = filter {
        val language = it.normalizedLanguage()
        language in preferredLanguages || language == UNIVERSAL_SOURCE_LANGUAGE
    }
    return (preferredSources.ifEmpty { this })
        .filterNot { it.sourceSettingsKey() in disabledSourceKeys }
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

internal fun SourceDescriptor.sourceSettingsKey(): String =
    "$packageName:$id"
