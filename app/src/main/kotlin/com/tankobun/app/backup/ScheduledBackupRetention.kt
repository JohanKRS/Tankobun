package com.tankobun.app.backup

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.tankobun.app.BackupContent
import com.tankobun.app.LibraryMode
import com.tankobun.app.SCHEDULED_BACKUP_RETENTION_UNLIMITED
import com.tankobun.app.includesLibrary
import com.tankobun.app.includesSettings

internal enum class ScheduledBackupFileKind {
    ANILIST_LIBRARY,
    LOCAL_LIBRARY,
    APP_SETTINGS,
}

internal fun BackupContent.scheduledBackupFileKinds(libraryMode: LibraryMode): Set<ScheduledBackupFileKind> =
    buildSet {
        if (includesLibrary()) {
            add(
                if (libraryMode == LibraryMode.LOCAL) {
                    ScheduledBackupFileKind.LOCAL_LIBRARY
                } else {
                    ScheduledBackupFileKind.ANILIST_LIBRARY
                },
            )
        }
        if (includesSettings()) {
            add(ScheduledBackupFileKind.APP_SETTINGS)
        }
    }

internal fun pruneScheduledBackups(
    contentResolver: ContentResolver,
    treeUri: Uri,
    retentionCount: Int,
    kinds: Set<ScheduledBackupFileKind>,
) {
    if (retentionCount == SCHEDULED_BACKUP_RETENTION_UNLIMITED || retentionCount < 1 || kinds.isEmpty()) return
    val backups = scheduledBackupDocuments(contentResolver, treeUri)
        .filter { it.kind in kinds }
        .groupBy { it.kind }
    backups.values.forEach { documents ->
        documents
            .sortedWith(
                compareByDescending<ScheduledBackupDocument> { it.lastModifiedEpochMillis }
                    .thenByDescending { it.timestampFromName },
            )
            .drop(retentionCount)
            .forEach { document ->
                runCatching {
                    DocumentsContract.deleteDocument(contentResolver, document.uri)
                }
            }
    }
}

internal fun scheduledBackupKindForFileName(fileName: String): ScheduledBackupFileKind? =
    when {
        ANILIST_BACKUP_REGEX.matches(fileName) -> ScheduledBackupFileKind.ANILIST_LIBRARY
        LOCAL_LIBRARY_BACKUP_REGEX.matches(fileName) -> ScheduledBackupFileKind.LOCAL_LIBRARY
        APP_SETTINGS_BACKUP_REGEX.matches(fileName) -> ScheduledBackupFileKind.APP_SETTINGS
        else -> null
    }

internal fun scheduledBackupTimestampFromFileName(fileName: String): Long =
    listOf(ANILIST_BACKUP_REGEX, LOCAL_LIBRARY_BACKUP_REGEX, APP_SETTINGS_BACKUP_REGEX)
        .firstNotNullOfOrNull { regex -> regex.matchEntire(fileName)?.groupValues?.getOrNull(1)?.toLongOrNull() }
        ?: 0L

private fun scheduledBackupDocuments(
    contentResolver: ContentResolver,
    treeUri: Uri,
): List<ScheduledBackupDocument> {
    val treeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return emptyList()
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId)
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )
    return contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
        val documentIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val displayNameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val lastModifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        buildList {
            while (cursor.moveToNext()) {
                val documentId = cursor.getStringOrNull(documentIdIndex) ?: continue
                val displayName = cursor.getStringOrNull(displayNameIndex) ?: continue
                val kind = scheduledBackupKindForFileName(displayName) ?: continue
                add(
                    ScheduledBackupDocument(
                        uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                        kind = kind,
                        lastModifiedEpochMillis = cursor.getLongOrZero(lastModifiedIndex),
                        timestampFromName = scheduledBackupTimestampFromFileName(displayName),
                    ),
                )
            }
        }
    }.orEmpty()
}

private data class ScheduledBackupDocument(
    val uri: Uri,
    val kind: ScheduledBackupFileKind,
    val lastModifiedEpochMillis: Long,
    val timestampFromName: Long,
)

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (index >= 0 && !isNull(index)) getString(index) else null

private fun android.database.Cursor.getLongOrZero(index: Int): Long =
    if (index >= 0 && !isNull(index)) getLong(index) else 0L

private val ANILIST_BACKUP_REGEX = Regex("""tankobun_anilist_backup_.+_(\d+)\.xml""")
private val LOCAL_LIBRARY_BACKUP_REGEX = Regex("""tankobun_library_(\d+)\.json""")
private val APP_SETTINGS_BACKUP_REGEX = Regex("""tankobun_settings_(\d+)\.json""")
