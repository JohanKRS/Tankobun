package com.tankobun.app.ui.filters

import com.tankobun.app.AppLanguage
import com.tankobun.app.qa.LayoutQaScreens
import com.tankobun.core.model.CatalogTag
import com.tankobun.core.model.CatalogTaxonomy
import org.junit.Test

class FilterSheetQaScreens(language: AppLanguage) : LayoutQaScreens(language) {
    private val genres = listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Mystery", "Psychological", "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural", "Thriller")
        .map { CatalogTag(key = "genre:$it", name = it, category = "Genres", isGenre = true, anilistGenre = it) }
    private val tags = listOf(
        "Female Protagonist" to "Cast-Main Cast",
        "Time Skip" to "Theme-Other",
        "Magic" to "Theme-Fantasy",
        "Elf" to "Cast-Traits",
        "Travel" to "Theme-Other",
    ).map { (name, category) -> CatalogTag(key = "tag:$name", name = name, category = category, anilistTag = name) }
    private val taxonomy = CatalogTaxonomy(genres + tags)

    @Test
    fun genreSheet() = capture("filter-genres", topWindow = true) {
        CatalogFilterDialog(
            title = "Genres",
            taxonomy = taxonomy,
            options = genres,
            selected = setOf(genres[0].key, genres[4].key),
            includeAdult = false,
            genresOnly = true,
            onApply = {},
            onDismiss = {},
        )
    }

    @Test
    fun tagSheet() = capture("filter-tags", topWindow = true) {
        CatalogFilterDialog(
            title = "Tags",
            taxonomy = taxonomy,
            options = genres + tags,
            selected = setOf(tags[0].key, tags[2].key),
            includeAdult = false,
            onApply = {},
            onDismiss = {},
        )
    }
}
