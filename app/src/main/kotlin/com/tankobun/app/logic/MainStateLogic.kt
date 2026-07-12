package com.tankobun.app.logic

import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.state.LibraryItem
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.withTitleLanguage
import com.tankobun.core.model.withFallbackDetails
import java.util.Locale

internal fun TankobunUiState.readerSourceForChapter(chapter: SourceChapter): SourceDescriptor? =
    installedSources.firstOrNull {
        it.id == chapter.sourceId && it.packageName == selectedSourcePackageName
    } ?: allInstalledSources.firstOrNull {
        it.id == chapter.sourceId && it.packageName == selectedSourcePackageName
    } ?: installedSources.firstOrNull { it.id == chapter.sourceId }
        ?: allInstalledSources.firstOrNull { it.id == chapter.sourceId }
        ?: selectedSource?.takeIf { it.id == chapter.sourceId }

internal fun TankobunUiState.withAniListTitleLanguage(language: AnilistTitleLanguage): TankobunUiState =
    copy(
        anilistTitleLanguage = language,
        library = library.map { it.withTitleLanguage(language) },
        libraryItems = libraryItems.map { item -> item.copy(media = item.media.withTitleLanguage(language)) },
        recentReadingProgress = recentReadingProgress.map { item ->
            item.copy(media = item.media.withTitleLanguage(language))
        },
        searchResults = searchResults.map { it.withTitleLanguage(language) },
        browseTrending = browseTrending.map { it.withTitleLanguage(language) },
        browsePopular = browsePopular.map { it.withTitleLanguage(language) },
        browsePopularManhwa = browsePopularManhwa.map { it.withTitleLanguage(language) },
        browseTopManga = browseTopManga.map { it.withTitleLanguage(language) },
        selectedMedia = selectedMedia?.withTitleLanguage(language),
        selectedRecommendations = selectedRecommendations.map { recommendation ->
            recommendation.copy(media = recommendation.media.withTitleLanguage(language))
        },
    )

internal fun TankobunUiState.mediaTitle(mediaId: Int, fallback: String = "Manga $mediaId"): String =
    libraryItems.firstOrNull { it.media.id == mediaId }?.media?.title?.userPreferred
        ?: library.firstOrNull { it.id == mediaId }?.title?.userPreferred
        ?: selectedMedia?.takeIf { it.id == mediaId }?.title?.userPreferred
        ?: fallback

internal fun AnilistListEntry?.trackingStatusForForm(defaultStatus: MediaStatus): MediaStatus =
    when {
        this == null -> defaultStatus
        hiddenFromStatusLists -> MediaStatus.UNKNOWN
        else -> status
    }

internal fun TankobunUiState.downloadSourceName(sourceId: Long, fallback: String = "Source $sourceId"): String =
    installedSources.firstOrNull { it.id == sourceId }?.name
        ?: allInstalledSources.firstOrNull { it.id == sourceId }?.name
        ?: selectedSource?.takeIf { it.id == sourceId }?.name
        ?: fallback

internal fun TankobunUiState.withSelectedMedia(
    media: AnilistMedia,
    existingEntry: AnilistListEntry?,
): TankobunUiState =
    copy(
        selectedMedia = media,
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
        trackingStatus = existingEntry.trackingStatusForForm(MediaStatus.PLANNING),
        trackingProgress = (existingEntry?.progress ?: 0).toString(),
        trackingScore = existingEntry?.score.formatTrackingScore(anilistScoreFormat),
        trackingNotes = existingEntry?.notes.orEmpty(),
        trackingPrivate = existingEntry?.private ?: false,
        trackingCustomLists = existingEntry?.customLists.orEmpty().toSet(),
        trackingDirty = false,
        trackingSaveInProgress = false,
        trackingSaveFailed = false,
        message = null,
    ).withoutSourceReaderSelection()

internal fun TankobunUiState.withSelectedSource(sourceId: Long): TankobunUiState {
    val match = sourceMatches.firstOrNull { match ->
        match.source.id == sourceId && (
            selectedSourcePackageName == null || match.source.packageName == selectedSourcePackageName
        )
    } ?: sourceMatches.firstOrNull { match -> match.source.id == sourceId }
    val source = match?.source
        ?: installedSources.firstOrNull { it.id == sourceId && it.packageName == selectedSourcePackageName }
        ?: allInstalledSources.firstOrNull { it.id == sourceId && it.packageName == selectedSourcePackageName }
        ?: installedSources.firstOrNull { it.id == sourceId }
        ?: allInstalledSources.firstOrNull { it.id == sourceId }
    val nextPackageName = source?.packageName ?: selectedSourcePackageName
    val sameSource = selectedSourceId == sourceId && selectedSourcePackageName == nextPackageName
    return copy(
        selectedSourceId = sourceId,
        selectedSourcePackageName = nextPackageName,
        selectedSourceManga = match?.manga ?: selectedSourceManga?.takeIf { manga ->
            sameSource && manga.sourceId == sourceId
        },
        sourceChapters = sourceChapters.takeIf { sameSource }.orEmpty(),
        chapterProgress = chapterProgress.takeIf { sameSource }.orEmpty(),
    ).withoutReaderAndDownloadSelection()
}

internal fun TankobunUiState.withoutSelectedMedia(): TankobunUiState =
    if (selectedMedia == null) {
        this
    } else {
        copy(
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
            message = null,
        ).withoutSourceReaderSelection()
    }

