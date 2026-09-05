package com.tankobun.app

import android.content.Context
import com.tankobun.app.cache.CachePreferences
import com.tankobun.core.model.AnilistMangaStats
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistStatItem
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.ReaderMode
import com.tankobun.app.state.LocalReadingActivity
import java.util.Base64
import java.util.Locale

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("tankobun_settings", Context.MODE_PRIVATE)
    private val resources = context.resources

    fun cachePreferences(): CachePreferences = CachePreferences(
        readerLimitMiB = preferences.getInt("cache.reader.limit.mib", 2048),
        prefetchPages = preferences.getInt("cache.reader.prefetch.pages", 6),
        prefetchUnmeteredOnly = preferences.getBoolean("cache.reader.prefetch.unmetered", true),
    ).normalized()

    fun saveCachePreferences(value: CachePreferences) {
        val normalized = value.normalized()
        preferences.edit()
            .putInt("cache.reader.limit.mib", normalized.readerLimitMiB)
            .putInt("cache.reader.prefetch.pages", normalized.prefetchPages)
            .putBoolean("cache.reader.prefetch.unmetered", normalized.prefetchUnmeteredOnly)
            .apply()
    }

    fun extensionRepositoryUrl(): String =
        preferences.getString(KEY_EXTENSION_REPOSITORY_URL, "").orEmpty()

    fun saveExtensionRepositoryUrl(url: String) {
        preferences.edit().putString(KEY_EXTENSION_REPOSITORY_URL, url).apply()
    }

    fun themePreference(): TankobunThemePreference {
        val storedDirection = preferences.getString(KEY_THEME_ART_DIRECTION, null)
            ?.let { runCatching { TankobunArtDirection.valueOf(it) }.getOrNull() }
        val storedPalette = preferences.getString(KEY_THEME_PALETTE, null)
            ?.let { runCatching { TankobunPaletteId.valueOf(it) }.getOrNull() }
        if (storedDirection != null && storedPalette != null) {
            return TankobunThemePreference(
                automatic = preferences.getBoolean(KEY_THEME_AUTOMATIC, false),
                direction = storedDirection,
                palette = storedPalette,
            ).normalized()
        }
        return legacyThemePreference(themeMode())
    }

    fun saveThemePreference(preference: TankobunThemePreference) {
        val normalized = preference.normalized()
        preferences.edit()
            .putBoolean(KEY_THEME_AUTOMATIC, normalized.automatic)
            .putString(KEY_THEME_ART_DIRECTION, normalized.direction.name)
            .putString(KEY_THEME_PALETTE, normalized.palette.name)
            .putString(KEY_THEME_MODE, normalized.toLegacyThemeMode().name)
            .apply()
    }

    fun themeMode(): TankobunThemeMode =
        preferences.getString(KEY_THEME_MODE, null)
            ?.let { stored -> runCatching { TankobunThemeMode.valueOf(stored) }.getOrNull() }
            ?.let { mode -> if (mode == TankobunThemeMode.MIDNIGHT_RAMEN) TankobunThemeMode.NEON_KOI else mode }
            ?: TankobunThemeMode.SYSTEM

    fun saveThemeMode(mode: TankobunThemeMode) {
        saveThemePreference(legacyThemePreference(mode))
    }

    fun ignoreDisplayCutout(): Boolean =
        preferences.getBoolean(KEY_IGNORE_DISPLAY_CUTOUT, true)

    fun saveIgnoreDisplayCutout(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_IGNORE_DISPLAY_CUTOUT, enabled).apply()
    }

    fun showAppStatusBar(): Boolean =
        preferences.getBoolean(KEY_SHOW_APP_STATUS_BAR, true)

    fun saveShowAppStatusBar(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_APP_STATUS_BAR, enabled).apply()
    }

    fun dockAlignment(): DockAlignment =
        preferences.getString(KEY_DOCK_ALIGNMENT, null)
            ?.let { stored -> runCatching { DockAlignment.valueOf(stored) }.getOrNull() }
            ?: DockAlignment.CENTER

    fun saveDockAlignment(alignment: DockAlignment) {
        preferences.edit().putString(KEY_DOCK_ALIGNMENT, alignment.name).apply()
    }

    fun dockIndicatorAnimation(): DockIndicatorAnimation {
        val stored = preferences.getString(KEY_DOCK_INDICATOR_ANIMATION, null)
        val animation = stored?.let { value -> runCatching { DockIndicatorAnimation.valueOf(value) }.getOrNull() }
        if (stored != null && animation == null) {
            saveDockIndicatorAnimation(DockIndicatorAnimation.POP)
        }
        return animation ?: DockIndicatorAnimation.POP
    }

    fun saveDockIndicatorAnimation(animation: DockIndicatorAnimation) {
        preferences.edit().putString(KEY_DOCK_INDICATOR_ANIMATION, animation.name).apply()
    }

    fun libraryMode(): LibraryMode =
        preferences.getString(KEY_LIBRARY_MODE, null)
            ?.let { stored -> runCatching { LibraryMode.valueOf(stored) }.getOrNull() }
            ?: LibraryMode.LOCAL

    fun saveLibraryMode(mode: LibraryMode) {
        preferences.edit().putString(KEY_LIBRARY_MODE, mode.name).apply()
    }

    fun onboardingVersion(): Int =
        preferences.getInt(KEY_ONBOARDING_VERSION, if (onboardingCompleted()) 1 else 0)

    fun saveOnboardingVersion(version: Int) {
        preferences.edit()
            .putInt(KEY_ONBOARDING_VERSION, version.coerceAtLeast(0))
            .putBoolean(KEY_ONBOARDING_COMPLETED, version > 0)
            .apply()
    }

    fun onboardingCompleted(): Boolean =
        preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun saveOnboardingCompleted(completed: Boolean) {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun readerTutorialCompleted(): Boolean =
        preferences.getBoolean(KEY_READER_TUTORIAL_COMPLETED, false)

    fun saveReaderTutorialCompleted(completed: Boolean) {
        preferences.edit().putBoolean(KEY_READER_TUTORIAL_COMPLETED, completed).apply()
    }

    fun libraryViewMode(): MediaViewMode =
        preferences.getString(KEY_LIBRARY_VIEW_MODE, null)
            ?.let { stored -> runCatching { MediaViewMode.valueOf(stored) }.getOrNull() }
            ?.supportedMediaViewMode()
            ?: MediaViewMode.COVER_WITH_INFO

    fun saveLibraryViewMode(mode: MediaViewMode) {
        preferences.edit().putString(KEY_LIBRARY_VIEW_MODE, mode.supportedMediaViewMode().name).apply()
    }

    fun libraryCoverColumns(): Int =
        preferences.getInt(KEY_LIBRARY_COVER_COLUMNS, defaultMediaCoverColumns())
            .supportedCoverColumns()

    fun saveLibraryCoverColumns(count: Int) {
        preferences.edit().putInt(KEY_LIBRARY_COVER_COLUMNS, count.supportedCoverColumns()).apply()
    }

    fun libraryShowWholeCovers(): Boolean =
        preferences.getBoolean(KEY_LIBRARY_SHOW_WHOLE_COVERS, false)

    fun saveLibraryShowWholeCovers(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_LIBRARY_SHOW_WHOLE_COVERS, enabled).apply()
    }

    fun browseViewMode(): MediaViewMode =
        preferences.getString(KEY_BROWSE_VIEW_MODE, null)
            ?.let { stored -> runCatching { MediaViewMode.valueOf(stored) }.getOrNull() }
            ?.supportedMediaViewMode()
            ?: MediaViewMode.COVER_WITH_INFO

    fun saveBrowseViewMode(mode: MediaViewMode) {
        preferences.edit().putString(KEY_BROWSE_VIEW_MODE, mode.supportedMediaViewMode().name).apply()
    }

    fun browseCoverColumns(): Int =
        preferences.getInt(KEY_BROWSE_COVER_COLUMNS, defaultMediaCoverColumns())
            .supportedCoverColumns()

    fun saveBrowseCoverColumns(count: Int) {
        preferences.edit().putInt(KEY_BROWSE_COVER_COLUMNS, count.supportedCoverColumns()).apply()
    }

    fun browseShowWholeCovers(): Boolean =
        preferences.getBoolean(KEY_BROWSE_SHOW_WHOLE_COVERS, false)

    fun saveBrowseShowWholeCovers(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BROWSE_SHOW_WHOLE_COVERS, enabled).apply()
    }

    fun readerMode(): ReaderMode =
        preferences.getString(KEY_READER_MODE, null)
            ?.let { stored -> runCatching { ReaderMode.valueOf(stored) }.getOrNull() }
            ?: ReaderMode.PAGED

    fun saveReaderMode(mode: ReaderMode) {
        preferences.edit().putString(KEY_READER_MODE, mode.name).apply()
    }

    fun readerPageGapLevel(): Int =
        preferences.getInt(KEY_READER_PAGE_GAP_LEVEL, 0).coerceIn(0, 3)

    fun saveReaderPageGapLevel(level: Int) {
        preferences.edit().putInt(KEY_READER_PAGE_GAP_LEVEL, level.coerceIn(0, 3)).apply()
    }

    fun showWebtoonChapterDividers(): Boolean =
        preferences.getBoolean(KEY_READER_WEBTOON_CHAPTER_DIVIDERS, false)

    fun saveShowWebtoonChapterDividers(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_READER_WEBTOON_CHAPTER_DIVIDERS, enabled).apply()
    }

    fun readerScreenOrientation(): ReaderScreenOrientation =
        preferences.getString(KEY_READER_SCREEN_ORIENTATION, null)
            ?.let { stored -> runCatching { ReaderScreenOrientation.valueOf(stored) }.getOrNull() }
            ?: ReaderScreenOrientation.SYSTEM

    fun saveReaderScreenOrientation(orientation: ReaderScreenOrientation) {
        preferences.edit().putString(KEY_READER_SCREEN_ORIENTATION, orientation.name).apply()
    }

    fun chapterListStartsAtFirst(): Boolean =
        preferences.getBoolean(KEY_CHAPTER_LIST_STARTS_AT_FIRST, true)

    fun saveChapterListStartsAtFirst(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_CHAPTER_LIST_STARTS_AT_FIRST, enabled).apply()
    }

    fun keepNextTenDownloads(): Boolean =
        preferences.getBoolean(KEY_KEEP_NEXT_TEN_DOWNLOADS, false)

    fun saveKeepNextTenDownloads(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_KEEP_NEXT_TEN_DOWNLOADS, enabled).apply()
    }

    fun newChapterChecksEnabled(): Boolean =
        preferences.getBoolean(KEY_NEW_CHAPTER_CHECKS_ENABLED, false)

    fun saveNewChapterChecksEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NEW_CHAPTER_CHECKS_ENABLED, enabled).apply()
    }

    fun anilistAutoSaveTrackingChanges(): Boolean =
        preferences.getBoolean(KEY_ANILIST_AUTO_SAVE_TRACKING_CHANGES, false)

    fun saveAnilistAutoSaveTrackingChanges(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ANILIST_AUTO_SAVE_TRACKING_CHANGES, enabled).apply()
    }

    fun anilistAutoSyncReaderProgress(): Boolean =
        preferences.getBoolean(KEY_ANILIST_AUTO_SYNC_READER_PROGRESS, true)

    fun anilistRefreshLibraryOnOpen(): Boolean =
        preferences.getBoolean("anilist.refresh.library.on.open", true)

    fun saveAnilistRefreshLibraryOnOpen(enabled: Boolean) {
        preferences.edit().putBoolean("anilist.refresh.library.on.open", enabled).apply()
    }

    fun libraryRefreshAttemptMillis(sessionKey: String): Long =
        if (preferences.getString("anilist.library.refresh.session", null) == sessionKey) {
            preferences.getLong("anilist.library.refresh.attempt", 0L)
        } else 0L

    fun saveLibraryRefreshAttempt(sessionKey: String, nowMillis: Long) {
        preferences.edit()
            .putString("anilist.library.refresh.session", sessionKey)
            .putLong("anilist.library.refresh.attempt", nowMillis)
            .apply()
    }

    fun saveAnilistAutoSyncReaderProgress(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ANILIST_AUTO_SYNC_READER_PROGRESS, enabled).apply()
    }

    fun anilistSyncManualReadProgress(): Boolean =
        preferences.getBoolean(KEY_ANILIST_SYNC_MANUAL_READ_PROGRESS, true)

    fun saveAnilistSyncManualReadProgress(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ANILIST_SYNC_MANUAL_READ_PROGRESS, enabled).apply()
    }

    fun autoUpdateStatusFromReading(): Boolean =
        preferences.getBoolean(KEY_AUTO_UPDATE_STATUS_FROM_READING, true)

    fun saveAutoUpdateStatusFromReading(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_UPDATE_STATUS_FROM_READING, enabled).apply()
    }

    fun showNsfwContent(): Boolean =
        preferences.getBoolean(KEY_SHOW_NSFW_CONTENT, false)

    fun saveShowNsfwContent(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_NSFW_CONTENT, enabled).apply()
    }

    fun backupFolderUri(): String? =
        preferences.getString(KEY_BACKUP_FOLDER_URI, null)

    fun saveBackupFolderUri(uri: String?) {
        preferences.edit().putString(KEY_BACKUP_FOLDER_URI, uri).apply()
    }

    fun backupSchedule(): BackupSchedule {
        val stored = preferences.getString(KEY_BACKUP_SCHEDULE, null)
        val schedule = stored?.toBackupScheduleOrNull() ?: BackupSchedule.OFF
        if (stored != null && stored != schedule.name) {
            saveBackupSchedule(schedule)
        }
        return schedule
    }

    fun saveBackupSchedule(schedule: BackupSchedule) {
        preferences.edit().putString(KEY_BACKUP_SCHEDULE, schedule.name).apply()
    }

    fun backupContent(): BackupContent =
        preferences.getString(KEY_BACKUP_CONTENT, null)
            ?.let { stored -> runCatching { BackupContent.valueOf(stored) }.getOrNull() }
            ?: BackupContent.BOTH

    fun saveBackupContent(content: BackupContent) {
        preferences.edit().putString(KEY_BACKUP_CONTENT, content.name).apply()
    }

    fun scheduledBackupRetentionCount(): Int =
        preferences.getInt(KEY_BACKUP_RETENTION_COUNT, DEFAULT_SCHEDULED_BACKUP_RETENTION_COUNT)
            .supportedScheduledBackupRetentionCount()

    fun saveScheduledBackupRetentionCount(count: Int) {
        preferences.edit()
            .putInt(KEY_BACKUP_RETENTION_COUNT, count.supportedScheduledBackupRetentionCount())
            .apply()
    }

    fun lastScheduledBackupAtEpochMillis(): Long =
        preferences.getLong(KEY_BACKUP_LAST_RUN_AT, 0L)

    fun saveLastScheduledBackupAtEpochMillis(value: Long) {
        preferences.edit().putLong(KEY_BACKUP_LAST_RUN_AT, value).apply()
    }

    fun lastNewChapterCheckAtEpochMillis(): Long =
        preferences.getLong(KEY_NEW_CHAPTER_CHECK_LAST_RUN_AT, 0L)

    fun saveLastNewChapterCheckAtEpochMillis(value: Long) {
        preferences.edit().putLong(KEY_NEW_CHAPTER_CHECK_LAST_RUN_AT, value).apply()
    }

    fun lastAppUpdateCheckAtEpochMillis(): Long =
        preferences.getLong(KEY_APP_UPDATE_LAST_CHECK_AT, 0L)

    fun saveLastAppUpdateCheckAtEpochMillis(value: Long) {
        preferences.edit().putLong(KEY_APP_UPDATE_LAST_CHECK_AT, value).apply()
    }

    fun anilistGenres(): List<String> =
        preferences.getString(KEY_ANILIST_GENRES, "").orEmpty()
            .lineSequence()
            .mapNotNull { encoded -> runCatching { decodePart(encoded) }.getOrNull() }
            .filter { it.isNotBlank() }
            .toList()

    fun anilistGenresCachedAtEpochMillis(): Long =
        preferences.getLong(KEY_ANILIST_GENRES_CACHED_AT, 0L)

    fun saveAnilistGenres(genres: List<String>, cachedAtEpochMillis: Long) {
        preferences.edit()
            .putString(KEY_ANILIST_GENRES, genres.joinToString("\n", transform = ::encodePart))
            .putLong(KEY_ANILIST_GENRES_CACHED_AT, cachedAtEpochMillis)
            .apply()
    }

    fun homeFeedCachedAtEpochMillis(includeAdult: Boolean): Long =
        preferences.getLong(
            if (includeAdult) KEY_HOME_FEED_NSFW_CACHED_AT else KEY_HOME_FEED_SAFE_CACHED_AT,
            0L,
        )

    fun saveHomeFeedCachedAtEpochMillis(includeAdult: Boolean, cachedAtEpochMillis: Long) {
        preferences.edit()
            .putLong(
                if (includeAdult) KEY_HOME_FEED_NSFW_CACHED_AT else KEY_HOME_FEED_SAFE_CACHED_AT,
                cachedAtEpochMillis,
            )
            .apply()
    }

    fun anilistTags(): List<AnilistMediaTag> =
        preferences.getString(KEY_ANILIST_TAGS, "").orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val parts = line.split('|')
                if (parts.size != 3) return@mapNotNull null
                runCatching {
                    AnilistMediaTag(
                        name = decodePart(parts[0]),
                        category = parts[1].takeIf { it.isNotBlank() }?.let(::decodePart),
                        isAdult = parts[2].toBooleanStrictOrNull() ?: false,
                    )
                }.getOrNull()
            }
            .toList()

    fun anilistTagsCachedAtEpochMillis(): Long =
        preferences.getLong(KEY_ANILIST_TAGS_CACHED_AT, 0L)

    fun saveAnilistTags(tags: List<AnilistMediaTag>, cachedAtEpochMillis: Long) {
        val payload = tags.joinToString("\n") { tag ->
            listOf(
                encodePart(tag.name),
                tag.category?.let(::encodePart).orEmpty(),
                tag.isAdult.toString(),
            ).joinToString("|")
        }
        preferences.edit()
            .putString(KEY_ANILIST_TAGS, payload)
            .putLong(KEY_ANILIST_TAGS_CACHED_AT, cachedAtEpochMillis)
            .apply()
    }

    fun sourceLanguages(): Set<String> =
        preferences.getStringSet(KEY_SOURCE_LANGUAGES, null)
            ?.map { it.trim().lowercase().replace('_', '-') }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?.plus(UNIVERSAL_SOURCE_LANGUAGE)
            ?: defaultSourceLanguages()

    fun appLanguage(): AppLanguage =
        AppLanguage.fromStorageValue(preferences.getString(KEY_APP_LANGUAGE, null))

    fun saveAppLanguage(language: AppLanguage) {
        preferences.edit().putString(KEY_APP_LANGUAGE, language.storageValue).apply()
    }

    fun saveSourceLanguages(languages: Set<String>) {
        preferences.edit()
            .putStringSet(KEY_SOURCE_LANGUAGES, languages.map { it.trim().lowercase().replace('_', '-') }.toSet())
            .apply()
    }

    fun disabledSourceKeys(): Set<String> =
        preferences.getStringSet(KEY_DISABLED_SOURCE_KEYS, emptySet())
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    fun saveDisabledSourceKeys(keys: Set<String>) {
        preferences.edit()
            .putStringSet(KEY_DISABLED_SOURCE_KEYS, keys.map { it.trim() }.filter { it.isNotBlank() }.toSet())
            .apply()
    }

    fun viewerName(): String? =
        preferences.getString(KEY_VIEWER_NAME, null)

    fun saveViewerName(name: String?) {
        preferences.edit().putString(KEY_VIEWER_NAME, name).apply()
    }

    fun viewerAvatarUrl(): String? =
        preferences.getString(KEY_VIEWER_AVATAR_URL, null)

    fun saveViewerAvatarUrl(url: String?) {
        preferences.edit().putString(KEY_VIEWER_AVATAR_URL, url).apply()
    }

    fun viewerBannerImageUrl(): String? =
        preferences.getString(KEY_VIEWER_BANNER_IMAGE_URL, null)

    fun saveViewerBannerImageUrl(url: String?) {
        preferences.edit().putString(KEY_VIEWER_BANNER_IMAGE_URL, url).apply()
    }

    fun customProfileAvatarUri(): String? =
        preferences.getString(KEY_CUSTOM_PROFILE_AVATAR_URI, null)

    fun saveCustomProfileAvatarUri(uri: String?) {
        preferences.edit().putString(KEY_CUSTOM_PROFILE_AVATAR_URI, uri).apply()
    }

    fun customProfileBannerUri(): String? =
        preferences.getString(KEY_CUSTOM_PROFILE_BANNER_URI, null)

    fun saveCustomProfileBannerUri(uri: String?) {
        preferences.edit().putString(KEY_CUSTOM_PROFILE_BANNER_URI, uri).apply()
    }

    fun localReadingActivity(): LocalReadingActivity {
        val parts = preferences.getString(KEY_LOCAL_READING_ACTIVITY, null)?.split('|').orEmpty()
        if (parts.size != 10) return LocalReadingActivity()
        return LocalReadingActivity(
            generatedAtEpochMillis = parts[0].toLongOrNull() ?: 0L,
            chaptersTracked = parts[1].toIntOrNull() ?: 0,
            chaptersToday = parts[2].toIntOrNull() ?: 0,
            chaptersLast7Days = parts[3].toIntOrNull() ?: 0,
            chaptersLast30Days = parts[4].toIntOrNull() ?: 0,
            averagePerActiveDay30 = parts[5].toDoubleOrNull() ?: 0.0,
            currentStreakDays = parts[6].toIntOrNull() ?: 0,
            longestStreakDays = parts[7].toIntOrNull() ?: 0,
            totalReadingDays = parts[8].toIntOrNull() ?: 0,
            last14Days = parts[9].split(',').mapNotNull(String::toIntOrNull).takeIf { it.size == 14 }
                ?: List(14) { 0 },
        )
    }

    fun saveLocalReadingActivity(activity: LocalReadingActivity) {
        val encoded = listOf(
            activity.generatedAtEpochMillis,
            activity.chaptersTracked,
            activity.chaptersToday,
            activity.chaptersLast7Days,
            activity.chaptersLast30Days,
            activity.averagePerActiveDay30,
            activity.currentStreakDays,
            activity.longestStreakDays,
            activity.totalReadingDays,
            activity.last14Days.joinToString(","),
        ).joinToString("|")
        preferences.edit().putString(KEY_LOCAL_READING_ACTIVITY, encoded).apply()
    }

    fun anilistMangaStats(): AnilistMangaStats? {
        val lines = preferences.getString(KEY_ANILIST_MANGA_STATS, null)
            ?.lineSequence()
            ?.filter { it.isNotBlank() }
            ?.toList()
            .orEmpty()
        val header = lines.firstOrNull()?.split('|') ?: return null
        if (header.size != 4) return null
        return AnilistMangaStats(
            count = header[0].toIntOrNull() ?: 0,
            chaptersRead = header[1].toIntOrNull() ?: 0,
            volumesRead = header[2].toIntOrNull() ?: 0,
            meanScore = header[3].toDoubleOrNull(),
            genres = lines.statItems("genre"),
            tags = lines.statItems("tag"),
            formats = lines.statItems("format"),
            statuses = lines.statItems("status"),
        )
    }

    fun saveAnilistMangaStats(stats: AnilistMangaStats?) {
        if (stats == null) {
            preferences.edit().remove(KEY_ANILIST_MANGA_STATS).apply()
            return
        }
        val lines = buildList {
            add(
                listOf(
                    stats.count.toString(),
                    stats.chaptersRead.toString(),
                    stats.volumesRead.toString(),
                    stats.meanScore?.toString().orEmpty(),
                ).joinToString("|"),
            )
            stats.genres.forEach { add(it.toStatsLine("genre")) }
            stats.tags.forEach { add(it.toStatsLine("tag")) }
            stats.formats.forEach { add(it.toStatsLine("format")) }
            stats.statuses.forEach { add(it.toStatsLine("status")) }
        }
        preferences.edit()
            .putString(KEY_ANILIST_MANGA_STATS, lines.joinToString("\n"))
            .apply()
    }

    fun anilistScoreFormat(): AnilistScoreFormat =
        preferences.getString(KEY_ANILIST_SCORE_FORMAT, null)
            ?.let { stored -> runCatching { AnilistScoreFormat.valueOf(stored) }.getOrNull() }
            ?: AnilistScoreFormat.POINT_100

    fun saveAnilistScoreFormat(format: AnilistScoreFormat) {
        preferences.edit().putString(KEY_ANILIST_SCORE_FORMAT, format.name).apply()
    }

    fun anilistTitleLanguage(): AnilistTitleLanguage =
        preferences.getString(KEY_ANILIST_TITLE_LANGUAGE, null)
            ?.let { stored -> runCatching { AnilistTitleLanguage.valueOf(stored) }.getOrNull() }
            ?: AnilistTitleLanguage.ROMAJI

    fun saveAnilistTitleLanguage(language: AnilistTitleLanguage) {
        preferences.edit().putString(KEY_ANILIST_TITLE_LANGUAGE, language.name).apply()
    }

    fun anilistCustomLists(): List<String> =
        preferences.getString(KEY_ANILIST_CUSTOM_LISTS, "").orEmpty()
            .lineSequence()
            .mapNotNull { encoded -> runCatching { decodePart(encoded) }.getOrNull() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .toList()

    fun saveAnilistCustomLists(lists: List<String>) {
        preferences.edit()
            .putString(
                KEY_ANILIST_CUSTOM_LISTS,
                lists.map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase(Locale.ROOT) }
                    .joinToString("\n", transform = ::encodePart),
            )
            .apply()
    }

    fun pendingAnilistOAuthState(): String? =
        preferences.getString(KEY_ANILIST_OAUTH_STATE, null)

    fun savePendingAnilistOAuthState(state: String?) {
        preferences.edit().apply {
            if (state.isNullOrBlank()) {
                remove(KEY_ANILIST_OAUTH_STATE)
            } else {
                putString(KEY_ANILIST_OAUTH_STATE, state)
            }
        }.apply()
    }

    fun librarySyncedAtEpochMillis(): Long =
        preferences.getLong(KEY_LIBRARY_SYNCED_AT, 0L)

    fun saveLibrarySyncedAtEpochMillis(value: Long) {
        preferences.edit().putLong(KEY_LIBRARY_SYNCED_AT, value).apply()
    }

    private fun defaultMediaCoverColumns(): Int =
        if (resources.configuration.smallestScreenWidthDp in 1 until PHONE_TABLET_BREAKPOINT_DP) {
            2
        } else {
            DEFAULT_MEDIA_COVER_COLUMNS
        }

    private companion object {
        const val KEY_EXTENSION_REPOSITORY_URL = "extension.repository.url"
        const val KEY_THEME_MODE = "theme.mode"
        const val KEY_THEME_AUTOMATIC = "theme.automatic"
        const val KEY_THEME_ART_DIRECTION = "theme.art.direction"
        const val KEY_THEME_PALETTE = "theme.palette"
        const val KEY_IGNORE_DISPLAY_CUTOUT = "layout.ignore.display.cutout"
        const val KEY_SHOW_APP_STATUS_BAR = "layout.show.app.status.bar"
        const val KEY_DOCK_ALIGNMENT = "layout.dock.alignment"
        const val KEY_DOCK_INDICATOR_ANIMATION = "layout.dock.indicator.animation"
        const val KEY_LIBRARY_MODE = "library.mode"
        const val KEY_ONBOARDING_VERSION = "onboarding.version"
        const val KEY_ONBOARDING_COMPLETED = "onboarding.completed"
        const val KEY_READER_TUTORIAL_COMPLETED = "reader.tutorial.completed"
        const val KEY_LIBRARY_VIEW_MODE = "library.view.mode"
        const val KEY_LIBRARY_COVER_COLUMNS = "library.cover.columns"
        const val KEY_LIBRARY_SHOW_WHOLE_COVERS = "library.show.whole.covers"
        const val KEY_BROWSE_VIEW_MODE = "browse.view.mode"
        const val KEY_BROWSE_COVER_COLUMNS = "browse.cover.columns"
        const val KEY_BROWSE_SHOW_WHOLE_COVERS = "browse.show.whole.covers"
        const val KEY_READER_MODE = "reader.mode"
        const val KEY_READER_PAGE_GAP_LEVEL = "reader.page.gap.level"
        const val KEY_READER_WEBTOON_CHAPTER_DIVIDERS = "reader.webtoon.chapter.dividers"
        const val KEY_READER_SCREEN_ORIENTATION = "reader.screen.orientation"
        const val KEY_CHAPTER_LIST_STARTS_AT_FIRST = "chapters.list.starts.at.first"
        const val KEY_KEEP_NEXT_TEN_DOWNLOADS = "downloads.keep.next.ten"
        const val KEY_NEW_CHAPTER_CHECKS_ENABLED = "library.new.chapter.checks.enabled"
        const val KEY_NEW_CHAPTER_CHECK_LAST_RUN_AT = "library.new.chapter.check.last.run.at"
        const val KEY_APP_UPDATE_LAST_CHECK_AT = "app.update.last.check.at"
        const val KEY_ANILIST_GENRES = "anilist.genres"
        const val KEY_ANILIST_GENRES_CACHED_AT = "anilist.genres.cached.at"
        const val KEY_HOME_FEED_SAFE_CACHED_AT = "home.feed.safe.cached.at"
        const val KEY_HOME_FEED_NSFW_CACHED_AT = "home.feed.nsfw.cached.at"
        const val KEY_ANILIST_AUTO_SAVE_TRACKING_CHANGES = "anilist.auto.save.tracking.changes"
        const val KEY_ANILIST_AUTO_SYNC_READER_PROGRESS = "anilist.auto.sync.reader.progress"
        const val KEY_ANILIST_SYNC_MANUAL_READ_PROGRESS = "anilist.sync.manual.read.progress"
        const val KEY_AUTO_UPDATE_STATUS_FROM_READING = "library.auto.update.status.from.reading"
        const val KEY_SHOW_NSFW_CONTENT = "profile.show.nsfw.content"
        const val KEY_BACKUP_FOLDER_URI = "backup.folder.uri"
        const val KEY_BACKUP_SCHEDULE = "backup.schedule"
        const val KEY_BACKUP_CONTENT = "backup.content"
        const val KEY_BACKUP_RETENTION_COUNT = "backup.retention.count"
        const val KEY_BACKUP_LAST_RUN_AT = "backup.last.run.at"
        const val KEY_ANILIST_TAGS = "anilist.tags"
        const val KEY_ANILIST_TAGS_CACHED_AT = "anilist.tags.cached.at"
        const val KEY_APP_LANGUAGE = "app.language"
        const val KEY_SOURCE_LANGUAGES = "source.languages"
        const val KEY_DISABLED_SOURCE_KEYS = "source.disabled.keys"
        const val KEY_VIEWER_NAME = "anilist.viewer.name"
        const val KEY_VIEWER_AVATAR_URL = "anilist.viewer.avatar.url"
        const val KEY_VIEWER_BANNER_IMAGE_URL = "anilist.viewer.banner.image.url"
        const val KEY_CUSTOM_PROFILE_AVATAR_URI = "profile.custom.avatar.uri"
        const val KEY_CUSTOM_PROFILE_BANNER_URI = "profile.custom.banner.uri"
        const val KEY_LOCAL_READING_ACTIVITY = "profile.local.reading.activity"
        const val KEY_ANILIST_MANGA_STATS = "anilist.viewer.manga.stats"
        const val KEY_ANILIST_SCORE_FORMAT = "anilist.score.format"
        const val KEY_ANILIST_TITLE_LANGUAGE = "anilist.title.language"
        const val KEY_ANILIST_CUSTOM_LISTS = "anilist.custom.lists"
        const val KEY_ANILIST_OAUTH_STATE = "anilist.oauth.state"
        const val KEY_LIBRARY_SYNCED_AT = "anilist.library.synced.at"
        const val PHONE_TABLET_BREAKPOINT_DP = 600
    }
}

