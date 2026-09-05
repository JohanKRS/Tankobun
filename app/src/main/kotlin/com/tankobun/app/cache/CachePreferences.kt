package com.tankobun.app.cache

internal const val MIB = 1024L * 1024L
internal const val PAGE_CACHE_FRESH_MILLIS = 7 * 24 * 60 * 60 * 1000L

enum class CacheProfile(val readerLimitMiB: Int, val prefetchPages: Int, val imageLimitMiB: Int, val navigationRetentionDays: Int) {
    COMPACT(256, 2, 128, 7),
    BALANCED(2048, 6, 512, 30),
    EXTENSIVE(8192, 12, 2048, 90),
}

data class CachePreferences(
    val readerLimitMiB: Int = CacheProfile.BALANCED.readerLimitMiB,
    val prefetchPages: Int = CacheProfile.BALANCED.prefetchPages,
    val prefetchUnmeteredOnly: Boolean = true,
    val imageLimitMiB: Int = CacheProfile.BALANCED.imageLimitMiB,
    val navigationRetentionDays: Int = CacheProfile.BALANCED.navigationRetentionDays,
) {
    val imageLimitBytes: Long get() = imageLimitMiB * MIB
    val readerLimitBytes: Long get() = readerLimitMiB * MIB
    val profile: CacheProfile? get() = CacheProfile.entries.firstOrNull {
        it.readerLimitMiB == readerLimitMiB && it.prefetchPages == prefetchPages && it.imageLimitMiB == imageLimitMiB && it.navigationRetentionDays == navigationRetentionDays
    }

    fun allowsPrefetch(isMetered: Boolean): Boolean =
        prefetchPages > 0 && (!prefetchUnmeteredOnly || !isMetered)

    fun normalized(): CachePreferences = copy(
        readerLimitMiB = readerLimitMiB.coerceIn(128, 32 * 1024),
        prefetchPages = prefetchPages.coerceIn(0, 12),
        imageLimitMiB = imageLimitMiB.coerceIn(32, 8192),
        navigationRetentionDays = navigationRetentionDays.coerceIn(7, 365),
    )

    fun withProfile(profile: CacheProfile): CachePreferences = copy(
        readerLimitMiB = profile.readerLimitMiB,
        prefetchPages = profile.prefetchPages,
        imageLimitMiB = profile.imageLimitMiB,
        navigationRetentionDays = profile.navigationRetentionDays,
    )
}
