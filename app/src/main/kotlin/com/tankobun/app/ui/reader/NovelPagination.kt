package com.tankobun.app.ui.reader

/** Screen pages never replace the paragraph/character anchors saved in the library. */
internal data class NovelTextLine(val start: Int, val end: Int, val height: Float)
internal data class NovelMeasuredParagraph(val blockIndex: Int, val lines: List<NovelTextLine>)
internal data class NovelPagePart(val blockIndex: Int, val start: Int, val end: Int, val gapBefore: Float = 0f, val lineCount: Int = 0)
internal data class NovelTextPage(val parts: List<NovelPagePart>) {
    val blockIndex get() = parts.first().blockIndex
    val character get() = parts.first().start
}

/** Empty line lists represent illustrations and chapter-end markers, each on its own page. */
internal fun paginateNovel(
    paragraphs: List<NovelMeasuredParagraph>,
    height: Float,
    paragraphSpacing: Float,
): List<NovelTextPage> = buildList {
    val parts = mutableListOf<NovelPagePart>()
    var used = 0f
    fun flush() {
        if (parts.isNotEmpty()) add(NovelTextPage(parts.toList()))
        parts.clear()
        used = 0f
    }
    for (paragraph in paragraphs) {
        if (paragraph.lines.isEmpty()) {
            flush()
            add(NovelTextPage(listOf(NovelPagePart(paragraph.blockIndex, 0, 0))))
            continue
        }
        var line = 0
        while (line < paragraph.lines.size) {
            var gap = if (parts.isEmpty()) 0f else paragraphSpacing
            if (parts.isNotEmpty() && used + gap + paragraph.lines[line].height > height) {
                flush()
                gap = 0f
            }
            val first = line
            var chunkHeight = 0f
            do {
                chunkHeight += paragraph.lines[line].height
                line++
            } while (line < paragraph.lines.size && used + gap + chunkHeight + paragraph.lines[line].height <= height)
            parts += NovelPagePart(paragraph.blockIndex, paragraph.lines[first].start, paragraph.lines[line - 1].end, gap, line - first)
            used += gap + chunkHeight
            if (line < paragraph.lines.size) flush()
        }
    }
    flush()
}

internal fun List<NovelTextPage>.pageForAnchor(blockIndex: Int, character: Int): Int =
    indexOfLast { page -> page.blockIndex < blockIndex || (page.blockIndex == blockIndex && page.character <= character) }.coerceAtLeast(0)

internal fun novelColumnCount(twoPages: Boolean, widthDp: Float, heightDp: Float): Int =
    if (twoPages && widthDp >= 600f && widthDp > heightDp) 2 else 1
