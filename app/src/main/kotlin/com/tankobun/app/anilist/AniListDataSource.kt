package com.tankobun.app.anilist

import android.util.Log
import com.tankobun.app.AppContainer
import com.tankobun.app.logic.nullableBoolean
import com.tankobun.app.logic.nullableDouble
import com.tankobun.app.logic.nullableInt
import com.tankobun.app.logic.nullableString
import com.tankobun.app.logic.nullableStringList
import com.tankobun.app.logic.normalizedCustomLists
import com.tankobun.app.logic.RECOMMENDATIONS_PAGE_SIZE
import com.tankobun.app.logic.recommendationPageCount
import com.tankobun.app.logic.renamedCustomList
import com.tankobun.app.logic.withoutCustomList
import com.tankobun.app.state.LibraryItem
import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.core.anilist.AnilistViewer
import com.tankobun.core.database.SyncMutationEntity
import com.tankobun.core.database.toEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.CachePolicy
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.SyncMutationType
import com.tankobun.core.model.withTitleLanguage
import com.tankobun.core.sync.SyncBackoff
import com.tankobun.core.sync.SyncMutationFactory
import org.json.JSONObject
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

internal data class SyncedListEntryData(
    val media: AnilistMedia?,
    val entry: AnilistListEntry,
    val updateTrackingForm: Boolean = false,
)

internal data class SavedTrackingData(
    val knownCustomLists: List<String>,
    val entry: AnilistListEntry,
)

internal data class CustomListEntriesData(
    val customLists: List<String>,
    val updatedEntries: Map<Int, AnilistListEntry>,
)

