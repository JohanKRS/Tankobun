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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
}
