package com.tankobun.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AnilistMediaEntity::class,
        AnilistListEntryEntity::class,
        AnilistRecommendationEntity::class,
        AnilistSearchResultEntity::class,
        SourceBindingEntity::class,
        SourceSearchResultEntity::class,
        SourceChapterEntity::class,
        ReadingProgressEntity::class,
        DownloadJobEntity::class,
        DownloadPageEntity::class,
        SyncMutationEntity::class,
    ],
    version = 13,
    exportSchema = true,
)
@TypeConverters(TankobunTypeConverters::class)
abstract class TankobunDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun listEntryDao(): ListEntryDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun searchResultDao(): SearchResultDao
    abstract fun sourceBindingDao(): SourceBindingDao
    abstract fun sourceSearchDao(): SourceSearchDao
    abstract fun chapterDao(): ChapterDao
    abstract fun progressDao(): ProgressDao
    abstract fun downloadDao(): DownloadDao
    abstract fun downloadPageDao(): DownloadPageDao
    abstract fun syncMutationDao(): SyncMutationDao
}
