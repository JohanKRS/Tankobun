package com.tankobun.core.database

import androidx.room.withTransaction
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.CatalogIdentity
import com.tankobun.core.model.withFallbackDetails

class DatabaseCatalogIdentity(private val database: TankobunDatabase) : CatalogIdentity {
    private val dao get() = database.catalogDao()
    override suspend fun localId(anilistId: Int): Int = dao.byAniList(anilistId)?.localId ?: anilistId
    override suspend fun anilistId(localId: Int): Int? =
        dao.byLocalId(localId)?.anilistId ?: localId.takeIf { it > 0 }

    override suspend fun resolve(media: AnilistMedia): AnilistMedia = database.withTransaction {
        var byAniList = media.anilistId?.let { dao.byAniList(it) }
        var additionalDetails: AnilistMedia? = null
        val byMangaBaka = media.mangaBakaId?.let { dao.byMangaBaka(it) }
        // Conflicting existing identities need explicit reconciliation; do not silently move a library.
        if (byAniList != null && byMangaBaka != null && byAniList.localId != byMangaBaka.localId) {
            if (dao.isOwned(byAniList.localId)) {
                return@withTransaction media.copy(id = byAniList.localId, mangaBakaId = byAniList.mangaBakaId)
            }
            // A cached preview may have arrived before the official crosswalk. It must not
            // force a new key on an existing MangaBaka library item.
            additionalDetails = database.mediaDao().cachedMedia(byAniList.localId)?.toModel()
            dao.removeUnownedMedia(byAniList.localId)
            dao.removeIdentity(byAniList.localId)
            byAniList = null
        }
        val existing = byMangaBaka ?: byAniList
        val localId = existing?.localId ?: media.anilistId ?: -(requireNotNull(media.mangaBakaId))
        val identity = CatalogIdentityEntity(
            localId = localId,
            anilistId = media.anilistId ?: existing?.anilistId,
            mangaBakaId = media.mangaBakaId ?: existing?.mangaBakaId,
        )
        dao.upsert(identity)
        media.copy(id = localId, anilistId = identity.anilistId, mangaBakaId = identity.mangaBakaId)
            .withFallbackDetails(additionalDetails?.copy(id = localId))
    }
}
