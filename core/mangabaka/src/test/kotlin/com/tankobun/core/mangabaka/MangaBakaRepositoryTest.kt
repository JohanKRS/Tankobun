package com.tankobun.core.mangabaka

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import com.tankobun.core.model.CatalogSearchFilters
import com.tankobun.core.model.PublicationYears

class MangaBakaRepositoryTest {
    private fun response(body: String) = MockResponse.Builder().code(200).body(body).build()
    private fun repository(server: MockWebServer) = MangaBakaRepository(OkHttpClient(), baseUrl = server.url("/").toString())

    @Test fun multipleCountriesStatusesAndYearsShareOneFilteredPage() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":[],"pagination":{"next":null}}"""))
            repository(server).search(selection = CatalogSearchFilters(
                formats = setOf("MANGA", "ONE_SHOT"), countries = setOf("JP", "KR"),
                statuses = setOf("FINISHED", "RELEASING"), years = PublicationYears(2000, 2010),
            ))
            val target = server.takeRequest().target
            listOf("type=manga", "type=manhwa", "status=completed", "status=releasing",
                "published_start_date_lower=2000-01-01", "published_start_date_upper=2010-12-31").forEach { assertTrue(target, target.contains(it)) }
            assertFalse(target.contains("tag="))
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun novelsOrOneShotsUseTwoBoundedBranchesWithoutIntersectingTheirFormats() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":[${series(82, "123")}],"pagination":{"next":null}}"""))
            server.enqueue(response("""{"data":[${series(83, "null")}],"pagination":{"next":"next"}}"""))
            val page = repository(server).search(limit = 50, oneShotTagId = 110,
                selection = CatalogSearchFilters(formats = setOf("NOVEL", "ONE_SHOT")))
            val comics = server.takeRequest().target
            val novels = server.takeRequest().target
            assertTrue(comics.contains("tag=110"))
            assertTrue(comics.contains("type_not=novel"))
            assertTrue(novels.contains("type=novel"))
            assertFalse(novels.contains("tag=110"))
            assertTrue(comics.contains("limit=25") && novels.contains("limit=25"))
            assertEquals(2, page.media.size)
            assertTrue(page.hasNextPage)
            assertTrue(CatalogSearchFilters(formats = setOf("NOVEL"), countries = setOf("JP")).mangaBakaBranches().isEmpty())
        }
    }

    @Test fun tagTreeKeepsIdentityHierarchyAndAdultClassification() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":[
                {"id":1,"name":"Themes","name_path":"Themes","content_rating":"safe"},
                {"id":2,"parent_id":1,"name":"Historical","name_path":"Themes > Historical","is_genre":true,"content_rating":"safe"},
                {"id":3,"name":"Explicit","name_path":"Explicit","content_rating":"pornographic"},
                {"id":4,"parent_id":3,"name":"Child","name_path":"Explicit > Child","content_rating":"safe"}
            ]}"""))
            val repo = repository(server)
            val tags = repo.tags()
            assertEquals(4, tags.size)
            assertEquals("mb:2", tags[1].key)
            assertEquals("Themes", tags[1].category)
            assertTrue(tags[1].isGenre)
            assertTrue(tags[3].isAdult)
            assertEquals(tags, repo.tags())
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun idFiltersAvoidAmbiguousNamesAndDoNotNeedAnotherTaxonomyRequest() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":[${series(82, "123")}],"pagination":{"next":null}}"""))
            val page = repository(server).search(tagIds = setOf(515, 29))
            assertEquals(123, page.media.single().anilistId)
            val request = server.takeRequest()
            assertTrue(request.target.contains("tag=515"))
            assertTrue(request.target.contains("tag=29"))
            assertTrue(request.target.contains("tag_mode=and"))
            assertTrue(request.target.contains("content_rating=safe"))
            assertEquals(1, server.requestCount)
        }
    }

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

    @Test fun serializationTagsIdentifyOneShotsButNovelTypeDoesNotInventCountry() {
        val raw = Json.parseToJsonElement(series(82, "123")).jsonObject
        val oneShot = JsonObject(raw + ("tags" to Json.parseToJsonElement("""[{"id":2050,"name":"Promotional Oneshot","name_path":"Work Info > One Shot > Promotional Oneshot"},{"id":9,"name":"Spoiler","is_spoiler":true}]""")))
        assertEquals("ONE_SHOT", MangaBakaMapper.media(oneShot)?.format)
        assertEquals(listOf(2050, 9), MangaBakaMapper.media(oneShot)?.mangaBakaTagIds)
        assertFalse(MangaBakaMapper.media(oneShot)!!.tags.contains("Spoiler"))
        assertNull(MangaBakaMapper.media(JsonObject(raw + ("type" to JsonPrimitive("novel"))))?.countryOfOrigin)
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
