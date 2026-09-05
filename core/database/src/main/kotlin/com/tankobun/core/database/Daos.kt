package com.tankobun.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT id FROM anilist_media")
    suspend fun cachedMediaIds(): List<Int>

    @Query("SELECT m.* FROM anilist_media m INNER JOIN anilist_list_entries e ON e.mediaId = m.id")
    suspend fun libraryMedia(): List<AnilistMediaEntity>

    @Query("SELECT * FROM anilist_media ORDER BY titleUserPreferred COLLATE NOCASE")
    fun observeMedia(): Flow<List<AnilistMediaEntity>>

    @Query("SELECT * FROM anilist_media WHERE id = :mediaId")
    fun observeMedia(mediaId: Int): Flow<AnilistMediaEntity?>

    @Query("SELECT * FROM anilist_media WHERE id = :mediaId")
    suspend fun cachedMedia(mediaId: Int): AnilistMediaEntity?

    @Query("SELECT * FROM anilist_media ORDER BY titleUserPreferred COLLATE NOCASE")
    suspend fun cachedMedia(): List<AnilistMediaEntity>

    @Query("SELECT * FROM anilist_media WHERE id IN (:mediaIds)")
    suspend fun cachedMedia(mediaIds: List<Int>): List<AnilistMediaEntity>

    @Upsert
    suspend fun upsertMediaEntities(media: List<AnilistMediaEntity>)

    @Transaction
    suspend fun upsertMedia(media: List<AnilistMediaEntity>) {
        if (media.isEmpty()) return
        val existingById = cachedMedia(media.map(AnilistMediaEntity::id)).associateBy(AnilistMediaEntity::id)
        upsertMediaEntities(media.map { incoming -> incoming.withFallbackDetails(existingById[incoming.id]) })
    }

    @Transaction
    suspend fun upsertMedia(media: AnilistMediaEntity) {
        upsertMedia(listOf(media))
    }
}

private fun AnilistMediaEntity.withFallbackDetails(fallback: AnilistMediaEntity?): AnilistMediaEntity {
    if (fallback == null) return this
    return copy(
        idMal = idMal ?: fallback.idMal,
        titleRomaji = titleRomaji ?: fallback.titleRomaji,
        titleEnglish = titleEnglish ?: fallback.titleEnglish,
        titleNative = titleNative ?: fallback.titleNative,
        titleUserPreferred = titleUserPreferred.ifBlank { fallback.titleUserPreferred },
        description = description ?: fallback.description,
        coverImage = coverImage ?: fallback.coverImage,
        bannerImage = bannerImage ?: fallback.bannerImage,
        mainCharacterImage = mainCharacterImage ?: fallback.mainCharacterImage,
        characterImages = characterImages.ifEmpty { fallback.characterImages },
        chapters = chapters ?: fallback.chapters,
        volumes = volumes ?: fallback.volumes,
        format = format ?: fallback.format,
        countryOfOrigin = countryOfOrigin ?: fallback.countryOfOrigin,
        status = status ?: fallback.status,
        averageScore = averageScore ?: fallback.averageScore,
        popularity = popularity ?: fallback.popularity,
        startDateYear = startDateYear ?: fallback.startDateYear,
        endDateYear = endDateYear ?: fallback.endDateYear,
        siteUrl = siteUrl ?: fallback.siteUrl,
        genres = genres.ifEmpty { fallback.genres },
        synonyms = synonyms.ifEmpty { fallback.synonyms },
        staff = staff.ifEmpty { fallback.staff },
        tags = tags.ifEmpty { fallback.tags },
        updatedAtEpochSeconds = updatedAtEpochSeconds ?: fallback.updatedAtEpochSeconds,
    )
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

    @Query("DELETE FROM anilist_list_entries WHERE mediaId NOT IN (:mediaIds)")
    suspend fun deleteEntriesNotIn(mediaIds: List<Int>)

    @Query("DELETE FROM anilist_list_entries WHERE mediaId = :mediaId")
    suspend fun deleteEntryForMedia(mediaId: Int)

    @Query("DELETE FROM anilist_list_entries WHERE mediaId IN (:mediaIds)")
    suspend fun deleteEntriesForMedia(mediaIds: List<Int>)

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

    @Query("SELECT * FROM anilist_search_results WHERE query IN (:queries) ORDER BY query ASC, orderIndex ASC")
    suspend fun cachedSearchRows(queries: List<String>): List<AnilistSearchResultEntity>

    @Query("DELETE FROM anilist_search_results WHERE query = :query")
    suspend fun deleteForQuery(query: String)

    @Query("DELETE FROM anilist_search_results WHERE query IN (:queries)")
    suspend fun deleteForQueries(queries: List<String>)

    @Upsert
    suspend fun upsertResults(results: List<AnilistSearchResultEntity>)

    @Transaction
    suspend fun replaceResults(queries: List<String>, results: List<AnilistSearchResultEntity>) {
        if (queries.isEmpty()) return
        deleteForQueries(queries)
        if (results.isNotEmpty()) upsertResults(results)
    }
}