private fun List<String>.statItems(section: String): List<AnilistStatItem> =
    drop(1).mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size != 4 || parts[0] != section) return@mapNotNull null
        runCatching {
            AnilistStatItem(
                name = decodePart(parts[1]),
                count = parts[2].toIntOrNull() ?: 0,
                chaptersRead = parts[3].toIntOrNull() ?: 0,
            )
        }.getOrNull()
    }

private fun AnilistStatItem.toStatsLine(section: String): String =
    listOf(section, encodePart(name), count.toString(), chaptersRead.toString()).joinToString("|")

private fun encodePart(value: String): String =
    Base64.getUrlEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

private fun decodePart(value: String): String =
    String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)

enum class LibraryMode {
    LOCAL,
    ANILIST,
}

enum class TankobunThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    BUNNY_MOCHI,
    PEACH_SODA,
    MATCHA_MILK,
    SAKURA_MINT,
    CLOUDBERRY_POP,
    YUZU_GARDEN,
    MIDNIGHT_RAMEN,
    STARRY_INK,
    PLUM_NIGHT,
    NEON_KOI,
    MOON_JELLY,
    INKBERRY_FIZZ,
    CHARCOAL_GOLD,
}

enum class MediaViewMode {
    COVER_GRID,
    COVER_WITH_INFO,
    MASONRY,
    JUSTIFIED,
    LIST,
}

