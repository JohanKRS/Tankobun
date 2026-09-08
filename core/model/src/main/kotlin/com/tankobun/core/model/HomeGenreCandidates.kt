package com.tankobun.core.model

/** Keep the ranked alternatives until all genres have claimed a distinct local work. */
data class HomeGenreCandidates(
    val trending: List<AnilistMedia>,
    val byGenre: Map<String, List<AnilistMedia>>,
) {
    fun feed(genres: List<String>): AnilistHomeFeed =
        AnilistHomeFeed(trending, selectHomeGenreHighlights(genres, byGenre))
}

fun selectHomeGenreHighlights(
    genres: List<String>,
    candidates: Map<String, List<AnilistMedia>>,
): List<AnilistGenreHighlight> {
    val used = hashSetOf<Int>()
    return genres.distinct().mapNotNull { genre ->
        candidates[genre]?.firstOrNull { used.add(it.id) }?.let { AnilistGenreHighlight(genre, it) }
    }
}
