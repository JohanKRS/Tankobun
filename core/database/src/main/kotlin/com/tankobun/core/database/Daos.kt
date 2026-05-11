package com.tankobun.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM anilist_media ORDER BY titleUserPreferred COLLATE NOCASE")
    fun observeMedia(): Flow<List<AnilistMediaEntity>>

    @Query("SELECT * FROM anilist_media WHERE id = :mediaId")
    fun observeMedia(mediaId: Int): Flow<AnilistMediaEntity?>

    @Query("SELECT * FROM anilist_media WHERE id = :mediaId")
    suspend fun cachedMedia(mediaId: Int): AnilistMediaEntity?

    @Query("SELECT * FROM anilist_media ORDER BY titleUserPreferred COLLATE NOCASE")
    suspend fun cachedMedia(): List<AnilistMediaEntity>

    @Upsert
    suspend fun upsertMedia(media: List<AnilistMediaEntity>)

    @Upsert
    suspend fun upsertMedia(media: AnilistMediaEntity)
}

@Dao
interface ListEntryDao {
    @Query("SELECT * FROM anilist_list_entries")
    fun observeEntries(): Flow<List<AnilistListEntryEntity>>

    @Query("SELECT * FROM anilist_list_entries WHERE mediaId = :mediaId")
    fun observeEntry(mediaId: Int): Flow<AnilistListEntryEntity?>

    @Query("SELECT * FROM anilist_list_entries ORDER BY fetchedAtEpochMillis DESC")
    suspend fun cachedEntries(): List<AnilistListEntryEntity>

    @Query("SELECT * FROM anilist_list_entries WHERE mediaId = :mediaId")
    suspend fun cachedEntry(mediaId: Int): AnilistListEntryEntity?

    @Upsert
    suspend fun upsertEntries(entries: List<AnilistListEntryEntity>)

    @Upsert
    suspend fun upsertEntry(entry: AnilistListEntryEntity)

    @Query("DELETE FROM anilist_list_entries WHERE id NOT IN (:ids)")
    suspend fun deleteEntriesNotIn(ids: List<Int>)

    @Query("DELETE FROM anilist_list_entries")
    suspend fun deleteAllEntries()
}

@Dao
interface RecommendationDao {
    @Query(
        """
        SELECT media.* FROM anilist_recommendations AS rec
        INNER JOIN anilist_media AS media ON media.id = rec.recommendationMediaId
        WHERE rec.mediaId = :mediaId
        ORDER BY COALESCE(rec.rating, 0) DESC
        """,
    )
    suspend fun cachedRecommendationMedia(mediaId: Int): List<AnilistMediaEntity>

    @Query("SELECT * FROM anilist_recommendations WHERE mediaId = :mediaId ORDER BY COALESCE(rating, 0) DESC")
    suspend fun cachedRecommendations(mediaId: Int): List<AnilistRecommendationEntity>

    @Query("DELETE FROM anilist_recommendations WHERE mediaId = :mediaId")
    suspend fun deleteForMedia(mediaId: Int)

    @Upsert
    suspend fun upsertRecommendations(recommendations: List<AnilistRecommendationEntity>)
}

@Dao
interface SearchResultDao {
    @Query(
        """
        SELECT media.* FROM anilist_search_results AS result
        INNER JOIN anilist_media AS media ON media.id = result.mediaId
        WHERE result.query = :query
        ORDER BY result.orderIndex ASC
        """,
    )
    suspend fun cachedSearchMedia(query: String): List<AnilistMediaEntity>

    @Query("SELECT * FROM anilist_search_results WHERE query = :query ORDER BY orderIndex ASC")
    suspend fun cachedSearchRows(query: String): List<AnilistSearchResultEntity>

    @Query("DELETE FROM anilist_search_results WHERE query = :query")
    suspend fun deleteForQuery(query: String)

    @Upsert
    suspend fun upsertResults(results: List<AnilistSearchResultEntity>)
}

@Dao
interface SourceBindingDao {
    @Query("SELECT * FROM source_bindings WHERE mediaId = :mediaId")
    fun observeBinding(mediaId: Int): Flow<SourceBindingEntity?>

    @Query("SELECT * FROM source_bindings WHERE mediaId = :mediaId")
    suspend fun bindingForMedia(mediaId: Int): SourceBindingEntity?

    @Query("SELECT * FROM source_bindings")
    fun observeBindings(): Flow<List<SourceBindingEntity>>

    @Upsert
    suspend fun upsertBinding(binding: SourceBindingEntity)

    @Delete
    suspend fun deleteBinding(binding: SourceBindingEntity)
}

@Dao
interface SourceSearchDao {
    @Query("SELECT * FROM source_search_results WHERE mediaId = :mediaId ORDER BY score DESC")
    fun observeSearchResults(mediaId: Int): Flow<List<SourceSearchResultEntity>>

    @Query("SELECT * FROM source_search_results WHERE mediaId = :mediaId ORDER BY score DESC")
    suspend fun cachedSearchResults(mediaId: Int): List<SourceSearchResultEntity>

    @Query("DELETE FROM source_search_results WHERE mediaId = :mediaId")
    suspend fun clearForMedia(mediaId: Int)

    @Upsert
    suspend fun upsertResults(results: List<SourceSearchResultEntity>)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM source_chapters WHERE sourceId = :sourceId AND mangaUrl = :mangaUrl ORDER BY chapterNumber DESC")
    fun observeChapters(sourceId: Long, mangaUrl: String): Flow<List<SourceChapterEntity>>

    @Query("SELECT * FROM source_chapters WHERE sourceId = :sourceId AND mangaUrl = :mangaUrl ORDER BY chapterNumber DESC")
    suspend fun cachedChapters(sourceId: Long, mangaUrl: String): List<SourceChapterEntity>

    @Upsert
    suspend fun upsertChapters(chapters: List<SourceChapterEntity>)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM reader_progress WHERE mediaId = :mediaId ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    fun observeLatestProgress(mediaId: Int): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reader_progress WHERE mediaId = :mediaId ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    suspend fun latestProgress(mediaId: Int): ReadingProgressEntity?

    @Query("SELECT * FROM reader_progress WHERE mediaId = :mediaId AND chapterUrl = :chapterUrl")
    suspend fun progressForChapter(mediaId: Int, chapterUrl: String): ReadingProgressEntity?

    @Upsert
    suspend fun upsertProgress(progress: ReadingProgressEntity)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_jobs ORDER BY updatedAtEpochMillis DESC")
    fun observeDownloads(): Flow<List<DownloadJobEntity>>

    @Query("SELECT * FROM download_jobs WHERE id = :id")
    suspend fun getDownload(id: String): DownloadJobEntity?

    @Upsert
    suspend fun upsertDownload(job: DownloadJobEntity)

    @Query("UPDATE download_jobs SET state = :state, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateState(id: String, state: com.tankobun.core.model.DownloadState, updatedAt: Long)
}

@Dao
interface SyncMutationDao {
    @Query("SELECT * FROM sync_mutations WHERE nextAttemptAtEpochMillis <= :nowMillis ORDER BY createdAtEpochMillis ASC")
    suspend fun dueMutations(nowMillis: Long): List<SyncMutationEntity>

    @Upsert
    suspend fun upsertMutation(mutation: SyncMutationEntity)

    @Delete
    suspend fun deleteMutation(mutation: SyncMutationEntity)
}