internal fun TankobunUiState.withSelectedAniListDetails(
    mediaId: Int,
    media: AnilistMedia?,
    entry: AnilistListEntry?,
    recommendations: List<AnilistRecommendation>,
    recommendationsPage: Int,
    recommendationsHasMore: Boolean,
    recommendationsLoading: Boolean = false,
): TankobunUiState {
    if (selectedMedia?.id != mediaId) return this

    val effectiveMedia = media?.withFallbackDetails(selectedMedia) ?: selectedMedia
    val preserveTrackingForm = trackingDirty || trackingSaveInProgress || trackingSaveFailed
    val nextItems = if (entry == null) {
        libraryItems
    } else {
        (libraryItems.filterNot { item -> item.media.id == mediaId } + LibraryItem(effectiveMedia, entry))
            .sortedBy { item -> item.media.title.userPreferred.lowercase(Locale.ROOT) }
    }

    return copy(
        selectedMedia = effectiveMedia,
        selectedListEntry = entry,
        selectedRecommendations = recommendations,
        selectedRecommendationsPage = recommendationsPage,
        selectedRecommendationsHasMore = recommendationsHasMore,
        recommendationsLoading = recommendationsLoading,
        library = nextItems.map { item -> item.media },
        libraryItems = nextItems,
        trackingStatus = if (preserveTrackingForm) trackingStatus else entry.trackingStatusForForm(trackingStatus),
        trackingProgress = if (preserveTrackingForm) {
            trackingProgress
        } else {
            entry?.progress?.toString() ?: trackingProgress
        },
        trackingScore = if (preserveTrackingForm) {
            trackingScore
        } else {
            entry?.score.formatTrackingScore(anilistScoreFormat)
                .takeIf { score -> score.isNotBlank() }
                ?: trackingScore
        },
        trackingNotes = if (preserveTrackingForm) trackingNotes else entry?.notes ?: trackingNotes,
        trackingPrivate = if (preserveTrackingForm) trackingPrivate else entry?.private ?: trackingPrivate,
        trackingCustomLists = if (preserveTrackingForm) trackingCustomLists else entry?.customLists?.toSet() ?: trackingCustomLists,
    )
}

internal fun TankobunUiState.withRefreshedTrackingEntry(
    mediaId: Int,
    entry: AnilistListEntry?,
): TankobunUiState {
    if (selectedMedia?.id != mediaId) return this

    val preserveTrackingForm = trackingDirty || trackingSaveInProgress || trackingSaveFailed
    val nextItems = if (entry == null) {
        libraryItems.filterNot { item -> item.media.id == mediaId }
    } else {
        val media = selectedMedia
        (libraryItems.filterNot { item -> item.media.id == mediaId } + LibraryItem(media, entry))
            .sortedBy { item -> item.media.title.userPreferred.lowercase(Locale.ROOT) }
    }

    return copy(
        selectedListEntry = entry,
        library = nextItems.map { item -> item.media },
        libraryItems = nextItems,
        trackingStatus = if (preserveTrackingForm) {
            trackingStatus
        } else {
            entry.trackingStatusForForm(MediaStatus.PLANNING)
        },
        trackingProgress = if (preserveTrackingForm) trackingProgress else (entry?.progress ?: 0).toString(),
        trackingScore = if (preserveTrackingForm) trackingScore else entry?.score.formatTrackingScore(anilistScoreFormat),
        trackingNotes = if (preserveTrackingForm) trackingNotes else entry?.notes.orEmpty(),
        trackingPrivate = if (preserveTrackingForm) trackingPrivate else entry?.private == true,
        trackingCustomLists = if (preserveTrackingForm) trackingCustomLists else entry?.customLists?.toSet().orEmpty(),
    )
}

internal fun TankobunUiState.withSyncedListEntry(
    media: AnilistMedia?,
    entry: AnilistListEntry,
    updateTrackingForm: Boolean,
): TankobunUiState {
    val existingMedia = media
        ?: libraryItems.firstOrNull { it.media.id == entry.mediaId }?.media
        ?: selectedMedia?.takeIf { it.id == entry.mediaId }
    val nextItems = if (existingMedia == null) {
        libraryItems
    } else {
        (libraryItems.filterNot { item -> item.media.id == entry.mediaId } + LibraryItem(existingMedia, entry))
            .sortedBy { item -> item.media.title.userPreferred.lowercase(Locale.ROOT) }
    }
    val selected = selectedMedia?.id == entry.mediaId
    return copy(
        library = nextItems.map { it.media },
        libraryItems = nextItems,
        selectedListEntry = if (selected) entry else selectedListEntry,
        trackingStatus = if (selected && updateTrackingForm) entry.trackingStatusForForm(trackingStatus) else trackingStatus,
        trackingProgress = if (selected) {
            if (updateTrackingForm) {
                entry.progress.toString()
            } else {
                maxOf(trackingProgress.toIntOrNull() ?: 0, entry.progress).toString()
            }
        } else {
            trackingProgress
        },
        trackingScore = if (selected && updateTrackingForm) {
            entry.score.formatTrackingScore(anilistScoreFormat)
        } else {
            trackingScore
        },
        trackingNotes = if (selected && updateTrackingForm) entry.notes.orEmpty() else trackingNotes,
        trackingPrivate = if (selected && updateTrackingForm) entry.private else trackingPrivate,
        trackingCustomLists = if (selected && updateTrackingForm) entry.customLists.toSet() else trackingCustomLists,
        trackingDirty = if (selected && updateTrackingForm) false else trackingDirty,
        trackingSaveInProgress = if (selected) false else trackingSaveInProgress,
        trackingSaveFailed = if (selected && updateTrackingForm) false else trackingSaveFailed,
    )
}

private fun TankobunUiState.withoutSourceReaderSelection(): TankobunUiState =
    copy(
        selectedSourceManga = null,
        sourceChapters = emptyList(),
        latestProgress = null,
        chapterProgress = emptyMap(),
    ).withoutReaderAndDownloadSelection()

private fun TankobunUiState.withoutReaderAndDownloadSelection(): TankobunUiState =
    copy(
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
