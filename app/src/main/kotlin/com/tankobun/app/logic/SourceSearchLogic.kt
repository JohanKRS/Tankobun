package com.tankobun.app.logic

import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.SourceSearchResult
import java.util.Locale

internal fun sourceSearchQueries(
    media: AnilistMedia,
    titleOverride: String? = null,
    limit: Int = SOURCE_SEARCH_QUERY_LIMIT,
): List<String> {
    val rawTitles = titleOverride
        ?.takeIf { it.isNotBlank() }
        ?.let(::listOf)
        ?: buildList {
            add(media.title.userPreferred)
            media.title.romaji?.let(::add)
            media.title.english?.let(::add)
            media.title.native?.let(::add)
            addAll(media.synonyms)
        }

    return rawTitles
        .flatMap(::sourceSearchQueryVariants)
        .map(::cleanSourceSearchQuery)
        .filter { it.length >= 2 }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .take(limit)
}

internal fun sourceSearchRankTitleVariants(title: String): List<String> =
    sourceSearchQueryVariants(title)
        .flatMap { variant -> listOf(variant, cleanSourceSearchQuery(variant)) }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase(Locale.ROOT) }

internal fun sourcePickerDefaultSearchTitle(media: AnilistMedia): String =
    listOf(
        media.title.userPreferred,
        media.title.romaji,
        media.title.english,
        media.title.native,
    ).firstOrNull { !it.isNullOrBlank() }.orEmpty()

internal fun sourcePickerErrorMessage(sourceName: String, error: Throwable): String {
    val detail = errorDetail(error)
        ?: error.javaClass.simpleName
    return when {
        isMissingSourceCompatibilityClass(error) ->
            "$sourceName needs a compatibility library that was missing from the app. Update the app and try again."
        isDirectUrlRequiredError(error) ->
            "$sourceName needs a direct source URL instead of a title search. Paste a supported URL or choose another source."
        isUnexpectedSourceResponseError(error) ->
            "$sourceName returned data the extension could not parse. Try updating that extension or choose another source."
        detail.contains("syntax error in regexp pattern", ignoreCase = true) ->
            "$sourceName failed while parsing source data. The extension reported a regexp error; try another source or update that extension."
        detail.contains("timeout", ignoreCase = true) || detail.contains("timed out", ignoreCase = true) ->
            "$sourceName took too long to respond. Try again or choose another source."
        detail.contains("No readable manga found", ignoreCase = true) ->
            detail
        else -> "$sourceName failed: $detail"
    }
}

internal fun sourcePickerDiagnosticDetail(error: Throwable): String {
    val detail = errorDetail(error) ?: error.javaClass.simpleName
    return when {
        isMissingSourceCompatibilityClass(error) -> "missing compatibility class"
        isDirectUrlRequiredError(error) -> "requires a direct URL"
        isUnexpectedSourceResponseError(error) -> "unexpected source response"
        detail.contains("HTTP error", ignoreCase = true) -> detail
        detail.contains("syntax error in regexp pattern", ignoreCase = true) -> "regexp parse error"
        detail.contains("timeout", ignoreCase = true) -> "timed out"
        else -> detail.take(120)
    }
}

internal fun isFatalSourceSearchError(error: Throwable): Boolean {
    val details = errorDetails(error).joinToString(separator = "\n")
    return isMissingSourceCompatibilityClass(error) ||
        isDirectUrlRequiredError(error) ||
        isUnexpectedSourceResponseError(error) ||
        details.contains("HTTP error 401", ignoreCase = true) ||
        details.contains("HTTP error 403", ignoreCase = true)
}

internal fun cleanSourceSearchQuery(query: String): String =
    buildString {
        var previousWasSpace = true
        query.forEach { char ->
            val replacement = when {
                char == '&' -> " and "
                char.isLetterOrDigit() -> char.toString()
                char.isWhitespace() -> " "
                else -> " "
            }
            replacement.forEach { next ->
                if (next.isWhitespace()) {
                    if (!previousWasSpace) append(' ')
                    previousWasSpace = true
                } else {
                    append(next)
                    previousWasSpace = false
                }
            }
        }
    }.trim()

internal fun SourceSearchResult.sourceMatchKey(): String =
    sourceMatchKey(source.id, manga.url)

internal fun SourceSearchResult.isReadableMatchCandidate(): Boolean =
    score >= SOURCE_READABLE_MATCH_SCORE

internal fun sourceMatchKey(sourceId: Long, mangaUrl: String): String =
    "$sourceId:$mangaUrl"

