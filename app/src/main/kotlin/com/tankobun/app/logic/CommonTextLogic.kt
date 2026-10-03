package com.tankobun.app.logic

import java.util.Locale

internal fun Iterable<String>.normalizedCustomLists(): List<String> =
    map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase(Locale.ROOT) }


private val HtmlBreak = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val HtmlTag = Regex("<[^>]*>")
private val MarkdownLink = Regex("""\[([^\]]*)]\((?:https?://|www\.)[^)\s]*\)""")
private val MarkdownEmphasis = Regex("""(\*\*|__|\*|~~)(?=\S)(.+?)(?<=\S)\1""")
private val MarkdownLinePrefix = Regex("""^\s{0,3}(#{1,6}\s+|>\s?|[-*+]\s+)""")
private val MarkdownRule = Regex("""^\s*([-*_]\s*){3,}$""")
private val Whitespace = Regex("\\s+")

/**
 * Catalog synopses as one plain paragraph. AniList sends HTML and MangaBaka sends Markdown,
 * often with lines that only link to the publisher; those carry no synopsis and are dropped.
 */
internal fun String?.plainMediaDescription(): String {
    if (this == null) return ""
    val lines = replace(HtmlBreak, "\n")
        .replace(HtmlTag, "")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&amp;", "&")
        .lines()
        .filterNot { MarkdownRule.matches(it) }
        .filterNot { line -> MarkdownLink.containsMatchIn(line) && line.replace(MarkdownLink, "").none { it.isLetterOrDigit() } }
        .map { line ->
            line.replace(MarkdownLinePrefix, "")
                .replace(MarkdownLink) { it.groupValues[1] }
                .replace(MarkdownEmphasis) { it.groupValues[2] }
                .trim()
        }
        .filter { it.isNotEmpty() }
        // A label left without the links it introduced ("Original Webtoon:") says nothing on its own.
        .dropLastWhile { it.endsWith(':') }
    return lines.joinToString(" ").replace(Whitespace, " ").trim()
}
