package com.tankobun.app

import com.tankobun.app.backup.BackupDataSource
import com.tankobun.app.backup.AppSettingsBackupDataSource
import com.tankobun.app.backup.isDue
import com.tankobun.app.anilist.AniListDataSource
import com.tankobun.app.browse.BrowseDataSource
import com.tankobun.app.cache.CacheClearTarget
import com.tankobun.app.cache.CacheStorageDataSource
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
import com.tankobun.app.logic.browseLandingCacheKey
import com.tankobun.app.logic.downloadSourceName
import com.tankobun.app.logic.filteredScoreInput
import com.tankobun.app.logic.formatTrackingScore
import com.tankobun.app.logic.hasBrowseQueryOrFilters
import com.tankobun.app.logic.hasContent
import com.tankobun.app.logic.mediaTitle
import com.tankobun.app.logic.nextInReadingOrderAfter
import com.tankobun.app.logic.nextTenDownloadCandidates
import com.tankobun.app.logic.normalizedLanguage
import com.tankobun.app.logic.normalizedCustomLists
import com.tankobun.app.logic.CURRENT_ONBOARDING_VERSION
import com.tankobun.app.logic.preferredVisibleSources
import com.tankobun.app.logic.previousInReadingOrderBefore
import com.tankobun.app.logic.readerLoadErrorFor
import com.tankobun.app.logic.readerSourceForChapter
import com.tankobun.app.logic.ReaderSegmentDirection
import com.tankobun.app.logic.selectedSourceChapterSelection
import com.tankobun.app.logic.automaticStatusForReaderPosition
import com.tankobun.app.logic.initialLibraryModeForStartup
import com.tankobun.app.logic.shouldShowOnboarding
import com.tankobun.app.logic.sourceSettingsKey
import com.tankobun.app.logic.sourcePickerDiagnosticDetail
import com.tankobun.app.logic.sourcePickerSources
import com.tankobun.app.logic.toAniListScore
import com.tankobun.app.logic.userMessage
import com.tankobun.app.logic.visibleSources
import com.tankobun.app.logic.withDeletedAniListCustomList
import com.tankobun.app.logic.withAddedTrackingCustomList
import com.tankobun.app.logic.withActivatedReaderSegment
import com.tankobun.app.logic.withAdjacentReaderSegments
import com.tankobun.app.logic.withAniListTitleLanguage
import com.tankobun.app.logic.withRenamedAniListCustomList
import com.tankobun.app.logic.withReaderClosed
import com.tankobun.app.logic.withReaderLoadError
import com.tankobun.app.logic.withReaderLoading
import com.tankobun.app.logic.withReaderPagePosition
import com.tankobun.app.logic.withReaderPagesLoaded
import com.tankobun.app.logic.withRecentProgressOpened
import com.tankobun.app.logic.withSelectedAniListDetails
import com.tankobun.app.logic.withSelectedMedia
import com.tankobun.app.logic.withSelectedSource
import com.tankobun.app.logic.withSourcePickerClosed
import com.tankobun.app.logic.withSourcePickerDiagnostic
import com.tankobun.app.logic.withSourcePickerEditedTitleTooShort
import com.tankobun.app.logic.withSourcePickerFailure
import com.tankobun.app.logic.withSourcePickerMatchOpening
import com.tankobun.app.logic.withSourcePickerMatchPublished
import com.tankobun.app.logic.withSourcePickerNoSources
import com.tankobun.app.logic.withSourcePickerOpened
import com.tankobun.app.logic.withSourcePickerSearchCompleted
import com.tankobun.app.logic.withSourcePickerSearchStarted
import com.tankobun.app.logic.withSourcePickerSearchTitle
import com.tankobun.app.logic.withSourcePickerSourceSearchStarted
import com.tankobun.app.logic.withSourcePickerSourceSelected
import com.tankobun.app.logic.withSourceChapterSelectionMissing
import com.tankobun.app.logic.withSourceChaptersLoaded
import com.tankobun.app.logic.withSourceChaptersLoadFailed
import com.tankobun.app.logic.withSourceChaptersLoading
import com.tankobun.app.logic.withSyncedListEntry
import com.tankobun.app.logic.withRecomputedTrackingDirty
import com.tankobun.app.logic.withTrackingCustomListSelected
import com.tankobun.app.logic.withTrackingCustomListSaveResult
import com.tankobun.app.logic.withTrackingSaveFailure
import com.tankobun.app.logic.withTrackingSaveResult
import com.tankobun.app.logic.withTrackingSaveStarted
import com.tankobun.app.logic.withoutSelectedMedia
import com.tankobun.app.reader.ReaderDataSource
import com.tankobun.app.source.SourceDataSource
import com.tankobun.app.source.SourcePickerSearchUpdate
import com.tankobun.app.updates.AppUpdateDataSource
import com.tankobun.app.state.ExtensionInstallRequest
import com.tankobun.app.state.AppUpdateInstallRequest
import com.tankobun.app.state.BackupMissingSource
import com.tankobun.app.state.ReaderChapterSegment
import com.tankobun.app.state.ReaderLoadError
import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.app.state.TankobunUiState

import android.net.Uri
import android.util.Log
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
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
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private data class ReaderPagePosition(
    val chapterUrl: String,
    val pageIndex: Int,
    val pageScrollOffset: Int,
)

private data class ScheduledBackupRunResult(
    val libraryItems: Int? = null,
    val sourcePackages: Int? = null,
)

private data class BrowseCriteria(
    val searchQuery: String = "",
    val genres: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val format: String? = null,
    val publishingStatus: String? = null,
    val countryOfOrigin: String? = null,
    val year: Int? = null,
    val staffName: String? = null,
    val sort: String = BROWSE_SORT_SEARCH_MATCH,
) {
    val hasQueryOrFilters: Boolean
        get() = searchQuery.trim().isNotBlank() ||
            genres.isNotEmpty() ||
            tags.isNotEmpty() ||
            format != null ||
            publishingStatus != null ||
            countryOfOrigin != null ||
            year != null ||
            staffName != null ||
            sort != BROWSE_SORT_SEARCH_MATCH
}

private const val BROWSE_TRENDING_SORT = "TRENDING_DESC"
private const val BROWSE_BACK_STACK_LIMIT = 24

class MainViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val cachePolicy = CachePolicy()
    private val aniListDataSource = AniListDataSource(container, cachePolicy)
    private val backupDataSource = BackupDataSource(container)
    private val appSettingsBackupDataSource = AppSettingsBackupDataSource(container)
    private val cacheStorageDataSource = CacheStorageDataSource(container)
    private val downloadDataSource = DownloadDataSource(container)
    private val extensionDataSource = ExtensionDataSource(container)
    private val appUpdateDataSource = AppUpdateDataSource(container)
    private val readerDataSource = ReaderDataSource(container)
    private var trackingAutoSaveJob: Job? = null
    private var pendingAniListSyncJob: Job? = null
    private var scheduledBackupJob: Job? = null
    private var browseLandingJob: Job? = null
    private var readerAdjacentLoadJob: Job? = null
    private var sourcePickerJob: Job? = null
    private var sourcePickerRequestId: Long = 0L
    private var browseBackStack: List<BrowseCriteria> = emptyList()
    private var committedBrowseCriteria: BrowseCriteria = BrowseCriteria()
    private var lastReaderProgressSavedAtEpochMillis: Long = 0L
    private var latestReaderPosition: ReaderPagePosition? = null
    private val readerPageCacheJobs = ConcurrentHashMap<String, Job>()
    private val initialAccessToken = container.tokenStore.accessToken()
    private val initialOnboardingVersion = container.settingsStore.onboardingVersion()
    private val initialLibraryMode = initialLibraryModeForStartup(
        storedMode = container.settingsStore.libraryMode(),
        hasAccessToken = initialAccessToken != null,
        onboardingVersion = initialOnboardingVersion,
    )
    private val _state = MutableStateFlow(
        TankobunUiState(
            loggedIn = initialAccessToken != null,
            clientConfigured = BuildConfig.ANILIST_CLIENT_ID.isNotBlank(),
            libraryMode = initialLibraryMode,
            viewerName = container.settingsStore.viewerName(),
            anilistScoreFormat = container.settingsStore.anilistScoreFormat(),
            anilistTitleLanguage = container.settingsStore.anilistTitleLanguage(),
            anilistCustomLists = container.settingsStore.anilistCustomLists(),
            librarySyncedAtEpochMillis = container.settingsStore.librarySyncedAtEpochMillis(),
            backupFolderUri = container.settingsStore.backupFolderUri(),
            backupSchedule = container.settingsStore.backupSchedule(),
            backupContent = container.settingsStore.backupContent(),
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
            appLanguage = container.settingsStore.appLanguage(),
            ignoreDisplayCutout = container.settingsStore.ignoreDisplayCutout(),
            showAppStatusBar = container.settingsStore.showAppStatusBar(),
            dockAlignment = container.settingsStore.dockAlignment(),
            onboardingVisible = shouldShowOnboarding(initialOnboardingVersion),
            readerTutorialVisible = !container.settingsStore.readerTutorialCompleted(),
            readerMode = container.settingsStore.readerMode(),
            readerPageGapLevel = container.settingsStore.readerPageGapLevel(),
            chapterListStartsAtFirst = container.settingsStore.chapterListStartsAtFirst(),
            keepNextTenDownloads = container.settingsStore.keepNextTenDownloads(),
            newChapterChecksEnabled = container.settingsStore.newChapterChecksEnabled(),
            lastNewChapterCheckAtEpochMillis = container.settingsStore.lastNewChapterCheckAtEpochMillis(),
            appUpdateLastCheckedAtEpochMillis = container.settingsStore.lastAppUpdateCheckAtEpochMillis(),
            viewerAvatarUrl = container.settingsStore.viewerAvatarUrl(),
            viewerBannerImageUrl = container.settingsStore.viewerBannerImageUrl(),
            anilistMangaStats = container.settingsStore.anilistMangaStats(),
            showNsfwContent = container.settingsStore.showNsfwContent(),
            anilistAutoSaveTrackingChanges = container.settingsStore.anilistAutoSaveTrackingChanges(),
            anilistAutoSyncReaderProgress = container.settingsStore.anilistAutoSyncReaderProgress(),
            anilistSyncManualReadProgress = container.settingsStore.anilistSyncManualReadProgress(),
            autoUpdateStatusFromReading = container.settingsStore.autoUpdateStatusFromReading(),
        ),
    )
    private fun localizedContext() =
        container.application.withAppLanguage(_state.value.appLanguage)

    private fun string(@StringRes id: Int, vararg args: Any): String =
        localizedContext().getString(id, *args)

    private fun quantityString(@PluralsRes id: Int, quantity: Int, vararg args: Any): String =
        localizedContext().resources.getQuantityString(id, quantity, *args)

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
        refreshCacheStorageSummary()
        if (_state.value.libraryMode == LibraryMode.LOCAL || _state.value.loggedIn) {
            loadCachedLibrary(syncIfEmpty = _state.value.libraryMode == LibraryMode.ANILIST && _state.value.loggedIn)
        }
        if (_state.value.loggedIn) {
            refreshAniListViewer()
            processPendingAniListSync()
        }
        checkForAppUpdateIfDue()
    }

    fun loginUrl(): String? {
        if (BuildConfig.ANILIST_CLIENT_ID.isBlank()) return null
        val state = newOAuthState()
        container.settingsStore.savePendingAnilistOAuthState(state)
        return AnilistOAuth.authorizationUrl(
            clientId = BuildConfig.ANILIST_CLIENT_ID,
            redirectUri = BuildConfig.ANILIST_REDIRECT_URI,
            state = state,
        )
    }

    fun handleOAuthRedirect(uri: String) {
        val token = AnilistOAuth.parseRedirect(uri) ?: return
        val expectedState = container.settingsStore.pendingAnilistOAuthState()
        if (expectedState.isNullOrBlank() || token.state != expectedState) {
            Log.w(TAG, "Ignoring AniList OAuth redirect with missing or mismatched state.")
            return
        }
        container.settingsStore.savePendingAnilistOAuthState(null)
        container.tokenStore.saveAccessToken(token.accessToken)
        val shouldGuideMerge = _state.value.libraryMode == LibraryMode.LOCAL && _state.value.libraryItems.isNotEmpty()
        _state.update {
            it.copy(
                loggedIn = true,
                anilistMergePromptVisible = shouldGuideMerge,
                message = string(R.string.msg_anilist_connected),
            )
        }
        if (!shouldGuideMerge) {
            container.settingsStore.saveLibraryMode(LibraryMode.ANILIST)
            _state.update { it.copy(libraryMode = LibraryMode.ANILIST) }
            refreshLibrary()
        }
    }

    private fun newOAuthState(): String {
        val bytes = ByteArray(OAUTH_STATE_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun signOut() {
        container.tokenStore.clear()
        container.settingsStore.saveLibraryMode(LibraryMode.LOCAL)
        container.settingsStore.savePendingAnilistOAuthState(null)
        container.settingsStore.saveViewerName(null)
        container.settingsStore.saveViewerAvatarUrl(null)
        container.settingsStore.saveViewerBannerImageUrl(null)
        container.settingsStore.saveAnilistMangaStats(null)
        _state.update {
            it.copy(
                loggedIn = false,
                libraryMode = LibraryMode.LOCAL,
                viewerName = null,
                viewerAvatarUrl = null,
                viewerBannerImageUrl = null,
                anilistMangaStats = null,
                message = string(R.string.msg_signed_out),
            )
        }
    }

    fun setLibraryMode(mode: LibraryMode) {
        if (_state.value.libraryMode == mode) return
        container.settingsStore.saveLibraryMode(mode)
        _state.update { it.copy(libraryMode = mode) }
        if (mode == LibraryMode.LOCAL) {
            loadCachedLibrary()
        } else if (_state.value.loggedIn) {
            refreshLibrary()
        }
    }

    fun dismissAniListMergePrompt() {
        _state.update { it.copy(anilistMergePromptVisible = false) }
    }

    fun mergeLocalLibraryWithAniList() {
        val token = container.tokenStore.accessToken() ?: return
        val snapshot = _state.value
        val localItems = snapshot.libraryItems
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null, anilistMergePromptVisible = false) }
            runCatching {
                aniListDataSource.mergeLocalLibraryToAniList(
                    token = token,
                    localItems = localItems,
                    knownCustomLists = snapshot.anilistCustomLists,
                    scoreFormat = snapshot.anilistScoreFormat,
                )
            }.onSuccess { synced ->
                val viewer = synced.viewer
                container.settingsStore.saveLibraryMode(LibraryMode.ANILIST)
                _state.update {
                    it.copy(
                        libraryMode = LibraryMode.ANILIST,
                        viewerName = viewer.name,
                        viewerAvatarUrl = viewer.avatarUrl,
                        viewerBannerImageUrl = viewer.bannerImageUrl,
                        anilistMangaStats = viewer.mangaStats,
                        anilistScoreFormat = viewer.scoreFormat,
                        anilistTitleLanguage = viewer.titleLanguage,
                        anilistCustomLists = viewer.mangaCustomLists,
                        library = synced.items.map { item -> item.media },
                        libraryItems = synced.items,
                        librarySyncedAtEpochMillis = synced.syncedAtEpochMillis,
                        busy = false,
                        message = string(R.string.msg_library_merged),
                    )
                }
                loadRecentReadingProgress()
                runScheduledAniListBackupIfDue()
            }.onFailure { error ->
                Log.e(TAG, "AniList local library merge failed", error)
                _state.update {
                    it.copy(
                        busy = false,
                        anilistMergePromptVisible = true,
                        message = error.userMessage(localizedContext(), string(R.string.msg_library_merge_failed)),
                    )
                }
            }
        }
    }

    fun replaceLocalLibraryWithAniList() {
        container.settingsStore.saveLibraryMode(LibraryMode.ANILIST)
        _state.update {
            it.copy(
                libraryMode = LibraryMode.ANILIST,
                anilistMergePromptVisible = false,
            )
        }
        refreshLibrary()
    }

    fun showOnboarding() {
        _state.update { it.copy(onboardingVisible = true) }
    }

    fun dismissOnboarding() {
        container.settingsStore.saveOnboardingVersion(CURRENT_ONBOARDING_VERSION)
        _state.update { it.copy(onboardingVisible = false) }
    }

    fun completeOnboarding(mode: LibraryMode, themeMode: TankobunThemeMode) {
        container.settingsStore.saveLibraryMode(mode)
        container.settingsStore.saveThemeMode(themeMode)
        container.settingsStore.saveOnboardingVersion(CURRENT_ONBOARDING_VERSION)
        _state.update {
            it.copy(
                libraryMode = mode,
                themeMode = themeMode,
                onboardingVisible = false,
            )
        }
        if (mode == LibraryMode.LOCAL) {
            loadCachedLibrary()
        } else if (_state.value.loggedIn) {
            refreshLibrary()
        }
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
                        viewerAvatarUrl = viewer.avatarUrl,
                        viewerBannerImageUrl = viewer.bannerImageUrl,
                        anilistMangaStats = viewer.mangaStats,
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

    fun setAppLanguage(language: AppLanguage) {
        container.settingsStore.saveAppLanguage(language)
        _state.update { it.copy(appLanguage = language) }
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

    fun setNewChapterChecksEnabled(enabled: Boolean) {
        container.settingsStore.saveNewChapterChecksEnabled(enabled)
        _state.update {
            it.copy(
                newChapterChecksEnabled = enabled,
                message = if (enabled) {
                    string(R.string.msg_new_chapter_check_enabled)
                } else {
                    string(R.string.msg_new_chapter_check_disabled)
                },
            )
        }
        NewChapterCheckWork.update(container.application, enabled)
        if (enabled) {
            NewChapterCheckWork.runOnce(container.application)
        }
    }

    fun onNewChapterNotificationPermissionDenied() {
        container.settingsStore.saveNewChapterChecksEnabled(false)
        _state.update {
            it.copy(
                newChapterChecksEnabled = false,
                message = string(R.string.msg_notification_permission_denied),
            )
        }
        NewChapterCheckWork.update(container.application, enabled = false)
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
                _state.update { it.copy(message = string(R.string.msg_paste_repository_first)) }
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
                        message = if (silent) it.message else string(R.string.msg_loaded_extensions, extensions.size),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        busy = if (silent) it.busy else false,
                        message = if (silent) it.message else error.message ?: string(R.string.msg_extension_index_failed),
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
                    message = string(R.string.msg_downloading_extension, entry.name),
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
                        message = string(R.string.msg_ready_to_install_extension, entry.name),
                    )
                }
            }.onFailure { error ->
                Log.w(TAG, "Extension APK download failed for ${entry.packageName}", error)
                _state.update {
                    it.copy(
                        installingExtensionPackageName = null,
                        message = error.message ?: string(R.string.msg_extension_download_failed),
                    )
                }
            }
        }
    }

    fun requireExtensionInstallPermission() {
        _state.update {
            it.copy(message = string(R.string.msg_allow_extension_install))
        }
    }

    fun consumeExtensionInstallRequest() {
        _state.update { it.copy(extensionInstallRequest = null) }
    }

    private fun checkForAppUpdateIfDue() {
        val manifestUrl = BuildConfig.UPDATE_MANIFEST_URL.trim()
        if (manifestUrl.isBlank()) return
        val lastCheck = container.settingsStore.lastAppUpdateCheckAtEpochMillis()
        val now = System.currentTimeMillis()
        if (now - lastCheck >= APP_UPDATE_CHECK_INTERVAL_MILLIS) {
            checkForAppUpdate(silent = true)
        }
    }

    fun checkForAppUpdate() {
        checkForAppUpdate(silent = false)
    }

    private fun checkForAppUpdate(silent: Boolean) {
        val manifestUrl = BuildConfig.UPDATE_MANIFEST_URL.trim()
        if (manifestUrl.isBlank()) {
            if (!silent) {
                val resultMessage = string(R.string.msg_app_update_manifest_not_configured)
                _state.update { it.copy(message = resultMessage, appUpdateMessage = resultMessage) }
            }
            return
        }
        if (_state.value.appUpdateCheckInProgress) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    appUpdateCheckInProgress = true,
                    appUpdateMessage = if (silent) it.appUpdateMessage else null,
                    message = if (silent) it.message else string(R.string.msg_checking_app_updates),
                )
            }
            runCatching {
                appUpdateDataSource.fetchUpdateInfo(manifestUrl)
            }.onSuccess { update ->
                val checkedAt = System.currentTimeMillis()
                container.settingsStore.saveLastAppUpdateCheckAtEpochMillis(checkedAt)
                val available = update.versionCode > BuildConfig.VERSION_CODE
                _state.update {
                    val resultMessage = when {
                        available -> string(R.string.msg_app_update_available, update.versionName)
                        else -> string(R.string.msg_app_update_current)
                    }
                    it.copy(
                        appUpdateInfo = update,
                        appUpdateCheckInProgress = false,
                        appUpdateLastCheckedAtEpochMillis = checkedAt,
                        appUpdateMessage = if (silent) it.appUpdateMessage else resultMessage,
                        message = when {
                            silent -> it.message
                            else -> resultMessage
                        },
                    )
                }
            }.onFailure { error ->
                Log.w(TAG, "App update check failed", error)
                _state.update {
                    val resultMessage = string(R.string.msg_app_update_check_failed)
                    it.copy(
                        appUpdateCheckInProgress = false,
                        appUpdateMessage = if (silent) it.appUpdateMessage else resultMessage,
                        message = if (silent) it.message else resultMessage,
                    )
                }
            }
        }
    }

    fun downloadAppUpdate() {
        val update = _state.value.appUpdateInfo
        if (update == null || update.versionCode <= BuildConfig.VERSION_CODE) {
            val resultMessage = string(R.string.msg_app_update_current)
            _state.update { it.copy(message = resultMessage, appUpdateMessage = resultMessage) }
            return
        }
        if (_state.value.appUpdateDownloadInProgress) return
        viewModelScope.launch {
            _state.update {
                val resultMessage = string(R.string.msg_downloading_app_update, update.versionName)
                it.copy(
                    appUpdateDownloadInProgress = true,
                    appUpdateInstallRequest = null,
                    appUpdateMessage = resultMessage,
                    message = resultMessage,
                )
            }
            runCatching {
                appUpdateDataSource.downloadUpdateApk(update)
            }.onSuccess { apkUri ->
                _state.update {
                    val resultMessage = string(R.string.msg_ready_to_install_app_update, update.versionName)
                    it.copy(
                        appUpdateDownloadInProgress = false,
                        appUpdateInstallRequest = AppUpdateInstallRequest(
                            apkUri = apkUri.toString(),
                            expectedVersionCode = update.versionCode,
                            expectedVersionName = update.versionName,
                        ),
                        appUpdateMessage = resultMessage,
                        message = resultMessage,
                    )
                }
            }.onFailure { error ->
                Log.w(TAG, "App update APK download failed for ${update.versionName}", error)
                _state.update {
                    val resultMessage = string(R.string.msg_app_update_download_failed)
                    it.copy(
                        appUpdateDownloadInProgress = false,
                        appUpdateMessage = resultMessage,
                        message = resultMessage,
                    )
                }
            }
        }
    }

    fun requireAppUpdateInstallPermission() {
        val resultMessage = string(R.string.msg_allow_app_update_install)
        _state.update { it.copy(message = resultMessage, appUpdateMessage = resultMessage) }
    }

    fun consumeAppUpdateInstallRequest() {
        _state.update { it.copy(appUpdateInstallRequest = null) }
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
                version == null -> string(R.string.msg_installer_returned_before_install, request.name)
                version.versionCode >= request.expectedVersionCode -> string(R.string.msg_updated_extension, request.name, version.versionName)
                else -> string(R.string.msg_extension_still_old, request.name, version.versionName)
            }
            _state.update {
                it.copy(
                    backupMissingSources = if ((version?.versionCode ?: -1) >= request.expectedVersionCode) {
                        it.backupMissingSources.filterNot { missing -> missing.packageName == request.packageName }
                    } else {
                        it.backupMissingSources
                    },
                    message = message,
                )
            }
            if (BuildConfig.DEBUG) {
                Log.i(
                    TAG,
                    "Installer returned for ${request.packageName}; installed=${version?.versionCode}, expected=${request.expectedVersionCode}",
                )
            }
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
        if (_state.value.libraryMode == LibraryMode.LOCAL) {
            loadCachedLibrary()
            _state.update { it.copy(message = string(R.string.msg_library_loaded)) }
            return
        }
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = string(R.string.msg_connect_anilist_sync_library)) }
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
                        viewerAvatarUrl = viewer.avatarUrl,
                        viewerBannerImageUrl = viewer.bannerImageUrl,
                        anilistMangaStats = viewer.mangaStats,
                        anilistScoreFormat = viewer.scoreFormat,
                        anilistTitleLanguage = viewer.titleLanguage,
                        anilistCustomLists = viewer.mangaCustomLists,
                        library = items.map { item -> item.media },
                        libraryItems = items,
                        librarySyncedAtEpochMillis = synced.syncedAtEpochMillis,
                        busy = false,
                        message = string(R.string.msg_library_synced),
                    )
                }
                loadRecentReadingProgress()
                processPendingAniListSync()
                runScheduledAniListBackupIfDue()
            }.onFailure { error ->
                Log.e(TAG, "AniList library sync failed", error)
                _state.update {
                    it.copy(busy = false, message = error.userMessage(localizedContext(), string(R.string.msg_library_sync_failed)))
                }
            }
        }
    }

    fun saveAniListBackup(uri: Uri) {
        val snapshot = _state.value
        val items = snapshot.libraryItems
        if (items.isEmpty()) {
            _state.update { it.copy(message = string(R.string.msg_sync_before_backup)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                if (snapshot.libraryMode == LibraryMode.LOCAL) {
                    backupDataSource.saveLocalLibraryBackup(uri = uri, snapshot = snapshot)
                } else {
                    backupDataSource.saveBackup(
                        uri = uri,
                        items = items,
                        viewerName = snapshot.viewerName,
                        scoreFormat = snapshot.anilistScoreFormat,
                    )
                }
            }.onSuccess {
                _state.update {
                    it.copy(
                        busy = false,
                        message = string(
                            if (snapshot.libraryMode == LibraryMode.LOCAL) {
                                R.string.msg_local_backup_saved
                            } else {
                                R.string.msg_backup_saved
                            },
                            quantityString(R.plurals.manga_count, items.size, items.size),
                        ),
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList backup failed", error)
                _state.update { it.copy(busy = false, message = error.userMessage(localizedContext(), string(R.string.msg_backup_failed))) }
            }
        }
    }

    fun restoreAniListBackup(uri: Uri) {
        val snapshot = _state.value
        val token = container.tokenStore.accessToken()
        if (snapshot.libraryMode == LibraryMode.ANILIST && token == null) {
            _state.update { it.copy(message = string(R.string.msg_connect_before_restore)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                if (snapshot.libraryMode == LibraryMode.LOCAL) {
                    backupDataSource.restoreLocalLibraryBackup(
                        uri = uri,
                        scoreFormat = snapshot.anilistScoreFormat,
                        knownCustomLists = snapshot.anilistCustomLists,
                    )
                } else {
                    backupDataSource.restoreBackup(
                        uri = uri,
                        accessToken = requireNotNull(token),
                        scoreFormat = snapshot.anilistScoreFormat,
                        knownCustomLists = snapshot.anilistCustomLists,
                    )
                }
            }.onSuccess { result ->
                container.settingsStore.saveAnilistCustomLists(result.customLists)
                loadCachedLibrary()
                _state.update {
                    it.copy(
                        anilistCustomLists = result.customLists,
                        busy = false,
                        message = buildList {
                            add(
                                string(
                                    R.string.msg_restored_manga,
                                    quantityString(R.plurals.manga_count, result.restored, result.restored),
                                ),
                            )
                            if (result.skipped > 0) add(string(R.string.msg_restore_skipped, result.skipped))
                        }.joinToString(" / "),
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList backup restore failed", error)
                _state.update { it.copy(busy = false, message = error.userMessage(localizedContext(), string(R.string.msg_restore_failed))) }
            }
        }
    }

    fun saveAppSettingsBackup(uri: Uri) {
        val snapshot = _state.value
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                appSettingsBackupDataSource.saveBackup(uri, snapshot)
            }.onSuccess { sourcePackageCount ->
                _state.update {
                    it.copy(
                        busy = false,
                        message = string(R.string.msg_app_settings_backup_saved, sourcePackageCount),
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "App settings backup failed", error)
                _state.update {
                    it.copy(
                        busy = false,
                        message = error.userMessage(localizedContext(), string(R.string.msg_app_settings_backup_failed)),
                    )
                }
            }
        }
    }

    fun restoreAppSettingsBackup(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                appSettingsBackupDataSource.restoreBackup(uri)
            }.onSuccess { result ->
                applyRestoredAppSettings(
                    missingSources = result.missingSources,
                    message = if (result.missingSources.isEmpty()) {
                        container.string(R.string.msg_app_settings_restored)
                    } else {
                        container.string(R.string.msg_app_settings_restored_with_missing_sources, result.missingSources.size)
                    },
                )
                refreshInstalledSources()
                refreshCacheStorageSummary()
                if (_state.value.extensionRepositoryUrl.isNotBlank()) {
                    refreshExtensionIndex(silent = true)
                }
                ScheduledBackupWork.update(container.application, container.settingsStore.backupSchedule())
            }.onFailure { error ->
                Log.e(TAG, "App settings restore failed", error)
                _state.update {
                    it.copy(
                        busy = false,
                        message = error.userMessage(localizedContext(), string(R.string.msg_app_settings_restore_failed)),
                    )
                }
            }
        }
    }

    private fun applyRestoredAppSettings(
        missingSources: List<BackupMissingSource>,
        message: String,
    ) {
        val store = container.settingsStore
        val sourceLanguages = store.sourceLanguages()
        val disabledSourceKeys = store.disabledSourceKeys()
        _state.update { current ->
            val visibleSources = current.allInstalledSources.preferredVisibleSources(sourceLanguages, disabledSourceKeys)
            current.copy(
                themeMode = store.themeMode(),
                appLanguage = store.appLanguage(),
                ignoreDisplayCutout = store.ignoreDisplayCutout(),
                showAppStatusBar = store.showAppStatusBar(),
                dockAlignment = store.dockAlignment(),
                libraryMode = store.libraryMode(),
                libraryViewMode = store.libraryViewMode(),
                libraryCoverColumns = store.libraryCoverColumns(),
                libraryShowWholeCovers = store.libraryShowWholeCovers(),
                browseViewMode = store.browseViewMode(),
                browseCoverColumns = store.browseCoverColumns(),
                browseShowWholeCovers = store.browseShowWholeCovers(),
                readerMode = store.readerMode(),
                readerPageGapLevel = store.readerPageGapLevel(),
                chapterListStartsAtFirst = store.chapterListStartsAtFirst(),
                keepNextTenDownloads = store.keepNextTenDownloads(),
                newChapterChecksEnabled = store.newChapterChecksEnabled(),
                lastNewChapterCheckAtEpochMillis = store.lastNewChapterCheckAtEpochMillis(),
                showNsfwContent = store.showNsfwContent(),
                anilistScoreFormat = store.anilistScoreFormat(),
                anilistTitleLanguage = store.anilistTitleLanguage(),
                anilistAutoSaveTrackingChanges = store.anilistAutoSaveTrackingChanges(),
                anilistAutoSyncReaderProgress = store.anilistAutoSyncReaderProgress(),
                anilistSyncManualReadProgress = store.anilistSyncManualReadProgress(),
                autoUpdateStatusFromReading = store.autoUpdateStatusFromReading(),
                anilistCustomLists = store.anilistCustomLists(),
                backupSchedule = store.backupSchedule(),
                backupContent = store.backupContent(),
                extensionRepositoryUrl = store.extensionRepositoryUrl(),
                sourceLanguages = sourceLanguages,
                disabledSourceKeys = disabledSourceKeys,
                installedSources = visibleSources,
                selectedSourceId = current.selectedSourceId
                    ?.takeIf { selected -> visibleSources.any { source -> source.id == selected } }
                    ?: visibleSources.firstOrNull()?.id,
                backupMissingSources = missingSources,
                busy = false,
                message = message,
            )
        }
        NewChapterCheckWork.update(container.application, store.newChapterChecksEnabled())
    }

    fun installBackupMissingSource(source: BackupMissingSource) {
        val entry = _state.value.availableExtensions.firstOrNull { it.packageName == source.packageName }
        if (entry == null) {
            _state.update {
                it.copy(message = string(R.string.msg_backup_missing_source_not_found, source.name))
            }
            return
        }
        installExtension(entry)
    }

    fun dismissBackupMissingSource(packageName: String) {
        _state.update { current ->
            current.copy(backupMissingSources = current.backupMissingSources.filterNot { it.packageName == packageName })
        }
    }

    fun setScheduledBackupFolder(uri: Uri) {
        if (!backupDataSource.persistBackupFolderPermission(uri)) {
            Log.w(TAG, "Could not persist backup folder permission")
            _state.update {
                it.copy(message = string(R.string.msg_backup_folder_permission_failed))
            }
            return
        }
        val uriString = uri.toString()
        container.settingsStore.saveBackupFolderUri(uriString)
        _state.update { it.copy(backupFolderUri = uriString, message = string(R.string.msg_backup_folder_selected)) }
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

    fun setBackupContent(content: BackupContent) {
        container.settingsStore.saveBackupContent(content)
        _state.update { it.copy(backupContent = content) }
        runScheduledAniListBackupIfDue(force = true, reportResult = _state.value.backupSchedule != BackupSchedule.OFF)
    }

    fun runScheduledAniListBackupNow() {
        if (_state.value.backupFolderUri == null) {
            _state.update { it.copy(message = string(R.string.msg_choose_backup_folder_first)) }
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
        val content = snapshot.backupContent
        if (schedule == BackupSchedule.OFF && requireSchedule) return
        val folderUri = snapshot.backupFolderUri?.let(Uri::parse)
        if (folderUri == null) {
            if (reportResult) {
                _state.update { it.copy(message = string(R.string.msg_choose_backup_folder_first)) }
            }
            return
        }
        if (content == BackupContent.LIBRARY && snapshot.libraryItems.isEmpty()) {
            if (reportResult) {
                _state.update { it.copy(message = string(R.string.msg_sync_before_backup)) }
            }
            return
        }
        val now = System.currentTimeMillis()
        if (!force && !schedule.isDue(lastRunAt = snapshot.lastScheduledBackupAtEpochMillis, now = now)) return
        if (scheduledBackupJob?.isActive == true) {
            if (reportResult) {
                _state.update { it.copy(message = string(R.string.msg_backup_already_running)) }
            }
            return
        }

        scheduledBackupJob = viewModelScope.launch {
            if (reportResult) {
                _state.update { it.copy(busy = true, message = null) }
            }
            runCatching {
                ScheduledBackupRunResult(
                    libraryItems = if (content.includesLibrary()) {
                        snapshot.libraryItems
                            .takeIf { it.isNotEmpty() }
                            ?.let {
                                if (snapshot.libraryMode == LibraryMode.LOCAL) {
                                    backupDataSource.writeScheduledLocalLibraryBackup(folderUri = folderUri, snapshot = snapshot)
                                } else {
                                    backupDataSource.writeScheduledBackup(folderUri = folderUri, snapshot = snapshot)
                                }
                            }
                    } else {
                        null
                    },
                    sourcePackages = if (content.includesSettings()) {
                        appSettingsBackupDataSource.writeScheduledBackup(folderUri = folderUri, snapshot = snapshot)
                    } else {
                        null
                    },
                )
            }.onSuccess { result ->
                container.settingsStore.saveLastScheduledBackupAtEpochMillis(now)
                _state.update {
                    it.copy(
                        lastScheduledBackupAtEpochMillis = now,
                        busy = if (reportResult) false else it.busy,
                        message = if (reportResult) {
                            scheduledBackupSavedMessage(result)
                        } else {
                            it.message
                        },
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Scheduled backup failed", error)
                _state.update {
                    it.copy(
                        busy = if (reportResult) false else it.busy,
                        message = if (reportResult) {
                            error.userMessage(localizedContext(), string(R.string.msg_scheduled_backup_failed))
                        } else {
                            it.message
                        },
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
        status: MediaStatus? = null,
    ) {
        val snapshot = _state.value
        var nextProgress = chapterProgress.takeIf {
            it > 0 &&
                snapshot.anilistAutoSyncReaderProgress &&
                (!triggeredByManualRead || snapshot.anilistSyncManualReadProgress)
        }
        var nextStatus = status
        if (nextProgress == null && nextStatus == null) return
        val trackedProgress = snapshot.trackedProgressFor(media.id)
        val trackedStatus = snapshot.trackedListEntryFor(media.id)?.status
        if (nextProgress != null && nextProgress <= trackedProgress) nextProgress = null
        if (nextStatus != null && nextStatus == trackedStatus) nextStatus = null
        if (nextProgress == null && nextStatus == null) return

        if (snapshot.libraryMode == LibraryMode.LOCAL) {
            aniListDataSource.saveLocalProgressFromChapter(
                media = media,
                chapterProgress = nextProgress ?: 0,
                status = nextStatus,
            )?.let { synced ->
                _state.update {
                    it.withSyncedListEntry(
                        media = synced.media,
                        entry = synced.entry,
                        updateTrackingForm = synced.updateTrackingForm,
                    )
                }
                loadRecentReadingProgress()
            }
            return
        }

        aniListDataSource.syncProgressFromChapter(
            media = media,
            chapterProgress = nextProgress ?: 0,
            status = nextStatus,
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

    private fun TankobunUiState.trackedListEntryFor(mediaId: Int): AnilistListEntry? =
        selectedListEntry?.takeIf { selectedMedia?.id == mediaId || it.mediaId == mediaId }
            ?: libraryItems.firstOrNull { it.media.id == mediaId }?.entry

    private fun TankobunUiState.trackedProgressFor(mediaId: Int): Int {
        val savedProgress = trackedListEntryFor(mediaId)?.progress ?: 0
        if (selectedMedia?.id != mediaId) return savedProgress
        return maxOf(savedProgress, trackingProgress.toIntOrNull() ?: savedProgress)
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
        browseBackStack = emptyList()
        committedBrowseCriteria = BrowseCriteria()
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

    fun navigateBrowseBack(): Boolean {
        val currentCriteria = _state.value.toBrowseCriteria()
        if (currentCriteria != committedBrowseCriteria) {
            restoreBrowseCriteria(committedBrowseCriteria)
            return true
        }

        val previousCriteria = browseBackStack.lastOrNull()
        if (previousCriteria != null) {
            browseBackStack = browseBackStack.dropLast(1)
            restoreBrowseCriteria(previousCriteria)
            return true
        }

        if (currentCriteria.hasQueryOrFilters || _state.value.browseSearched) {
            restoreBrowseCriteria(BrowseCriteria())
            return true
        }

        return false
    }

    private fun recordBrowseCriteria(criteria: BrowseCriteria) {
        if (criteria == committedBrowseCriteria) return
        browseBackStack = (browseBackStack + committedBrowseCriteria)
            .distinctConsecutive()
            .takeLast(BROWSE_BACK_STACK_LIMIT)
        committedBrowseCriteria = criteria
    }

    private fun restoreBrowseCriteria(criteria: BrowseCriteria) {
        committedBrowseCriteria = criteria
        _state.update { it.withBrowseCriteria(criteria) }
        if (criteria.hasQueryOrFilters) {
            searchAniList(recordHistory = false)
        } else {
            loadBrowseLanding()
        }
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
        if (!force && browseLandingJob?.isActive == true) return
        browseLandingJob = viewModelScope.launch {
            val snapshot = _state.value
            val includeAdult = snapshot.showNsfwContent
            val trendingKey = snapshot.browseLandingCacheKey(BROWSE_TRENDING_CACHE_KEY)
            val popularKey = snapshot.browseLandingCacheKey(BROWSE_POPULAR_CACHE_KEY)
            val manhwaKey = snapshot.browseLandingCacheKey(BROWSE_MANHWA_CACHE_KEY)
            val topMangaKey = snapshot.browseLandingCacheKey(BROWSE_TOP_MANGA_CACHE_KEY)
            val cachedLanding = BrowseLandingData(
                trending = browseDataSource.cachedBrowseMedia(trendingKey),
                popular = browseDataSource.cachedBrowseMedia(popularKey),
                popularManhwa = browseDataSource.cachedBrowseMedia(manhwaKey),
                topManga = browseDataSource.cachedBrowseMedia(topMangaKey).take(BROWSE_LANDING_SECTION_SIZE),
            )
            val hasCachedLanding = cachedLanding.hasContent()
            val hasVisibleLanding = BrowseLandingData(
                trending = snapshot.browseTrending,
                popular = snapshot.browsePopular,
                popularManhwa = snapshot.browsePopularManhwa,
                topManga = snapshot.browseTopManga,
            ).hasContent()
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
            } else if (!hasVisibleLanding) {
                _state.update { it.copy(busy = true, message = null) }
            }

            val trendingFresh = !force && browseDataSource.browseCacheFresh(trendingKey, cachePolicy.browseLandingTtlMillis)
            val popularFresh = !force && browseDataSource.browseCacheFresh(popularKey, cachePolicy.anilistSearchTtlMillis)
            val manhwaFresh = !force && browseDataSource.browseCacheFresh(manhwaKey, cachePolicy.anilistSearchTtlMillis)
            val topMangaFresh = !force && browseDataSource.browseCacheFresh(topMangaKey, cachePolicy.anilistSearchTtlMillis)
            if (trendingFresh && popularFresh && manhwaFresh && topMangaFresh) {
                _state.update { it.copy(browseLandingLoaded = true, busy = false) }
                return@launch
            }
            runCatching {
                val accessToken = container.tokenStore.accessToken()
                val trending = if (trendingFresh && cachedLanding.trending.isNotEmpty()) {
                    cachedLanding.trending
                } else {
                    browseDataSource.refreshAnilistBrowseMedia(trendingKey) {
                        container.anilistRepository.browseManga(
                            sort = BROWSE_TRENDING_SORT,
                            perPage = BROWSE_LANDING_SECTION_SIZE,
                            accessToken = accessToken,
                            includeAdult = includeAdult,
                        )
                    }
                }
                val popular = if (popularFresh && cachedLanding.popular.isNotEmpty()) {
                    cachedLanding.popular
                } else {
                    browseDataSource.refreshAnilistBrowseMedia(popularKey) {
                        container.anilistRepository.browseManga(
                            sort = "POPULARITY_DESC",
                            perPage = BROWSE_LANDING_SECTION_SIZE,
                            accessToken = accessToken,
                            includeAdult = includeAdult,
                        )
                    }
                }
                val popularManhwa = if (manhwaFresh && cachedLanding.popularManhwa.isNotEmpty()) {
                    cachedLanding.popularManhwa
                } else {
                    browseDataSource.refreshAnilistBrowseMedia(manhwaKey) {
                        container.anilistRepository.browseManga(
                            countryOfOrigin = "KR",
                            sort = "POPULARITY_DESC",
                            perPage = BROWSE_LANDING_SECTION_SIZE,
                            accessToken = accessToken,
                            includeAdult = includeAdult,
                        )
                    }
                }
                val topManga = if (topMangaFresh && cachedLanding.topManga.isNotEmpty()) {
                    cachedLanding.topManga
                } else {
                    browseDataSource.refreshAnilistBrowseMedia(topMangaKey) {
                        container.anilistRepository.browseManga(
                            sort = "SCORE_DESC",
                            perPage = BROWSE_LANDING_SECTION_SIZE,
                            accessToken = accessToken,
                            includeAdult = includeAdult,
                        )
                    }.take(BROWSE_LANDING_SECTION_SIZE)
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
                    it.copy(
                        busy = false,
                        message = if (hasCachedLanding) {
                            it.message
                        } else if (hasVisibleLanding) {
                            it.message
                        } else {
                            error.userMessage(localizedContext(), string(R.string.msg_browse_failed))
                        },
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

    fun searchAniList(
        forceRefresh: Boolean = false,
        recordHistory: Boolean = true,
    ) {
        val snapshot = _state.value
        val query = snapshot.searchQuery.trim()
        val criteria = snapshot.toBrowseCriteria()
        if (!snapshot.hasBrowseQueryOrFilters()) {
            if (recordHistory) {
                browseBackStack = emptyList()
                committedBrowseCriteria = BrowseCriteria()
            }
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
        if (recordHistory) {
            recordBrowseCriteria(criteria)
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
                val ttlMillis = if (snapshot.usesTrendingResultsCacheTtl()) {
                    cachePolicy.browseLandingTtlMillis
                } else {
                    cachePolicy.anilistSearchTtlMillis
                }
                browseDataSource.cachedAnilistBrowseMediaPage(
                    cacheKey = cacheKey,
                    ttlMillis = ttlMillis,
                    forceRefresh = forceRefresh,
                ) {
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
                            message = error.userMessage(localizedContext(), string(R.string.msg_search_failed)),
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
                            message = error.userMessage(localizedContext(), string(R.string.msg_could_not_load_more_manga)),
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
        _state.update { it.withSelectedMedia(displayMedia, existingEntry) }
        loadAnilistDetails(media.id)
        loadCachedSourceState(media.id)
    }

    fun selectSource(sourceId: Long) {
        _state.update { it.withSelectedSource(sourceId) }
    }

    fun clearSelectedMedia() {
        _state.update { it.withoutSelectedMedia() }
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

    fun toggleChapterListOrder() {
        val nextValue = !_state.value.chapterListStartsAtFirst
        container.settingsStore.saveChapterListStartsAtFirst(nextValue)
        _state.update { it.copy(chapterListStartsAtFirst = nextValue) }
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

    fun setAutoUpdateStatusFromReading(enabled: Boolean) {
        container.settingsStore.saveAutoUpdateStatusFromReading(enabled)
        _state.update { it.copy(autoUpdateStatusFromReading = enabled) }
    }

    fun setShowNsfwContent(enabled: Boolean) {
        container.settingsStore.saveShowNsfwContent(enabled)
        _state.update {
            it.copy(
                showNsfwContent = enabled,
                browseLandingLoaded = false,
                searchResults = emptyList(),
                browseSearched = false,
                browseResultsPage = 0,
                browseResultsHasMore = false,
                browseResultsLoadingMore = false,
                browseTags = if (enabled) {
                    it.browseTags
                } else {
                    val adultTagNames = it.browseAvailableTags
                        .filter { tag -> tag.isAdult }
                        .map { tag -> tag.name }
                        .toSet()
                    it.browseTags - adultTagNames
                },
            )
        }
    }

    private fun scheduledBackupSavedMessage(result: ScheduledBackupRunResult): String {
        val libraryPart = result.libraryItems?.let { count ->
            string(R.string.backup_content_library_with_count, quantityString(R.plurals.manga_count, count, count))
        }
        val settingsPart = result.sourcePackages?.let { count ->
            string(R.string.backup_content_settings_with_count, count)
        }
        return string(
            R.string.msg_scheduled_backup_saved,
            listOfNotNull(libraryPart, settingsPart).joinToString(" / "),
        )
    }

    fun setAnilistTitleLanguage(language: AnilistTitleLanguage) {
        if (_state.value.anilistTitleLanguage == language) return
        if (_state.value.libraryMode == LibraryMode.LOCAL) {
            container.settingsStore.saveAnilistTitleLanguage(language)
            _state.update {
                it.withAniListTitleLanguage(language).copy(message = string(R.string.msg_title_language_saved))
            }
            return
        }
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = string(R.string.msg_connect_before_account_preferences)) }
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
                        viewerAvatarUrl = viewer.avatarUrl,
                        viewerBannerImageUrl = viewer.bannerImageUrl,
                        anilistMangaStats = viewer.mangaStats,
                        anilistScoreFormat = viewer.scoreFormat,
                        anilistCustomLists = viewer.mangaCustomLists,
                        busy = false,
                        message = string(R.string.msg_title_language_saved),
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList title language update failed", error)
                _state.update {
                    it.copy(
                        busy = false,
                        message = error.userMessage(localizedContext(), string(R.string.msg_title_language_update_failed)),
                    )
                }
            }
        }
    }

    fun setAnilistScoreFormat(format: AnilistScoreFormat) {
        if (_state.value.anilistScoreFormat == format) return
        if (_state.value.libraryMode == LibraryMode.LOCAL) {
            container.settingsStore.saveAnilistScoreFormat(format)
            _state.update {
                it.copy(
                    anilistScoreFormat = format,
                    trackingScore = it.selectedListEntry?.score.formatTrackingScore(format),
                    message = string(R.string.msg_rating_format_saved),
                )
            }
            return
        }
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = string(R.string.msg_connect_before_account_preferences)) }
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
                        viewerAvatarUrl = viewer.avatarUrl,
                        viewerBannerImageUrl = viewer.bannerImageUrl,
                        anilistMangaStats = viewer.mangaStats,
                        anilistScoreFormat = viewer.scoreFormat,
                        anilistTitleLanguage = viewer.titleLanguage,
                        anilistCustomLists = viewer.mangaCustomLists,
                        trackingScore = it.selectedListEntry?.score.formatTrackingScore(viewer.scoreFormat),
                        busy = false,
                        message = string(R.string.msg_rating_format_saved),
                    )
                }
                refreshLibrary()
            }.onFailure { error ->
                Log.e(TAG, "AniList rating format update failed", error)
                _state.update {
                    it.copy(
                        busy = false,
                        message = error.userMessage(localizedContext(), string(R.string.msg_rating_format_update_failed)),
                    )
                }
            }
        }
    }

    fun createAnilistCustomList(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        val currentLists = _state.value.anilistCustomLists.normalizedCustomLists()
        if (currentLists.any { it.equals(normalizedName, ignoreCase = true) }) {
            _state.update { it.copy(message = string(R.string.msg_custom_list_exists)) }
            return
        }
        val nextLists = (currentLists + normalizedName).normalizedCustomLists()
        if (_state.value.libraryMode == LibraryMode.LOCAL) {
            val savedLists = aniListDataSource.saveLocalCustomLists(nextLists)
            _state.update {
                it.copy(
                    anilistCustomLists = savedLists,
                    message = string(R.string.msg_custom_list_created),
                )
            }
            return
        }
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = string(R.string.msg_connect_before_custom_lists)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                aniListDataSource.updateCustomLists(token, nextLists)
            }.onSuccess { savedLists ->
                _state.update {
                    it.copy(
                        anilistCustomLists = savedLists,
                        busy = false,
                        message = string(R.string.msg_custom_list_created),
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList custom list create failed", error)
                _state.update {
                    it.copy(
                        busy = false,
                        message = error.userMessage(localizedContext(), string(R.string.msg_custom_list_create_failed)),
                    )
                }
            }
        }
    }

    fun renameAnilistCustomList(oldName: String, newName: String) {
        val normalizedOldName = oldName.trim()
        val normalizedNewName = newName.trim()
        if (normalizedOldName.isBlank() || normalizedNewName.isBlank()) return
        if (normalizedOldName.equals(normalizedNewName, ignoreCase = true)) return
        val snapshot = _state.value
        val currentLists = snapshot.anilistCustomLists.normalizedCustomLists()
        if (currentLists.any { it.equals(normalizedNewName, ignoreCase = true) && !it.equals(normalizedOldName, ignoreCase = true) }) {
            _state.update { it.copy(message = string(R.string.msg_custom_list_name_exists)) }
            return
        }
        val nextLists = currentLists
            .map { if (it.equals(normalizedOldName, ignoreCase = true)) normalizedNewName else it }
            .normalizedCustomLists()
        if (snapshot.libraryMode == LibraryMode.LOCAL) {
            viewModelScope.launch {
                _state.update { it.copy(busy = true, message = null) }
                runCatching {
                    aniListDataSource.renameLocalCustomListEntries(
                        items = snapshot.libraryItems,
                        oldName = normalizedOldName,
                        newName = normalizedNewName,
                        nextCustomLists = nextLists,
                    )
                }.onSuccess { result ->
                    _state.update {
                        it.withRenamedAniListCustomList(
                            customLists = result.customLists,
                            updatedEntries = result.updatedEntries,
                            oldName = normalizedOldName,
                            newName = normalizedNewName,
                            successMessage = string(R.string.msg_custom_list_renamed),
                        )
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Local custom list rename failed", error)
                    _state.update {
                        it.copy(
                            busy = false,
                            message = error.userMessage(localizedContext(), string(R.string.msg_custom_list_rename_failed)),
                        )
                    }
                }
            }
            return
        }
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = string(R.string.msg_connect_before_custom_lists)) }
            return
        }
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
                        successMessage = string(R.string.msg_custom_list_renamed),
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList custom list rename failed", error)
                _state.update {
                    it.copy(
                        busy = false,
                        message = error.userMessage(localizedContext(), string(R.string.msg_custom_list_rename_failed)),
                    )
                }
            }
        }
    }

    fun deleteAnilistCustomList(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        val snapshot = _state.value
        val currentLists = snapshot.anilistCustomLists.normalizedCustomLists()
        val nextLists = currentLists
            .filterNot { it.equals(normalizedName, ignoreCase = true) }
            .normalizedCustomLists()
        if (snapshot.libraryMode == LibraryMode.LOCAL) {
            viewModelScope.launch {
                _state.update { it.copy(busy = true, message = null) }
                runCatching {
                    aniListDataSource.deleteLocalCustomListEntries(
                        items = snapshot.libraryItems,
                        listName = normalizedName,
                        nextCustomLists = nextLists,
                    )
                }.onSuccess { result ->
                    _state.update {
                        it.withDeletedAniListCustomList(
                            customLists = result.customLists,
                            updatedEntries = result.updatedEntries,
                            name = normalizedName,
                            successMessage = string(R.string.msg_custom_list_deleted),
                        )
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Local custom list delete failed", error)
                    _state.update {
                        it.copy(
                            busy = false,
                            message = error.userMessage(localizedContext(), string(R.string.msg_custom_list_delete_failed)),
                        )
                    }
                }
            }
            return
        }
        val token = container.tokenStore.accessToken()
        if (token == null) {
            _state.update { it.copy(message = string(R.string.msg_connect_before_custom_lists)) }
            return
        }
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
                        successMessage = string(R.string.msg_custom_list_deleted),
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList custom list delete failed", error)
                _state.update {
                    it.copy(
                        busy = false,
                        message = error.userMessage(localizedContext(), string(R.string.msg_custom_list_delete_failed)),
                    )
                }
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
        val snapshot = _state.value
        val customLists = snapshot.trackingCustomLists.normalizedCustomLists()
        if (customLists.isEmpty() && snapshot.selectedListEntry == null) return
        val token = container.tokenStore.accessToken()
        if (snapshot.libraryMode == LibraryMode.ANILIST && token == null) return

        viewModelScope.launch {
            runCatching {
                val knownCustomLists = snapshot.anilistCustomLists.normalizedCustomLists()
                if (snapshot.libraryMode == LibraryMode.LOCAL) {
                    aniListDataSource.saveLocalTracking(
                        media = media,
                        status = if (snapshot.selectedListEntry == null) snapshot.trackingStatus else snapshot.selectedListEntry.status,
                        progress = snapshot.selectedListEntry?.progress,
                        score = snapshot.selectedListEntry?.score,
                        notes = snapshot.selectedListEntry?.notes,
                        private = snapshot.selectedListEntry?.private ?: false,
                        customLists = customLists,
                        knownCustomLists = knownCustomLists,
                    )
                } else {
                    aniListDataSource.saveTracking(
                        token = requireNotNull(token),
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
                }
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
                    it.copy(message = error.userMessage(localizedContext(), string(R.string.msg_custom_list_save_failed)))
                }
            }
        }
    }

    private fun scheduleTrackingAutoSave() {
        val snapshot = _state.value
        val canAutoSave = snapshot.libraryMode == LibraryMode.LOCAL || snapshot.loggedIn
        if (!snapshot.anilistAutoSaveTrackingChanges || !canAutoSave || snapshot.selectedMedia == null) return
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
        val snapshot = _state.value
        val token = container.tokenStore.accessToken()
        if (snapshot.libraryMode == LibraryMode.ANILIST && token == null) {
            if (!autoSave) {
                _state.update { it.copy(message = string(R.string.msg_connect_anilist_track_manga)) }
            }
            return
        }

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
                if (snapshot.libraryMode == LibraryMode.LOCAL) {
                    aniListDataSource.saveLocalTracking(
                        media = media,
                        status = snapshot.trackingStatus,
                        progress = progress,
                        score = score,
                        notes = notes,
                        private = snapshot.trackingPrivate,
                        customLists = customLists,
                        knownCustomLists = knownCustomLists,
                    )
                } else {
                    aniListDataSource.saveTracking(
                        token = requireNotNull(token),
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
                }
            }.onSuccess { result ->
                _state.update {
                    it.withTrackingSaveResult(
                        media = media,
                        entry = result.entry,
                        knownCustomLists = result.knownCustomLists,
                        autoSave = autoSave,
                        successMessage = if (snapshot.libraryMode == LibraryMode.LOCAL) {
                            string(R.string.msg_tracking_saved_local)
                        } else {
                            string(R.string.msg_tracking_saved)
                        },
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList tracking save failed for ${media.id}", error)
                _state.update {
                    it.withTrackingSaveFailure(
                        mediaId = media.id,
                        autoSave = autoSave,
                        message = error.userMessage(
                            localizedContext(),
                            if (autoSave) {
                                string(R.string.msg_tracking_auto_save_failed)
                            } else {
                                string(R.string.msg_tracking_save_failed)
                            },
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
                            message = error.userMessage(localizedContext(), string(R.string.msg_recommendations_failed)),
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
        _state.update { it.withSourcePickerOpened(media) }
        if (sources.isEmpty()) {
            _state.update { it.withSourcePickerNoSources(localizedContext()) }
            return
        }
        if (!_state.value.sourcePickerLoading) {
            findSourceMatches(forceRefresh = true)
        }
    }

    fun closeSourcePicker() {
        cancelSourcePickerJob()
        _state.update { it.withSourcePickerClosed() }
    }

    fun updateSourcePickerSearchTitle(title: String) {
        _state.update { it.withSourcePickerSearchTitle(title) }
    }

    fun findSourceMatchesWithEditedTitle() {
        val title = _state.value.sourcePickerSearchTitle.trim()
        if (title.length < 2) {
            _state.update { it.withSourcePickerEditedTitleTooShort(localizedContext()) }
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
            _state.update { it.withSourcePickerNoSources(localizedContext()) }
            return
        }
        bindSource(source)
    }

    fun bindSource(source: SourceDescriptor) {
        val media = _state.value.selectedMedia ?: return
        val requestId = beginSourcePickerJob()
        sourcePickerJob = viewModelScope.launch {
            _state.update { it.withSourcePickerSourceSearchStarted(localizedContext(), source) }
            try {
                val now = System.currentTimeMillis()
                val match = sourceDataSource.readableSourceMatch(media, source, now)
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
                sourceDataSource.saveSourceBinding(match)
                _state.update { it.withSourcePickerSourceSelected(localizedContext(), match, addToMatches = true) }
                loadChapters(match.source, match.manga)
            } catch (error: Throwable) {
                if (error is CancellationException) return@launch
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
                Log.w(TAG, "Selected source binding failed for ${source.name}", error)
                _state.update { it.withSourcePickerFailure(localizedContext(), source.name, error) }
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
            _state.update { it.withSourcePickerNoSources(localizedContext()) }
            return
        }
        val editedTitle = titleOverride?.trim()?.takeIf { it.length >= 2 }
        val requestId = beginSourcePickerJob()
        sourcePickerJob = viewModelScope.launch {
            _state.update { it.withSourcePickerSearchStarted(localizedContext(), editedTitle) }
            try {
                val now = System.currentTimeMillis()
                val verified = if (editedTitle == null) {
                    sourceDataSource.cachedVerifiedMatches(media.id, sources, now)
                        .takeIf { !forceRefresh && it.matches.isNotEmpty() }
                        ?: sourceDataSource.searchVerifiedMatches(
                            media = media,
                            sources = sources,
                            now = now,
                            diagnosticSearchTimedOut = string(R.string.source_picker_search_timed_out),
                            diagnosticNoSearchResults = string(R.string.source_picker_no_search_results),
                            diagnosticNoConfidentTitleMatch = string(R.string.source_picker_no_confident_match),
                            diagnosticNoReadableChapters = string(R.string.source_picker_no_readable_chapters),
                            diagnosticDetail = { error -> sourcePickerDiagnosticDetail(localizedContext(), error) },
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
                        diagnosticSearchTimedOut = string(R.string.source_picker_search_timed_out),
                        diagnosticNoSearchResults = string(R.string.source_picker_no_search_results),
                        diagnosticNoConfidentTitleMatch = string(R.string.source_picker_no_confident_match),
                        diagnosticNoReadableChapters = string(R.string.source_picker_no_readable_chapters),
                        diagnosticDetail = { error -> sourcePickerDiagnosticDetail(localizedContext(), error) },
                        onUpdate = { update -> publishSourcePickerUpdate(requestId, media.id, update) },
                    )
                }
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
                _state.update { it.withSourcePickerSearchCompleted(localizedContext(), verified, editedTitle) }
            } catch (error: Throwable) {
                if (error is CancellationException) return@launch
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
                _state.update { it.withSourcePickerFailure(localizedContext(), "source search", error) }
            } finally {
                if (sourcePickerRequestId == requestId) {
                    sourcePickerJob = null
                }
            }
        }
    }

    private fun sourcePickerSources(): List<SourceDescriptor> {
        return _state.value.sourcePickerSources()
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
                it.withSourcePickerMatchPublished(localizedContext(), match, chapterCount)
            }
        }
    }

    private fun publishSourcePickerDiagnostic(requestId: Long, mediaId: Int, source: SourceDescriptor, detail: String) {
        _state.update {
            if (!isActiveSourcePickerRequest(requestId, mediaId)) {
                it
            } else {
                it.withSourcePickerDiagnostic(source, detail)
            }
        }
    }

    fun bindSourceMatch(match: SourceSearchResult) {
        val media = _state.value.selectedMedia ?: return
        val requestId = beginSourcePickerJob()
        sourcePickerJob = viewModelScope.launch {
            _state.update { it.withSourcePickerMatchOpening(localizedContext(), match) }
            try {
                val resolved = sourceDataSource.resolveMangaDetails(match)
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
                sourceDataSource.saveSourceBinding(resolved)
                _state.update { it.withSourcePickerSourceSelected(localizedContext(), resolved, addToMatches = false) }
                loadChapters(resolved.source, resolved.manga)
            } catch (error: Throwable) {
                if (error is CancellationException) return@launch
                if (!isActiveSourcePickerRequest(requestId, media.id)) return@launch
                Log.w(TAG, "Source binding failed for ${match.source.name}/${match.manga.title}", error)
                _state.update { it.withSourcePickerFailure(localizedContext(), match.source.name, error) }
            } finally {
                if (sourcePickerRequestId == requestId) {
                    sourcePickerJob = null
                }
            }
        }
    }

    fun loadChaptersForCurrentMatch() {
        val selection = _state.value.selectedSourceChapterSelection()
        if (selection == null) {
            _state.update { it.withSourceChapterSelectionMissing(localizedContext()) }
            return
        }
        loadChapters(selection.source, selection.manga)
    }

    private fun loadChapters(source: SourceDescriptor, manga: com.tankobun.core.model.SourceManga) {
        viewModelScope.launch {
            _state.update { it.withSourceChaptersLoading() }
            runCatching {
                val now = System.currentTimeMillis()
                sourceDataSource.loadChapters(
                    source = source,
                    manga = manga,
                    mediaId = _state.value.selectedMedia?.id,
                    now = now,
                )
            }.onSuccess { (detailedManga, chapters, chapterProgress) ->
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Chapter load ${source.name}/${detailedManga.title}: chapters=${chapters.size}")
                }
                _state.update { it.withSourceChaptersLoaded(localizedContext(), source, detailedManga, chapters, chapterProgress) }
                if (_state.value.keepNextTenDownloads) {
                    val result = ensureNextTenDownloads()
                    if (result.changed > 0) {
                        _state.update { it.copy(message = string(R.string.msg_queued_next_chapters, result.changed)) }
                    }
                }
            }.onFailure { error ->
                Log.w(TAG, "Chapter load failed for ${source.name}/${manga.title}", error)
                _state.update { it.withSourceChaptersLoadFailed(localizedContext(), error) }
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
            _state.update { it.withReaderLoading(chapter) }
            runCatching {
                readerDataSource.loadPagesForChapter(media.id, chapter, source)
            }.onSuccess { pages ->
                if (pages.isEmpty() && source == null) {
                    val readerError = ReaderLoadError(
                        title = string(R.string.msg_source_not_installed_title),
                        message = string(R.string.msg_source_not_installed_reader),
                    )
                    _state.update { it.withReaderLoadError(chapter, readerError) }
                    return@onSuccess
                }
                if (pages.isEmpty()) {
                    val readerError = ReaderLoadError(
                        title = string(R.string.msg_no_pages_found_title),
                        message = string(R.string.msg_no_pages_found_reader),
                    )
                    _state.update { it.withReaderLoadError(chapter, readerError) }
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
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Page load ${source?.name ?: "downloaded"}/${chapter.name}: pages=${pages.size}")
                }
                var readerStillOpen = false
                _state.update {
                    if (it.activeChapter?.url == chapter.url) {
                        readerStillOpen = true
                        recordReaderPosition(chapter.url, startPageIndex, startPageScrollOffset)
                        it.withReaderPagesLoaded(
                            chapter = chapter,
                            pages = pages,
                            pageIndex = startPageIndex,
                            pageScrollOffset = startPageScrollOffset,
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
                val readerError = readerLoadErrorFor(localizedContext(), error, source)
                _state.update { it.withReaderLoadError(chapter, readerError) }
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
            _state.update { it.withAdjacentReaderSegments(chapter, previousSegment, nextSegment) }
        }
    }

    fun openRecentProgress(item: RecentReadingProgress) {
        val existingEntry = _state.value.libraryItems.firstOrNull { libraryItem ->
            libraryItem.media.id == item.media.id
        }?.entry
        _state.update { it.withRecentProgressOpened(item, existingEntry) }
        loadAnilistDetails(item.media.id)
        loadCachedSourceState(item.media.id)
        val chapter = item.chapter
        if (chapter == null) {
            _state.update { it.copy(message = string(R.string.msg_chapter_cache_missing, item.media.title.userPreferred)) }
        } else {
            openChapter(chapter)
        }
    }

    fun closeReader() {
        saveReaderProgress()
        readerAdjacentLoadJob?.cancel()
        readerAdjacentLoadJob = null
        cancelReaderPageCacheJobs()
        _state.update { it.withReaderClosed() }
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
        _state.update { it.withReaderPagePosition(nextIndex, nextOffset) }
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
            if (nextIndex >= pages.lastIndex) {
                saveReaderProgressFor(
                    media = media,
                    chapter = chapter,
                    pages = pages,
                    pageIndex = nextIndex,
                    pageScrollOffset = nextOffset,
                )
            } else {
                maybeSyncAutomaticReaderStatusForPosition(
                    media = media,
                    chapter = chapter,
                    pages = pages,
                    pageIndex = nextIndex,
                )
            }
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
            else -> openWebtoonAdjacentChapter(chapterUrl)
        }
    }

    private fun openWebtoonAdjacentChapter(chapterUrl: String) {
        val snapshot = _state.value
        val activeChapter = snapshot.activeChapter ?: return
        when (chapterUrl) {
            snapshot.sourceChapters.previousInReadingOrderBefore(activeChapter)?.url -> openPreviousChapter()
            snapshot.sourceChapters.nextInReadingOrderAfter(activeChapter)?.url -> openNextChapter()
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

        _state.update { it.withReaderPagePosition(nextIndex, nextOffset) }
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
            if (nextIndex >= pages.lastIndex) {
                saveReaderProgressFor(
                    media = media,
                    chapter = chapter,
                    pages = pages,
                    pageIndex = nextIndex,
                    pageScrollOffset = nextOffset,
                )
            } else {
                maybeSyncAutomaticReaderStatusForPosition(
                    media = media,
                    chapter = chapter,
                    pages = pages,
                    pageIndex = nextIndex,
                )
            }
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
            it.withActivatedReaderSegment(
                targetSegment = targetSegment,
                oldActiveSegment = oldActiveSegment,
                pageIndex = nextIndex,
                pageScrollOffset = pageScrollOffset,
                direction = direction,
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
            _state.update { it.withReaderPagePosition(snapshot.readerPages.lastIndex, 0) }
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
            _state.update { it.withReaderPagePosition(0, 0) }
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
                val trackedProgress = _state.value.trackedProgressFor(media.id)
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
                        message = if (read) {
                            string(R.string.msg_marked_read, chapter.name)
                        } else {
                            string(R.string.msg_marked_unread, chapter.name)
                        },
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
                result.queued > 0 -> string(R.string.msg_queued_chapter, chapter.name)
                result.resumed > 0 -> string(R.string.msg_resumed_chapter, chapter.name)
                result.retried > 0 -> string(R.string.msg_retrying_chapter, chapter.name)
                else -> {
                    val existing = downloadDataSource.latestForChapter(media.id, chapter.url)
                    when (existing?.state) {
                        DownloadState.COMPLETE -> string(R.string.msg_chapter_already_downloaded, chapter.name)
                        DownloadState.QUEUED,
                        DownloadState.RUNNING -> string(R.string.msg_chapter_already_queued, chapter.name)
                        else -> string(R.string.msg_chapter_already_in_downloads, chapter.name)
                    }
                }
            }
            _state.update { it.copy(message = message) }
        }
    }

    fun downloadAllChapters() {
        val chapters = _state.value.sourceChapters
        enqueueVisibleChapterDownloads(chapters, R.string.download_label_all_chapters)
    }

    fun downloadUnreadChapters() {
        val snapshot = _state.value
        val chapters = snapshot.sourceChapters.filterNot { snapshot.chapterProgress[it.url]?.completed == true }
        enqueueVisibleChapterDownloads(chapters, R.string.download_label_unread_chapters)
    }

    fun downloadNextTenChapters() {
        val chapters = nextTenDownloadCandidates(_state.value)
        enqueueVisibleChapterDownloads(chapters, R.string.download_label_next_10_chapters)
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
                            string(R.string.msg_keep_next_10_ready, result.changed)
                        } else {
                            string(R.string.msg_next_10_ready)
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
        enqueueVisibleChapterDownloads(chapters, R.string.download_label_selected_chapters) {
            it.copy(
                selectingDownloadChapters = false,
                selectedDownloadChapterUrls = emptySet(),
            )
        }
    }

    private fun enqueueVisibleChapterDownloads(
        chapters: List<SourceChapter>,
        @StringRes labelRes: Int,
        stateAfterMessage: (TankobunUiState) -> TankobunUiState = { it },
    ) {
        viewModelScope.launch {
            val media = _state.value.selectedMedia ?: return@launch
            val distinctChapters = chapters.distinctBy { "${it.sourceId}:${it.url}" }
            val label = string(labelRes)
            if (distinctChapters.isEmpty()) {
                _state.update { stateAfterMessage(it).copy(message = string(R.string.msg_no_label_to_download, label)) }
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

    private fun bulkDownloadMessage(label: String, result: BulkDownloadResult): String {
        if (result.changed == 0) return string(R.string.msg_no_new_downloads, label)
        val parts = buildList {
            if (result.queued > 0) add(string(R.string.msg_bulk_queued, result.queued))
            if (result.resumed > 0) add(string(R.string.msg_bulk_resumed, result.resumed))
            if (result.retried > 0) add(string(R.string.msg_bulk_retrying, result.retried))
        }.joinToString(" / ")
        return string(R.string.msg_bulk_download_result, parts, label)
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
            _state.update { it.copy(message = string(R.string.msg_paused_download)) }
        }
    }

    fun resumeDownload(jobId: String) {
        viewModelScope.launch {
            downloadDataSource.resume(jobId)
            _state.update { it.copy(message = string(R.string.msg_resumed_download)) }
        }
    }

    fun retryDownload(jobId: String) {
        viewModelScope.launch {
            downloadDataSource.retry(jobId)
            _state.update { it.copy(message = string(R.string.msg_retrying_download)) }
        }
    }

    fun removeDownload(jobId: String) {
        viewModelScope.launch {
            downloadDataSource.remove(jobId)
            refreshDownloadState()
            _state.update { it.copy(message = string(R.string.msg_removed_download)) }
        }
    }

    fun removeDownloadsForMedia(mediaId: Int) {
        viewModelScope.launch {
            val title = _state.value.mediaTitle(mediaId, string(R.string.sources_manga_fallback, mediaId))
            downloadDataSource.removeMedia(mediaId)
            refreshDownloadState()
            _state.update { it.copy(message = string(R.string.msg_removed_downloads_for, title)) }
        }
    }

    fun removeDownloadsForMediaSource(mediaId: Int, sourceId: Long) {
        viewModelScope.launch {
            val snapshot = _state.value
            val title = snapshot.mediaTitle(mediaId, string(R.string.sources_manga_fallback, mediaId))
            val sourceName = snapshot.downloadSourceName(sourceId, string(R.string.sources_source_fallback, sourceId))
            downloadDataSource.removeMediaSource(mediaId, sourceId)
            refreshDownloadState()
            _state.update { it.copy(message = string(R.string.msg_removed_source_downloads_for, sourceName, title)) }
        }
    }

    fun removeAllDownloads() {
        viewModelScope.launch {
            downloadDataSource.removeAll()
            refreshDownloadState()
            _state.update { it.copy(message = string(R.string.msg_removed_all_downloads)) }
        }
    }

    fun refreshCacheStorageSummary() {
        viewModelScope.launch {
            runCatching {
                cacheStorageDataSource.summary()
            }.onSuccess { summary ->
                _state.update { it.copy(cacheStorageSummary = summary) }
            }.onFailure { error ->
                Log.w(TAG, "Cache storage summary failed", error)
            }
        }
    }

    fun clearAnilistAndImageCache() {
        clearCacheStorage(CacheClearTarget.ANILIST_IMAGES)
    }

    fun clearSourceNetworkCache() {
        clearCacheStorage(CacheClearTarget.SOURCE_NETWORK)
    }

    fun clearReaderPageCache() {
        cancelReaderPageCacheJobs()
        clearCacheStorage(CacheClearTarget.READER_PAGES)
    }

    fun clearTemporaryCache() {
        clearCacheStorage(CacheClearTarget.TEMPORARY_FILES)
    }

    fun clearAllCaches() {
        cancelReaderPageCacheJobs()
        clearCacheStorage(CacheClearTarget.ALL)
    }

    private fun clearCacheStorage(target: CacheClearTarget) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                cacheStorageDataSource.clear(target)
                cacheStorageDataSource.summary()
            }.onSuccess { summary ->
                _state.update {
                    it.copy(
                        busy = false,
                        cacheStorageSummary = summary,
                        message = string(R.string.msg_cache_cleared),
                    )
                }
            }.onFailure { error ->
                Log.w(TAG, "Cache clear failed for $target", error)
                _state.update {
                    it.copy(
                        busy = false,
                        message = error.userMessage(localizedContext(), string(R.string.msg_cache_clear_failed)),
                    )
                }
            }
        }
    }

    fun retryFailedDownloads() {
        val failed = _state.value.downloads.filter { it.state == DownloadState.FAILED }
        viewModelScope.launch {
            failed.forEach { downloadDataSource.retry(it.id) }
            if (failed.isNotEmpty()) {
                _state.update { it.copy(message = string(R.string.msg_retrying_failed_downloads, failed.size)) }
            }
        }
    }

    fun pauseActiveDownloads() {
        val active = _state.value.downloads.filter { it.state == DownloadState.QUEUED || it.state == DownloadState.RUNNING }
        viewModelScope.launch {
            active.forEach { downloadDataSource.pause(it.id) }
            if (active.isNotEmpty()) {
                _state.update { it.copy(message = string(R.string.msg_paused_downloads, active.size)) }
            }
        }
    }

    fun resumePausedDownloads() {
        val paused = _state.value.downloads.filter { it.state == DownloadState.PAUSED }
        viewModelScope.launch {
            paused.forEach { downloadDataSource.resume(it.id) }
            if (paused.isNotEmpty()) {
                _state.update { it.copy(message = string(R.string.msg_resumed_downloads, paused.size)) }
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

    private fun maybeSyncAutomaticReaderStatusForPosition(
        media: AnilistMedia,
        chapter: SourceChapter,
        pages: List<ReaderPage>,
        pageIndex: Int,
    ) {
        val snapshot = _state.value
        val status = automaticStatusForReaderPosition(
            pageIndex = pageIndex,
            pages = pages,
            mediaStatus = media.status,
            chapter = chapter,
            sourceChapters = snapshot.sourceChapters,
            currentStatus = snapshot.trackedListEntryFor(media.id)?.status,
            enabled = snapshot.autoUpdateStatusFromReading,
        ) ?: return
        viewModelScope.launch {
            syncAniListProgressFromChapter(
                media = media,
                chapterProgress = 0,
                triggeredByManualRead = false,
                status = status,
            )
        }
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
            val snapshot = _state.value
            val completedChapterNumber = chapter.chapterNumber.toInt()
            val syncProgress = completedChapterNumber.takeIf { progress.completed && it > 0 }
            val status = automaticStatusForReaderPosition(
                pageIndex = progress.pageIndex,
                totalPages = progress.totalPages,
                mediaStatus = media.status,
                chapter = chapter,
                sourceChapters = snapshot.sourceChapters,
                currentStatus = snapshot.trackedListEntryFor(media.id)?.status,
                enabled = snapshot.autoUpdateStatusFromReading,
            )
            if (syncProgress != null || status != null) {
                syncAniListProgressFromChapter(
                    media = media,
                    chapterProgress = syncProgress ?: 0,
                    triggeredByManualRead = false,
                    status = status,
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
        private const val OAUTH_STATE_BYTES = 24
        private const val RECENT_READING_LIMIT = 3
        private const val TRACKING_AUTO_SAVE_DELAY_MILLIS = 1_200L
        private const val APP_UPDATE_CHECK_INTERVAL_MILLIS = 24L * 60L * 60L * 1_000L
        private val secureRandom = SecureRandom()
    }
}

private fun TankobunUiState.usesTrendingResultsCacheTtl(): Boolean =
    searchQuery.trim().isBlank() &&
        browseStaffName.isNullOrBlank() &&
        browseSort == BROWSE_TRENDING_SORT

private fun TankobunUiState.toBrowseCriteria(): BrowseCriteria =
    BrowseCriteria(
        searchQuery = searchQuery,
        genres = browseGenres,
        tags = browseTags,
        format = browseFormat,
        publishingStatus = browsePublishingStatus,
        countryOfOrigin = browseCountryOfOrigin,
        year = browseYear,
        staffName = browseStaffName,
        sort = browseSort,
    )

private fun TankobunUiState.withBrowseCriteria(criteria: BrowseCriteria): TankobunUiState =
    copy(
        searchQuery = criteria.searchQuery,
        searchResults = emptyList(),
        browseSearched = criteria.hasQueryOrFilters,
        browseResultsPage = 0,
        browseResultsHasMore = false,
        browseResultsLoadingMore = false,
        browseGenres = criteria.genres,
        browseTags = criteria.tags,
        browseFormat = criteria.format,
        browsePublishingStatus = criteria.publishingStatus,
        browseCountryOfOrigin = criteria.countryOfOrigin,
        browseYear = criteria.year,
        browseStaffName = criteria.staffName,
        browseSort = criteria.sort,
        message = null,
    )

private fun List<BrowseCriteria>.distinctConsecutive(): List<BrowseCriteria> {
    if (isEmpty()) return this
    val distinct = mutableListOf<BrowseCriteria>()
    forEach { criteria ->
        if (distinct.lastOrNull() != criteria) {
            distinct += criteria
        }
    }
    return distinct
}
