package com.tankobun.app

import android.content.Context
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.ReaderMode
import java.util.Base64
import java.util.Locale

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("tankobun_settings", Context.MODE_PRIVATE)
    private val resources = context.resources

    fun extensionRepositoryUrl(): String =
        preferences.getString(KEY_EXTENSION_REPOSITORY_URL, "").orEmpty()

    fun saveExtensionRepositoryUrl(url: String) {
        preferences.edit().putString(KEY_EXTENSION_REPOSITORY_URL, url).apply()
    }

    fun themeMode(): TankobunThemeMode =
        preferences.getString(KEY_THEME_MODE, null)
            ?.let { stored -> runCatching { TankobunThemeMode.valueOf(stored) }.getOrNull() }
            ?.let { mode -> if (mode == TankobunThemeMode.MIDNIGHT_RAMEN) TankobunThemeMode.NEON_KOI else mode }
            ?: TankobunThemeMode.SYSTEM

    fun saveThemeMode(mode: TankobunThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun ignoreDisplayCutout(): Boolean =
        preferences.getBoolean(KEY_IGNORE_DISPLAY_CUTOUT, false)

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
            ?: MediaViewMode.COVER_GRID

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
            ?: MediaViewMode.COVER_GRID

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

    fun anilistAutoSaveTrackingChanges(): Boolean =
        preferences.getBoolean(KEY_ANILIST_AUTO_SAVE_TRACKING_CHANGES, false)

    fun saveAnilistAutoSaveTrackingChanges(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ANILIST_AUTO_SAVE_TRACKING_CHANGES, enabled).apply()
    }

    fun anilistAutoSyncReaderProgress(): Boolean =
        preferences.getBoolean(KEY_ANILIST_AUTO_SYNC_READER_PROGRESS, true)

    fun saveAnilistAutoSyncReaderProgress(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ANILIST_AUTO_SYNC_READER_PROGRESS, enabled).apply()
    }

    fun anilistSyncManualReadProgress(): Boolean =
        preferences.getBoolean(KEY_ANILIST_SYNC_MANUAL_READ_PROGRESS, true)

    fun saveAnilistSyncManualReadProgress(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ANILIST_SYNC_MANUAL_READ_PROGRESS, enabled).apply()
    }

    fun backupFolderUri(): String? =
        preferences.getString(KEY_BACKUP_FOLDER_URI, null)

    fun saveBackupFolderUri(uri: String?) {
        preferences.edit().putString(KEY_BACKUP_FOLDER_URI, uri).apply()
    }

    fun backupSchedule(): BackupSchedule =
        preferences.getString(KEY_BACKUP_SCHEDULE, null)
            ?.let { stored -> runCatching { BackupSchedule.valueOf(stored) }.getOrNull() }
            ?: BackupSchedule.OFF

    fun saveBackupSchedule(schedule: BackupSchedule) {
        preferences.edit().putString(KEY_BACKUP_SCHEDULE, schedule.name).apply()
    }

    fun lastScheduledBackupAtEpochMillis(): Long =
        preferences.getLong(KEY_BACKUP_LAST_RUN_AT, 0L)

    fun saveLastScheduledBackupAtEpochMillis(value: Long) {
        preferences.edit().putLong(KEY_BACKUP_LAST_RUN_AT, value).apply()
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
        const val KEY_IGNORE_DISPLAY_CUTOUT = "layout.ignore.display.cutout"
        const val KEY_SHOW_APP_STATUS_BAR = "layout.show.app.status.bar"
        const val KEY_DOCK_ALIGNMENT = "layout.dock.alignment"
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
        const val KEY_CHAPTER_LIST_STARTS_AT_FIRST = "chapters.list.starts.at.first"
        const val KEY_KEEP_NEXT_TEN_DOWNLOADS = "downloads.keep.next.ten"
        const val KEY_ANILIST_AUTO_SAVE_TRACKING_CHANGES = "anilist.auto.save.tracking.changes"
        const val KEY_ANILIST_AUTO_SYNC_READER_PROGRESS = "anilist.auto.sync.reader.progress"
        const val KEY_ANILIST_SYNC_MANUAL_READ_PROGRESS = "anilist.sync.manual.read.progress"
        const val KEY_BACKUP_FOLDER_URI = "backup.folder.uri"
        const val KEY_BACKUP_SCHEDULE = "backup.schedule"
        const val KEY_BACKUP_LAST_RUN_AT = "backup.last.run.at"
        const val KEY_ANILIST_TAGS = "anilist.tags"
        const val KEY_ANILIST_TAGS_CACHED_AT = "anilist.tags.cached.at"
        const val KEY_APP_LANGUAGE = "app.language"
        const val KEY_SOURCE_LANGUAGES = "source.languages"
        const val KEY_DISABLED_SOURCE_KEYS = "source.disabled.keys"
        const val KEY_VIEWER_NAME = "anilist.viewer.name"
        const val KEY_ANILIST_SCORE_FORMAT = "anilist.score.format"
        const val KEY_ANILIST_TITLE_LANGUAGE = "anilist.title.language"
        const val KEY_ANILIST_CUSTOM_LISTS = "anilist.custom.lists"
        const val KEY_LIBRARY_SYNCED_AT = "anilist.library.synced.at"
        const val PHONE_TABLET_BREAKPOINT_DP = 600
    }
}

private fun encodePart(value: String): String =
    Base64.getUrlEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

private fun decodePart(value: String): String =
    String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)

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

enum class BackupSchedule {
    OFF,
    DAILY,
    WEEKLY,
    MONTHLY,
}

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
