package com.tankobun.app.sharing

import android.net.Uri
import androidx.core.content.FileProvider
import com.tankobun.app.AppContainer
import com.tankobun.app.backup.MAX_RECOMMENDATION_BYTES
import com.tankobun.app.backup.readImportBytes
import com.tankobun.app.state.LibraryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class RecommendationShareDataSource(
    private val container: AppContainer,
) {
    suspend fun createShareFile(
        selectedItems: List<LibraryItem>,
        suggestedListName: String,
        messagesByMediaId: Map<Int, String> = emptyMap(),
    ): Uri = withContext(Dispatchers.IO) {
        check(selectedItems.isNotEmpty()) { "No recommendations selected" }
        val directory = File(container.application.cacheDir, "recommendations").also { it.mkdirs() }
        directory.listFiles()?.forEach { file -> file.delete() }
        val file = File(directory, suggestedRecommendationFileName(suggestedListName))
        val payload = buildRecommendationShareJson(
            suggestedListName = suggestedListName,
            items = selectedItems.map { item ->
                RecommendationShareItem(
                    media = item.media,
                    message = messagesByMediaId[item.media.id]?.trim()?.take(RECOMMENDATION_MESSAGE_MAX_LENGTH),
                )
            },
        )
        file.writeText(payload, Charsets.UTF_8)
        FileProvider.getUriForFile(
            container.application,
            "${container.application.packageName}.fileprovider",
            file,
        )
    }

    suspend fun readShareFile(uri: Uri): RecommendationSharePayload = withContext(Dispatchers.IO) {
        val text = container.application.contentResolver
            .readImportBytes(uri, MAX_RECOMMENDATION_BYTES).toString(Charsets.UTF_8)
        parseRecommendationShareJson(text)
    }
}

private fun suggestedRecommendationFileName(suggestedListName: String): String {
    val friendlyName = suggestedListName
        .trim()
        .ifBlank { DEFAULT_RECOMMENDATION_FILE_NAME }
        .replace(Regex("""[\\/:*?"<>|]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .replace(' ', '_')
        .take(MAX_RECOMMENDATION_FILE_NAME_LENGTH)
        .trim('_', '.')
        .ifBlank { DEFAULT_RECOMMENDATION_FILE_NAME }
    return "$friendlyName.$RECOMMENDATION_SHARE_EXTENSION"
}

private const val DEFAULT_RECOMMENDATION_FILE_NAME = "tankobun_recommendations"
private const val MAX_RECOMMENDATION_FILE_NAME_LENGTH = 72
