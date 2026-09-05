package com.tankobun.core.network

import okhttp3.Headers
import org.junit.Assert.*
import org.junit.Test

class ServerBackoffTest {
    @Test fun respectsTheLaterOfRetryAfterAndAnExhaustedBudget() {
        val headers = Headers.headersOf("Retry-After", "2", "X-RateLimit-Remaining", "0", "X-RateLimit-Reset", "10")
        assertEquals(9_000L, serverBackoffMillis(headers, 200, 1_000))
    }

    @Test fun readsHttpDatesAndIgnoresInvalidAndPastValues() {
        assertEquals(1_000L, serverBackoffMillis(Headers.headersOf("Retry-After", "Thu, 01 Jan 1970 00:00:02 GMT"), 503, 1_000))
        assertEquals(0L, serverBackoffMillis(Headers.headersOf("Retry-After", "Thu, 01 Jan 1970 00:00:02 GMT"), 503, 3_000))
        assertNull(serverBackoffMillis(Headers.headersOf("Retry-After", "invalid"), 429, 1_000))
    }

    @Test fun largeServerValuesCannotWrapIntoAnImmediateRetry() {
        for (value in listOf(Long.MAX_VALUE.toString(), "999999999999999999999999999")) {
            assertEquals(Long.MAX_VALUE, serverBackoffMillis(Headers.headersOf("Retry-After", value), 429, 1_000))
            assertEquals(Long.MAX_VALUE, saturatedAddMillis(1_000, serverBackoffMillis(Headers.headersOf("X-RateLimit-Reset", value), 429, 1_000)!!))
        }
    }
}
