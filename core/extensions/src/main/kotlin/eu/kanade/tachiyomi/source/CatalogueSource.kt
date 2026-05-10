package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import kotlinx.coroutines.runBlocking
import rx.Observable

interface CatalogueSource : Source {
    override val supportsLatest: Boolean
        get() = false

    fun fetchPopularManga(page: Int): Observable<MangasPage> =
        Observable.fromCallable { runBlocking { getPopularManga(page) } }

    fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        Observable.fromCallable { runBlocking { getSearchManga(page, query, filters) } }

    fun fetchLatestUpdates(page: Int): Observable<MangasPage> =
        Observable.fromCallable { runBlocking { getLatestUpdates(page) } }
}
