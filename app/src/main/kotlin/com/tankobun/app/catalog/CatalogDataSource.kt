package com.tankobun.app.catalog

import com.tankobun.app.AppContainer
import com.tankobun.app.home.withHomeTrendingLimit
import com.tankobun.core.anilist.AnilistBrowseLanding
import com.tankobun.core.database.toModel
import com.tankobun.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** AniList and Combined navigation may call AniList. */
internal val CatalogMode.usesAniList: Boolean get() = this != CatalogMode.MANGABAKA

/** MangaBaka and Combined navigation may call MangaBaka. */
internal val CatalogMode.usesMangaBaka: Boolean get() = this != CatalogMode.ANILIST

/**
 * Navigation uses only the selected catalog; Combined uses both. A work that only the other
 * catalog knows still opens from it, so library entries never become unreachable.
 */
internal class CatalogDataSource(private val container: AppContainer) {
    @Volatile private var aniListRetryAt = 0L

    /**
     * With a fallback (Combined), a slow AniList is cut short and skipped for a minute. Alone it
     * gets time to answer: its requests can queue behind startup syncs in the shared rate limiter.
     */
    private suspend fun <T> aniList(withFallback: Boolean = true, block: suspend () -> T): T? {
        if (withFallback && System.currentTimeMillis() < aniListRetryAt) return null
        val timeoutMillis = if (withFallback) 10_000L else 30_000L
        return try {
            withTimeoutOrNull(timeoutMillis) { block() }.also {
                if (it == null && withFallback) aniListRetryAt = System.currentTimeMillis() + 60_000L
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (withFallback) aniListRetryAt = System.currentTimeMillis() + 60_000L
            null
        }
    }

    suspend fun search(
        primary: (suspend () -> AnilistMediaPage)?,
        secondary: suspend () -> AnilistMediaPage,
        page: Int,
        mode: CatalogMode,
    ): AnilistMediaPage {
        val (main, additional) = navigationCatalogs(mode,
            aniList = { if (mode.usesAniList) primary?.let { aniList(withFallback = mode == CatalogMode.COMBINED) { it() } } else null },
            // AniList mode still asks MangaBaka when AniList cannot express the query at all.
            mangaBaka = { if (mode.usesMangaBaka || primary == null) optional { secondary() } else null },
            supplementWaitMillis = if (mode == CatalogMode.COMBINED) 12_000L else 4_000L,
            hasContent = { it.media.isNotEmpty() },
        )
        check(main != null || additional != null) { "Catalogs unavailable" }
        return AnilistMediaPage(
            mergeCatalogMedia(main?.media.orEmpty(), withCachedPresentation(additional?.media.orEmpty()), mode),
            page, main?.hasNextPage == true || additional?.hasNextPage == true,
        )
    }

    private suspend fun withCachedPresentation(media: List<AnilistMedia>): List<AnilistMedia> {
        val cached = container.database.mediaDao().cachedMedia(media.map { it.id }).associateBy { it.id }
        return media.map { item ->
            val existing = cached[item.id]?.toModel()
            if (existing?.anilistId != null) existing.withFallbackDetails(item) else item.withFallbackDetails(existing)
        }
    }

    suspend fun details(mediaId: Int, accessToken: String?, scoreFormat: AnilistScoreFormat): AnilistMediaDetails = coroutineScope {
        val mode = container.settingsStore.catalogMode()
        val cached = container.database.mediaDao().cachedMedia(mediaId)?.toModel()
        val remoteId = container.catalogIdentity.anilistId(mediaId)
        suspend fun fromMangaBaka(): AnilistMedia? = optional {
            val linked = cached?.mangaBakaId ?: container.database.catalogDao().byLocalId(mediaId)?.mangaBakaId
                ?: remoteId?.let { container.mangaBakaRepository.findByAniList(it)?.mangaBakaId }
            linked?.let { container.mangaBakaRepository.details(it, container.settingsStore.showNsfwContent()) }
        }
        suspend fun fromAniList(withFallback: Boolean) = remoteId?.let {
            aniList(withFallback) { container.anilistRepository.mediaDetailsWithEntry(mediaId, accessToken, scoreFormat) }
        }
        val (primary, supplement) = when (mode) {
            CatalogMode.COMBINED -> {
                val extra = async { fromMangaBaka() }
                val primary = fromAniList(withFallback = true)
                val fallback = try { withTimeoutOrNull(if (primary != null) 3_000L else 12_000L) { extra.await() } } finally { extra.cancel() }
                primary to fallback
            }
            // MangaBaka is only asked about works AniList does not know.
            CatalogMode.ANILIST -> {
                val primary = fromAniList(withFallback = false)
                primary to if (primary == null && remoteId == null) fromMangaBaka() else null
            }
            // AniList is only asked about works MangaBaka cannot match.
            CatalogMode.MANGABAKA -> {
                val supplement = fromMangaBaka()
                (if (supplement == null) fromAniList(withFallback = false) else null) to supplement
            }
        }
        val media = (primary?.media ?: supplement ?: cached ?: error("Title unavailable"))
            .withFallbackDetails(supplement).withFallbackDetails(cached)
        val entry = primary?.listEntry ?: container.database.listEntryDao().cachedEntry(media.id)?.toModel()
        val recommendations = recommendations(media.id, 1, accessToken, primary?.recommendationPage)
        return@coroutineScope AnilistMediaDetails(media, entry, recommendations)
    }

    suspend fun recommendations(mediaId: Int, page: Int, accessToken: String?, preloaded: AnilistRecommendationPage? = null): AnilistRecommendationPage = coroutineScope {
        val mode = container.settingsStore.catalogMode()
        val adult = container.settingsStore.showNsfwContent()
        val anilistId = container.catalogIdentity.anilistId(mediaId)
        suspend fun mangaBakaId(): Int? = container.database.catalogDao().byLocalId(mediaId)?.mangaBakaId
            ?: anilistId?.let { optional { container.mangaBakaRepository.findByAniList(it)?.mangaBakaId } }
        suspend fun fromMangaBaka(mb: Int): List<AnilistRecommendation> = coroutineScope {
            val similar = async { optional { container.mangaBakaRepository.recommendations(mb, 1, adult).recommendations }.orEmpty() }
            val mixed = async { optional { container.mangaBakaRepository.mix(listOf(mb) + mixSeeds(resolveMissing = false).take(2), adult, 24) }.orEmpty() }
            val owned = container.database.listEntryDao().cachedEntries().mapTo(hashSetOf()) { it.mediaId }
            similar.await() + mixed.await().filter { it.id !in owned }.map { AnilistRecommendation(it, null) }
        }
        suspend fun fromAniList(withFallback: Boolean) = preloaded ?: anilistId?.let {
            aniList(withFallback) { container.anilistRepository.mediaRecommendations(mediaId, page, accessToken = accessToken) }
        }
        when (mode) {
            CatalogMode.COMBINED -> {
                val supplemental = async {
                    if (page != 1) return@async emptyList<AnilistRecommendation>()
                    optional { mangaBakaId()?.let { fromMangaBaka(it) }.orEmpty() }
                }
                val main = fromAniList(withFallback = true)
                if (page > 1 && main == null) error("Recommendations unavailable")
                val extra = try { withTimeoutOrNull(if (main != null) 2_000L else 12_000L) { supplemental.await() } } finally { supplemental.cancel() }
                mergeRecommendations(mediaId, page, main, extra.orEmpty(), adult)
            }
            CatalogMode.ANILIST -> if (anilistId != null) {
                val main = fromAniList(withFallback = false) ?: error("Recommendations unavailable")
                mergeRecommendations(mediaId, page, main, emptyList(), adult)
            } else {
                // A MangaBaka-only work has no AniList suggestions to show.
                val extra = if (page == 1) optional { mangaBakaId()?.let { fromMangaBaka(it) } }.orEmpty() else emptyList()
                mergeRecommendations(mediaId, page, null, extra, adult)
            }
            CatalogMode.MANGABAKA -> {
                val mb = if (page == 1) mangaBakaId() else null
                if (mb != null) {
                    mergeRecommendations(mediaId, page, null, optional { fromMangaBaka(mb) }.orEmpty(), adult)
                } else {
                    // Only works MangaBaka cannot match fall back to AniList suggestions.
                    val main = fromAniList(withFallback = false) ?: error("Recommendations unavailable")
                    mergeRecommendations(mediaId, page, main, emptyList(), adult)
                }
            }
        }
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

    suspend fun homeArtwork(media: List<AnilistMedia>, includeAdult: Boolean, accessToken: String?, mode: CatalogMode): List<AnilistMedia> = coroutineScope {
        val visible = media.distinctBy { it.id }.take(com.tankobun.app.home.HOME_TRENDING_LIMIT)
            .filter { includeAdult || !it.isAdult }
        val primary = if (mode.usesAniList) {
            aniList(withFallback = mode == CatalogMode.COMBINED) {
                container.anilistRepository.mangaByIds(visible.map { it.id }, accessToken, includeCharacters = true)
            }.orEmpty().filter { includeAdult || !it.isAdult }.associateBy { it.id }
        } else {
            emptyMap()
        }
        val permits = Semaphore(2)
        visible.map { original -> async {
            val al = primary[original.id]
            val hasPresentation = al?.let { it.bannerImage != null || it.mainCharacterImage != null || it.characterImages.isNotEmpty() } == true
            val mb = if (hasPresentation || !mode.usesMangaBaka) null else permits.withPermit {
                optional { container.mangaBakaRepository.artwork(original, includeAdult) }
            }
            (al ?: mb)?.withFallbackDetails(mb)?.withFallbackDetails(original)
        } }.awaitAll().filterNotNull()
    }

    suspend fun homeFeed(
        genres: List<String>, accessToken: String?, includeAdult: Boolean, mode: CatalogMode,
        onTrendingLoaded: (List<AnilistMedia>) -> Unit = {},
        onGenreHighlightsLoaded: (List<AnilistGenreHighlight>) -> Unit = {},
    ): AnilistHomeFeed {
        suspend fun fromAniList() = aniList(withFallback = mode == CatalogMode.COMBINED) {
            container.anilistRepository.homeCandidates(genres, accessToken, includeAdult) { candidates ->
                if (mode == CatalogMode.ANILIST) {
                    onTrendingLoaded(candidates.trending.take(com.tankobun.app.home.HOME_TRENDING_LIMIT))
                    onGenreHighlightsLoaded(candidates.feed(genres).genreHighlights)
                }
            }
        }
        var taxonomy = CatalogTaxonomy.Empty
        suspend fun fromMangaBaka(): HomeGenreCandidates? = optional {
            val settings = container.settingsStore
            val cachedTags = settings.mangaBakaTags()
            val tags = if (cachedTags.isNotEmpty() && System.currentTimeMillis() - settings.mangaBakaTagsCachedAtEpochMillis() < 7 * 24 * 60 * 60_000L) {
                cachedTags
            } else optional { container.mangaBakaRepository.tags() }?.also {
                settings.saveMangaBakaTags(it, System.currentTimeMillis())
            } ?: cachedTags
            taxonomy = CatalogTaxonomy(tags)
            // Candidate metadata only: the UI still loads five carousel images and one per genre.
            val media = container.mangaBakaRepository.search(sort = "trending_7d", limit = 100, includeAdult = includeAdult).media
            val candidates = mangaBakaHomeCandidates(genres, media, taxonomy)
            val presentation = withCachedPresentation(media).associateBy { it.id }
            candidates.copy(
                trending = media.map { presentation.getValue(it.id) },
                byGenre = candidates.byGenre.mapValues { (_, items) -> items.map { presentation.getValue(it.id) } },
            )
        }
        val (al, mb) = when (mode) {
            CatalogMode.ANILIST -> fromAniList() to null
            CatalogMode.MANGABAKA -> null to fromMangaBaka()
            CatalogMode.COMBINED -> navigationCatalogs(mode, ::fromAniList, ::fromMangaBaka, supplementWaitMillis = 12_000L)
        }
        check(al != null || mb != null) { "Catalogs unavailable" }
        var candidates = mergeHomeCandidates(genres, al, mb, mode)
        onTrendingLoaded(candidates.trending.take(com.tankobun.app.home.HOME_TRENDING_LIMIT))
        onGenreHighlightsLoaded(candidates.feed(genres).genreHighlights)
        if (mb != null) {
            candidates = completeHomeGenres(genres, candidates, fetch = { genre ->
                val tagId = taxonomy.resolve(genre)?.mangaBakaId
                if (tagId == null) emptyList() else optional {
                    withCachedPresentation(container.mangaBakaRepository.search(
                        sort = "trending_7d", limit = genres.size.coerceIn(1, 100),
                        tagIds = setOf(tagId), includeAdult = includeAdult,
                    ).media)
                }
            }, onLoaded = onGenreHighlightsLoaded)
        }
        return candidates.feed(genres).withHomeTrendingLimit()
    }

    suspend fun browseLanding(perPage: Int, accessToken: String?, includeAdult: Boolean, mode: CatalogMode): AnilistBrowseLanding {
        suspend fun fromAniList() = aniList(withFallback = mode == CatalogMode.COMBINED) {
            container.anilistRepository.browseLanding(perPage, accessToken, includeAdult)
        }
        // Four searches share MangaBaka's 2.2 s request spacing, also with Home.
        // Give that queue time to finish instead of consistently dropping it from Combined.
        suspend fun fromMangaBaka(): AnilistBrowseLanding? = optional(timeoutMillis = 20_000L) {
            coroutineScope {
                val trending = async { container.mangaBakaRepository.search(sort = "trending_7d", limit = perPage, includeAdult = includeAdult).media }
                val popular = async { container.mangaBakaRepository.search(sort = "popularity_asc", limit = perPage, includeAdult = includeAdult).media }
                val manhwa = async { container.mangaBakaRepository.search(sort = "popularity_asc", country = "KR", limit = perPage, includeAdult = includeAdult).media }
                val top = async { container.mangaBakaRepository.search(sort = "score_desc", limit = perPage, includeAdult = includeAdult).media }
                AnilistBrowseLanding(withCachedPresentation(trending.await()), withCachedPresentation(popular.await()),
                    withCachedPresentation(manhwa.await()), withCachedPresentation(top.await()))
            }
        }
        val (al, mb) = when (mode) {
            CatalogMode.ANILIST -> fromAniList() to null
            CatalogMode.MANGABAKA -> null to fromMangaBaka()
            CatalogMode.COMBINED -> navigationCatalogs(mode, ::fromAniList, ::fromMangaBaka, supplementWaitMillis = 20_000L)
        }
        check(al != null || mb != null) { "Catalogs unavailable" }
        return AnilistBrowseLanding(
            mergeCatalogMedia(al?.trending.orEmpty(), mb?.trending.orEmpty(), mode).take(perPage),
            mergeCatalogMedia(al?.popular.orEmpty(), mb?.popular.orEmpty(), mode).take(perPage),
            mergeCatalogMedia(al?.popularManhwa.orEmpty(), mb?.popularManhwa.orEmpty(), mode).take(perPage),
            mergeCatalogMedia(al?.topManga.orEmpty(), mb?.topManga.orEmpty(), mode).take(perPage),
        )
    }

}

internal suspend fun <T> optional(timeoutMillis: Long = 12_000L, block: suspend () -> T): T? = try { withTimeoutOrNull(timeoutMillis) { block() } }
    catch (error: Exception) { if (error is CancellationException) throw error; android.util.Log.w("TankobunCatalog", "Optional catalog request failed", error); null }

internal fun mergeRecommendations(mediaId: Int, page: Int, primary: AnilistRecommendationPage?, supplemental: List<AnilistRecommendation>, includeAdult: Boolean): AnilistRecommendationPage {
    val combined = LinkedHashMap<Int, AnilistRecommendation>()
    (primary?.recommendations.orEmpty() + supplemental).filter { it.media.id != mediaId && (includeAdult || !it.media.isAdult) }.forEach { rec ->
        val earlier = combined[rec.media.id]
        combined[rec.media.id] = earlier?.copy(media = earlier.media.withFallbackDetails(rec.media)) ?: rec
    }
    return AnilistRecommendationPage(combined.values.toList(), page, primary?.hasNextPage == true)
}
