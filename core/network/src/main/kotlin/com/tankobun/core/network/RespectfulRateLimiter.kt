package com.tankobun.core.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Headers
import kotlin.math.max

class RespectfulRateLimiter(
    private val minSpacingMillis: Long,
    private val timeSource: TimeSource = SystemTimeSource,
) {
    private val mutex = Mutex()
    private var nextAllowedAtMillis: Long = 0L

    suspend fun awaitTurn() {
        mutex.withLock {
            val now = timeSource.nowMillis()
            val waitMillis = nextAllowedAtMillis - now
            if (waitMillis > 0) {
                delay(waitMillis)
            }
            nextAllowedAtMillis = max(timeSource.nowMillis(), nextAllowedAtMillis) + minSpacingMillis
        }
    }

    fun recordResponse(headers: Headers, statusCode: Int) {
        val retryAfterMillis = headers["Retry-After"]?.toLongOrNull()?.times(1000)
        val resetEpochSeconds = headers["X-RateLimit-Reset"]?.toLongOrNull()
        val remaining = headers["X-RateLimit-Remaining"]?.toIntOrNull()

        val serverWaitUntil = when {
            retryAfterMillis != null -> timeSource.nowMillis() + retryAfterMillis
            resetEpochSeconds != null && (statusCode == 429 || remaining == 0) -> resetEpochSeconds * 1000
            else -> null
        }

        if (serverWaitUntil != null) {
            nextAllowedAtMillis = max(nextAllowedAtMillis, serverWaitUntil)
        }
    }

    suspend fun <T> run(block: suspend () -> T): T {
        awaitTurn()
        return block()
    }
}
