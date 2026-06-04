package com.tankobun.app.backup

import android.content.Intent
import android.net.Uri
import com.tankobun.app.AppContainer
import com.tankobun.app.state.LibraryItem
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.AnilistScoreFormat

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

    suspend fun writeScheduledBackup(folderUri: Uri, snapshot: TankobunUiState): Int =
        backupService.writeScheduledBackup(folderUri = folderUri, snapshot = snapshot)

    fun persistBackupFolderPermission(uri: Uri): Boolean {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        return runCatching {
            container.application.contentResolver.takePersistableUriPermission(uri, flags)
        }.isSuccess
    }
}