@Dao
interface SourceBindingDao {
    @Query("SELECT * FROM source_bindings WHERE mediaId = :mediaId")
    fun observeBinding(mediaId: Int): Flow<SourceBindingEntity?>

    @Query("SELECT * FROM source_bindings WHERE mediaId = :mediaId")
    suspend fun bindingForMedia(mediaId: Int): SourceBindingEntity?

    @Query("SELECT * FROM source_bindings")
    fun observeBindings(): Flow<List<SourceBindingEntity>>

    @Query("SELECT * FROM source_bindings")
    suspend fun cachedBindings(): List<SourceBindingEntity>

    @Upsert
    suspend fun upsertBinding(binding: SourceBindingEntity)

    @Query("UPDATE source_bindings SET memoJson = :memoJson WHERE sourceId = :sourceId AND mangaUrl = :mangaUrl")
    suspend fun updateMemo(sourceId: Long, mangaUrl: String, memoJson: String?)

    @Delete
    suspend fun deleteBinding(binding: SourceBindingEntity)

    @Query("DELETE FROM source_bindings WHERE mediaId = :mediaId")
    suspend fun deleteBindingForMedia(mediaId: Int)

    @Query("DELETE FROM source_bindings WHERE mediaId IN (:mediaIds)")
    suspend fun deleteBindingsForMedia(mediaIds: List<Int>)
}

@Dao
interface SourceSearchDao {
    @Query("SELECT * FROM source_search_results WHERE mediaId = :mediaId ORDER BY score DESC")
    fun observeSearchResults(mediaId: Int): Flow<List<SourceSearchResultEntity>>

    @Query("SELECT * FROM source_search_results WHERE mediaId = :mediaId ORDER BY score DESC")
    suspend fun cachedSearchResults(mediaId: Int): List<SourceSearchResultEntity>

    @Query("DELETE FROM source_search_results WHERE mediaId = :mediaId")
    suspend fun clearForMedia(mediaId: Int)

    @Query("DELETE FROM source_search_results WHERE mediaId IN (:mediaIds)")
    suspend fun clearForMedia(mediaIds: List<Int>)

    @Query("UPDATE source_search_results SET memoJson = :memoJson WHERE sourceId = :sourceId AND mangaUrl = :mangaUrl")
    suspend fun updateMemo(sourceId: Long, mangaUrl: String, memoJson: String?)

    @Upsert
    suspend fun upsertResults(results: List<SourceSearchResultEntity>)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM source_chapters WHERE sourceId = :sourceId AND mangaUrl = :mangaUrl ORDER BY chapterNumber DESC")
    fun observeChapters(sourceId: Long, mangaUrl: String): Flow<List<SourceChapterEntity>>

    @Query("SELECT * FROM source_chapters WHERE sourceId = :sourceId AND mangaUrl = :mangaUrl ORDER BY chapterNumber DESC")
    suspend fun cachedChapters(sourceId: Long, mangaUrl: String): List<SourceChapterEntity>

    @Query("SELECT * FROM source_chapters WHERE chapterUrl = :chapterUrl LIMIT 1")
    suspend fun cachedChapterByUrl(chapterUrl: String): SourceChapterEntity?

    @Query("SELECT * FROM source_chapters WHERE sourceId = :sourceId AND chapterUrl = :chapterUrl LIMIT 1")
    suspend fun cachedChapterByUrl(sourceId: Long, chapterUrl: String): SourceChapterEntity?

    @Upsert
    suspend fun upsertChapters(chapters: List<SourceChapterEntity>)

