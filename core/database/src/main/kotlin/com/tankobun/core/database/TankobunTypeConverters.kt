package com.tankobun.core.database

import androidx.room.TypeConverter
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.SyncMutationType

class TankobunTypeConverters {
    @TypeConverter
    fun stringListToDb(value: List<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun stringListFromDb(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(SEPARATOR)

    @TypeConverter
    fun mediaStatusToDb(value: MediaStatus): String = value.name

    @TypeConverter
    fun mediaStatusFromDb(value: String): MediaStatus =
        enumValueOf<MediaStatus>(value)

    @TypeConverter
    fun readerModeToDb(value: ReaderMode): String = value.name

    @TypeConverter
    fun readerModeFromDb(value: String): ReaderMode =
        enumValueOf<ReaderMode>(value)

    @TypeConverter
    fun downloadStateToDb(value: DownloadState): String = value.name

    @TypeConverter
    fun downloadStateFromDb(value: String): DownloadState =
        enumValueOf<DownloadState>(value)

    @TypeConverter
    fun syncMutationTypeToDb(value: SyncMutationType): String = value.name

    @TypeConverter
    fun syncMutationTypeFromDb(value: String): SyncMutationType =
        enumValueOf<SyncMutationType>(value)

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}
