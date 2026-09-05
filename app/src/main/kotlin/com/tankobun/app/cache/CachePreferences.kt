package com.tankobun.app.cache

internal const val MIB = 1024L * 1024L
internal const val PAGE_CACHE_FRESH_MILLIS = 7 * 24 * 60 * 60 * 1000L

enum class CacheProfile(val readerLimitMiB: Int, val prefetchPages: Int) {
    COMPACT(256, 2),
    BALANCED(2048, 6),
    EXTENSIVE(8192, 12),
}

data class CachePreferences(
    val readerLimitMiB: Int = CacheProfile.BALANCED.readerLimitMiB,
    val prefetchPages: Int = CacheProfile.BALANCED.prefetchPages,
    val prefetchUnmeteredOnly: Boolean = true,
) {
    val readerLimitBytes: Long get() = readerLimitMiB * MIB
    val profile: CacheProfile? get() = CacheProfile.entries.firstOrNull {
        it.readerLimitMiB == readerLimitMiB && it.prefetchPages == prefetchPages
    }

    fun allowsPrefetch(isMetered: Boolean): Boolean =
        prefetchPages > 0 && (!prefetchUnmeteredOnly || !isMetered)

    fun normalized(): CachePreferences = copy(
        readerLimitMiB = readerLimitMiB.coerceIn(128, 32 * 1024),
        prefetchPages = prefetchPages.coerceIn(0, 12),
    )

    fun withProfile(profile: CacheProfile): CachePreferences = copy(
        readerLimitMiB = profile.readerLimitMiB,
        prefetchPages = profile.prefetchPages,
    )
}
