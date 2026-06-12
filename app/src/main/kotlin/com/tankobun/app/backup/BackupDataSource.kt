package com.tankobun.app.backup

import android.content.Intent
import android.net.Uri
import com.tankobun.app.AppContainer
import com.tankobun.app.state.LibraryItem
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.database.toEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.model.AnilistScoreFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

internal data class BackupRestoreResult(
    val restored: Int,
    val skipped: Int,
    val customLists: List<String>,
)

internal class BackupDataSource(
    private val container: AppContainer,
) {
    private val backupService = AniListBackupService(container)

    suspend fun saveBackup(
        uri: Uri,
        items: List<LibraryItem>,
        viewerName: String?,
        scoreFormat: AnilistScoreFormat,
    ): Int =
        backupService.saveBackup(
            uri = uri,
            items = items,
            viewerName = viewerName,
            scoreFormat = scoreFormat,
        )

    suspend fun restoreBackup(
        uri: Uri,
        accessToken: String,
        scoreFormat: AnilistScoreFormat,
        knownCustomLists: List<String>,
    ): BackupRestoreResult {
        val result = backupService.restoreBackup(
            uri = uri,
            accessToken = accessToken,
            scoreFormat = scoreFormat,
            knownCustomLists = knownCustomLists,
        )
        return BackupRestoreResult(
            restored = result.restored,
            skipped = result.skipped,
            customLists = result.customLists,
        )
    }

    suspend fun saveLocalLibraryBackup(uri: Uri, snapshot: TankobunUiState): Int = withContext(Dispatchers.IO) {
        val mediaIds = snapshot.libraryItems.map { it.media.id }.toSet()
        val payload = buildTankobunLibraryBackupJson(
            items = snapshot.libraryItems,
            scoreFormat = snapshot.anilistScoreFormat,
            titleLanguage = snapshot.anilistTitleLanguage,
            customLists = snapshot.anilistCustomLists,
            sourceBindings = container.database.sourceBindingDao()
                .cachedBindings()
                .map { it.toModel() }
                .filter { it.mediaId in mediaIds },
            progress = container.database.progressDao()
                .allProgress()
                .map { it.toModel() }
                .filter { it.mediaId in mediaIds },
        )
        container.application.contentResolver.openOutputStream(uri, "wt").use { output ->
            checkNotNull(output) { "Could not open backup destination" }
            output.write(payload.toByteArray(Charsets.UTF_8))
        }
        snapshot.libraryItems.size
    }

    suspend fun restoreLocalLibraryBackup(
        uri: Uri,
        scoreFormat: AnilistScoreFormat,
        knownCustomLists: List<String>,
    ): BackupRestoreResult = withContext(Dispatchers.IO) {
        val text = container.application.contentResolver.openInputStream(uri).use { input ->
            checkNotNull(input) { "Could not open backup file" }
            input.reader(Charsets.UTF_8).readText()
        }
        if (!isTankobunLibraryBackupJson(text)) {
            return@withContext restoreMyAnimeListXmlToLocalLibrary(
                text = text,
                scoreFormat = scoreFormat,
                knownCustomLists = knownCustomLists,
            )
        }
        restoreTankobunLibraryBackup(parseTankobunLibraryBackupJson(text))
    }

    private suspend fun restoreTankobunLibraryBackup(backup: TankobunLibraryBackup): BackupRestoreResult {
        val now = System.currentTimeMillis()
        val media = backup.items.map { it.media.toEntity(now) }
        val entries = backup.items.map { item ->
            item.entry.copy(id = item.entry.id.takeIf { it != 0 } ?: -item.media.id).toEntity(now)
        }
        container.database.mediaDao().upsertMedia(media)
        container.database.listEntryDao().upsertEntries(entries)
        backup.items.mapNotNull { it.sourceBinding?.toEntity() }.forEach { binding ->
            container.database.sourceBindingDao().upsertBinding(binding)
        }
        backup.items.flatMap { item -> item.progress.map { it.toEntity() } }.forEach { progress ->
            container.database.progressDao().upsertProgress(progress)
        }
        container.settingsStore.saveAnilistScoreFormat(backup.scoreFormat)
        container.settingsStore.saveAnilistTitleLanguage(backup.titleLanguage)
        container.settingsStore.saveAnilistCustomLists(backup.customLists)
        container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
        return BackupRestoreResult(
            restored = backup.items.size,
            skipped = 0,
            customLists = backup.customLists,
        )
    }

    private suspend fun restoreMyAnimeListXmlToLocalLibrary(
        text: String,
        scoreFormat: AnilistScoreFormat,
        knownCustomLists: List<String>,
    ): BackupRestoreResult {
        val entries = parseMyAnimeListBackupXml(
            input = ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)),
            scoreFormat = scoreFormat,
        )
        check(entries.isNotEmpty()) { "No manga found in backup" }
        val now = System.currentTimeMillis()
        var restored = 0
        var skipped = 0
        val customLists = (knownCustomLists + entries.flatMap { it.customLists })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        entries.forEach { entry ->
            val media = entry.mediaId
                ?.let { mediaId -> container.anilistRepository.mangaById(mediaId) }
                ?: entry.idMal?.let { idMal -> container.anilistRepository.mangaByMalId(idMal) }
            if (media == null) {
                skipped += 1
            } else {
                val localEntry = com.tankobun.core.model.AnilistListEntry(
                    id = -media.id,
                    mediaId = media.id,
                    status = entry.status,
                    progress = entry.progress ?: 0,
                    score = entry.score,
                    notes = entry.notes,
                    private = entry.private ?: false,
                    customLists = entry.customLists,
                    updatedAtEpochSeconds = now / 1000L,
                )
                container.database.mediaDao().upsertMedia(media.toEntity(now))
                container.database.listEntryDao().upsertEntry(localEntry.toEntity(now))
                restored += 1
            }
        }
        container.settingsStore.saveAnilistCustomLists(customLists)
        container.settingsStore.saveLibrarySyncedAtEpochMillis(now)
        return BackupRestoreResult(
            restored = restored,
            skipped = skipped,
            customLists = customLists,
        )
    }

    suspend fun writeScheduledBackup(folderUri: Uri, snapshot: TankobunUiState): Int =
        backupService.writeScheduledBackup(folderUri = folderUri, snapshot = snapshot)

    suspend fun writeScheduledLocalLibraryBackup(folderUri: Uri, snapshot: TankobunUiState): Int =
        withContext(Dispatchers.IO) {
            val fileUri = createDocumentInTree(
                contentResolver = container.application.contentResolver,
                treeUri = folderUri,
                mimeType = "application/json",
                displayName = suggestedScheduledTankobunLibraryBackupFileName(),
            )
            saveLocalLibraryBackup(uri = fileUri, snapshot = snapshot)
        }

    fun persistBackupFolderPermission(uri: Uri): Boolean {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        return runCatching {
            container.application.contentResolver.takePersistableUriPermission(uri, flags)
        }.isSuccess
    }
}

private fun suggestedScheduledTankobunLibraryBackupFileName(): String =
    "tankobun_library_${System.currentTimeMillis()}.json"
