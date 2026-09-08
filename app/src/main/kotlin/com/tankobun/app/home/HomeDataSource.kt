package com.tankobun.app.home

import androidx.room.withTransaction
import com.tankobun.app.AppContainer
import com.tankobun.core.database.AnilistSearchResultEntity
import com.tankobun.core.database.toEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.model.AnilistGenreHighlight
import com.tankobun.core.model.AnilistHomeFeed
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.CatalogMode
import com.tankobun.core.model.CachePolicy

internal const val HOME_TRENDING_LIMIT = 5

internal fun AnilistHomeFeed.withHomeTrendingLimit(): AnilistHomeFeed =
    copy(
        trending = trending.distinctBy(AnilistMedia::id).take(HOME_TRENDING_LIMIT),
        genreHighlights = genreHighlights.distinctBy { it.media.id },
    )

internal class HomeDataSource(
    private val container: AppContainer,
    private val cachePolicy: CachePolicy,
    private val titleLanguage: () -> AnilistTitleLanguage,
) {
    suspend fun cachedHomeFeed(
        genres: List<String>,
        includeAdult: Boolean,
        freshOnly: Boolean,
        mode: CatalogMode,
    ): AnilistHomeFeed? {
        val cachedAt = container.settingsStore.homeFeedCachedAtEpochMillis(includeAdult, mode)
        if (cachedAt <= 0L) return null
        if (freshOnly && System.currentTimeMillis() - cachedAt > cachePolicy.homeFeedTtlMillis) return null

        val trendingKey = trendingKey(includeAdult, mode)
        val genreKeys = genres.associateWith { genre -> genreKey(genre, includeAdult, mode) }
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
        val trending = mediaByKey[trendingKey].orEmpty().take(HOME_TRENDING_LIMIT)
        if (trending.isEmpty()) return null
        val highlights = genres.mapNotNull { genre ->
            mediaByKey[genreKeys.getValue(genre)]
                .orEmpty()
                .firstOrNull()
                ?.let { media -> AnilistGenreHighlight(genre = genre, media = media) }
        }
        // The fallback can cover only some genres. Its fresh cache is still usable;
        // missing categories must not trigger the same request on every visit.
        return AnilistHomeFeed(trending = trending, genreHighlights = highlights).withHomeTrendingLimit()
    }

    suspend fun saveHomeFeed(feed: AnilistHomeFeed, genres: List<String>, includeAdult: Boolean, mode: CatalogMode) {
        val now = System.currentTimeMillis()
        val boundedFeed = feed.withHomeTrendingLimit()
        val media = (boundedFeed.trending + boundedFeed.genreHighlights.map { it.media }).distinctBy(AnilistMedia::id)
        container.database.withTransaction {
            container.database.mediaDao().upsertMedia(media.map { item -> item.toEntity(now) })
            val resultsByKey = buildMap {
                put(trendingKey(includeAdult, mode), boundedFeed.trending)
                genres.forEach { put(genreKey(it, includeAdult, mode), emptyList()) }
                boundedFeed.genreHighlights.forEach { highlight ->
                    put(genreKey(highlight.genre, includeAdult, mode), listOf(highlight.media))
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
        }
        container.settingsStore.saveHomeFeedCachedAtEpochMillis(includeAdult, now, mode)
    }

    private fun trendingKey(includeAdult: Boolean, mode: CatalogMode): String =
        "home:v7:${mode.name}:${adultCacheSegment(includeAdult)}:trending"

    private fun genreKey(genre: String, includeAdult: Boolean, mode: CatalogMode): String =
        "home:v7:${mode.name}:${adultCacheSegment(includeAdult)}:genre:$genre"

    private fun adultCacheSegment(includeAdult: Boolean): String = if (includeAdult) "nsfw" else "safe"
}
