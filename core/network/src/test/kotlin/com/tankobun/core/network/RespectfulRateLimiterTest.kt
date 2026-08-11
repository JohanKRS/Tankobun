package com.tankobun.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.Headers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class RespectfulRateLimiterTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun retryAfterPushesNextRequestOut() = runTest {
        val baseTime = 1_000L
        val limiter = RespectfulRateLimiter(
            minSpacingMillis = 100,
            timeSource = TimeSource { baseTime + testScheduler.currentTime },
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun retryAfterHttpDatePushesNextRequestOut() = runTest {
        val baseTime = 1_000_000L
        val retryAt = Instant.ofEpochMilli(baseTime + 3_000L)
            .atZone(ZoneOffset.UTC)
            .format(DateTimeFormatter.RFC_1123_DATE_TIME)
        val limiter = RespectfulRateLimiter(
            minSpacingMillis = 100L,
            timeSource = TimeSource { baseTime + testScheduler.currentTime },
        )

        limiter.recordResponse(Headers.headersOf("Retry-After", retryAt), statusCode = 503)
        limiter.awaitTurn()

        assertEquals(3_000L, testScheduler.currentTime)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun serverLimitAdaptsSpacingWithoutExceedingTargetUtilization() = runTest {
        val limiter = RespectfulRateLimiter(
            minSpacingMillis = 2_200L,
            timeSource = TimeSource { testScheduler.currentTime },
            serverRateLimitWindowMillis = 60_000L,
            targetUtilization = 0.9,
        )
        limiter.recordResponse(Headers.headersOf("X-RateLimit-Limit", "90"), statusCode = 200)

        limiter.awaitTurn()
        limiter.awaitTurn()

        assertEquals(741L, testScheduler.currentTime)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun lowRemainingBudgetSlowsRequestsUntilReset() = runTest {
        val baseTime = 1_000_000L
        val limiter = RespectfulRateLimiter(
            minSpacingMillis = 2_200L,
            timeSource = TimeSource { baseTime + testScheduler.currentTime },
            serverRateLimitWindowMillis = 60_000L,
            targetUtilization = 0.9,
        )
        limiter.recordResponse(
            Headers.headersOf(
                "X-RateLimit-Limit", "90",
                "X-RateLimit-Remaining", "5",
                "X-RateLimit-Reset", ((baseTime + 30_000L) / 1_000L).toString(),
            ),
            statusCode = 200,
        )

        limiter.awaitTurn()
        limiter.awaitTurn()

        assertEquals(6_667L, testScheduler.currentTime)
    }
}
