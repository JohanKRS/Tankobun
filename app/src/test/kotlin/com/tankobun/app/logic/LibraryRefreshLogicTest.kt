package com.tankobun.app.logic

import org.junit.Assert.*
import org.junit.Test

class LibraryRefreshLogicTest {
    @Test fun refreshCooldownSurvivesQuickReopensAndAllowsClockCorrections() {
        assertTrue(shouldRefreshLibraryOnOpen(1_000, 0))
        assertFalse(shouldRefreshLibraryOnOpen(120_999, 1_000))
        assertTrue(shouldRefreshLibraryOnOpen(121_000, 1_000))
        assertTrue(shouldRefreshLibraryOnOpen(500, 1_000))
    }

    @Test fun reconcilesRemoteAddsEditsAndRemovals() {
        val before = mapOf(1 to "removed", 2 to "old")
        assertEquals(mapOf(2 to "updated", 3 to "added"), reconcileLibrarySnapshot(
            before, before, mapOf(2 to "updated", 3 to "added"), emptySet(),
        ))
    }

    @Test fun emptyCompleteSnapshotRemovesMembership() {
        val before = mapOf(1 to "removed")
        assertTrue(reconcileLibrarySnapshot(before, before, emptyMap(), emptySet()).isEmpty())
    }

    @Test fun preservesConcurrentLocalEditAdditionAndDeletion() {
        val before = mapOf(1 to "old", 2 to "delete locally")
        val current = mapOf(1 to "edited locally", 3 to "added locally")
        val remote = mapOf(1 to "remote", 2 to "stale remote", 4 to "added remotely")
        assertEquals(current + (4 to "added remotely"), reconcileLibrarySnapshot(before, current, remote, emptySet()))
    }

    @Test fun protectsDelayedPendingEditsAndDeletionTombstones() {
        val before = mapOf(1 to "pending addition", 2 to "pending edit", 4 to "remote removal")
        val remote = mapOf(2 to "older value", 3 to "pending deletion")
        assertEquals(before - 4, reconcileLibrarySnapshot(before, before, remote, setOf(1, 2, 3)))
    }
}
