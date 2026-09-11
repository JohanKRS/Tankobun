package com.tankobun.core.model

enum class ReadingContentKind { MANGA, NOVEL }

val AnilistMedia.readingContentKind: ReadingContentKind
    get() = if (format.equals("NOVEL", ignoreCase = true)) ReadingContentKind.NOVEL else ReadingContentKind.MANGA

fun SourceDescriptor.supports(media: AnilistMedia): Boolean = contentKind == media.readingContentKind

/** A stable paragraph/illustration anchor, independent of font size and screen dimensions. */
@kotlinx.serialization.Serializable
data class NovelBlock(
    val html: String = "",
    val text: String = "",
    val anchor: String? = null,
    val headingLevel: Int = 0,
    val endOfChapter: Boolean = false,
)

enum class NovelFont { SERIF, SANS_SERIF, MONOSPACE }
enum class NovelTheme { SYSTEM, LIGHT, SEPIA, DARK, BLACK }
enum class NovelReadingMode { SCROLL, PAGED }

@kotlinx.serialization.Serializable
data class NovelReaderPreferences(
    val font: NovelFont = NovelFont.SERIF,
    val theme: NovelTheme = NovelTheme.SYSTEM,
    val fontSize: Int = 20,
    val lineHeightPercent: Int = 160,
    val paragraphSpacing: Int = 12,
    val margin: Int = 20,
    val justified: Boolean = false,
    val keepScreenOn: Boolean = true,
    val readingMode: NovelReadingMode = NovelReadingMode.SCROLL,
    val continuousReading: Boolean = true,
    val maxTextWidth: Int = 720,
    val landscapeTwoPages: Boolean = true,
) {
    fun normalized() = copy(
        fontSize = fontSize.coerceIn(14, 36),
        lineHeightPercent = lineHeightPercent.coerceIn(120, 220),
        paragraphSpacing = paragraphSpacing.coerceIn(0, 32),
        margin = margin.coerceIn(8, 48),
        maxTextWidth = maxTextWidth.coerceIn(400, 1000),
    )
}
