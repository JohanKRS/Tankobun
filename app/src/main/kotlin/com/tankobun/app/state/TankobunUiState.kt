package com.tankobun.app.state

import com.tankobun.app.BackupSchedule
import com.tankobun.app.BackupContent
import com.tankobun.app.DEFAULT_MEDIA_COVER_COLUMNS
import com.tankobun.app.DockAlignment
import com.tankobun.app.DockIndicatorAnimation
import com.tankobun.app.LibraryMode
import com.tankobun.app.MediaViewMode
import com.tankobun.app.AppLanguage
import com.tankobun.app.ReaderScreenOrientation
import com.tankobun.app.TankobunThemePreference
import com.tankobun.app.defaultSourceLanguages
import com.tankobun.app.logic.BROWSE_SORT_SEARCH_MATCH
import com.tankobun.app.updates.AppUpdateInfo
import com.tankobun.app.sharing.RecommendationImportPreview
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistGenreHighlight
import com.tankobun.core.model.AnilistMangaStats
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceManga
import com.tankobun.core.model.SourceSearchResult

data class LocalReadingActivity(
    val generatedAtEpochMillis: Long = 0L,
    val chaptersTracked: Int = 0,
    val chaptersToday: Int = 0,
    val chaptersLast7Days: Int = 0,
    val chaptersLast30Days: Int = 0,
    val averagePerActiveDay30: Double = 0.0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val totalReadingDays: Int = 0,
    val last14Days: List<Int> = List(14) { 0 },
)

