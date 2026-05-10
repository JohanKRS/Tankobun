package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.coroutines.runBlocking
import rx.Observable

interface Source {
    val id: Long
    val name: String
    val lang: String
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
        val details = if (fetchDetails) fetchMangaDetails(manga).toBlocking().first() else manga
        val nextChapters = if (fetchChapters) fetchChapterList(manga).toBlocking().first() else chapters
        return SMangaUpdate(details, nextChapters)
    }

    suspend fun getPageList(chapter: SChapter): List<Page> =
        fetchPageList(chapter).toBlocking().first()

    fun fetchMangaDetails(manga: SManga): Observable<SManga> =
        Observable.fromCallable {
            runBlocking {
                getMangaUpdate(manga, emptyList(), fetchDetails = true, fetchChapters = false).manga
            }
        }

    fun fetchChapterList(manga: SManga): Observable<List<SChapter>> =
        Observable.fromCallable {
            runBlocking {
                getMangaUpdate(manga, emptyList(), fetchDetails = false, fetchChapters = true).chapters
            }
        }

    fun fetchPageList(chapter: SChapter): Observable<List<Page>> =
        Observable.fromCallable { runBlocking { getPageList(chapter) } }
}

interface SourceFactory {
    fun createSources(): List<Source>
}
