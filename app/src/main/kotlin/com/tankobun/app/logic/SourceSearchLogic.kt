package com.tankobun.app.logic

import android.content.Context
import com.tankobun.app.R
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceManga
import com.tankobun.core.model.SourceSearchResult
import java.util.Locale

internal const val SOURCE_CANDIDATES_TO_VERIFY = 5

internal data class VerifiedSourceMatches(
    val matches: List<SourceSearchResult>,
    val chapterCounts: Map<String, Int>,
)

internal data class VerifiedReadableMatch(
    val match: SourceSearchResult,
    val chapterCount: Int,
)

internal data class SourceChapterSelection(
    val source: SourceDescriptor,
    val manga: SourceManga,
)

internal class SourceQueryTimeoutException(query: String) : RuntimeException("Search timed out for '$query'")

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

internal fun TankobunUiState.sourcePickerSources(): List<SourceDescriptor> =
    installedSources
        .distinctBy { "${it.packageName}:${it.id}" }
        .sortedWith(
            compareBy<SourceDescriptor> { if (it.id == selectedSourceId) 0 else 1 }
                .thenBy { it.languageSortPriority(sourceLanguages) }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.lang },
        )

internal fun TankobunUiState.withSourcePickerOpened(media: AnilistMedia): TankobunUiState =
    copy(
        sourcePickerOpen = true,
        sourcePickerMessage = null,
        sourcePickerDiagnostics = emptyList(),
        sourcePickerSearchTitle = sourcePickerDefaultSearchTitle(media),
        message = null,
    )

internal fun TankobunUiState.withSourcePickerNoSources(context: Context): TankobunUiState =
    context.getString(R.string.source_picker_no_sources).let { pickerMessage ->
        copy(
            sourcePickerMessage = pickerMessage,
            message = if (sourcePickerOpen) message else pickerMessage,
        )
    }

internal fun TankobunUiState.withSourcePickerNoSources(): TankobunUiState =
    copy(
        sourcePickerMessage = SOURCE_PICKER_NO_SOURCES_MESSAGE,
        message = if (sourcePickerOpen) message else SOURCE_PICKER_NO_SOURCES_MESSAGE,
    )

internal fun TankobunUiState.withSourcePickerClosed(): TankobunUiState =
    copy(
        busy = false,
        sourcePickerOpen = false,
        sourcePickerLoading = false,
        sourcePickerMessage = null,
        sourcePickerDiagnostics = emptyList(),
        sourcePickerSearchTitle = "",
    )

internal fun TankobunUiState.withSourcePickerSearchTitle(title: String): TankobunUiState =
    copy(sourcePickerSearchTitle = title)

internal fun TankobunUiState.withSourcePickerEditedTitleTooShort(context: Context): TankobunUiState =
    copy(sourcePickerMessage = context.getString(R.string.source_picker_short_query))

internal fun TankobunUiState.withSourcePickerSourceSearchStarted(
    context: Context,
    source: SourceDescriptor,
): TankobunUiState =
    copy(
        sourcePickerLoading = true,
        sourcePickerMessage = context.getString(R.string.source_picker_searching_source, source.name),
        sourcePickerDiagnostics = emptyList(),
        message = null,
    )

internal fun TankobunUiState.withSourcePickerMatchOpening(
    context: Context,
    match: SourceSearchResult,
): TankobunUiState =
    copy(
        sourcePickerLoading = true,
        sourcePickerMessage = context.getString(
            R.string.source_picker_opening_match,
            match.manga.title,
            match.source.name,
        ),
        message = null,
    )

internal fun TankobunUiState.withSourcePickerSearchStarted(
    context: Context,
    editedTitle: String?,
): TankobunUiState =
    copy(
        sourcePickerLoading = true,
        sourcePickerMessage = editedTitle?.let { title ->
            context.getString(R.string.source_picker_searching_title, title)
        } ?: context.getString(R.string.source_picker_searching_enabled),
        sourcePickerDiagnostics = emptyList(),
        message = null,
    )

internal fun TankobunUiState.withSourcePickerSearchCompleted(
    context: Context,
    verified: VerifiedSourceMatches,
    editedTitle: String?,
): TankobunUiState {
    val selectedMatches = sourceMatches.filter { match ->
        selectedSourceId == match.source.id &&
            selectedSourceManga?.url == match.manga.url
    }
    val nextMatches = (selectedMatches + verified.matches)
        .distinctSourceMatches()
        .sortedByDescending { match -> match.score }
    return copy(
        sourceMatches = nextMatches,
        sourceMatchChapterCounts = sourceMatchChapterCounts + verified.chapterCounts,
        busy = false,
        sourcePickerLoading = false,
        sourcePickerMessage = sourcePickerSearchCompletedMessage(context, nextMatches.size, editedTitle),
        message = null,
    )
}

