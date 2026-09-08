package com.tankobun.core.extensions.novel

import android.content.Context
import android.util.AtomicFile
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.extensions.ExtensionIndexSource
import com.tankobun.core.model.ReadingContentKind
import com.tankobun.core.model.SourceDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.security.MessageDigest

const val LNREADER_PACKAGE_PREFIX = "com.tankobun.lnreader."

data class LnReaderChapterAssets(val javascript: String = "", val css: String = "")

@Serializable
data class LnReaderPlugin(
    val id: String,
    val name: String,
    val site: String,
    val lang: String,
    val version: String,
    val url: String,
    val iconUrl: String? = null,
    val customJS: String? = null,
    val customCSS: String? = null,
    val repositoryUrl: String = "",
) {
    val packageName: String get() = LNREADER_PACKAGE_PREFIX + digest("$repositoryUrl\n$id".toByteArray()).take(32)
    val sourceId: Long get() = ByteBuffer.wrap(MessageDigest.getInstance("SHA-256").digest(packageName.toByteArray())).long and Long.MAX_VALUE
    val language: String get() = languageCode(lang)
    val versionCode: Int get() = version.split('.').take(3).fold(0) { result, part -> result * 1000 + (part.toIntOrNull() ?: 0).coerceIn(0, 999) }
    fun descriptor() = SourceDescriptor(sourceId, name, language, packageName, version, versionCode, isNsfw = false, installed = true, contentKind = ReadingContentKind.NOVEL, isPrivateExtension = true)
    fun indexEntry() = ExtensionIndexEntry(name, packageName, url, language, versionCode, version,
        sources = listOf(ExtensionIndexSource(name, language, sourceId)), iconUrl = iconUrl,
        repositoryUrl = repositoryUrl, lnReaderPlugin = this)
}

