package com.tankobun.core.network

import kotlinx.coroutines.delay
import okhttp3.Headers
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max

class RespectfulRateLimiter(
    private val minSpacingMillis: Long,
    private val timeSource: TimeSource = SystemTimeSource,
    private val serverRateLimitWindowMillis: Long? = null,
    private val targetUtilization: Double = DEFAULT_TARGET_UTILIZATION,
) {
    private val lock = Any()
    private var nextAllowedAtMillis: Long = 0L
    private var effectiveSpacingMillis: Long = minSpacingMillis

    init {
        require(minSpacingMillis >= 0L) { "minSpacingMillis must not be negative" }
        require(targetUtilization > 0.0 && targetUtilization <= 1.0) {
            "targetUtilization must be in (0, 1]"
        }
    }

    suspend fun awaitTurn() {
        while (true) {
            val waitMillis = synchronized(lock) {
                val now = timeSource.nowMillis()
                if (now >= nextAllowedAtMillis) {
                    nextAllowedAtMillis = now + effectiveSpacingMillis
                    NO_WAIT_REQUIRED
                } else {
                    nextAllowedAtMillis - now
                }
            }

            if (waitMillis == NO_WAIT_REQUIRED) return
            delay(waitMillis)
        }
    }

    fun recordResponse(headers: Headers, statusCode: Int) {
        synchronized(lock) {
            val now = timeSource.nowMillis()
            updateSpacingFromServerLimit(headers, now)

            val retryAfterMillis = parseRetryAfterMillis(headers["Retry-After"], now)
            val resetEpochSeconds = headers["X-RateLimit-Reset"]?.toLongOrNull()
            val remaining = headers["X-RateLimit-Remaining"]?.toIntOrNull()

            val serverWaitUntil = when {
                retryAfterMillis != null -> now + retryAfterMillis
                resetEpochSeconds != null && (statusCode == 429 || remaining == 0) -> {
                    resetEpochSeconds * 1_000L
                }
                else -> null
            }

            if (serverWaitUntil != null) {
                nextAllowedAtMillis = max(nextAllowedAtMillis, serverWaitUntil)
            }
        }
    }

    suspend fun <T> run(block: suspend () -> T): T {
        awaitTurn()
        return block()
    }

    private fun updateSpacingFromServerLimit(headers: Headers, nowMillis: Long) {
        val windowMillis = serverRateLimitWindowMillis ?: return
        val limit = headers["X-RateLimit-Limit"]?.toLongOrNull()?.takeIf { it > 0L } ?: return
        val limitSpacing = ceil(windowMillis / (limit * targetUtilization))
            .toLong()
            .coerceAtLeast(1L)
        val remaining = headers["X-RateLimit-Remaining"]?.toLongOrNull()?.takeIf { it > 0L }
        val resetAtMillis = headers["X-RateLimit-Reset"]?.toLongOrNull()?.times(1_000L)
        val remainingBudgetSpacing = if (remaining != null && resetAtMillis != null && resetAtMillis > nowMillis) {
            ceil((resetAtMillis - nowMillis) / (remaining * targetUtilization))
                .toLong()
                .coerceAtLeast(1L)
        } else {
            0L
        }
        effectiveSpacingMillis = max(limitSpacing, remainingBudgetSpacing)
    }

    private fun parseRetryAfterMillis(value: String?, nowMillis: Long): Long? {
        if (value == null) return null
        value.toLongOrNull()?.let { seconds ->
            return seconds.coerceIn(0L, Long.MAX_VALUE / 1_000L) * 1_000L
        }

        val retryAtMillis = runCatching {
            ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        }.getOrNull() ?: return null
        return (retryAtMillis - nowMillis).coerceAtLeast(0L)
    }

    private companion object {
        const val DEFAULT_TARGET_UTILIZATION = 0.9
        const val NO_WAIT_REQUIRED = -1L
    }
}
