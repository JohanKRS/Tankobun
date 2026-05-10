package com.tankobun.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheFreshnessTest {
    private val cacheFreshness = CacheFreshness { 10_000L }

    @Test
    fun freshWhenWithinTtl() {
        assertTrue(cacheFreshness.isFresh(fetchedAtEpochMillis = 9_000L, ttlMillis = 2_000L))
    }

    @Test
    fun staleWhenOutsideTtl() {
        assertFalse(cacheFreshness.isFresh(fetchedAtEpochMillis = 7_000L, ttlMillis = 2_000L))
    }
}