internal fun TankobunUiState.withSourcePickerSearchCompleted(
    verified: VerifiedSourceMatches,
    editedTitle: String?,
): TankobunUiState {
    val selectedMatches = sourceMatches.filter { match ->
        selectedSourceId == match.source.id &&
            selectedSourceManga?.url == match.manga.url
    }
    val nextMatches = (selectedMatches + verified.matches)
        .distinctSourceMatches()
        .sortedByDescending { match -> match.score }
    return copy(
        sourceMatches = nextMatches,
        sourceMatchChapterCounts = sourceMatchChapterCounts + verified.chapterCounts,
        busy = false,
        sourcePickerLoading = false,
        sourcePickerMessage = sourcePickerSearchCompletedMessage(nextMatches.size, editedTitle),
        message = null,
    )
}

internal fun TankobunUiState.withSourcePickerMatchPublished(
    context: Context,
    match: SourceSearchResult,
    chapterCount: Int,
): TankobunUiState {
    val nextMatches = (sourceMatches + match)
        .distinctSourceMatches()
        .sortedByDescending { result -> result.score }
    return copy(
        sourceMatches = nextMatches,
        sourceMatchChapterCounts = sourceMatchChapterCounts + (match.sourceMatchKey() to chapterCount),
        sourcePickerMessage = context.getString(R.string.source_picker_found_readable, nextMatches.size),
        message = null,
    )
}

internal fun TankobunUiState.withSourcePickerDiagnostic(
    source: SourceDescriptor,
    detail: String,
): TankobunUiState {
    val diagnostic = "${source.name}: $detail"
    return if (diagnostic in sourcePickerDiagnostics) {
        this
    } else {
        copy(sourcePickerDiagnostics = sourcePickerDiagnostics + diagnostic)
    }
}

internal fun TankobunUiState.withSourcePickerSourceSelected(
    context: Context,
    match: SourceSearchResult,
    addToMatches: Boolean,
): TankobunUiState {
    val nextMatches = if (addToMatches) {
        (listOf(match) + sourceMatches).distinctSourceMatches()
    } else {
        sourceMatches
    }
    return copy(
        sourceMatches = nextMatches,
        selectedSourceId = match.source.id,
        selectedSourceManga = match.manga,
        sourcePickerOpen = false,
        sourcePickerLoading = false,
        sourcePickerMessage = null,
        message = context.getString(R.string.source_picker_selected, match.manga.title),
    )
}

internal fun TankobunUiState.withSourcePickerSourceSelected(
    match: SourceSearchResult,
    addToMatches: Boolean,
): TankobunUiState {
    val nextMatches = if (addToMatches) {
        (listOf(match) + sourceMatches).distinctSourceMatches()
    } else {
        sourceMatches
    }
    return copy(
        sourceMatches = nextMatches,
        selectedSourceId = match.source.id,
        selectedSourceManga = match.manga,
        sourcePickerOpen = false,
        sourcePickerLoading = false,
        sourcePickerMessage = null,
        message = "Source selected for ${match.manga.title}",
    )
}

internal fun TankobunUiState.withSourcePickerFailure(
    context: Context,
    sourceName: String,
    error: Throwable,
): TankobunUiState =
    copy(
        busy = false,
        sourcePickerLoading = false,
        sourcePickerMessage = sourcePickerErrorMessage(context, sourceName, error),
        message = null,
    )

internal fun TankobunUiState.selectedSourceChapterSelection(): SourceChapterSelection? {
    val selectedSourceId = this.selectedSourceId
    val selectedManga = this.selectedSourceManga
    val match = sourceMatches.firstOrNull { result ->
        result.source.id == selectedSourceId &&
            selectedManga != null &&
            result.manga.sourceId == selectedManga.sourceId &&
            result.manga.url == selectedManga.url
    } ?: sourceMatches.firstOrNull { result ->
        selectedManga != null &&
            result.manga.sourceId == selectedManga.sourceId &&
            result.manga.url == selectedManga.url
    } ?: sourceMatches.firstOrNull { it.source.id == selectedSourceId }
    val manga = match?.manga ?: selectedManga?.takeIf { manga ->
        selectedSourceId == null || manga.sourceId == selectedSourceId
    }
    val source = match?.source
        ?: manga?.let { selected ->
            installedSources.firstOrNull { it.id == selected.sourceId }
                ?: allInstalledSources.firstOrNull { it.id == selected.sourceId }
        }
        ?: selectedSource
    return if (source == null || manga == null) {
        null
    } else {
        SourceChapterSelection(source, manga)
    }
}

internal fun TankobunUiState.withSourceChapterSelectionMissing(context: Context): TankobunUiState =
    copy(message = context.getString(R.string.source_picker_choose_match_first))

internal fun TankobunUiState.withSourceChaptersLoading(): TankobunUiState =
    copy(busy = true, message = null)

