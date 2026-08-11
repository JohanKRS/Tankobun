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

        val trendingKey = trendingKey(includeAdult)
        val genreKeys = genres.associateWith { genre -> genreKey(genre, includeAdult) }
        val keys = listOf(trendingKey) + genreKeys.values
        val rows = container.database.searchResultDao().cachedSearchRows(keys)
        val mediaById = container.database.mediaDao()
            .cachedMedia(rows.map(AnilistSearchResultEntity::mediaId).distinct())
            .associateBy { it.id }
        val mediaByKey = rows.groupBy(AnilistSearchResultEntity::query)
            .mapValues { (_, keyRows) ->
                keyRows.mapNotNull { row -> mediaById[row.mediaId] }
                    .map { entity -> entity.toModel(titleLanguage()) }
                    .filter { media -> includeAdult || !media.isAdult }
            }
        val trending = mediaByKey[trendingKey].orEmpty()
        if (trending.isEmpty()) return null
        val highlights = genres.mapNotNull { genre ->
            mediaByKey[genreKeys.getValue(genre)]
                .orEmpty()
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
        val resultsByKey = buildMap {
            put(trendingKey(includeAdult), feed.trending)
            feed.genreHighlights.forEach { highlight ->
                put(genreKey(highlight.genre, includeAdult), listOf(highlight.media))
            }
        }
        container.database.searchResultDao().replaceResults(
            queries = resultsByKey.keys.toList(),
            results = resultsByKey.flatMap { (key, items) ->
                items.mapIndexed { index, item ->
                    AnilistSearchResultEntity(
                        query = key,
                        mediaId = item.id,
                        orderIndex = index,
                        fetchedAtEpochMillis = now,
                    )
                }
            },
        )
        container.settingsStore.saveHomeFeedCachedAtEpochMillis(includeAdult, now)
    }

    private fun trendingKey(includeAdult: Boolean): String =
        "home:v5:${adultCacheSegment(includeAdult)}:trending"

    private fun genreKey(genre: String, includeAdult: Boolean): String =
        "home:v5:${adultCacheSegment(includeAdult)}:genre:$genre"

    private fun adultCacheSegment(includeAdult: Boolean): String = if (includeAdult) "nsfw" else "safe"
}
