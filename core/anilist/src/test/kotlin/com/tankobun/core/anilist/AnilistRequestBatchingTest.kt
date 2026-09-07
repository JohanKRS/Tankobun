package com.tankobun.core.anilist

import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.tankobun.core.model.CatalogSearchFilters
import com.tankobun.core.model.PublicationYears
import kotlinx.serialization.json.*
import org.junit.Test

class AnilistRequestBatchingTest {
    @Test
    fun multipleFiltersAndInclusiveYearsUseOneRequestWithNativeArrays() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":{"Page":{"pageInfo":{"currentPage":2,"hasNextPage":true},"media":[]}}}"""))
            val page = repository(server).browseMangaPage(page = 2, selection = CatalogSearchFilters(
                formats = setOf("MANGA", "ONE_SHOT"), statuses = setOf("FINISHED", "RELEASING"),
                countries = setOf("KR", "JP"), years = PublicationYears(2000, 2010),
            ))
            val body = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
            val variables = body.getValue("variables").jsonObject
            assertEquals(setOf("MANGA", "ONE_SHOT"), variables.getValue("formats").jsonArray.map { it.jsonPrimitive.content }.toSet())
            assertEquals(setOf("JP", "KR"), variables.getValue("countries").jsonArray.map { it.jsonPrimitive.content }.toSet())
            assertEquals(setOf("FINISHED", "RELEASING"), variables.getValue("statuses").jsonArray.map { it.jsonPrimitive.content }.toSet())
            assertEquals(19991231, variables.getValue("startDateGreater").jsonPrimitive.int)
            assertEquals(20110000, variables.getValue("startDateLesser").jsonPrimitive.int)
            assertTrue(body.getValue("query").jsonPrimitive.content.contains("countryOfOrigin_in:"))
            assertEquals(2, page.currentPage)
            assertTrue(page.hasNextPage)
            assertEquals(1, server.requestCount)
        }
    }

    @Test
    fun browseLandingLoadsFourSectionsInOneRequest() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                response(
                    """{"data":{
                        "trending":{"media":[${mediaJson(1, "Trending")}]},
                        "popular":{"media":[${mediaJson(2, "Popular")}]},
                        "popularManhwa":{"media":[${mediaJson(3, "Manhwa")}]},
                        "topManga":{"media":[${mediaJson(4, "Top")}]}
                    }}""".trimIndent(),
                ),
            )

            val landing = repository(server).browseLanding(perPage = 12)

            assertEquals(listOf(1), landing.trending.map { it.id })
            assertEquals(listOf(2), landing.popular.map { it.id })
            assertEquals(listOf(3), landing.popularManhwa.map { it.id })
            assertEquals(listOf(4), landing.topManga.map { it.id })
            assertEquals(1, server.requestCount)
        }
    }

    @Test
    fun emptyDirectSearchUsesOnlyOneBoundedFallbackRequest() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response("""{"data":{"Page":{"pageInfo":{"currentPage":1,"hasNextPage":false},"media":[]}}}"""))
            server.enqueue(
                response(
                    """{"data":{
                        "popular":{"media":[${mediaJson(10, "Needle Popular")}]},
                        "trending":{"media":[${mediaJson(11, "Needle Trending")}]} 
                    }}""".trimIndent(),
                ),
            )

            val page = repository(server).searchMangaPage(query = "needle")

            assertEquals(listOf(10, 11), page.media.map { it.id })
            assertEquals(2, server.requestCount)
        }
    }

    @Test
    fun mangaDetailsForRecommendationImportAreAliasedIntoOneRequest() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                response(
                    """{"data":{
                        "media0":${mediaJson(20, "First")},
                        "media1":${mediaJson(21, "Second")},
                        "media2":${mediaJson(22, "Third")}
                    }}""".trimIndent(),
                ),
            )

            val media = repository(server).mangaByIds(listOf(20, 21, 22))

            assertEquals(listOf(20, 21, 22), media.map { it.id })
            assertEquals(1, server.requestCount)
        }
    }

    private fun repository(server: MockWebServer): AnilistRepository =
        AnilistRepository(
            AnilistGraphQlClient(
                okHttpClient = OkHttpClient(),
                rateLimiter = RespectfulRateLimiter(minSpacingMillis = 0L),
                endpoint = server.url("/graphql").toString(),
            ),
        )

    private fun response(body: String): MockResponse =
        MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body(body)
            .build()

    private fun mediaJson(id: Int, title: String): String =
        """{"id":$id,"title":{"userPreferred":"$title"},"coverImage":{"extraLarge":"https://example.test/$id.jpg"},"genres":[],"isAdult":false}"""
}
