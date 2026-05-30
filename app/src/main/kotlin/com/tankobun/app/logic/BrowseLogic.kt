package com.tankobun.app.logic

import com.tankobun.app.state.TankobunUiState
import java.util.Locale

internal const val BROWSE_SORT_SEARCH_MATCH = "SEARCH_MATCH"
internal const val BROWSE_TRENDING_CACHE_KEY = "browse:section:trending"
internal const val BROWSE_POPULAR_CACHE_KEY = "browse:section:popular"
internal const val BROWSE_MANHWA_CACHE_KEY = "browse:section:popular-manhwa"
internal const val BROWSE_TOP_MANGA_CACHE_KEY = "browse:section:top-100:v2"

internal fun TankobunUiState.hasBrowseFilters(): Boolean =
    browseGenres.isNotEmpty() ||
        browseTags.isNotEmpty() ||
        browseFormat != null ||
        browsePublishingStatus != null ||
        browseCountryOfOrigin != null ||
        browseYear != null ||
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
    append("browse:")
    append("q=").append(searchQuery.normalizedSearchKey())
    append("|genres=").append(browseGenres.sorted().joinToString(",") { it.normalizedSearchKey() })
    append("|tags=").append(browseTags.sorted().joinToString(",") { it.normalizedSearchKey() })
    append("|format=").append(browseFormat.orEmpty())
    append("|status=").append(browsePublishingStatus.orEmpty())
    append("|country=").append(browseCountryOfOrigin.orEmpty())
    append("|year=").append(browseYear?.toString().orEmpty())
    append("|staff=").append(browseStaffName.orEmpty().normalizedSearchKey())
    append("|sort=").append(effectiveBrowseSort())
    append("|title=").append(anilistTitleLanguage.name)
}

internal fun String.normalizedSearchKey(): String =
    trim().lowercase(Locale.ROOT)