/** Only explicitly installed user plugins live here. Each version is atomically committed as one file. */
class LnReaderPluginStore(context: Context) {
    private val directory = File(context.filesDir, "novel_plugins")
    private val json = Json { ignoreUnknownKeys = true }
    fun installed(): List<LnReaderPlugin> = directory.listFiles().orEmpty().filter { it.extension == "json" }.mapNotNull {
        runCatching { val record = JSONObject(AtomicFile(it).openRead().bufferedReader().use { input -> input.readText() });
            val plugin = json.decodeFromString<LnReaderPlugin>(record.getString("manifest"))
            plugin.takeIf { candidate -> candidate.packageName == it.nameWithoutExtension && digest(record.getString("code").toByteArray()) == record.getString("sha256") }
        }.getOrNull()
    }
    fun record(packageName: String): Pair<LnReaderPlugin, String>? = runCatching {
        require(packageName.matches(Regex("com\\.tankobun\\.lnreader\\.[a-f0-9]{32}")))
        val record = JSONObject(AtomicFile(File(directory, "$packageName.json")).openRead().bufferedReader().use { it.readText() })
        val code = record.getString("code")
        check(digest(code.toByteArray()) == record.getString("sha256")) { "Plugin integrity check failed" }
        val plugin = json.decodeFromString<LnReaderPlugin>(record.getString("manifest"))
        check(plugin.packageName == packageName)
        verifyChapterAssets(record)
        plugin to code
    }.getOrNull()
    fun chapterAssets(packageName: String): LnReaderChapterAssets {
        check(record(packageName) != null) { "Novel plugin is not installed" }
        val record = JSONObject(AtomicFile(File(directory, "$packageName.json")).openRead().bufferedReader().use { it.readText() })
        verifyChapterAssets(record)
        return LnReaderChapterAssets(record.optString("chapterJS"), record.optString("chapterCSS"))
    }
    fun contentVersion(packageName: String): String {
        val (_, code) = record(packageName) ?: error("Novel plugin is not installed")
        val assets = chapterAssets(packageName)
        return digest((code + assets.javascript + assets.css).toByteArray()).take(20)
    }
    private fun verifyChapterAssets(record: JSONObject) {
        listOf("chapterJS", "chapterCSS").forEach { key ->
            if (record.has(key)) check(digest(record.getString(key).toByteArray()) == record.getString("${key}Sha256")) { "Plugin asset integrity check failed" }
        }
    }
    suspend fun install(plugin: LnReaderPlugin, client: OkHttpClient, context: Context) = withContext(Dispatchers.IO) {
        require(plugin.repositoryUrl.isNotBlank() && plugin.id.isNotBlank())
        val request = Request.Builder().url(plugin.url).build()
        require(request.url.isHttps) { "Novel plugins must be downloaded over HTTPS" }
        val code = client.newCall(request).execute().use { response ->
            check(response.isSuccessful && response.request.url.isHttps) { "Plugin download failed: HTTP ${response.code}" }
            val bytes = response.body.byteStream().use { readBounded(it, 4 * 1024 * 1024) }
            check(bytes.isNotEmpty() && bytes.size <= 4 * 1024 * 1024) { "Invalid plugin size" }
            bytes.decodeToString()
        }
        val metadata = LnReaderRuntime(context).call(plugin, code, "metadata", JSONArray(), persistStorage = false) as JSONObject
        check(metadata.optString("id") == plugin.id && metadata.optString("version") == plugin.version) { "Plugin does not match repository metadata" }
        fun downloadAsset(url: String?): String {
            if (url.isNullOrBlank()) return ""
            val assetRequest = Request.Builder().url(URI(plugin.repositoryUrl).resolve(url).toString()).build()
            require(assetRequest.url.isHttps) { "Plugin assets must use HTTPS" }
            return client.newCall(assetRequest).execute().use { response ->
                check(response.isSuccessful && response.request.url.isHttps) { "Plugin asset download failed: HTTP ${response.code}" }
                response.body.byteStream().use { readBounded(it, 1024 * 1024) }.decodeToString()
            }
        }
        val javascript = downloadAsset(plugin.customJS)
        val css = downloadAsset(plugin.customCSS)
        directory.mkdirs()
        val target = AtomicFile(File(directory, "${plugin.packageName}.json"))
        val output = target.startWrite()
        try {
            output.write(JSONObject().put("manifest", json.encodeToString(LnReaderPlugin.serializer(), plugin)).put("code", code).put("sha256", digest(code.toByteArray()))
                .put("chapterJS", javascript).put("chapterJSSha256", digest(javascript.toByteArray()))
                .put("chapterCSS", css).put("chapterCSSSha256", digest(css.toByteArray())).toString().toByteArray())
            target.finishWrite(output)
        } catch (error: Throwable) { target.failWrite(output); throw error }
        LnReaderSettingsStore(context, plugin.packageName).saveDefinitions(metadata.optJSONObject("pluginSettings") ?: JSONObject())
    }
    fun uninstall(packageName: String) {
        if (record(packageName) == null) return
        AtomicFile(File(directory, "$packageName.json")).delete()
        // Keep source settings and library bindings so reinstalling can restore access.
    }
}

internal fun parseLnReaderIndex(payload: String, repositoryUrl: String): List<ExtensionIndexEntry>? {
    val array = JSONArray(payload)
    if (array.length() == 0 || !array.getJSONObject(0).has("url") || !array.getJSONObject(0).has("site")) return null
    val json = Json { ignoreUnknownKeys = true }
    return (0 until array.length()).map { index ->
        val plugin = json.decodeFromString<LnReaderPlugin>(array.getJSONObject(index).toString())
        plugin.copy(repositoryUrl = repositoryUrl,
            url = URI(repositoryUrl).resolve(plugin.url).toString(),
            iconUrl = plugin.iconUrl?.let { URI(repositoryUrl).resolve(it).toString() }).indexEntry()
    }
}

internal fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
internal fun languageCode(value: String): String = when (value.replace(Regex("[\\p{Cf}]"), "").trim().lowercase()) {
    "english" -> "en"; "português", "portuguese" -> "pt"; "español", "spanish" -> "es"
    "русский", "russian" -> "ru"; "中文", "中文, 汉语, 漢語", "chinese" -> "zh"; "日本語", "japanese" -> "ja"
    "indonesian", "bahasa indonesia" -> "id"; "français", "french" -> "fr"; "deutsch", "german" -> "de"
    "türkçe", "turkish" -> "tr"; "한국어", "조선말, 한국어", "korean" -> "ko"; "italian", "italiano" -> "it"
    "arabic", "العربية" -> "ar"; "vietnamese", "tiếng việt" -> "vi"; "thai", "ไทย" -> "th"
    "українська", "ukrainian" -> "uk"; "polish", "polski" -> "pl"; "multi", "all" -> "all"; else -> value.lowercase()
}
