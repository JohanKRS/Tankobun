package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.Page

/** Compatibility contract for installed text extensions; contains no source implementation. */
interface NovelSource : Source {
    override val isNovelSource: Boolean get() = true
    override suspend fun fetchPageText(page: Page): String
}
