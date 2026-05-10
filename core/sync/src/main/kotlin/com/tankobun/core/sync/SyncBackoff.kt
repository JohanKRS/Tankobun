package com.tankobun.core.sync

import kotlin.math.pow
import kotlin.random.Random

class SyncBackoff(
    private val baseDelayMillis: Long = 30_000L,
    private val maxDelayMillis: Long = 6 * 60 * 60 * 1000L,
    private val random: Random = Random.Default,
) {
    fun nextDelayMillis(attempts: Int): Long {
        val exponential = baseDelayMillis * 2.0.pow(attempts.coerceAtLeast(0)).toLong()
        val capped = exponential.coerceAtMost(maxDelayMillis)
        val jitter = random.nextLong(0, (capped / 4).coerceAtLeast(1))
        return capped + jitter
    }
}
