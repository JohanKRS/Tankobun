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
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
            )
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

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `anilist_list_entries` RENAME TO `anilist_list_entries_v7`")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `anilist_list_entries` (
                    `id` INTEGER NOT NULL,
                    `mediaId` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `progress` INTEGER NOT NULL,
                    `score` REAL,
                    `notes` TEXT,
                    `private` INTEGER NOT NULL,
                    `customLists` TEXT NOT NULL,
                    `updatedAtEpochSeconds` INTEGER,
                    `fetchedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`mediaId`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT OR REPLACE INTO `anilist_list_entries` (
                    `id`,
                    `mediaId`,
                    `status`,
                    `progress`,
                    `score`,
                    `notes`,
                    `private`,
                    `customLists`,
                    `updatedAtEpochSeconds`,
                    `fetchedAtEpochMillis`
                )
                SELECT
                    `id`,
                    `mediaId`,
                    `status`,
                    `progress`,
                    `score`,
                    `notes`,
                    `private`,
                    `customLists`,
                    `updatedAtEpochSeconds`,
                    `fetchedAtEpochMillis`
                FROM `anilist_list_entries_v7`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `anilist_list_entries_v7`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_anilist_list_entries_mediaId` ON `anilist_list_entries` (`mediaId`)")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `anilist_list_entries` ADD COLUMN `hiddenFromStatusLists` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `anilist_media` ADD COLUMN `mainCharacterImage` TEXT")
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `anilist_media` ADD COLUMN `characterImages` TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE `anilist_media` SET `characterImages` = `mainCharacterImage` WHERE `mainCharacterImage` IS NOT NULL")
        }
    }

    internal val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `source_bindings` ADD COLUMN `memoJson` TEXT")
            db.execSQL("ALTER TABLE `source_search_results` ADD COLUMN `memoJson` TEXT")
            db.execSQL("ALTER TABLE `source_chapters` ADD COLUMN `memoJson` TEXT")
        }
    }

    internal val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `download_pages` ADD COLUMN `indexIsPosition` INTEGER NOT NULL DEFAULT 0")
        }
    }

}
