package com.tankobun.app.catalog

import com.tankobun.app.AppContainer
import com.tankobun.app.home.withHomeTrendingLimit
import com.tankobun.core.anilist.AnilistBrowseLanding
import com.tankobun.core.database.toModel
import com.tankobun.core.model.*
import kotlinx.coroutines.*

/** AniList supplies the presentation when available; MangaBaka supplies missing works and fallback. */
internal class CatalogDataSource(private val container: AppContainer) {
    @Volatile private var aniListRetryAt = 0L

    private suspend fun <T> aniList(block: suspend () -> T): T? {
        if (System.currentTimeMillis() < aniListRetryAt) return null
        return try { withTimeoutOrNull(10_000L) { block() }.also { if (it == null) aniListRetryAt = System.currentTimeMillis() + 60_000L } }
        catch (error: Exception) { if (error is CancellationException) throw error; aniListRetryAt = System.currentTimeMillis() + 60_000L; null }
    }

    suspend fun search(primary: (suspend () -> AnilistMediaPage)?, secondary: suspend () -> AnilistMediaPage, page: Int): AnilistMediaPage = coroutineScope {
        val extra = async { optional { secondary() } }
        val main = primary?.let { aniList { it() } }
        val additional = try { withTimeoutOrNull(if (main != null) 4_000L else 12_000L) { extra.await() } } finally { extra.cancel() }
        check(main != null || additional != null) { "Catalogs unavailable" }
        val combined = LinkedHashMap<Int, AnilistMedia>()
        main?.media.orEmpty().forEach { combined[it.id] = it }
        additional?.media.orEmpty().forEach { media ->
            val existing = combined[media.id] ?: container.database.mediaDao().cachedMedia(media.id)?.toModel()
            combined[media.id] = if (existing?.anilistId != null) existing.withFallbackDetails(media) else media.withFallbackDetails(existing)
        }
        AnilistMediaPage(combined.values.toList(), page, main?.hasNextPage == true || additional?.hasNextPage == true)
    }

    suspend fun details(mediaId: Int, accessToken: String?, scoreFormat: AnilistScoreFormat): AnilistMediaDetails = coroutineScope {
        val cached = container.database.mediaDao().cachedMedia(mediaId)?.toModel()
        val remoteId = container.catalogIdentity.anilistId(mediaId)
        val extra = async {
            optional {
                val linked = cached?.mangaBakaId ?: container.database.catalogDao().byLocalId(mediaId)?.mangaBakaId
                    ?: remoteId?.let { container.mangaBakaRepository.findByAniList(it)?.mangaBakaId }
                linked?.let { container.mangaBakaRepository.details(it, container.settingsStore.showNsfwContent()) }
            }
        }
        val primary = remoteId?.let { aniList { container.anilistRepository.mediaDetailsWithEntry(mediaId, accessToken, scoreFormat) } }
        val fallback = try { withTimeoutOrNull(if (primary != null) 3_000L else 12_000L) { extra.await() } } finally { extra.cancel() }
        val media = (primary?.media ?: fallback ?: error("Title unavailable"))
            .withFallbackDetails(fallback).withFallbackDetails(cached)
        val entry = primary?.listEntry ?: container.database.listEntryDao().cachedEntry(media.id)?.toModel()
        val recommendations = recommendations(media.id, 1, accessToken, primary?.recommendationPage)
        return@coroutineScope AnilistMediaDetails(media, entry, recommendations)
    }

    suspend fun recommendations(mediaId: Int, page: Int, accessToken: String?, preloaded: AnilistRecommendationPage? = null): AnilistRecommendationPage = coroutineScope {
        val supplemental = async {
            if (page != 1) return@async emptyList<AnilistRecommendation>()
            optional {
                val mb = container.database.catalogDao().byLocalId(mediaId)?.mangaBakaId
                    ?: container.catalogIdentity.anilistId(mediaId)?.let { container.mangaBakaRepository.findByAniList(it)?.mangaBakaId }
                    ?: return@optional emptyList<AnilistRecommendation>()
                val adult = container.settingsStore.showNsfwContent()
                val similar = async { optional { container.mangaBakaRepository.recommendations(mb, 1, adult).recommendations }.orEmpty() }
                val mixed = async { optional { container.mangaBakaRepository.mix(listOf(mb) + mixSeeds(resolveMissing = false).take(2), adult, 24) }.orEmpty() }
                val owned = container.database.listEntryDao().cachedEntries().mapTo(hashSetOf()) { it.mediaId }
                similar.await() + mixed.await().filter { it.id !in owned }.map { AnilistRecommendation(it, null) }
            }
        }
        val main = preloaded ?: if (container.catalogIdentity.anilistId(mediaId) != null) {
            aniList { container.anilistRepository.mediaRecommendations(mediaId, page, accessToken = accessToken) }
        } else null
        if (page > 1 && main == null) error("Recommendations unavailable")
        val extra = try { withTimeoutOrNull(if (main != null) 2_000L else 12_000L) { supplemental.await() } } finally { supplemental.cancel() }
        mergeRecommendations(mediaId, page, main, extra.orEmpty(), container.settingsStore.showNsfwContent())
    }

