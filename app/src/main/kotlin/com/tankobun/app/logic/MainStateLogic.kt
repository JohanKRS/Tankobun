package com.tankobun.app.logic

import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.withTitleLanguage

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