internal class AniListDataSource(
    private val container: AppContainer,
    private val cachePolicy: CachePolicy,
) {
    private val syncMutationFactory = SyncMutationFactory()
    private val syncBackoff = SyncBackoff()

    suspend fun refreshViewer(token: String): AnilistViewer {
        val viewer = container.anilistRepository.viewer(token)
        saveViewerSettings(viewer)
        return viewer
    }

    fun saveViewerSettings(viewer: AnilistViewer) {
        container.settingsStore.saveViewerName(viewer.name)
        container.settingsStore.saveViewerAvatarUrl(viewer.avatarUrl)
        container.settingsStore.saveViewerBannerImageUrl(viewer.bannerImageUrl)
        container.settingsStore.saveAnilistMangaStats(viewer.mangaStats)
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

    suspend fun updateCustomLists(token: String, customLists: List<String>): List<String> {
        val normalizedLists = customLists.normalizedCustomLists()
        val savedLists = container.anilistRepository.updateMangaCustomLists(token, normalizedLists)
            .ifEmpty { normalizedLists }
            .normalizedCustomLists()
        container.settingsStore.saveAnilistCustomLists(savedLists)
        return savedLists
    }

    suspend fun renameCustomListEntries(
        token: String,
        items: List<LibraryItem>,
        oldName: String,
        newName: String,
        nextCustomLists: List<String>,
        scoreFormat: AnilistScoreFormat,
    ): CustomListEntriesData {
        val savedLists = updateCustomLists(token, nextCustomLists)
        val affectedItems = items.filter { item ->
            item.entry.customLists.any { it.equals(oldName, ignoreCase = true) }
        }
        val updatedEntries = affectedItems.associate { item ->
            val updatedCustomLists = item.entry.customLists.renamedCustomList(oldName, newName)
            item.media.id to saveListEntry(
                token = token,
                mediaId = item.media.id,
                status = null,
                progress = null,
                score = null,
                notes = null,
                private = null,
                customLists = updatedCustomLists,
                scoreFormat = scoreFormat,
            )
        }
        return CustomListEntriesData(
            customLists = savedLists,
            updatedEntries = updatedEntries,
        )
    }

    suspend fun deleteCustomListEntries(
        token: String,
        items: List<LibraryItem>,
        listName: String,
        nextCustomLists: List<String>,
        scoreFormat: AnilistScoreFormat,
    ): CustomListEntriesData {
        val savedLists = updateCustomLists(token, nextCustomLists)
        val affectedItems = items.filter { item ->
            item.entry.customLists.any { it.equals(listName, ignoreCase = true) }
        }
        val updatedEntries = affectedItems.associate { item ->
            val updatedCustomLists = item.entry.customLists.withoutCustomList(listName)
            item.media.id to saveListEntry(
                token = token,
                mediaId = item.media.id,
                status = null,
                progress = null,
                score = null,
                notes = null,
                private = null,
                customLists = updatedCustomLists,
                scoreFormat = scoreFormat,
            )
        }
        return CustomListEntriesData(
            customLists = savedLists,
            updatedEntries = updatedEntries,
        )
    }

    suspend fun saveLocalTracking(
        media: AnilistMedia,
        status: MediaStatus,
        progress: Int?,
        score: Double?,
        notes: String?,
        private: Boolean,
        customLists: List<String>,
        knownCustomLists: List<String>,
    ): SavedTrackingData {
        val now = System.currentTimeMillis()
        val normalizedCustomLists = customLists.normalizedCustomLists()
        val nextKnownCustomLists = (knownCustomLists + normalizedCustomLists).normalizedCustomLists()
        val existing = container.database.listEntryDao().cachedEntry(media.id)?.toModel()
        val entry = AnilistListEntry(
            id = existing?.id ?: -media.id,
            mediaId = media.id,
            status = status,
            progress = progress ?: existing?.progress ?: 0,
            score = score,
            notes = notes,
            private = private,
            customLists = normalizedCustomLists,
            updatedAtEpochSeconds = now / 1000L,
        )
        container.database.mediaDao().upsertMedia(media.toEntity(now))
        container.database.listEntryDao().upsertEntry(entry.toEntity(now))
        container.settingsStore.saveAnilistCustomLists(nextKnownCustomLists)
        container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
        return SavedTrackingData(
            knownCustomLists = nextKnownCustomLists,
            entry = entry,
        )
    }

    suspend fun saveLocalProgressFromChapter(
        media: AnilistMedia,
        chapterProgress: Int,
    ): SyncedListEntryData? {
        val now = System.currentTimeMillis()
        val existing = container.database.listEntryDao().cachedEntry(media.id)?.toModel()
        if (existing != null && chapterProgress <= existing.progress) return null
        val entry = AnilistListEntry(
            id = existing?.id ?: -media.id,
            mediaId = media.id,
            status = existing?.status ?: MediaStatus.CURRENT,
            progress = chapterProgress,
            score = existing?.score,
            notes = existing?.notes,
            private = existing?.private ?: false,
            customLists = existing?.customLists.orEmpty(),
            updatedAtEpochSeconds = now / 1000L,
        )
        container.database.mediaDao().upsertMedia(media.toEntity(now))
        container.database.listEntryDao().upsertEntry(entry.toEntity(now))
        container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
        return SyncedListEntryData(media = media, entry = entry)
    }

    fun saveLocalCustomLists(customLists: List<String>): List<String> {
        val normalizedLists = customLists.normalizedCustomLists()
        container.settingsStore.saveAnilistCustomLists(normalizedLists)
        return normalizedLists
    }

    suspend fun renameLocalCustomListEntries(
        items: List<LibraryItem>,
        oldName: String,
        newName: String,
        nextCustomLists: List<String>,
    ): CustomListEntriesData {
        val savedLists = saveLocalCustomLists(nextCustomLists)
        val now = System.currentTimeMillis()
        val updatedEntries = items
            .filter { item -> item.entry.customLists.any { it.equals(oldName, ignoreCase = true) } }
            .associate { item ->
                val entry = item.entry.copy(
                    customLists = item.entry.customLists.renamedCustomList(oldName, newName),
                    updatedAtEpochSeconds = now / 1000L,
                )
                container.database.listEntryDao().upsertEntry(entry.toEntity(now))
                item.media.id to entry
            }
        return CustomListEntriesData(customLists = savedLists, updatedEntries = updatedEntries)
    }

    suspend fun deleteLocalCustomListEntries(
        items: List<LibraryItem>,
        listName: String,
        nextCustomLists: List<String>,
    ): CustomListEntriesData {
        val savedLists = saveLocalCustomLists(nextCustomLists)
        val now = System.currentTimeMillis()
        val updatedEntries = items
            .filter { item -> item.entry.customLists.any { it.equals(listName, ignoreCase = true) } }
            .associate { item ->
                val entry = item.entry.copy(
                    customLists = item.entry.customLists.withoutCustomList(listName),
                    updatedAtEpochSeconds = now / 1000L,
                )
                container.database.listEntryDao().upsertEntry(entry.toEntity(now))
                item.media.id to entry
            }
        return CustomListEntriesData(customLists = savedLists, updatedEntries = updatedEntries)
    }

    suspend fun saveTracking(
        token: String,
        media: AnilistMedia,
        status: MediaStatus?,
        progress: Int?,
        score: Double?,
        notes: String?,
        private: Boolean?,
        customLists: List<String>,
        knownCustomLists: List<String>,
        scoreFormat: AnilistScoreFormat,
    ): SavedTrackingData {
        val normalizedCustomLists = customLists.normalizedCustomLists()
        val normalizedKnownCustomLists = knownCustomLists.normalizedCustomLists()
        val missingCustomLists = normalizedCustomLists.filterNot { selectedList ->
            normalizedKnownCustomLists.any { knownList -> knownList.equals(selectedList, ignoreCase = true) }
        }
        val nextKnownCustomLists = if (missingCustomLists.isEmpty()) {
            normalizedKnownCustomLists
        } else {
            updateCustomLists(
                token = token,
                customLists = (normalizedKnownCustomLists + missingCustomLists).normalizedCustomLists(),
            )
        }
        val entry = saveListEntry(
            token = token,
            mediaId = media.id,
            status = status,
            progress = progress,
            score = score,
            notes = notes,
            private = private,
            customLists = normalizedCustomLists,
            scoreFormat = scoreFormat,
        )
        val now = System.currentTimeMillis()
        container.database.mediaDao().upsertMedia(media.toEntity(now))
        container.database.listEntryDao().upsertEntry(entry.toEntity(now))
        container.settingsStore.saveAnilistCustomLists(nextKnownCustomLists)
        return SavedTrackingData(
            knownCustomLists = nextKnownCustomLists,
            entry = entry,
        )
    }

    suspend fun processDueSyncMutations(
        token: String,
        titleLanguage: AnilistTitleLanguage,
        scoreFormat: AnilistScoreFormat,
    ): List<SyncedListEntryData> {
        val dao = container.database.syncMutationDao()
        val syncedEntries = mutableListOf<SyncedListEntryData>()
        val mutations = dao.dueMutations(System.currentTimeMillis())
        mutations.forEach { mutation ->
            runCatching {
                processSyncMutation(
                    token = token,
                    mutation = mutation,
                    titleLanguage = titleLanguage,
                    scoreFormat = scoreFormat,
                )
            }.onSuccess { synced ->
                dao.deleteMutation(mutation)
                if (synced != null) syncedEntries += synced
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
        return syncedEntries
    }

    suspend fun syncProgressFromChapter(
        media: AnilistMedia,
        chapterProgress: Int,
        token: String?,
        scoreFormat: AnilistScoreFormat,
    ): SyncedListEntryData? {
        val now = System.currentTimeMillis()
        if (token == null) {
            queueProgressMutation(media.id, chapterProgress, now)
            return null
        }

        return runCatching {
            saveListEntry(
                token = token,
                mediaId = media.id,
                status = null,
                progress = chapterProgress,
                score = null,
                notes = null,
                private = null,
                customLists = null,
                scoreFormat = scoreFormat,
            )
        }.map { entry ->
            container.database.listEntryDao().upsertEntry(entry.toEntity(now))
            SyncedListEntryData(media = media, entry = entry)
        }.onFailure { error ->
            Log.w(TAG, "AniList progress sync failed for ${media.id}", error)
            queueProgressMutation(media.id, chapterProgress, now)
        }.getOrNull()
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
        val mediaIds = preferredEntries.map { it.second.mediaId }
        if (mediaIds.isEmpty()) {
            container.database.listEntryDao().deleteAllEntries()
        } else {
            container.database.listEntryDao().deleteEntriesNotIn(mediaIds)
        }
        saveViewerSettings(viewer)
        container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
        return SyncedLibraryData(
            viewer = viewer,
            items = preferredEntries.map { (media, entry) -> LibraryItem(media, entry) }.sortedByTitle(),
            syncedAtEpochMillis = now,
        )
    }

    suspend fun mergeLocalLibraryToAniList(
        token: String,
        localItems: List<LibraryItem>,
        knownCustomLists: List<String>,
        scoreFormat: AnilistScoreFormat,
    ): SyncedLibraryData {
        val allCustomLists = (knownCustomLists + localItems.flatMap { it.entry.customLists }).normalizedCustomLists()
        if (allCustomLists != knownCustomLists.normalizedCustomLists()) {
            updateCustomLists(token = token, customLists = allCustomLists)
        }
        localItems.forEach { item ->
            val entry = item.entry
            saveTracking(
                token = token,
                media = item.media,
                status = entry.status,
                progress = entry.progress,
                score = entry.score,
                notes = entry.notes,
                private = entry.private,
                customLists = entry.customLists,
                knownCustomLists = allCustomLists,
                scoreFormat = scoreFormat,
            )
        }
        return syncLibrary(token)
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

    private suspend fun processSyncMutation(
        token: String,
        mutation: SyncMutationEntity,
        titleLanguage: AnilistTitleLanguage,
        scoreFormat: AnilistScoreFormat,
    ): SyncedListEntryData? =
        when (mutation.type) {
            SyncMutationType.SAVE_MEDIA_LIST_ENTRY -> {
                val payload = JSONObject(mutation.payloadJson)
                val entry = saveListEntry(
                    token = token,
                    mediaId = mutation.mediaId,
                    status = payload.nullableString("status")?.let { status ->
                        runCatching { MediaStatus.valueOf(status) }.getOrNull()
                    },
                    progress = payload.nullableInt("progress"),
                    score = payload.nullableDouble("score"),
                    notes = payload.nullableString("notes"),
                    private = payload.nullableBoolean("private"),
                    customLists = payload.nullableStringList("customLists"),
                    scoreFormat = scoreFormat,
                )
                val now = System.currentTimeMillis()
                val media = container.database.mediaDao()
                    .cachedMedia(mutation.mediaId)
                    ?.toModel(titleLanguage)
                container.database.listEntryDao().upsertEntry(entry.toEntity(now))
                SyncedListEntryData(media = media, entry = entry)
            }
            SyncMutationType.DELETE_MEDIA_LIST_ENTRY -> {
                Log.w(TAG, "Ignoring unsupported queued AniList delete for ${mutation.mediaId}")
                null
            }
        }

    private suspend fun saveListEntry(
        token: String,
        mediaId: Int,
        status: MediaStatus?,
        progress: Int?,
        score: Double?,
        notes: String?,
        private: Boolean?,
        customLists: List<String>?,
        scoreFormat: AnilistScoreFormat,
    ): AnilistListEntry {
        val entry = container.anilistRepository.saveListEntry(
            accessToken = token,
            mediaId = mediaId,
            status = status,
            progress = progress,
            score = score,
            notes = notes,
            private = private,
            customLists = customLists,
            scoreFormat = scoreFormat,
        )
        container.database.listEntryDao().upsertEntry(entry.toEntity(System.currentTimeMillis()))
        return entry
    }

    private suspend fun queueProgressMutation(mediaId: Int, progress: Int, nowMillis: Long) {
        val mutation = syncMutationFactory.saveMediaListEntry(
            mediaId = mediaId,
            progress = progress,
            nowMillis = nowMillis,
        )
        container.database.syncMutationDao().upsertMutation(mutation.toEntity())
    }

    private fun List<LibraryItem>.sortedByTitle(): List<LibraryItem> =
        sortedBy { it.media.title.userPreferred.lowercase(Locale.ROOT) }

    private companion object {
        private const val TAG = "TankobunAniListData"
    }
}
