package com.tankobun.core.extensions

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import kotlinx.coroutines.*
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.*
import org.jsoup.nodes.Element
import org.junit.Assert.*
import org.junit.Test
import rx.Observable
import rx.subscriptions.Subscriptions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SourceContractTest {
    @Test
    fun legacyCatalogueIsAccessibleThroughSuspendApi() = runBlocking {
        val searches = AtomicInteger()
        val chapters = AtomicInteger()
        val legacy = object : CatalogueSource {
            override val id = 1L
            override val name = "Fixture"
            override val lang = "en"
            override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
                Observable.fromCallable {
                    searches.incrementAndGet()
                    MangasPage(listOf(SManga.create().apply { title = query }), false)
                }
            override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> = Observable.fromCallable {
                chapters.incrementAndGet()
                emptyList()
            }
        }
        assertEquals("query", legacy.getSearchManga(1, "query", FilterList()).mangas.single().title)
        assertTrue(legacy.getMangaUpdate(SManga.create(), emptyList(), false, true).chapters.isEmpty())
        assertEquals(1, searches.get())
        assertEquals(1, chapters.get())
    }

    @Test
    fun suspendOnlySourceDoesNotInvokeLegacyRequestBuilders() = runBlocking {
        val modern = object : HttpSource() {
            override val baseUrl = "https://example.test"
            override val name = "Fixture"
            override val lang = "en"
            override suspend fun getSearchManga(page: Int, query: String, filters: FilterList) = MangasPage(emptyList(), false)
            override suspend fun getMangaUpdate(manga: SManga, chapters: List<SChapter>, fetchDetails: Boolean, fetchChapters: Boolean) =
                SMangaUpdate(manga, chapters)
            override suspend fun getPageList(chapter: SChapter) = listOf(Page(0, imageUrl = "https://example.test/image"))
        }
        assertTrue(modern.getSearchManga(1, "query", FilterList()).mangas.isEmpty())
        assertTrue(modern.getMangaUpdate(SManga.create(), emptyList(), true, true).chapters.isEmpty())
        assertEquals(1, modern.getPageList(SChapter.create()).size)
    }

    @Test
    fun missingImplementationFailsWithoutRecursiveBridges() = runBlocking {
        val source = object : CatalogueSource {
            override val id = 1L
            override val name = "Fixture"
            override val lang = "en"
        }
        val failure = runCatching { source.getPageList(SChapter.create()) }.exceptionOrNull()
        assertTrue(failure is UnsupportedOperationException)
    }

    @Test
    fun httpFailureIsNotPassedToParserOrRetriedByBridge() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().code(429).addHeader("Retry-After", "60").body("limited").build())
            val parses = AtomicInteger()
            val source = object : HttpSource() {
                override val client = OkHttpClient()
                override val baseUrl = server.url("/").toString()
                override val name = "Fixture"
                override val lang = "en"
                override fun searchMangaRequest(page: Int, query: String, filters: FilterList) = GET(baseUrl)
                override fun searchMangaParse(response: Response): MangasPage {
                    parses.incrementAndGet()
                    return MangasPage(emptyList(), false)
                }
            }
            val failure = runCatching { source.getSearchManga(1, "query", FilterList()) }.exceptionOrNull()
            assertEquals(429, (failure as HttpException).code)
            assertEquals(1, server.requestCount)
            assertEquals(0, parses.get())
        }
    }

    @Test
    fun parsedSourcesAcceptNullAndEmptyNextPageSelectors() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val source = object : ParsedHttpSource() {
                override val client = OkHttpClient()
                override val baseUrl = server.url("/").toString()
                override val name = "Fixture"
                override val lang = "en"
                override fun popularMangaRequest(page: Int) = GET(baseUrl)
                override fun popularMangaSelector() = "a.title"
                override fun popularMangaFromElement(element: Element) = SManga.create().apply { title = element.text() }
                override fun searchMangaRequest(page: Int, query: String, filters: FilterList) = GET(baseUrl)
                override fun searchMangaNextPageSelector() = ""
            }
            repeat(2) { server.enqueue(MockResponse.Builder().body("<a class='title'>Local fixture</a>").build()) }
            assertFalse(source.getPopularManga(1).hasNextPage)
            assertFalse(source.getSearchManga(1, "query", FilterList()).hasNextPage)
        }
    }

    @Test
    fun cancellingLegacyOperationUnsubscribesItsWork() = runBlocking {
        val started = CountDownLatch(1)
        val unsubscribed = CountDownLatch(1)
        val observable = Observable.create<String> { subscriber ->
            subscriber.add(Subscriptions.create { unsubscribed.countDown() })
            started.countDown()
        }
        val job = launch(Dispatchers.Default) { observable.awaitSourceValue() }
        assertTrue(started.await(3, TimeUnit.SECONDS))
        withTimeout(3_000) { job.cancelAndJoin() }
        assertTrue(unsubscribed.await(3, TimeUnit.SECONDS))
    }

    @Test
    fun cancellingHttpParseCancelsTheNetworkCall() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().bodyDelay(30, TimeUnit.SECONDS).body("slow body").build())
            val started = CountDownLatch(1)
            lateinit var call: Call
            val source = object : HttpSource() {
                override val baseUrl = server.url("/").toString()
                override val name = "Fixture"
                override val lang = "en"
                override val client = OkHttpClient.Builder().eventListener(object : EventListener() {
                    override fun callStart(startingCall: Call) { call = startingCall }
                }).build()
                override fun searchMangaRequest(page: Int, query: String, filters: FilterList) = GET(baseUrl)
                override fun searchMangaParse(response: Response): MangasPage {
                    started.countDown()
                    response.body.string()
                    return MangasPage(emptyList(), false)
                }
            }
            val job = launch(Dispatchers.Default) { source.getSearchManga(1, "query", FilterList()) }
            assertTrue(started.await(3, TimeUnit.SECONDS))
            withTimeout(3_000) { job.cancelAndJoin() }
            assertTrue(call.isCanceled())
        }
    }
}
