package eu.kanade.tachiyomi.network

import com.tankobun.core.network.awaitResponse
import eu.kanade.tachiyomi.network.interceptor.RequestThrottle
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import kotlinx.coroutines.*
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

class NetworkTrafficTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun fractionalPermitIntervalsNeverRoundDown() {
        var now = 0L
        val throttle = RequestThrottle(3, 1_000, { now }, { now += it })
        val call = OkHttpClient().newCall(Request.Builder().url("https://example.test").build())
        val starts = List(4) { throttle.awaitTurn(call); now }
        assertEquals(listOf(0L, 334L, 668L, 1_002L), starts)
    }

    @Test fun cancellingWhileThrottledDoesNotReserveAnotherSlot() {
        var now = 0L
        val client = OkHttpClient()
        val request = Request.Builder().url("https://example.test").build()
        val cancelled = client.newCall(request)
        val throttle = RequestThrottle(1, 1_000, { now }, { now += it; cancelled.cancel() })
        throttle.awaitTurn(client.newCall(request))
        val error = runCatching { throttle.awaitTurn(cancelled) }.exceptionOrNull()
        assertTrue(error is InterruptedIOException)
        assertEquals(100L, now)
        now = 1_000
        throttle.awaitTurn(client.newCall(request))
        assertEquals(1_000L, now)
    }

    @Test fun cachedResponsesRemainAvailableDuringServerBackoff() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            Cache(temporaryFolder.newFolder(), 1_000_000).use { cache ->
                val client = OkHttpClient.Builder().cache(cache)
                    .addNetworkInterceptor(PoliteHostThrottleInterceptor())
                    .rateLimit(1, 60, TimeUnit.SECONDS).build()
                server.enqueue(MockResponse.Builder().addHeader("Cache-Control", "public, max-age=3600")
                    .addHeader("Retry-After", "60").body("cached fixture").build())
                val request = Request.Builder().url(server.url("/cached")).build()
                client.newCall(request).awaitResponse().use { assertEquals("cached fixture", it.body.string()) }
                withTimeout(2_000) {
                    client.newCall(request).awaitResponse().use {
                        assertNotNull(it.cacheResponse)
                        assertEquals("cached fixture", it.body.string())
                    }
                }
                assertEquals(1, server.requestCount)
                val queued = client.newCall(request.newBuilder().url(server.url("/other")).build())
                val job = launch(Dispatchers.Default) { queued.awaitResponse().close() }
                delay(150)
                withTimeout(2_000) { job.cancelAndJoin() }
                assertTrue(queued.isCanceled())
                assertEquals(1, server.requestCount)
            }
        }
    }

    @Test fun redirectResponsesApplyBackoffToTheirOwnHost() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val redirected = server.url("/target").newBuilder().host("127.0.0.1").build()
            server.enqueue(MockResponse.Builder().code(302).addHeader("Location", redirected.toString()).build())
            server.enqueue(MockResponse.Builder().addHeader("Retry-After", "60").body("target").build())
            val client = OkHttpClient.Builder().addNetworkInterceptor(PoliteHostThrottleInterceptor()).build()
            client.newCall(Request.Builder().url(server.url("/redirect")).build()).awaitResponse().use { assertEquals("target", it.body.string()) }
            val call = client.newCall(Request.Builder().url(redirected).build())
            val job = launch(Dispatchers.Default) { call.awaitResponse().close() }
            delay(150)
            withTimeout(2_000) { job.cancelAndJoin() }
            assertEquals(2, server.requestCount)
        }
    }
}
