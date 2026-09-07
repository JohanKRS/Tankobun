package com.tankobun.core.mangabaka

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test

class MangaBakaRepositoryTest {
    private fun response(body: String) = MockResponse.Builder().code(200).body(body).build()
    private fun repository(server: MockWebServer) = MangaBakaRepository(OkHttpClient(), baseUrl = server.url("/").toString())

    @Test fun independentIdentityAndMissingArtworkArePreserved() {
        val media = MangaBakaMapper.media(Json.parseToJsonElement(series(82, "null")).jsonObject)!!
        assertEquals(-82, media.id)
        assertEquals(82, media.mangaBakaId)
        assertNull(media.anilistId)
        assertNull(media.bannerImage)
        assertTrue(media.characterImages.isEmpty())
        assertNull(media.popularity)
        assertEquals("Fictional story", media.title.english)
    }

    @Test fun linkedRecordKeepsTheAniListKey() {
        val media = MangaBakaMapper.media(Json.parseToJsonElement(series(82, "123")).jsonObject)!!
        assertEquals(123, media.id)
        assertEquals(123, media.anilistId)
    }

    @Test fun pageMetadataAndSafeFiltersComeFromTheContract() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":[${series(82, "null")}],"pagination":{"next":"https://example.test/page2","count":21,"page":1,"limit":20}}"""))
            val result = repository(server).search("fiction", limit = 20)
            assertTrue(result.hasNextPage)
            assertEquals(listOf(-82), result.media.map { it.id })
            val request = server.takeRequest()
            assertTrue(request.target.contains("content_rating=safe"))
            assertTrue(request.target.contains("content_rating=suggestive"))
            assertTrue(request.target.contains("schema=full"))
        }
    }

    @Test fun emptyResultsAreCachedAndSimultaneousRequestsCoalesce() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":[],"pagination":{"next":null}}"""))
            val repo = repository(server)
            val first = async { repo.search("missing") }
            val second = async { repo.search("missing") }
            assertFalse(first.await().hasNextPage)
            assertTrue(second.await().media.isEmpty())
            repo.search("missing")
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun manualCleanupForcesAFreshCatalogRequest() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":[],"pagination":{"next":null}}"""))
            server.enqueue(response("""{"data":[${series(82, "null")}],"pagination":{"next":null}}"""))
            val repo = repository(server)
            assertTrue(repo.search("fiction").media.isEmpty())
            repo.clearCache()
            assertEquals(listOf(-82), repo.search("fiction").media.map { it.id })
            assertEquals(2, server.requestCount)
        }
    }

    @Test fun mixRefreshBypassesItsCacheWithoutEvictingOtherCatalogData() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":[],"pagination":{"next":null}}"""))
            server.enqueue(response("""{"data":[{"series":${series(82, "null")}}]}"""))
            server.enqueue(response("""{"data":[{"series":${series(83, "null")}}]}"""))
            val repo = repository(server)
            repo.search("fiction")
            assertEquals(listOf(-82), repo.mix(listOf(1), false).map { it.id })
            assertEquals(listOf(-82), repo.mix(listOf(1), false).map { it.id })
            assertEquals(listOf(-83), repo.mix(listOf(1), false, forceRefresh = true).map { it.id })
            assertEquals(listOf(-83), repo.mix(listOf(1), false).map { it.id })
            repo.search("fiction")
            assertEquals(3, server.requestCount)
            server.takeRequest()
            server.takeRequest()
            assertEquals("no-cache", server.takeRequest().headers["Cache-Control"])
        }
    }

    @Test fun ambiguousMappingsDoNotMergeWorks() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":{"series":[${series(1, "123")},${series(2, "123")}]}}"""))
            assertNull(repository(server).findByAniList(123))
        }
    }

    @Test fun adultUnknownAndDeletedRecordsStayOutOfSafeSearch() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":[${series(1,"null").replace("safe","erotica")},${series(2,"null").replace("active","deleted")},${series(3,"null").replace("safe","unknown")}],"pagination":{"next":null}}"""))
            assertTrue(repository(server).search("fiction").media.isEmpty())
        }
    }

    @Test fun trackingUsesAtomicUpsertAndDoesNotOverwriteUnsupportedFields() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":[]}"""))
            repository(server).updateTracking("test-token", 82, """{"progress_chapter":8,"state":"reading"}""")
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/v1/my/library/batch", request.target)
            assertEquals("Bearer test-token", request.headers["Authorization"])
            assertTrue(request.headers["Cache-Control"]!!.contains("no-store"))
            val entry = Json.parseToJsonElement(request.body!!.utf8()).jsonArray.single().jsonObject
            assertEquals(82, entry["series_id"]!!.jsonPrimitive.int)
            assertFalse(entry.containsKey("start_date"))
            assertFalse(entry.containsKey("rating"))
        }
    }

    @Test fun portraitAndAdultBannersDoNotBreakSafePresentation() {
        val data = Json.parseToJsonElement("""{"data":[
            {"type":"banner","content_rating":"safe","image":{"raw":{"width":600,"height":900,"url":"https://example.test/portrait"}}},
            {"type":"banner","content_rating":"erotica","image":{"raw":{"width":1800,"height":600,"url":"https://example.test/adult"}}},
            {"type":"banner","content_rating":"safe","image":{"raw":{"width":1800,"height":600,"url":"https://example.test/banner"}}}
        ]}""").jsonObject
        assertEquals("https://example.test/banner", MangaBakaMapper.banner(data, false))
    }

    @Test fun mixReadsRankedSeriesAndExcludesSeedsAndUnsafeWorks() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":[
                {"score":0.98,"series":${series(82,"null")}},
                {"score":0.9,"series":${series(83,"123")}},
                {"score":0.8,"series":${series(84,"null").replace("safe","erotica")}},
                {"score":0.7,"series":${series(83,"123")}}
            ]}"""))
            val media = repository(server).mix(listOf(82, 82, 81, -1), false)
            assertEquals(listOf(123), media.map { it.id })
            val target = server.takeRequest().target
            assertTrue(target.startsWith("/v2/series/mix?"))
            assertTrue(target.contains("series=82"))
            assertTrue(target.contains("series=81"))
            assertTrue(target.contains("strict=true"))
            assertTrue(target.contains("schema=full"))
            assertTrue(target.contains("content_rating=safe"))
            assertFalse(target.contains("page="))
        }
    }

    @Test fun similarityIsAOnePageRankedListAndEmptyMixNeedsNoRequest() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":[{"score":0.75,"series":${series(83,"null")}}]}"""))
            val repo = repository(server)
            assertTrue(repo.mix(emptyList(), false).isEmpty())
            assertTrue(repo.recommendations(82, 2, false).recommendations.isEmpty())
            assertEquals(0, server.requestCount)
            val page = repo.recommendations(82, 1, false)
            assertEquals(listOf(-83), page.recommendations.map { it.media.id })
            assertFalse(page.hasNextPage)
            assertNull(page.recommendations.single().rating)
            assertEquals(1, server.requestCount)
        }
    }

    private fun series(id: Int, anilist: String) = """{"id":$id,"state":"active","type":"manga","source":{"anilist":{"id":$anilist}},"titles":[{"language":"en","is_primary":true,"title":"Fictional story"}],"content_rating":"safe","cover":{"x350":"https://example.test/cover.jpg"},"popularity":{"global":{"current":1}}}"""
}
