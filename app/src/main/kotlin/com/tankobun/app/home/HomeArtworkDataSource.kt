package com.tankobun.app.home

import androidx.room.withTransaction
import com.tankobun.app.AppContainer
import com.tankobun.core.database.CatalogPageEntity
import com.tankobun.core.database.toEntity
import com.tankobun.core.model.*

internal const val HOME_ARTWORK_RETRY_MILLIS = 30 * 60_000L

internal fun homeArtworkNeedsRefresh(checkedAt: Long?, attemptedAt: Long?, now: Long, ttl: Long): Boolean =
    (checkedAt == null || now - checkedAt >= ttl) &&
        (attemptedAt == null || now - attemptedAt >= HOME_ARTWORK_RETRY_MILLIS)

internal class HomeArtworkDataSource(private val container: AppContainer, private val cachePolicy: CachePolicy) {
    suspend fun enrich(feed: AnilistHomeFeed, includeAdult: Boolean): AnilistHomeFeed {
        val bounded = feed.withHomeTrendingLimit()
        val now = System.currentTimeMillis()
        val dao = container.database.catalogDao()
        fun key(id: Int) = "home:artwork:v1:$id|adult=$includeAdult"
        val candidates = bounded.trending.filter { media ->
            media.mangaBakaId != null && (includeAdult || !media.isAdult) && homeArtworkNeedsRefresh(
                dao.page(key(media.id))?.fetchedAtEpochMillis,
                dao.page("${key(media.id)}:attempt")?.fetchedAtEpochMillis,
                now, cachePolicy.mediaDetailsTtlMillis,
            )
        }
        if (candidates.isEmpty()) return bounded
        val updated = container.catalog.homeArtwork(candidates, includeAdult, container.tokenStore.accessToken()).associateBy { it.id }
        container.database.withTransaction {
            container.database.mediaDao().upsertMedia(updated.values.map { it.toEntity(now) })
            candidates.forEach { media ->
                // Successful checks also cache the absence of banners. Failures retry later.
                val cacheKey = if (media.id in updated) key(media.id) else "${key(media.id)}:attempt"
                dao.upsertPage(CatalogPageEntity(cacheKey, false, now))
            }
        }
        return bounded.copy(
            trending = bounded.trending.map { updated[it.id] ?: it },
            genreHighlights = bounded.genreHighlights.map { it.copy(media = updated[it.media.id] ?: it.media) },
        )
    }
}