    suspend fun mixSeeds(resolveMissing: Boolean = true): List<Int> {
        val eligible = container.database.listEntryDao().cachedEntries()
            .filter { it.status in setOf(MediaStatus.CURRENT, MediaStatus.REPEATING, MediaStatus.COMPLETED) }
            .sortedWith(compareByDescending<com.tankobun.core.database.AnilistListEntryEntity> { it.status == MediaStatus.CURRENT || it.status == MediaStatus.REPEATING }
                .thenByDescending { it.score ?: 0.0 }.thenByDescending { it.updatedAtEpochSeconds ?: 0L }).take(3)
        return eligible.mapNotNull { entry ->
            container.database.catalogDao().byLocalId(entry.mediaId)?.mangaBakaId ?: if (resolveMissing) {
                container.catalogIdentity.anilistId(entry.mediaId)?.let { id -> optional { container.mangaBakaRepository.findByAniList(id)?.mangaBakaId } }
            } else null
        }.distinct()
    }

    suspend fun homeFeed(genres: List<String>, accessToken: String?, includeAdult: Boolean,
        onTrendingLoaded: (List<AnilistMedia>) -> Unit = {}, onGenreHighlightsLoaded: (List<AnilistGenreHighlight>) -> Unit = {}): AnilistHomeFeed {
        aniList { container.anilistRepository.homeFeed(genres, accessToken, includeAdult, onTrendingLoaded, onGenreHighlightsLoaded) }?.let { return it }
        val media = container.mangaBakaRepository.search(sort = "trending_7d", includeAdult = includeAdult).media
        val highlights = genres.mapNotNull { genre -> media.firstOrNull { item -> item.genres.any { it.equals(genre, ignoreCase = true) } }?.let { AnilistGenreHighlight(genre, it) } }
        val feed = AnilistHomeFeed(media, highlights).withHomeTrendingLimit()
        onTrendingLoaded(feed.trending)
        onGenreHighlightsLoaded(highlights)
        return feed
    }

    suspend fun browseLanding(perPage: Int, accessToken: String?, includeAdult: Boolean): AnilistBrowseLanding {
        aniList { container.anilistRepository.browseLanding(perPage, accessToken, includeAdult) }?.let { return it }
        return coroutineScope {
            val trending = async { container.mangaBakaRepository.search(sort = "trending_7d", limit = perPage, includeAdult = includeAdult).media }
            val popular = async { container.mangaBakaRepository.search(sort = "popularity_asc", limit = perPage, includeAdult = includeAdult).media }
            val manhwa = async { container.mangaBakaRepository.search(sort = "popularity_asc", country = "KR", limit = perPage, includeAdult = includeAdult).media }
            val top = async { container.mangaBakaRepository.search(sort = "score_desc", limit = perPage, includeAdult = includeAdult).media }
            AnilistBrowseLanding(trending.await(), popular.await(), manhwa.await(), top.await())
        }
    }
}

internal suspend fun <T> optional(block: suspend () -> T): T? = try { withTimeoutOrNull(12_000L) { block() } }
    catch (error: Exception) { if (error is CancellationException) throw error; android.util.Log.w("TankobunCatalog", "Optional catalog request failed", error); null }

internal fun mergeRecommendations(mediaId: Int, page: Int, primary: AnilistRecommendationPage?, supplemental: List<AnilistRecommendation>, includeAdult: Boolean): AnilistRecommendationPage {
    val combined = LinkedHashMap<Int, AnilistRecommendation>()
    (primary?.recommendations.orEmpty() + supplemental).filter { it.media.id != mediaId && (includeAdult || !it.media.isAdult) }.forEach { rec ->
        val earlier = combined[rec.media.id]
        combined[rec.media.id] = earlier?.copy(media = earlier.media.withFallbackDetails(rec.media)) ?: rec
    }
    return AnilistRecommendationPage(combined.values.toList(), page, primary?.hasNextPage == true)
}
