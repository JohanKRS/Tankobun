package eu.kanade.tachiyomi.source.model

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.online.HttpSource
import org.junit.Assert.*
import org.junit.Test

class SourceUrlsTest {
    @Test fun removingDomainsPreservesRelativePathsAndEncodedIdentifiers() {
        for ((input, expected) in mapOf(
            "https://example.test/series/a%2Fb?key=a%2Bb#part" to "/series/a%2Fb?key=a%2Bb#part",
            "//example.test/series/a" to "/series/a",
            "series/a" to "series/a",
            "/series/a b" to "/series/a%20b",
            "?page=2" to "?page=2",
            "https://example.test" to "/",
        )) {
            val manga = SManga.create().apply { setUrlWithoutDomain(input) }
            val chapter = SChapter.create().apply { setUrlWithoutDomain(input) }
            assertEquals(expected, manga.url)
            assertEquals(expected, chapter.url)
        }
    }

    @Test fun defaultRequestsResolveProtocolRelativeAndParentPaths() {
        val source = object : HttpSource() {
            override val name = "Fixture"
            override val lang = "en"
            override val baseUrl = "https://example.test/root/"
        }
        assertEquals("https://cdn.example.test/image", source.imageRequest(Page(0, imageUrl = "//cdn.example.test/image")).url.toString())
        assertEquals("https://example.test/chapter", source.pageListRequest(SChapter.create().apply { url = "../chapter" }).url.toString())
        assertEquals("https://example.test/root/?page=2", source.mangaDetailsRequest(SManga.create().apply { url = "?page=2" }).url.toString())
    }

    @Test fun publicMangaUrlUsesTheExtensionsRequestOverride() {
        val source = object : HttpSource() {
            override val name = "Fixture"
            override val lang = "en"
            override val baseUrl = "https://example.test"
            override fun mangaDetailsRequest(manga: SManga) = GET("$baseUrl/details?id=${manga.url}")
            fun explicitId(name: String) = generateId(name, lang, 1)
        }
        assertEquals("https://example.test/details?id=42", source.getMangaUrl(SManga.create().apply { url = "42" }))
        assertEquals(source.explicitId("FIXTURE"), source.explicitId("fixture"))
    }
}
