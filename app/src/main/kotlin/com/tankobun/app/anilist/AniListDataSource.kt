package com.tankobun.app.anilist

import android.util.Log
import com.tankobun.app.AppContainer
import com.tankobun.app.logic.isInReadingCategory
import com.tankobun.app.logic.nullableBoolean
import com.tankobun.app.logic.nullableDouble
import com.tankobun.app.logic.nullableInt
import com.tankobun.app.logic.nullableString
import com.tankobun.app.logic.nullableStringList
import com.tankobun.app.logic.normalizedCustomLists
import com.tankobun.app.logic.RECOMMENDATIONS_PAGE_SIZE
import com.tankobun.app.logic.recommendationPageCount
import com.tankobun.app.logic.recentReadingMetrics
import com.tankobun.app.logic.renamedCustomList
import com.tankobun.app.logic.shouldShowInContinueReading
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

internal data class LibraryBatchMutationData(
    val customLists: List<String>,
    val updatedItems: List<LibraryItem>,
    val removedMediaIds: Set<Int> = emptySet(),
    val queuedRemoteUpdates: Int = 0,
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
                hiddenFromStatusLists = item.entry.hiddenFromStatusLists,
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
                hiddenFromStatusLists = item.entry.hiddenFromStatusLists,
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
        hiddenFromStatusLists: Boolean? = null,
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
            hiddenFromStatusLists = hiddenFromStatusLists ?: existing?.hiddenFromStatusLists ?: false,
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
        status: MediaStatus? = null,
    ): SyncedListEntryData? {
        val now = System.currentTimeMillis()
        val existing = container.database.listEntryDao().cachedEntry(media.id)?.toModel()
        val nextProgress = maxOf(chapterProgress.coerceAtLeast(0), existing?.progress ?: 0)
        val nextStatus = status ?: existing?.status ?: MediaStatus.CURRENT
        val nextHiddenFromStatusLists = if (status != null) false else existing?.hiddenFromStatusLists ?: false
        if (
            existing != null &&
            nextProgress == existing.progress &&
            nextStatus == existing.status &&
            nextHiddenFromStatusLists == existing.hiddenFromStatusLists
        ) {
            return null
        }
        val entry = AnilistListEntry(
            id = existing?.id ?: -media.id,
            mediaId = media.id,
            status = nextStatus,
            progress = nextProgress,
            score = existing?.score,
            notes = existing?.notes,
            private = existing?.private ?: false,
            customLists = existing?.customLists.orEmpty(),
            updatedAtEpochSeconds = now / 1000L,
            hiddenFromStatusLists = nextHiddenFromStatusLists,
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

    suspend fun importRecommendationList(
        token: String?,
        syncRemote: Boolean,
        media: List<AnilistMedia>,
        listName: String,
        knownCustomLists: List<String>,
        existingItems: List<LibraryItem>,
        scoreFormat: AnilistScoreFormat,
    ): LibraryBatchMutationData {
        val normalizedListName = listName.trim().ifBlank { DEFAULT_RECOMMENDATION_LIST_NAME }
        val requestedList = listOf(normalizedListName).normalizedCustomLists().first()
        val nextKnownCustomLists = (knownCustomLists + requestedList).normalizedCustomLists()
        val savedLists = saveCustomListsForBatch(token, syncRemote, nextKnownCustomLists)
        val existingById = existingItems.associateBy { it.media.id }
        val now = System.currentTimeMillis()
        var queued = 0
        val updatedItems = media.distinctBy { it.id }.map { recommendation ->
            val existing = existingById[recommendation.id]
            val entry = if (existing == null) {
                AnilistListEntry(
                    id = -recommendation.id,
                    mediaId = recommendation.id,
                    status = MediaStatus.PLANNING,
                    progress = 0,
                    score = null,
                    notes = null,
                    private = false,
                    customLists = listOf(requestedList),
                    updatedAtEpochSeconds = now / 1000L,
                    hiddenFromStatusLists = true,
                )
            } else {
                existing.entry.copy(
                    customLists = (existing.entry.customLists + requestedList).normalizedCustomLists(),
                    updatedAtEpochSeconds = now / 1000L,
                )
            }
            container.database.mediaDao().upsertMedia(recommendation.toEntity(now))
            container.database.listEntryDao().upsertEntry(entry.toEntity(now))
            val syncedEntry = syncOrQueueListEntry(
                token = token.takeIf { syncRemote },
                syncRemote = syncRemote,
                mediaId = recommendation.id,
                status = if (existing == null) entry.status else null,
                progress = if (existing == null) entry.progress else null,
                score = if (existing == null) entry.score else null,
                notes = if (existing == null) entry.notes else null,
                private = if (existing == null) entry.private else null,
                customLists = entry.customLists,
                hiddenFromStatusLists = entry.hiddenFromStatusLists,
                scoreFormat = scoreFormat,
                fallbackEntry = entry,
                nowMillis = now,
            )
            if (syncedEntry.queued) queued += 1
            LibraryItem(recommendation, syncedEntry.entry)
        }
        container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
        return LibraryBatchMutationData(
            customLists = savedLists,
            updatedItems = updatedItems,
            queuedRemoteUpdates = queued,
        )
    }

    suspend fun updateBatchStatus(
        token: String?,
        syncRemote: Boolean,
        items: List<LibraryItem>,
        status: MediaStatus,
        scoreFormat: AnilistScoreFormat,
    ): LibraryBatchMutationData {
        val now = System.currentTimeMillis()
        var queued = 0
        val updatedItems = items.map { item ->
            val entry = item.entry.copy(
                status = status,
                hiddenFromStatusLists = false,
                updatedAtEpochSeconds = now / 1000L,
            )
            container.database.mediaDao().upsertMedia(item.media.toEntity(now))
            container.database.listEntryDao().upsertEntry(entry.toEntity(now))
            val syncedEntry = syncOrQueueListEntry(
                token = token.takeIf { syncRemote },
                syncRemote = syncRemote,
                mediaId = item.media.id,
                status = status,
                progress = null,
                score = null,
                notes = null,
                private = null,
                customLists = item.entry.customLists,
                hiddenFromStatusLists = false,
                scoreFormat = scoreFormat,
                fallbackEntry = entry,
                nowMillis = now,
            )
            if (syncedEntry.queued) queued += 1
            item.copy(entry = syncedEntry.entry)
        }
        container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
        return LibraryBatchMutationData(
            customLists = container.settingsStore.anilistCustomLists(),
            updatedItems = updatedItems,
            queuedRemoteUpdates = queued,
        )
    }

    suspend fun addBatchCustomList(
        token: String?,
        syncRemote: Boolean,
        items: List<LibraryItem>,
        listName: String,
        knownCustomLists: List<String>,
        scoreFormat: AnilistScoreFormat,
    ): LibraryBatchMutationData {
        val requestedList = listOf(listName.trim()).normalizedCustomLists().firstOrNull()
            ?: return LibraryBatchMutationData(customLists = knownCustomLists, updatedItems = emptyList())
        val savedLists = saveCustomListsForBatch(token, syncRemote, (knownCustomLists + requestedList).normalizedCustomLists())
        val now = System.currentTimeMillis()
        var queued = 0
        val updatedItems = items.map { item ->
            val entry = item.entry.copy(
                customLists = (item.entry.customLists + requestedList).normalizedCustomLists(),
                updatedAtEpochSeconds = now / 1000L,
            )
            container.database.mediaDao().upsertMedia(item.media.toEntity(now))
            container.database.listEntryDao().upsertEntry(entry.toEntity(now))
            val syncedEntry = syncOrQueueListEntry(
                token = token.takeIf { syncRemote },
                syncRemote = syncRemote,
                mediaId = item.media.id,
                status = null,
                progress = null,
                score = null,
                notes = null,
                private = null,
                customLists = entry.customLists,
                hiddenFromStatusLists = entry.hiddenFromStatusLists,
                scoreFormat = scoreFormat,
                fallbackEntry = entry,
                nowMillis = now,
            )
            if (syncedEntry.queued) queued += 1
            item.copy(entry = syncedEntry.entry)
        }
        container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
        return LibraryBatchMutationData(
            customLists = savedLists,
            updatedItems = updatedItems,
            queuedRemoteUpdates = queued,
        )
    }

    suspend fun removeBatchCustomList(
        token: String?,
        syncRemote: Boolean,
        items: List<LibraryItem>,
        listName: String,
        scoreFormat: AnilistScoreFormat,
    ): LibraryBatchMutationData {
        val now = System.currentTimeMillis()
        var queued = 0
        val updatedItems = mutableListOf<LibraryItem>()
        val removedMediaIds = mutableSetOf<Int>()
        items.forEach { item ->
            val nextLists = item.entry.customLists.withoutCustomList(listName)
            if (item.entry.hiddenFromStatusLists && nextLists.isEmpty()) {
                val deleteQueued = deleteListEntryRemoteOrQueue(
                    token = token.takeIf { syncRemote },
                    syncRemote = syncRemote,
                    entry = item.entry,
                    nowMillis = now,
                )
                if (deleteQueued) queued += 1
                container.database.listEntryDao().deleteEntryForMedia(item.media.id)
                removedMediaIds += item.media.id
            } else {
                val entry = item.entry.copy(
                    customLists = nextLists,
                    updatedAtEpochSeconds = now / 1000L,
                )
                container.database.listEntryDao().upsertEntry(entry.toEntity(now))
                val syncedEntry = syncOrQueueListEntry(
                    token = token.takeIf { syncRemote },
                    syncRemote = syncRemote,
                    mediaId = item.media.id,
                    status = null,
                    progress = null,
                    score = null,
                    notes = null,
                    private = null,
                    customLists = entry.customLists,
                    hiddenFromStatusLists = entry.hiddenFromStatusLists,
                    scoreFormat = scoreFormat,
                    fallbackEntry = entry,
                    nowMillis = now,
                )
                if (syncedEntry.queued) queued += 1
                updatedItems += item.copy(entry = syncedEntry.entry)
            }
        }
        container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
        return LibraryBatchMutationData(
            customLists = container.settingsStore.anilistCustomLists(),
            updatedItems = updatedItems,
            removedMediaIds = removedMediaIds,
            queuedRemoteUpdates = queued,
        )
    }

    suspend fun deleteBatchEntries(
        token: String?,
        syncRemote: Boolean,
        items: List<LibraryItem>,
        deleteLocalData: Boolean,
    ): LibraryBatchMutationData {
        val now = System.currentTimeMillis()
        var queued = 0
        items.forEach { item ->
            val deleteQueued = deleteListEntryRemoteOrQueue(
                token = token.takeIf { syncRemote },
                syncRemote = syncRemote,
                entry = item.entry,
                nowMillis = now,
            )
            if (deleteQueued) queued += 1
        }
        deleteLocalEntries(
            mediaIds = items.map { it.media.id },
            deleteLocalData = deleteLocalData,
        )
        container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
        return LibraryBatchMutationData(
            customLists = container.settingsStore.anilistCustomLists(),
            updatedItems = emptyList(),
            removedMediaIds = items.mapTo(mutableSetOf()) { it.media.id },
            queuedRemoteUpdates = queued,
        )
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
        hiddenFromStatusLists: Boolean? = null,
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
            hiddenFromStatusLists = hiddenFromStatusLists,
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
        status: MediaStatus? = null,
        token: String?,
        scoreFormat: AnilistScoreFormat,
    ): SyncedListEntryData? {
        val now = System.currentTimeMillis()
        val progress = chapterProgress.takeIf { it > 0 }
        if (progress == null && status == null) return null
        if (token == null) {
            queueProgressMutation(media.id, progress, status, now)
            return null
        }

        return runCatching {
            saveListEntry(
                token = token,
                mediaId = media.id,
                status = status,
                progress = progress,
                score = null,
                notes = null,
                private = null,
                customLists = null,
                hiddenFromStatusLists = status?.let { false },
                scoreFormat = scoreFormat,
            )
        }.map { entry ->
            container.database.listEntryDao().upsertEntry(entry.toEntity(now))
            SyncedListEntryData(media = media, entry = entry)
        }.onFailure { error ->
            Log.w(TAG, "AniList progress sync failed for ${media.id}", error)
            queueProgressMutation(media.id, progress, status, now)
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
                hiddenFromStatusLists = entry.hiddenFromStatusLists,
            )
        }
        return syncLibrary(token)
    }

    suspend fun recentReadingProgressItems(
        titleLanguage: AnilistTitleLanguage,
        limit: Int,
    ): List<RecentReadingProgress> {
        val latestProgress = container.database.progressDao().latestReadingProgress()
            .map { it.toModel() }
        if (latestProgress.isEmpty()) return emptyList()
        val readingEntriesByMediaId = container.database.listEntryDao()
            .cachedEntries()
            .asSequence()
            .map { it.toModel() }
            .filter { it.isInReadingCategory() }
            .associateBy { it.mediaId }
        if (readingEntriesByMediaId.isEmpty()) return emptyList()
        val mediaById = container.database.mediaDao().cachedMedia().associateBy { it.id }
        val chapterDao = container.database.chapterDao()
        val bindingDao = container.database.sourceBindingDao()
        return buildList {
            for (progress in latestProgress) {
                val entry = readingEntriesByMediaId[progress.mediaId] ?: continue
                val media = mediaById[progress.mediaId]?.toModel(titleLanguage) ?: continue
                val binding = bindingDao.bindingForMedia(progress.mediaId)
                val boundChapters = binding?.let { selected ->
                    chapterDao.cachedChapters(selected.sourceId, selected.mangaUrl).map { it.toModel() }
                }.orEmpty()
                val boundChapter = boundChapters.firstOrNull { it.url == progress.chapterUrl }
                val cachedChapter = boundChapter
                    ?: chapterDao.cachedChapterByUrl(progress.chapterUrl)?.toModel()
                val availableChapters = when {
                    boundChapter != null -> boundChapters
                    cachedChapter != null -> chapterDao
                        .cachedChapters(cachedChapter.sourceId, cachedChapter.mangaUrl)
                        .map { it.toModel() }
                    else -> boundChapters
                }
                val metrics = recentReadingMetrics(progress, cachedChapter, availableChapters)
                if (!media.shouldShowInContinueReading(entry, progress, metrics)) continue
                add(
                    RecentReadingProgress(
                        media = media,
                        progress = progress,
                        chapter = cachedChapter,
                        currentChapterNumber = metrics.currentChapterNumber,
                        lastAvailableChapterNumber = metrics.lastAvailableChapterNumber,
                        overallProgress = metrics.overallProgress,
                    ),
                )
                if (size >= limit) break
            }
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

    suspend fun refreshListEntry(
        mediaId: Int,
        accessToken: String,
        scoreFormat: AnilistScoreFormat,
    ): AnilistListEntry? {
        val listEntry = container.anilistRepository.mediaListEntry(
            mediaId = mediaId,
            accessToken = accessToken,
            scoreFormat = scoreFormat,
        )
        if (listEntry == null) {
            container.database.listEntryDao().deleteEntryForMedia(mediaId)
        } else {
            container.database.listEntryDao().upsertEntry(listEntry.toEntity(System.currentTimeMillis()))
        }
        return listEntry
    }

    suspend fun enrichRecommendationMedia(
        media: List<AnilistMedia>,
        accessToken: String?,
        titleLanguage: AnilistTitleLanguage,
    ): List<AnilistMedia> {
        if (media.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val enriched = media.distinctBy { it.id }.map { original ->
            runCatching {
                container.anilistRepository.mangaById(original.id, accessToken)
                    ?.withTitleLanguage(titleLanguage)
            }.onFailure { error ->
                Log.w(TAG, "Failed to enrich recommendation media ${original.id}", error)
            }.getOrNull() ?: original.withTitleLanguage(titleLanguage)
        }
        container.database.mediaDao().upsertMedia(enriched.map { it.toEntity(now) })
        return enriched
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
                val customLists = payload.nullableStringList("customLists")
                if (!customLists.isNullOrEmpty()) {
                    val knownCustomLists = container.settingsStore.anilistCustomLists()
                    val nextCustomLists = (knownCustomLists + customLists).normalizedCustomLists()
                    if (nextCustomLists != knownCustomLists.normalizedCustomLists()) {
                        updateCustomLists(token, nextCustomLists)
                    }
                }
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
                    customLists = customLists,
                    hiddenFromStatusLists = payload.nullableBoolean("hiddenFromStatusLists"),
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
                val payload = JSONObject(mutation.payloadJson)
                val entryId = payload.optInt("entryId", 0)
                if (entryId > 0) {
                    container.anilistRepository.deleteListEntry(token, entryId)
                }
                container.database.listEntryDao().deleteEntryForMedia(mutation.mediaId)
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
        hiddenFromStatusLists: Boolean?,
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
            hiddenFromStatusLists = hiddenFromStatusLists,
            scoreFormat = scoreFormat,
        )
        container.database.listEntryDao().upsertEntry(entry.toEntity(System.currentTimeMillis()))
        return entry
    }

    private suspend fun queueProgressMutation(
        mediaId: Int,
        progress: Int?,
        status: MediaStatus?,
        nowMillis: Long,
    ) {
        val mutation = syncMutationFactory.saveMediaListEntry(
            mediaId = mediaId,
            status = status,
            progress = progress,
            hiddenFromStatusLists = status?.let { false },
            nowMillis = nowMillis,
        )
        container.database.syncMutationDao().upsertMutation(mutation.toEntity())
    }

    private suspend fun saveCustomListsForBatch(
        token: String?,
        syncRemote: Boolean,
        nextCustomLists: List<String>,
    ): List<String> =
        if (syncRemote && token != null) {
            runCatching { updateCustomLists(token, nextCustomLists) }
                .onFailure { error -> Log.w(TAG, "AniList custom list update failed; using local list", error) }
                .getOrElse { saveLocalCustomLists(nextCustomLists) }
        } else {
            saveLocalCustomLists(nextCustomLists)
        }

    private suspend fun syncOrQueueListEntry(
        token: String?,
        syncRemote: Boolean,
        mediaId: Int,
        status: MediaStatus?,
        progress: Int?,
        score: Double?,
        notes: String?,
        private: Boolean?,
        customLists: List<String>?,
        hiddenFromStatusLists: Boolean?,
        scoreFormat: AnilistScoreFormat,
        fallbackEntry: AnilistListEntry,
        nowMillis: Long,
    ): SyncedOrQueuedEntry {
        if (!syncRemote) return SyncedOrQueuedEntry(entry = fallbackEntry, queued = false)
        if (token != null) {
            runCatching {
                saveListEntry(
                    token = token,
                    mediaId = mediaId,
                    status = status,
                    progress = progress,
                    score = score,
                    notes = notes,
                    private = private,
                    customLists = customLists,
                    hiddenFromStatusLists = hiddenFromStatusLists,
                    scoreFormat = scoreFormat,
                )
            }.onSuccess { entry ->
                return SyncedOrQueuedEntry(entry = entry, queued = false)
            }.onFailure { error ->
                Log.w(TAG, "AniList batch save failed for $mediaId; queued", error)
            }
        }
        container.database.syncMutationDao().upsertMutation(
            syncMutationFactory.saveMediaListEntry(
                mediaId = mediaId,
                status = status,
                progress = progress,
                score = score,
                notes = notes,
                private = private,
                customLists = customLists,
                hiddenFromStatusLists = hiddenFromStatusLists,
                nowMillis = nowMillis,
            ).toEntity(),
        )
        return SyncedOrQueuedEntry(entry = fallbackEntry, queued = true)
    }

    private suspend fun deleteListEntryRemoteOrQueue(
        token: String?,
        syncRemote: Boolean,
        entry: AnilistListEntry,
        nowMillis: Long,
    ): Boolean {
        if (!syncRemote || entry.id <= 0) return false
        if (token != null) {
            runCatching {
                check(container.anilistRepository.deleteListEntry(token, entry.id)) {
                    "AniList delete did not confirm"
                }
            }.onSuccess {
                return false
            }.onFailure { error ->
                Log.w(TAG, "AniList batch delete failed for ${entry.mediaId}; queued", error)
            }
        }
        container.database.syncMutationDao().upsertMutation(
            syncMutationFactory.deleteMediaListEntry(
                mediaId = entry.mediaId,
                entryId = entry.id,
                nowMillis = nowMillis,
            ).toEntity(),
        )
        return true
    }

    private suspend fun deleteLocalEntries(
        mediaIds: List<Int>,
        deleteLocalData: Boolean,
    ) {
        if (mediaIds.isEmpty()) return
        val bindings = if (deleteLocalData) {
            container.database.sourceBindingDao().cachedBindings().filter { it.mediaId in mediaIds }
        } else {
            emptyList()
        }
        container.database.listEntryDao().deleteEntriesForMedia(mediaIds)
        if (!deleteLocalData) return
        bindings.forEach { binding ->
            container.database.chapterDao().deleteChaptersForSourceManga(binding.sourceId, binding.mangaUrl)
        }
        container.database.sourceBindingDao().deleteBindingsForMedia(mediaIds)
        container.database.sourceSearchDao().clearForMedia(mediaIds)
        container.database.progressDao().deleteProgressForMedia(mediaIds)
        mediaIds.forEach { mediaId -> container.downloadCoordinator.removeMedia(mediaId) }
    }

    private fun List<LibraryItem>.sortedByTitle(): List<LibraryItem> =
        sortedBy { it.media.title.userPreferred.lowercase(Locale.ROOT) }

    private companion object {
        private const val TAG = "TankobunAniListData"
        private const val DEFAULT_RECOMMENDATION_LIST_NAME = "Tankobun recommendations"
    }
}

private data class SyncedOrQueuedEntry(
    val entry: AnilistListEntry,
    val queued: Boolean,
)
