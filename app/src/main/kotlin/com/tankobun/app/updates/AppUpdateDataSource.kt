package com.tankobun.app.updates

import android.net.Uri
import androidx.core.content.FileProvider
import com.tankobun.app.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Locale

internal const val TANKOBUN_UPDATE_MANIFEST_TYPE = "tankobun.update-manifest"

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val apkSha256: String?,
    val releaseUrl: String?,
    val publishedAt: String?,
    val sizeBytes: Long?,
    val mandatory: Boolean,
    val changelog: Map<String, List<String>>,
)

internal class AppUpdateDataSource(
    private val container: AppContainer,
) {
    suspend fun fetchUpdateInfo(manifestUrl: String): AppUpdateInfo =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(manifestUrl)
                .header("Accept", "application/json")
                .build()
            container.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Update check failed: HTTP ${response.code}")
                }
                parseTankobunUpdateManifestJson(response.body.string())
            }
        }

    suspend fun downloadUpdateApk(update: AppUpdateInfo): Uri =
        withContext(Dispatchers.IO) {
            val cacheDir = File(container.application.cacheDir, "app_updates").also { it.mkdirs() }
            val safeName = "tankobun-${update.versionCode}-${update.versionName}.apk"
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
            val apkFile = File(cacheDir, safeName)
            val partialFile = File(cacheDir, "$safeName.part")

            cacheDir.listFiles()
                ?.filter { it.name != apkFile.name }
                ?.forEach { it.delete() }

            val request = Request.Builder()
                .url(update.apkUrl)
                .header("Accept", "application/vnd.android.package-archive")
                .build()
            container.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("APK download failed: HTTP ${response.code}")
                }
                partialFile.outputStream().use { output ->
                    response.body.byteStream().use { input -> input.copyTo(output) }
                }
            }

            if (partialFile.length() <= 0L) {
                partialFile.delete()
                error("APK download failed: empty file")
            }
            update.apkSha256?.takeIf { it.isNotBlank() }?.let { expected ->
                val actual = partialFile.sha256()
                if (!actual.equals(expected.removePrefix("sha256:").trim(), ignoreCase = true)) {
                    partialFile.delete()
                    error("APK download failed: SHA-256 mismatch")
                }
            }
            if (apkFile.exists()) apkFile.delete()
            check(partialFile.renameTo(apkFile)) { "APK download failed: could not finalize file" }

            FileProvider.getUriForFile(
                container.application,
                "${container.application.packageName}.fileprovider",
                apkFile,
            )
        }
}

internal fun parseTankobunUpdateManifestJson(text: String): AppUpdateInfo {
    val root = JSONObject(text)
    require(root.optString("type") == TANKOBUN_UPDATE_MANIFEST_TYPE) { "Unsupported update manifest" }
    require(root.optInt("version", 0) == 1) { "Unsupported update manifest version" }
    val release = root.optJSONObject("stable")
        ?: root.optJSONObject("latest")
        ?: error("Update manifest has no stable release")
    val versionCode = release.optInt("versionCode", -1)
    require(versionCode > 0) { "Update manifest release has no valid versionCode" }
    val versionName = release.optString("versionName").trim()
    require(versionName.isNotBlank()) { "Update manifest release has no versionName" }
    val apkUrl = release.optString("apkUrl").trim()
    require(apkUrl.startsWith("https://")) { "Update manifest APK URL must use HTTPS" }

    return AppUpdateInfo(
        versionCode = versionCode,
        versionName = versionName,
        apkUrl = apkUrl,
        apkSha256 = release.optStringOrNull("apkSha256"),
        releaseUrl = release.optStringOrNull("releaseUrl"),
        publishedAt = release.optStringOrNull("publishedAt"),
        sizeBytes = release.optLongOrNull("sizeBytes"),
        mandatory = release.optBoolean("mandatory", false),
        changelog = release.optJSONObject("changelog").toChangelogMap(),
    )
}

private fun JSONObject?.toChangelogMap(): Map<String, List<String>> {
    if (this == null) return emptyMap()
    return keys().asSequence()
        .associateWith { key ->
            when (val value = opt(key)) {
                is JSONArray -> value.stringValues()
                is String -> value.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
                else -> emptyList()
            }
        }
        .filterValues { it.isNotEmpty() }
}

private fun JSONArray.stringValues(): List<String> =
    (0 until length()).mapNotNull { index -> optString(index).trim().takeIf { it.isNotBlank() } }

private fun JSONObject.optStringOrNull(name: String): String? =
    optString(name).trim().takeIf { it.isNotBlank() && it.lowercase(Locale.ROOT) != "null" }

private fun JSONObject.optLongOrNull(name: String): Long? =
    if (has(name) && !isNull(name)) optLong(name) else null

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}
