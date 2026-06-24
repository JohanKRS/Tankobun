package com.tankobun.app.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduledBackupRetentionTest {
    @Test
    fun detectsScheduledBackupKindsFromFileNames() {
        assertEquals(
            ScheduledBackupFileKind.ANILIST_LIBRARY,
            scheduledBackupKindForFileName("tankobun_anilist_backup_user_name_1781295618151.xml"),
        )
        assertEquals(
            ScheduledBackupFileKind.LOCAL_LIBRARY,
            scheduledBackupKindForFileName("tankobun_library_1781295618151.json"),
        )
        assertEquals(
            ScheduledBackupFileKind.APP_SETTINGS,
            scheduledBackupKindForFileName("tankobun_settings_1781295618151.json"),
        )
        assertNull(scheduledBackupKindForFileName("qa-tankobun_library_1781295618151.json"))
    }

    @Test
    fun extractsScheduledBackupTimestampFromFileName() {
        assertEquals(
            1781295618151L,
            scheduledBackupTimestampFromFileName("tankobun_anilist_backup_user_name_1781295618151.xml"),
        )
        assertEquals(1781295618151L, scheduledBackupTimestampFromFileName("tankobun_library_1781295618151.json"))
        assertEquals(0L, scheduledBackupTimestampFromFileName("other.json"))
    }
}
