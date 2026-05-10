package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

abstract class ParsedHttpSource : HttpSource() {
    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(popularMangaSelector()).map(::popularMangaFromElement)
        return MangasPage(mangas, document.selectFirst(popularMangaNextPageSelector()) != null)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(searchMangaSelector()).map(::searchMangaFromElement)
        return MangasPage(mangas, document.selectFirst(searchMangaNextPageSelector()) != null)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(latestUpdatesSelector()).map(::latestUpdatesFromElement)
        return MangasPage(mangas, document.selectFirst(latestUpdatesNextPageSelector()) != null)
    }

    override fun mangaDetailsParse(response: Response): SManga = mangaDetailsParse(response.asJsoup())

    override fun chapterListParse(response: Response): List<SChapter> {
        return response.asJsoup().select(chapterListSelector()).map(::chapterFromElement)
    }

    override fun pageListParse(response: Response): List<Page> = pageListParse(response.asJsoup())

    override fun imageUrlParse(response: Response): String = imageUrlParse(response.asJsoup())

    protected open fun popularMangaSelector(): String = throw UnsupportedOperationException()
    protected open fun popularMangaFromElement(element: Element): SManga = throw UnsupportedOperationException()
    protected open fun popularMangaNextPageSelector(): String = throw UnsupportedOperationException()
    protected open fun searchMangaSelector(): String = popularMangaSelector()
    protected open fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)
    protected open fun searchMangaNextPageSelector(): String = popularMangaNextPageSelector()
    protected open fun latestUpdatesSelector(): String = popularMangaSelector()
    protected open fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)
    protected open fun latestUpdatesNextPageSelector(): String = popularMangaNextPageSelector()
    protected open fun mangaDetailsParse(document: Document): SManga = throw UnsupportedOperationException()
    protected open fun chapterListSelector(): String = throw UnsupportedOperationException()
    protected open fun chapterFromElement(element: Element): SChapter = throw UnsupportedOperationException()
    protected open fun pageListParse(document: Document): List<Page> = throw UnsupportedOperationException()
    protected open fun imageUrlParse(document: Document): String = throw UnsupportedOperationException()
}
