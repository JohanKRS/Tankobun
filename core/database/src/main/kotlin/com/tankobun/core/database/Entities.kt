package com.tankobun.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.SyncMutationType

@Entity(tableName = "anilist_media")
data class AnilistMediaEntity(
    @PrimaryKey val id: Int,
    val idMal: Int?,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val titleUserPreferred: String,
    val description: String?,
    val coverImage: String?,
    val bannerImage: String?,
    val chapters: Int?,
    val volumes: Int?,
    val status: String?,
    val siteUrl: String?,
    val genres: List<String>,
    val synonyms: List<String>,
    val isAdult: Boolean,
    val updatedAtEpochSeconds: Long?,
    val fetchedAtEpochMillis: Long,
)

@Entity(
    tableName = "anilist_list_entries",
    indices = [Index("mediaId")],
)
data class AnilistListEntryEntity(
    @PrimaryKey val id: Int,
    val mediaId: Int,
    val status: MediaStatus,
    val progress: Int,
    val score: Double?,
    val notes: String?,
    val private: Boolean,
    val customLists: List<String>,
    val updatedAtEpochSeconds: Long?,
    val fetchedAtEpochMillis: Long,
)

@Entity(
    tableName = "anilist_recommendations",
    primaryKeys = ["mediaId", "recommendationMediaId"],
    indices = [Index("mediaId"), Index("recommendationMediaId")],
)
data class AnilistRecommendationEntity(
    val mediaId: Int,
    val recommendationMediaId: Int,
    val rating: Int?,
    val fetchedAtEpochMillis: Long,
)

@Entity(
    tableName = "anilist_search_results",
    primaryKeys = ["query", "mediaId"],
    indices = [Index("query"), Index("mediaId")],
)
data class AnilistSearchResultEntity(
    val query: String,
    val mediaId: Int,
    val orderIndex: Int,
    val fetchedAtEpochMillis: Long,
)

@Entity(tableName = "source_bindings")
data class SourceBindingEntity(
    @PrimaryKey val mediaId: Int,
    val sourceId: Long,
    val sourcePackageName: String,
    val mangaUrl: String,
    val mangaTitle: String,
    val thumbnailUrl: String?,
    val selectedAtEpochMillis: Long,
)

@Entity(
    tableName = "source_search_results",
    primaryKeys = ["mediaId", "sourceId", "mangaUrl"],
)
data class SourceSearchResultEntity(
    val mediaId: Int,
    val sourceId: Long,
    val sourcePackageName: String,
    val sourceName: String,
    val sourceLang: String,
    val mangaUrl: String,
    val mangaTitle: String,
    val mangaThumbnailUrl: String?,
    val score: Double,
    val reasons: List<String>,
    val searchedAtEpochMillis: Long,
)

@Entity(
    tableName = "source_chapters",
    primaryKeys = ["sourceId", "chapterUrl"],
    indices = [Index("mangaUrl")],
)
data class SourceChapterEntity(
    val sourceId: Long,
    val mangaUrl: String,
    val chapterUrl: String,
    val name: String,
    val chapterNumber: Float,
    val scanlator: String?,
    val uploadedAtEpochMillis: Long?,
    val fetchedAtEpochMillis: Long,
)

@Entity(
    tableName = "reader_progress",
    primaryKeys = ["mediaId", "chapterUrl"],
)
data class ReadingProgressEntity(
    val mediaId: Int,
    val chapterUrl: String,
    val chapterNumber: Float,
    val pageIndex: Int,
    val totalPages: Int,
    val readerMode: ReaderMode,
    val completed: Boolean,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "download_jobs",
    indices = [Index("mediaId"), Index("chapterUrl")],
)
data class DownloadJobEntity(
    @PrimaryKey val id: String,
    val mediaId: Int,
    val sourceId: Long,
    val mangaUrl: String,
    val chapterUrl: String,
    val chapterName: String,
    val state: DownloadState,
    val pageCount: Int,
    val completedPages: Int,
    val retryCount: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "sync_mutations")
data class SyncMutationEntity(
    @PrimaryKey val id: String,
    val type: SyncMutationType,
    val mediaId: Int,
    val payloadJson: String,
    val attempts: Int,
    val nextAttemptAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
)
