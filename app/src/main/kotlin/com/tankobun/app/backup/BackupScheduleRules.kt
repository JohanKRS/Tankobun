package com.tankobun.app.backup

import com.tankobun.app.BackupSchedule

internal fun BackupSchedule.isDue(lastRunAt: Long, now: Long): Boolean {
    if (this == BackupSchedule.OFF) return false
    if (lastRunAt <= 0L) return true
    return now - lastRunAt >= when (this) {
        BackupSchedule.OFF -> Long.MAX_VALUE
        BackupSchedule.DAILY -> 24L * 60L * 60L * 1_000L
        BackupSchedule.WEEKLY -> 7L * 24L * 60L * 60L * 1_000L
        BackupSchedule.MONTHLY -> 30L * 24L * 60L * 60L * 1_000L
    }
}

