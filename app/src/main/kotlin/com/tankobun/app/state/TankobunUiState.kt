package com.tankobun.app.state

import com.tankobun.app.BackupSchedule
import com.tankobun.app.DEFAULT_MEDIA_COVER_COLUMNS
import com.tankobun.app.MediaViewMode
import com.tankobun.app.TankobunThemeMode
import com.tankobun.app.defaultSourceLanguages
import com.tankobun.app.logic.BROWSE_SORT_SEARCH_MATCH
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceManga
import com.tankobun.core.model.SourceSearchResult

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
