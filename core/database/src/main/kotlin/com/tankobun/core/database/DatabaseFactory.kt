package com.tankobun.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseFactory {
    fun create(context: Context): TankobunDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            TankobunDatabase::class.java,
            "tankobun.db",
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .build()
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `anilist_recommendations` (
                    `mediaId` INTEGER NOT NULL,
                    `recommendationMediaId` INTEGER NOT NULL,
                    `rating` INTEGER,
                    `fetchedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`mediaId`, `recommendationMediaId`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_anilist_recommendations_mediaId` ON `anilist_recommendations` (`mediaId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_anilist_recommendations_recommendationMediaId` ON `anilist_recommendations` (`recommendationMediaId`)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `anilist_search_results` (
                    `query` TEXT NOT NULL,
                    `mediaId` INTEGER NOT NULL,
                    `orderIndex` INTEGER NOT NULL,
                    `fetchedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`query`, `mediaId`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_anilist_search_results_query` ON `anilist_search_results` (`query`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_anilist_search_results_mediaId` ON `anilist_search_results` (`mediaId`)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `anilist_media` ADD COLUMN `format` TEXT")
            db.execSQL("ALTER TABLE `anilist_media` ADD COLUMN `averageScore` INTEGER")
            db.execSQL("ALTER TABLE `anilist_media` ADD COLUMN `popularity` INTEGER")
            db.execSQL("ALTER TABLE `anilist_media` ADD COLUMN `startDateYear` INTEGER")
            db.execSQL("ALTER TABLE `anilist_media` ADD COLUMN `endDateYear` INTEGER")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `anilist_media` ADD COLUMN `staff` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `anilist_media` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `download_pages` (
                    `jobId` TEXT NOT NULL,
                    `mediaId` INTEGER NOT NULL,
                    `sourceId` INTEGER NOT NULL,
                    `mangaUrl` TEXT NOT NULL,
                    `chapterUrl` TEXT NOT NULL,
                    `pageIndex` INTEGER NOT NULL,
                    `imageUrl` TEXT NOT NULL,
                    `filePath` TEXT NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`jobId`, `pageIndex`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_pages_mediaId_chapterUrl` ON `download_pages` (`mediaId`, `chapterUrl`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_pages_chapterUrl` ON `download_pages` (`chapterUrl`)")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `reader_progress` ADD COLUMN `pageScrollOffset` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `anilist_media` ADD COLUMN `countryOfOrigin` TEXT")
        }
    }
}
