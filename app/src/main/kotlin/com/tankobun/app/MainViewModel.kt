package com.tankobun.app

import com.tankobun.app.backup.BackupDataSource
import com.tankobun.app.backup.isDue
import com.tankobun.app.anilist.AniListDataSource
import com.tankobun.app.browse.BrowseDataSource
import com.tankobun.app.download.DownloadDataSource
import com.tankobun.app.extensions.ExtensionDataSource
import com.tankobun.app.extensions.InstalledExtensionVersion
import com.tankobun.app.logic.BROWSE_LANDING_SECTION_SIZE
import com.tankobun.app.logic.BROWSE_MANHWA_CACHE_KEY
import com.tankobun.app.logic.BROWSE_POPULAR_CACHE_KEY
import com.tankobun.app.logic.BROWSE_SORT_SEARCH_MATCH
import com.tankobun.app.logic.BROWSE_TOP_MANGA_CACHE_KEY
import com.tankobun.app.logic.BROWSE_TRENDING_CACHE_KEY
import com.tankobun.app.logic.BulkDownloadResult
import com.tankobun.app.logic.BrowseLandingData
import com.tankobun.app.logic.browseCacheKey
import com.tankobun.app.logic.bulkDownloadMessage
import com.tankobun.app.logic.downloadSourceName
import com.tankobun.app.logic.filteredScoreInput
import com.tankobun.app.logic.formatTrackingScore
import com.tankobun.app.logic.hasBrowseQueryOrFilters
import com.tankobun.app.logic.hasContent
import com.tankobun.app.logic.languageSortPriority
import com.tankobun.app.logic.mediaTitle
import com.tankobun.app.logic.nextInReadingOrderAfter
import com.tankobun.app.logic.nextTenDownloadCandidates
import com.tankobun.app.logic.normalizedLanguage
import com.tankobun.app.logic.normalizedCustomLists
import com.tankobun.app.logic.preferredVisibleSources
import com.tankobun.app.logic.previousInReadingOrderBefore
import com.tankobun.app.logic.readerLoadErrorFor
import com.tankobun.app.logic.readerSourceForChapter
import com.tankobun.app.logic.sourceSettingsKey
import com.tankobun.app.logic.sourcePickerDefaultSearchTitle
import com.tankobun.app.logic.sourcePickerErrorMessage
import com.tankobun.app.logic.sourceMatchKey
import com.tankobun.app.logic.toAniListScore
import com.tankobun.app.logic.userMessage
import com.tankobun.app.logic.visibleSources
import com.tankobun.app.logic.withDeletedAniListCustomList
import com.tankobun.app.logic.withAddedTrackingCustomList
import com.tankobun.app.logic.withAniListTitleLanguage
import com.tankobun.app.logic.withRenamedAniListCustomList
import com.tankobun.app.logic.withSelectedAniListDetails
import com.tankobun.app.logic.withSyncedListEntry
import com.tankobun.app.logic.withRecomputedTrackingDirty
import com.tankobun.app.logic.withTrackingCustomListSelected
import com.tankobun.app.logic.withTrackingCustomListSaveResult
import com.tankobun.app.logic.withTrackingSaveFailure
import com.tankobun.app.logic.withTrackingSaveResult
import com.tankobun.app.logic.withTrackingSaveStarted
import com.tankobun.app.reader.ReaderDataSource
import com.tankobun.app.source.SourceDataSource
import com.tankobun.app.source.SourcePickerSearchUpdate
import com.tankobun.app.state.ExtensionInstallRequest
import com.tankobun.app.state.ReaderChapterSegment
import com.tankobun.app.state.ReaderLoadError
import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.app.state.TankobunUiState

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tankobun.core.anilist.AnilistOAuth
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.CachePolicy
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceSearchResult
import com.tankobun.core.model.withTitleLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private data class ReaderPagePosition(
    val chapterUrl: String,
    val pageIndex: Int,
    val pageScrollOffset: Int,
)

private enum class ReaderSegmentDirection {
    PREVIOUS,
    NEXT,
}


class MainViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val cachePolicy = CachePolicy()
    private val aniListDataSource = AniListDataSource(container, cachePolicy)
    private val backupDataSource = BackupDataSource(container)
    private val downloadDataSource = DownloadDataSource(container)
    private val extensionDataSource = ExtensionDataSource(container)
    private val readerDataSource = ReaderDataSource(container)
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
    private val browseDataSource = BrowseDataSource(container, cachePolicy) { _state.value.anilistTitleLanguage }
    private val sourceDataSource = SourceDataSource(container, cachePolicy)
    val state: StateFlow<TankobunUiState> = _state

    init {
        viewModelScope.launch {
            downloadDataSource.observeDownloads().collect { downloads ->
                val storageSummary = downloadDataSource.storageSummary(downloads)
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
                aniListDataSource.refreshViewer(token)
            }.onSuccess { viewer ->
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

    fun refreshInstalledSources() {
        viewModelScope.launch {
            val sourceState = extensionDataSource.installedSourceState(
                preferredLanguages = _state.value.sourceLanguages,
                disabledSourceKeys = _state.value.disabledSourceKeys,
            )
            val selectedSourceId = _state.value.selectedSourceId
            _state.update {
                it.copy(
                    allInstalledSources = sourceState.allSources,
                    installedSources = sourceState.preferredSources,
                    selectedSourceId = selectedSourceId
                        ?.takeIf { current -> sourceState.preferredSources.any { source -> source.id == current } }
                        ?: sourceState.preferredSources.firstOrNull()?.id,
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
                extensionDataSource.fetchExtensionIndex(repositoryUrl)
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
        extensionDataSource.extensionApkUrl(_state.value.extensionRepositoryUrl.trim(), entry)

    fun extensionIconUrl(entry: ExtensionIndexEntry): String? =
        _state.value.extensionRepositoryUrl.trim()
            .takeIf { it.isNotBlank() }
            ?.let { extensionDataSource.extensionIconUrl(it, entry) }

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
                extensionDataSource.downloadExtensionApk(apkUrl, entry)
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
                installedVersion = extensionDataSource.installedExtensionVersion(request.packageName)
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

    private fun loadCachedLibrary(syncIfEmpty: Boolean = false) {
        viewModelScope.launch {
            val cached = aniListDataSource.cachedLibrary(_state.value.anilistTitleLanguage)
            val items = cached.items
            if (items.isNotEmpty()) {
                _state.update {
                    it.copy(
                        library = items.map { item -> item.media },
                        libraryItems = items,
                        librarySyncedAtEpochMillis = cached.syncedAtEpochMillis,
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
            val items = aniListDataSource.recentReadingProgressItems(
                titleLanguage = _state.value.anilistTitleLanguage,
                limit = RECENT_READING_LIMIT,
            )
            _state.update { it.copy(recentReadingProgress = items) }
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
                aniListDataSource.syncLibrary(token)
            }.onSuccess { synced ->
                val viewer = synced.viewer
                val items = synced.items
                _state.update {
                    it.copy(
                        viewerName = viewer.name,
                        anilistScoreFormat = viewer.scoreFormat,
                        anilistTitleLanguage = viewer.titleLanguage,
                        anilistCustomLists = viewer.mangaCustomLists,
                        library = items.map { item -> item.media },
                        libraryItems = items,
                        librarySyncedAtEpochMillis = synced.syncedAtEpochMillis,
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
                backupDataSource.saveBackup(
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
                backupDataSource.restoreBackup(
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
        if (!backupDataSource.persistBackupFolderPermission(uri)) {
            Log.w(TAG, "Could not persist backup folder permission")
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
                backupDataSource.writeScheduledBackup(folderUri = folderUri, snapshot = snapshot)
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
            aniListDataSource.processDueSyncMutations(
                token = token,
                titleLanguage = _state.value.anilistTitleLanguage,
                scoreFormat = _state.value.anilistScoreFormat,
            ).forEach { synced ->
                _state.update {
                    it.withSyncedListEntry(
                        media = synced.media,
                        entry = synced.entry,
                        updateTrackingForm = synced.updateTrackingForm,
                    )
                }
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

        aniListDataSource.syncProgressFromChapter(
            media = media,
            chapterProgress = chapterProgress,
            token = container.tokenStore.accessToken(),
            scoreFormat = snapshot.anilistScoreFormat,
        )?.let { synced ->
            _state.update {
                it.withSyncedListEntry(
                    media = synced.media,
                    entry = synced.entry,
                    updateTrackingForm = synced.updateTrackingForm,
                )
            }
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
                trending = browseDataSource.cachedBrowseMedia(BROWSE_TRENDING_CACHE_KEY),
                popular = browseDataSource.cachedBrowseMedia(BROWSE_POPULAR_CACHE_KEY),
                popularManhwa = browseDataSource.cachedBrowseMedia(BROWSE_MANHWA_CACHE_KEY),
                topManga = browseDataSource.cachedBrowseMedia(BROWSE_TOP_MANGA_CACHE_KEY).take(BROWSE_LANDING_SECTION_SIZE),
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
                val trending = browseDataSource.cachedAnilistBrowseMedia(BROWSE_TRENDING_CACHE_KEY) {
                    container.anilistRepository.browseManga(
                        sort = "TRENDING_DESC",
                        perPage = BROWSE_LANDING_SECTION_SIZE,
                        accessToken = accessToken,
                    )
                }
                val popular = browseDataSource.cachedAnilistBrowseMedia(BROWSE_POPULAR_CACHE_KEY) {
                    container.anilistRepository.browseManga(
                        sort = "POPULARITY_DESC",
                        perPage = BROWSE_LANDING_SECTION_SIZE,
                        accessToken = accessToken,
                    )
                }
                val popularManhwa = browseDataSource.cachedAnilistBrowseMedia(BROWSE_MANHWA_CACHE_KEY) {
                    container.anilistRepository.browseManga(
                        countryOfOrigin = "KR",
                        sort = "POPULARITY_DESC",
                        perPage = BROWSE_LANDING_SECTION_SIZE,
                        accessToken = accessToken,
                    )
                }
                val topManga = browseDataSource.cachedAnilistBrowseMedia(BROWSE_TOP_MANGA_CACHE_KEY) {
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
                browseDataSource.cachedAnilistBrowseMediaPage(cacheKey) {
                    browseDataSource.fetchBrowseResultsPage(snapshot, page = 1)
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
                browseDataSource.fetchBrowseResultsPage(snapshot, page = nextPage)
            }.onSuccess { page ->
                if (_state.value.browseCacheKey() != cacheKey) return@onSuccess
                val merged = (_state.value.searchResults + page.media).distinctBy { it.id }
                browseDataSource.cacheBrowseMedia(cacheKey, merged)
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
                aniListDataSource.updateTitleLanguage(token, language)
            }.onSuccess { viewer ->
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
                aniListDataSource.updateScoreFormat(token, format)
            }.onSuccess { viewer ->
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
                aniListDataSource.updateCustomLists(token, nextLists)
            }.onSuccess { savedLists ->
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
                aniListDataSource.renameCustomListEntries(
                    token = token,
                    items = snapshot.libraryItems,
                    oldName = normalizedOldName,
                    newName = normalizedNewName,
                    nextCustomLists = nextLists,
                    scoreFormat = snapshot.anilistScoreFormat,
                )
            }.onSuccess { result ->
                _state.update {
                    it.withRenamedAniListCustomList(
                        customLists = result.customLists,
                        updatedEntries = result.updatedEntries,
                        oldName = normalizedOldName,
                        newName = normalizedNewName,
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
                aniListDataSource.deleteCustomListEntries(
                    token = token,
                    items = snapshot.libraryItems,
                    listName = normalizedName,
                    nextCustomLists = nextLists,
                    scoreFormat = snapshot.anilistScoreFormat,
                )
            }.onSuccess { result ->
                _state.update {
                    it.withDeletedAniListCustomList(
                        customLists = result.customLists,
                        updatedEntries = result.updatedEntries,
                        name = normalizedName,
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
        _state.update { it.withTrackingCustomListSelected(normalizedName, selected) }
        scheduleTrackingCustomListSave()
    }

    fun addTrackingCustomList(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        _state.update { it.withAddedTrackingCustomList(normalizedName) }
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
                aniListDataSource.saveTracking(
                    token = token,
                    media = media,
                    status = if (snapshot.selectedListEntry == null) snapshot.trackingStatus else null,
                    progress = null,
                    score = null,
                    notes = null,
                    private = null,
                    customLists = customLists,
                    knownCustomLists = knownCustomLists,
                    scoreFormat = snapshot.anilistScoreFormat,
                )
            }.onSuccess { result ->
                _state.update {
                    it.withTrackingCustomListSaveResult(
                        media = media,
                        entry = result.entry,
                        knownCustomLists = result.knownCustomLists,
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
            _state.update {
                it.withTrackingSaveStarted(
                    media = media,
                    entry = optimisticEntry,
                    knownCustomLists = optimisticKnownCustomLists,
                    autoSave = autoSave,
                )
            }
            runCatching {
                aniListDataSource.saveTracking(
                    token = token,
                    media = media,
                    status = snapshot.trackingStatus,
                    progress = progress,
                    score = score,
                    notes = notes,
                    private = snapshot.trackingPrivate,
                    customLists = customLists,
                    knownCustomLists = knownCustomLists,
                    scoreFormat = snapshot.anilistScoreFormat,
                )
            }.onSuccess { result ->
                _state.update {
                    it.withTrackingSaveResult(
                        media = media,
                        entry = result.entry,
                        knownCustomLists = result.knownCustomLists,
                        autoSave = autoSave,
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList tracking save failed for ${media.id}", error)
                _state.update {
                    it.withTrackingSaveFailure(
                        mediaId = media.id,
                        autoSave = autoSave,
                        message = error.userMessage(
                            if (autoSave) "Tracking auto-save failed. Tap save to retry" else "Tracking save failed",
                        ),
                    )
                }
            }
        }
    }

    private fun loadAnilistDetails(mediaId: Int) {
        viewModelScope.launch {
            val cached = aniListDataSource.cachedMediaDetails(
                mediaId = mediaId,
                titleLanguage = _state.value.anilistTitleLanguage,
            )
            if (cached.media != null || cached.entry != null || cached.recommendations.isNotEmpty()) {
                _state.update {
                    it.withSelectedAniListDetails(
                        mediaId = mediaId,
                        media = cached.media,
                        entry = cached.entry,
                        recommendations = cached.recommendations,
                        recommendationsPage = cached.recommendationsPage,
                        recommendationsHasMore = cached.recommendationsHasMore,
                    )
                }
            }

            if (cached.isFresh) return@launch

            runCatching {
                aniListDataSource.fetchMediaDetails(
                    mediaId = mediaId,
                    accessToken = container.tokenStore.accessToken(),
                    scoreFormat = _state.value.anilistScoreFormat,
                    titleLanguage = _state.value.anilistTitleLanguage,
                )
            }.onSuccess { details ->
                _state.update {
                    it.withSelectedAniListDetails(
                        mediaId = mediaId,
                        media = details.media,
                        entry = details.entry ?: cached.entry,
                        recommendations = details.recommendations,
                        recommendationsPage = details.recommendationsPage,
                        recommendationsHasMore = details.recommendationsHasMore,
                    )
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
                aniListDataSource.fetchRecommendationPage(
                    mediaId = mediaId,
                    page = nextPage,
                    accessToken = token,
                    titleLanguage = _state.value.anilistTitleLanguage,
                )
            }.onSuccess { recommendationPage ->
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

    private fun loadCachedSourceState(mediaId: Int) {
        viewModelScope.launch {
            val sources = _state.value.allInstalledSources.ifEmpty { _state.value.installedSources }
            val cached = sourceDataSource.cachedSourceState(mediaId, sources)
            _state.update {
                if (it.selectedMedia?.id != mediaId) {
                    it
                } else {
                    it.copy(
                        sourceMatches = cached.sourceMatches,
                        sourceMatchChapterCounts = cached.sourceMatchChapterCounts,
                        selectedSourceId = cached.boundSource?.id ?: it.selectedSourceId,
                        selectedSourceManga = cached.boundManga,
                        sourceChapters = cached.sourceChapters,
                        latestProgress = cached.latestProgress,
                        chapterProgress = cached.chapterProgress,
                    )
                }
            }
        }
    }

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
                val match = sourceDataSource.readableSourceMatch(media, source, now)
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
                sourceDataSource.saveSourceBinding(match)
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
                    sourceDataSource.cachedVerifiedMatches(media.id, sources, now)
                        .takeIf { !forceRefresh && it.matches.isNotEmpty() }
                        ?: sourceDataSource.searchVerifiedMatches(
                            media = media,
                            sources = sources,
                            now = now,
                            onUpdate = { update -> publishSourcePickerUpdate(requestId, media.id, update) },
                        ).also { verified ->
                            sourceDataSource.cacheVerifiedMatches(media.id, verified.matches)
                        }
                } else {
                    sourceDataSource.searchVerifiedMatches(
                        media = media,
                        sources = sources,
                        now = now,
                        titleOverride = editedTitle,
                        onUpdate = { update -> publishSourcePickerUpdate(requestId, media.id, update) },
                    )
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

    private fun publishSourcePickerUpdate(requestId: Long, mediaId: Int, update: SourcePickerSearchUpdate) {
        when (update) {
            is SourcePickerSearchUpdate.Match ->
                publishSourcePickerMatch(requestId, mediaId, update.match, update.chapterCount)
            is SourcePickerSearchUpdate.Diagnostic ->
                publishSourcePickerDiagnostic(requestId, mediaId, update.source, update.detail)
        }
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
                val resolved = sourceDataSource.resolveMangaDetails(match)
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
                sourceDataSource.saveSourceBinding(resolved)
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

    private fun loadChapters(source: SourceDescriptor, manga: com.tankobun.core.model.SourceManga) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                val now = System.currentTimeMillis()
                sourceDataSource.loadChapters(
                    source = source,
                    manga = manga,
                    mediaId = _state.value.selectedMedia?.id,
                    now = now,
                )
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
                readerDataSource.loadPagesForChapter(media.id, chapter, source)
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
                    readerDataSource.cachedProgressForChapter(media.id, chapter.url)
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
                        readerDataSource.loadPagesForChapter(mediaId, adjacentChapter, previousSource)
                    }.getOrDefault(emptyList())
                }
            }
            val nextDeferred = nextChapter?.let { adjacentChapter ->
                async {
                    runCatching {
                        readerDataSource.loadPagesForChapter(mediaId, adjacentChapter, nextSource)
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
                    pages = readerDataSource.adjacentTailPages(segment.pages),
                    cacheKeySuffix = "tail",
                    initialDelayMillis = ReaderDataSource.ADJACENT_CACHE_INITIAL_DELAY_MILLIS,
                )
            }
            nextSegment?.let { segment ->
                startReaderPageCache(
                    source = nextSource,
                    mediaId = mediaId,
                    chapter = segment.chapter,
                    pages = readerDataSource.adjacentHeadPages(segment.pages),
                    cacheKeySuffix = "head",
                    initialDelayMillis = ReaderDataSource.ADJACENT_CACHE_INITIAL_DELAY_MILLIS,
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
        val windowPages = readerDataSource.cacheWindowPages(
            pages = pages,
            pageIndex = pageIndex.coerceIn(0, pages.lastIndex),
            preferredDirection = preferredDirection,
        )
        startReaderPageCache(
            source = source,
            mediaId = mediaId,
            chapter = chapter,
            pages = windowPages,
            cacheKeySuffix = "window",
            replaceExisting = true,
            initialDelayMillis = ReaderDataSource.CACHE_INITIAL_DELAY_MILLIS,
        )
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
        val job = viewModelScope.launch {
            readerDataSource.cachePages(
                source = source,
                mediaId = mediaId,
                chapter = chapter,
                pages = pagesToCache,
                initialDelayMillis = initialDelayMillis,
            )
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
            val result = readerDataSource.markChapterRead(
                mediaId = media.id,
                chapter = chapter,
                read = read,
                readerMode = _state.value.readerMode,
                nowMillis = System.currentTimeMillis(),
            )
            val syncProgress = result.syncProgress
            if (syncProgress != null) {
                val trackedProgress = _state.value.trackingProgress.toIntOrNull()
                    ?: _state.value.selectedListEntry?.progress
                    ?: 0
                if (syncProgress > trackedProgress) {
                    syncAniListProgressFromChapter(
                        media = media,
                        chapterProgress = syncProgress,
                        triggeredByManualRead = true,
                    )
                }
            }

            _state.update {
                if (it.selectedMedia?.id == media.id) {
                    it.copy(
                        latestProgress = result.latestProgress,
                        chapterProgress = result.chapterProgress,
                        trackingProgress = if (syncProgress != null && syncProgress > (it.trackingProgress.toIntOrNull() ?: 0)) {
                            syncProgress.toString()
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
                    val existing = downloadDataSource.latestForChapter(media.id, chapter.url)
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
        return downloadDataSource.enqueueChapters(
            mediaId = mediaId,
            chapters = chapters,
            retryFailed = retryFailed,
        )
    }

    fun pauseDownload(jobId: String) {
        viewModelScope.launch {
            downloadDataSource.pause(jobId)
            _state.update { it.copy(message = "Paused download") }
        }
    }

    fun resumeDownload(jobId: String) {
        viewModelScope.launch {
            downloadDataSource.resume(jobId)
            _state.update { it.copy(message = "Resumed download") }
        }
    }

    fun retryDownload(jobId: String) {
        viewModelScope.launch {
            downloadDataSource.retry(jobId)
            _state.update { it.copy(message = "Retrying download") }
        }
    }

    fun removeDownload(jobId: String) {
        viewModelScope.launch {
            downloadDataSource.remove(jobId)
            refreshDownloadState()
            _state.update { it.copy(message = "Removed download") }
        }
    }

    fun removeDownloadsForMedia(mediaId: Int) {
        viewModelScope.launch {
            val title = _state.value.mediaTitle(mediaId)
            downloadDataSource.removeMedia(mediaId)
            refreshDownloadState()
            _state.update { it.copy(message = "Removed downloads for $title") }
        }
    }

    fun removeDownloadsForMediaSource(mediaId: Int, sourceId: Long) {
        viewModelScope.launch {
            val snapshot = _state.value
            val title = snapshot.mediaTitle(mediaId)
            val sourceName = snapshot.downloadSourceName(sourceId)
            downloadDataSource.removeMediaSource(mediaId, sourceId)
            refreshDownloadState()
            _state.update { it.copy(message = "Removed $sourceName downloads for $title") }
        }
    }

    fun removeAllDownloads() {
        viewModelScope.launch {
            downloadDataSource.removeAll()
            refreshDownloadState()
            _state.update { it.copy(message = "Removed all downloads") }
        }
    }

    fun retryFailedDownloads() {
        val failed = _state.value.downloads.filter { it.state == DownloadState.FAILED }
        viewModelScope.launch {
            failed.forEach { downloadDataSource.retry(it.id) }
            if (failed.isNotEmpty()) {
                _state.update { it.copy(message = "Retrying ${failed.size} failed downloads") }
            }
        }
    }

    fun pauseActiveDownloads() {
        val active = _state.value.downloads.filter { it.state == DownloadState.QUEUED || it.state == DownloadState.RUNNING }
        viewModelScope.launch {
            active.forEach { downloadDataSource.pause(it.id) }
            if (active.isNotEmpty()) {
                _state.update { it.copy(message = "Paused ${active.size} downloads") }
            }
        }
    }

    fun resumePausedDownloads() {
        val paused = _state.value.downloads.filter { it.state == DownloadState.PAUSED }
        viewModelScope.launch {
            paused.forEach { downloadDataSource.resume(it.id) }
            if (paused.isNotEmpty()) {
                _state.update { it.copy(message = "Resumed ${paused.size} downloads") }
            }
        }
    }

    private suspend fun refreshDownloadState() {
        val downloads = downloadDataSource.allDownloads()
        val storageSummary = downloadDataSource.storageSummary(downloads)
        _state.update {
            it.copy(
                downloads = downloads,
                downloadStorageSummary = storageSummary,
            )
        }
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
        val readerMode = _state.value.readerMode
        val updatedAtMillis = nextReaderProgressTimestamp()
        viewModelScope.launch {
            val progress = readerDataSource.saveProgress(
                mediaId = media.id,
                chapter = chapter,
                pages = pages,
                readerMode = readerMode,
                pageIndex = pageIndex,
                pageScrollOffset = pageScrollOffset,
                updatedAtMillis = updatedAtMillis,
            )
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
        private const val RECENT_READING_LIMIT = 3
        private const val TRACKING_AUTO_SAVE_DELAY_MILLIS = 1_200L
    }
}
