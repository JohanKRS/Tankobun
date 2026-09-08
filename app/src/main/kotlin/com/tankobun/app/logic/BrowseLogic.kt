package com.tankobun.app.logic

import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistMediaPage
import java.util.Locale

internal const val BROWSE_LANDING_SECTION_SIZE = 10
internal const val BROWSE_RESULTS_PAGE_SIZE = 50
internal const val BROWSE_SORT_SEARCH_MATCH = "SEARCH_MATCH"
internal const val BROWSE_TRENDING_CACHE_KEY = "browse:section:trending"
internal const val BROWSE_POPULAR_CACHE_KEY = "browse:section:popular"
internal const val BROWSE_MANHWA_CACHE_KEY = "browse:section:popular-manhwa"
internal const val BROWSE_TOP_MANGA_CACHE_KEY = "browse:section:top-100:v2"

internal data class BrowseLandingData(
    val trending: List<AnilistMedia>,
    val popular: List<AnilistMedia>,
    val popularManhwa: List<AnilistMedia>,
    val topManga: List<AnilistMedia>,
)

internal fun BrowseLandingData.hasContent(): Boolean =
    trending.isNotEmpty() || popular.isNotEmpty() || popularManhwa.isNotEmpty() || topManga.isNotEmpty()

internal fun TankobunUiState.hasBrowseFilters(): Boolean =
    browseGenres.isNotEmpty() ||
        browseTags.isNotEmpty() ||
        browseSelection.isActive ||
        browseStaffName != null

internal fun TankobunUiState.hasBrowseQueryOrFilters(): Boolean =
    searchQuery.trim().isNotBlank() ||
        hasBrowseFilters() ||
        browseSort != BROWSE_SORT_SEARCH_MATCH

internal fun TankobunUiState.effectiveBrowseSort(): String =
    if (browseSort == BROWSE_SORT_SEARCH_MATCH && searchQuery.isBlank()) {
        "TRENDING_DESC"
    } else {
        browseSort
    }

internal fun TankobunUiState.browseCacheKey(): String = buildString {
    append("browse:v2:")
    append("q=").append(searchQuery.normalizedSearchKey())
    append("|genres=").append(browseGenres.sorted().joinToString(",") { it.normalizedSearchKey() })
    append("|tags=").append(browseTags.sorted().joinToString(",") { it.normalizedSearchKey() })
    append("|format=").append(browseSelection.formats.sorted().joinToString(","))
    append("|status=").append(browseSelection.statuses.sorted().joinToString(","))
    append("|country=").append(browseSelection.countries.sorted().joinToString(","))
    append("|year=").append(browseSelection.years?.let { "${it.from ?: ""}..${it.to ?: ""}" }.orEmpty())
    append("|staff=").append(browseStaffName.orEmpty().normalizedSearchKey())
    append("|sort=").append(effectiveBrowseSort())
    append("|title=").append(anilistTitleLanguage.name)
    append("|catalog=").append(catalogMode.name)
    append("|nsfw=").append(showNsfwContent)
    if (browseGenres.isNotEmpty() || browseTags.isNotEmpty()) append("|taxonomy=v1")
}

internal fun TankobunUiState.browseLandingCacheKey(baseKey: String): String =
    "$baseKey|nsfw=$showNsfwContent|catalog=${catalogMode.name}"

internal fun String.normalizedSearchKey(): String =
    trim().lowercase(Locale.ROOT)

internal fun cachedBrowsePageFromMedia(media: List<AnilistMedia>): AnilistMediaPage {
    val cachedPage = if (media.size < BROWSE_RESULTS_PAGE_SIZE) {
        0
    } else {
        ((media.size - 1) / BROWSE_RESULTS_PAGE_SIZE + 1).coerceAtLeast(1)
    }
    return AnilistMediaPage(
        media = media,
        currentPage = cachedPage,
        hasNextPage = media.isNotEmpty() &&
            (media.size < BROWSE_RESULTS_PAGE_SIZE || media.size % BROWSE_RESULTS_PAGE_SIZE == 0),
    )
}
