package com.tankobun.app.backup

import android.net.Uri
import com.tankobun.app.AppContainer
import com.tankobun.app.AppLanguage
import com.tankobun.app.BackupContent
import com.tankobun.app.BackupSchedule
import com.tankobun.app.DockAlignment
import com.tankobun.app.DockIndicatorAnimation
import com.tankobun.app.MediaViewMode
import com.tankobun.app.ReaderScreenOrientation
import com.tankobun.app.TankobunThemeMode
import com.tankobun.app.TankobunArtDirection
import com.tankobun.app.TankobunPaletteId
import com.tankobun.app.TankobunThemePreference
import com.tankobun.app.toLegacyThemeMode
import com.tankobun.app.defaultSourceLanguages
import com.tankobun.app.logic.sourceSettingsKey
import com.tankobun.app.logic.visibleSources
import com.tankobun.app.state.BackupMissingSource
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.state.LocalReadingActivity
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.SourceDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Base64

internal data class AppSettingsBackupRestoreResult(
    val missingSources: List<BackupMissingSource>,
)

internal class AppSettingsBackupDataSource(
    private val container: AppContainer,
) {
    suspend fun saveBackup(uri: Uri, snapshot: TankobunUiState): Int = withContext(Dispatchers.IO) {
        val payload = buildJson(snapshot).toString(JSON_INDENT)
        container.application.contentResolver.openOutputStream(uri, "wt").use { output ->
            checkNotNull(output) { "Could not open backup destination" }
            output.write(payload.toByteArray(Charsets.UTF_8))
        }
        snapshot.allInstalledSources.map { it.packageName }.distinct().size
    }

    suspend fun writeScheduledBackup(folderUri: Uri, snapshot: TankobunUiState): Int = withContext(Dispatchers.IO) {
        val fileUri = createDocumentInTree(
            contentResolver = container.application.contentResolver,
            treeUri = folderUri,
            mimeType = "application/json",
            displayName = suggestedScheduledAppSettingsBackupFileName(),
        )
        saveBackup(uri = fileUri, snapshot = snapshot)
    }

    suspend fun writeScheduledBackupFromCurrentSettings(folderUri: Uri): Int = withContext(Dispatchers.IO) {
        val fileUri = createDocumentInTree(
            contentResolver = container.application.contentResolver,
            treeUri = folderUri,
            mimeType = "application/json",
            displayName = suggestedScheduledAppSettingsBackupFileName(),
        )
        val snapshot = currentSettingsSnapshot()
        saveBackup(uri = fileUri, snapshot = snapshot)
    }

    suspend fun restoreBackup(uri: Uri): AppSettingsBackupRestoreResult = withContext(Dispatchers.IO) {
        val text = container.application.contentResolver
            .readImportBytes(uri, MAX_BACKUP_BYTES).toString(Charsets.UTF_8)
        val root = JSONObject(text)
        check(root.optString("type") == BACKUP_TYPE) { "Unsupported Tankobun settings backup" }
        val settings = root.optJSONObject("settings") ?: JSONObject()
        restoreSettings(settings)

        val sources = root.optJSONObject("sources") ?: JSONObject()
        restoreSourcePreferences(sources)
        AppSettingsBackupRestoreResult(
            missingSources = missingSources(sources),
        )
    }

    private fun buildJson(snapshot: TankobunUiState): JSONObject =
        JSONObject()
            .put("type", BACKUP_TYPE)
            .put("version", BACKUP_VERSION)
            .put("createdAtEpochMillis", System.currentTimeMillis())
            .put("settings", settingsJson(snapshot))
            .put("sources", sourcesJson(snapshot))

    private fun settingsJson(snapshot: TankobunUiState): JSONObject =
        JSONObject()
            .put("themeMode", snapshot.themePreference.toLegacyThemeMode().name)
            .put("themeAutomatic", snapshot.themePreference.automatic)
            .put("themeArtDirection", snapshot.themePreference.direction.name)
            .put("themePalette", snapshot.themePreference.palette.name)
            .put("appLanguage", snapshot.appLanguage.storageValue)
            .put("ignoreDisplayCutout", snapshot.ignoreDisplayCutout)
            .put("showAppStatusBar", snapshot.showAppStatusBar)
            .put("dockAlignment", snapshot.dockAlignment.name)
            .put("dockIndicatorAnimation", snapshot.dockIndicatorAnimation.name)
            .put("libraryMode", snapshot.libraryMode.name)
            .put("libraryViewMode", snapshot.libraryViewMode.name)
            .put("libraryCoverColumns", snapshot.libraryCoverColumns)
            .put("libraryShowWholeCovers", snapshot.libraryShowWholeCovers)
            .put("browseViewMode", snapshot.browseViewMode.name)
            .put("browseCoverColumns", snapshot.browseCoverColumns)
            .put("browseShowWholeCovers", snapshot.browseShowWholeCovers)
            .put("readerMode", snapshot.readerMode.name)
            .put("readerPageGapLevel", snapshot.readerPageGapLevel)
            .put("showWebtoonChapterDividers", snapshot.showWebtoonChapterDividers)
            .put("readerScreenOrientation", snapshot.readerScreenOrientation.name)
            .put("chapterListStartsAtFirst", snapshot.chapterListStartsAtFirst)
            .put("keepNextTenDownloads", snapshot.keepNextTenDownloads)
            .put("newChapterChecksEnabled", snapshot.newChapterChecksEnabled)
            .put("showNsfwContent", snapshot.showNsfwContent)
            .put("anilistScoreFormat", snapshot.anilistScoreFormat.name)
            .put("anilistTitleLanguage", snapshot.anilistTitleLanguage.name)
            .put("anilistAutoSaveTrackingChanges", snapshot.anilistAutoSaveTrackingChanges)
            .put("anilistAutoSyncReaderProgress", snapshot.anilistAutoSyncReaderProgress)
            .put("anilistSyncManualReadProgress", snapshot.anilistSyncManualReadProgress)
            .put("autoUpdateStatusFromReading", snapshot.autoUpdateStatusFromReading)
            .put("anilistCustomLists", snapshot.anilistCustomLists.toJsonArray())
            .put("backupSchedule", snapshot.backupSchedule.name)
            .put("backupContent", snapshot.backupContent.name)
            .put("scheduledBackupRetentionCount", snapshot.scheduledBackupRetentionCount)
            .put("localReadingActivity", localReadingActivityJson(snapshot.localReadingActivity))
            .put("customProfileAvatar", profileImageBase64(snapshot.customProfileAvatarUri) ?: JSONObject.NULL)
            .put("customProfileBanner", profileImageBase64(snapshot.customProfileBannerUri) ?: JSONObject.NULL)

    private fun sourcesJson(snapshot: TankobunUiState): JSONObject =
        JSONObject()
            .put("extensionRepositoryUrl", snapshot.extensionRepositoryUrl)
            .put("sourceLanguages", snapshot.sourceLanguages.sorted().toJsonArray())
            .put("disabledSourceKeys", snapshot.disabledSourceKeys.sorted().toJsonArray())
            .put("installedExtensions", installedExtensionsJson(snapshot))

    private fun installedExtensionsJson(snapshot: TankobunUiState): JSONArray {
        val repositoryByPackage = snapshot.availableExtensions.associateBy { it.packageName }
        return snapshot.allInstalledSources
            .groupBy { it.packageName }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .map { (packageName, sources) ->
                val repositoryEntry = repositoryByPackage[packageName]
                val firstSource = sources.first()
                JSONObject()
                    .put("packageName", packageName)
                    .put("name", repositoryEntry?.name ?: firstSource.name)
                    .put("lang", repositoryEntry?.lang ?: firstSource.lang)
                    .put("versionCode", repositoryEntry?.versionCode ?: firstSource.versionCode)
                    .put("versionName", repositoryEntry?.versionName ?: firstSource.versionName)
                    .put("isNsfw", repositoryEntry?.isNsfw ?: sources.any { it.isNsfw })
                    .put("sourceKeys", sources.map { it.sourceSettingsKey() }.sorted().toJsonArray())
                    .put("sources", sourceListJson(repositoryEntry, sources))
            }
            .toJsonArray()
    }

    private fun sourceListJson(
        repositoryEntry: ExtensionIndexEntry?,
        sources: List<SourceDescriptor>,
    ): JSONArray {
        val sourceObjects = repositoryEntry?.sources
            ?.map { source ->
                JSONObject()
                    .put("id", source.id)
                    .put("name", source.name)
                    .put("lang", source.lang)
            }
            ?: sources.map { source ->
                JSONObject()
                    .put("id", source.id)
                    .put("name", source.name)
                    .put("lang", source.lang)
            }
        return sourceObjects.toJsonArray()
    }

    private fun restoreSettings(settings: JSONObject) {
        val store = container.settingsStore
        val direction = settings.enumOrNull<TankobunArtDirection>("themeArtDirection")
        val palette = settings.enumOrNull<TankobunPaletteId>("themePalette")
        if (direction != null && palette != null) {
            store.saveThemePreference(
                TankobunThemePreference(
                    automatic = settings.optBooleanOrNull("themeAutomatic") ?: false,
                    direction = direction,
                    palette = palette,
                ),
            )
        } else {
            settings.enumOrNull<TankobunThemeMode>("themeMode")?.let(store::saveThemeMode)
        }
        settings.optStringOrNull("appLanguage")
            ?.let(AppLanguage::fromStorageValue)
            ?.let(store::saveAppLanguage)
        settings.optBooleanOrNull("ignoreDisplayCutout")?.let(store::saveIgnoreDisplayCutout)
        settings.optBooleanOrNull("showAppStatusBar")?.let(store::saveShowAppStatusBar)
        settings.enumOrNull<DockAlignment>("dockAlignment")?.let(store::saveDockAlignment)
        settings.enumOrNull<DockIndicatorAnimation>("dockIndicatorAnimation")?.let(store::saveDockIndicatorAnimation)
        settings.enumOrNull<com.tankobun.app.LibraryMode>("libraryMode")?.let(store::saveLibraryMode)
        settings.enumOrNull<MediaViewMode>("libraryViewMode")?.let(store::saveLibraryViewMode)
        settings.optIntOrNull("libraryCoverColumns")?.let(store::saveLibraryCoverColumns)
        settings.optBooleanOrNull("libraryShowWholeCovers")?.let(store::saveLibraryShowWholeCovers)
        settings.enumOrNull<MediaViewMode>("browseViewMode")?.let(store::saveBrowseViewMode)
        settings.optIntOrNull("browseCoverColumns")?.let(store::saveBrowseCoverColumns)
        settings.optBooleanOrNull("browseShowWholeCovers")?.let(store::saveBrowseShowWholeCovers)
        settings.enumOrNull<ReaderMode>("readerMode")?.let(store::saveReaderMode)
        settings.optIntOrNull("readerPageGapLevel")?.let(store::saveReaderPageGapLevel)
        settings.optBooleanOrNull("showWebtoonChapterDividers")?.let(store::saveShowWebtoonChapterDividers)
        settings.enumOrNull<ReaderScreenOrientation>("readerScreenOrientation")?.let(store::saveReaderScreenOrientation)
        settings.optBooleanOrNull("chapterListStartsAtFirst")?.let(store::saveChapterListStartsAtFirst)
        settings.optBooleanOrNull("keepNextTenDownloads")?.let(store::saveKeepNextTenDownloads)
        settings.optBooleanOrNull("newChapterChecksEnabled")?.let(store::saveNewChapterChecksEnabled)
        settings.optBooleanOrNull("showNsfwContent")?.let(store::saveShowNsfwContent)
        settings.enumOrNull<AnilistScoreFormat>("anilistScoreFormat")?.let(store::saveAnilistScoreFormat)
        settings.enumOrNull<AnilistTitleLanguage>("anilistTitleLanguage")?.let(store::saveAnilistTitleLanguage)
        settings.optBooleanOrNull("anilistAutoSaveTrackingChanges")?.let(store::saveAnilistAutoSaveTrackingChanges)
        settings.optBooleanOrNull("anilistAutoSyncReaderProgress")?.let(store::saveAnilistAutoSyncReaderProgress)
        settings.optBooleanOrNull("anilistSyncManualReadProgress")?.let(store::saveAnilistSyncManualReadProgress)
        settings.optBooleanOrNull("autoUpdateStatusFromReading")?.let(store::saveAutoUpdateStatusFromReading)
        settings.optJSONArray("anilistCustomLists")?.stringValues()?.let(store::saveAnilistCustomLists)
        settings.enumOrNull<BackupSchedule>("backupSchedule")?.let(store::saveBackupSchedule)
        settings.enumOrNull<BackupContent>("backupContent")?.let(store::saveBackupContent)
        settings.optIntOrNull("scheduledBackupRetentionCount")?.let(store::saveScheduledBackupRetentionCount)
        settings.optJSONObject("localReadingActivity")?.toLocalReadingActivity()?.let(store::saveLocalReadingActivity)
        if (settings.has("customProfileAvatar")) {
            store.saveCustomProfileAvatarUri(restoreProfileImage(settings.optStringOrNull("customProfileAvatar"), "avatar.img"))
        }
        if (settings.has("customProfileBanner")) {
            store.saveCustomProfileBannerUri(restoreProfileImage(settings.optStringOrNull("customProfileBanner"), "banner.img"))
        }
    }

    private fun currentSettingsSnapshot(): TankobunUiState {
        val store = container.settingsStore
        val allSources = container.extensionScanner.installedExtensions().flatMap { descriptor ->
            runCatching {
                container.sourceHost.loadSources(descriptor.packageName).map { source ->
                    descriptor.copy(
                        id = source.id,
                        name = source.name,
                        lang = source.lang,
                    )
                }
            }.getOrDefault(emptyList()).ifEmpty { listOf(descriptor) }
        }.visibleSources()
        return TankobunUiState(
            loggedIn = container.tokenStore.accessToken() != null,
            libraryMode = store.libraryMode(),
            themePreference = store.themePreference(),
            appLanguage = store.appLanguage(),
            ignoreDisplayCutout = store.ignoreDisplayCutout(),
            showAppStatusBar = store.showAppStatusBar(),
            dockAlignment = store.dockAlignment(),
            dockIndicatorAnimation = store.dockIndicatorAnimation(),
            libraryViewMode = store.libraryViewMode(),
            libraryCoverColumns = store.libraryCoverColumns(),
            libraryShowWholeCovers = store.libraryShowWholeCovers(),
            browseViewMode = store.browseViewMode(),
            browseCoverColumns = store.browseCoverColumns(),
            browseShowWholeCovers = store.browseShowWholeCovers(),
            readerMode = store.readerMode(),
            readerPageGapLevel = store.readerPageGapLevel(),
            showWebtoonChapterDividers = store.showWebtoonChapterDividers(),
            readerScreenOrientation = store.readerScreenOrientation(),
            chapterListStartsAtFirst = store.chapterListStartsAtFirst(),
            keepNextTenDownloads = store.keepNextTenDownloads(),
            newChapterChecksEnabled = store.newChapterChecksEnabled(),
            lastNewChapterCheckAtEpochMillis = store.lastNewChapterCheckAtEpochMillis(),
            showNsfwContent = store.showNsfwContent(),
            anilistScoreFormat = store.anilistScoreFormat(),
            anilistTitleLanguage = store.anilistTitleLanguage(),
            anilistAutoSaveTrackingChanges = store.anilistAutoSaveTrackingChanges(),
            anilistAutoSyncReaderProgress = store.anilistAutoSyncReaderProgress(),
            anilistSyncManualReadProgress = store.anilistSyncManualReadProgress(),
            autoUpdateStatusFromReading = store.autoUpdateStatusFromReading(),
            anilistCustomLists = store.anilistCustomLists(),
            backupFolderUri = store.backupFolderUri(),
            backupSchedule = store.backupSchedule(),
            backupContent = store.backupContent(),
            scheduledBackupRetentionCount = store.scheduledBackupRetentionCount(),
            customProfileAvatarUri = store.customProfileAvatarUri(),
            customProfileBannerUri = store.customProfileBannerUri(),
            localReadingActivity = store.localReadingActivity(),
            extensionRepositoryUrl = store.extensionRepositoryUrl(),
            sourceLanguages = store.sourceLanguages(),
            disabledSourceKeys = store.disabledSourceKeys(),
            allInstalledSources = allSources,
            installedSources = allSources,
        )
    }

    private fun restoreSourcePreferences(sources: JSONObject) {
        val store = container.settingsStore
        sources.optStringOrNull("extensionRepositoryUrl")?.let(store::saveExtensionRepositoryUrl)
        val languages = sources.optJSONArray("sourceLanguages")
            ?.stringValues()
            ?.map { it.trim().lowercase().replace('_', '-') }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: defaultSourceLanguages()
        store.saveSourceLanguages(languages)
        sources.optJSONArray("disabledSourceKeys")
            ?.stringValues()
            ?.toSet()
            ?.let(store::saveDisabledSourceKeys)
    }

    private fun missingSources(sources: JSONObject): List<BackupMissingSource> {
        val installedPackages = container.extensionScanner.installedExtensions()
            .map { it.packageName }
            .toSet()
        return sources.optJSONArray("installedExtensions")
            .orEmptyObjects()
            .mapNotNull { extension ->
                val packageName = extension.optString("packageName").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                if (packageName in installedPackages) return@mapNotNull null
                BackupMissingSource(
                    packageName = packageName,
                    name = extension.optString("name").takeIf { it.isNotBlank() } ?: packageName,
                    lang = extension.optString("lang").takeIf { it.isNotBlank() }.orEmpty(),
                    versionName = extension.optStringOrNull("versionName"),
                    sourceNames = extension.optJSONArray("sources")
                        .orEmptyObjects()
                        .mapNotNull { it.optString("name").takeIf(String::isNotBlank) }
                        .distinct(),
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    private companion object {
        const val BACKUP_TYPE = "tankobun.app-settings"
        const val BACKUP_VERSION = 3
        const val JSON_INDENT = 2
    }

    private fun localReadingActivityJson(activity: LocalReadingActivity): JSONObject =
        JSONObject()
            .put("generatedAtEpochMillis", activity.generatedAtEpochMillis)
            .put("chaptersTracked", activity.chaptersTracked)
            .put("chaptersToday", activity.chaptersToday)
            .put("chaptersLast7Days", activity.chaptersLast7Days)
            .put("chaptersLast30Days", activity.chaptersLast30Days)
            .put("averagePerActiveDay30", activity.averagePerActiveDay30)
            .put("currentStreakDays", activity.currentStreakDays)
            .put("longestStreakDays", activity.longestStreakDays)
            .put("totalReadingDays", activity.totalReadingDays)
            .put("last14Days", activity.last14Days.toJsonArray())

    private fun JSONObject.toLocalReadingActivity(): LocalReadingActivity =
        LocalReadingActivity(
            generatedAtEpochMillis = optLong("generatedAtEpochMillis", 0L),
            chaptersTracked = optInt("chaptersTracked", 0),
            chaptersToday = optInt("chaptersToday", 0),
            chaptersLast7Days = optInt("chaptersLast7Days", 0),
            chaptersLast30Days = optInt("chaptersLast30Days", 0),
            averagePerActiveDay30 = optDouble("averagePerActiveDay30", 0.0),
            currentStreakDays = optInt("currentStreakDays", 0),
            longestStreakDays = optInt("longestStreakDays", 0),
            totalReadingDays = optInt("totalReadingDays", 0),
            last14Days = optJSONArray("last14Days")
                ?.let { array -> (0 until array.length()).map { index -> array.optInt(index, 0) } }
                ?.takeIf { it.size == 14 }
                ?: List(14) { 0 },
        )

    private fun profileImageBase64(uriString: String?): String? {
        val uri = uriString?.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return null
        val bytes = when (uri.scheme) {
            "file" -> uri.path?.let(::File)?.takeIf(File::isFile)?.readBytes()
            else -> container.application.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: return null
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun restoreProfileImage(encoded: String?, fileName: String): String? {
        val target = File(container.application.filesDir, "profile/$fileName")
        if (encoded.isNullOrBlank()) {
            target.delete()
            return null
        }
        target.parentFile?.mkdirs()
        target.writeBytes(Base64.getDecoder().decode(encoded))
        return target.toURI().toString()
    }
}

private fun suggestedScheduledAppSettingsBackupFileName(): String =
    "tankobun_settings_${System.currentTimeMillis()}.json"

private fun Iterable<Any>.toJsonArray(): JSONArray =
    JSONArray().also { array -> forEach { array.put(it) } }

private fun JSONObject.optStringOrNull(name: String): String? =
    if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null

private fun JSONObject.optBooleanOrNull(name: String): Boolean? =
    if (has(name) && !isNull(name)) optBoolean(name) else null

private fun JSONObject.optIntOrNull(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null

private inline fun <reified T : Enum<T>> JSONObject.enumOrNull(name: String): T? =
    optStringOrNull(name)?.let { value -> runCatching { enumValueOf<T>(value) }.getOrNull() }

private fun JSONArray?.stringValues(): List<String> =
    orEmptyObjectsAndValues().mapNotNull { value -> value as? String }

private fun JSONArray?.orEmptyObjects(): List<JSONObject> =
    orEmptyObjectsAndValues().mapNotNull { value -> value as? JSONObject }

private fun JSONArray?.orEmptyObjectsAndValues(): List<Any> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> opt(index) }
}
