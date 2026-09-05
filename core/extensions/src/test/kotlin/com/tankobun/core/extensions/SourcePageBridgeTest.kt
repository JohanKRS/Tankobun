package com.tankobun.core.extensions

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.*
import org.junit.Test

class SourcePageBridgeTest {
    @Test fun imageRetriesRecognizeExtensionHttpErrorsWithoutRetryingParserFailures() {
        assertTrue(eu.kanade.tachiyomi.network.HttpException(429).isTransientSourceImageFailure())
        assertTrue(eu.kanade.tachiyomi.network.HttpException(503).isTransientSourceImageFailure())
        assertFalse(eu.kanade.tachiyomi.network.HttpException(404).isTransientSourceImageFailure())
        assertFalse(IllegalStateException("parser failure").isTransientSourceImageFailure())
    }

    @Test fun readerOrderIsIndependentOfExtensionPageIndexes() {
        val pages = listOf(Page(9, "", "https://example.test/first"), Page(9, "second"), Page(-1, "third"))
        val reader = pages.toReaderPages()
        assertEquals(listOf(0, 1, 2), reader.map { it.index })
        assertEquals(listOf(9, 9, -1), reader.map { it.toSourcePage().index })
        assertEquals("", reader.first().toSourcePage().url)
        assertNull(reader[1].toSourcePage().imageUrl)
        assertFalse(reader[1].imageUrlResolved)
    }

    @Test fun extensionBuildsItsSignedPostRequestExactlyOnce() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body("image fixture").build())
            var requests = 0
            val source = object : HttpSource() {
                override val name = "Fixture"
                override val lang = "en"
                override val baseUrl = server.url("/").toString()
                override val client = OkHttpClient()
                override suspend fun getPageList(chapter: SChapter) = listOf(Page(12, "", "original-image-key"))
                override fun imageRequest(page: Page): Request {
                    requests++
                    assertEquals(12, page.index)
                    assertEquals("", page.url)
                    return Request.Builder().url(server.url("/image?signature=$requests"))
                        .header("X-Fixture-Header", "preserved")
                        .post(page.imageUrl!!.toRequestBody()).build()
                }
            }
            val page = source.getPageList(SChapter.create()).toReaderPages().single()
            assertEquals(0, requests)
            assertEquals("image fixture", source.fetchImage(page.toSourcePage()).map { it.use { it.body.string() } }.awaitSourceValue())
            assertEquals(1, requests)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("original-image-key", request.body!!.utf8())
            assertEquals("preserved", request.headers["X-Fixture-Header"])
            assertEquals("/image?signature=1", request.target)
        }
    }
}