    @Query("DELETE FROM source_chapters WHERE sourceId = :sourceId AND mangaUrl = :mangaUrl")
    suspend fun deleteChaptersForSourceManga(sourceId: Long, mangaUrl: String)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM reader_progress WHERE mediaId = :mediaId ORDER BY updatedAtEpochMillis DESC, chapterNumber DESC, pageIndex DESC LIMIT 1")
    fun observeLatestProgress(mediaId: Int): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reader_progress WHERE mediaId = :mediaId ORDER BY updatedAtEpochMillis DESC, chapterNumber DESC, pageIndex DESC LIMIT 1")
    suspend fun latestProgress(mediaId: Int): ReadingProgressEntity?

    @Query("SELECT * FROM reader_progress WHERE mediaId = :mediaId")
    suspend fun progressForMedia(mediaId: Int): List<ReadingProgressEntity>

    @Query("SELECT * FROM reader_progress")
    suspend fun allProgress(): List<ReadingProgressEntity>

    @Query(
        """
        SELECT progress.* FROM reader_progress AS progress
        INNER JOIN anilist_media AS media
            ON media.id = progress.mediaId
        WHERE progress.chapterUrl = (
            SELECT latest.chapterUrl
            FROM reader_progress AS latest
            WHERE latest.mediaId = progress.mediaId
            ORDER BY latest.updatedAtEpochMillis DESC, latest.chapterNumber DESC, latest.pageIndex DESC
            LIMIT 1
        )
        ORDER BY progress.updatedAtEpochMillis DESC, progress.chapterNumber DESC, progress.pageIndex DESC
        """,
    )
    suspend fun latestReadingProgress(): List<ReadingProgressEntity>

    @Query("SELECT * FROM reader_progress WHERE mediaId = :mediaId AND chapterUrl = :chapterUrl")
    suspend fun progressForChapter(mediaId: Int, chapterUrl: String): ReadingProgressEntity?

    @Upsert
    suspend fun upsertProgress(progress: ReadingProgressEntity)

    @Query("DELETE FROM reader_progress WHERE mediaId = :mediaId AND chapterUrl = :chapterUrl")
    suspend fun deleteProgressForChapter(mediaId: Int, chapterUrl: String)

    @Query("DELETE FROM reader_progress WHERE mediaId = :mediaId")
    suspend fun deleteProgressForMedia(mediaId: Int)

    @Query("DELETE FROM reader_progress WHERE mediaId IN (:mediaIds)")
    suspend fun deleteProgressForMedia(mediaIds: List<Int>)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_jobs ORDER BY updatedAtEpochMillis DESC")
    fun observeDownloads(): Flow<List<DownloadJobEntity>>

    @Query("SELECT * FROM download_jobs WHERE id = :id")
    suspend fun getDownload(id: String): DownloadJobEntity?

    @Query("SELECT * FROM download_jobs")
    suspend fun allDownloads(): List<DownloadJobEntity>

    @Query("SELECT * FROM download_jobs WHERE mediaId = :mediaId")
    suspend fun downloadsForMedia(mediaId: Int): List<DownloadJobEntity>

    @Query("SELECT * FROM download_jobs WHERE mediaId = :mediaId AND sourceId = :sourceId")
    suspend fun downloadsForMediaSource(mediaId: Int, sourceId: Long): List<DownloadJobEntity>

