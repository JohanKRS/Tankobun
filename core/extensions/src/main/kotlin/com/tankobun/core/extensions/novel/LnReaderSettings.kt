package com.tankobun.core.extensions.novel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

enum class LnReaderSettingType { TEXT, SWITCH, SELECT, CHECKBOX_GROUP }
data class LnReaderSettingOption(val label: String, val value: String)
data class LnReaderSetting(
    val key: String,
    val label: String,
    val type: LnReaderSettingType,
    val value: Any,
    val options: List<LnReaderSettingOption>,
    val sensitive: Boolean,
)

fun lnReaderSettings(definitions: JSONObject, values: JSONObject): List<LnReaderSetting> =
    definitions.keys().asSequence().map { key ->
        val item = definitions.getJSONObject(key)
        val label = item.optString("label", key)
        val type = when (item.optString("type", "Text")) {
            "Text" -> LnReaderSettingType.TEXT
            "Switch" -> LnReaderSettingType.SWITCH
            "Select" -> LnReaderSettingType.SELECT
            "CheckboxGroup" -> LnReaderSettingType.CHECKBOX_GROUP
            else -> error("Unsupported source setting: $label")
        }
        val options = item.optJSONArray("options") ?: JSONArray()
        LnReaderSetting(key, label, type, values.opt(key) ?: item.get("value"),
            (0 until options.length()).map { index -> options.getJSONObject(index).let { LnReaderSettingOption(it.getString("label"), it.getString("value")) } },
            Regex("password|passwd|secret|token|credential|authorization|api.?key|senha|密码", RegexOption.IGNORE_CASE).containsMatchIn("$key $label"))
    }.toList()

/** Explicit preferences and website snapshots are kept apart from plugin-managed caches. */
class LnReaderSettingsStore(private val context: Context, private val packageName: String) {
    init { require(packageName.matches(Regex("com\\.tankobun\\.lnreader\\.[a-f0-9]{32}"))) }
    private val preferences = context.getSharedPreferences("novel_$packageName", Context.MODE_PRIVATE)
    fun configuredValues(): JSONObject = read("settings")
    fun values(): JSONObject = configuredValues().apply {
        val storage = read("storage")
        val applied = read("appliedSettings")
        definitions().keys().forEach { key ->
            // A saved/restored preference takes effect immediately, before the next plugin call.
            if (has(key) && (!applied.has(key) || opt(key).toString() != applied.opt(key).toString())) return@forEach
            storage.optJSONObject(key)?.let { item ->
                if (!item.has("expires") || item.optLong("expires") >= System.currentTimeMillis()) item.opt("value")?.let { put(key, it) }
            }
        }
    }
    fun definitions(): JSONObject = read("settingDefinitions")
    fun revision(): Long = preferences.getLong("revision", 0L)
    fun saveDefinitions(value: JSONObject) { preferences.edit().putString("settingDefinitions", value.toString()).apply() }
    fun save(values: JSONObject) {
        validateLnReaderSettings(definitions(), values)
        if (values.toString() != values().toString() || values.toString() != configuredValues().toString()) {
            check(preferences.edit().putString("settings", values.toString()).putString("appliedSettings", "{}").putLong("revision", revision() + 1).commit())
        }
    }
    fun webStorage(): JSONObject = read("webOrigins")
    fun capture(origin: String, local: JSONObject, session: JSONObject) {
        require(webOrigin(origin) == origin)
        val origins = webStorage()
        val value = JSONObject().put("local", local).put("session", session)
        check(value.toString().length <= 2 * 1024 * 1024) { "Website storage is too large" }
        if (origins.optJSONObject(origin)?.toString() == value.toString()) return
        origins.put(origin, value)
        check(preferences.edit().putString("webOrigins", origins.toString()).putLong("revision", revision() + 1).commit())
    }
    private fun read(key: String) = runCatching { JSONObject(preferences.getString(key, "{}").orEmpty()) }.getOrDefault(JSONObject())

    fun backupValues(): JSONObject = safeLnReaderBackupValues(definitions(), values())
    fun restoreValues(values: JSONObject) {
        // A source need not be installed yet. Validate against its schema when it is reinstalled.
        val filtered = JSONObject()
        values.keys().forEach { key ->
            val value = values.get(key)
            if (value is Boolean || value is String || value is JSONArray) filtered.put(key, value)
        }
        check(preferences.edit().putString("settings", filtered.toString()).putString("appliedSettings", "{}").putLong("revision", revision() + 1).commit())
    }
}

internal fun validateLnReaderSettings(definitions: JSONObject, values: JSONObject) {
    val settings = lnReaderSettings(definitions, values).associateBy { it.key }
    values.keys().forEach { key ->
        val setting = settings[key] ?: error("Unknown source setting")
        val value = values.get(key)
        val valid = when (setting.type) {
            LnReaderSettingType.TEXT -> value is String && value.length <= 16_384
            LnReaderSettingType.SWITCH -> value is Boolean
            LnReaderSettingType.SELECT -> value is String && setting.options.any { it.value == value }
            LnReaderSettingType.CHECKBOX_GROUP -> value is JSONArray && (0 until value.length()).all { index -> setting.options.any { it.value == value.opt(index) } }
        }
        require(valid) { "Invalid value for ${setting.label}" }
    }
}

internal fun safeLnReaderBackupValues(definitions: JSONObject, values: JSONObject): JSONObject = JSONObject().apply {
    lnReaderSettings(definitions, values).filter { !it.sensitive && values.has(it.key) }.forEach { setting ->
        // Arbitrary text can contain credentials even when a plugin did not label it as such.
        if (setting.type != LnReaderSettingType.TEXT) put(setting.key, values.get(setting.key))
    }
}

fun webOrigin(url: String): String? = runCatching {
    val uri = URI(url)
    val scheme = uri.scheme?.lowercase()
    require(scheme == "https" || scheme == "http")
    require(uri.userInfo == null && !uri.host.isNullOrBlank())
    val port = uri.port.takeUnless { it < 0 || (scheme == "https" && it == 443) || (scheme == "http" && it == 80) }
    "$scheme://${uri.host.lowercase()}${port?.let { ":$it" }.orEmpty()}"
}.getOrNull()