internal fun TankobunUiState.withSourceChaptersLoaded(
    context: Context,
    source: SourceDescriptor,
    manga: SourceManga,
    chapters: List<SourceChapter>,
    chapterProgress: Map<String, ReadingProgress>,
): TankobunUiState =
    copy(
        selectedSourceManga = manga,
        sourceChapters = chapters,
        chapterProgress = chapterProgress,
        selectingDownloadChapters = false,
        selectedDownloadChapterUrls = emptySet(),
        busy = false,
        sourceMatchChapterCounts = sourceMatchChapterCounts + (sourceMatchKey(source.id, manga.url) to chapters.size),
        message = if (chapters.isEmpty()) context.getString(R.string.source_picker_no_chapters) else null,
    )

internal fun TankobunUiState.withSourceChaptersLoaded(
    source: SourceDescriptor,
    manga: SourceManga,
    chapters: List<SourceChapter>,
    chapterProgress: Map<String, ReadingProgress>,
): TankobunUiState =
    copy(
        selectedSourceManga = manga,
        sourceChapters = chapters,
        chapterProgress = chapterProgress,
        selectingDownloadChapters = false,
        selectedDownloadChapterUrls = emptySet(),
        busy = false,
        sourceMatchChapterCounts = sourceMatchChapterCounts + (sourceMatchKey(source.id, manga.url) to chapters.size),
        message = if (chapters.isEmpty()) "No chapters found" else null,
    )

internal fun TankobunUiState.withSourceChaptersLoadFailed(context: Context, error: Throwable): TankobunUiState =
    copy(busy = false, message = error.message ?: context.getString(R.string.source_picker_chapter_load_failed))

internal fun sourcePickerErrorMessage(context: Context, sourceName: String, error: Throwable): String {
    val detail = errorDetail(error)
        ?: error.javaClass.simpleName
    return when {
        isMissingSourceCompatibilityClass(error) ->
            context.getString(R.string.source_picker_missing_class, sourceName)
        isDirectUrlRequiredError(error) ->
            context.getString(R.string.source_picker_direct_url, sourceName)
        isUnexpectedSourceResponseError(error) ->
            context.getString(R.string.source_picker_unexpected_response, sourceName)
        detail.contains("syntax error in regexp pattern", ignoreCase = true) ->
            context.getString(R.string.source_picker_regexp_error, sourceName)
        detail.contains("timeout", ignoreCase = true) || detail.contains("timed out", ignoreCase = true) ->
            context.getString(R.string.source_picker_timeout, sourceName)
        detail.contains("No readable manga found", ignoreCase = true) ->
            detail
        else -> context.getString(R.string.source_picker_failed_detail, sourceName, detail)
    }
}

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

internal fun sourcePickerDiagnosticDetail(context: Context, error: Throwable): String {
    val detail = errorDetail(error) ?: error.javaClass.simpleName
    return when {
        isMissingSourceCompatibilityClass(error) -> context.getString(R.string.source_picker_missing_class_short)
        isDirectUrlRequiredError(error) -> context.getString(R.string.source_picker_direct_url_short)
        isUnexpectedSourceResponseError(error) -> context.getString(R.string.source_picker_unexpected_response_short)
        detail.contains("HTTP error", ignoreCase = true) -> detail
        detail.contains("syntax error in regexp pattern", ignoreCase = true) -> context.getString(R.string.source_picker_regexp_short)
        detail.contains("timeout", ignoreCase = true) -> context.getString(R.string.source_picker_timeout_short)
        else -> detail.take(120)
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

private fun List<SourceSearchResult>.distinctSourceMatches(): List<SourceSearchResult> =
    distinctBy { match -> "${match.source.id}:${match.manga.url}" }

private fun sourcePickerSearchCompletedMessage(
    context: Context,
    matchCount: Int,
    editedTitle: String?,
): String =
    if (matchCount == 0) {
        editedTitle?.let { title ->
            context.getString(R.string.source_picker_no_matches_for_title, title)
        } ?: context.getString(R.string.source_picker_no_matches)
    } else {
        editedTitle?.let { title -> context.getString(R.string.source_picker_found_for_title, matchCount, title) }
            ?: context.getString(R.string.source_picker_found_readable, matchCount)
    }

private fun sourcePickerSearchCompletedMessage(
    matchCount: Int,
    editedTitle: String?,
): String =
    if (matchCount == 0) {
        editedTitle?.let { title ->
            "No readable matches found for \"$title\". Edit the search title or tap a source below to try it directly."
        } ?: "No readable matches found automatically. Edit the search title or tap a source below to try it directly."
    } else {
        editedTitle?.let { title -> "Found $matchCount readable sources for \"$title\"" }
            ?: "Found $matchCount readable sources"
    }

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
private const val SOURCE_PICKER_NO_SOURCES_MESSAGE = "Enable or install a source extension first"
