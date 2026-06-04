package com.tankobun.app

import com.tankobun.app.backup.AniListBackupService
import com.tankobun.app.backup.isDue
import com.tankobun.app.logic.BROWSE_MANHWA_CACHE_KEY
import com.tankobun.app.logic.BROWSE_POPULAR_CACHE_KEY
import com.tankobun.app.logic.BROWSE_SORT_SEARCH_MATCH
import com.tankobun.app.logic.BROWSE_TOP_MANGA_CACHE_KEY
import com.tankobun.app.logic.BROWSE_TRENDING_CACHE_KEY
import com.tankobun.app.logic.BulkDownloadResult
import com.tankobun.app.logic.RECOMMENDATIONS_PAGE_SIZE
import com.tankobun.app.logic.browseCacheKey
import com.tankobun.app.logic.buildDownloadStorageSummary
import com.tankobun.app.logic.bulkDownloadMessage
import com.tankobun.app.logic.downloadSourceName
import com.tankobun.app.logic.effectiveBrowseSort
import com.tankobun.app.logic.filteredScoreInput
import com.tankobun.app.logic.formatTrackingScore
import com.tankobun.app.logic.hasBrowseFilters
import com.tankobun.app.logic.hasBrowseQueryOrFilters
import com.tankobun.app.logic.isReadableMatchCandidate
import com.tankobun.app.logic.languageSortPriority
import com.tankobun.app.logic.mediaTitle
import com.tankobun.app.logic.nextInReadingOrderAfter
import com.tankobun.app.logic.nextTenDownloadCandidates
import com.tankobun.app.logic.nullableBoolean
import com.tankobun.app.logic.nullableDouble
import com.tankobun.app.logic.nullableInt
import com.tankobun.app.logic.nullableString
import com.tankobun.app.logic.nullableStringList
import com.tankobun.app.logic.normalizedLanguage
import com.tankobun.app.logic.normalizedCustomLists
import com.tankobun.app.logic.preferredVisibleSources
import com.tankobun.app.logic.previousInReadingOrderBefore
import com.tankobun.app.logic.readerLoadErrorFor
import com.tankobun.app.logic.readerSourceForChapter
import com.tankobun.app.logic.recommendationPageCount
import com.tankobun.app.logic.renamedCustomList
import com.tankobun.app.logic.sourceSettingsKey
import com.tankobun.app.logic.isFatalSourceSearchError
import com.tankobun.app.logic.sourcePickerDefaultSearchTitle
import com.tankobun.app.logic.sourcePickerDiagnosticDetail
import com.tankobun.app.logic.sourcePickerErrorMessage
import com.tankobun.app.logic.sourceMatchKey
import com.tankobun.app.logic.sourceSearchQueries
import com.tankobun.app.logic.sourceSearchRankTitleVariants
import com.tankobun.app.logic.toAniListScore
import com.tankobun.app.logic.userMessage
import com.tankobun.app.logic.visibleSources
import com.tankobun.app.logic.withAniListTitleLanguage
import com.tankobun.app.logic.withRecomputedTrackingDirty
import com.tankobun.app.logic.withoutCustomList
import com.tankobun.app.state.ExtensionInstallRequest
import com.tankobun.app.state.LibraryItem
import com.tankobun.app.state.ReaderChapterSegment
import com.tankobun.app.state.ReaderLoadError
import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.app.state.TankobunUiState

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tankobun.core.anilist.AnilistViewer
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tankobun.core.anilist.AnilistOAuth
import com.tankobun.core.database.SyncMutationEntity
import com.tankobun.core.database.toEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.database.toReaderPage
import com.tankobun.core.database.AnilistSearchResultEntity
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMediaPage
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.reader.ReaderProgressCalculator
import com.tankobun.core.reader.ReaderSession
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
import com.tankobun.core.model.SyncMutationType
import com.tankobun.core.model.withTitleLanguage
import com.tankobun.core.sync.SyncBackoff
import com.tankobun.core.sync.SyncMutationFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val BROWSE_LANDING_SECTION_SIZE = 12
private const val BROWSE_RESULTS_PAGE_SIZE = 50

