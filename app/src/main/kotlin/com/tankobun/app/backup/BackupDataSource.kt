package com.tankobun.app.backup

import androidx.room.withTransaction
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
        val items = container.database.withTransaction {
            val media = container.database.mediaDao().libraryMedia().associateBy { it.id }
            container.database.listEntryDao().cachedEntries().mapNotNull { entry -> media[entry.mediaId]?.let { LibraryItem(it.toModel(), entry.toModel()) } }
        }
        val mediaIds = items.map { it.media.id }.toSet()
        val payload = buildTankobunLibraryBackupJson(
            items = items,
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
        items.size
    }

    suspend fun restoreLocalLibraryBackup(
        uri: Uri,
        scoreFormat: AnilistScoreFormat,
        knownCustomLists: List<String>,
    ): BackupRestoreResult = withContext(Dispatchers.IO) {
        val text = container.application.contentResolver
            .readImportBytes(uri, MAX_BACKUP_BYTES).toString(Charsets.UTF_8)
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
        container.database.withTransaction {
            backup.items.forEach { item ->
                val media = container.catalogIdentity.resolve(item.media)
                val existing = container.database.listEntryDao().cachedEntry(media.id)
                // List-entry IDs belong to an account, and cannot safely travel in a backup.
                val entry = item.entry.copy(mediaId = media.id, id = existing?.id ?: -kotlin.math.abs(media.id))
                container.database.mediaDao().upsertMedia(media.toEntity(now))
                container.database.listEntryDao().upsertEntry(entry.toEntity(now))
                item.sourceBinding?.copy(mediaId = media.id)?.toEntity()?.let { container.database.sourceBindingDao().upsertBinding(it) }
                item.progress.forEach { progress -> container.database.progressDao().upsertProgress(progress.copy(mediaId = media.id).toEntity()) }
                // Restored progress must remain pending locally until the connected tracker
                // accepts it; an older remote snapshot must not undo the restore.
                container.mangaBakaTracking.changed(entry, scoreFormat = backup.scoreFormat)
                if (media.anilistId != null) {
                    val token = container.tokenStore.accessToken()
                    val mutation = com.tankobun.core.sync.SyncMutationFactory().saveMediaListEntry(
                        mediaId = media.id, status = entry.status, progress = entry.progress, score = entry.score,
                        notes = entry.notes, private = entry.private, customLists = entry.customLists,
                        hiddenFromStatusLists = entry.hiddenFromStatusLists, nowMillis = now,
                        sessionKey = com.tankobun.core.sync.syncSessionKey(token),
                    )
                    container.database.syncMutationDao().upsertMutation(mutation.toEntity())
                }
            }
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
                    id = -kotlin.math.abs(media.id),
                    mediaId = media.id,
                    status = entry.status,
                    progress = entry.progress ?: 0,
                    score = entry.score,
                    notes = entry.notes,
                    private = entry.private ?: false,
                    customLists = entry.customLists,
                    updatedAtEpochSeconds = now / 1000L,
                )
                container.database.withTransaction {
                    container.database.mediaDao().upsertMedia(media.toEntity(now))
                    container.database.listEntryDao().upsertEntry(localEntry.toEntity(now))
                }
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
