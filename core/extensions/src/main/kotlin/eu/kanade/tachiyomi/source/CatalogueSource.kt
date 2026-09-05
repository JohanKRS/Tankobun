package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import com.tankobun.core.extensions.awaitSourceValue
import rx.Observable

interface CatalogueSource : Source {
    override val supportsLatest: Boolean
        get() = false

    override suspend fun getPopularManga(page: Int): MangasPage = fetchPopularManga(page).awaitSourceValue()

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        fetchSearchManga(page, query, filters).awaitSourceValue()

    override suspend fun getLatestUpdates(page: Int): MangasPage = fetchLatestUpdates(page).awaitSourceValue()

    fun fetchPopularManga(page: Int): Observable<MangasPage> =
        throw UnsupportedOperationException()

    fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        throw UnsupportedOperationException()

    fun fetchLatestUpdates(page: Int): Observable<MangasPage> =
        throw UnsupportedOperationException()
}
