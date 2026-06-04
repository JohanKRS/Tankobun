package com.tankobun.app.anilist

import com.tankobun.app.AppContainer
import com.tankobun.app.logic.RECOMMENDATIONS_PAGE_SIZE
import com.tankobun.app.logic.recommendationPageCount
import com.tankobun.app.state.LibraryItem
import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.core.anilist.AnilistViewer
import com.tankobun.core.database.toEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.CachePolicy
import com.tankobun.core.model.withTitleLanguage
import java.util.Locale

internal data class CachedLibraryData(
    val items: List<LibraryItem>,
    val syncedAtEpochMillis: Long,
)

internal data class SyncedLibraryData(
    val viewer: AnilistViewer,
    val items: List<LibraryItem>,
    val syncedAtEpochMillis: Long,
)

internal data class CachedMediaDetailsData(
    val media: AnilistMedia?,
    val entry: AnilistListEntry?,
    val recommendations: List<AnilistRecommendation>,
    val recommendationsPage: Int,
    val recommendationsHasMore: Boolean,
    val isFresh: Boolean,
)

internal data class MediaDetailsData(
    val media: AnilistMedia,
    val entry: AnilistListEntry?,
    val recommendations: List<AnilistRecommendation>,
    val recommendationsPage: Int,
    val recommendationsHasMore: Boolean,
)

internal data class RecommendationPageData(
    val recommendations: List<AnilistRecommendation>,
    val currentPage: Int,
    val hasNextPage: Boolean,
)

