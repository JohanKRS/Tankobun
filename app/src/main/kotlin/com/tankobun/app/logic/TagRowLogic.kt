package com.tankobun.app.logic

/** One tag in a justified row: which item, and the width it is laid out at. */
internal data class TagSlot(val index: Int, val width: Int)

/**
 * Packs tags into rows that leave as little ragged space as possible. A tag that does not fit
 * lets a later one that does take its place, so order shifts only where a gap would otherwise
 * open. Full rows then stretch every tag equally to reach both edges; the last row only does
 * when it is already mostly full, like the last line of justified text.
 */
internal fun justifiedTagRows(
    widths: List<Int>,
    maxWidth: Int,
    spacing: Int,
    lastRowFillToJustify: Float = 0.7f,
): List<List<TagSlot>> {
    if (maxWidth <= 0) return widths.indices.map { listOf(TagSlot(it, 0)) }
    val remaining = widths.indices.toMutableList()
    val rows = mutableListOf<List<Int>>()
    while (remaining.isNotEmpty()) {
        val row = mutableListOf<Int>()
        var used = 0
        val iterator = remaining.iterator()
        while (iterator.hasNext()) {
            val index = iterator.next()
            val width = widths[index].coerceIn(0, maxWidth)
            val needed = if (row.isEmpty()) width else used + spacing + width
            if (needed <= maxWidth) {
                row += index
                used = needed
                iterator.remove()
            }
        }
        rows += row
    }
    return rows.mapIndexed { rowIndex, row ->
        val naturalWidths = row.map { widths[it].coerceIn(0, maxWidth) }
        val used = naturalWidths.sum() + spacing * (row.size - 1)
        val extra = maxWidth - used
        val justify = rowIndex < rows.lastIndex || used >= maxWidth * lastRowFillToJustify
        row.mapIndexed { position, index ->
            val grow = if (justify && extra > 0) extra / row.size + if (position < extra % row.size) 1 else 0 else 0
            TagSlot(index, naturalWidths[position] + grow)
        }
    }
}
