package com.tankobun.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

// These records support user-owned state even when a title is no longer in the list.
private const val UNOWNED_MEDIA = """
    id NOT IN (SELECT mediaId FROM anilist_list_entries
        UNION SELECT mediaId FROM source_bindings
        UNION SELECT mediaId FROM reader_progress
        UNION SELECT mediaId FROM download_jobs
        UNION SELECT mediaId FROM download_pages
        UNION SELECT mediaId FROM sync_mutations)
"""
private const val UNBOUND_CHAPTER = """
    NOT EXISTS (SELECT 1 FROM source_bindings b WHERE b.sourceId = source_chapters.sourceId AND b.mangaUrl = source_chapters.mangaUrl)
    AND NOT EXISTS (SELECT 1 FROM download_jobs d WHERE d.sourceId = source_chapters.sourceId AND d.mangaUrl = source_chapters.mangaUrl)
    AND NOT EXISTS (SELECT 1 FROM reader_progress p WHERE p.chapterUrl = source_chapters.chapterUrl)
"""

@Dao
interface NavigationCacheDao {
    @Query("DELETE FROM anilist_search_results WHERE query IN (SELECT query FROM anilist_search_results GROUP BY query HAVING MAX(fetchedAtEpochMillis) < :before)")
    suspend fun deleteOldSearches(before: Long)

    @Query("DELETE FROM anilist_recommendations WHERE mediaId IN (SELECT mediaId FROM anilist_recommendations GROUP BY mediaId HAVING MAX(fetchedAtEpochMillis) < :before)")
    suspend fun deleteOldRecommendations(before: Long)

    @Query("DELETE FROM source_search_results WHERE searchedAtEpochMillis < :before")
    suspend fun deleteOldSourceSearches(before: Long)

    @Query("DELETE FROM source_chapters WHERE fetchedAtEpochMillis < :before AND " + UNBOUND_CHAPTER)
    suspend fun deleteOldUnboundChapters(before: Long)

    @Query("""DELETE FROM anilist_media WHERE fetchedAtEpochMillis < :before AND """ + UNOWNED_MEDIA + """
        AND NOT EXISTS (SELECT 1 FROM source_search_results s WHERE s.mediaId = anilist_media.id)
        AND NOT EXISTS (SELECT 1 FROM anilist_search_results s WHERE s.mediaId = anilist_media.id)
        AND NOT EXISTS (SELECT 1 FROM anilist_recommendations r WHERE r.mediaId = anilist_media.id OR r.recommendationMediaId = anilist_media.id)
    """)
    suspend fun deleteOldUnownedMedia(before: Long)

    @Query("SELECT (SELECT COUNT(*) FROM anilist_media WHERE " + UNOWNED_MEDIA + ") + (SELECT COUNT(*) FROM anilist_search_results) + (SELECT COUNT(*) FROM anilist_recommendations) + (SELECT COUNT(*) FROM source_search_results) + (SELECT COUNT(*) FROM source_chapters WHERE " + UNBOUND_CHAPTER + ")")
    suspend fun recordCount(): Long

    @Transaction
    suspend fun prune(before: Long) {
        deleteOldSearches(before)
        deleteOldRecommendations(before)
        deleteOldSourceSearches(before)
        deleteOldUnboundChapters(before)
        deleteOldUnownedMedia(before)
    }
}
