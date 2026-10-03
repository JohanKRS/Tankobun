package com.tankobun.core.model

/**
 * Generic words for "chapter" or "episode" in the languages extensions publish in, longest first
 * so "chapter" wins over "ch". These describe chapter names, never particular sources.
 */
private val ChapterKeywords = listOf(
    "chapter", "chapitre", "chap", "capítulo", "capitulo", "capitolo", "cap", "kapitel", "kap",
    "episódio", "episodio", "episode", "épisode", "ch", "ep",
    "глава", "розділ", "rozdział", "rozdzial", "bölüm", "bolum", "chương", "bab", "ตอนที่", "الفصل",
)
private val KeywordChapterNumber = Regex(
    """(?<![\p{L}\d])(?:${ChapterKeywords.joinToString("|") { Regex.escape(it) }})\.?\s*#?\s*(\d+(?:\.\d+)?)(?![\p{L}\d]|\.\d)""",
    RegexOption.IGNORE_CASE,
)
// 第12話, 第12章, 12話 and the Korean 제12화 / 12화.
private val CjkChapterNumber = Regex("""(?:第|제)?\s*(\d+(?:\.\d+)?)\s*[話话章回화]""")
// A name that is nothing but the number: "12", "#12", "12.5".
private val BareChapterNumber = Regex("""^\s*#?\s*(\d+(?:\.\d+)?)\s*$""")

/**
 * Many extensions send -1 and leave the number to the app, while still naming the chapter
 * "Chapter 12". Only an explicit chapter keyword or a bare number counts; any other name stays
 * unnumbered, and a number the source did send is never replaced.
 */
fun SourceChapter.withRecognizedNumber(): SourceChapter {
    if (chapterNumber.isFinite() && chapterNumber >= 0f) return this
    if (!chapterNumberText.isNullOrBlank()) return this
    val match = KeywordChapterNumber.find(name) ?: CjkChapterNumber.find(name) ?: BareChapterNumber.find(name) ?: return this
    val number = match.groupValues[1].toFloatOrNull()?.takeIf { it.isFinite() } ?: return this
    return copy(chapterNumber = number)
}
