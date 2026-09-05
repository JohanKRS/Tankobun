package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.model.Page
import com.tankobun.core.extensions.awaitSourceValue
import rx.Observable

interface Source {
    val id: Long
    val name: String
    val lang: String get() = ""
    val supportsLatest: Boolean
        get() = false

    fun getFilterList(): FilterList = FilterList()

    suspend fun getPopularManga(page: Int): MangasPage =
        throw UnsupportedOperationException("Popular manga is not supported by $name")

    suspend fun getLatestUpdates(page: Int): MangasPage =
        throw UnsupportedOperationException("Latest updates are not supported by $name")

    suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        throw UnsupportedOperationException("Search is not supported by $name")

    suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val details = if (fetchDetails) fetchMangaDetails(manga).awaitSourceValue() else manga
        val nextChapters = if (fetchChapters) fetchChapterList(manga).awaitSourceValue() else chapters
        return SMangaUpdate(details, nextChapters)
    }

    suspend fun getPageList(chapter: SChapter): List<Page> =
        fetchPageList(chapter).awaitSourceValue()

    fun fetchMangaDetails(manga: SManga): Observable<SManga> =
        throw UnsupportedOperationException()

    fun fetchChapterList(manga: SManga): Observable<List<SChapter>> =
        throw UnsupportedOperationException()

    fun fetchPageList(chapter: SChapter): Observable<List<Page>> =
        throw UnsupportedOperationException()
}

interface SourceFactory {
    fun createSources(): List<Source>
}