private fun sourceSearchQueryVariants(title: String): List<String> {
    val withoutHtml = title.withoutHtmlTags()
    val withoutParenthetical = withoutHtml.withoutBracketedText()
    val subtitlePrefix = withoutHtml.mainTitlePrefix()
    val spacedNumber = withoutHtml.withSpacedNoNumber()
    val withoutNumberPrefix = withoutHtml.withoutNoNumberPrefix()
    val words = cleanSourceSearchQuery(withoutHtml)
        .split(' ')
        .filter { it.isNotBlank() }
    val acronym = words
        .takeIf { it.size >= 3 }
        ?.joinToString(separator = "") { it.first().uppercaseChar().toString() }
    val leadingWords = words
        .takeIf { it.size >= 4 }
        ?.take(4)
        ?.joinToString(" ")

    return buildList {
        add(withoutHtml)
        add(withoutParenthetical)
        add(subtitlePrefix)
        add(spacedNumber)
        add(withoutNumberPrefix)
        leadingWords?.let(::add)
        acronym?.let(::add)
    }.distinctBy { it.lowercase(Locale.ROOT) }
}

private fun String.withoutHtmlTags(): String = buildString {
    var insideTag = false
    this@withoutHtmlTags.forEach { char ->
        when (char) {
            '<' -> {
                insideTag = true
                append(' ')
            }
            '>' -> {
                insideTag = false
                append(' ')
            }
            else -> if (!insideTag) append(char)
        }
    }
}

private fun String.withoutBracketedText(): String = buildString {
    var closingBracket: Char? = null
    this@withoutBracketedText.forEach { char ->
        when {
            closingBracket == null && char == '(' -> {
                closingBracket = ')'
                append(' ')
            }
            closingBracket == null && char == '[' -> {
                closingBracket = ']'
                append(' ')
            }
            closingBracket == null && char == '{' -> {
                closingBracket = '}'
                append(' ')
            }
            closingBracket != null && char == closingBracket -> {
                closingBracket = null
                append(' ')
            }
            closingBracket == null -> append(char)
        }
    }
}

private fun String.mainTitlePrefix(): String {
    val separators = listOf(
        ":",
        "\uFF1A",
        " - ",
        " \u2010 ",
        " \u2011 ",
        " \u2012 ",
        " \u2013 ",
        " \u2014 ",
        " \u2015 ",
    )
    val splitAt = separators.mapNotNull { separator ->
        indexOf(separator).takeIf { it >= 0 }
    }.minOrNull()
    return splitAt?.let { take(it) }.orEmpty()
}

private fun String.withSpacedNoNumber(): String = buildString {
    var index = 0
    while (index < this@withSpacedNoNumber.length) {
        val char = this@withSpacedNoNumber[index]
        val next = this@withSpacedNoNumber.getOrNull(index + 1)
        if ((char == 'N' || char == 'n') && (next == 'O' || next == 'o')) {
            var cursor = index + 2
            if (this@withSpacedNoNumber.getOrNull(cursor) == '.') cursor += 1
            if (this@withSpacedNoNumber.getOrNull(cursor)?.isDigit() == true) {
                append("No. ")
                index = cursor
                continue
            }
        }
        append(char)
        index += 1
    }
}

private fun String.withoutNoNumberPrefix(): String = buildString {
    var index = 0
    while (index < this@withoutNoNumberPrefix.length) {
        val char = this@withoutNoNumberPrefix[index]
        val next = this@withoutNoNumberPrefix.getOrNull(index + 1)
        if ((char == 'N' || char == 'n') && (next == 'O' || next == 'o')) {
            var cursor = index + 2
            if (this@withoutNoNumberPrefix.getOrNull(cursor) == '.') cursor += 1
            while (this@withoutNoNumberPrefix.getOrNull(cursor)?.isWhitespace() == true) {
                cursor += 1
            }
            if (this@withoutNoNumberPrefix.getOrNull(cursor)?.isDigit() == true) {
                index = cursor
                continue
            }
        }
        append(char)
        index += 1
    }
}

private fun errorDetail(error: Throwable): String? =
    errorDetails(error)
        .firstOrNull { it.contains("HTTP error", ignoreCase = true) }
        ?: errorDetails(error).firstOrNull()

private fun errorDetails(error: Throwable): List<String> =
    generateSequence(error) { it.cause }
        .mapNotNull { it.message?.takeIf(String::isNotBlank) }
        .toList()

private fun isMissingSourceCompatibilityClass(error: Throwable): Boolean =
    generateSequence(error) { it.cause }.any { cause ->
        cause is NoClassDefFoundError ||
            cause.message?.contains("OkioStreamsKt", ignoreCase = true) == true
    }

private fun isDirectUrlRequiredError(error: Throwable): Boolean =
    errorDetails(error).any { detail ->
        detail.contains("enter a valid", ignoreCase = true) &&
            detail.contains("url", ignoreCase = true)
    }

private fun isUnexpectedSourceResponseError(error: Throwable): Boolean =
    generateSequence(error) { it.cause }.any { cause ->
        cause.javaClass.name == "kotlinx.serialization.MissingFieldException"
    }

private const val SOURCE_READABLE_MATCH_SCORE = 0.9
private const val SOURCE_SEARCH_QUERY_LIMIT = 12