    @Query("SELECT * FROM download_jobs WHERE mediaId = :mediaId AND chapterUrl = :chapterUrl ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    suspend fun latestForChapter(mediaId: Int, chapterUrl: String): DownloadJobEntity?

    @Query("SELECT * FROM download_jobs WHERE mediaId = :mediaId AND chapterUrl = :chapterUrl AND state = 'COMPLETE' ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    suspend fun completedForChapter(mediaId: Int, chapterUrl: String): DownloadJobEntity?

    @Query("SELECT * FROM download_jobs WHERE state IN ('QUEUED', 'RUNNING') ORDER BY createdAtEpochMillis ASC")
    suspend fun pendingDownloads(): List<DownloadJobEntity>

    @Upsert
    suspend fun upsertDownload(job: DownloadJobEntity)

    @Query("UPDATE download_jobs SET state = :state, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateState(id: String, state: com.tankobun.core.model.DownloadState, updatedAt: Long)

    @Query("UPDATE download_jobs SET state = 'RUNNING', updatedAtEpochMillis = :updatedAt WHERE id = :id AND state IN ('QUEUED', 'RUNNING')")
    suspend fun markRunningIfActive(id: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE download_jobs
        SET pageCount = :pageCount,
            completedPages = MAX(completedPages, :completedPages),
            updatedAtEpochMillis = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateProgress(id: String, completedPages: Int, pageCount: Int, updatedAt: Long)

    @Query(
        """
        UPDATE download_jobs
        SET state = 'COMPLETE',
            pageCount = :pageCount,
            completedPages = :pageCount,
            updatedAtEpochMillis = :updatedAt
        WHERE id = :id AND state IN ('QUEUED', 'RUNNING')
        """,
    )
    suspend fun markComplete(id: String, pageCount: Int, updatedAt: Long)

    @Query(
        """
        UPDATE download_jobs
        SET state = 'FAILED',
            retryCount = retryCount + 1,
            updatedAtEpochMillis = :updatedAt
        WHERE id = :id AND state IN ('QUEUED', 'RUNNING')
        """,
    )
    suspend fun markFailed(id: String, updatedAt: Long)

    @Query("DELETE FROM download_jobs WHERE id = :id")
    suspend fun deleteDownload(id: String)

    @Query("DELETE FROM download_jobs WHERE mediaId = :mediaId")
    suspend fun deleteDownloadsForMedia(mediaId: Int)

    @Query("DELETE FROM download_jobs WHERE mediaId = :mediaId AND sourceId = :sourceId")
    suspend fun deleteDownloadsForMediaSource(mediaId: Int, sourceId: Long)

    @Query("DELETE FROM download_jobs")
    suspend fun deleteAllDownloads()
}

@Dao
interface DownloadPageDao {
    @Query("SELECT * FROM download_pages")
    suspend fun allPages(): List<DownloadPageEntity>

    @Query("SELECT * FROM download_pages WHERE mediaId = :mediaId AND chapterUrl = :chapterUrl ORDER BY pageIndex ASC")
    suspend fun pagesForChapter(mediaId: Int, chapterUrl: String): List<DownloadPageEntity>

    @Query("SELECT * FROM download_pages WHERE jobId = :jobId ORDER BY pageIndex ASC")
    suspend fun pagesForJob(jobId: String): List<DownloadPageEntity>

    @Query("SELECT * FROM download_pages WHERE mediaId = :mediaId ORDER BY chapterUrl ASC, pageIndex ASC")
    suspend fun pagesForMedia(mediaId: Int): List<DownloadPageEntity>

    @Query("SELECT * FROM download_pages WHERE mediaId = :mediaId AND sourceId = :sourceId ORDER BY chapterUrl ASC, pageIndex ASC")
    suspend fun pagesForMediaSource(mediaId: Int, sourceId: Long): List<DownloadPageEntity>

    @Upsert
    suspend fun upsertPage(page: DownloadPageEntity)

    @Query("DELETE FROM download_pages WHERE jobId = :jobId AND pageIndex NOT IN (:indexes)")
    suspend fun deletePagesOutsideIndexes(jobId: String, indexes: List<Int>)

    @Query("DELETE FROM download_pages WHERE jobId = :jobId")
    suspend fun deletePagesForJob(jobId: String)

    @Query("DELETE FROM download_pages WHERE mediaId = :mediaId")
    suspend fun deletePagesForMedia(mediaId: Int)

    @Query("DELETE FROM download_pages WHERE mediaId = :mediaId AND sourceId = :sourceId")
    suspend fun deletePagesForMediaSource(mediaId: Int, sourceId: Long)

    @Query("DELETE FROM download_pages")
    suspend fun deleteAllPages()
}

@Dao
interface SyncMutationDao {
    @Query("SELECT * FROM sync_mutations ORDER BY createdAtEpochMillis ASC")
    suspend fun pendingMutations(): List<SyncMutationEntity>

    @Query("SELECT * FROM sync_mutations WHERE nextAttemptAtEpochMillis <= :nowMillis ORDER BY createdAtEpochMillis ASC")
    suspend fun dueMutations(nowMillis: Long): List<SyncMutationEntity>

    @Upsert
    suspend fun upsertMutation(mutation: SyncMutationEntity)

    @Delete
    suspend fun deleteMutation(mutation: SyncMutationEntity)
}
