package com.tankobun.core.downloads

import com.tankobun.core.model.ReaderPage

data class SavedPageIndex(val index: Int, val imageUrl: String, val indexIsPosition: Boolean)

fun reusablePageIndexes(pages: List<ReaderPage>, saved: List<SavedPageIndex>): Set<Int> {
    val byPosition = pages.associateBy { it.index }
    // Older downloads keyed files by the extension's arbitrary index. Reuse
    // those only when both the order and unique image identities agree.
    val legacyOrderIsReliable = pages.all { it.sourcePageIndex == null || it.sourcePageIndex == it.index } &&
        pages.all { it.imageUrl.isNotBlank() } && pages.map { it.imageUrl }.distinct().size == pages.size
    return saved.filter { entry ->
        val page = byPosition[entry.index] ?: return@filter false
        entry.indexIsPosition || (legacyOrderIsReliable && entry.imageUrl == page.imageUrl)
    }.mapTo(mutableSetOf()) { it.index }
}
