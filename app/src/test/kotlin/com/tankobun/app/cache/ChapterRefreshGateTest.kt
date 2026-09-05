package com.tankobun.app.cache

import org.junit.Assert.*
import org.junit.Test

class ChapterRefreshGateTest {
    @Test fun repeatNavigationWaitsButManualRefreshIsAvailable() {
        val gate = ChapterRefreshGate()
        assertTrue(gate.allow("title", 1000, false))
        assertFalse(gate.allow("title", 120999, false))
        assertTrue(gate.allow("title", 121000, false))
        assertTrue(gate.allow("title", 121001, true))
        assertFalse(gate.allow("title", 121002, false))
    }
    @Test fun differentTitlesAndClockCorrectionsAreNotBlocked() {
        val gate = ChapterRefreshGate()
        assertTrue(gate.allow("one", 1000, false))
        assertTrue(gate.allow("two", 1000, false))
        assertTrue(gate.allow("one", 500, false))
    }
    @Test fun oldAttemptRecordsStayBounded() {
        val gate = ChapterRefreshGate(maxEntries = 2)
        gate.allow("one", 1000, false)
        gate.allow("two", 1000, false)
        gate.allow("three", 1000, false)
        assertTrue(gate.allow("one", 1001, false))
    }
}