internal class AniListDataSource(
    private val container: AppContainer,
    private val cachePolicy: CachePolicy,
) {
    suspend fun refreshViewer(token: String): AnilistViewer {
        val viewer = container.anilistRepository.viewer(token)
        saveViewerSettings(viewer)
        return viewer
    }

    fun saveViewerSettings(viewer: AnilistViewer) {
        container.settingsStore.saveViewerName(viewer.name)
        container.settingsStore.saveAnilistScoreFormat(viewer.scoreFormat)
        container.settingsStore.saveAnilistTitleLanguage(viewer.titleLanguage)
        container.settingsStore.saveAnilistCustomLists(viewer.mangaCustomLists)
    }

    suspend fun updateTitleLanguage(token: String, titleLanguage: AnilistTitleLanguage): AnilistViewer {
        val viewer = container.anilistRepository.updateUserPreferences(
            accessToken = token,
            titleLanguage = titleLanguage,
        )
        saveViewerSettings(viewer)
        return viewer
    }

    suspend fun updateScoreFormat(token: String, scoreFormat: AnilistScoreFormat): AnilistViewer {
        val viewer = container.anilistRepository.updateUserPreferences(
            accessToken = token,
            scoreFormat = scoreFormat,
        )
        saveViewerSettings(viewer)
        return viewer
    }

    suspend fun cachedLibrary(titleLanguage: AnilistTitleLanguage): CachedLibraryData {
        val media = container.database.mediaDao().cachedMedia().associateBy { it.id }
        val items = container.database.listEntryDao().cachedEntries()
            .mapNotNull { entry ->
                media[entry.mediaId]?.toModel(titleLanguage)?.let { cachedMedia ->
                    LibraryItem(cachedMedia, entry.toModel())
                }
            }
            .distinctBy { it.media.id }
            .sortedByTitle()
        return CachedLibraryData(
            items = items,
            syncedAtEpochMillis = container.settingsStore.librarySyncedAtEpochMillis(),
        )
    }

    suspend fun syncLibrary(token: String): SyncedLibraryData {
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
        saveViewerSettings(viewer)
        container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
        return SyncedLibraryData(
            viewer = viewer,
            items = preferredEntries.map { (media, entry) -> LibraryItem(media, entry) }.sortedByTitle(),
            syncedAtEpochMillis = now,
        )
    }

    suspend fun recentReadingProgressItems(
        titleLanguage: AnilistTitleLanguage,
        limit: Int,
    ): List<RecentReadingProgress> {
        val latestProgress = container.database.progressDao().latestReadingProgress(limit)
            .map { it.toModel() }
        if (latestProgress.isEmpty()) return emptyList()
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

    suspend fun cachedMediaDetails(
        mediaId: Int,
        titleLanguage: AnilistTitleLanguage,
    ): CachedMediaDetailsData {
        val now = System.currentTimeMillis()
        val cachedMedia = container.database.mediaDao().cachedMedia(mediaId)
        val cachedEntry = container.database.listEntryDao().cachedEntry(mediaId)?.toModel()
        val cachedRecommendations = cachedRecommendations(mediaId, titleLanguage)
        val cachedMediaHasEnrichedDetails = cachedMedia?.let { it.staff.isNotEmpty() && it.tags.isNotEmpty() } ?: false
        val cachedMediaIsFresh = cachedMedia != null &&
            cachedMediaHasEnrichedDetails &&
            now - cachedMedia.fetchedAtEpochMillis <= cachePolicy.mediaDetailsTtlMillis
        val cachedRecommendationsAreFresh = container.database.recommendationDao()
            .cachedRecommendations(mediaId)
            .firstOrNull()
            ?.let { now - it.fetchedAtEpochMillis <= cachePolicy.mediaDetailsTtlMillis }
            ?: false
        return CachedMediaDetailsData(
            media = cachedMedia?.toModel(titleLanguage),
            entry = cachedEntry,
            recommendations = cachedRecommendations,
            recommendationsPage = cachedRecommendations.recommendationPageCount(),
            recommendationsHasMore = cachedRecommendations.size >= RECOMMENDATIONS_PAGE_SIZE,
            isFresh = cachedMediaIsFresh && cachedRecommendationsAreFresh,
        )
    }

    suspend fun fetchMediaDetails(
        mediaId: Int,
        accessToken: String?,
        scoreFormat: AnilistScoreFormat,
        titleLanguage: AnilistTitleLanguage,
    ): MediaDetailsData {
        val now = System.currentTimeMillis()
        val result = container.anilistRepository.mediaDetailsWithEntry(
            mediaId = mediaId,
            accessToken = accessToken,
            scoreFormat = scoreFormat,
            recommendationsPage = 1,
            recommendationsPerPage = RECOMMENDATIONS_PAGE_SIZE,
        )
        val details = result.media.withTitleLanguage(titleLanguage)
        val recommendations = result.recommendationPage.recommendations.map { recommendation ->
            recommendation.copy(media = recommendation.media.withTitleLanguage(titleLanguage))
        }
        val listEntry = result.listEntry
        container.database.mediaDao().upsertMedia(details.toEntity(now))
        container.database.mediaDao().upsertMedia(recommendations.map { it.media.toEntity(now) })
        container.database.recommendationDao().deleteForMedia(mediaId)
        container.database.recommendationDao().upsertRecommendations(
            recommendations.map { it.toEntity(mediaId, now) },
        )
        if (listEntry != null) {
            container.database.listEntryDao().upsertEntry(listEntry.toEntity(now))
        }
        return MediaDetailsData(
            media = details,
            entry = listEntry,
            recommendations = recommendations,
            recommendationsPage = result.recommendationPage.currentPage,
            recommendationsHasMore = result.recommendationPage.hasNextPage,
        )
    }

    suspend fun fetchRecommendationPage(
        mediaId: Int,
        page: Int,
        accessToken: String?,
        titleLanguage: AnilistTitleLanguage,
    ): RecommendationPageData {
        val recommendationPage = container.anilistRepository.mediaRecommendations(
            mediaId = mediaId,
            page = page,
            perPage = RECOMMENDATIONS_PAGE_SIZE,
            accessToken = accessToken,
        )
        val now = System.currentTimeMillis()
        val recommendations = recommendationPage.recommendations.map { recommendation ->
            recommendation.copy(media = recommendation.media.withTitleLanguage(titleLanguage))
        }
        container.database.mediaDao().upsertMedia(recommendations.map { it.media.toEntity(now) })
        container.database.recommendationDao().upsertRecommendations(
            recommendations.map { it.toEntity(mediaId, now) },
        )
        return RecommendationPageData(
            recommendations = recommendations,
            currentPage = recommendationPage.currentPage,
            hasNextPage = recommendationPage.hasNextPage,
        )
    }

    suspend fun cachedRecommendations(
        mediaId: Int,
        titleLanguage: AnilistTitleLanguage,
    ): List<AnilistRecommendation> {
        val recommendationEntities = container.database.recommendationDao().cachedRecommendations(mediaId)
        if (recommendationEntities.isEmpty()) return emptyList()
        val mediaById = container.database.recommendationDao()
            .cachedRecommendationMedia(mediaId)
            .associateBy { it.id }
        return recommendationEntities.mapNotNull { recommendation ->
            mediaById[recommendation.recommendationMediaId]
                ?.toModel(titleLanguage)
                ?.let { media -> AnilistRecommendation(media = media, rating = recommendation.rating) }
        }
    }

    private fun List<LibraryItem>.sortedByTitle(): List<LibraryItem> =
        sortedBy { it.media.title.userPreferred.lowercase(Locale.ROOT) }
}
