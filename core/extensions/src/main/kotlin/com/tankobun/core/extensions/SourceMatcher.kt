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
        titleOverrides: List<String> = emptyList(),
    ): List<SourceSearchResult> {
        val titles = if (titleOverrides.isEmpty()) {
            buildSet {
                add(media.title.userPreferred)
                media.title.romaji?.let(::add)
                media.title.english?.let(::add)
                media.title.native?.let(::add)
                addAll(media.synonyms)
            }
        } else {
            titleOverrides.toSet()
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

        val expectedTokens = titleTokens(expected)
        val candidateTokens = titleTokens(candidate)
        val contains = expected.contains(candidate) || candidate.contains(expected)
        val looseSingleTokenContainment = contains &&
            minOf(expectedTokens.size, candidateTokens.size) == 1 &&
            maxOf(expectedTokens.size, candidateTokens.size) > 1
        val tokenScore = tokenOverlap(expectedTokens, candidateTokens)
        val acronym = acronymOf(expected)?.let { it == candidate } == true ||
            acronymOf(candidate)?.let { it == expected } == true
        val baseScore = when {
            looseSingleTokenContainment -> max(0.62, tokenScore.coerceAtMost(0.82))
            contains -> max(0.84, tokenScore)
            acronym -> 0.9
            else -> tokenScore
        }
        val score = if (looseSingleTokenContainment) baseScore else max(baseScore, tokenScore)

        return TitleMatchScore(
            score = score,
            contains = contains,
            acronym = acronym,
            sharedWords = tokenScore > 0.58,
        )
    }

    private fun tokenOverlap(leftTokens: Set<String>, rightTokens: Set<String>): Double {
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0
        val intersection = leftTokens.intersect(rightTokens).size.toDouble()
        val dice = (2.0 * intersection) / (leftTokens.size + rightTokens.size)
        val containment = intersection / minOf(leftTokens.size, rightTokens.size)
        return max(dice, containment * 0.92)
    }

    private fun titleTokens(value: String): Set<String> =
        value.split(' ').filter { it.isNotBlank() }.toSet()

    private fun acronymOf(value: String): String? {
        val tokens = value.split(' ').filter { it.isNotBlank() }
        if (tokens.size < 2) return null
        return tokens.joinToString(separator = "") { it.first().toString() }
            .takeIf { it.length >= 2 }
    }

    private fun normalize(value: String): String {
        val withoutMarks = Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replace(COMBINING_MARKS_REGEX, "")
        return withoutMarks
            .lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(APOSTROPHE_REGEX, "")
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .trim()
            .replace(WHITESPACE_REGEX, " ")
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
        val COMBINING_MARKS_REGEX = Regex("\\p{Mn}+")
        val APOSTROPHE_REGEX = Regex("['’]")
        val NON_ALPHANUMERIC_REGEX = Regex("[^\\p{L}\\p{N}]+")
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
