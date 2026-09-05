package com.tankobun.core.sync

import org.junit.Assert.*
import org.junit.Test

class SyncSessionTest {
    @Test
    fun queuedChangesCannotCrossLoginSessions() {
        val accountA = requireNotNull(syncSessionKey("test-session-a"))
        val accountB = requireNotNull(syncSessionKey("test-session-b"))
        val mutation = SyncMutationFactory().saveMediaListEntry(
            mediaId = 7, progress = 12, nowMillis = 1, sessionKey = accountA,
        )
        assertTrue(belongsToSyncSession(mutation.payloadJson, accountA))
        assertFalse(belongsToSyncSession(mutation.payloadJson, accountB))
        assertFalse(mutation.payloadJson.contains("test-session-a"))
    }

    @Test
    fun legacyUnauthenticatedAndMalformedRowsAreHeldLocally() {
        val session = requireNotNull(syncSessionKey("test-session"))
        val legacy = SyncMutationFactory().saveMediaListEntry(mediaId = 7, progress = 12, nowMillis = 1)
        assertFalse(belongsToSyncSession(legacy.payloadJson, session))
        assertFalse(belongsToSyncSession("invalid", session))
        assertNull(syncSessionKey(null))
        assertNull(syncSessionKey(""))
    }

    @Test
    fun deletionIsBoundToItsOriginalSession() {
        val session = requireNotNull(syncSessionKey("test-session"))
        val deletion = SyncMutationFactory().deleteMediaListEntry(7, 9, 1, session)
        assertTrue(belongsToSyncSession(deletion.payloadJson, session))
        assertFalse(belongsToSyncSession(deletion.payloadJson, requireNotNull(syncSessionKey("new-login"))))
    }
}
