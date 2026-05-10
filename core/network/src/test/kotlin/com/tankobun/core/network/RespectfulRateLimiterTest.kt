package com.tankobun.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.Headers
import org.junit.Assert.assertTrue
import org.junit.Test

class RespectfulRateLimiterTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun retryAfterPushesNextRequestOut() = runTest {
        var now = 1_000L
        val limiter = RespectfulRateLimiter(
            minSpacingMillis = 100,
            timeSource = TimeSource { now },
        )

        limiter.recordResponse(
            Headers.headersOf("Retry-After", "2"),
            statusCode = 429,
        )

        val before = testScheduler.currentTime
        limiter.awaitTurn()
        val delayedBy = testScheduler.currentTime - before

        assertTrue(delayedBy >= 2_000L)
    }
}