data class TankobunUiState(
    val loggedIn: Boolean = false,
    val clientConfigured: Boolean = false,
    val themePreference: TankobunThemePreference = TankobunThemePreference(),
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val ignoreDisplayCutout: Boolean = true,
    val showAppStatusBar: Boolean = true,
    val dockAlignment: DockAlignment = DockAlignment.CENTER,
    val dockIndicatorAnimation: DockIndicatorAnimation = DockIndicatorAnimation.POP,
    val libraryMode: LibraryMode = LibraryMode.LOCAL,
    val onboardingVisible: Boolean = false,
    val appTourVisible: Boolean = false,
    val anilistMergePromptVisible: Boolean = false,
    val readerTutorialVisible: Boolean = false,
    val viewerName: String? = null,
    val viewerAvatarUrl: String? = null,
    val viewerBannerImageUrl: String? = null,
    val customProfileAvatarUri: String? = null,
    val customProfileBannerUri: String? = null,
    val anilistMangaStats: AnilistMangaStats? = null,
    val localReadingActivity: LocalReadingActivity = LocalReadingActivity(),
    val showNsfwContent: Boolean = false,
    val anilistScoreFormat: AnilistScoreFormat = AnilistScoreFormat.POINT_100,
    val anilistTitleLanguage: AnilistTitleLanguage = AnilistTitleLanguage.ROMAJI,
    val anilistCustomLists: List<String> = emptyList(),
    val library: List<AnilistMedia> = emptyList(),
    val libraryItems: List<LibraryItem> = emptyList(),
    val librarySyncedAtEpochMillis: Long = 0L,
    val backupFolderUri: String? = null,
    val backupSchedule: BackupSchedule = BackupSchedule.OFF,
    val backupContent: BackupContent = BackupContent.BOTH,
    val scheduledBackupRetentionCount: Int = 10,
    val lastScheduledBackupAtEpochMillis: Long = 0L,
    val libraryViewMode: MediaViewMode = MediaViewMode.COVER_WITH_INFO,
    val libraryCoverColumns: Int = DEFAULT_MEDIA_COVER_COLUMNS,
    val libraryShowWholeCovers: Boolean = false,
    val browseViewMode: MediaViewMode = MediaViewMode.COVER_WITH_INFO,
    val browseCoverColumns: Int = DEFAULT_MEDIA_COVER_COLUMNS,
    val browseShowWholeCovers: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<AnilistMedia> = emptyList(),
    val browseSearched: Boolean = false,
    val browseResultsPage: Int = 0,
    val browseResultsHasMore: Boolean = false,
    val browseResultsLoadingMore: Boolean = false,
    val browseGenres: Set<String> = emptySet(),
    val browseTags: Set<String> = emptySet(),
    val browseAvailableTags: List<AnilistMediaTag> = emptyList(),
    val browseFormat: String? = null,
    val browsePublishingStatus: String? = null,
    val browseCountryOfOrigin: String? = null,
    val browseYear: Int? = null,
    val browseStaffName: String? = null,
    val browseSort: String = BROWSE_SORT_SEARCH_MATCH,
    val browseTrending: List<AnilistMedia> = emptyList(),
    val browsePopular: List<AnilistMedia> = emptyList(),
    val browsePopularManhwa: List<AnilistMedia> = emptyList(),
    val browseTopManga: List<AnilistMedia> = emptyList(),
    val browseLandingLoaded: Boolean = false,
    val homeTrending: List<AnilistMedia> = emptyList(),
    val homeGenreHighlights: List<AnilistGenreHighlight> = emptyList(),
    val homeLoaded: Boolean = false,
    val homeTrendingRefreshing: Boolean = false,
    val homeGenreHighlightsRefreshing: Boolean = false,
    val homeRefreshingGenres: Set<String> = emptySet(),
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
    val trackingDirty: Boolean = false,
    val trackingSaveInProgress: Boolean = false,
    val trackingSaveFailed: Boolean = false,
    val allInstalledSources: List<SourceDescriptor> = emptyList(),
    val untrustedExtensions: List<com.tankobun.core.extensions.UntrustedExtension> = emptyList(),
    val installedSources: List<SourceDescriptor> = emptyList(),
    val sourceLanguages: Set<String> = defaultSourceLanguages(),
    val disabledSourceKeys: Set<String> = emptySet(),
    val extensionRepositoryUrl: String = "",
    val availableExtensions: List<ExtensionIndexEntry> = emptyList(),
    val installingExtensionPackageName: String? = null,
    val extensionInstallRequest: ExtensionInstallRequest? = null,
    val appUpdateInfo: AppUpdateInfo? = null,
    val appUpdateCheckInProgress: Boolean = false,
    val appUpdateDownloadInProgress: Boolean = false,
    val appUpdateInstallRequest: AppUpdateInstallRequest? = null,
    val appUpdateLastCheckedAtEpochMillis: Long = 0L,
    val appUpdateMessage: String? = null,
    val selectedLibraryMediaIds: Set<Int> = emptySet(),
    val selectedLibraryBatchMedia: List<AnilistMedia> = emptyList(),
    val libraryShareDialogVisible: Boolean = false,
    val libraryBatchStatusDialogVisible: Boolean = false,
    val libraryBatchCustomListDialogVisible: Boolean = false,
    val libraryBatchDeleteDialogVisible: Boolean = false,
    val libraryBatchRemoveCustomList: Boolean = false,
    val recommendationImportPreview: RecommendationImportPreview? = null,
    val recommendationImportError: String? = null,
    val recommendationImportLoadingDetails: Boolean = false,
    val selectedRecommendationImportMediaIds: Set<Int> = emptySet(),
    val recommendationImportListName: String = "",
    val sourceMatches: List<SourceSearchResult> = emptyList(),
    val sourceMatchChapterCounts: Map<String, Int> = emptyMap(),
    val sourcePickerOpen: Boolean = false,
    val sourcePickerLoading: Boolean = false,
    val sourcePickerMessage: String? = null,
    val sourcePickerDiagnostics: List<String> = emptyList(),
    val sourcePickerSearchTitle: String = "",
    val selectedSourceManga: SourceManga? = null,
    val sourceChapters: List<SourceChapter> = emptyList(),
    val chapterListStartsAtFirst: Boolean = true,
    val latestProgress: ReadingProgress? = null,
    val chapterProgress: Map<String, ReadingProgress> = emptyMap(),
    val recentReadingProgress: List<RecentReadingProgress> = emptyList(),
    val activeChapter: SourceChapter? = null,
    val readerPages: List<ReaderPage> = emptyList(),
    val readerPreviousSegment: ReaderChapterSegment? = null,
    val readerNextSegment: ReaderChapterSegment? = null,
    val readerError: ReaderLoadError? = null,
    val currentPageIndex: Int = 0,
    val currentPageScrollOffset: Int = 0,
    val downloads: List<DownloadJob> = emptyList(),
    val downloadStorageSummary: DownloadStorageSummary = DownloadStorageSummary(),
    val cacheStorageSummary: CacheStorageSummary = CacheStorageSummary(),
    val backupMissingSources: List<BackupMissingSource> = emptyList(),
    val keepNextTenDownloads: Boolean = false,
    val newChapterChecksEnabled: Boolean = false,
    val lastNewChapterCheckAtEpochMillis: Long = 0L,
    val anilistAutoSaveTrackingChanges: Boolean = false,
    val anilistAutoSyncReaderProgress: Boolean = true,
    val anilistRefreshLibraryOnOpen: Boolean = true,
    val anilistSyncManualReadProgress: Boolean = true,
    val autoUpdateStatusFromReading: Boolean = true,
    val selectingDownloadChapters: Boolean = false,
    val selectedDownloadChapterUrls: Set<String> = emptySet(),
    val selectedSourceId: Long? = null,
    val selectedSourcePackageName: String? = null,
    val readerMode: ReaderMode = ReaderMode.PAGED,
    val readerPageGapLevel: Int = 0,
    val showWebtoonChapterDividers: Boolean = false,
    val readerScreenOrientation: ReaderScreenOrientation = ReaderScreenOrientation.SYSTEM,
    val busy: Boolean = false,
    val message: String? = null,
) {
    val selectedSource: SourceDescriptor?
        get() {
            val sourceId = selectedSourceId ?: return null
            return installedSources.firstOrNull { it.matchesSelectedSource(sourceId, selectedSourcePackageName) }
                ?: allInstalledSources.firstOrNull { it.matchesSelectedSource(sourceId, selectedSourcePackageName) }
                ?: installedSources.firstOrNull { it.id == sourceId }
                ?: allInstalledSources.firstOrNull { it.id == sourceId }
        }

    val librarySections: List<LibrarySection>
        get() = libraryItems.toLibrarySections()
}

