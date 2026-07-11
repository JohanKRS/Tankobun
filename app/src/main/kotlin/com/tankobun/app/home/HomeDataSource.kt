package com.tankobun.app.home

import com.tankobun.app.AppContainer
import com.tankobun.core.database.AnilistSearchResultEntity
import com.tankobun.core.database.toEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.model.AnilistGenreHighlight
import com.tankobun.core.model.AnilistHomeFeed
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.CachePolicy

internal class HomeDataSource(
    private val container: AppContainer,
    private val cachePolicy: CachePolicy,
    private val titleLanguage: () -> AnilistTitleLanguage,
) {
    suspend fun cachedHomeFeed(
        genres: List<String>,
        includeAdult: Boolean,
        freshOnly: Boolean,
    ): AnilistHomeFeed? {
        val cachedAt = container.settingsStore.homeFeedCachedAtEpochMillis(includeAdult)
        if (cachedAt <= 0L) return null
        if (freshOnly && System.currentTimeMillis() - cachedAt > cachePolicy.homeFeedTtlMillis) return null

        val trending = cachedMedia(trendingKey(includeAdult), includeAdult)
        if (trending.isEmpty()) return null
        val highlights = genres.mapNotNull { genre ->
            cachedMedia(genreKey(genre, includeAdult), includeAdult)
                .firstOrNull()
                ?.let { media -> AnilistGenreHighlight(genre = genre, media = media) }
        }
        if (highlights.size != genres.size) return null
        return AnilistHomeFeed(trending = trending, genreHighlights = highlights)
    }

    suspend fun saveHomeFeed(feed: AnilistHomeFeed, includeAdult: Boolean) {
        val now = System.currentTimeMillis()
        val media = (feed.trending + feed.genreHighlights.map { it.media }).distinctBy(AnilistMedia::id)
        container.database.mediaDao().upsertMedia(media.map { item -> item.toEntity(now) })
        replaceResults(trendingKey(includeAdult), feed.trending, now)
        feed.genreHighlights.forEach { highlight ->
            replaceResults(genreKey(highlight.genre, includeAdult), listOf(highlight.media), now)
        }
        container.settingsStore.saveHomeFeedCachedAtEpochMillis(includeAdult, now)
    }

    private suspend fun cachedMedia(key: String, includeAdult: Boolean): List<AnilistMedia> =
        container.database.searchResultDao()
            .cachedSearchMedia(key)
            .map { entity -> entity.toModel(titleLanguage()) }
            .filter { media -> includeAdult || !media.isAdult }

    private suspend fun replaceResults(key: String, media: List<AnilistMedia>, now: Long) {
        container.database.searchResultDao().deleteForQuery(key)
        container.database.searchResultDao().upsertResults(
            media.mapIndexed { index, item ->
                AnilistSearchResultEntity(
                    query = key,
                    mediaId = item.id,
                    orderIndex = index,
                    fetchedAtEpochMillis = now,
                )
            },
        )
    }

    private fun trendingKey(includeAdult: Boolean): String =
        "home:v2:${adultCacheSegment(includeAdult)}:trending"

    private fun genreKey(genre: String, includeAdult: Boolean): String =
        "home:v2:${adultCacheSegment(includeAdult)}:genre:$genre"

    private fun adultCacheSegment(includeAdult: Boolean): String = if (includeAdult) "nsfw" else "safe"
}
