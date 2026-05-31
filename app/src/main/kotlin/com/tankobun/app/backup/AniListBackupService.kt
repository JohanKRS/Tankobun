package com.tankobun.app.backup

import android.net.Uri
import android.provider.DocumentsContract
import android.content.ContentResolver
import com.tankobun.app.AppContainer
import com.tankobun.app.logic.normalizedCustomLists
import com.tankobun.app.state.LibraryItem
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.database.toEntity
import com.tankobun.core.model.AnilistScoreFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

internal data class AniListBackupRestoreResult(
    val restored: Int,
    val skipped: Int,
    val customLists: List<String>,
)

internal class AniListBackupService(
    private val container: AppContainer,
) {
    suspend fun saveBackup(
        uri: Uri,
        items: List<LibraryItem>,
        viewerName: String?,
        scoreFormat: AnilistScoreFormat,
    ): Int = withContext(Dispatchers.IO) {
        val xml = buildMyAnimeListBackupXml(
            items = items,
            viewerName = viewerName,
            scoreFormat = scoreFormat,
        )
        val output = container.application.contentResolver.openOutputStream(uri)
            ?: error("Could not open backup file")
        OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
            writer.write(xml)
        }
        items.size
    }

    suspend fun writeScheduledBackup(
        folderUri: Uri,
        snapshot: TankobunUiState,
    ): Int = withContext(Dispatchers.IO) {
        val fileUri = createDocumentInTree(
            contentResolver = container.application.contentResolver,
            treeUri = folderUri,
            mimeType = "text/xml",
            displayName = suggestedScheduledAniListBackupFileName(snapshot.viewerName),
        )
        saveBackup(
            uri = fileUri,
            items = snapshot.libraryItems,
            viewerName = snapshot.viewerName,
            scoreFormat = snapshot.anilistScoreFormat,
        )
    }

    suspend fun restoreBackup(
        uri: Uri,
        accessToken: String,
        scoreFormat: AnilistScoreFormat,
        knownCustomLists: List<String>,
    ): AniListBackupRestoreResult {
        val entries = withContext(Dispatchers.IO) {
            val input = container.application.contentResolver.openInputStream(uri)
                ?: error("Could not open backup file")
            input.use { stream ->
                parseMyAnimeListBackupXml(stream, scoreFormat)
            }
        }
        check(entries.isNotEmpty()) { "No manga found in backup" }

        val customLists = ensureAniListCustomLists(
            accessToken = accessToken,
            knownCustomLists = knownCustomLists,
            requestedCustomLists = entries.flatMap { it.customLists },
        )
        var restored = 0
        var skipped = 0
        val now = System.currentTimeMillis()
        entries.forEach { entry ->
            val media = entry.mediaId
                ?.let { mediaId -> container.anilistRepository.mangaById(mediaId, accessToken = accessToken) }
                ?: entry.idMal?.let { idMal -> container.anilistRepository.mangaByMalId(idMal, accessToken = accessToken) }
            if (media == null) {
                skipped += 1
            } else {
                val savedEntry = container.anilistRepository.saveListEntry(
                    accessToken = accessToken,
                    mediaId = media.id,
                    status = entry.status,
                    progress = entry.progress,
                    score = entry.score,
                    notes = entry.notes,
                    private = entry.private,
                    customLists = entry.customLists,
                    scoreFormat = scoreFormat,
                )
                container.database.mediaDao().upsertMedia(media.toEntity(now))
                container.database.listEntryDao().upsertEntry(savedEntry.toEntity(now))
                restored += 1
                delay(350L)
            }
        }
        return AniListBackupRestoreResult(
            restored = restored,
            skipped = skipped,
            customLists = customLists,
        )
    }

    private suspend fun ensureAniListCustomLists(
        accessToken: String,
        knownCustomLists: List<String>,
        requestedCustomLists: List<String>,
    ): List<String> {
        val normalizedKnownCustomLists = knownCustomLists.normalizedCustomLists()
        val missingCustomLists = requestedCustomLists.normalizedCustomLists().filterNot { selectedList ->
            normalizedKnownCustomLists.any { knownList -> knownList.equals(selectedList, ignoreCase = true) }
        }
        return if (missingCustomLists.isEmpty()) {
            normalizedKnownCustomLists
        } else {
            container.anilistRepository.updateMangaCustomLists(
                accessToken = accessToken,
                customLists = (normalizedKnownCustomLists + missingCustomLists).normalizedCustomLists(),
            ).ifEmpty { (normalizedKnownCustomLists + missingCustomLists).normalizedCustomLists() }
        }
    }
}

internal fun createDocumentInTree(
    contentResolver: ContentResolver,
    treeUri: Uri,
    mimeType: String,
    displayName: String,
): Uri {
    val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        DocumentsContract.getTreeDocumentId(treeUri),
    )
    return DocumentsContract.createDocument(
        contentResolver,
        parentDocumentUri,
        mimeType,
        displayName,
    ) ?: error("Could not create backup file in selected folder")
}

private fun suggestedScheduledAniListBackupFileName(viewerName: String?): String {
    val userPart = viewerName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
        ?: "user"
    return "tankobun_anilist_backup_${userPart}_${System.currentTimeMillis()}.xml"
}
