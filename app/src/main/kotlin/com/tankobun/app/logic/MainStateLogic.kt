package com.tankobun.app.logic

import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.state.LibraryItem
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.withTitleLanguage
import java.util.Locale

internal fun TankobunUiState.readerSourceForChapter(chapter: SourceChapter): SourceDescriptor? =
    installedSources.firstOrNull { it.id == chapter.sourceId }
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

internal fun TankobunUiState.mediaTitle(mediaId: Int): String =
    libraryItems.firstOrNull { it.media.id == mediaId }?.media?.title?.userPreferred
        ?: library.firstOrNull { it.id == mediaId }?.title?.userPreferred
        ?: selectedMedia?.takeIf { it.id == mediaId }?.title?.userPreferred
        ?: "Manga $mediaId"

internal fun TankobunUiState.downloadSourceName(sourceId: Long): String =
    installedSources.firstOrNull { it.id == sourceId }?.name
        ?: allInstalledSources.firstOrNull { it.id == sourceId }?.name
        ?: selectedSource?.takeIf { it.id == sourceId }?.name
        ?: "Source $sourceId"

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

    val effectiveMedia = media ?: selectedMedia
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
        trackingStatus = if (preserveTrackingForm) trackingStatus else entry?.status ?: trackingStatus,
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
