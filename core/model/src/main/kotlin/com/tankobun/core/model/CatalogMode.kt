package com.tankobun.core.model

/** Navigation ranking only; independent of reading sources, local identity and tracking accounts. */
enum class CatalogMode {
    ANILIST,
    MANGABAKA,
    COMBINED;

    companion object {
        fun fromStored(value: String?): CatalogMode = entries.firstOrNull { it.name == value } ?: ANILIST
    }
}
