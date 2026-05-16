package com.tankobun.core.extensions

import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceManga
import com.tankobun.core.model.SourceSearchResult
import java.text.Normalizer
import java.util.Locale
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
            .filter { it.isNotBlank() }

        return candidates
            .map { manga ->
                val normalizedCandidate = normalize(manga.title)
                val best = titles
                    .asSequence()
                    .map { scoreTitleMatch(it, normalizedCandidate) }
                    .maxByOrNull { it.score }
                    ?: TitleMatchScore()
                val score = best.score
                val preferredLanguage = source.lang.equals("en", ignoreCase = true)
                val adjustedScore = if (preferredLanguage) minOf(1.0, score + 0.03) else score
                val reasons = buildList {
                    if (best.exact) add("exact title")
                    if (!best.exact && best.contains) add("title contains")
                    if (best.acronym) add("acronym")
                    if (best.sharedWords) add("shared words")
                    if (preferredLanguage) add("preferred language")
                }
                SourceSearchResult(
                    mediaId = media.id,
                    source = source,
                    manga = manga,
                    score = adjustedScore,
                    reasons = reasons,
                    searchedAtEpochMillis = searchedAtEpochMillis,
                )
            }
            .filter { it.score >= MIN_MATCH_SCORE }
            .sortedByDescending { it.score }
    }

    private fun scoreTitleMatch(expected: String, candidate: String): TitleMatchScore {
        if (expected.isBlank() || candidate.isBlank()) return TitleMatchScore()
        if (expected == candidate) {
            return TitleMatchScore(score = 1.0, exact = true)
        }

        val contains = expected.contains(candidate) || candidate.contains(expected)
        val tokenScore = tokenOverlap(expected, candidate)
        val acronym = acronymOf(expected)?.let { it == candidate } == true ||
            acronymOf(candidate)?.let { it == expected } == true
        val score = max(
            when {
                contains -> max(0.84, tokenScore)
                acronym -> 0.9
                else -> tokenScore
            },
            tokenScore,
        )

        return TitleMatchScore(
            score = score,
            contains = contains,
            acronym = acronym,
            sharedWords = tokenScore > 0.58,
        )
    }

    private fun tokenOverlap(left: String, right: String): Double {
        val leftTokens = left.split(' ').filter { it.isNotBlank() }.toSet()
        val rightTokens = right.split(' ').filter { it.isNotBlank() }.toSet()
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0
        val intersection = leftTokens.intersect(rightTokens).size.toDouble()
        val dice = (2.0 * intersection) / (leftTokens.size + rightTokens.size)
        val containment = intersection / minOf(leftTokens.size, rightTokens.size)
        return max(dice, containment * 0.92)
    }

    private fun acronymOf(value: String): String? {
        val tokens = value.split(' ').filter { it.isNotBlank() }
        if (tokens.size < 2) return null
        return tokens.joinToString(separator = "") { it.first().toString() }
            .takeIf { it.length >= 2 }
    }

    private fun normalize(value: String): String {
        val withoutMarks = Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replace(Regex("\\p{Mn}+"), "")
        return withoutMarks
            .lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(Regex("['’]"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private data class TitleMatchScore(
        val score: Double = 0.0,
        val exact: Boolean = false,
        val contains: Boolean = false,
        val acronym: Boolean = false,
        val sharedWords: Boolean = false,
    )

    private companion object {
        const val MIN_MATCH_SCORE = 0.35
    }
}
