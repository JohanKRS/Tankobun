package com.tankobun.core.mangabaka

import com.tankobun.core.model.CatalogSearchFilters

internal data class MangaBakaSearchBranch(
    val types: Set<String> = emptySet(),
    val excludeNovels: Boolean = false,
    val oneShot: Boolean? = null,
)

/** The API has types and serialization tags, rather than AniList's format and origin fields. */
internal fun CatalogSearchFilters.mangaBakaBranches(): List<MangaBakaSearchBranch> {
    if (formats.any { it !in setOf("MANGA", "NOVEL", "ONE_SHOT") }) return emptyList()
    val comics = formats.isEmpty() || "MANGA" in formats || "ONE_SHOT" in formats
    // A generic novel type does not establish the work's country of origin.
    val novels = (formats.isEmpty() || "NOVEL" in formats) && countries.isEmpty()
    val countryTypes = countries.mapNotNull { mapOf("JP" to "manga", "KR" to "manhwa", "CN" to "manhua")[it] }.toSet()
    val oneShot = when {
        formats.isEmpty() || formats.containsAll(setOf("MANGA", "ONE_SHOT")) -> null
        "ONE_SHOT" in formats -> true
        else -> false
    }
    if (comics && novels && oneShot == null) return listOf(MangaBakaSearchBranch())
    return buildList {
        if (comics && (countries.isEmpty() || countryTypes.isNotEmpty())) {
            add(MangaBakaSearchBranch(types = countryTypes, excludeNovels = countryTypes.isEmpty(), oneShot = oneShot))
        }
        if (novels) add(MangaBakaSearchBranch(types = setOf("novel")))
    }
}
