package com.tankobun.app.logic

import com.tankobun.core.model.ReaderMode

/**
 * Pages the paged reader shows at once, by index in reading order: one page, or two side by side.
 * Page turns move from spread to spread, and the scrubber points at [first].
 */
internal data class ReaderSpread(val first: Int, val last: Int) {
    val isDouble: Boolean
        get() = last > first

    operator fun contains(pageIndex: Int): Boolean = pageIndex in first..last
}

/** Spreads only make sense in paged reading on a window wider than tall, and only when the reader wants them. */
internal fun readerShowsSpreads(
    enabled: Boolean,
    readerMode: ReaderMode,
    windowWidth: Int,
    windowHeight: Int,
): Boolean = enabled && readerMode == ReaderMode.PAGED && windowWidth > windowHeight

/**
 * Groups a chapter's pages into spreads. Without [pairPages] every page stands alone.
 *
 * When pairing, the first page stands alone because it is usually a cover, and a page whose known
 * aspect ratio is wide stands alone because it is already a two-page spread scanned as one image.
 * The remaining pages pair in reading order, and a page left without a partner stands alone.
 * Pages whose size is still unknown are assumed to be ordinary single pages.
 */
internal fun readerPageSpreads(
    pageCount: Int,
    pairPages: Boolean,
    aspectRatioAt: (pageIndex: Int) -> Float?,
): List<ReaderSpread> {
    if (pageCount <= 0) return emptyList()
    if (!pairPages) return List(pageCount) { ReaderSpread(it, it) }
    fun isWide(pageIndex: Int): Boolean = (aspectRatioAt(pageIndex) ?: 0f) > 1f
    val spreads = ArrayList<ReaderSpread>(pageCount)
    var pageIndex = 0
    while (pageIndex < pageCount) {
        val pairs = pageIndex > 0 &&
            pageIndex + 1 < pageCount &&
            !isWide(pageIndex) &&
            !isWide(pageIndex + 1)
        val last = if (pairs) pageIndex + 1 else pageIndex
        spreads += ReaderSpread(pageIndex, last)
        pageIndex = last + 1
    }
    return spreads
}

/** Position of the spread showing [pageIndex]; pages outside the chapter clamp to the first or last spread. */
internal fun List<ReaderSpread>.indexOfSpreadContaining(pageIndex: Int): Int {
    if (isEmpty()) return -1
    val found = binarySearch { spread ->
        when {
            spread.last < pageIndex -> -1
            spread.first > pageIndex -> 1
            else -> 0
        }
    }
    return when {
        found >= 0 -> found
        pageIndex < first().first -> 0
        else -> lastIndex
    }
}

/** The spread's pages from the left edge of the screen to the right; right-to-left reading puts the earlier page on the right. */
internal fun ReaderSpread.screenOrder(rightToLeft: Boolean): List<Int> {
    val pages = (first..last).toList()
    return if (rightToLeft) pages.asReversed() else pages
}

/** Width over height of the spread's pages laid side by side at the same height, or null while any size is unknown. */
internal fun ReaderSpread.combinedAspectRatio(aspectRatioAt: (pageIndex: Int) -> Float?): Float? {
    var total = 0f
    for (pageIndex in first..last) {
        total += aspectRatioAt(pageIndex)?.takeIf { it > 0f } ?: return null
    }
    return total
}
