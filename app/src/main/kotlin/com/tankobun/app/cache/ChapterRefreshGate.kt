package com.tankobun.app.cache

/** Foreground navigation must not repeatedly retry a failing source. No polling is scheduled. */
internal class ChapterRefreshGate(private val maxEntries: Int = 128) {
    private val attempts = LinkedHashMap<String, Long>()

    fun allow(key: String, now: Long, manual: Boolean): Boolean {
        val previous = attempts[key]
        if (!manual && previous != null && now - previous in 0 until 120_000L) return false
        attempts.remove(key)
        attempts[key] = now
        while (attempts.size > maxEntries) attempts.remove(attempts.keys.first())
        return true
    }
}