internal fun TankobunUiState.selectedLibraryBatchItems(): List<LibraryItem> {
    val libraryItemsById = libraryItems.associateBy { item -> item.media.id }
    val selectedMediaById = selectedLibraryBatchMedia.associateBy { media -> media.id }
    return selectedLibraryMediaIds.mapNotNull { mediaId ->
        libraryItemsById[mediaId] ?: selectedMediaById[mediaId]?.toHiddenLibraryItem()
    }
}

private fun AnilistMedia.toHiddenLibraryItem(): LibraryItem =
    LibraryItem(
        media = this,
        entry = AnilistListEntry(
            id = -id,
            mediaId = id,
            status = MediaStatus.PLANNING,
            progress = 0,
            score = null,
            notes = null,
            private = false,
            customLists = emptyList(),
            updatedAtEpochSeconds = 0L,
            hiddenFromStatusLists = true,
        ),
    )

private fun SourceDescriptor.matchesSelectedSource(sourceId: Long, packageName: String?): Boolean =
    id == sourceId && (packageName == null || this.packageName == packageName)

data class AppUpdateInstallRequest(
    val apkUri: String,
    val expectedVersionCode: Int,
    val expectedVersionName: String,
)

private fun List<LibraryItem>.toLibrarySections(): List<LibrarySection> {
    val statusSections = listOf(
        MediaStatus.CURRENT,
        MediaStatus.PLANNING,
        MediaStatus.COMPLETED,
        MediaStatus.PAUSED,
        MediaStatus.DROPPED,
        MediaStatus.REPEATING,
        MediaStatus.UNKNOWN,
    ).mapNotNull { status ->
        val items = filter { it.entry.status == status && !it.entry.hiddenFromStatusLists }
        if (items.isEmpty()) null else LibrarySection(status.name, status.name, items, status)
    }

    val customSections = flatMap { item ->
        item.entry.customLists.map { customList -> customList to item }
    }
        .groupBy({ it.first }, { it.second })
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        .map { (name, items) -> LibrarySection("custom:$name", name, items.distinctBy { it.media.id }) }

    return statusSections + customSections
}
