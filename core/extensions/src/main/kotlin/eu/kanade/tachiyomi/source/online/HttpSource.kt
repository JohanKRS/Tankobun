package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import com.tankobun.core.extensions.awaitSourceValue
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import java.math.BigInteger
import java.security.MessageDigest

abstract class HttpSource : CatalogueSource {
    abstract val baseUrl: String
    open fun getHomeUrl(): String = baseUrl
    open val versionId: Int = 1
    override val id: Long by lazy { generatedSourceId(name, lang, versionId) }
    override val supportsLatest: Boolean = true

    open val network: NetworkHelper = NetworkHelper()
    open val client: OkHttpClient get() = network.client
    open val headers: Headers by lazy {
        ensureHttpAgent()
        headersBuilder().build()
    }

    open fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", NetworkHelper.defaultUserAgent())

    override suspend fun getPopularManga(page: Int): MangasPage =
        fetchPopularManga(page).awaitSourceValue()

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        fetchSearchManga(page, query, filters).awaitSourceValue()

    override suspend fun getLatestUpdates(page: Int): MangasPage =
        fetchLatestUpdates(page).awaitSourceValue()

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val details = if (fetchDetails) fetchMangaDetails(manga).awaitSourceValue() else manga
        val nextChapters = if (fetchChapters) fetchChapterList(manga).awaitSourceValue() else chapters
        return SMangaUpdate(details, nextChapters)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> =
        fetchPageList(chapter).awaitSourceValue()

    open suspend fun getImageUrl(page: Page): String =
        fetchImageUrl(page).awaitSourceValue()

    override fun fetchPopularManga(page: Int): Observable<MangasPage> =
        Observable.defer {
            client.newCall(popularMangaRequest(page)).asObservableSuccess()
                .map { response -> response.use(::popularMangaParse) }
        }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        Observable.defer {
            client.newCall(searchMangaRequest(page, query, filters)).asObservableSuccess()
                .map { response -> response.use(::searchMangaParse) }
        }

    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> =
        Observable.defer {
            client.newCall(latestUpdatesRequest(page)).asObservableSuccess()
                .map { response -> response.use(::latestUpdatesParse) }
        }

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> =
        Observable.defer {
            client.newCall(mangaDetailsRequest(manga)).asObservableSuccess()
                .map { response -> response.use(::mangaDetailsParse).apply { initialized = true } }
        }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> =
        Observable.defer {
            client.newCall(chapterListRequest(manga)).asObservableSuccess()
                .map { response -> response.use(::chapterListParse) }
        }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> =
        Observable.defer {
            client.newCall(pageListRequest(chapter)).asObservableSuccess()
                .map { response -> response.use(::pageListParse) }
        }

    open fun fetchImageUrl(page: Page): Observable<String> =
        Observable.defer {
            client.newCall(imageUrlRequest(page)).asObservableSuccess()
                .map { response -> response.use(::imageUrlParse) }
        }

    open fun fetchImage(page: Page): Observable<Response> =
        Observable.defer {
            client.newCall(imageRequest(page)).asObservable()
        }

    open fun popularMangaRequest(page: Int): Request = throw UnsupportedOperationException()
    open fun popularMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()
    open fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException()
    open fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()
    open fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()
    open fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException()
    open fun mangaDetailsRequest(manga: SManga): Request = eu.kanade.tachiyomi.network.GET(getMangaUrl(manga), headers)
    open fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException()
    open fun chapterListRequest(manga: SManga): Request = eu.kanade.tachiyomi.network.GET(getMangaUrl(manga), headers)
    open fun chapterListParse(response: Response): List<SChapter> = throw UnsupportedOperationException()
    open fun pageListRequest(chapter: SChapter): Request = eu.kanade.tachiyomi.network.GET(getChapterUrl(chapter), headers)
    open fun pageListParse(response: Response): List<Page> = throw UnsupportedOperationException()
    open fun imageUrlRequest(page: Page): Request = eu.kanade.tachiyomi.network.GET(page.url, headers)
    open fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()
    open fun imageRequest(page: Page): Request = eu.kanade.tachiyomi.network.GET(page.imageUrl ?: page.url, headers)
    override fun getFilterList(): FilterList = FilterList()

    open fun getMangaUrl(manga: SManga): String = absoluteUrl(manga.url)

    open fun getChapterUrl(chapter: SChapter): String = absoluteUrl(chapter.url)

    open fun prepareNewChapter(chapter: SChapter, manga: SManga) = Unit

    open fun setUrlWithoutDomain(manga: SManga, url: String) {
        manga.setUrlWithoutDomain(url)
    }

    open fun setUrlWithoutDomain(chapter: SChapter, url: String) {
        chapter.setUrlWithoutDomain(url)
    }

    override fun toString(): String = name

    private fun absoluteUrl(url: String): String =
        if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            baseUrl.trimEnd('/') + "/" + url.trimStart('/')
        }
}

private fun generatedSourceId(name: String, lang: String, versionId: Int): Long {
    val key = "$name/$lang/$versionId"
    val digest = MessageDigest.getInstance("MD5").digest(key.toByteArray())
    return BigInteger(1, digest.copyOfRange(0, 8)).toLong() and Long.MAX_VALUE
}

private fun ensureHttpAgent() {
    if (System.getProperty("http.agent").isNullOrBlank()) {
        System.setProperty("http.agent", NetworkHelper.defaultUserAgent())
    }
}
