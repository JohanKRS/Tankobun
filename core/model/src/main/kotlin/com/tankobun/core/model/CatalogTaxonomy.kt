package com.tankobun.core.model

import java.text.Normalizer
import java.util.Locale

data class CatalogTag(
    val key: String,
    val name: String,
    val category: String? = null,
    val isAdult: Boolean = false,
    val isGenre: Boolean = false,
    val mangaBakaId: Int? = null,
    val parentId: Int? = null,
    val anilistTag: String? = null,
    val anilistGenre: String? = null,
)

data class CatalogFilterPlan(
    val anilistGenres: Set<String>,
    val anilistTags: Set<String>,
    val mangaBakaTagIds: Set<Int>,
    val canQueryAniList: Boolean,
    val canQueryMangaBaka: Boolean,
)

/** Filter identity is independent of its label: MangaBaka has homonymous tags. */
class CatalogTaxonomy(val tags: List<CatalogTag>) {
    private val byKey = tags.associateBy { it.key }
    private val byName = tags.groupBy { it.name.catalogNameKey() }
    private val byMangaBakaId = tags.mapNotNull { tag -> tag.mangaBakaId?.let { it to tag } }.toMap()
    private val ancestors = byMangaBakaId.keys.associateWith { id ->
        buildSet {
            var current: Int? = id
            while (current != null && add(current)) current = byMangaBakaId[current]?.parentId
        }
    }

    fun resolve(keyOrName: String): CatalogTag? = byKey[keyOrName] ?: byName[keyOrName.catalogNameKey()]?.let { matches ->
        matches.singleOrNull() ?: matches.filter { it.anilistGenre != null || it.anilistTag != null }.firstOrNull()
            ?: matches.takeIf { group -> group.map { it.category }.distinct().size == 1 }?.firstOrNull()
    }

    fun label(keyOrName: String): String = resolve(keyOrName)?.name ?: keyOrName

    fun plan(genres: Set<String>, selectedTags: Set<String>): CatalogFilterPlan {
        val selected = (genres + selectedTags).map(::resolve)
        return CatalogFilterPlan(
            anilistGenres = selected.mapNotNull { it?.anilistGenre }.toSet(),
            anilistTags = selected.mapNotNull { it?.anilistTag }.toSet(),
            mangaBakaTagIds = selected.mapNotNull { it?.mangaBakaId }.toSet(),
            canQueryAniList = selected.all { it != null && (it.anilistGenre != null || it.anilistTag != null) },
            canQueryMangaBaka = selected.all { it?.mangaBakaId != null },
        )
    }

    /** Includes ancestors locally, matching MangaBaka's subtree search semantics. */
    fun mediaFilterKeys(media: AnilistMedia): Set<String> = buildSet {
        val ids = media.mangaBakaTagIds.toMutableSet()
        for (name in media.genres + media.tags) {
            add(name.catalogNameKey())
            val matches = byName[name.catalogNameKey()].orEmpty()
            // A name-only legacy record cannot distinguish different branches with the same name.
            val unambiguous = matches.map { it.category }.distinct().size <= 1
            matches.filter { unambiguous || it.anilistTag != null || it.anilistGenre != null }.forEach { tag ->
                add(tag.key)
                tag.mangaBakaId?.let(ids::add)
            }
        }
        for (id in ids) {
            (ancestors[id] ?: setOf(id)).forEach { ancestor -> add("mb:$ancestor") }
        }
    }

    fun matchesAny(keys: Set<String>, selected: Set<String>): Boolean = selected.isEmpty() || selected.any { key ->
        val term = resolve(key)
        if (term != null) term.key in keys else key.catalogNameKey() in keys
    }

    companion object {
        val Empty = CatalogTaxonomy(emptyList())

        fun merge(anilistTags: List<AnilistMediaTag>, anilistGenres: List<String>, mangaBakaTags: List<CatalogTag>): CatalogTaxonomy {
            val alTags = anilistTags.associateBy { it.name.catalogNameKey() }
            val alGenres = anilistGenres.associateBy(String::catalogNameKey)
            val mbGroups = mangaBakaTags.groupBy { it.name.catalogNameKey() }
            val merged = mangaBakaTags.map { tag ->
                val name = tag.name.catalogNameKey()
                val unambiguous = mbGroups.getValue(name).map { it.category }.distinct().size == 1
                val alTag = alTags[name].takeIf { unambiguous }
                val alGenre = alGenres[name].takeIf { unambiguous }
                tag.copy(
                    anilistTag = alTag?.name,
                    anilistGenre = alGenre,
                    isGenre = tag.isGenre || alGenre != null,
                    isAdult = tag.isAdult || alTag?.isAdult == true || alGenre == "Hentai",
                )
            }
            val linkedTags = merged.mapNotNull { it.anilistTag }.toSet()
            val linkedGenres = merged.mapNotNull { it.anilistGenre }.toSet()
            return CatalogTaxonomy((merged +
                anilistTags.filter { it.name !in linkedTags }.map {
                    CatalogTag("al-tag:${it.name}", it.name, it.category, it.isAdult, anilistTag = it.name)
                } + anilistGenres.filter { it !in linkedGenres }.map {
                    CatalogTag("al-genre:$it", it, "Genres", isAdult = it == "Hentai", isGenre = true, anilistGenre = it)
                }).sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }))
        }
    }
}

fun String.catalogNameKey(): String = Normalizer.normalize(this, Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT).replace('’', '\'').replace("'", "").replace('-', ' ')
    .trim().replace(Regex("\\s+"), " ")
