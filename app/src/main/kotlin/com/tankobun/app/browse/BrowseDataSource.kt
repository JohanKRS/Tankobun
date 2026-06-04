package com.tankobun.app.browse

import com.tankobun.app.AppContainer
import com.tankobun.app.logic.BROWSE_RESULTS_PAGE_SIZE
import com.tankobun.app.logic.BROWSE_SORT_SEARCH_MATCH
import com.tankobun.app.logic.cachedBrowsePageFromMedia
import com.tankobun.app.logic.effectiveBrowseSort
import com.tankobun.app.logic.hasBrowseFilters
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.database.AnilistSearchResultEntity
import com.tankobun.core.database.toEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistMediaPage
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.CachePolicy
import com.tankobun.core.model.withTitleLanguage

internal class BrowseDataSource(
    private val container: AppContainer,
    private val cachePolicy: CachePolicy,
    private val titleLanguage: () -> AnilistTitleLanguage,
) {
    suspend fun cachedAnilistBrowseMedia(
        cacheKey: String,
        fetch: suspend () -> List<AnilistMedia>,
    ): List<AnilistMedia> {
        val now = System.currentTimeMillis()
        val cachedRows = container.database.searchResultDao().cachedSearchRows(cacheKey)
        val cachedIsFresh = cachedRows.isNotEmpty() &&
            cachedRows.all { now - it.fetchedAtEpochMillis <= cachePolicy.anilistSearchTtlMillis }
        if (cachedIsFresh) {
            return cachedBrowseMedia(cacheKey)
        }
        val results = fetch().map { it.withTitleLanguage(titleLanguage()) }
        cacheBrowseMedia(cacheKey, results, now)
        return results
    }

    suspend fun cachedAnilistBrowseMediaPage(
        cacheKey: String,
        fetch: suspend () -> AnilistMediaPage,
    ): AnilistMediaPage {
        val now = System.currentTimeMillis()
        val cachedRows = container.database.searchResultDao().cachedSearchRows(cacheKey)
        val cachedIsFresh = cachedRows.isNotEmpty() &&
            cachedRows.all { now - it.fetchedAtEpochMillis <= cachePolicy.anilistSearchTtlMillis }
        if (cachedIsFresh) {
            return cachedBrowsePageFromMedia(cachedBrowseMedia(cacheKey))
        }
        val page = fetch()
        cacheBrowseMedia(cacheKey, page.media)
        return page
    }

    suspend fun fetchBrowseResultsPage(snapshot: TankobunUiState, page: Int): AnilistMediaPage {
        val query = snapshot.searchQuery.trim()
        val staffName = snapshot.browseStaffName?.trim().orEmpty()
        val accessToken = container.tokenStore.accessToken()
        return when {
            staffName.isNotBlank() -> container.anilistRepository.staffMangaPage(
                staffName = staffName,
                sort = snapshot.effectiveBrowseSort(),
                page = page,
                perPage = BROWSE_RESULTS_PAGE_SIZE,
                accessToken = accessToken,
            )
            !snapshot.hasBrowseFilters() && snapshot.browseSort == BROWSE_SORT_SEARCH_MATCH -> {
                container.anilistRepository.searchMangaPage(
                    query = query,
                    page = page,
                    perPage = BROWSE_RESULTS_PAGE_SIZE,
                    accessToken = accessToken,
                )
            }
            else -> container.anilistRepository.browseMangaPage(
                search = query.takeIf { it.isNotBlank() },
                genres = snapshot.browseGenres,
                tags = snapshot.browseTags,
                format = snapshot.browseFormat,
                status = snapshot.browsePublishingStatus,
                countryOfOrigin = snapshot.browseCountryOfOrigin,
                year = snapshot.browseYear,
                sort = snapshot.effectiveBrowseSort(),
                page = page,
                perPage = BROWSE_RESULTS_PAGE_SIZE,
                accessToken = accessToken,
            )
        }.withTitleLanguage(snapshot.anilistTitleLanguage)
    }

    suspend fun cacheBrowseMedia(cacheKey: String, media: List<AnilistMedia>) {
        cacheBrowseMedia(cacheKey, media, System.currentTimeMillis())
    }

    suspend fun cachedBrowseMedia(cacheKey: String): List<AnilistMedia> =
        container.database.searchResultDao()
            .cachedSearchMedia(cacheKey)
            .map { it.toModel(titleLanguage()) }

    private suspend fun cacheBrowseMedia(cacheKey: String, media: List<AnilistMedia>, now: Long) {
        container.database.mediaDao().upsertMedia(media.map { it.toEntity(now) })
        container.database.searchResultDao().deleteForQuery(cacheKey)
        container.database.searchResultDao().upsertResults(
            media.mapIndexed { index, item ->
                AnilistSearchResultEntity(
                    query = cacheKey,
                    mediaId = item.id,
                    orderIndex = index,
                    fetchedAtEpochMillis = now,
                )
            },
        )
    }
}
