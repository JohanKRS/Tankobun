package com.tankobun.core.network

class CacheFreshness(
    private val timeSource: TimeSource = SystemTimeSource,
) {
    fun isFresh(fetchedAtEpochMillis: Long?, ttlMillis: Long): Boolean {
        if (fetchedAtEpochMillis == null) return false
        if (ttlMillis <= 0L) return false
        return timeSource.nowMillis() - fetchedAtEpochMillis < ttlMillis
    }
}
