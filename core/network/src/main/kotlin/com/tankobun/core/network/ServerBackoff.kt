package com.tankobun.core.network

import okhttp3.Headers
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Returns a delay, not a wall-clock deadline, so callers can use a monotonic clock. */
fun serverBackoffMillis(headers: Headers, statusCode: Int, nowMillis: Long): Long? {
    val retryAfter = headers["Retry-After"]?.trim()?.let { value ->
        secondsToMillis(value) ?: runCatching {
            ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        }.getOrNull()?.let { futureDelay(it, nowMillis) }
    }
    val resetDelay = if (statusCode == 429 || headers["X-RateLimit-Remaining"]?.trim()?.toLongOrNull() == 0L) {
        headers["X-RateLimit-Reset"]?.trim()?.let(::secondsToMillis)?.let { futureDelay(it, nowMillis) }
    } else {
        null
    }
    return listOfNotNull(retryAfter, resetDelay).maxOrNull()
}

fun saturatedAddMillis(nowMillis: Long, delayMillis: Long): Long {
    require(delayMillis >= 0)
    return if (nowMillis > Long.MAX_VALUE - delayMillis) Long.MAX_VALUE else nowMillis + delayMillis
}

internal fun secondsToMillis(value: String): Long? {
    if (value.isEmpty() || value.any { it !in '0'..'9' }) return null
    val seconds = value.toLongOrNull() ?: return Long.MAX_VALUE
    return if (seconds > Long.MAX_VALUE / 1_000) Long.MAX_VALUE else seconds * 1_000
}

private fun futureDelay(deadline: Long, now: Long): Long = when {
    deadline <= now -> 0
    now < 0 && deadline > Long.MAX_VALUE + now -> Long.MAX_VALUE
    else -> deadline - now
}
