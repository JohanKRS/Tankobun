package com.tankobun.app.catalog

import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.CatalogMode

/** Preserve filters, saved works, progress and personalized suggestions when changing navigation. */
internal fun TankobunUiState.withCatalogMode(mode: CatalogMode): TankobunUiState = copy(
    catalogMode = mode,
    browseTrending = emptyList(),
    browsePopular = emptyList(),
    browsePopularManhwa = emptyList(),
    browseTopManga = emptyList(),
    browseLandingLoaded = false,
    homeTrending = emptyList(),
    homeGenreHighlights = emptyList(),
    homeLoaded = false,
    homeTrendingRefreshing = false,
    homeGenreHighlightsRefreshing = false,
    homeRefreshingGenres = emptySet(),
    searchResults = emptyList(),
    browseSearched = false,
    browseResultsPage = 0,
    browseResultsHasMore = false,
    browseResultsLoadingMore = false,
    busy = false,
    message = null,
)
