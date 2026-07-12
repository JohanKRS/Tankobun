package com.tankobun.core.model

enum class MediaStatus {
    CURRENT,
    PLANNING,
    COMPLETED,
    DROPPED,
    PAUSED,
    REPEATING,
    UNKNOWN,
}

enum class AnilistScoreFormat {
    POINT_100,
    POINT_10_DECIMAL,
    POINT_10,
    POINT_5,
    POINT_3,
}

enum class AnilistTitleLanguage {
    ROMAJI,
    ENGLISH,
    NATIVE,
    ROMAJI_STYLISED,
    ENGLISH_STYLISED,
    NATIVE_STYLISED,
}

enum class ReaderMode {
    PAGED,
    WEBTOON,
}

enum class DownloadState {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETE,
    FAILED,
}

enum class SyncMutationType {
    SAVE_MEDIA_LIST_ENTRY,
    DELETE_MEDIA_LIST_ENTRY,
}

data class AnilistTitle(
    val romaji: String?,
    val english: String?,
    val native: String?,
    val userPreferred: String,
)

fun AnilistMedia.withTitleLanguage(language: AnilistTitleLanguage): AnilistMedia =
    copy(title = title.withUserPreferred(language))

fun AnilistTitle.withUserPreferred(language: AnilistTitleLanguage): AnilistTitle =
    copy(userPreferred = preferredFor(language))

private fun AnilistTitle.preferredFor(language: AnilistTitleLanguage): String =
    when (language) {
        AnilistTitleLanguage.ENGLISH,
        AnilistTitleLanguage.ENGLISH_STYLISED -> firstNonBlank(english, romaji, native, userPreferred)
        AnilistTitleLanguage.NATIVE,
        AnilistTitleLanguage.NATIVE_STYLISED -> firstNonBlank(native, romaji, english, userPreferred)
        AnilistTitleLanguage.ROMAJI,
        AnilistTitleLanguage.ROMAJI_STYLISED -> firstNonBlank(romaji, english, native, userPreferred)
    }

private fun firstNonBlank(vararg values: String?): String =
    values.firstOrNull { !it.isNullOrBlank() } ?: ""

data class AnilistMedia(
    val id: Int,
    val idMal: Int?,
    val title: AnilistTitle,
    val description: String?,
    val coverImage: String?,
    val bannerImage: String?,
    val chapters: Int?,
    val volumes: Int?,
    val format: String?,
    val status: String?,
    val averageScore: Int?,
    val popularity: Int?,
    val startDateYear: Int?,
    val endDateYear: Int?,
    val siteUrl: String?,
    val genres: List<String>,
    val synonyms: List<String>,
    val isAdult: Boolean,
    val updatedAtEpochSeconds: Long?,
    val staff: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val countryOfOrigin: String? = null,
    val mainCharacterImage: String? = null,
    val characterImages: List<String> = emptyList(),
)

fun AnilistMedia.withFallbackDetails(fallback: AnilistMedia?): AnilistMedia {
    if (fallback == null || fallback.id != id) return this
    return copy(
        idMal = idMal ?: fallback.idMal,
        title = title.copy(
            romaji = title.romaji ?: fallback.title.romaji,
            english = title.english ?: fallback.title.english,
            native = title.native ?: fallback.title.native,
            userPreferred = title.userPreferred.ifBlank { fallback.title.userPreferred },
        ),
        description = description ?: fallback.description,
        coverImage = coverImage ?: fallback.coverImage,
        bannerImage = bannerImage ?: fallback.bannerImage,
        chapters = chapters ?: fallback.chapters,
        volumes = volumes ?: fallback.volumes,
        format = format ?: fallback.format,
        status = status ?: fallback.status,
        averageScore = averageScore ?: fallback.averageScore,
        popularity = popularity ?: fallback.popularity,
        startDateYear = startDateYear ?: fallback.startDateYear,
        endDateYear = endDateYear ?: fallback.endDateYear,
        siteUrl = siteUrl ?: fallback.siteUrl,
        genres = genres.ifEmpty { fallback.genres },
        synonyms = synonyms.ifEmpty { fallback.synonyms },
        updatedAtEpochSeconds = updatedAtEpochSeconds ?: fallback.updatedAtEpochSeconds,
        staff = staff.ifEmpty { fallback.staff },
        tags = tags.ifEmpty { fallback.tags },
        countryOfOrigin = countryOfOrigin ?: fallback.countryOfOrigin,
        mainCharacterImage = mainCharacterImage ?: fallback.mainCharacterImage,
        characterImages = characterImages.ifEmpty { fallback.characterImages },
    )
}

data class AnilistGenreHighlight(
    val genre: String,
    val media: AnilistMedia,
)

