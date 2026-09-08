package com.tankobun.core.model

data class PublicationYears(val from: Int? = null, val to: Int? = null) {
    init {
        require(from != null || to != null)
        require(from == null || from in 1679..2262)
        require(to == null || to in 1679..2262)
        require(from == null || to == null || from <= to)
    }

    fun contains(year: Int?): Boolean = year != null && (from == null || year >= from) && (to == null || year <= to)
}

/** Alternatives within a field; intersections between fields. Empty selections impose no restriction. */
data class CatalogSearchFilters(
    val formats: Set<String> = emptySet(),
    val statuses: Set<String> = emptySet(),
    val countries: Set<String> = emptySet(),
    val years: PublicationYears? = null,
) {
    val isActive: Boolean get() = formats.isNotEmpty() || statuses.isNotEmpty() || countries.isNotEmpty() || years != null

    fun matches(media: AnilistMedia): Boolean =
        (formats.isEmpty() || media.format in formats) &&
            (statuses.isEmpty() || media.status in statuses) &&
            (countries.isEmpty() || media.countryOfOrigin in countries) &&
            (years == null || years.contains(media.startDateYear))
}
