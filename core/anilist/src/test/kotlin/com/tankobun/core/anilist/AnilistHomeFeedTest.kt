package com.tankobun.core.anilist

import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class AnilistHomeFeedTest {
    @Test
    fun publishesTrendingAndGenreChunksBeforeReturningCompleteFeed() = runTest {
        val genres = listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Mystery")

        MockWebServer().use { server ->
            server.start()
            server.enqueue(response(initialResponse(genres.take(6))))
            server.enqueue(response(genreResponse(genres.drop(6))))

            val repository = AnilistRepository(
                AnilistGraphQlClient(
                    okHttpClient = OkHttpClient(),
                    rateLimiter = RespectfulRateLimiter(minSpacingMillis = 0L),
                    endpoint = server.url("/graphql").toString(),
                ),
            )
            var trendingCallbackSize = 0
            val genreCallbackSizes = mutableListOf<Int>()

            val feed = repository.homeFeed(
                genres = genres,
                onTrendingLoaded = { trendingCallbackSize = it.size },
                onGenreHighlightsLoaded = { genreCallbackSizes += it.size },
            )

            assertEquals(1, trendingCallbackSize)
            assertEquals(listOf(6, 7), genreCallbackSizes)
            assertEquals(1, feed.trending.size)
            assertEquals(genres, feed.genreHighlights.map { it.genre })
            assertEquals(2, server.requestCount)
        }
    }

    private fun response(body: String): MockResponse =
        MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body(body)
            .build()

    private fun initialResponse(genres: List<String>): String {
        val genreFields = genres.mapIndexed { index, genre ->
            "\"genre${index}Page1\":{\"media\":[${mediaJson(index + genre.hashCode(), genre)}]}"
        }
        val fields = listOf("\"trending\":{\"media\":[${mediaJson(1, "Action")}]}") + genreFields
        return "{\"data\":{${fields.joinToString(",")}}}"
    }

    private fun genreResponse(genres: List<String>): String {
        val fields = genres.mapIndexed { index, genre ->
            "\"genre${index}Page1\":{\"media\":[${mediaJson(index + genre.hashCode(), genre)}]}"
        }
        return "{\"data\":{${fields.joinToString(",")}}}"
    }

    private fun mediaJson(id: Int, genre: String): String =
        """{"id":$id,"title":{"userPreferred":"$genre manga"},"coverImage":{"extraLarge":"https://example.com/$id.jpg"},"bannerImage":"https://example.com/$id-banner.jpg","genres":["$genre"],"isAdult":false}"""
}