data class AnilistHomeFeed(
    val trending: List<AnilistMedia>,
    val genreHighlights: List<AnilistGenreHighlight>,
)

data class AnilistMediaPage(
    val media: List<AnilistMedia>,
    val currentPage: Int,
    val hasNextPage: Boolean,
)

fun AnilistMediaPage.withTitleLanguage(language: AnilistTitleLanguage): AnilistMediaPage =
    copy(media = media.map { it.withTitleLanguage(language) })

data class AnilistMediaTag(
    val name: String,
    val category: String?,
    val isAdult: Boolean,
)

data class AnilistMangaStats(
    val count: Int,
    val chaptersRead: Int,
    val volumesRead: Int,
    val meanScore: Double?,
    val genres: List<AnilistStatItem> = emptyList(),
    val tags: List<AnilistStatItem> = emptyList(),
    val formats: List<AnilistStatItem> = emptyList(),
    val statuses: List<AnilistStatItem> = emptyList(),
)

data class AnilistStatItem(
    val name: String,
    val count: Int,
    val chaptersRead: Int,
)

data class AnilistListEntry(
    val id: Int,
    val mediaId: Int,
    val status: MediaStatus,
    val progress: Int,
    val score: Double?,
    val notes: String?,
    val private: Boolean,
    val customLists: List<String>,
    val updatedAtEpochSeconds: Long?,
    val hiddenFromStatusLists: Boolean = false,
)

data class AnilistRecommendation(
    val media: AnilistMedia,
    val rating: Int?,
)

data class AnilistRecommendationPage(
    val recommendations: List<AnilistRecommendation>,
    val currentPage: Int,
    val hasNextPage: Boolean,
)

data class AnilistMediaDetails(
    val media: AnilistMedia,
    val listEntry: AnilistListEntry?,
    val recommendationPage: AnilistRecommendationPage,
)

data class SourceDescriptor(
    val id: Long,
    val name: String,
    val lang: String,
    val packageName: String,
    val versionName: String?,
    val versionCode: Int?,
    val isNsfw: Boolean,
    val installed: Boolean,
)

data class SourceManga(
    val sourceId: Long,
    val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val description: String?,
    val author: String?,
    val artist: String?,
    val status: String?,
)

data class SourceChapter(
    val sourceId: Long,
    val mangaUrl: String,
    val url: String,
    val name: String,
    val chapterNumber: Float,
    val scanlator: String?,
    val uploadedAtEpochMillis: Long?,
)

data class ReaderPage(
    val index: Int,
    val imageUrl: String,
    val cachedFilePath: String?,
    val headers: Map<String, String> = emptyMap(),
    val sourcePageUrl: String = "",
    val imageUrlResolved: Boolean = true,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
)

data class SourceBinding(
    val mediaId: Int,
    val sourceId: Long,
    val sourcePackageName: String,
    val mangaUrl: String,
    val mangaTitle: String,
    val thumbnailUrl: String?,
    val selectedAtEpochMillis: Long,
)

data class SourceSearchResult(
    val mediaId: Int,
    val source: SourceDescriptor,
    val manga: SourceManga,
    val score: Double,
    val reasons: List<String>,
    val searchedAtEpochMillis: Long,
)

data class ReadingProgress(
    val mediaId: Int,
    val chapterUrl: String,
    val chapterNumber: Float,
    val pageIndex: Int,
    val pageScrollOffset: Int,
    val totalPages: Int,
    val readerMode: ReaderMode,
    val completed: Boolean,
    val updatedAtEpochMillis: Long,
)

data class DownloadJob(
    val id: String,
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

data class SyncMutation(
    val id: String,
    val type: SyncMutationType,
    val mediaId: Int,
    val payloadJson: String,
    val attempts: Int,
    val nextAttemptAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
)

data class CachePolicy(
    val libraryTtlMillis: Long = 6 * 60 * 60 * 1000L,
    val mediaDetailsTtlMillis: Long = 7 * 24 * 60 * 60 * 1000L,
    val anilistSearchTtlMillis: Long = 24 * 60 * 60 * 1000L,
    val browseLandingTtlMillis: Long = 15 * 60 * 1000L,
    val homeFeedTtlMillis: Long = 4 * 60 * 60 * 1000L,
    val anilistTaxonomyTtlMillis: Long = 7 * 24 * 60 * 60 * 1000L,
    val anilistTagsTtlMillis: Long = 7 * 24 * 60 * 60 * 1000L,
    val sourceSearchTtlMillis: Long = 7 * 24 * 60 * 60 * 1000L,
    val sourceChapterTtlMillis: Long = 24 * 60 * 60 * 1000L,
)
