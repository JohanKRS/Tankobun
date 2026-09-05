package com.tankobun.app.logic

internal const val LIBRARY_REFRESH_INTERVAL_MILLIS = 2 * 60 * 1000L

internal fun shouldRefreshLibraryOnOpen(nowMillis: Long, lastAttemptMillis: Long): Boolean =
    lastAttemptMillis <= 0L || nowMillis < lastAttemptMillis ||
        nowMillis - lastAttemptMillis >= LIBRARY_REFRESH_INTERVAL_MILLIS

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
