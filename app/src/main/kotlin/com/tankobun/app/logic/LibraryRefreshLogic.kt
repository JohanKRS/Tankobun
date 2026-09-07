package com.tankobun.app.logic

internal const val LIBRARY_REFRESH_INTERVAL_MILLIS = 2 * 60 * 60 * 1000L

/** Checks only on a fresh app session or after a long absence; never polls. */
internal class LibraryRefreshGate {
    private var opened = false
    private var backgroundedAtMillis: Long? = null

    fun onForeground(nowMillis: Long): Boolean {
        val due = !opened || backgroundedAtMillis?.let {
            nowMillis - it >= LIBRARY_REFRESH_INTERVAL_MILLIS
        } == true
        opened = true
        backgroundedAtMillis = null
        return due
    }

    fun onBackground(nowMillis: Long) {
        // Repeated lifecycle callbacks must not shorten an existing absence.
        if (backgroundedAtMillis == null) backgroundedAtMillis = nowMillis
    }
}

/** Keep edits, additions and tombstones that appeared while the request was in flight. */
internal fun <T> reconcileLibrarySnapshot(
    before: Map<Int, T>,
    current: Map<Int, T>,
    remote: Map<Int, T>,
    pendingMediaIds: Set<Int>,
): Map<Int, T> {
    val protected = pendingMediaIds + (before.keys + current.keys).filter { before[it] != current[it] }
    return remote.filterKeys { it !in protected } + current.filterKeys { it in protected }
}
