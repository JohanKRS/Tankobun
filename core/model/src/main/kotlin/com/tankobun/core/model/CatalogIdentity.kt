package com.tankobun.core.model

/** Translates provider IDs at the network boundary while keeping reading/source keys stable. */
interface CatalogIdentity {
    suspend fun resolve(media: AnilistMedia): AnilistMedia
    suspend fun localId(anilistId: Int): Int
    suspend fun anilistId(localId: Int): Int?

    object Default : CatalogIdentity {
        override suspend fun resolve(media: AnilistMedia) = media
        override suspend fun localId(anilistId: Int) = anilistId
        override suspend fun anilistId(localId: Int) = localId.takeIf { it > 0 }
    }
}
