package com.tankobun.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CatalogDao {
    @Upsert
    suspend fun queue(mutation: MangaBakaMutationEntity)
    @Query("SELECT * FROM mangabaka_mutations WHERE accountKey = :accountKey ORDER BY retryAtEpochMillis")
    suspend fun pending(accountKey: String): List<MangaBakaMutationEntity>
    @Query("DELETE FROM mangabaka_mutations WHERE accountKey = :accountKey AND mediaId = :mediaId AND revision = :revision")
    suspend fun acknowledge(accountKey: String, mediaId: Int, revision: String)
    @Query("UPDATE mangabaka_mutations SET attempts = attempts + 1, retryAtEpochMillis = :retryAt WHERE accountKey = :accountKey AND mediaId = :mediaId AND revision = :revision")
    suspend fun retry(accountKey: String, mediaId: Int, revision: String, retryAt: Long)
    @Query("SELECT * FROM catalog_identity WHERE localId = :id")
    suspend fun byLocalId(id: Int): CatalogIdentityEntity?
    @Query("SELECT * FROM catalog_identity WHERE anilistId = :id")
    suspend fun byAniList(id: Int): CatalogIdentityEntity?
    @Query("SELECT * FROM catalog_identity WHERE mangaBakaId = :id")
    suspend fun byMangaBaka(id: Int): CatalogIdentityEntity?
    @Query("SELECT EXISTS(SELECT 1 FROM anilist_list_entries WHERE mediaId = :id UNION SELECT 1 FROM source_bindings WHERE mediaId = :id UNION SELECT 1 FROM reader_progress WHERE mediaId = :id UNION SELECT 1 FROM download_jobs WHERE mediaId = :id UNION SELECT 1 FROM download_pages WHERE mediaId = :id UNION SELECT 1 FROM sync_mutations WHERE mediaId = :id UNION SELECT 1 FROM mangabaka_mutations WHERE mediaId = :id)")
    suspend fun isOwned(id: Int): Boolean
    @Query("DELETE FROM catalog_identity WHERE localId = :id")
    suspend fun removeIdentity(id: Int)
    @Query("DELETE FROM anilist_media WHERE id = :id")
    suspend fun removeUnownedMedia(id: Int)
    @Upsert
    suspend fun upsert(identity: CatalogIdentityEntity)
    @Query("SELECT * FROM catalog_pages WHERE cacheKey = :key")
    suspend fun page(key: String): CatalogPageEntity?
    @Query("SELECT * FROM catalog_pages WHERE cacheKey LIKE 'recommendations:' || :mediaId || ':%' ORDER BY CAST(substr(cacheKey, length('recommendations:' || :mediaId || ':') + 1) AS INTEGER) DESC LIMIT 1")
    suspend fun lastRecommendationPage(mediaId: Int): CatalogPageEntity?
    @Query("DELETE FROM catalog_pages WHERE cacheKey LIKE 'recommendations:' || :mediaId || ':%'")
    suspend fun clearRecommendationPages(mediaId: Int)
    @Upsert
    suspend fun upsertPage(page: CatalogPageEntity)
}