private data class ReaderPagePosition(
    val chapterUrl: String,
    val pageIndex: Int,
    val pageScrollOffset: Int,
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

private enum class ReaderSegmentDirection {
    PREVIOUS,
    NEXT,
}


class MainViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val progressCalculator = ReaderProgressCalculator()
    private val syncMutationFactory = SyncMutationFactory()
    private val syncBackoff = SyncBackoff()
    private val cachePolicy = CachePolicy()
    private val backupService = AniListBackupService(container)
    private var trackingAutoSaveJob: Job? = null
    private var pendingAniListSyncJob: Job? = null
    private var scheduledBackupJob: Job? = null
    private var readerAdjacentLoadJob: Job? = null
    private var sourcePickerJob: Job? = null
    private var sourcePickerRequestId: Long = 0L
    private var lastReaderProgressSavedAtEpochMillis: Long = 0L
    private var latestReaderPosition: ReaderPagePosition? = null
    private val readerPageCacheJobs = ConcurrentHashMap<String, Job>()
    private val _state = MutableStateFlow(
        TankobunUiState(
            loggedIn = container.tokenStore.accessToken() != null,
            clientConfigured = BuildConfig.ANILIST_CLIENT_ID.isNotBlank(),
            viewerName = container.settingsStore.viewerName(),
            anilistScoreFormat = container.settingsStore.anilistScoreFormat(),
            anilistTitleLanguage = container.settingsStore.anilistTitleLanguage(),
            anilistCustomLists = container.settingsStore.anilistCustomLists(),
            librarySyncedAtEpochMillis = container.settingsStore.librarySyncedAtEpochMillis(),
            backupFolderUri = container.settingsStore.backupFolderUri(),
            backupSchedule = container.settingsStore.backupSchedule(),
            lastScheduledBackupAtEpochMillis = container.settingsStore.lastScheduledBackupAtEpochMillis(),
            libraryViewMode = container.settingsStore.libraryViewMode(),
            libraryCoverColumns = container.settingsStore.libraryCoverColumns(),
            libraryShowWholeCovers = container.settingsStore.libraryShowWholeCovers(),
            browseViewMode = container.settingsStore.browseViewMode(),
            browseCoverColumns = container.settingsStore.browseCoverColumns(),
            browseShowWholeCovers = container.settingsStore.browseShowWholeCovers(),
            browseAvailableTags = container.settingsStore.anilistTags(),
            sourceLanguages = container.settingsStore.sourceLanguages(),
            disabledSourceKeys = container.settingsStore.disabledSourceKeys(),
            extensionRepositoryUrl = container.settingsStore.extensionRepositoryUrl(),
            themeMode = container.settingsStore.themeMode(),
            ignoreDisplayCutout = container.settingsStore.ignoreDisplayCutout(),
            showAppStatusBar = container.settingsStore.showAppStatusBar(),
            dockAlignment = container.settingsStore.dockAlignment(),
            onboardingVisible = !container.settingsStore.onboardingCompleted(),
            readerTutorialVisible = !container.settingsStore.readerTutorialCompleted(),
            readerMode = container.settingsStore.readerMode(),
            readerPageGapLevel = container.settingsStore.readerPageGapLevel(),
            keepNextTenDownloads = container.settingsStore.keepNextTenDownloads(),
            anilistAutoSaveTrackingChanges = container.settingsStore.anilistAutoSaveTrackingChanges(),
            anilistAutoSyncReaderProgress = container.settingsStore.anilistAutoSyncReaderProgress(),
            anilistSyncManualReadProgress = container.settingsStore.anilistSyncManualReadProgress(),
        ),
    )
    val state: StateFlow<TankobunUiState> = _state

    init {
        viewModelScope.launch {
            container.database.downloadDao().observeDownloads().collect { rows ->
                val downloads = rows.map { row -> row.toModel() }
                val storageSummary = loadDownloadStorageSummary(downloads)
                _state.update {
                    it.copy(
                        downloads = downloads,
                        downloadStorageSummary = storageSummary,
                    )
                }
            }
        }
        viewModelScope.launch {
            container.downloadCoordinator.schedulePending()
        }
        refreshInstalledSources()
        if (_state.value.extensionRepositoryUrl.isNotBlank()) {
            refreshExtensionIndex(silent = true)
        }
        if (_state.value.loggedIn) {
            refreshAniListViewer()
            loadCachedLibrary(syncIfEmpty = true)
            processPendingAniListSync()
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
        container.settingsStore.saveAnilistScoreFormat(AnilistScoreFormat.POINT_100)
        container.settingsStore.saveAnilistTitleLanguage(AnilistTitleLanguage.ROMAJI)
        container.settingsStore.saveAnilistCustomLists(emptyList())
        container.settingsStore.saveLibrarySyncedAtEpochMillis(0L)
        _state.update {
            it.copy(
                loggedIn = false,
                viewerName = null,
                anilistScoreFormat = AnilistScoreFormat.POINT_100,
                anilistTitleLanguage = AnilistTitleLanguage.ROMAJI,
                anilistCustomLists = emptyList(),
                library = emptyList(),
                libraryItems = emptyList(),
                librarySyncedAtEpochMillis = 0L,
                recentReadingProgress = emptyList(),
                message = "Signed out",
            )
        }
    }

    fun showOnboarding() {
        _state.update { it.copy(onboardingVisible = true) }
    }

    fun dismissOnboarding() {
        container.settingsStore.saveOnboardingCompleted(true)
        _state.update { it.copy(onboardingVisible = false) }
    }

    fun dismissReaderTutorial() {
        container.settingsStore.saveReaderTutorialCompleted(true)
        _state.update { it.copy(readerTutorialVisible = false) }
    }

    private fun refreshAniListViewer() {
        val token = container.tokenStore.accessToken() ?: return
        viewModelScope.launch {
            runCatching {
                container.anilistRepository.viewer(token)
            }.onSuccess { viewer ->
                saveAniListViewerSettings(viewer)
                _state.update {
                    it.withAniListTitleLanguage(viewer.titleLanguage).copy(
                        viewerName = viewer.name,
                        anilistScoreFormat = viewer.scoreFormat,
                        anilistCustomLists = viewer.mangaCustomLists,
                    )
                }
            }.onFailure { error ->
                Log.w(TAG, "AniList viewer refresh failed", error)
            }
        }
    }

    private fun saveAniListViewerSettings(viewer: AnilistViewer) {
        container.settingsStore.saveViewerName(viewer.name)
        container.settingsStore.saveAnilistScoreFormat(viewer.scoreFormat)
        container.settingsStore.saveAnilistTitleLanguage(viewer.titleLanguage)
        container.settingsStore.saveAnilistCustomLists(viewer.mangaCustomLists)
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

    fun setIgnoreDisplayCutout(enabled: Boolean) {
        container.settingsStore.saveIgnoreDisplayCutout(enabled)
        _state.update { it.copy(ignoreDisplayCutout = enabled) }
    }

    fun setShowAppStatusBar(enabled: Boolean) {
        container.settingsStore.saveShowAppStatusBar(enabled)
        _state.update { it.copy(showAppStatusBar = enabled) }
    }

    fun setDockAlignment(alignment: DockAlignment) {
        container.settingsStore.saveDockAlignment(alignment)
        _state.update { it.copy(dockAlignment = alignment) }
    }

    fun setLibraryViewMode(mode: MediaViewMode) {
        val supportedMode = mode.supportedMediaViewMode()
        container.settingsStore.saveLibraryViewMode(supportedMode)
        _state.update { it.copy(libraryViewMode = supportedMode) }
    }

    fun setLibraryCoverColumns(count: Int) {
        val supportedCount = count.supportedCoverColumns()
        container.settingsStore.saveLibraryCoverColumns(supportedCount)
        _state.update { it.copy(libraryCoverColumns = supportedCount) }
    }

    fun setLibraryShowWholeCovers(enabled: Boolean) {
        container.settingsStore.saveLibraryShowWholeCovers(enabled)
        _state.update { it.copy(libraryShowWholeCovers = enabled) }
    }

    fun setBrowseViewMode(mode: MediaViewMode) {
        val supportedMode = mode.supportedMediaViewMode()
        container.settingsStore.saveBrowseViewMode(supportedMode)
        _state.update { it.copy(browseViewMode = supportedMode) }
    }

    fun setBrowseCoverColumns(count: Int) {
        val supportedCount = count.supportedCoverColumns()
        container.settingsStore.saveBrowseCoverColumns(supportedCount)
        _state.update { it.copy(browseCoverColumns = supportedCount) }
    }

    fun setBrowseShowWholeCovers(enabled: Boolean) {
        container.settingsStore.saveBrowseShowWholeCovers(enabled)
        _state.update { it.copy(browseShowWholeCovers = enabled) }
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

    fun dismissMessage(message: String? = null) {
        _state.update {
            if (message == null || it.message == message) {
                it.copy(message = null)
            } else {
                it
            }
        }
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
            val titleLanguage = _state.value.anilistTitleLanguage
            val media = container.database.mediaDao().cachedMedia().associateBy { it.id }
            val entries = container.database.listEntryDao().cachedEntries()
            val items = entries.mapNotNull { entry ->
                media[entry.mediaId]?.toModel(titleLanguage)?.let { cachedMedia ->
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
                loadRecentReadingProgress()
                runScheduledAniListBackupIfDue()
            } else if (syncIfEmpty) {
                refreshLibrary()
            }
        }
    }

    private fun loadRecentReadingProgress() {
        viewModelScope.launch {
            val items = recentReadingProgressItems()
            _state.update { it.copy(recentReadingProgress = items) }
        }
    }

    private suspend fun recentReadingProgressItems(): List<RecentReadingProgress> {
        val latestProgress = container.database.progressDao().latestReadingProgress(RECENT_READING_LIMIT)
            .map { it.toModel() }
        if (latestProgress.isEmpty()) return emptyList()
        val titleLanguage = _state.value.anilistTitleLanguage
        val mediaById = container.database.mediaDao().cachedMedia().associateBy { it.id }
        return latestProgress.mapNotNull { progress ->
            val media = mediaById[progress.mediaId]?.toModel(titleLanguage) ?: return@mapNotNull null
            RecentReadingProgress(
                media = media,
                progress = progress,
                chapter = container.database.chapterDao().cachedChapterByUrl(progress.chapterUrl)?.toModel(),
            )
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
                val entries = container.anilistRepository.mangaList(
                    accessToken = token,
                    userId = viewer.id,
                    scoreFormat = viewer.scoreFormat,
                )
                val preferredEntries = entries.map { (media, entry) ->
                    media.withTitleLanguage(viewer.titleLanguage) to entry
                }
                val now = System.currentTimeMillis()
                container.database.mediaDao().upsertMedia(preferredEntries.map { it.first.toEntity(now) })
                container.database.listEntryDao().upsertEntries(preferredEntries.map { it.second.toEntity(now) })
                val entryIds = preferredEntries.map { it.second.id }
                if (entryIds.isEmpty()) {
                    container.database.listEntryDao().deleteAllEntries()
                } else {
                    container.database.listEntryDao().deleteEntriesNotIn(entryIds)
                }
                saveAniListViewerSettings(viewer)
                container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
                _state.update {
                    it.copy(
                        viewerName = viewer.name,
                        anilistScoreFormat = viewer.scoreFormat,
                        anilistTitleLanguage = viewer.titleLanguage,
                        anilistCustomLists = viewer.mangaCustomLists,
                        library = preferredEntries.map { pair -> pair.first },
                        libraryItems = preferredEntries.map { (media, entry) -> LibraryItem(media, entry) },
                        librarySyncedAtEpochMillis = now,
                        busy = false,
                        message = "Library synced",
                    )
                }
                loadRecentReadingProgress()
                processPendingAniListSync()
                runScheduledAniListBackupIfDue()
            }.onFailure { error ->
                Log.e(TAG, "AniList library sync failed", error)
                _state.update {
                    it.copy(busy = false, message = error.userMessage("Library sync failed"))
                }
            }
        }
    }

    fun saveAniListBackup(uri: Uri) {
        val snapshot = _state.value
        val items = snapshot.libraryItems
        if (items.isEmpty()) {
            _state.update { it.copy(message = "Sync AniList before creating a backup") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                backupService.saveBackup(
                    uri = uri,
                    items = items,
                    viewerName = snapshot.viewerName,
                    scoreFormat = snapshot.anilistScoreFormat,
                )
            }.onSuccess {
                _state.update {
                    it.copy(
                        busy = false,
                        message = "AniList backup saved (${items.size} manga)",
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList backup failed", error)
                _state.update { it.copy(busy = false, message = error.userMessage("Backup failed")) }
            }
        }
    }

    fun restoreAniListBackup(uri: Uri) {
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = "Connect AniList before restoring a backup") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                backupService.restoreBackup(
                    uri = uri,
                    accessToken = token,
                    scoreFormat = _state.value.anilistScoreFormat,
                    knownCustomLists = _state.value.anilistCustomLists,
                )
            }.onSuccess { result ->
                container.settingsStore.saveAnilistCustomLists(result.customLists)
                loadCachedLibrary()
                _state.update {
                    it.copy(
                        anilistCustomLists = result.customLists,
                        busy = false,
                        message = buildList {
                            add("Restored ${result.restored} manga")
                            if (result.skipped > 0) add("${result.skipped} skipped")
                        }.joinToString(" / "),
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList backup restore failed", error)
                _state.update { it.copy(busy = false, message = error.userMessage("Restore failed")) }
            }
        }
    }

    fun setScheduledBackupFolder(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val persisted = runCatching {
            container.application.contentResolver.takePersistableUriPermission(uri, flags)
        }.onFailure { error ->
            Log.w(TAG, "Could not persist backup folder permission", error)
        }.isSuccess
        if (!persisted) {
            _state.update {
                it.copy(message = "Could not keep access to that folder. Choose another writable folder.")
            }
            return
        }
        val uriString = uri.toString()
        container.settingsStore.saveBackupFolderUri(uriString)
        _state.update { it.copy(backupFolderUri = uriString, message = "Backup folder selected") }
        ScheduledBackupWork.update(container.application, _state.value.backupSchedule)
        if (_state.value.backupSchedule != BackupSchedule.OFF) {
            runScheduledAniListBackupIfDue(force = true, reportResult = true)
        }
    }

    fun setBackupSchedule(schedule: BackupSchedule) {
        container.settingsStore.saveBackupSchedule(schedule)
        _state.update { it.copy(backupSchedule = schedule) }
        ScheduledBackupWork.update(container.application, schedule)
        runScheduledAniListBackupIfDue(force = true, reportResult = schedule != BackupSchedule.OFF)
    }

    fun runScheduledAniListBackupNow() {
        if (_state.value.backupFolderUri == null) {
            _state.update { it.copy(message = "Choose a backup folder first") }
            return
        }
        runScheduledAniListBackupIfDue(
            force = true,
            reportResult = true,
            requireSchedule = false,
        )
    }

    private fun runScheduledAniListBackupIfDue(
        force: Boolean = false,
        reportResult: Boolean = false,
        requireSchedule: Boolean = true,
    ) {
        val snapshot = _state.value
        val schedule = snapshot.backupSchedule
        if (schedule == BackupSchedule.OFF && requireSchedule) return
        val folderUri = snapshot.backupFolderUri?.let(Uri::parse)
        if (folderUri == null) {
            if (reportResult) {
                _state.update { it.copy(message = "Choose a backup folder first") }
            }
            return
        }
        if (snapshot.libraryItems.isEmpty()) {
            if (reportResult) {
                _state.update { it.copy(message = "Sync AniList before creating a backup") }
            }
            return
        }
        val now = System.currentTimeMillis()
        if (!force && !schedule.isDue(lastRunAt = snapshot.lastScheduledBackupAtEpochMillis, now = now)) return
        if (scheduledBackupJob?.isActive == true) {
            if (reportResult) {
                _state.update { it.copy(message = "Backup is already running") }
            }
            return
        }

        scheduledBackupJob = viewModelScope.launch {
            if (reportResult) {
                _state.update { it.copy(busy = true, message = null) }
            }
            runCatching {
                backupService.writeScheduledBackup(folderUri = folderUri, snapshot = snapshot)
            }.onSuccess { count ->
                container.settingsStore.saveLastScheduledBackupAtEpochMillis(now)
                _state.update {
                    it.copy(
                        lastScheduledBackupAtEpochMillis = now,
                        busy = if (reportResult) false else it.busy,
                        message = if (reportResult) "Scheduled backup saved ($count manga)" else it.message,
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Scheduled AniList backup failed", error)
                _state.update {
                    it.copy(
                        busy = if (reportResult) false else it.busy,
                        message = if (reportResult) error.userMessage("Scheduled backup failed") else it.message,
                    )
                }
            }
        }
    }

    private fun processPendingAniListSync() {
        val token = container.tokenStore.accessToken() ?: return
        if (!_state.value.anilistAutoSyncReaderProgress) return
        if (pendingAniListSyncJob?.isActive == true) return
        pendingAniListSyncJob = viewModelScope.launch {
            val dao = container.database.syncMutationDao()
            val mutations = dao.dueMutations(System.currentTimeMillis())
            mutations.forEach { mutation ->
                runCatching {
                    processAniListSyncMutation(token, mutation)
                }.onSuccess {
                    dao.deleteMutation(mutation)
                }.onFailure { error ->
                    Log.w(TAG, "Queued AniList sync failed for ${mutation.mediaId}", error)
                    val now = System.currentTimeMillis()
                    dao.upsertMutation(
                        mutation.copy(
                            attempts = mutation.attempts + 1,
                            nextAttemptAtEpochMillis = now + syncBackoff.nextDelayMillis(mutation.attempts),
                        ),
                    )
                }
            }
        }
    }

    private suspend fun processAniListSyncMutation(token: String, mutation: SyncMutationEntity) {
        when (mutation.type) {
            SyncMutationType.SAVE_MEDIA_LIST_ENTRY -> {
                val payload = JSONObject(mutation.payloadJson)
                val entry = container.anilistRepository.saveListEntry(
                    accessToken = token,
                    mediaId = mutation.mediaId,
                    status = payload.nullableString("status")?.let { status ->
                        runCatching { MediaStatus.valueOf(status) }.getOrNull()
                    },
                    progress = payload.nullableInt("progress"),
                    score = payload.nullableDouble("score"),
                    notes = payload.nullableString("notes"),
                    private = payload.nullableBoolean("private"),
                    customLists = payload.nullableStringList("customLists"),
                    scoreFormat = _state.value.anilistScoreFormat,
                )
                val now = System.currentTimeMillis()
                val media = container.database.mediaDao()
                    .cachedMedia(mutation.mediaId)
                    ?.toModel(_state.value.anilistTitleLanguage)
                    ?: _state.value.selectedMedia?.takeIf { it.id == mutation.mediaId }
                container.database.listEntryDao().upsertEntry(entry.toEntity(now))
                applySyncedListEntry(media, entry, updateTrackingForm = false)
            }
            SyncMutationType.DELETE_MEDIA_LIST_ENTRY -> {
                Log.w(TAG, "Ignoring unsupported queued AniList delete for ${mutation.mediaId}")
            }
        }
    }

    private suspend fun syncAniListProgressFromChapter(
        media: AnilistMedia,
        chapterProgress: Int,
        triggeredByManualRead: Boolean,
    ) {
        if (chapterProgress <= 0) return
        val snapshot = _state.value
        if (!snapshot.anilistAutoSyncReaderProgress) return
        if (triggeredByManualRead && !snapshot.anilistSyncManualReadProgress) return
        val trackedProgress = snapshot.trackingProgress.toIntOrNull()
            ?: snapshot.selectedListEntry?.progress
            ?: 0
        if (snapshot.selectedMedia?.id == media.id && chapterProgress <= trackedProgress) return

        val now = System.currentTimeMillis()
        val token = container.tokenStore.accessToken()
        if (token == null) {
            queueAniListProgressMutation(media.id, chapterProgress, now)
            return
        }

        runCatching {
            container.anilistRepository.saveListEntry(
                accessToken = token,
                mediaId = media.id,
                status = null,
                progress = chapterProgress,
                score = null,
                notes = null,
                private = null,
                customLists = null,
                scoreFormat = snapshot.anilistScoreFormat,
            )
        }.onSuccess { entry ->
            container.database.listEntryDao().upsertEntry(entry.toEntity(now))
            applySyncedListEntry(media, entry, updateTrackingForm = false)
        }.onFailure { error ->
            Log.w(TAG, "AniList progress sync failed for ${media.id}", error)
            queueAniListProgressMutation(media.id, chapterProgress, now)
        }
    }

    private suspend fun queueAniListProgressMutation(mediaId: Int, progress: Int, nowMillis: Long) {
        val mutation = syncMutationFactory.saveMediaListEntry(
            mediaId = mediaId,
            progress = progress,
            nowMillis = nowMillis,
        )
        container.database.syncMutationDao().upsertMutation(mutation.toEntity())
    }

    private fun applySyncedListEntry(
        media: AnilistMedia?,
        entry: AnilistListEntry,
        updateTrackingForm: Boolean,
    ) {
        _state.update { state ->
            val existingMedia = media
                ?: state.libraryItems.firstOrNull { it.media.id == entry.mediaId }?.media
                ?: state.selectedMedia?.takeIf { it.id == entry.mediaId }
            val nextItems = if (existingMedia == null) {
                state.libraryItems
            } else {
                (state.libraryItems.filterNot { item -> item.media.id == entry.mediaId } + LibraryItem(existingMedia, entry))
                    .sortedBy { item -> item.media.title.userPreferred.lowercase(Locale.ROOT) }
            }
            val selected = state.selectedMedia?.id == entry.mediaId
            state.copy(
                library = nextItems.map { it.media },
                libraryItems = nextItems,
                selectedListEntry = if (selected) entry else state.selectedListEntry,
                trackingStatus = if (selected && updateTrackingForm) entry.status else state.trackingStatus,
                trackingProgress = if (selected) {
                    if (updateTrackingForm) {
                        entry.progress.toString()
                    } else {
                        maxOf(state.trackingProgress.toIntOrNull() ?: 0, entry.progress).toString()
                    }
                } else {
                    state.trackingProgress
                },
                trackingScore = if (selected && updateTrackingForm) {
                    entry.score.formatTrackingScore(state.anilistScoreFormat)
                } else {
                    state.trackingScore
                },
                trackingNotes = if (selected && updateTrackingForm) entry.notes.orEmpty() else state.trackingNotes,
                trackingPrivate = if (selected && updateTrackingForm) entry.private else state.trackingPrivate,
                trackingCustomLists = if (selected && updateTrackingForm) entry.customLists.toSet() else state.trackingCustomLists,
                trackingDirty = if (selected && updateTrackingForm) false else state.trackingDirty,
                trackingSaveInProgress = if (selected) false else state.trackingSaveInProgress,
                trackingSaveFailed = if (selected && updateTrackingForm) false else state.trackingSaveFailed,
            )
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query, browseStaffName = null) }
    }

    fun setBrowseGenre(genre: String, selected: Boolean) {
        _state.update {
            it.copy(
                browseStaffName = null,
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
                browseStaffName = null,
                browseTags = if (selected) {
                    it.browseTags + tag
                } else {
                    it.browseTags - tag
                },
            )
        }
    }

    fun setBrowseFormat(format: String?) {
        _state.update { it.copy(browseFormat = format, browseStaffName = null) }
    }

    fun setBrowsePublishingStatus(status: String?) {
        _state.update { it.copy(browsePublishingStatus = status, browseStaffName = null) }
    }

    fun setBrowseCountryOfOrigin(country: String?) {
        _state.update { it.copy(browseCountryOfOrigin = country, browseStaffName = null) }
    }

    fun setBrowseYear(year: Int?) {
        _state.update { it.copy(browseYear = year, browseStaffName = null) }
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
                browseResultsPage = 0,
                browseResultsHasMore = false,
                browseResultsLoadingMore = false,
                browseGenres = emptySet(),
                browseTags = emptySet(),
                browseFormat = null,
                browsePublishingStatus = null,
                browseCountryOfOrigin = null,
                browseYear = null,
                browseStaffName = null,
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
                browseResultsPage = 0,
                browseResultsHasMore = false,
                browseResultsLoadingMore = false,
                browseGenres = emptySet(),
                browseTags = emptySet(),
                browseFormat = null,
                browsePublishingStatus = null,
                browseCountryOfOrigin = null,
                browseYear = null,
                browseStaffName = null,
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
                browseResultsPage = 0,
                browseResultsHasMore = false,
                browseResultsLoadingMore = false,
                browseGenres = emptySet(),
                browseTags = emptySet(),
                browseFormat = null,
                browsePublishingStatus = null,
                browseCountryOfOrigin = "KR",
                browseYear = null,
                browseStaffName = null,
                browseSort = "POPULARITY_DESC",
                message = null,
            )
        }
        searchAniList()
    }

    fun browseByTag(tag: String) {
        _state.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                browseSearched = false,
                browseResultsPage = 0,
                browseResultsHasMore = false,
                browseResultsLoadingMore = false,
                browseGenres = emptySet(),
                browseTags = setOf(tag),
                browseFormat = null,
                browsePublishingStatus = null,
                browseCountryOfOrigin = null,
                browseYear = null,
                browseStaffName = null,
                browseSort = BROWSE_SORT_SEARCH_MATCH,
                message = null,
            )
        }
        searchAniList()
    }

    fun browseByAuthor(author: String) {
        _state.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                browseSearched = false,
                browseResultsPage = 0,
                browseResultsHasMore = false,
                browseResultsLoadingMore = false,
                browseGenres = emptySet(),
                browseTags = emptySet(),
                browseFormat = null,
                browsePublishingStatus = null,
                browseCountryOfOrigin = null,
                browseYear = null,
                browseStaffName = author,
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
            val cachedLanding = BrowseLandingData(
                trending = cachedBrowseMedia(BROWSE_TRENDING_CACHE_KEY),
                popular = cachedBrowseMedia(BROWSE_POPULAR_CACHE_KEY),
                popularManhwa = cachedBrowseMedia(BROWSE_MANHWA_CACHE_KEY),
                topManga = cachedBrowseMedia(BROWSE_TOP_MANGA_CACHE_KEY).take(BROWSE_LANDING_SECTION_SIZE),
            )
            val hasCachedLanding = cachedLanding.hasContent()
            if (hasCachedLanding) {
                _state.update {
                    it.copy(
                        browseTrending = cachedLanding.trending,
                        browsePopular = cachedLanding.popular,
                        browsePopularManhwa = cachedLanding.popularManhwa,
                        browseTopManga = cachedLanding.topManga,
                        browseLandingLoaded = true,
                        busy = false,
                    )
                }
            }
            runCatching {
                val accessToken = container.tokenStore.accessToken()
                val trending = cachedAnilistBrowseMedia(BROWSE_TRENDING_CACHE_KEY) {
                    container.anilistRepository.browseManga(
                        sort = "TRENDING_DESC",
                        perPage = BROWSE_LANDING_SECTION_SIZE,
                        accessToken = accessToken,
                    )
                }
                val popular = cachedAnilistBrowseMedia(BROWSE_POPULAR_CACHE_KEY) {
                    container.anilistRepository.browseManga(
                        sort = "POPULARITY_DESC",
                        perPage = BROWSE_LANDING_SECTION_SIZE,
                        accessToken = accessToken,
                    )
                }
                val popularManhwa = cachedAnilistBrowseMedia(BROWSE_MANHWA_CACHE_KEY) {
                    container.anilistRepository.browseManga(
                        countryOfOrigin = "KR",
                        sort = "POPULARITY_DESC",
                        perPage = BROWSE_LANDING_SECTION_SIZE,
                        accessToken = accessToken,
                    )
                }
                val topManga = cachedAnilistBrowseMedia(BROWSE_TOP_MANGA_CACHE_KEY) {
                    container.anilistRepository.browseManga(
                        sort = "SCORE_DESC",
                        perPage = BROWSE_LANDING_SECTION_SIZE,
                        accessToken = accessToken,
                    )
                }.take(BROWSE_LANDING_SECTION_SIZE)
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
                    it.copy(
                        busy = false,
                        message = if (hasCachedLanding) it.message else error.userMessage("Browse failed"),
                    )
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
            _state.update {
                it.copy(
                    searchResults = emptyList(),
                    browseSearched = false,
                    browseResultsPage = 0,
                    browseResultsHasMore = false,
                    browseResultsLoadingMore = false,
                    message = null,
                )
            }
            loadBrowseLanding()
            return
        }
        val cacheKey = snapshot.browseCacheKey()
        viewModelScope.launch {
            _state.update {
                it.copy(
                    busy = true,
                    browseSearched = true,
                    browseResultsPage = 0,
                    browseResultsHasMore = false,
                    browseResultsLoadingMore = false,
                    message = null,
                )
            }
            runCatching {
                cachedAnilistBrowseMediaPage(cacheKey) {
                    fetchBrowseResultsPage(snapshot, page = 1)
                }
            }.onSuccess { page ->
                if (_state.value.browseCacheKey() == cacheKey) {
                    _state.update {
                        it.copy(
                            searchResults = page.media,
                            browseResultsPage = page.currentPage,
                            browseResultsHasMore = page.hasNextPage,
                            browseResultsLoadingMore = false,
                            busy = false,
                        )
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList search failed for $query", error)
                if (_state.value.browseCacheKey() == cacheKey) {
                    _state.update {
                        it.copy(
                            busy = false,
                            browseResultsLoadingMore = false,
                            message = error.userMessage("Search failed"),
                        )
                    }
                }
            }
        }
    }

    fun loadMoreBrowseResults() {
        val snapshot = _state.value
        if (
            !snapshot.browseSearched ||
            !snapshot.browseResultsHasMore ||
            snapshot.busy ||
            snapshot.browseResultsLoadingMore ||
            !snapshot.hasBrowseQueryOrFilters()
        ) {
            return
        }
        val cacheKey = snapshot.browseCacheKey()
        val nextPage = snapshot.browseResultsPage.coerceAtLeast(1) + 1
        viewModelScope.launch {
            _state.update {
                if (
                    it.browseCacheKey() == cacheKey &&
                    it.browseResultsHasMore &&
                    !it.browseResultsLoadingMore &&
                    !it.busy
                ) {
                    it.copy(browseResultsLoadingMore = true, message = null)
                } else {
                    it
                }
            }
            runCatching {
                fetchBrowseResultsPage(snapshot, page = nextPage)
            }.onSuccess { page ->
                if (_state.value.browseCacheKey() != cacheKey) return@onSuccess
                val merged = (_state.value.searchResults + page.media).distinctBy { it.id }
                cacheBrowseMedia(cacheKey, merged)
                _state.update {
                    it.copy(
                        searchResults = merged,
                        browseResultsPage = page.currentPage.coerceAtLeast(nextPage),
                        browseResultsHasMore = page.hasNextPage && page.media.isNotEmpty(),
                        browseResultsLoadingMore = false,
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList browse page $nextPage failed for $cacheKey", error)
                if (_state.value.browseCacheKey() == cacheKey) {
                    _state.update {
                        it.copy(
                            browseResultsLoadingMore = false,
                            message = error.userMessage("Could not load more manga"),
                        )
                    }
                }
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
            val titleLanguage = _state.value.anilistTitleLanguage
            return container.database.searchResultDao().cachedSearchMedia(cacheKey).map { it.toModel(titleLanguage) }
        }
        val titleLanguage = _state.value.anilistTitleLanguage
        val results = fetch().map { it.withTitleLanguage(titleLanguage) }
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

    private suspend fun cachedAnilistBrowseMediaPage(
        cacheKey: String,
        fetch: suspend () -> AnilistMediaPage,
    ): AnilistMediaPage {
        val now = System.currentTimeMillis()
        val cachedRows = container.database.searchResultDao().cachedSearchRows(cacheKey)
        val cachedIsFresh = cachedRows.isNotEmpty() &&
            cachedRows.all { now - it.fetchedAtEpochMillis <= cachePolicy.anilistSearchTtlMillis }
        if (cachedIsFresh) {
            val titleLanguage = _state.value.anilistTitleLanguage
            val cachedMedia = container.database.searchResultDao()
                .cachedSearchMedia(cacheKey)
                .map { it.toModel(titleLanguage) }
            val cachedPage = if (cachedMedia.size < BROWSE_RESULTS_PAGE_SIZE) {
                0
            } else {
                ((cachedMedia.size - 1) / BROWSE_RESULTS_PAGE_SIZE + 1).coerceAtLeast(1)
            }
            return AnilistMediaPage(
                media = cachedMedia,
                currentPage = cachedPage,
                hasNextPage = cachedMedia.isNotEmpty() &&
                    (cachedMedia.size < BROWSE_RESULTS_PAGE_SIZE || cachedMedia.size % BROWSE_RESULTS_PAGE_SIZE == 0),
            )
        }
        val page = fetch()
        cacheBrowseMedia(cacheKey, page.media)
        return page
    }

    private suspend fun fetchBrowseResultsPage(snapshot: TankobunUiState, page: Int): AnilistMediaPage {
        val query = snapshot.searchQuery.trim()
        val staffName = snapshot.browseStaffName?.trim().orEmpty()
        val accessToken = container.tokenStore.accessToken()
        return when {
            staffName.isNotBlank() -> container.anilistRepository.staffMangaPage(
                staffName = staffName,
                sort = snapshot.effectiveBrowseSort(),
                page = page,
                perPage = BROWSE_RESULTS_PAGE_SIZE,
                accessToken = accessToken,
            )
            !snapshot.hasBrowseFilters() && snapshot.browseSort == BROWSE_SORT_SEARCH_MATCH -> {
                container.anilistRepository.searchMangaPage(
                    query = query,
                    page = page,
                    perPage = BROWSE_RESULTS_PAGE_SIZE,
                    accessToken = accessToken,
                )
            }
            else -> container.anilistRepository.browseMangaPage(
                search = query.takeIf { it.isNotBlank() },
                genres = snapshot.browseGenres,
                tags = snapshot.browseTags,
                format = snapshot.browseFormat,
                status = snapshot.browsePublishingStatus,
                countryOfOrigin = snapshot.browseCountryOfOrigin,
                year = snapshot.browseYear,
                sort = snapshot.effectiveBrowseSort(),
                page = page,
                perPage = BROWSE_RESULTS_PAGE_SIZE,
                accessToken = accessToken,
            )
        }.withTitleLanguage(snapshot.anilistTitleLanguage)
    }

    private suspend fun cacheBrowseMedia(cacheKey: String, media: List<AnilistMedia>) {
        val now = System.currentTimeMillis()
        container.database.mediaDao().upsertMedia(media.map { it.toEntity(now) })
        container.database.searchResultDao().deleteForQuery(cacheKey)
        container.database.searchResultDao().upsertResults(
            media.mapIndexed { index, item ->
                AnilistSearchResultEntity(
                    query = cacheKey,
                    mediaId = item.id,
                    orderIndex = index,
                    fetchedAtEpochMillis = now,
                )
            },
        )
    }

    private suspend fun cachedBrowseMedia(cacheKey: String): List<AnilistMedia> =
        container.database.searchResultDao()
            .cachedSearchMedia(cacheKey)
            .map { it.toModel(_state.value.anilistTitleLanguage) }

    private fun BrowseLandingData.hasContent(): Boolean =
        trending.isNotEmpty() || popular.isNotEmpty() || popularManhwa.isNotEmpty() || topManga.isNotEmpty()

    fun selectMedia(media: AnilistMedia) {
        val titleLanguage = _state.value.anilistTitleLanguage
        val displayMedia = media.withTitleLanguage(titleLanguage)
        val existingEntry = _state.value.libraryItems.firstOrNull { item -> item.media.id == media.id }?.entry
        _state.update {
            it.copy(
                selectedMedia = displayMedia,
                sourceMatches = emptyList(),
                sourceMatchChapterCounts = emptyMap(),
                sourcePickerOpen = false,
                sourcePickerLoading = false,
                sourcePickerMessage = null,
                sourcePickerDiagnostics = emptyList(),
                sourcePickerSearchTitle = "",
                selectedListEntry = existingEntry,
                selectedRecommendations = emptyList(),
                selectedRecommendationsPage = 0,
                selectedRecommendationsHasMore = false,
                recommendationsLoading = false,
                trackingStatus = existingEntry?.status ?: MediaStatus.PLANNING,
                trackingProgress = (existingEntry?.progress ?: 0).toString(),
                trackingScore = existingEntry?.score.formatTrackingScore(it.anilistScoreFormat),
                trackingNotes = existingEntry?.notes.orEmpty(),
                trackingPrivate = existingEntry?.private ?: false,
                trackingCustomLists = existingEntry?.customLists.orEmpty().toSet(),
                trackingDirty = false,
                trackingSaveInProgress = false,
                trackingSaveFailed = false,
                selectedSourceManga = null,
                sourceChapters = emptyList(),
                latestProgress = null,
                chapterProgress = emptyMap(),
                activeChapter = null,
                readerPages = emptyList(),
                readerPreviousSegment = null,
                readerNextSegment = null,
                readerError = null,
                currentPageIndex = 0,
                currentPageScrollOffset = 0,
                selectingDownloadChapters = false,
                selectedDownloadChapterUrls = emptySet(),
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
                chapterProgress = it.chapterProgress.takeIf { sameSource }.orEmpty(),
                activeChapter = null,
                readerPages = emptyList(),
                readerPreviousSegment = null,
                readerNextSegment = null,
                readerError = null,
                currentPageIndex = 0,
                currentPageScrollOffset = 0,
                selectingDownloadChapters = false,
                selectedDownloadChapterUrls = emptySet(),
            )
        }
    }

    fun clearSelectedMedia() {
        _state.update {
            if (it.selectedMedia == null) {
                it
            } else {
                it.copy(
                    selectedMedia = null,
                    sourceMatches = emptyList(),
                    sourceMatchChapterCounts = emptyMap(),
                    sourcePickerOpen = false,
                    sourcePickerLoading = false,
                    sourcePickerMessage = null,
                    sourcePickerDiagnostics = emptyList(),
                    sourcePickerSearchTitle = "",
                    selectedListEntry = null,
                    selectedRecommendations = emptyList(),
                    selectedRecommendationsPage = 0,
                    selectedRecommendationsHasMore = false,
                    recommendationsLoading = false,
                    trackingDirty = false,
                    trackingSaveInProgress = false,
                    trackingSaveFailed = false,
                    selectedSourceManga = null,
                    sourceChapters = emptyList(),
                    latestProgress = null,
                    chapterProgress = emptyMap(),
                    activeChapter = null,
                    readerPages = emptyList(),
                    readerPreviousSegment = null,
                    readerNextSegment = null,
                    readerError = null,
                    currentPageIndex = 0,
                    currentPageScrollOffset = 0,
                    selectingDownloadChapters = false,
                    selectedDownloadChapterUrls = emptySet(),
                    message = null,
                )
            }
        }
    }

    fun setReaderMode(mode: ReaderMode) {
        container.settingsStore.saveReaderMode(mode)
        _state.update { it.copy(readerMode = mode) }
        saveReaderProgress()
        if (mode == ReaderMode.WEBTOON) {
            val snapshot = _state.value
            val media = snapshot.selectedMedia
            val chapter = snapshot.activeChapter
            if (media != null && chapter != null) {
                loadAdjacentReaderSegments(media.id, chapter)
            }
        }
    }

    fun setReaderPageGapLevel(level: Int) {
        val normalized = level.coerceIn(0, 3)
        container.settingsStore.saveReaderPageGapLevel(normalized)
        _state.update { it.copy(readerPageGapLevel = normalized) }
    }

    fun setAnilistAutoSaveTrackingChanges(enabled: Boolean) {
        container.settingsStore.saveAnilistAutoSaveTrackingChanges(enabled)
        _state.update { it.copy(anilistAutoSaveTrackingChanges = enabled) }
        if (enabled) scheduleTrackingAutoSave()
    }

    fun setAnilistAutoSyncReaderProgress(enabled: Boolean) {
        container.settingsStore.saveAnilistAutoSyncReaderProgress(enabled)
        _state.update { it.copy(anilistAutoSyncReaderProgress = enabled) }
        if (enabled) processPendingAniListSync()
    }

    fun setAnilistSyncManualReadProgress(enabled: Boolean) {
        container.settingsStore.saveAnilistSyncManualReadProgress(enabled)
        _state.update { it.copy(anilistSyncManualReadProgress = enabled) }
    }

    fun setAnilistTitleLanguage(language: AnilistTitleLanguage) {
        if (_state.value.anilistTitleLanguage == language) return
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = "Connect AniList before changing account preferences") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                container.anilistRepository.updateUserPreferences(
                    accessToken = token,
                    titleLanguage = language,
                )
            }.onSuccess { viewer ->
                saveAniListViewerSettings(viewer)
                _state.update {
                    it.withAniListTitleLanguage(viewer.titleLanguage).copy(
                        viewerName = viewer.name,
                        anilistScoreFormat = viewer.scoreFormat,
                        anilistCustomLists = viewer.mangaCustomLists,
                        busy = false,
                        message = "AniList title language saved",
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList title language update failed", error)
                _state.update {
                    it.copy(
                        busy = false,
                        message = error.userMessage("Could not update AniList title language"),
                    )
                }
            }
        }
    }

    fun setAnilistScoreFormat(format: AnilistScoreFormat) {
        if (_state.value.anilistScoreFormat == format) return
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = "Connect AniList before changing account preferences") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                container.anilistRepository.updateUserPreferences(
                    accessToken = token,
                    scoreFormat = format,
                )
            }.onSuccess { viewer ->
                saveAniListViewerSettings(viewer)
                _state.update {
                    it.copy(
                        viewerName = viewer.name,
                        anilistScoreFormat = viewer.scoreFormat,
                        anilistTitleLanguage = viewer.titleLanguage,
                        anilistCustomLists = viewer.mangaCustomLists,
                        trackingScore = it.selectedListEntry?.score.formatTrackingScore(viewer.scoreFormat),
                        busy = false,
                        message = "AniList rating format saved",
                    )
                }
                refreshLibrary()
            }.onFailure { error ->
                Log.e(TAG, "AniList rating format update failed", error)
                _state.update {
                    it.copy(
                        busy = false,
                        message = error.userMessage("Could not update AniList rating format"),
                    )
                }
            }
        }
    }

    fun createAnilistCustomList(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = "Connect AniList before managing custom lists") }
            return
        }
        val currentLists = _state.value.anilistCustomLists.normalizedCustomLists()
        if (currentLists.any { it.equals(normalizedName, ignoreCase = true) }) {
            _state.update { it.copy(message = "Custom list already exists") }
            return
        }
        val nextLists = (currentLists + normalizedName).normalizedCustomLists()
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                container.anilistRepository.updateMangaCustomLists(token, nextLists)
                    .ifEmpty { nextLists }
                    .normalizedCustomLists()
            }.onSuccess { savedLists ->
                container.settingsStore.saveAnilistCustomLists(savedLists)
                _state.update {
                    it.copy(
                        anilistCustomLists = savedLists,
                        busy = false,
                        message = "Custom list created",
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList custom list create failed", error)
                _state.update { it.copy(busy = false, message = error.userMessage("Custom list create failed")) }
            }
        }
    }

    fun renameAnilistCustomList(oldName: String, newName: String) {
        val normalizedOldName = oldName.trim()
        val normalizedNewName = newName.trim()
        if (normalizedOldName.isBlank() || normalizedNewName.isBlank()) return
        if (normalizedOldName.equals(normalizedNewName, ignoreCase = true)) return
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = "Connect AniList before managing custom lists") }
            return
        }
        val snapshot = _state.value
        val currentLists = snapshot.anilistCustomLists.normalizedCustomLists()
        if (currentLists.any { it.equals(normalizedNewName, ignoreCase = true) && !it.equals(normalizedOldName, ignoreCase = true) }) {
            _state.update { it.copy(message = "A custom list with that name already exists") }
            return
        }
        val nextLists = currentLists
            .map { if (it.equals(normalizedOldName, ignoreCase = true)) normalizedNewName else it }
            .normalizedCustomLists()
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                val savedLists = container.anilistRepository.updateMangaCustomLists(token, nextLists)
                    .ifEmpty { nextLists }
                    .normalizedCustomLists()
                val affectedItems = snapshot.libraryItems.filter { item ->
                    item.entry.customLists.any { it.equals(normalizedOldName, ignoreCase = true) }
                }
                val now = System.currentTimeMillis()
                val updatedEntries = affectedItems.associate { item ->
                    val updatedCustomLists = item.entry.customLists.renamedCustomList(normalizedOldName, normalizedNewName)
                    item.media.id to container.anilistRepository.saveListEntry(
                        accessToken = token,
                        mediaId = item.media.id,
                        status = null,
                        progress = null,
                        score = null,
                        notes = null,
                        private = null,
                        customLists = updatedCustomLists,
                        scoreFormat = snapshot.anilistScoreFormat,
                    )
                }
                if (updatedEntries.isNotEmpty()) {
                    container.database.listEntryDao().upsertEntries(updatedEntries.values.map { it.toEntity(now) })
                }
                container.settingsStore.saveAnilistCustomLists(savedLists)
                savedLists to updatedEntries
            }.onSuccess { (savedLists, updatedEntries) ->
                _state.update {
                    val nextItems = it.libraryItems.map { item ->
                        val entry = updatedEntries[item.media.id]
                            ?: item.entry.copy(customLists = item.entry.customLists.renamedCustomList(normalizedOldName, normalizedNewName))
                        item.copy(entry = entry)
                    }.sortedBy { item -> item.media.title.userPreferred.lowercase(Locale.ROOT) }
                    it.copy(
                        anilistCustomLists = savedLists,
                        libraryItems = nextItems,
                        library = nextItems.map { item -> item.media },
                        selectedListEntry = it.selectedListEntry?.let { entry ->
                            updatedEntries[entry.mediaId]
                                ?: entry.copy(customLists = entry.customLists.renamedCustomList(normalizedOldName, normalizedNewName))
                        },
                        trackingCustomLists = it.trackingCustomLists.renamedCustomList(normalizedOldName, normalizedNewName).toSet(),
                        busy = false,
                        message = "Custom list renamed",
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList custom list rename failed", error)
                _state.update { it.copy(busy = false, message = error.userMessage("Custom list rename failed")) }
            }
        }
    }

    fun deleteAnilistCustomList(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = "Connect AniList before managing custom lists") }
            return
        }
        val snapshot = _state.value
        val currentLists = snapshot.anilistCustomLists.normalizedCustomLists()
        val nextLists = currentLists
            .filterNot { it.equals(normalizedName, ignoreCase = true) }
            .normalizedCustomLists()
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                val savedLists = container.anilistRepository.updateMangaCustomLists(token, nextLists)
                    .ifEmpty { nextLists }
                    .normalizedCustomLists()
                val affectedItems = snapshot.libraryItems.filter { item ->
                    item.entry.customLists.any { it.equals(normalizedName, ignoreCase = true) }
                }
                val now = System.currentTimeMillis()
                val updatedEntries = affectedItems.associate { item ->
                    val updatedCustomLists = item.entry.customLists.withoutCustomList(normalizedName)
                    item.media.id to container.anilistRepository.saveListEntry(
                        accessToken = token,
                        mediaId = item.media.id,
                        status = null,
                        progress = null,
                        score = null,
                        notes = null,
                        private = null,
                        customLists = updatedCustomLists,
                        scoreFormat = snapshot.anilistScoreFormat,
                    )
                }
                if (updatedEntries.isNotEmpty()) {
                    container.database.listEntryDao().upsertEntries(updatedEntries.values.map { it.toEntity(now) })
                }
                container.settingsStore.saveAnilistCustomLists(savedLists)
                savedLists to updatedEntries
            }.onSuccess { (savedLists, updatedEntries) ->
                _state.update {
                    val nextItems = it.libraryItems.map { item ->
                        val entry = updatedEntries[item.media.id]
                            ?: item.entry.copy(customLists = item.entry.customLists.withoutCustomList(normalizedName))
                        item.copy(entry = entry)
                    }.sortedBy { item -> item.media.title.userPreferred.lowercase(Locale.ROOT) }
                    it.copy(
                        anilistCustomLists = savedLists,
                        libraryItems = nextItems,
                        library = nextItems.map { item -> item.media },
                        selectedListEntry = it.selectedListEntry?.let { entry ->
                            updatedEntries[entry.mediaId]
                                ?: entry.copy(customLists = entry.customLists.withoutCustomList(normalizedName))
                        },
                        trackingCustomLists = it.trackingCustomLists.withoutCustomList(normalizedName).toSet(),
                        busy = false,
                        message = "Custom list deleted",
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList custom list delete failed", error)
                _state.update { it.copy(busy = false, message = error.userMessage("Custom list delete failed")) }
            }
        }
    }

    fun setTrackingStatus(status: MediaStatus) {
        _state.update { it.copy(trackingStatus = status).withRecomputedTrackingDirty() }
        scheduleTrackingAutoSave()
    }

    fun setTrackingProgress(progress: String) {
        _state.update { it.copy(trackingProgress = progress.filter { char -> char.isDigit() }.take(5)).withRecomputedTrackingDirty() }
        scheduleTrackingAutoSave()
    }

    fun setTrackingScore(score: String) {
        _state.update { it.copy(trackingScore = score.filteredScoreInput(it.anilistScoreFormat)).withRecomputedTrackingDirty() }
        scheduleTrackingAutoSave()
    }

    fun setTrackingNotes(notes: String) {
        _state.update { it.copy(trackingNotes = notes).withRecomputedTrackingDirty() }
        scheduleTrackingAutoSave()
    }

    fun setTrackingPrivate(private: Boolean) {
        _state.update { it.copy(trackingPrivate = private).withRecomputedTrackingDirty() }
        scheduleTrackingAutoSave()
    }

    fun setTrackingCustomListSelected(name: String, selected: Boolean) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        _state.update {
            it.copy(
                trackingCustomLists = if (selected) {
                    it.trackingCustomLists + normalizedName
                } else {
                    it.trackingCustomLists - normalizedName
                },
            ).withRecomputedTrackingDirty()
        }
        scheduleTrackingCustomListSave()
    }

    fun addTrackingCustomList(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        _state.update {
            val knownLists = (it.anilistCustomLists + normalizedName).distinctBy { listName ->
                listName.lowercase(Locale.ROOT)
            }
            it.copy(
                anilistCustomLists = knownLists,
                trackingCustomLists = it.trackingCustomLists + normalizedName,
            ).withRecomputedTrackingDirty()
        }
        scheduleTrackingCustomListSave()
    }

    private fun scheduleTrackingCustomListSave() {
        if (_state.value.anilistAutoSaveTrackingChanges) {
            scheduleTrackingAutoSave()
        }
    }

    private fun saveTrackingCustomListsOnly() {
        val media = _state.value.selectedMedia ?: return
        val token = container.tokenStore.accessToken() ?: return
        val snapshot = _state.value
        val customLists = snapshot.trackingCustomLists.normalizedCustomLists()
        if (customLists.isEmpty() && snapshot.selectedListEntry == null) return

        viewModelScope.launch {
            runCatching {
                val knownCustomLists = snapshot.anilistCustomLists.normalizedCustomLists()
                val missingCustomLists = customLists.filterNot { selectedList ->
                    knownCustomLists.any { knownList -> knownList.equals(selectedList, ignoreCase = true) }
                }
                val nextKnownCustomLists = if (missingCustomLists.isEmpty()) {
                    knownCustomLists
                } else {
                    container.anilistRepository.updateMangaCustomLists(
                        accessToken = token,
                        customLists = (knownCustomLists + missingCustomLists).normalizedCustomLists(),
                    ).ifEmpty { (knownCustomLists + missingCustomLists).normalizedCustomLists() }
                }
                val entry = container.anilistRepository.saveListEntry(
                    accessToken = token,
                    mediaId = media.id,
                    status = if (snapshot.selectedListEntry == null) snapshot.trackingStatus else null,
                    progress = null,
                    score = null,
                    notes = null,
                    private = null,
                    customLists = customLists,
                    scoreFormat = snapshot.anilistScoreFormat,
                )
                val now = System.currentTimeMillis()
                container.database.mediaDao().upsertMedia(media.toEntity(now))
                container.database.listEntryDao().upsertEntry(entry.toEntity(now))
                container.settingsStore.saveAnilistCustomLists(nextKnownCustomLists)
                nextKnownCustomLists to entry
            }.onSuccess { (knownCustomLists, entry) ->
                _state.update {
                    val nextItem = LibraryItem(media, entry)
                    val nextItems = (it.libraryItems.filterNot { item -> item.media.id == media.id } + nextItem)
                        .sortedBy { item -> item.media.title.userPreferred.lowercase(Locale.ROOT) }
                    it.copy(
                        anilistCustomLists = knownCustomLists,
                        library = nextItems.map { item -> item.media },
                        libraryItems = nextItems,
                        selectedListEntry = entry,
                        trackingStatus = entry.status,
                        trackingProgress = entry.progress.toString(),
                        trackingScore = entry.score.formatTrackingScore(it.anilistScoreFormat),
                        trackingNotes = entry.notes.orEmpty(),
                        trackingPrivate = entry.private,
                        trackingCustomLists = entry.customLists.toSet(),
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList custom list save failed for ${media.id}", error)
                _state.update {
                    it.copy(message = error.userMessage("Custom list save failed"))
                }
            }
        }
    }

    private fun scheduleTrackingAutoSave() {
        val snapshot = _state.value
        if (!snapshot.anilistAutoSaveTrackingChanges || !snapshot.loggedIn || snapshot.selectedMedia == null) return
        trackingAutoSaveJob?.cancel()
        trackingAutoSaveJob = viewModelScope.launch {
            delay(TRACKING_AUTO_SAVE_DELAY_MILLIS)
            saveTracking(autoSave = true)
        }
    }

    fun saveTracking() {
        saveTracking(autoSave = false)
    }

    private fun applyOptimisticTrackingEntry(
        media: AnilistMedia,
        entry: AnilistListEntry,
        knownCustomLists: List<String>,
        autoSave: Boolean,
    ) {
        _state.update {
            val selected = it.selectedMedia?.id == media.id
            val nextItem = LibraryItem(media, entry)
            val nextItems = (it.libraryItems.filterNot { item -> item.media.id == media.id } + nextItem)
                .sortedBy { item -> item.media.title.userPreferred.lowercase(Locale.ROOT) }
            it.copy(
                anilistCustomLists = knownCustomLists,
                library = nextItems.map { item -> item.media },
                libraryItems = nextItems,
                selectedListEntry = if (selected) entry else it.selectedListEntry,
                trackingDirty = if (selected) false else it.trackingDirty,
                trackingSaveInProgress = if (selected) true else it.trackingSaveInProgress,
                trackingSaveFailed = if (selected) false else it.trackingSaveFailed,
                busy = if (autoSave) it.busy else true,
                message = null,
            )
        }
    }

    private fun saveTracking(autoSave: Boolean) {
        val media = _state.value.selectedMedia ?: return
        val token = container.tokenStore.accessToken()
        if (token == null) {
            if (!autoSave) {
                _state.update { it.copy(message = "Connect AniList to track this manga") }
            }
            return
        }

        val snapshot = _state.value
        val progress = snapshot.trackingProgress.toIntOrNull()?.coerceAtLeast(0)
        val score = snapshot.trackingScore.toAniListScore(snapshot.anilistScoreFormat)
        val notes = snapshot.trackingNotes.trim().ifBlank { null }
        val customLists = snapshot.trackingCustomLists.normalizedCustomLists()
        val knownCustomLists = snapshot.anilistCustomLists.normalizedCustomLists()
        val missingCustomLists = customLists.filterNot { selectedList ->
            knownCustomLists.any { knownList -> knownList.equals(selectedList, ignoreCase = true) }
        }
        val optimisticKnownCustomLists = (knownCustomLists + missingCustomLists).normalizedCustomLists()
        val optimisticEntry = AnilistListEntry(
            id = snapshot.selectedListEntry?.id ?: -media.id,
            mediaId = media.id,
            status = snapshot.trackingStatus,
            progress = progress ?: 0,
            score = score,
            notes = notes,
            private = snapshot.trackingPrivate,
            customLists = customLists,
            updatedAtEpochSeconds = System.currentTimeMillis() / 1000L,
        )

        viewModelScope.launch {
            applyOptimisticTrackingEntry(
                media = media,
                entry = optimisticEntry,
                knownCustomLists = optimisticKnownCustomLists,
                autoSave = autoSave,
            )
            runCatching {
                val nextKnownCustomLists = if (missingCustomLists.isEmpty()) {
                    knownCustomLists
                } else {
                    container.anilistRepository.updateMangaCustomLists(
                        accessToken = token,
                        customLists = (knownCustomLists + missingCustomLists).normalizedCustomLists(),
                    ).ifEmpty { (knownCustomLists + missingCustomLists).normalizedCustomLists() }
                }
                val entry = container.anilistRepository.saveListEntry(
                    accessToken = token,
                    mediaId = media.id,
                    status = snapshot.trackingStatus,
                    progress = progress,
                    score = score,
                    notes = notes,
                    private = snapshot.trackingPrivate,
                    customLists = customLists,
                    scoreFormat = snapshot.anilistScoreFormat,
                )
                val now = System.currentTimeMillis()
                container.database.mediaDao().upsertMedia(media.toEntity(now))
                container.database.listEntryDao().upsertEntry(entry.toEntity(now))
                container.settingsStore.saveAnilistCustomLists(nextKnownCustomLists)
                nextKnownCustomLists to entry
            }.onSuccess { (knownCustomLists, entry) ->
                _state.update {
                    val nextItem = LibraryItem(media, entry)
                    val nextItems = (it.libraryItems.filterNot { item -> item.media.id == media.id } + nextItem)
                        .sortedBy { item -> item.media.title.userPreferred.lowercase(Locale.ROOT) }
                    val selected = it.selectedMedia?.id == media.id
                    val preserveEditedForm = selected && it.trackingDirty
                    it.copy(
                        anilistCustomLists = knownCustomLists,
                        library = nextItems.map { item -> item.media },
                        libraryItems = nextItems,
                        selectedListEntry = if (selected) entry else it.selectedListEntry,
                        trackingStatus = if (!selected || preserveEditedForm) it.trackingStatus else entry.status,
                        trackingProgress = if (!selected || preserveEditedForm) it.trackingProgress else entry.progress.toString(),
                        trackingScore = if (!selected || preserveEditedForm) {
                            it.trackingScore
                        } else {
                            entry.score.formatTrackingScore(it.anilistScoreFormat)
                        },
                        trackingNotes = if (!selected || preserveEditedForm) it.trackingNotes else entry.notes.orEmpty(),
                        trackingPrivate = if (!selected || preserveEditedForm) it.trackingPrivate else entry.private,
                        trackingCustomLists = if (!selected || preserveEditedForm) it.trackingCustomLists else entry.customLists.toSet(),
                        trackingDirty = if (selected) preserveEditedForm else it.trackingDirty,
                        trackingSaveInProgress = if (selected) false else it.trackingSaveInProgress,
                        trackingSaveFailed = if (selected) false else it.trackingSaveFailed,
                        busy = if (autoSave) it.busy else false,
                        message = if (autoSave) it.message else "AniList tracking saved",
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList tracking save failed for ${media.id}", error)
                _state.update {
                    val selected = it.selectedMedia?.id == media.id
                    it.copy(
                        trackingDirty = if (selected) true else it.trackingDirty,
                        trackingSaveInProgress = if (selected) false else it.trackingSaveInProgress,
                        trackingSaveFailed = if (selected) true else it.trackingSaveFailed,
                        busy = if (autoSave) it.busy else false,
                        message = error.userMessage(if (autoSave) "Tracking auto-save failed. Tap save to retry" else "Tracking save failed"),
                    )
                }
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
                        val preserveTrackingForm = it.trackingDirty || it.trackingSaveInProgress || it.trackingSaveFailed
                        it.copy(
                            selectedMedia = cachedMedia?.toModel(it.anilistTitleLanguage) ?: it.selectedMedia,
                            selectedListEntry = cachedEntry,
                            selectedRecommendations = cachedRecommendations,
                            selectedRecommendationsPage = cachedRecommendations.recommendationPageCount(),
                            selectedRecommendationsHasMore = cachedRecommendations.size >= RECOMMENDATIONS_PAGE_SIZE,
                            trackingStatus = if (preserveTrackingForm) it.trackingStatus else cachedEntry?.status ?: it.trackingStatus,
                            trackingProgress = if (preserveTrackingForm) {
                                it.trackingProgress
                            } else {
                                cachedEntry?.progress?.toString() ?: it.trackingProgress
                            },
                            trackingScore = if (preserveTrackingForm) {
                                it.trackingScore
                            } else {
                                cachedEntry?.score.formatTrackingScore(it.anilistScoreFormat)
                                    .takeIf { score -> score.isNotBlank() }
                                    ?: it.trackingScore
                            },
                            trackingNotes = if (preserveTrackingForm) it.trackingNotes else cachedEntry?.notes ?: it.trackingNotes,
                            trackingPrivate = if (preserveTrackingForm) it.trackingPrivate else cachedEntry?.private ?: it.trackingPrivate,
                            trackingCustomLists = if (preserveTrackingForm) {
                                it.trackingCustomLists
                            } else {
                                cachedEntry?.customLists?.toSet() ?: it.trackingCustomLists
                            },
                        )
                    }
                }
            }

            val cachedMediaHasEnrichedDetails = cachedMedia?.let { it.staff.isNotEmpty() && it.tags.isNotEmpty() } ?: false
            val cachedMediaIsFresh = cachedMedia != null &&
                cachedMediaHasEnrichedDetails &&
                now - cachedMedia.fetchedAtEpochMillis <= cachePolicy.mediaDetailsTtlMillis
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
                    scoreFormat = _state.value.anilistScoreFormat,
                    recommendationsPage = 1,
                    recommendationsPerPage = RECOMMENDATIONS_PAGE_SIZE,
                )
            }.onSuccess { result ->
                val titleLanguage = _state.value.anilistTitleLanguage
                val details = result.media.withTitleLanguage(titleLanguage)
                val entry = result.listEntry
                val recommendationPage = result.recommendationPage
                val recommendations = recommendationPage.recommendations.map { recommendation ->
                    recommendation.copy(media = recommendation.media.withTitleLanguage(titleLanguage))
                }
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
                        val preserveTrackingForm = it.trackingDirty || it.trackingSaveInProgress || it.trackingSaveFailed
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
                            trackingStatus = if (preserveTrackingForm) it.trackingStatus else effectiveEntry?.status ?: it.trackingStatus,
                            trackingProgress = if (preserveTrackingForm) {
                                it.trackingProgress
                            } else {
                                effectiveEntry?.progress?.toString() ?: it.trackingProgress
                            },
                            trackingScore = if (preserveTrackingForm) {
                                it.trackingScore
                            } else {
                                effectiveEntry?.score.formatTrackingScore(it.anilistScoreFormat)
                                    .takeIf { score -> score.isNotBlank() }
                                    ?: it.trackingScore
                            },
                            trackingNotes = if (preserveTrackingForm) it.trackingNotes else effectiveEntry?.notes ?: it.trackingNotes,
                            trackingPrivate = if (preserveTrackingForm) it.trackingPrivate else effectiveEntry?.private ?: it.trackingPrivate,
                            trackingCustomLists = if (preserveTrackingForm) {
                                it.trackingCustomLists
                            } else {
                                effectiveEntry?.customLists?.toSet() ?: it.trackingCustomLists
                            },
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
                val titleLanguage = _state.value.anilistTitleLanguage
                val recommendations = recommendationPage.recommendations.map { recommendation ->
                    recommendation.copy(media = recommendation.media.withTitleLanguage(titleLanguage))
                }
                container.database.mediaDao().upsertMedia(recommendations.map { it.media.toEntity(now) })
                container.database.recommendationDao().upsertRecommendations(
                    recommendations.map { it.toEntity(mediaId, now) },
                )
                _state.update {
                    if (it.selectedMedia?.id != mediaId) {
                        it
                    } else {
                        val combined = (it.selectedRecommendations + recommendations)
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
            mediaById[recommendation.recommendationMediaId]
                ?.toModel(_state.value.anilistTitleLanguage)
                ?.let { media ->
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
            val chapterProgress = cachedProgressByChapter(mediaId)
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
                        chapterProgress = chapterProgress,
                    )
                }
            }
        }
    }

    private suspend fun cachedProgressByChapter(mediaId: Int): Map<String, ReadingProgress> =
        container.database.progressDao()
            .progressForMedia(mediaId)
            .map { it.toModel() }
            .associateBy { it.chapterUrl }

    fun openSourcePicker() {
        val media = _state.value.selectedMedia ?: return
        val sources = sourcePickerSources()
        _state.update {
            it.copy(
                sourcePickerOpen = true,
                sourcePickerMessage = null,
                sourcePickerDiagnostics = emptyList(),
                sourcePickerSearchTitle = sourcePickerDefaultSearchTitle(media),
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
        cancelSourcePickerJob()
        _state.update {
            it.copy(
                busy = false,
                sourcePickerOpen = false,
                sourcePickerLoading = false,
                sourcePickerMessage = null,
                sourcePickerDiagnostics = emptyList(),
                sourcePickerSearchTitle = "",
            )
        }
    }

    fun updateSourcePickerSearchTitle(title: String) {
        _state.update { it.copy(sourcePickerSearchTitle = title) }
    }

    fun findSourceMatchesWithEditedTitle() {
        val title = _state.value.sourcePickerSearchTitle.trim()
        if (title.length < 2) {
            _state.update { it.copy(sourcePickerMessage = "Enter at least two characters to search.") }
            return
        }
        findSourceMatches(forceRefresh = true, titleOverride = title)
    }

    private fun beginSourcePickerJob(): Long {
        sourcePickerJob?.cancel()
        sourcePickerRequestId += 1
        return sourcePickerRequestId
    }

    private fun cancelSourcePickerJob() {
        sourcePickerRequestId += 1
        sourcePickerJob?.cancel()
        sourcePickerJob = null
    }

    private fun isActiveSourcePickerRequest(requestId: Long, mediaId: Int): Boolean {
        val snapshot = _state.value
        return requestId == sourcePickerRequestId &&
            snapshot.sourcePickerOpen &&
            snapshot.selectedMedia?.id == mediaId
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
        val requestId = beginSourcePickerJob()
        sourcePickerJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    sourcePickerLoading = true,
                    sourcePickerMessage = "Searching ${source.name}...",
                    sourcePickerDiagnostics = emptyList(),
                    message = null,
                )
            }
            try {
                val now = System.currentTimeMillis()
                val matches = searchSourceMatches(media, source, now)
                    .filter { it.isReadableMatchCandidate() }
                val verified = firstReadableMatch(source, matches, now)
                    ?: error("No readable manga found on ${source.name}")
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
                saveSourceBinding(verified.match)
                val match = verified.match
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
            } catch (error: Throwable) {
                if (error is CancellationException) return@launch
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
                Log.w(TAG, "Selected source binding failed for ${source.name}", error)
                _state.update {
                    it.copy(
                        busy = false,
                        sourcePickerLoading = false,
                        sourcePickerMessage = sourcePickerErrorMessage(source.name, error),
                        message = null,
                    )
                }
            } finally {
                if (sourcePickerRequestId == requestId) {
                    sourcePickerJob = null
                }
            }
        }
    }

    private suspend fun searchSourceMatches(
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

    fun findSourceMatches(forceRefresh: Boolean = false, titleOverride: String? = null) {
        val media = _state.value.selectedMedia ?: return
        val sources = sourcePickerSources()
        if (sources.isEmpty()) {
            _state.update { it.copy(sourcePickerMessage = "Enable or install a source extension first") }
            return
        }
        val editedTitle = titleOverride?.trim()?.takeIf { it.length >= 2 }
        val requestId = beginSourcePickerJob()
        sourcePickerJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    sourcePickerLoading = true,
                    sourcePickerMessage = editedTitle?.let { title -> "Searching enabled sources for \"$title\"..." }
                        ?: "Searching enabled sources...",
                    sourcePickerDiagnostics = emptyList(),
                    message = null,
                )
            }
            try {
                val now = System.currentTimeMillis()
                val verified = if (editedTitle == null) {
                    cachedVerifiedMatches(media.id, sources, now)
                        .takeIf { !forceRefresh && it.matches.isNotEmpty() }
                        ?: searchVerifiedMatches(media, sources, now, requestId).also { verified ->
                            container.database.sourceSearchDao().clearForMedia(media.id)
                            if (verified.matches.isNotEmpty()) {
                                container.database.sourceSearchDao().upsertResults(verified.matches.map { it.toEntity() })
                            }
                        }
                } else {
                    searchVerifiedMatches(media, sources, now, requestId, editedTitle)
                }
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
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
                            editedTitle?.let { title ->
                                "No readable matches found for \"$title\". Edit the search title or tap a source below to try it directly."
                            } ?: "No readable matches found automatically. Edit the search title or tap a source below to try it directly."
                        } else {
                            editedTitle?.let { title -> "Found ${nextMatches.size} readable sources for \"$title\"" }
                                ?: "Found ${nextMatches.size} readable sources"
                        },
                        message = null,
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException) return@launch
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
                _state.update {
                    it.copy(
                        busy = false,
                        sourcePickerLoading = false,
                        sourcePickerMessage = sourcePickerErrorMessage("source search", error),
                        message = null,
                    )
                }
            } finally {
                if (sourcePickerRequestId == requestId) {
                    sourcePickerJob = null
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
        requestId: Long,
        titleOverride: String? = null,
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
                                    publishSourcePickerDiagnostic(requestId, media.id, source, "search timed out")
                                    null
                                }
                                searchResults.isEmpty() -> {
                                    publishSourcePickerDiagnostic(requestId, media.id, source, "no search results")
                                    null
                                }
                                candidates.isEmpty() -> {
                                    publishSourcePickerDiagnostic(requestId, media.id, source, "no confident title match")
                                    null
                                }
                                else -> {
                                    val readable = withTimeoutOrNull(SOURCE_VERIFY_TIMEOUT_MILLIS) {
                                        firstReadableMatch(source, candidates, now)
                                    }
                                    if (readable == null) {
                                        publishSourcePickerDiagnostic(requestId, media.id, source, "no readable chapters")
                                    } else {
                                        publishSourcePickerMatch(requestId, media.id, readable.match, readable.chapterCount)
                                    }
                                    readable
                                }
                            }
                        } catch (error: Throwable) {
                            if (error is CancellationException) throw error
                            Log.w(TAG, "Source verification failed for ${source.name}", error)
                            publishSourcePickerDiagnostic(requestId, media.id, source, sourcePickerDiagnosticDetail(error))
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

    private fun publishSourcePickerMatch(requestId: Long, mediaId: Int, match: SourceSearchResult, chapterCount: Int) {
        _state.update {
            if (!isActiveSourcePickerRequest(requestId, mediaId)) {
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

    private fun publishSourcePickerDiagnostic(requestId: Long, mediaId: Int, source: SourceDescriptor, detail: String) {
        val diagnostic = "${source.name}: $detail"
        _state.update {
            if (!isActiveSourcePickerRequest(requestId, mediaId)) {
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
        val media = _state.value.selectedMedia ?: return
        val requestId = beginSourcePickerJob()
        sourcePickerJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    sourcePickerLoading = true,
                    sourcePickerMessage = "Opening ${match.manga.title} from ${match.source.name}...",
                    message = null,
                )
            }
            try {
                val resolved = match.withResolvedManga()
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
                saveSourceBinding(resolved)
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
            } catch (error: Throwable) {
                if (error is CancellationException) return@launch
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
                Log.w(TAG, "Source binding failed for ${match.source.name}/${match.manga.title}", error)
                _state.update {
                    it.copy(
                        busy = false,
                        sourcePickerLoading = false,
                        sourcePickerMessage = sourcePickerErrorMessage(match.source.name, error),
                        message = null,
                    )
                }
            } finally {
                if (sourcePickerRequestId == requestId) {
                    sourcePickerJob = null
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
                val mediaId = _state.value.selectedMedia?.id
                val chapterProgress = mediaId?.let { cachedProgressByChapter(it) }.orEmpty()
                Triple(detailedManga, chapters, chapterProgress)
            }.onSuccess { (detailedManga, chapters, chapterProgress) ->
                Log.i(TAG, "Chapter load ${source.name}/${detailedManga.title}: chapters=${chapters.size}")
                _state.update {
                    it.copy(
                        selectedSourceManga = detailedManga,
                        sourceChapters = chapters,
                        chapterProgress = chapterProgress,
                        selectingDownloadChapters = false,
                        selectedDownloadChapterUrls = emptySet(),
                        busy = false,
                        sourceMatchChapterCounts = it.sourceMatchChapterCounts + (sourceMatchKey(source.id, detailedManga.url) to chapters.size),
                        message = if (chapters.isEmpty()) "No chapters found" else null,
                    )
                }
                if (_state.value.keepNextTenDownloads) {
                    val result = ensureNextTenDownloads()
                    if (result.changed > 0) {
                        _state.update { it.copy(message = "Queued ${result.changed} next chapters for download") }
                    }
                }
            }.onFailure { error ->
                Log.w(TAG, "Chapter load failed for ${source.name}/${manga.title}", error)
                _state.update { it.copy(busy = false, message = error.message ?: "Chapter load failed") }
            }
        }
    }

    fun openChapter(
        chapter: SourceChapter,
        startFromSavedProgress: Boolean = true,
        startPageIndexOverride: Int? = null,
    ) {
        val state = _state.value
        val media = state.selectedMedia ?: return
        val source = state.readerSourceForChapter(chapter)
        viewModelScope.launch {
            readerAdjacentLoadJob?.cancel()
            cancelReaderPageCacheJobs()
            latestReaderPosition = null
            _state.update {
                it.copy(
                    busy = true,
                    message = null,
                    activeChapter = chapter,
                    readerPages = emptyList(),
                    readerPreviousSegment = null,
                    readerNextSegment = null,
                    readerError = null,
                    currentPageIndex = 0,
                    currentPageScrollOffset = 0,
                )
            }
            runCatching {
                loadReaderPagesForChapter(media.id, chapter, source)
            }.onSuccess { pages ->
                if (pages.isEmpty() && source == null) {
                    val readerError = ReaderLoadError(
                        title = "Source not installed",
                        message = "This chapter belongs to a source that is not installed anymore. Install the source again or choose another one.",
                    )
                    _state.update {
                        if (it.activeChapter?.url == chapter.url) {
                            it.copy(
                                readerPages = emptyList(),
                                readerPreviousSegment = null,
                                readerNextSegment = null,
                                readerError = readerError,
                                busy = false,
                                message = readerError.title,
                            )
                        } else {
                            it
                        }
                    }
                    return@onSuccess
                }
                if (pages.isEmpty()) {
                    val readerError = ReaderLoadError(
                        title = "No pages found",
                        message = "The source opened this chapter, but it did not return any pages. The chapter may have been removed, or the source may need an update.",
                    )
                    _state.update {
                        if (it.activeChapter?.url == chapter.url) {
                            it.copy(
                                readerPages = emptyList(),
                                readerPreviousSegment = null,
                                readerNextSegment = null,
                                readerError = readerError,
                                busy = false,
                                message = readerError.title,
                            )
                        } else {
                            it
                        }
                    }
                    return@onSuccess
                }
                val savedProgress = if (startFromSavedProgress) {
                    container.database.progressDao().progressForChapter(media.id, chapter.url)
                } else {
                    null
                }
                val startPageIndex = startPageIndexOverride
                    ?.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
                    ?: savedProgress?.pageIndex?.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
                    ?: 0
                val startPageScrollOffset = savedProgress?.pageScrollOffset?.coerceAtLeast(0) ?: 0
                Log.i(TAG, "Page load ${source?.name ?: "downloaded"}/${chapter.name}: pages=${pages.size}")
                var readerStillOpen = false
                _state.update {
                    if (it.activeChapter?.url == chapter.url) {
                        readerStillOpen = true
                        recordReaderPosition(chapter.url, startPageIndex, startPageScrollOffset)
                        it.copy(
                            activeChapter = chapter,
                            readerPages = pages,
                            readerPreviousSegment = null,
                            readerNextSegment = null,
                            readerError = null,
                            currentPageIndex = startPageIndex,
                            currentPageScrollOffset = startPageScrollOffset,
                            busy = false,
                            message = null,
                        )
                    } else {
                        it
                    }
                }
                if (!readerStillOpen) return@onSuccess
                cacheReaderWindow(
                    mediaId = media.id,
                    chapter = chapter,
                    pages = pages,
                    pageIndex = startPageIndex,
                    source = source,
                    preferredDirection = 1,
                )
                loadAdjacentReaderSegments(media.id, chapter)
            }.onFailure { error ->
                Log.w(TAG, "Page load failed for ${source?.name ?: "cached source"}/${chapter.name}", error)
                val readerError = readerLoadErrorFor(container.application, error, source)
                _state.update {
                    if (it.activeChapter?.url == chapter.url) {
                        it.copy(
                            readerPages = emptyList(),
                            readerPreviousSegment = null,
                            readerNextSegment = null,
                            readerError = readerError,
                            busy = false,
                            message = readerError.title,
                        )
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun retryReaderChapter() {
        val chapter = _state.value.activeChapter ?: return
        openChapter(chapter)
    }

    private suspend fun loadReaderPagesForChapter(
        mediaId: Int,
        chapter: SourceChapter,
        source: SourceDescriptor?,
    ): List<ReaderPage> {
        val cachedPages = cachedDownloadedPages(mediaId, chapter)
        if (cachedPages.isNotEmpty()) return cachedPages
        if (source == null) return emptyList()
        return ReaderPageCache.withCachedPaths(
            context = container.application,
            mediaId = mediaId,
            chapter = chapter,
            pages = container.sourceHost.pages(source, chapter),
        )
    }

    private fun loadAdjacentReaderSegments(mediaId: Int, chapter: SourceChapter) {
        val snapshot = _state.value
        val previousChapter = snapshot.sourceChapters.previousInReadingOrderBefore(chapter)
        val nextChapter = snapshot.sourceChapters.nextInReadingOrderAfter(chapter)
        if (previousChapter == null && nextChapter == null) return
        readerAdjacentLoadJob?.cancel()
        readerAdjacentLoadJob = viewModelScope.launch {
            val previousSource = previousChapter?.let { _state.value.readerSourceForChapter(it) }
            val nextSource = nextChapter?.let { _state.value.readerSourceForChapter(it) }
            val previousDeferred = previousChapter?.let { adjacentChapter ->
                async {
                    runCatching {
                        loadReaderPagesForChapter(mediaId, adjacentChapter, previousSource)
                    }.getOrDefault(emptyList())
                }
            }
            val nextDeferred = nextChapter?.let { adjacentChapter ->
                async {
                    runCatching {
                        loadReaderPagesForChapter(mediaId, adjacentChapter, nextSource)
                    }.getOrDefault(emptyList())
                }
            }
            val previousSegment = previousChapter?.let { adjacentChapter ->
                previousDeferred?.await()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { pages -> ReaderChapterSegment(adjacentChapter, pages) }
            }
            val nextSegment = nextChapter?.let { adjacentChapter ->
                nextDeferred?.await()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { pages -> ReaderChapterSegment(adjacentChapter, pages) }
            }
            previousSegment?.let { segment ->
                startReaderPageCache(
                    source = previousSource,
                    mediaId = mediaId,
                    chapter = segment.chapter,
                    pages = segment.pages.takeLast(READER_ADJACENT_CACHE_PAGE_COUNT),
                    cacheKeySuffix = "tail",
                    initialDelayMillis = READER_ADJACENT_CACHE_INITIAL_DELAY_MILLIS,
                )
            }
            nextSegment?.let { segment ->
                startReaderPageCache(
                    source = nextSource,
                    mediaId = mediaId,
                    chapter = segment.chapter,
                    pages = segment.pages.take(READER_ADJACENT_CACHE_PAGE_COUNT),
                    cacheKeySuffix = "head",
                    initialDelayMillis = READER_ADJACENT_CACHE_INITIAL_DELAY_MILLIS,
                )
            }
            _state.update {
                if (it.activeChapter?.url == chapter.url) {
                    it.copy(
                        readerPreviousSegment = previousSegment,
                        readerNextSegment = nextSegment,
                    )
                } else {
                    it
                }
            }
        }
    }

    private suspend fun cachedDownloadedPages(mediaId: Int, chapter: SourceChapter): List<ReaderPage> {
        container.database.downloadDao().completedForChapter(mediaId, chapter.url) ?: return emptyList()
        return container.database.downloadPageDao()
            .pagesForChapter(mediaId, chapter.url)
            .filter { File(it.filePath).isFile }
            .map { it.toReaderPage() }
    }

    fun openRecentProgress(item: RecentReadingProgress) {
        val existingEntry = _state.value.libraryItems.firstOrNull { libraryItem ->
            libraryItem.media.id == item.media.id
        }?.entry
        _state.update {
            it.copy(
                selectedMedia = item.media,
                selectedListEntry = existingEntry,
                sourceMatches = emptyList(),
                sourceMatchChapterCounts = emptyMap(),
                sourcePickerOpen = false,
                sourcePickerLoading = false,
                sourcePickerMessage = null,
                sourcePickerDiagnostics = emptyList(),
                selectedRecommendations = emptyList(),
                selectedRecommendationsPage = 0,
                selectedRecommendationsHasMore = false,
                recommendationsLoading = false,
                trackingStatus = existingEntry?.status ?: MediaStatus.CURRENT,
                trackingProgress = (existingEntry?.progress ?: 0).toString(),
                trackingScore = existingEntry?.score.formatTrackingScore(it.anilistScoreFormat),
                trackingNotes = existingEntry?.notes.orEmpty(),
                trackingPrivate = existingEntry?.private ?: false,
                trackingCustomLists = existingEntry?.customLists.orEmpty().toSet(),
                selectedSourceId = item.chapter?.sourceId ?: it.selectedSourceId,
                selectedSourceManga = null,
                sourceChapters = emptyList(),
                latestProgress = item.progress,
                chapterProgress = mapOf(item.progress.chapterUrl to item.progress),
                activeChapter = null,
                readerPages = emptyList(),
                readerPreviousSegment = null,
                readerNextSegment = null,
                readerError = null,
                currentPageIndex = 0,
                currentPageScrollOffset = 0,
                message = null,
            )
        }
        loadAnilistDetails(item.media.id)
        loadCachedSourceState(item.media.id)
        val chapter = item.chapter
        if (chapter == null) {
            _state.update { it.copy(message = "Chapter cache is missing for ${item.media.title.userPreferred}") }
        } else {
            openChapter(chapter)
        }
    }

    fun closeReader() {
        saveReaderProgress()
        readerAdjacentLoadJob?.cancel()
        readerAdjacentLoadJob = null
        cancelReaderPageCacheJobs()
        _state.update {
            it.copy(
                activeChapter = null,
                readerPages = emptyList(),
                readerPreviousSegment = null,
                readerNextSegment = null,
                readerError = null,
                currentPageIndex = 0,
                currentPageScrollOffset = 0,
                busy = false,
            )
        }
        latestReaderPosition = null
    }

    fun persistReaderProgress() {
        saveReaderProgress()
    }

    fun moveReaderPage(delta: Int) {
        val snapshot = _state.value
        val pages = snapshot.readerPages
        if (pages.isEmpty() || delta == 0) return
        val targetIndex = snapshot.currentPageIndex + delta
        if (delta < 0 && targetIndex < 0) {
            openPreviousChapter()
            return
        }
        if (delta > 0 && targetIndex > pages.lastIndex) {
            openNextChapter()
            return
        }
        val nextIndex = targetIndex.coerceIn(0, pages.lastIndex)
        if (nextIndex == snapshot.currentPageIndex) return
        setReaderPage(nextIndex)
    }

    fun setReaderPage(index: Int, pageScrollOffset: Int = 0) {
        val snapshot = _state.value
        val pages = snapshot.readerPages
        if (pages.isEmpty()) return
        val nextIndex = index.coerceIn(0, pages.lastIndex)
        val nextOffset = pageScrollOffset.coerceAtLeast(0)
        if (nextIndex == snapshot.currentPageIndex && nextOffset == snapshot.currentPageScrollOffset) return
        val chapter = snapshot.activeChapter
        if (chapter != null) {
            recordReaderPosition(chapter.url, nextIndex, nextOffset)
        }
        _state.update { it.copy(currentPageIndex = nextIndex, currentPageScrollOffset = nextOffset) }
        val media = snapshot.selectedMedia
        if (media != null && chapter != null) {
            cacheReaderWindow(
                mediaId = media.id,
                chapter = chapter,
                pages = pages,
                pageIndex = nextIndex,
                source = snapshot.readerSourceForChapter(chapter),
                preferredDirection = nextIndex.compareTo(snapshot.currentPageIndex),
            )
        }
    }

    fun setWebtoonReaderPosition(chapterUrl: String, pageIndex: Int, pageScrollOffset: Int) {
        val snapshot = _state.value
        val activeChapter = snapshot.activeChapter ?: return
        when (chapterUrl) {
            activeChapter.url -> setActiveWebtoonReaderPosition(pageIndex, pageScrollOffset)
            snapshot.readerPreviousSegment?.chapter?.url -> activateContinuousReaderSegment(
                segment = snapshot.readerPreviousSegment,
                pageIndex = pageIndex,
                pageScrollOffset = pageScrollOffset,
                direction = ReaderSegmentDirection.PREVIOUS,
            )
            snapshot.readerNextSegment?.chapter?.url -> activateContinuousReaderSegment(
                segment = snapshot.readerNextSegment,
                pageIndex = pageIndex,
                pageScrollOffset = pageScrollOffset,
                direction = ReaderSegmentDirection.NEXT,
            )
        }
    }

    private fun setActiveWebtoonReaderPosition(pageIndex: Int, pageScrollOffset: Int) {
        val snapshot = _state.value
        val pages = snapshot.readerPages
        if (pages.isEmpty()) return
        val nextIndex = pageIndex.coerceIn(0, pages.lastIndex)
        val nextOffset = pageScrollOffset.coerceAtLeast(0)
        val chapter = snapshot.activeChapter ?: return
        recordReaderPosition(chapter.url, nextIndex, nextOffset)
        val pageChanged = nextIndex != snapshot.currentPageIndex
        if (!pageChanged) return

        _state.update { it.copy(currentPageIndex = nextIndex, currentPageScrollOffset = nextOffset) }
        val media = snapshot.selectedMedia
        if (media != null) {
            cacheReaderWindow(
                mediaId = media.id,
                chapter = chapter,
                pages = pages,
                pageIndex = nextIndex,
                source = snapshot.readerSourceForChapter(chapter),
                preferredDirection = nextIndex.compareTo(snapshot.currentPageIndex),
            )
        }
    }

    private fun activateContinuousReaderSegment(
        segment: ReaderChapterSegment?,
        pageIndex: Int,
        pageScrollOffset: Int,
        direction: ReaderSegmentDirection,
    ) {
        val media = _state.value.selectedMedia ?: return
        val snapshot = _state.value
        val activeChapter = snapshot.activeChapter ?: return
        val activePages = snapshot.readerPages
        val targetSegment = segment ?: return
        if (activePages.isEmpty() || targetSegment.pages.isEmpty()) return

        val activeProgressIndex = when (direction) {
            ReaderSegmentDirection.PREVIOUS -> 0
            ReaderSegmentDirection.NEXT -> activePages.lastIndex
        }
        saveReaderProgressFor(
            media = media,
            chapter = activeChapter,
            pages = activePages,
            pageIndex = activeProgressIndex,
            pageScrollOffset = 0,
        )

        val nextIndex = pageIndex.coerceIn(0, targetSegment.pages.lastIndex)
        val oldActiveSegment = ReaderChapterSegment(activeChapter, activePages)
        recordReaderPosition(targetSegment.chapter.url, nextIndex, pageScrollOffset)
        _state.update {
            it.copy(
                activeChapter = targetSegment.chapter,
                readerPages = targetSegment.pages,
                readerPreviousSegment = if (direction == ReaderSegmentDirection.NEXT) oldActiveSegment else null,
                readerNextSegment = if (direction == ReaderSegmentDirection.PREVIOUS) oldActiveSegment else null,
                currentPageIndex = nextIndex,
                currentPageScrollOffset = pageScrollOffset.coerceAtLeast(0),
            )
        }
        cacheReaderWindow(
            mediaId = media.id,
            chapter = targetSegment.chapter,
            pages = targetSegment.pages,
            pageIndex = nextIndex,
            source = snapshot.readerSourceForChapter(targetSegment.chapter),
            preferredDirection = when (direction) {
                ReaderSegmentDirection.PREVIOUS -> -1
                ReaderSegmentDirection.NEXT -> 1
            },
        )
        loadAdjacentReaderSegments(media.id, targetSegment.chapter)
    }

    private fun cacheReaderWindow(
        mediaId: Int,
        chapter: SourceChapter,
        pages: List<ReaderPage>,
        pageIndex: Int,
        source: SourceDescriptor? = _state.value.readerSourceForChapter(chapter),
        preferredDirection: Int = 1,
    ) {
        if (pages.isEmpty()) return
        source ?: return
        val start = (pageIndex - READER_CACHE_BACK_PAGES).coerceAtLeast(0)
        val end = (pageIndex + READER_CACHE_FORWARD_PAGES).coerceAtMost(pages.lastIndex)
        val windowPages = orderedReaderCacheWindow(
            pages = pages,
            pageIndex = pageIndex.coerceIn(0, pages.lastIndex),
            start = start,
            end = end,
            preferredDirection = preferredDirection,
        )
        startReaderPageCache(
            source = source,
            mediaId = mediaId,
            chapter = chapter,
            pages = windowPages,
            cacheKeySuffix = "window",
            replaceExisting = true,
            initialDelayMillis = READER_CACHE_INITIAL_DELAY_MILLIS,
        )
    }

    private fun orderedReaderCacheWindow(
        pages: List<ReaderPage>,
        pageIndex: Int,
        start: Int,
        end: Int,
        preferredDirection: Int,
    ): List<ReaderPage> {
        val forwardFirst = preferredDirection >= 0
        val orderedIndexes = buildList {
            add(pageIndex)
            val radius = maxOf(pageIndex - start, end - pageIndex)
            for (offset in 1..radius) {
                val forward = pageIndex + offset
                val backward = pageIndex - offset
                if (forwardFirst) {
                    if (forward <= end) add(forward)
                    if (backward >= start) add(backward)
                } else {
                    if (backward >= start) add(backward)
                    if (forward <= end) add(forward)
                }
            }
        }
        return orderedIndexes.distinct().map { pages[it] }
    }

    private fun startReaderPageCache(
        source: SourceDescriptor?,
        mediaId: Int,
        chapter: SourceChapter,
        pages: List<ReaderPage>,
        cacheKeySuffix: String,
        replaceExisting: Boolean = false,
        initialDelayMillis: Long = 0L,
    ) {
        source ?: return
        val pagesToCache = pages.filter { it.cachedFilePath == null }
        if (pagesToCache.isEmpty()) return
        val key = "$mediaId:${chapter.sourceId}:${chapter.url}:$cacheKeySuffix"
        if (replaceExisting) {
            readerPageCacheJobs.remove(key)?.cancel()
        } else if (readerPageCacheJobs[key]?.isActive == true) {
            return
        }
        val job = viewModelScope.launch(Dispatchers.IO) {
            if (initialDelayMillis > 0L) {
                delay(initialDelayMillis)
            }
            pagesToCache.forEachIndexed { index, page ->
                if (index > 0) delay(READER_CACHE_REQUEST_SPACING_MILLIS)
                runCatching {
                    ReaderPageCache.cachedOrFetch(
                        context = container.application,
                        mediaId = mediaId,
                        chapter = chapter,
                        page = page,
                    ) {
                        container.sourceHost.imageBytes(source, page)
                    }
                }.onFailure { error ->
                    if (error !is CancellationException) {
                        Log.w(TAG, "Reader cache failed for ${chapter.name} page ${page.index + 1}", error)
                    }
                }
            }
            ReaderPageCache.prune(container.application)
        }
        readerPageCacheJobs[key] = job
        job.invokeOnCompletion { readerPageCacheJobs.remove(key, job) }
    }

    private fun cancelReaderPageCacheJobs() {
        readerPageCacheJobs.values.forEach { it.cancel() }
        readerPageCacheJobs.clear()
    }

    fun openNextChapter() {
        val snapshot = _state.value
        val nextChapter = snapshot.sourceChapters.nextInReadingOrderAfter(snapshot.activeChapter ?: return) ?: return
        if (snapshot.readerPages.isNotEmpty()) {
            _state.update { it.copy(currentPageIndex = snapshot.readerPages.lastIndex, currentPageScrollOffset = 0) }
        }
        saveReaderProgress()
        openChapter(nextChapter, startFromSavedProgress = false)
    }

    fun openPreviousChapter() {
        val snapshot = _state.value
        val previousChapter = snapshot.sourceChapters.previousInReadingOrderBefore(snapshot.activeChapter ?: return) ?: return
        snapshot.readerPreviousSegment
            ?.takeIf { it.chapter.url == previousChapter.url && it.pages.isNotEmpty() }
            ?.let { previousSegment ->
                activateContinuousReaderSegment(
                    segment = previousSegment,
                    pageIndex = previousSegment.pages.lastIndex,
                    pageScrollOffset = 0,
                    direction = ReaderSegmentDirection.PREVIOUS,
                )
                return
            }
        if (snapshot.readerPages.isNotEmpty()) {
            _state.update { it.copy(currentPageIndex = 0, currentPageScrollOffset = 0) }
        }
        saveReaderProgress()
        openChapter(
            chapter = previousChapter,
            startFromSavedProgress = false,
            startPageIndexOverride = Int.MAX_VALUE,
        )
    }

    fun setChapterRead(chapter: SourceChapter, read: Boolean) {
        val media = _state.value.selectedMedia ?: return
        viewModelScope.launch {
            val progressDao = container.database.progressDao()
            val now = System.currentTimeMillis()
            if (read) {
                val existing = progressDao.progressForChapter(media.id, chapter.url)?.toModel()
                val totalPages = existing?.totalPages?.takeIf { it > 0 } ?: 1
                val progress = ReadingProgress(
                    mediaId = media.id,
                    chapterUrl = chapter.url,
                    chapterNumber = chapter.chapterNumber,
                    pageIndex = totalPages - 1,
                    pageScrollOffset = 0,
                    totalPages = totalPages,
                    readerMode = existing?.readerMode ?: _state.value.readerMode,
                    completed = true,
                    updatedAtEpochMillis = now,
                )
                progressDao.upsertProgress(progress.toEntity())
                val trackedProgress = _state.value.trackingProgress.toIntOrNull()
                    ?: _state.value.selectedListEntry?.progress
                    ?: 0
                val chapterProgress = chapter.chapterNumber.toInt()
                if (chapterProgress > trackedProgress) {
                    syncAniListProgressFromChapter(
                        media = media,
                        chapterProgress = chapterProgress,
                        triggeredByManualRead = true,
                    )
                }
            } else {
                progressDao.deleteProgressForChapter(media.id, chapter.url)
            }

            val latestProgress = progressDao.latestProgress(media.id)?.toModel()
            val chapterProgress = cachedProgressByChapter(media.id)
            _state.update {
                if (it.selectedMedia?.id == media.id) {
                    it.copy(
                        latestProgress = latestProgress,
                        chapterProgress = chapterProgress,
                        trackingProgress = if (read && chapter.chapterNumber.toInt() > (it.trackingProgress.toIntOrNull() ?: 0)) {
                            chapter.chapterNumber.toInt().toString()
                        } else {
                            it.trackingProgress
                        },
                        message = if (read) "Marked ${chapter.name} as read" else "Marked ${chapter.name} as unread",
                    )
                } else {
                    it
                }
            }
            loadRecentReadingProgress()
            if (_state.value.keepNextTenDownloads) {
                ensureNextTenDownloads()
            }
        }
    }

    fun enqueueDownload(chapter: SourceChapter) {
        viewModelScope.launch {
            val media = _state.value.selectedMedia ?: return@launch
            val result = enqueueChapterDownloads(media.id, listOf(chapter))
            val message = when {
                result.queued > 0 -> "Queued ${chapter.name}"
                result.resumed > 0 -> "Resumed ${chapter.name}"
                result.retried > 0 -> "Retrying ${chapter.name}"
                else -> {
                    val existing = container.database.downloadDao().latestForChapter(media.id, chapter.url)?.toModel()
                    when (existing?.state) {
                        DownloadState.COMPLETE -> "${chapter.name} is already downloaded"
                        DownloadState.QUEUED,
                        DownloadState.RUNNING -> "${chapter.name} is already queued"
                        else -> "${chapter.name} is already in downloads"
                    }
                }
            }
            _state.update { it.copy(message = message) }
        }
    }

    fun downloadAllChapters() {
        val chapters = _state.value.sourceChapters
        enqueueVisibleChapterDownloads(chapters, "all chapters")
    }

    fun downloadUnreadChapters() {
        val snapshot = _state.value
        val chapters = snapshot.sourceChapters.filterNot { snapshot.chapterProgress[it.url]?.completed == true }
        enqueueVisibleChapterDownloads(chapters, "unread chapters")
    }

    fun downloadNextTenChapters() {
        val chapters = nextTenDownloadCandidates(_state.value)
        enqueueVisibleChapterDownloads(chapters, "next 10 chapters")
    }

    fun setKeepNextTenDownloads(enabled: Boolean) {
        container.settingsStore.saveKeepNextTenDownloads(enabled)
        _state.update { it.copy(keepNextTenDownloads = enabled) }
        if (enabled) {
            viewModelScope.launch {
                val result = ensureNextTenDownloads()
                _state.update {
                    it.copy(
                        message = if (result.changed > 0) {
                            "Keeping next 10 ready: queued ${result.changed}"
                        } else {
                            "Next 10 are already ready"
                        },
                    )
                }
            }
        }
    }

    fun startManualDownloadSelection() {
        _state.update {
            it.copy(
                selectingDownloadChapters = true,
                selectedDownloadChapterUrls = emptySet(),
            )
        }
    }

    fun cancelManualDownloadSelection() {
        _state.update {
            it.copy(
                selectingDownloadChapters = false,
                selectedDownloadChapterUrls = emptySet(),
            )
        }
    }

    fun toggleDownloadChapterSelection(chapter: SourceChapter) {
        _state.update {
            val selected = it.selectedDownloadChapterUrls
            it.copy(
                selectedDownloadChapterUrls = if (chapter.url in selected) {
                    selected - chapter.url
                } else {
                    selected + chapter.url
                },
            )
        }
    }

    fun downloadSelectedChapters() {
        val snapshot = _state.value
        val chapters = snapshot.sourceChapters.filter { it.url in snapshot.selectedDownloadChapterUrls }
        enqueueVisibleChapterDownloads(chapters, "selected chapters") {
            it.copy(
                selectingDownloadChapters = false,
                selectedDownloadChapterUrls = emptySet(),
            )
        }
    }

    private fun enqueueVisibleChapterDownloads(
        chapters: List<SourceChapter>,
        label: String,
        stateAfterMessage: (TankobunUiState) -> TankobunUiState = { it },
    ) {
        viewModelScope.launch {
            val media = _state.value.selectedMedia ?: return@launch
            val distinctChapters = chapters.distinctBy { "${it.sourceId}:${it.url}" }
            if (distinctChapters.isEmpty()) {
                _state.update { stateAfterMessage(it).copy(message = "No $label to download") }
                return@launch
            }
            val result = enqueueChapterDownloads(media.id, distinctChapters)
            _state.update {
                stateAfterMessage(it).copy(
                    message = bulkDownloadMessage(label, result),
                )
            }
        }
    }

    private suspend fun ensureNextTenDownloads(): BulkDownloadResult {
        val snapshot = _state.value
        val media = snapshot.selectedMedia ?: return BulkDownloadResult()
        return enqueueChapterDownloads(media.id, nextTenDownloadCandidates(snapshot), retryFailed = false)
    }

    private suspend fun enqueueChapterDownloads(
        mediaId: Int,
        chapters: List<SourceChapter>,
        retryFailed: Boolean = true,
    ): BulkDownloadResult {
        cancelReaderPageCacheJobs()
        var queued = 0
        var resumed = 0
        var retried = 0
        var skipped = 0
        chapters.distinctBy { "${it.sourceId}:${it.url}" }.forEach { chapter ->
            val existing = container.database.downloadDao().latestForChapter(mediaId, chapter.url)?.toModel()
            when (existing?.state) {
                DownloadState.QUEUED,
                DownloadState.RUNNING,
                DownloadState.COMPLETE -> skipped += 1

                DownloadState.PAUSED -> {
                    container.downloadCoordinator.resume(existing.id)
                    resumed += 1
                }

                DownloadState.FAILED -> {
                    if (retryFailed) {
                        container.downloadCoordinator.retry(existing.id)
                        retried += 1
                    } else {
                        skipped += 1
                    }
                }

                null -> {
                    val now = System.currentTimeMillis()
                    container.downloadCoordinator.enqueue(
                        DownloadJob(
                            id = UUID.randomUUID().toString(),
                            mediaId = mediaId,
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
                        ),
                    )
                    queued += 1
                }
            }
        }
        return BulkDownloadResult(
            queued = queued,
            resumed = resumed,
            retried = retried,
            skipped = skipped,
        )
    }

    fun pauseDownload(jobId: String) {
        viewModelScope.launch {
            container.downloadCoordinator.pause(jobId)
            _state.update { it.copy(message = "Paused download") }
        }
    }

    fun resumeDownload(jobId: String) {
        viewModelScope.launch {
            container.downloadCoordinator.resume(jobId)
            _state.update { it.copy(message = "Resumed download") }
        }
    }

    fun retryDownload(jobId: String) {
        viewModelScope.launch {
            container.downloadCoordinator.retry(jobId)
            _state.update { it.copy(message = "Retrying download") }
        }
    }

    fun removeDownload(jobId: String) {
        viewModelScope.launch {
            container.downloadCoordinator.remove(jobId)
            refreshDownloadState()
            _state.update { it.copy(message = "Removed download") }
        }
    }

    fun removeDownloadsForMedia(mediaId: Int) {
        viewModelScope.launch {
            val title = _state.value.mediaTitle(mediaId)
            container.downloadCoordinator.removeMedia(mediaId)
            refreshDownloadState()
            _state.update { it.copy(message = "Removed downloads for $title") }
        }
    }

    fun removeDownloadsForMediaSource(mediaId: Int, sourceId: Long) {
        viewModelScope.launch {
            val snapshot = _state.value
            val title = snapshot.mediaTitle(mediaId)
            val sourceName = snapshot.downloadSourceName(sourceId)
            container.downloadCoordinator.removeMediaSource(mediaId, sourceId)
            refreshDownloadState()
            _state.update { it.copy(message = "Removed $sourceName downloads for $title") }
        }
    }

    fun removeAllDownloads() {
        viewModelScope.launch {
            container.downloadCoordinator.removeAll()
            refreshDownloadState()
            _state.update { it.copy(message = "Removed all downloads") }
        }
    }

    fun retryFailedDownloads() {
        val failed = _state.value.downloads.filter { it.state == DownloadState.FAILED }
        viewModelScope.launch {
            failed.forEach { container.downloadCoordinator.retry(it.id) }
            if (failed.isNotEmpty()) {
                _state.update { it.copy(message = "Retrying ${failed.size} failed downloads") }
            }
        }
    }

    fun pauseActiveDownloads() {
        val active = _state.value.downloads.filter { it.state == DownloadState.QUEUED || it.state == DownloadState.RUNNING }
        viewModelScope.launch {
            active.forEach { container.downloadCoordinator.pause(it.id) }
            if (active.isNotEmpty()) {
                _state.update { it.copy(message = "Paused ${active.size} downloads") }
            }
        }
    }

    fun resumePausedDownloads() {
        val paused = _state.value.downloads.filter { it.state == DownloadState.PAUSED }
        viewModelScope.launch {
            paused.forEach { container.downloadCoordinator.resume(it.id) }
            if (paused.isNotEmpty()) {
                _state.update { it.copy(message = "Resumed ${paused.size} downloads") }
            }
        }
    }

    private suspend fun refreshDownloadState() {
        val downloads = container.database.downloadDao().allDownloads().map { it.toModel() }
        val storageSummary = loadDownloadStorageSummary(downloads)
        _state.update {
            it.copy(
                downloads = downloads,
                downloadStorageSummary = storageSummary,
            )
        }
    }

    private suspend fun loadDownloadStorageSummary(downloads: List<DownloadJob>) =
        withContext(Dispatchers.IO) {
            val pages = container.database.downloadPageDao().allPages()
            buildDownloadStorageSummary(downloads, pages)
        }

    private fun saveReaderProgress() {
        val media = _state.value.selectedMedia ?: return
        val chapter = _state.value.activeChapter ?: return
        val pages = _state.value.readerPages
        if (pages.isEmpty()) return
        val position = latestReaderPosition?.takeIf { it.chapterUrl == chapter.url }
        saveReaderProgressFor(
            media = media,
            chapter = chapter,
            pages = pages,
            pageIndex = position?.pageIndex ?: _state.value.currentPageIndex,
            pageScrollOffset = position?.pageScrollOffset ?: _state.value.currentPageScrollOffset,
        )
    }

    private fun recordReaderPosition(chapterUrl: String, pageIndex: Int, pageScrollOffset: Int) {
        latestReaderPosition = ReaderPagePosition(
            chapterUrl = chapterUrl,
            pageIndex = pageIndex.coerceAtLeast(0),
            pageScrollOffset = pageScrollOffset.coerceAtLeast(0),
        )
    }

    private fun saveReaderProgressFor(
        media: AnilistMedia,
        chapter: SourceChapter,
        pages: List<ReaderPage>,
        pageIndex: Int,
        pageScrollOffset: Int = 0,
    ) {
        if (pages.isEmpty()) return
        val normalizedPageIndex = pageIndex.coerceIn(0, pages.lastIndex)
        val session = ReaderSession(
            mediaId = media.id,
            chapter = chapter,
            pages = pages,
            mode = _state.value.readerMode,
            currentPageIndex = normalizedPageIndex,
            currentPageScrollOffset = pageScrollOffset.coerceAtLeast(0),
        )
        val progress = progressCalculator.progressFor(session, nextReaderProgressTimestamp())
        viewModelScope.launch {
            container.database.progressDao().upsertProgress(progress.toEntity())
            _state.update {
                if (it.selectedMedia?.id == media.id) {
                    it.copy(
                        latestProgress = progress,
                        chapterProgress = it.chapterProgress + (progress.chapterUrl to progress),
                    )
                } else {
                    it
                }
            }
            loadRecentReadingProgress()
            if (_state.value.keepNextTenDownloads) {
                ensureNextTenDownloads()
            }
            if (progress.completed && chapter.chapterNumber > 0) {
                syncAniListProgressFromChapter(
                    media = media,
                    chapterProgress = chapter.chapterNumber.toInt(),
                    triggeredByManualRead = false,
                )
            }
        }
    }

    private fun nextReaderProgressTimestamp(): Long {
        val now = System.currentTimeMillis()
        val next = if (now > lastReaderProgressSavedAtEpochMillis) {
            now
        } else {
            lastReaderProgressSavedAtEpochMillis + 1L
        }
        lastReaderProgressSavedAtEpochMillis = next
        return next
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
        private const val RECENT_READING_LIMIT = 3
        private const val SOURCE_SEARCH_CONCURRENCY = 4
        private const val SOURCE_CANDIDATES_TO_VERIFY = 5
        private const val SOURCE_FALLBACK_CANDIDATES_PER_SOURCE = 5
        private const val SOURCE_MAX_CANDIDATES_PER_SOURCE = 40
        private const val SOURCE_STRONG_MATCH_SCORE = 0.9
        private const val TRACKING_AUTO_SAVE_DELAY_MILLIS = 1_200L
        private const val READER_CACHE_BACK_PAGES = 5
        private const val READER_CACHE_FORWARD_PAGES = 10
        private const val READER_ADJACENT_CACHE_PAGE_COUNT = 2
        private const val READER_CACHE_INITIAL_DELAY_MILLIS = 500L
        private const val READER_ADJACENT_CACHE_INITIAL_DELAY_MILLIS = 12_000L
        private const val READER_CACHE_REQUEST_SPACING_MILLIS = 250L
    }
}
