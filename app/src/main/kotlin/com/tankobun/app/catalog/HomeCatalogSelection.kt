package com.tankobun.app.catalog

import com.tankobun.core.model.*

internal fun mangaBakaHomeCandidates(
    genres: List<String>, media: List<AnilistMedia>, taxonomy: CatalogTaxonomy,
): HomeGenreCandidates {
    val keys = media.associate { it.id to taxonomy.mediaFilterKeys(it) }
    return HomeGenreCandidates(media, genres.associateWith { genre ->
        media.filter { taxonomy.matchesAny(keys.getValue(it.id), setOf(genre)) }
    })
}

internal fun mergeHomeCandidates(
    genres: List<String>, aniList: HomeGenreCandidates?, mangaBaka: HomeGenreCandidates?, mode: CatalogMode,
): HomeGenreCandidates = HomeGenreCandidates(
    mergeCatalogMedia(aniList?.trending.orEmpty(), mangaBaka?.trending.orEmpty(), mode),
    genres.mapIndexed { index, genre ->
        val preference = if (mode == CatalogMode.COMBINED) {
            if (index % 2 == 0) CatalogMode.ANILIST else CatalogMode.MANGABAKA
        } else mode
        genre to mergeCatalogMedia(aniList?.byGenre?.get(genre).orEmpty(), mangaBaka?.byGenre?.get(genre).orEmpty(), preference)
    }.toMap(),
)

/** Fetch only empty/exhausted genres, in display order. Never reuse a previous genre's work. */
internal suspend fun completeHomeGenres(
    genres: List<String>, initial: HomeGenreCandidates,
    fetch: suspend (String) -> List<AnilistMedia>?,
    onLoaded: (List<AnilistGenreHighlight>) -> Unit,
): HomeGenreCandidates {
    val candidates = initial.byGenre.toMutableMap()
    var failures = 0
    for (genre in genres) {
        if (selectHomeGenreHighlights(genres, candidates).any { it.genre == genre }) continue
        val additional = fetch(genre)
        if (additional == null) {
            if (++failures >= 2) break
            continue
        }
        failures = 0
        candidates[genre] = (candidates[genre].orEmpty() + additional).distinctBy { it.id }
        onLoaded(selectHomeGenreHighlights(genres, candidates))
    }
    return initial.copy(byGenre = candidates)
}
