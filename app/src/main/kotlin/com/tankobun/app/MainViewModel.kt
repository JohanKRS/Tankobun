package com.tankobun.app

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.tankobun.core.anilist.AnilistGraphQlException
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
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistScoreFormat
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
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.io.InputStream
import java.io.OutputStreamWriter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToInt

data class TankobunUiState(
    val loggedIn: Boolean = false,
    val clientConfigured: Boolean = false,
    val themeMode: TankobunThemeMode = TankobunThemeMode.SYSTEM,
    val ignoreDisplayCutout: Boolean = false,
    val showAppStatusBar: Boolean = true,
    val viewerName: String? = null,
    val anilistScoreFormat: AnilistScoreFormat = AnilistScoreFormat.POINT_100,
    val anilistCustomLists: List<String> = emptyList(),
    val library: List<AnilistMedia> = emptyList(),
    val libraryItems: List<LibraryItem> = emptyList(),
    val librarySyncedAtEpochMillis: Long = 0L,
    val backupFolderUri: String? = null,
    val backupSchedule: BackupSchedule = BackupSchedule.OFF,
    val lastScheduledBackupAtEpochMillis: Long = 0L,
    val libraryViewMode: MediaViewMode = MediaViewMode.COVER_GRID,
    val libraryCoverColumns: Int = DEFAULT_MEDIA_COVER_COLUMNS,
    val libraryShowWholeCovers: Boolean = false,
    val browseViewMode: MediaViewMode = MediaViewMode.COVER_GRID,
    val browseCoverColumns: Int = DEFAULT_MEDIA_COVER_COLUMNS,
    val browseShowWholeCovers: Boolean = false,
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
    val trackingCustomLists: Set<String> = emptySet(),
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
    val chapterProgress: Map<String, ReadingProgress> = emptyMap(),
    val recentReadingProgress: List<RecentReadingProgress> = emptyList(),
    val activeChapter: SourceChapter? = null,
    val readerPages: List<ReaderPage> = emptyList(),
    val readerPreviousSegment: ReaderChapterSegment? = null,
    val readerNextSegment: ReaderChapterSegment? = null,
    val currentPageIndex: Int = 0,
    val currentPageScrollOffset: Int = 0,
    val downloads: List<DownloadJob> = emptyList(),
    val downloadStorageSummary: DownloadStorageSummary = DownloadStorageSummary(),
    val keepNextTenDownloads: Boolean = false,
    val anilistAutoSaveTrackingChanges: Boolean = false,
    val anilistAutoSyncReaderProgress: Boolean = true,
    val anilistSyncManualReadProgress: Boolean = true,
    val selectingDownloadChapters: Boolean = false,
    val selectedDownloadChapterUrls: Set<String> = emptySet(),
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

data class RecentReadingProgress(
    val media: AnilistMedia,
    val progress: ReadingProgress,
    val chapter: SourceChapter?,
)

data class ReaderChapterSegment(
    val chapter: SourceChapter,
    val pages: List<ReaderPage>,
)

data class DownloadStorageSummary(
    val totalBytes: Long = 0L,
    val items: List<DownloadStorageItem> = emptyList(),
)

data class DownloadStorageItem(
    val mediaId: Int,
    val bytes: Long,
    val chapterCount: Int,
    val completedChapterCount: Int,
    val activeChapterCount: Int,
    val pageCount: Int,
)

private data class BulkDownloadResult(
    val queued: Int = 0,
    val resumed: Int = 0,
    val retried: Int = 0,
    val skipped: Int = 0,
) {
    val changed: Int
        get() = queued + resumed + retried
}

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

private data class BackupRestoreEntry(
    val mediaId: Int?,
    val idMal: Int?,
    val status: MediaStatus,
    val progress: Int?,
    val score: Double?,
    val notes: String?,
    val private: Boolean?,
    val customLists: List<String>,
)

private data class BackupRestoreResult(
    val restored: Int,
    val skipped: Int,
)

class MainViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val progressCalculator = ReaderProgressCalculator()
    private val syncMutationFactory = SyncMutationFactory()
    private val syncBackoff = SyncBackoff()
    private val cachePolicy = CachePolicy()
    private var trackingAutoSaveJob: Job? = null
    private var pendingAniListSyncJob: Job? = null
    private var scheduledBackupJob: Job? = null
    private var readerAdjacentLoadJob: Job? = null
    private var lastReaderProgressSavedAtEpochMillis: Long = 0L
    private val readerPageCacheJobs = ConcurrentHashMap<String, Job>()
    private val _state = MutableStateFlow(
        TankobunUiState(
            loggedIn = container.tokenStore.accessToken() != null,
            clientConfigured = BuildConfig.ANILIST_CLIENT_ID.isNotBlank(),
            viewerName = container.settingsStore.viewerName(),
            anilistScoreFormat = container.settingsStore.anilistScoreFormat(),
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
            readerMode = container.settingsStore.readerMode(),
            readerPageGapLevel = container.settingsStore.readerPageGapLevel(),
            readerFitWidth = container.settingsStore.readerFitWidth(),
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
                val storageSummary = buildDownloadStorageSummary(downloads)
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
        container.settingsStore.saveAnilistCustomLists(emptyList())
        container.settingsStore.saveLibrarySyncedAtEpochMillis(0L)
        _state.update {
            it.copy(
                loggedIn = false,
                viewerName = null,
                anilistScoreFormat = AnilistScoreFormat.POINT_100,
                anilistCustomLists = emptyList(),
                library = emptyList(),
                libraryItems = emptyList(),
                librarySyncedAtEpochMillis = 0L,
                recentReadingProgress = emptyList(),
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

    fun setIgnoreDisplayCutout(enabled: Boolean) {
        container.settingsStore.saveIgnoreDisplayCutout(enabled)
        _state.update { it.copy(ignoreDisplayCutout = enabled) }
    }

    fun setShowAppStatusBar(enabled: Boolean) {
        container.settingsStore.saveShowAppStatusBar(enabled)
        _state.update { it.copy(showAppStatusBar = enabled) }
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
        val mediaById = container.database.mediaDao().cachedMedia().associateBy { it.id }
        return latestProgress.mapNotNull { progress ->
            val media = mediaById[progress.mediaId]?.toModel() ?: return@mapNotNull null
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
                container.settingsStore.saveAnilistScoreFormat(viewer.scoreFormat)
                container.settingsStore.saveAnilistCustomLists(viewer.mangaCustomLists)
                container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
                _state.update {
                    it.copy(
                        viewerName = viewer.name,
                        anilistScoreFormat = viewer.scoreFormat,
                        anilistCustomLists = viewer.mangaCustomLists,
                        library = entries.map { pair -> pair.first },
                        libraryItems = entries.map { (media, entry) -> LibraryItem(media, entry) },
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
                val xml = buildMyAnimeListBackupXml(
                    items = items,
                    viewerName = snapshot.viewerName,
                    scoreFormat = snapshot.anilistScoreFormat,
                )
                withContext(Dispatchers.IO) {
                    val output = container.application.contentResolver.openOutputStream(uri)
                        ?: error("Could not open backup file")
                    OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                        writer.write(xml)
                    }
                }
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
                val entries = withContext(Dispatchers.IO) {
                    val input = container.application.contentResolver.openInputStream(uri)
                        ?: error("Could not open backup file")
                    input.use { stream ->
                        parseMyAnimeListBackupXml(stream, _state.value.anilistScoreFormat)
                    }
                }
                check(entries.isNotEmpty()) { "No manga found in backup" }

                val customLists = ensureAniListCustomLists(
                    accessToken = token,
                    requestedCustomLists = entries.flatMap { it.customLists },
                )
                var restored = 0
                var skipped = 0
                val now = System.currentTimeMillis()
                entries.forEach { entry ->
                    val media = entry.mediaId
                        ?.let { mediaId -> container.anilistRepository.mangaById(mediaId) }
                        ?: entry.idMal?.let { idMal -> container.anilistRepository.mangaByMalId(idMal) }
                    if (media == null) {
                        skipped += 1
                    } else {
                        val savedEntry = container.anilistRepository.saveListEntry(
                            accessToken = token,
                            mediaId = media.id,
                            status = entry.status,
                            progress = entry.progress,
                            score = entry.score,
                            notes = entry.notes,
                            private = entry.private,
                            customLists = entry.customLists,
                            scoreFormat = _state.value.anilistScoreFormat,
                        )
                        container.database.mediaDao().upsertMedia(media.toEntity(now))
                        container.database.listEntryDao().upsertEntry(savedEntry.toEntity(now))
                        restored += 1
                        delay(350L)
                    }
                }
                BackupRestoreResult(restored = restored, skipped = skipped).also {
                    container.settingsStore.saveAnilistCustomLists(customLists)
                }
            }.onSuccess { result ->
                loadCachedLibrary()
                _state.update {
                    it.copy(
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
        runCatching {
            container.application.contentResolver.takePersistableUriPermission(uri, flags)
        }.onFailure { error ->
            Log.w(TAG, "Could not persist backup folder permission", error)
        }
        val uriString = uri.toString()
        container.settingsStore.saveBackupFolderUri(uriString)
        _state.update { it.copy(backupFolderUri = uriString, message = "Backup folder selected") }
        ScheduledBackupWork.update(container.application, _state.value.backupSchedule)
        runScheduledAniListBackupIfDue(force = true, reportResult = true)
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
        runScheduledAniListBackupIfDue(force = true, reportResult = true)
    }

    private fun runScheduledAniListBackupIfDue(
        force: Boolean = false,
        reportResult: Boolean = false,
    ) {
        val snapshot = _state.value
        val schedule = snapshot.backupSchedule
        if (schedule == BackupSchedule.OFF) return
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
        if (scheduledBackupJob?.isActive == true) return

        scheduledBackupJob = viewModelScope.launch {
            if (reportResult) {
                _state.update { it.copy(busy = true, message = null) }
            }
            runCatching {
                writeScheduledAniListBackup(folderUri = folderUri, snapshot = snapshot)
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

    private suspend fun writeScheduledAniListBackup(
        folderUri: Uri,
        snapshot: TankobunUiState,
    ): Int = withContext(Dispatchers.IO) {
        val fileUri = DocumentsContract.createDocument(
            container.application.contentResolver,
            folderUri,
            "text/xml",
            suggestedScheduledAniListBackupFileName(snapshot.viewerName),
        ) ?: error("Could not create backup file")
        val xml = buildMyAnimeListBackupXml(
            items = snapshot.libraryItems,
            viewerName = snapshot.viewerName,
            scoreFormat = snapshot.anilistScoreFormat,
        )
        val output = container.application.contentResolver.openOutputStream(fileUri)
            ?: error("Could not open backup file")
        OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
            writer.write(xml)
        }
        snapshot.libraryItems.size
    }

    private suspend fun ensureAniListCustomLists(
        accessToken: String,
        requestedCustomLists: List<String>,
    ): List<String> {
        val knownCustomLists = _state.value.anilistCustomLists.normalizedCustomLists()
        val missingCustomLists = requestedCustomLists.normalizedCustomLists().filterNot { selectedList ->
            knownCustomLists.any { knownList -> knownList.equals(selectedList, ignoreCase = true) }
        }
        val nextKnownCustomLists = if (missingCustomLists.isEmpty()) {
            knownCustomLists
        } else {
            container.anilistRepository.updateMangaCustomLists(
                accessToken = accessToken,
                customLists = (knownCustomLists + missingCustomLists).normalizedCustomLists(),
            ).ifEmpty { (knownCustomLists + missingCustomLists).normalizedCustomLists() }
        }
        _state.update { it.copy(anilistCustomLists = nextKnownCustomLists) }
        return nextKnownCustomLists
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
                val media = container.database.mediaDao().cachedMedia(mutation.mediaId)?.toModel()
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
            )
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
                trackingScore = existingEntry?.score.formatTrackingScore(it.anilistScoreFormat),
                trackingNotes = existingEntry?.notes.orEmpty(),
                trackingPrivate = existingEntry?.private ?: false,
                trackingCustomLists = existingEntry?.customLists.orEmpty().toSet(),
                selectedSourceManga = null,
                sourceChapters = emptyList(),
                latestProgress = null,
                chapterProgress = emptyMap(),
                activeChapter = null,
                readerPages = emptyList(),
                readerPreviousSegment = null,
                readerNextSegment = null,
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
                currentPageIndex = 0,
                currentPageScrollOffset = 0,
                selectingDownloadChapters = false,
                selectedDownloadChapterUrls = emptySet(),
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
                chapterProgress = emptyMap(),
                activeChapter = null,
                readerPages = emptyList(),
                readerPreviousSegment = null,
                readerNextSegment = null,
                currentPageIndex = 0,
                currentPageScrollOffset = 0,
                selectingDownloadChapters = false,
                selectedDownloadChapterUrls = emptySet(),
                message = null,
            )
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

    fun setReaderFitWidth(enabled: Boolean) {
        container.settingsStore.saveReaderFitWidth(enabled)
        _state.update { it.copy(readerFitWidth = enabled) }
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

    fun setTrackingStatus(status: MediaStatus) {
        _state.update { it.copy(trackingStatus = status) }
        scheduleTrackingAutoSave()
    }

    fun setTrackingProgress(progress: String) {
        _state.update { it.copy(trackingProgress = progress.filter { char -> char.isDigit() }.take(5)) }
        scheduleTrackingAutoSave()
    }

    fun setTrackingScore(score: String) {
        _state.update { it.copy(trackingScore = score.filteredScoreInput(it.anilistScoreFormat)) }
        scheduleTrackingAutoSave()
    }

    fun setTrackingNotes(notes: String) {
        _state.update { it.copy(trackingNotes = notes) }
        scheduleTrackingAutoSave()
    }

    fun setTrackingPrivate(private: Boolean) {
        _state.update { it.copy(trackingPrivate = private) }
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
            )
        }
        scheduleTrackingAutoSave()
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
            )
        }
        scheduleTrackingAutoSave()
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

        viewModelScope.launch {
            if (!autoSave) {
                _state.update { it.copy(busy = true, message = null) }
            }
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
                        busy = if (autoSave) it.busy else false,
                        message = if (autoSave) it.message else "AniList tracking saved",
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "AniList tracking save failed for ${media.id}", error)
                _state.update {
                    it.copy(
                        busy = if (autoSave) it.busy else false,
                        message = error.userMessage(if (autoSave) "Tracking auto-save failed" else "Tracking save failed"),
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
                        it.copy(
                            selectedMedia = cachedMedia?.toModel() ?: it.selectedMedia,
                            selectedListEntry = cachedEntry,
                            selectedRecommendations = cachedRecommendations,
                            selectedRecommendationsPage = cachedRecommendations.recommendationPageCount(),
                            selectedRecommendationsHasMore = cachedRecommendations.size >= RECOMMENDATIONS_PAGE_SIZE,
                            trackingStatus = cachedEntry?.status ?: it.trackingStatus,
                            trackingProgress = cachedEntry?.progress?.toString() ?: it.trackingProgress,
                            trackingScore = cachedEntry?.score.formatTrackingScore(it.anilistScoreFormat)
                                .takeIf { score -> score.isNotBlank() }
                                ?: it.trackingScore,
                            trackingNotes = cachedEntry?.notes ?: it.trackingNotes,
                            trackingPrivate = cachedEntry?.private ?: it.trackingPrivate,
                            trackingCustomLists = cachedEntry?.customLists?.toSet() ?: it.trackingCustomLists,
                        )
                    }
                }
            }

            val cachedMediaHasEnrichedDetails = cachedMedia?.let { it.staff.isNotEmpty() || it.tags.isNotEmpty() } ?: false
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
                            trackingScore = effectiveEntry?.score.formatTrackingScore(it.anilistScoreFormat)
                                .takeIf { score -> score.isNotBlank() }
                                ?: it.trackingScore,
                            trackingNotes = effectiveEntry?.notes ?: it.trackingNotes,
                            trackingPrivate = effectiveEntry?.private ?: it.trackingPrivate,
                            trackingCustomLists = effectiveEntry?.customLists?.toSet() ?: it.trackingCustomLists,
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
                        message = if (chapters.isEmpty()) "No chapters found" else "Loaded ${chapters.size} chapters",
                    )
                }
                if (_state.value.keepNextTenDownloads) {
                    val result = ensureNextTenDownloads()
                    if (result.changed > 0) {
                        _state.update { it.copy(message = "Loaded ${chapters.size} chapters / queued ${result.changed} next chapters") }
                    }
                }
            }.onFailure { error ->
                Log.w(TAG, "Chapter load failed for ${source.name}/${manga.title}", error)
                _state.update { it.copy(busy = false, message = error.message ?: "Chapter load failed") }
            }
        }
    }

    fun openChapter(chapter: SourceChapter, startFromSavedProgress: Boolean = true) {
        val state = _state.value
        val media = state.selectedMedia ?: return
        val source = state.readerSourceForChapter(chapter)
        viewModelScope.launch {
            readerAdjacentLoadJob?.cancel()
            _state.update {
                it.copy(
                    busy = true,
                    message = null,
                    readerPreviousSegment = null,
                    readerNextSegment = null,
                )
            }
            runCatching {
                loadReaderPagesForChapter(media.id, chapter, source)
            }.onSuccess { pages ->
                if (pages.isEmpty() && source == null) {
                    _state.update { it.copy(busy = false, message = "Source is not installed") }
                    return@onSuccess
                }
                val savedProgress = if (startFromSavedProgress) {
                    container.database.progressDao().progressForChapter(media.id, chapter.url)
                } else {
                    null
                }
                val startPageIndex = savedProgress?.pageIndex?.coerceIn(0, pages.lastIndex.coerceAtLeast(0)) ?: 0
                val startPageScrollOffset = savedProgress?.pageScrollOffset?.coerceAtLeast(0) ?: 0
                Log.i(TAG, "Page load ${source?.name ?: "downloaded"}/${chapter.name}: pages=${pages.size}")
                _state.update {
                    it.copy(
                        activeChapter = chapter,
                        readerPages = pages,
                        readerPreviousSegment = null,
                        readerNextSegment = null,
                        currentPageIndex = startPageIndex,
                        currentPageScrollOffset = startPageScrollOffset,
                        busy = false,
                        message = if (pages.isEmpty()) "No pages found" else "Opened ${chapter.name}",
                    )
                }
                saveReaderProgress()
                cacheReaderWindow(media.id, chapter, pages, startPageIndex)
                loadAdjacentReaderSegments(media.id, chapter)
            }.onFailure { error ->
                Log.w(TAG, "Page load failed for ${source?.name ?: "cached source"}/${chapter.name}", error)
                _state.update { it.copy(busy = false, message = error.message ?: "Reader failed") }
            }
        }
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
                    mediaId = mediaId,
                    chapter = segment.chapter,
                    pages = segment.pages.takeLast(READER_ADJACENT_CACHE_PAGE_COUNT),
                    cacheKeySuffix = "tail",
                )
            }
            nextSegment?.let { segment ->
                startReaderPageCache(
                    mediaId = mediaId,
                    chapter = segment.chapter,
                    pages = segment.pages.take(READER_ADJACENT_CACHE_PAGE_COUNT),
                    cacheKeySuffix = "head",
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
        _state.update {
            it.copy(
                activeChapter = null,
                readerPages = emptyList(),
                readerPreviousSegment = null,
                readerNextSegment = null,
                currentPageIndex = 0,
                currentPageScrollOffset = 0,
            )
        }
    }

    fun moveReaderPage(delta: Int) {
        val snapshot = _state.value
        val pages = snapshot.readerPages
        if (pages.isEmpty() || delta == 0) return
        val targetIndex = snapshot.currentPageIndex + delta
        if (delta > 0 && targetIndex > pages.lastIndex) {
            openNextChapter()
            return
        }
        val nextIndex = targetIndex.coerceIn(0, pages.lastIndex)
        if (nextIndex == snapshot.currentPageIndex) return
        _state.update { it.copy(currentPageIndex = nextIndex, currentPageScrollOffset = 0) }
        saveReaderProgress()
    }

    fun setReaderPage(index: Int, pageScrollOffset: Int = 0) {
        val snapshot = _state.value
        val pages = snapshot.readerPages
        if (pages.isEmpty()) return
        val nextIndex = index.coerceIn(0, pages.lastIndex)
        val nextOffset = pageScrollOffset.coerceAtLeast(0)
        if (nextIndex == snapshot.currentPageIndex && nextOffset == snapshot.currentPageScrollOffset) return
        _state.update { it.copy(currentPageIndex = nextIndex, currentPageScrollOffset = nextOffset) }
        saveReaderProgress()
        val media = snapshot.selectedMedia
        val chapter = snapshot.activeChapter
        if (media != null && chapter != null) {
            cacheReaderWindow(media.id, chapter, pages, nextIndex)
        }
    }

    fun setWebtoonReaderPosition(chapterUrl: String, pageIndex: Int, pageScrollOffset: Int) {
        val snapshot = _state.value
        val activeChapter = snapshot.activeChapter ?: return
        when (chapterUrl) {
            activeChapter.url -> setReaderPage(pageIndex, pageScrollOffset)
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
        saveReaderProgressFor(
            media = media,
            chapter = targetSegment.chapter,
            pages = targetSegment.pages,
            pageIndex = nextIndex,
            pageScrollOffset = pageScrollOffset,
        )
        cacheReaderWindow(media.id, targetSegment.chapter, targetSegment.pages, nextIndex)
        loadAdjacentReaderSegments(media.id, targetSegment.chapter)
    }

    private fun cacheReaderWindow(
        mediaId: Int,
        chapter: SourceChapter,
        pages: List<ReaderPage>,
        pageIndex: Int,
    ) {
        if (pages.isEmpty()) return
        val start = (pageIndex - READER_CACHE_BACK_PAGES).coerceAtLeast(0)
        val end = (pageIndex + READER_CACHE_FORWARD_PAGES).coerceAtMost(pages.lastIndex)
        startReaderPageCache(
            mediaId = mediaId,
            chapter = chapter,
            pages = pages.subList(start, end + 1),
            cacheKeySuffix = "window",
            replaceExisting = true,
        )
    }

    private fun startReaderPageCache(
        mediaId: Int,
        chapter: SourceChapter,
        pages: List<ReaderPage>,
        cacheKeySuffix: String,
        replaceExisting: Boolean = false,
    ) {
        val pagesToCache = pages.filter { it.cachedFilePath == null }
        if (pagesToCache.isEmpty()) return
        val key = "$mediaId:${chapter.sourceId}:${chapter.url}:$cacheKeySuffix"
        if (replaceExisting) {
            readerPageCacheJobs.remove(key)?.cancel()
        } else if (readerPageCacheJobs[key]?.isActive == true) {
            return
        }
        val job = viewModelScope.launch(Dispatchers.IO) {
            pagesToCache.forEachIndexed { index, page ->
                if (index > 0) delay(READER_CACHE_REQUEST_SPACING_MILLIS)
                runCatching {
                    if (ReaderPageCache.cachedBytes(container.application, mediaId, chapter, page) != null) {
                        return@runCatching
                    }
                    val request = Request.Builder()
                        .url(page.imageUrl)
                        .apply {
                            page.headers.forEach { (name, value) ->
                                if (name.isNotBlank() && value.isNotBlank()) header(name, value)
                            }
                        }
                        .build()
                    container.okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            error("Reader cache page ${page.index + 1} failed: HTTP ${response.code}")
                        }
                        ReaderPageCache.writePage(
                            context = container.application,
                            mediaId = mediaId,
                            chapter = chapter,
                            page = page,
                            bytes = response.body.bytes(),
                        )
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

    fun openNextChapter() {
        val snapshot = _state.value
        val nextChapter = snapshot.sourceChapters.nextInReadingOrderAfter(snapshot.activeChapter ?: return) ?: return
        if (snapshot.readerPages.isNotEmpty()) {
            _state.update { it.copy(currentPageIndex = snapshot.readerPages.lastIndex, currentPageScrollOffset = 0) }
        }
        saveReaderProgress()
        openChapter(nextChapter, startFromSavedProgress = false)
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
        val storageSummary = buildDownloadStorageSummary(downloads)
        _state.update {
            it.copy(
                downloads = downloads,
                downloadStorageSummary = storageSummary,
            )
        }
    }

    private suspend fun buildDownloadStorageSummary(downloads: List<DownloadJob>): DownloadStorageSummary =
        withContext(Dispatchers.IO) {
            val pages = container.database.downloadPageDao().allPages()
            val bytesByMedia = mutableMapOf<Int, Long>()
            val pageCountByMedia = mutableMapOf<Int, Int>()
            pages.forEach { page ->
                val file = File(page.filePath)
                val bytes = if (file.exists()) file.length().coerceAtLeast(0L) else 0L
                bytesByMedia[page.mediaId] = bytesByMedia.getOrDefault(page.mediaId, 0L) + bytes
                pageCountByMedia[page.mediaId] = pageCountByMedia.getOrDefault(page.mediaId, 0) + 1
            }
            val jobsByMedia = downloads.groupBy { it.mediaId }
            val mediaIds = (bytesByMedia.keys + jobsByMedia.keys).toSet()
            val items = mediaIds
                .map { mediaId ->
                    val jobs = jobsByMedia[mediaId].orEmpty()
                    val chapterUrls = jobs.map { it.chapterUrl }.distinct()
                    DownloadStorageItem(
                        mediaId = mediaId,
                        bytes = bytesByMedia.getOrDefault(mediaId, 0L),
                        chapterCount = chapterUrls.size,
                        completedChapterCount = jobs
                            .filter { it.state == DownloadState.COMPLETE }
                            .map { it.chapterUrl }
                            .distinct()
                            .size,
                        activeChapterCount = jobs
                            .filter { it.state == DownloadState.QUEUED || it.state == DownloadState.RUNNING || it.state == DownloadState.PAUSED }
                            .map { it.chapterUrl }
                            .distinct()
                            .size,
                        pageCount = pageCountByMedia.getOrDefault(mediaId, 0),
                    )
                }
                .filter { it.bytes > 0L || it.chapterCount > 0 }
                .sortedWith(compareByDescending<DownloadStorageItem> { it.bytes }.thenBy { it.mediaId })
            DownloadStorageSummary(
                totalBytes = items.sumOf { it.bytes },
                items = items,
            )
        }

    private fun saveReaderProgress() {
        val media = _state.value.selectedMedia ?: return
        val chapter = _state.value.activeChapter ?: return
        val pages = _state.value.readerPages
        if (pages.isEmpty()) return
        saveReaderProgressFor(
            media = media,
            chapter = chapter,
            pages = pages,
            pageIndex = _state.value.currentPageIndex,
            pageScrollOffset = _state.value.currentPageScrollOffset,
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
        private const val SOURCE_SEARCH_QUERY_LIMIT = 12
        private const val SOURCE_CANDIDATES_TO_VERIFY = 5
        private const val SOURCE_FALLBACK_CANDIDATES_PER_SOURCE = 5
        private const val SOURCE_MAX_CANDIDATES_PER_SOURCE = 40
        private const val SOURCE_STRONG_MATCH_SCORE = 0.9
        private const val TRACKING_AUTO_SAVE_DELAY_MILLIS = 1_200L
        private const val READER_CACHE_BACK_PAGES = 4
        private const val READER_CACHE_FORWARD_PAGES = 6
        private const val READER_ADJACENT_CACHE_PAGE_COUNT = 4
        private const val READER_CACHE_REQUEST_SPACING_MILLIS = 1_200L
    }
}

private fun JSONObject.nullableString(name: String): String? =
    if (!has(name) || isNull(name)) null else optString(name)

private fun JSONObject.nullableInt(name: String): Int? =
    if (!has(name) || isNull(name)) null else optInt(name)

private fun JSONObject.nullableDouble(name: String): Double? =
    if (!has(name) || isNull(name)) null else optDouble(name)

private fun JSONObject.nullableBoolean(name: String): Boolean? =
    if (!has(name) || isNull(name)) null else optBoolean(name)

private fun JSONObject.nullableStringList(name: String): List<String>? {
    if (!has(name) || isNull(name)) return null
    val array = optJSONArray(name) ?: return null
    return buildList {
        for (index in 0 until array.length()) {
            val value = array.optString(index).trim()
            if (value.isNotBlank()) add(value)
        }
    }
}

internal fun buildMyAnimeListBackupXml(
    items: List<LibraryItem>,
    viewerName: String?,
    scoreFormat: AnilistScoreFormat,
): String {
    val sortedItems = items.sortedWith(
        compareBy<LibraryItem> { it.entry.status.malSortOrder() }
            .thenBy { it.media.title.userPreferred.lowercase(Locale.ROOT) },
    )
    val counts = sortedItems
        .groupingBy { it.entry.status.toMyAnimeListStatus() }
        .eachCount()
    return buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8" ?>""")
        appendLine()
        appendLine("\t<!--")
        appendLine("\t Created by Tankobun AniList backup")
        appendLine("\t MyAnimeList-compatible manga XML for AniList import")
        appendLine("\t AniList-only metadata is kept in XML comments where MAL has no matching field")
        appendLine("\t-->")
        appendLine()
        appendLine("\t<myanimelist>")
        appendLine()
        appendLine("\t\t<myinfo>")
        appendLine("\t\t\t<user_id>0</user_id>")
        appendLine("\t\t\t<user_name>${viewerName.orEmpty().xmlEscaped()}</user_name>")
        appendLine("\t\t\t<user_export_type>2</user_export_type>")
        appendLine("\t\t\t<user_total_manga>${sortedItems.size}</user_total_manga>")
        appendLine("\t\t\t<user_total_reading>${counts.getOrDefault("Reading", 0)}</user_total_reading>")
        appendLine("\t\t\t<user_total_completed>${counts.getOrDefault("Completed", 0)}</user_total_completed>")
        appendLine("\t\t\t<user_total_onhold>${counts.getOrDefault("On-Hold", 0)}</user_total_onhold>")
        appendLine("\t\t\t<user_total_dropped>${counts.getOrDefault("Dropped", 0)}</user_total_dropped>")
        appendLine("\t\t\t<user_total_plantoread>${counts.getOrDefault("Plan to Read", 0)}</user_total_plantoread>")
        appendLine("\t\t</myinfo>")
        appendLine()
        sortedItems.forEach { item ->
            appendLine()
            appendLine("\t\t<!-- ${item.toAniListBackupComment()} -->")
            appendLine("\t\t<manga>")
            appendLine("\t\t\t<manga_mangadb_id>${item.media.idMal ?: 0}</manga_mangadb_id>")
            appendLine("\t\t\t<manga_title>${item.media.title.userPreferred.cdata()}</manga_title>")
            appendLine("\t\t\t<manga_volumes>${item.media.volumes ?: 0}</manga_volumes>")
            appendLine("\t\t\t<manga_chapters>${item.media.chapters ?: 0}</manga_chapters>")
            appendLine("\t\t\t<my_id>${item.entry.id}</my_id>")
            appendLine("\t\t\t<my_read_volumes>${item.readVolumesForBackup()}</my_read_volumes>")
            appendLine("\t\t\t<my_read_chapters>${item.entry.progress.coerceAtLeast(0)}</my_read_chapters>")
            appendLine("\t\t\t<my_start_date>0000-00-00</my_start_date>")
            appendLine("\t\t\t<my_finish_date>0000-00-00</my_finish_date>")
            appendLine("\t\t\t<my_scanalation_group><![CDATA[]]></my_scanalation_group>")
            appendLine("\t\t\t<my_score>${item.entry.score.toMyAnimeListScore(scoreFormat)}</my_score>")
            appendLine("\t\t\t<my_storage></my_storage>")
            appendLine("\t\t\t<my_retail_volumes>0</my_retail_volumes>")
            appendLine("\t\t\t<my_status>${item.entry.status.toMyAnimeListStatus()}</my_status>")
            appendLine("\t\t\t<my_comments>${item.entry.notes.orEmpty().cdata()}</my_comments>")
            appendLine("\t\t\t<my_times_read>${if (item.entry.status == MediaStatus.REPEATING) 1 else 0}</my_times_read>")
            appendLine("\t\t\t<my_tags>${item.entry.customLists.joinToString(", ").cdata()}</my_tags>")
            appendLine("\t\t\t<my_priority>Low</my_priority>")
            appendLine("\t\t\t<my_reread_value></my_reread_value>")
            appendLine("\t\t\t<my_rereading>${if (item.entry.status == MediaStatus.REPEATING) "YES" else "NO"}</my_rereading>")
            appendLine("\t\t\t<my_discuss>YES</my_discuss>")
            appendLine("\t\t\t<my_sns>default</my_sns>")
            appendLine("\t\t\t<update_on_import>1</update_on_import>")
            appendLine("\t\t</manga>")
        }
        appendLine()
        appendLine("\t</myanimelist>")
    }
}

private fun suggestedScheduledAniListBackupFileName(viewerName: String?): String {
    val userPart = viewerName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
        ?: "user"
    return "tankobun_anilist_backup_${userPart}_${System.currentTimeMillis()}.xml"
}

private fun parseMyAnimeListBackupXml(
    input: InputStream,
    scoreFormat: AnilistScoreFormat,
): List<BackupRestoreEntry> {
    val document = DocumentBuilderFactory.newInstance()
        .apply {
            isIgnoringComments = false
            isXIncludeAware = false
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        }
        .newDocumentBuilder()
        .parse(input)
    val root = document.documentElement
    val entries = mutableListOf<BackupRestoreEntry>()
    var pendingAniListMediaId: Int? = null
    var pendingPrivate: Boolean? = null
    val children = root.childNodes
    for (index in 0 until children.length) {
        val node = children.item(index)
        when (node.nodeType) {
            Node.COMMENT_NODE -> {
                val comment = node.nodeValue.orEmpty()
                pendingAniListMediaId = Regex("""AniList media id:\s*(\d+)""")
                    .find(comment)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                pendingPrivate = comment.contains("private: true", ignoreCase = true).takeIf { it }
            }
            Node.ELEMENT_NODE -> {
                val element = node as? Element ?: continue
                if (element.tagName != "manga") continue
                val idMal = element.childText("manga_mangadb_id")?.toIntOrNull()?.takeIf { it > 0 }
                val customLists = element.childText("my_tags")
                    .orEmpty()
                    .split(',')
                    .normalizedCustomLists()
                entries += BackupRestoreEntry(
                    mediaId = pendingAniListMediaId,
                    idMal = idMal,
                    status = element.childText("my_status").toMediaStatusFromMyAnimeList(),
                    progress = element.childText("my_read_chapters")?.toIntOrNull()?.coerceAtLeast(0),
                    score = element.childText("my_score")
                        ?.toIntOrNull()
                        ?.toAniListScoreFromMyAnimeList(scoreFormat),
                    notes = element.childText("my_comments")?.trim()?.ifBlank { null },
                    private = pendingPrivate,
                    customLists = customLists,
                )
                pendingAniListMediaId = null
                pendingPrivate = null
            }
        }
    }
    return entries
}

private fun Element.childText(tagName: String): String? {
    val nodes = getElementsByTagName(tagName)
    if (nodes.length == 0) return null
    return nodes.item(0)?.textContent?.trim()
}

private fun LibraryItem.toAniListBackupComment(): String =
    buildList {
        add("AniList media id: ${media.id}")
        add("AniList list entry id: ${entry.id}")
        media.siteUrl?.takeIf { it.isNotBlank() }?.let { add("AniList URL: $it") }
        if (media.idMal == null) add("No MAL id; this entry may need manual matching on import")
        if (entry.private) add("private: true")
        if (entry.customLists.isNotEmpty()) add("custom lists: ${entry.customLists.joinToString(", ")}")
        media.format?.takeIf { it.isNotBlank() }?.let { add("format: $it") }
    }.joinToString("; ").xmlCommentEscaped()

private fun LibraryItem.readVolumesForBackup(): Int {
    val volumes = media.volumes ?: 0
    return if (entry.status == MediaStatus.COMPLETED) volumes.coerceAtLeast(0) else 0
}

private fun MediaStatus.toMyAnimeListStatus(): String =
    when (this) {
        MediaStatus.CURRENT,
        MediaStatus.REPEATING -> "Reading"
        MediaStatus.PLANNING,
        MediaStatus.UNKNOWN -> "Plan to Read"
        MediaStatus.COMPLETED -> "Completed"
        MediaStatus.DROPPED -> "Dropped"
        MediaStatus.PAUSED -> "On-Hold"
    }

private fun String?.toMediaStatusFromMyAnimeList(): MediaStatus =
    when (this?.trim()?.lowercase(Locale.ROOT)) {
        "reading" -> MediaStatus.CURRENT
        "completed" -> MediaStatus.COMPLETED
        "on-hold", "on hold" -> MediaStatus.PAUSED
        "dropped" -> MediaStatus.DROPPED
        "plan to read", "plantoread" -> MediaStatus.PLANNING
        else -> MediaStatus.PLANNING
    }

private fun MediaStatus.malSortOrder(): Int =
    when (this) {
        MediaStatus.CURRENT -> 0
        MediaStatus.REPEATING -> 1
        MediaStatus.COMPLETED -> 2
        MediaStatus.PAUSED -> 3
        MediaStatus.DROPPED -> 4
        MediaStatus.PLANNING -> 5
        MediaStatus.UNKNOWN -> 6
    }

private fun Int.toAniListScoreFromMyAnimeList(format: AnilistScoreFormat): Double? {
    val value = takeIf { it > 0 }?.coerceIn(0, 10) ?: return null
    return when (format) {
        AnilistScoreFormat.POINT_100 -> (value * 10).toDouble()
        AnilistScoreFormat.POINT_10_DECIMAL,
        AnilistScoreFormat.POINT_10 -> value.toDouble()
        AnilistScoreFormat.POINT_5 -> (value / 2.0).roundToInt().coerceIn(0, 5).toDouble()
        AnilistScoreFormat.POINT_3 -> (value * 3.0 / 10.0).roundToInt().coerceIn(0, 3).toDouble()
    }
}

private fun Double?.toMyAnimeListScore(format: AnilistScoreFormat): Int {
    val value = this ?: return 0
    val score = when (format) {
        AnilistScoreFormat.POINT_100 -> value / 10.0
        AnilistScoreFormat.POINT_10_DECIMAL,
        AnilistScoreFormat.POINT_10 -> value
        AnilistScoreFormat.POINT_5 -> value * 2.0
        AnilistScoreFormat.POINT_3 -> value * (10.0 / 3.0)
    }
    return score.roundToInt().coerceIn(0, 10)
}

private fun String.cdata(): String =
    "<![CDATA[${replace("]]>", "]]]]><![CDATA[>")}]]>"

private fun String.xmlEscaped(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

private fun String.xmlCommentEscaped(): String =
    replace("--", "- -")
        .replace("<", "(")
        .replace(">", ")")

internal fun List<SourceChapter>.nextInReadingOrderAfter(chapter: SourceChapter): SourceChapter? {
    if (chapter.chapterNumber > 0f) {
        return filter { it.chapterNumber > chapter.chapterNumber }
            .minByOrNull { it.chapterNumber }
    }

    val currentIndex = indexOfFirst { it.sourceId == chapter.sourceId && it.url == chapter.url }
    return if (currentIndex > 0) this[currentIndex - 1] else null
}

internal fun List<SourceChapter>.previousInReadingOrderBefore(chapter: SourceChapter): SourceChapter? {
    if (chapter.chapterNumber > 0f) {
        return filter { it.chapterNumber < chapter.chapterNumber }
            .maxByOrNull { it.chapterNumber }
    }

    val currentIndex = indexOfFirst { it.sourceId == chapter.sourceId && it.url == chapter.url }
    return if (currentIndex >= 0 && currentIndex < lastIndex) this[currentIndex + 1] else null
}

private fun TankobunUiState.readerSourceForChapter(chapter: SourceChapter): SourceDescriptor? =
    installedSources.firstOrNull { it.id == chapter.sourceId }
        ?: allInstalledSources.firstOrNull { it.id == chapter.sourceId }
        ?: selectedSource?.takeIf { it.id == chapter.sourceId }

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
private const val NEXT_DOWNLOAD_WINDOW_SIZE = 10
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

private fun Iterable<String>.normalizedCustomLists(): List<String> =
    map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase(Locale.ROOT) }

private fun BackupSchedule.isDue(lastRunAt: Long, now: Long): Boolean {
    if (this == BackupSchedule.OFF) return false
    if (lastRunAt <= 0L) return true
    return now - lastRunAt >= when (this) {
        BackupSchedule.OFF -> Long.MAX_VALUE
        BackupSchedule.DAILY -> 24L * 60L * 60L * 1_000L
        BackupSchedule.WEEKLY -> 7L * 24L * 60L * 60L * 1_000L
        BackupSchedule.MONTHLY -> 30L * 24L * 60L * 60L * 1_000L
    }
}

private fun String.filteredScoreInput(format: AnilistScoreFormat): String {
    val allowDecimal = format == AnilistScoreFormat.POINT_10_DECIMAL
    var hasDecimal = false
    return buildString {
        this@filteredScoreInput.forEach { char ->
            when {
                char.isDigit() -> append(char)
                allowDecimal && char == '.' && !hasDecimal -> {
                    append(char)
                    hasDecimal = true
                }
            }
        }
    }.take(if (allowDecimal) 4 else 3)
}

private fun String.toAniListScore(format: AnilistScoreFormat): Double? {
    val value = trim().toDoubleOrNull() ?: return null
    return when (format) {
        AnilistScoreFormat.POINT_100 -> value.coerceIn(0.0, 100.0).roundToInt().toDouble()
        AnilistScoreFormat.POINT_10_DECIMAL -> (value.coerceIn(0.0, 10.0) * 10).roundToInt() / 10.0
        AnilistScoreFormat.POINT_10 -> value.coerceIn(0.0, 10.0).roundToInt().toDouble()
        AnilistScoreFormat.POINT_5 -> value.coerceIn(0.0, 5.0).roundToInt().toDouble()
        AnilistScoreFormat.POINT_3 -> value.coerceIn(0.0, 3.0).roundToInt().toDouble()
    }
}

private fun Double?.formatTrackingScore(format: AnilistScoreFormat): String {
    val value = this ?: return ""
    return when (format) {
        AnilistScoreFormat.POINT_10_DECIMAL -> "%.1f".format(Locale.US, value)
        AnilistScoreFormat.POINT_100,
        AnilistScoreFormat.POINT_10,
        AnilistScoreFormat.POINT_5,
        AnilistScoreFormat.POINT_3 -> value.roundToInt().toString()
    }
}

private fun TankobunUiState.mediaTitle(mediaId: Int): String =
    libraryItems.firstOrNull { it.media.id == mediaId }?.media?.title?.userPreferred
        ?: library.firstOrNull { it.id == mediaId }?.title?.userPreferred
        ?: selectedMedia?.takeIf { it.id == mediaId }?.title?.userPreferred
        ?: "Manga $mediaId"

private fun nextTenDownloadCandidates(state: TankobunUiState): List<SourceChapter> {
    val chapters = state.sourceChapters.readingOrder()
    if (chapters.isEmpty()) return emptyList()
    val progress = state.latestProgress
    val startIndex = if (progress == null) {
        0
    } else {
        val exactIndex = chapters.indexOfFirst { it.url == progress.chapterUrl }
        when {
            exactIndex >= 0 && progress.completed -> exactIndex + 1
            exactIndex >= 0 -> exactIndex
            progress.chapterNumber > 0f -> chapters.indexOfFirst { it.chapterNumber >= progress.chapterNumber }
                .takeIf { it >= 0 } ?: 0
            else -> 0
        }
    }.coerceIn(0, chapters.size)

    return chapters
        .drop(startIndex)
        .filterNot { state.chapterProgress[it.url]?.completed == true }
        .take(NEXT_DOWNLOAD_WINDOW_SIZE)
}

private fun List<SourceChapter>.readingOrder(): List<SourceChapter> =
    if (any { it.chapterNumber > 0f }) {
        sortedWith(compareBy<SourceChapter> { it.chapterNumber.takeIf { number -> number > 0f } ?: Float.MAX_VALUE }
            .thenBy { it.name })
    } else {
        asReversed()
    }

private fun bulkDownloadMessage(label: String, result: BulkDownloadResult): String {
    if (result.changed == 0) return "No new $label to download"
    val parts = buildList {
        if (result.queued > 0) add("queued ${result.queued}")
        if (result.resumed > 0) add("resumed ${result.resumed}")
        if (result.retried > 0) add("retrying ${result.retried}")
    }
    return "${parts.joinToString(" / ")} $label"
}

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
