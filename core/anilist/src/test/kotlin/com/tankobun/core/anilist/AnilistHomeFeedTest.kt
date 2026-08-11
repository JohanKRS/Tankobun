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
    fun loadsTrendingAndAllGenresInOneRequest() = runTest {
        val genres = listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Mystery")

        MockWebServer().use { server ->
            server.start()
            server.enqueue(response(homeResponse(genres)))

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
            assertEquals(listOf(7), genreCallbackSizes)
            assertEquals(1, feed.trending.size)
            assertEquals(genres, feed.genreHighlights.map { it.genre })
            assertEquals(1, server.requestCount)
        }
    }

    @Test
    fun overlapsTwoRequestsForTheCompleteGenreSet() = runTest {
        val genres = listOf(
            "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy", "Horror", "Mahou Shoujo", "Mecha",
            "Music", "Mystery", "Psychological", "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural", "Thriller",
        )

        MockWebServer().use { server ->
            server.start()
            server.enqueue(response(homeResponse(genres.take(9))))
            server.enqueue(response(homeResponse(genres.drop(9))))

            val repository = AnilistRepository(
                AnilistGraphQlClient(
                    okHttpClient = OkHttpClient(),
                    rateLimiter = RespectfulRateLimiter(minSpacingMillis = 0L),
                    endpoint = server.url("/graphql").toString(),
                ),
            )
            val callbackSizes = mutableListOf<Int>()

            val feed = repository.homeFeed(
                genres = genres,
                onGenreHighlightsLoaded = { callbackSizes += it.size },
            )

            assertEquals(listOf(18), callbackSizes)
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

    private fun homeResponse(genres: List<String>): String {
        val genreFields = genres.mapIndexed { index, genre ->
            "\"genre${index}Page1\":{\"media\":[${mediaJson(index + genre.hashCode(), genre)}]}"
        }
        val fields = listOf("\"trending\":{\"media\":[${mediaJson(1, "Action")}]}") + genreFields
        return "{\"data\":{${fields.joinToString(",")}}}"
    }

    private fun mediaJson(id: Int, genre: String): String =
        """{"id":$id,"title":{"userPreferred":"$genre manga"},"coverImage":{"extraLarge":"https://example.com/$id.jpg"},"bannerImage":"https://example.com/$id-banner.jpg","isAdult":false}"""
}
