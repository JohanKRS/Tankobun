package com.tankobun.app.catalog

import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.CatalogMode
import com.tankobun.core.model.withFallbackDetails
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull

/** Rank confirmed local identities, independently of which catalog supplies their artwork. */
internal fun mergeCatalogMedia(
    aniList: List<AnilistMedia>,
    mangaBaka: List<AnilistMedia>,
    mode: CatalogMode,
): List<AnilistMedia> {
    val details = LinkedHashMap<Int, AnilistMedia>()
    (aniList + mangaBaka).forEach { media ->
        details[media.id] = details[media.id]?.withFallbackDetails(media) ?: media
    }
    val ordered = when (mode) {
        CatalogMode.ANILIST -> aniList + mangaBaka
        CatalogMode.MANGABAKA -> mangaBaka + aniList
        CatalogMode.COMBINED -> {
            val first = aniList.iterator()
            val second = mangaBaka.iterator()
            val seen = hashSetOf<Int>()
            buildList {
                while (first.hasNext() || second.hasNext()) {
                    for (provider in listOf(first, second)) {
                        while (provider.hasNext()) {
                            val media = provider.next()
                            if (seen.add(media.id)) {
                                add(media)
                                break
                            }
                        }
                    }
                }
            }
        }
    }
    return ordered.distinctBy { it.id }.map { details.getValue(it.id) }
}

/** Both requests are bounded by their callers. A healthy catalog never waits for a full fallback timeout. */
internal suspend fun <T> navigationCatalogs(
    mode: CatalogMode,
    aniList: suspend () -> T?,
    mangaBaka: suspend () -> T?,
    supplementWaitMillis: Long = 4_000L,
    hasContent: (T) -> Boolean = { true },
): Pair<T?, T?> = coroutineScope {
    val al = async { aniList() }
    val mb = async { mangaBaka() }
    try {
        val firstIsAniList = when (mode) {
            CatalogMode.ANILIST -> true
            CatalogMode.MANGABAKA -> false
            CatalogMode.COMBINED -> select { al.onAwait { true }; mb.onAwait { false } }
        }
        val first = if (firstIsAniList) al else mb
        val second = if (firstIsAniList) mb else al
        val preferred = first.await()
        val supplement = if (preferred == null || !hasContent(preferred)) second.await()
            else withTimeoutOrNull(supplementWaitMillis) { second.await() }
        if (firstIsAniList) preferred to supplement else supplement to preferred
    } finally {
        al.cancel()
        mb.cancel()
    }
}
