package com.tankobun.core.database

import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceBinding
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceSearchResult
import com.tankobun.core.model.SyncMutation

fun AnilistMedia.toEntity(fetchedAtEpochMillis: Long): AnilistMediaEntity =
    AnilistMediaEntity(
        id = id,
        idMal = idMal,
        titleRomaji = title.romaji,
        titleEnglish = title.english,
        titleNative = title.native,
        titleUserPreferred = title.userPreferred,
        description = description,
        coverImage = coverImage,
        bannerImage = bannerImage,
        chapters = chapters,
        volumes = volumes,
        format = format,
        status = status,
        averageScore = averageScore,
        popularity = popularity,
        startDateYear = startDateYear,
        endDateYear = endDateYear,
        siteUrl = siteUrl,
        genres = genres,
        synonyms = synonyms,
        staff = staff,
        tags = tags,
        isAdult = isAdult,
        updatedAtEpochSeconds = updatedAtEpochSeconds,
        fetchedAtEpochMillis = fetchedAtEpochMillis,
    )

fun AnilistMediaEntity.toModel(): AnilistMedia =
    AnilistMedia(
        id = id,
        idMal = idMal,
        title = AnilistTitle(titleRomaji, titleEnglish, titleNative, titleUserPreferred),
        description = description,
        coverImage = coverImage,
        bannerImage = bannerImage,
        chapters = chapters,
        volumes = volumes,
        format = format,
        status = status,
        averageScore = averageScore,
        popularity = popularity,
        startDateYear = startDateYear,
        endDateYear = endDateYear,
        siteUrl = siteUrl,
        genres = genres,
        synonyms = synonyms,
        staff = staff,
        tags = tags,
        isAdult = isAdult,
        updatedAtEpochSeconds = updatedAtEpochSeconds,
    )

fun AnilistListEntry.toEntity(fetchedAtEpochMillis: Long): AnilistListEntryEntity =
    AnilistListEntryEntity(
        id = id,
        mediaId = mediaId,
        status = status,
        progress = progress,
        score = score,
        notes = notes,
        private = private,
        customLists = customLists,
        updatedAtEpochSeconds = updatedAtEpochSeconds,
        fetchedAtEpochMillis = fetchedAtEpochMillis,
    )

fun AnilistListEntryEntity.toModel(): AnilistListEntry =
    AnilistListEntry(
        id = id,
        mediaId = mediaId,
        status = status,
        progress = progress,
        score = score,
        notes = notes,
        private = private,
        customLists = customLists,
        updatedAtEpochSeconds = updatedAtEpochSeconds,
    )

fun AnilistRecommendation.toEntity(mediaId: Int, fetchedAtEpochMillis: Long): AnilistRecommendationEntity =
    AnilistRecommendationEntity(
        mediaId = mediaId,
        recommendationMediaId = media.id,
        rating = rating,
        fetchedAtEpochMillis = fetchedAtEpochMillis,
    )

fun SourceBinding.toEntity(): SourceBindingEntity =
    SourceBindingEntity(mediaId, sourceId, sourcePackageName, mangaUrl, mangaTitle, thumbnailUrl, selectedAtEpochMillis)

fun SourceBindingEntity.toModel(): SourceBinding =
    SourceBinding(mediaId, sourceId, sourcePackageName, mangaUrl, mangaTitle, thumbnailUrl, selectedAtEpochMillis)

fun SourceSearchResult.toEntity(): SourceSearchResultEntity =
    SourceSearchResultEntity(
        mediaId = mediaId,
        sourceId = source.id,
        sourcePackageName = source.packageName,
        sourceName = source.name,
        sourceLang = source.lang,
        mangaUrl = manga.url,
        mangaTitle = manga.title,
        mangaThumbnailUrl = manga.thumbnailUrl,
        score = score,
        reasons = reasons,
        searchedAtEpochMillis = searchedAtEpochMillis,
    )

fun SourceChapter.toEntity(fetchedAtEpochMillis: Long): SourceChapterEntity =
    SourceChapterEntity(
        sourceId = sourceId,
        mangaUrl = mangaUrl,
        chapterUrl = url,
        name = name,
        chapterNumber = chapterNumber,
        scanlator = scanlator,
        uploadedAtEpochMillis = uploadedAtEpochMillis,
        fetchedAtEpochMillis = fetchedAtEpochMillis,
    )

fun SourceChapterEntity.toModel(): SourceChapter =
    SourceChapter(
        sourceId = sourceId,
        mangaUrl = mangaUrl,
        url = chapterUrl,
        name = name,
        chapterNumber = chapterNumber,
        scanlator = scanlator,
        uploadedAtEpochMillis = uploadedAtEpochMillis,
    )

fun ReadingProgress.toEntity(): ReadingProgressEntity =
    ReadingProgressEntity(mediaId, chapterUrl, chapterNumber, pageIndex, pageScrollOffset, totalPages, readerMode, completed, updatedAtEpochMillis)

fun ReadingProgressEntity.toModel(): ReadingProgress =
    ReadingProgress(mediaId, chapterUrl, chapterNumber, pageIndex, pageScrollOffset, totalPages, readerMode, completed, updatedAtEpochMillis)

fun DownloadJob.toEntity(): DownloadJobEntity =
    DownloadJobEntity(id, mediaId, sourceId, mangaUrl, chapterUrl, chapterName, state, pageCount, completedPages, retryCount, createdAtEpochMillis, updatedAtEpochMillis)

fun DownloadJobEntity.toModel(): DownloadJob =
    DownloadJob(id, mediaId, sourceId, mangaUrl, chapterUrl, chapterName, state, pageCount, completedPages, retryCount, createdAtEpochMillis, updatedAtEpochMillis)

fun ReaderPage.toDownloadPageEntity(job: DownloadJob, filePath: String, updatedAtEpochMillis: Long): DownloadPageEntity =
    DownloadPageEntity(
        jobId = job.id,
        mediaId = job.mediaId,
        sourceId = job.sourceId,
        mangaUrl = job.mangaUrl,
        chapterUrl = job.chapterUrl,
        pageIndex = index,
        imageUrl = imageUrl,
        filePath = filePath,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

fun DownloadPageEntity.toReaderPage(): ReaderPage =
    ReaderPage(
        index = pageIndex,
        imageUrl = imageUrl,
        cachedFilePath = filePath,
        headers = emptyMap(),
    )

fun SyncMutation.toEntity(): SyncMutationEntity =
    SyncMutationEntity(id, type, mediaId, payloadJson, attempts, nextAttemptAtEpochMillis, createdAtEpochMillis)
