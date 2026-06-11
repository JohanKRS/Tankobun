package com.tankobun.app

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderMode

@StringRes
internal fun AppLanguage.labelRes(): Int =
    when (this) {
        AppLanguage.SYSTEM -> R.string.settings_language_system_default
        AppLanguage.ENGLISH -> R.string.settings_language_english
        AppLanguage.PORTUGUESE_BRAZIL -> R.string.settings_language_portuguese_brazil
        AppLanguage.SPANISH -> R.string.settings_language_spanish
    }

@Composable
@ReadOnlyComposable
internal fun AppLanguage.settingsLabel(): String =
    tankobunString(labelRes())

@StringRes
internal fun DockAlignment.labelRes(): Int =
    when (this) {
        DockAlignment.LEFT -> R.string.dock_left
        DockAlignment.CENTER -> R.string.dock_center
        DockAlignment.RIGHT -> R.string.dock_right
    }

@Composable
@ReadOnlyComposable
internal fun DockAlignment.settingsLabel(): String =
    tankobunString(labelRes())

@StringRes
internal fun MediaViewMode.labelRes(): Int =
    when (supportedMediaViewMode()) {
        MediaViewMode.COVER_GRID -> R.string.media_view_cover_only
        MediaViewMode.COVER_WITH_INFO -> R.string.media_view_cover_info
        MediaViewMode.LIST -> R.string.media_view_list
        MediaViewMode.MASONRY,
        MediaViewMode.JUSTIFIED -> R.string.media_view_cover_only
    }

@Composable
@ReadOnlyComposable
internal fun MediaViewMode.mediaViewLabel(): String =
    tankobunString(labelRes())

@StringRes
internal fun ReaderMode.labelRes(): Int =
    when (this) {
        ReaderMode.PAGED -> R.string.reader_paged
        ReaderMode.WEBTOON -> R.string.reader_webtoon
    }

@Composable
@ReadOnlyComposable
internal fun ReaderMode.readerModeLabel(): String =
    tankobunString(labelRes())

@StringRes
internal fun BackupSchedule.labelRes(): Int =
    when (this) {
        BackupSchedule.OFF -> R.string.backup_schedule_off
        BackupSchedule.DAILY -> R.string.backup_schedule_daily
        BackupSchedule.WEEKLY -> R.string.backup_schedule_weekly
        BackupSchedule.MONTHLY -> R.string.backup_schedule_monthly
    }

@Composable
@ReadOnlyComposable
internal fun BackupSchedule.label(): String =
    tankobunString(labelRes())

@StringRes
internal fun AnilistTitleLanguage.labelRes(): Int =
    when (this) {
        AnilistTitleLanguage.ROMAJI -> R.string.anilist_title_romaji
        AnilistTitleLanguage.ENGLISH -> R.string.anilist_title_english
        AnilistTitleLanguage.NATIVE -> R.string.anilist_title_native
        AnilistTitleLanguage.ROMAJI_STYLISED -> R.string.anilist_title_romaji_styled
        AnilistTitleLanguage.ENGLISH_STYLISED -> R.string.anilist_title_english_styled
        AnilistTitleLanguage.NATIVE_STYLISED -> R.string.anilist_title_native_styled
    }

@Composable
@ReadOnlyComposable
internal fun AnilistTitleLanguage.settingsLabel(): String =
    tankobunString(labelRes())

@StringRes
internal fun AnilistScoreFormat.labelRes(): Int =
    when (this) {
        AnilistScoreFormat.POINT_100 -> R.string.anilist_score_100_point
        AnilistScoreFormat.POINT_10_DECIMAL -> R.string.anilist_score_10_decimal
        AnilistScoreFormat.POINT_10 -> R.string.anilist_score_10_point
        AnilistScoreFormat.POINT_5 -> R.string.anilist_score_5_stars
        AnilistScoreFormat.POINT_3 -> R.string.anilist_score_3_smileys
    }

@Composable
@ReadOnlyComposable
internal fun AnilistScoreFormat.settingsLabel(): String =
    tankobunString(labelRes())

@StringRes
internal fun MediaStatus.sectionTitleRes(): Int =
    when (this) {
        MediaStatus.CURRENT -> R.string.library_status_reading
        MediaStatus.PLANNING -> R.string.library_status_plan_to_read
        MediaStatus.COMPLETED -> R.string.library_status_completed
        MediaStatus.PAUSED -> R.string.library_status_paused
        MediaStatus.DROPPED -> R.string.library_status_dropped
        MediaStatus.REPEATING -> R.string.library_status_rereading
        MediaStatus.UNKNOWN -> R.string.library_status_other
    }

@StringRes
internal fun MediaStatus.badgeLabelRes(): Int =
    when (this) {
        MediaStatus.CURRENT -> R.string.library_status_reading
        MediaStatus.PLANNING -> R.string.library_status_planning
        MediaStatus.COMPLETED -> R.string.library_status_completed
        MediaStatus.PAUSED -> R.string.library_status_paused
        MediaStatus.DROPPED -> R.string.library_status_dropped
        MediaStatus.REPEATING -> R.string.library_status_rereading
        MediaStatus.UNKNOWN -> R.string.library_status_tracked
    }

@StringRes
internal fun MediaStatus.trackingLabelRes(): Int =
    when (this) {
        MediaStatus.CURRENT -> R.string.library_status_reading
        MediaStatus.PLANNING -> R.string.library_status_plan
        MediaStatus.COMPLETED -> R.string.library_status_completed
        MediaStatus.PAUSED -> R.string.library_status_paused
        MediaStatus.DROPPED -> R.string.library_status_dropped
        MediaStatus.REPEATING -> R.string.library_status_rereading
        MediaStatus.UNKNOWN -> R.string.common_unknown
    }

@Composable
@ReadOnlyComposable
internal fun MediaStatus.statusLabel(): String =
    tankobunString(badgeLabelRes())

@Composable
@ReadOnlyComposable
internal fun MediaStatus.trackedBadgeLabel(): String =
    tankobunString(badgeLabelRes())

@Composable
@ReadOnlyComposable
internal fun MediaStatus.displayName(): String =
    tankobunString(trackingLabelRes())

@StringRes
internal fun DownloadState.labelRes(): Int =
    when (this) {
        DownloadState.QUEUED -> R.string.common_queued
        DownloadState.RUNNING -> R.string.downloads_status_downloading
        DownloadState.PAUSED -> R.string.common_paused
        DownloadState.COMPLETE -> R.string.downloads_status_complete
        DownloadState.FAILED -> R.string.common_failed
    }

@Composable
@ReadOnlyComposable
internal fun DownloadState.statusLabel(): String =
    tankobunString(labelRes())
