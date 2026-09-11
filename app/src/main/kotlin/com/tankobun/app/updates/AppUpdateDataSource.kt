package com.tankobun.app.updates

import android.net.Uri
import androidx.core.content.FileProvider
import com.tankobun.app.AppContainer
import com.tankobun.core.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
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
    private val distributionClient = container.okHttpClient.codeDistributionClient()
    private val apkValidator = AppUpdateApkValidator(container.application)
    suspend fun fetchUpdateInfo(manifestUrl: String): AppUpdateInfo =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(requireDistributionUrl(manifestUrl))
                .header("Accept", "application/json")
                .tag(ResponseByteLimit::class.java, ResponseByteLimit(1024L * 1024))
                .build()
            val call = distributionClient.newCall(request).also { it.timeout().timeout(TransferLimits.METADATA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS) }
            call.consumeCancellable { response, checkActive ->
                if (!response.isSuccessful) {
                    error("Update check failed: HTTP ${response.code}")
                }
                parseTankobunUpdateManifestJson(response.body.readBytesLimited(1024 * 1024, checkActive).decodeToString())
            }
        }

    suspend fun downloadUpdateApk(update: AppUpdateInfo): Uri =
        withContext(Dispatchers.IO) {
            val cacheDir = File(container.application.cacheDir, "app_updates").also { it.mkdirs() }
            val safeName = "tankobun-${update.versionCode}-${update.versionName}.apk"
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
            val apkFile = File(cacheDir, safeName)

            cacheDir.listFiles()
                ?.filter { it.name != apkFile.name }
                ?.forEach { it.delete() }

            val hash = normalizedSha256(update.apkSha256) ?: error("Update SHA-256 is required")
            downloadCodeFile(container.okHttpClient, update.apkUrl, apkFile, TransferLimits.APP_APK_BYTES,
                expectedSize = update.sizeBytes, expectedSha256 = hash) { apkValidator.validate(it, update) }

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
    requireDistributionUrl(apkUrl)
    val apkSha256 = normalizedSha256(release.optStringOrNull("apkSha256")) ?: error("Update manifest requires a valid SHA-256")
    val sizeBytes = release.optLongOrNull("sizeBytes")
    require(sizeBytes == null || sizeBytes in 1..TransferLimits.APP_APK_BYTES) { "Update manifest has an invalid APK size" }

    return AppUpdateInfo(
        versionCode = versionCode,
        versionName = versionName,
        apkUrl = apkUrl,
        apkSha256 = apkSha256,
        releaseUrl = release.optStringOrNull("releaseUrl"),
        publishedAt = release.optStringOrNull("publishedAt"),
        sizeBytes = sizeBytes,
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
