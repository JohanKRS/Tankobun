package com.tankobun.app.cache

import org.junit.Assert.*
import org.junit.Test

class CachePreferencesTest {
    @Test fun defaultAndLargeProfileProtectMeteredConnections() {
        for (profile in CacheProfile.entries) {
            val preferences = CachePreferences().withProfile(profile)
            assertFalse(preferences.allowsPrefetch(isMetered = true))
            assertTrue(preferences.allowsPrefetch(isMetered = false))
        }
    }

    @Test fun explicitNetworkChoiceSurvivesProfileChangeAndOffWins() {
        val preferences = CachePreferences(prefetchUnmeteredOnly = false).withProfile(CacheProfile.EXTENSIVE)
        assertTrue(preferences.allowsPrefetch(isMetered = true))
        assertFalse(preferences.copy(prefetchPages = 0).allowsPrefetch(isMetered = false))
    }

    @Test fun malformedBackupValuesStayWithinSafeBounds() {
        val preferences = CachePreferences(readerLimitMiB = Int.MAX_VALUE, prefetchPages = -10).normalized()
        assertEquals(32L * 1024 * MIB, preferences.readerLimitBytes)
        assertFalse(preferences.allowsPrefetch(isMetered = false))
        assertEquals(128 * MIB, CachePreferences(readerLimitMiB = -1).normalized().readerLimitBytes)
    }
}
