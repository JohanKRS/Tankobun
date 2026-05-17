package com.tankobun.app

import android.content.Context
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.ReaderMode
import java.util.Base64
import java.util.Locale

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("tankobun_settings", Context.MODE_PRIVATE)

    fun extensionRepositoryUrl(): String =
        preferences.getString(KEY_EXTENSION_REPOSITORY_URL, "").orEmpty()

    fun saveExtensionRepositoryUrl(url: String) {
        preferences.edit().putString(KEY_EXTENSION_REPOSITORY_URL, url).apply()
    }

    fun themeMode(): TankobunThemeMode =
        preferences.getString(KEY_THEME_MODE, null)
            ?.let { stored -> runCatching { TankobunThemeMode.valueOf(stored) }.getOrNull() }
            ?: TankobunThemeMode.SYSTEM

    fun saveThemeMode(mode: TankobunThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun libraryViewMode(): MediaViewMode =
        preferences.getString(KEY_LIBRARY_VIEW_MODE, null)
            ?.let { stored -> runCatching { MediaViewMode.valueOf(stored) }.getOrNull() }
            ?: MediaViewMode.COVER_GRID

    fun saveLibraryViewMode(mode: MediaViewMode) {
        preferences.edit().putString(KEY_LIBRARY_VIEW_MODE, mode.name).apply()
    }

    fun browseViewMode(): MediaViewMode =
        preferences.getString(KEY_BROWSE_VIEW_MODE, null)
            ?.let { stored -> runCatching { MediaViewMode.valueOf(stored) }.getOrNull() }
            ?: MediaViewMode.COVER_GRID

    fun saveBrowseViewMode(mode: MediaViewMode) {
        preferences.edit().putString(KEY_BROWSE_VIEW_MODE, mode.name).apply()
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

    fun readerFitWidth(): Boolean =
        preferences.getBoolean(KEY_READER_FIT_WIDTH, false)

    fun saveReaderFitWidth(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_READER_FIT_WIDTH, enabled).apply()
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

    fun librarySyncedAtEpochMillis(): Long =
        preferences.getLong(KEY_LIBRARY_SYNCED_AT, 0L)

    fun saveLibrarySyncedAtEpochMillis(value: Long) {
        preferences.edit().putLong(KEY_LIBRARY_SYNCED_AT, value).apply()
    }

    private companion object {
        const val KEY_EXTENSION_REPOSITORY_URL = "extension.repository.url"
        const val KEY_THEME_MODE = "theme.mode"
        const val KEY_LIBRARY_VIEW_MODE = "library.view.mode"
        const val KEY_BROWSE_VIEW_MODE = "browse.view.mode"
        const val KEY_READER_MODE = "reader.mode"
        const val KEY_READER_PAGE_GAP_LEVEL = "reader.page.gap.level"
        const val KEY_READER_FIT_WIDTH = "reader.fit.width"
        const val KEY_ANILIST_TAGS = "anilist.tags"
        const val KEY_ANILIST_TAGS_CACHED_AT = "anilist.tags.cached.at"
        const val KEY_SOURCE_LANGUAGES = "source.languages"
        const val KEY_DISABLED_SOURCE_KEYS = "source.disabled.keys"
        const val KEY_VIEWER_NAME = "anilist.viewer.name"
        const val KEY_LIBRARY_SYNCED_AT = "anilist.library.synced.at"
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
    MIDNIGHT_RAMEN,
    STARRY_INK,
    PLUM_NIGHT,
}

enum class MediaViewMode {
    COVER_GRID,
    COVER_WITH_INFO,
    MASONRY,
    JUSTIFIED,
    LIST,
}

fun defaultSourceLanguages(): Set<String> =
    buildSet {
        add("en")
        add(UNIVERSAL_SOURCE_LANGUAGE)
        val locale = Locale.getDefault()
        val language = locale.language.lowercase(Locale.ROOT)
        val tag = locale.toLanguageTag().lowercase(Locale.ROOT)
        if (language.isNotBlank()) add(language)
        if (tag.isNotBlank()) add(tag)
    }

const val UNIVERSAL_SOURCE_LANGUAGE = "all"
