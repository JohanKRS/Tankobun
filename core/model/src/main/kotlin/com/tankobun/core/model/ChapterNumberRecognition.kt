package com.tankobun.core.model

// Longer keywords first, so "chapter" wins over "ch" and "episode" over "ep".
private val KeywordChapterNumber = Regex(
    """\b(?:chapter|chapitre|chap|cap[ií]tulo|cap|epis[oó]dio|episode|ch|ep)\.?\s*#?\s*(\d+(?:\.\d+)?)(?![\p{L}\d]|\.\d)""",
    RegexOption.IGNORE_CASE,
)
private val CjkChapterNumber = Regex("""第\s*(\d+(?:\.\d+)?)\s*[話话章回]""")

/**
 * Many extensions send -1 and leave the number to the app, while still naming the chapter
 * "Chapter 12". Only an explicit chapter keyword counts; any other name stays unnumbered,
 * and a number the source did send is never replaced.
 */
fun SourceChapter.withRecognizedNumber(): SourceChapter {
    if (chapterNumber.isFinite() && chapterNumber >= 0f) return this
    if (!chapterNumberText.isNullOrBlank()) return this
    val match = KeywordChapterNumber.find(name) ?: CjkChapterNumber.find(name) ?: return this
    val number = match.groupValues[1].toFloatOrNull()?.takeIf { it.isFinite() } ?: return this
    return copy(chapterNumber = number)
}
