package com.tankobun.core.anilist

import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class AnilistGraphQlClientTest {
    @Test
    fun sendsConfiguredUserAgent() = runTest {
        val userAgent = "Tankobun/2.0.0 (Android; com.tankobun.app; https://github.com/JohanKRS/Tankobun)"

        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .setHeader("Content-Type", "application/json")
                    .body("""{"data":{}}""")
                    .build(),
            )

            AnilistGraphQlClient(
                okHttpClient = OkHttpClient(),
                rateLimiter = RespectfulRateLimiter(minSpacingMillis = 0L),
                endpoint = server.url("/graphql").toString(),
                userAgent = userAgent,
            ).execute(query = "query { Viewer { id } }")

            assertEquals(userAgent, server.takeRequest().headers["User-Agent"])
        }
    }
}
