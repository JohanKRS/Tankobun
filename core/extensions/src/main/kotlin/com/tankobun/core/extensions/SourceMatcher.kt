package com.tankobun.core.extensions

import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceManga
import com.tankobun.core.model.SourceSearchResult
import kotlin.math.max

class SourceMatcher {
    fun rank(
        media: AnilistMedia,
        source: SourceDescriptor,
        candidates: List<SourceManga>,
        searchedAtEpochMillis: Long,
    ): List<SourceSearchResult> {
        val titles = buildSet {
            add(media.title.userPreferred)
            media.title.romaji?.let(::add)
            media.title.english?.let(::add)
            media.title.native?.let(::add)
            addAll(media.synonyms)
        }.map(::normalize)

        return candidates
            .map { manga ->
                val normalizedCandidate = normalize(manga.title)
                val exact = titles.any { it == normalizedCandidate }
                val contains = titles.any { it.contains(normalizedCandidate) || normalizedCandidate.contains(it) }
                val tokenScore = titles.maxOfOrNull { tokenOverlap(it, normalizedCandidate) } ?: 0.0
                val score = when {
                    exact -> 1.0
                    contains -> max(0.82, tokenScore)
                    else -> tokenScore
                }
                val reasons = buildList {
                    if (exact) add("exact title")
                    if (!exact && contains) add("title contains")
                    if (tokenScore > 0.65) add("shared words")
                    if (source.lang == "en") add("preferred language")
                }
                SourceSearchResult(
                    mediaId = media.id,
                    source = source,
                    manga = manga,
                    score = if (source.lang == "en") minOf(1.0, score + 0.03) else score,
                    reasons = reasons,
                    searchedAtEpochMillis = searchedAtEpochMillis,
                )
            }
            .filter { it.score >= 0.45 }
            .sortedByDescending { it.score }
    }

    private fun tokenOverlap(left: String, right: String): Double {
        val leftTokens = left.split(' ').filter { it.isNotBlank() }.toSet()
        val rightTokens = right.split(' ').filter { it.isNotBlank() }.toSet()
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0
        val intersection = leftTokens.intersect(rightTokens).size.toDouble()
        val union = leftTokens.union(rightTokens).size.toDouble()
        return intersection / union
    }

    private fun normalize(value: String): String {
        return value
            .lowercase()
            .replace(Regex("[^a-z0-9\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}