enum class DockAlignment {
    LEFT,
    CENTER,
    RIGHT,
}

enum class DockIndicatorAnimation {
    BOUNCY,
    INCHWORM,
    RUBBER_BAND,
    POP,
    COMET,
}

enum class ReaderScreenOrientation {
    SYSTEM,
    PORTRAIT,
    LANDSCAPE,
}

enum class BackupSchedule {
    OFF,
    DAILY,
    WEEKLY,
    MONTHLY,
}

enum class BackupContent {
    LIBRARY,
    SETTINGS,
    BOTH,
}

const val SCHEDULED_BACKUP_RETENTION_UNLIMITED = 0
const val DEFAULT_SCHEDULED_BACKUP_RETENTION_COUNT = 10
private const val MIN_SCHEDULED_BACKUP_RETENTION_COUNT = 1
private const val MAX_SCHEDULED_BACKUP_RETENTION_COUNT = 100

fun BackupContent.includesLibrary(): Boolean =
    this == BackupContent.LIBRARY || this == BackupContent.BOTH

fun BackupContent.includesSettings(): Boolean =
    this == BackupContent.SETTINGS || this == BackupContent.BOTH

fun MediaViewMode.supportedMediaViewMode(): MediaViewMode =
    when (this) {
        MediaViewMode.MASONRY,
        MediaViewMode.JUSTIFIED -> MediaViewMode.COVER_GRID
        else -> this
    }

const val DEFAULT_MEDIA_COVER_COLUMNS = 4
private const val MIN_MEDIA_COVER_COLUMNS = 2
private const val MAX_MEDIA_COVER_COLUMNS = 8

fun Int.supportedCoverColumns(): Int =
    coerceIn(MIN_MEDIA_COVER_COLUMNS, MAX_MEDIA_COVER_COLUMNS)

fun defaultSourceLanguages(): Set<String> =
    buildSet {
        val locale = Locale.getDefault()
        val language = locale.language.lowercase(Locale.ROOT)
        val tag = locale.toLanguageTag().lowercase(Locale.ROOT)
        if (language.isNotBlank()) add(language)
        if (tag.isNotBlank()) add(tag)
        add(UNIVERSAL_SOURCE_LANGUAGE)
    }

const val UNIVERSAL_SOURCE_LANGUAGE = "all"

fun Int.supportedScheduledBackupRetentionCount(): Int =
    if (this == SCHEDULED_BACKUP_RETENTION_UNLIMITED) {
        SCHEDULED_BACKUP_RETENTION_UNLIMITED
    } else {
        coerceIn(MIN_SCHEDULED_BACKUP_RETENTION_COUNT, MAX_SCHEDULED_BACKUP_RETENTION_COUNT)
    }

private fun String.toBackupScheduleOrNull(): BackupSchedule? {
    val normalized = trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[_\\s-]+"), "")
    return when (normalized) {
        "off", "none", "disabled", "disable", "desativado", "desativada", "desligado", "desligada" -> BackupSchedule.OFF
        "daily", "day", "dia", "diario", "diaria", "diariamente" -> BackupSchedule.DAILY
        "weekly", "week", "semana", "semanal" -> BackupSchedule.WEEKLY
        "monthly", "month", "mes", "mensal" -> BackupSchedule.MONTHLY
        else -> runCatching { BackupSchedule.valueOf(trim().uppercase(Locale.ROOT)) }.getOrNull()
    }
}
