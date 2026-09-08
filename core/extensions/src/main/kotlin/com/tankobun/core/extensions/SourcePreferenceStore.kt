package com.tankobun.core.extensions

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.*
import org.json.JSONArray
import org.json.JSONObject

/** Uses the same per-source file as community APK extensions. Never scans arbitrary app preferences. */
class SourcePreferenceStore(private val context: Context) {
    private val registry = context.getSharedPreferences("source_preference_schemas", Context.MODE_PRIVATE)

    fun preferences(sourceId: Long): SharedPreferences = context.getSharedPreferences("source_$sourceId", Context.MODE_PRIVATE)

    fun register(packageName: String, sourceId: Long, screen: PreferenceScreen) {
        require(isSupportedExtensionPackageName(packageName))
        val fields = JSONObject()
        fun visit(group: PreferenceGroup) {
            for (index in 0 until group.preferenceCount) {
                val preference = group.getPreference(index)
                if (preference is PreferenceGroup) visit(preference)
                val key = preference.key ?: continue
                if (!preference.isPersistent || preference.preferenceDataStore != null) continue
                val type = when (preference) {
                    is TwoStatePreference -> "boolean"
                    is ListPreference -> "string"
                    is MultiSelectListPreference -> "set"
                    is SeekBarPreference -> "int"
                    else -> continue // Text and arbitrary plugin storage can contain credentials.
                }
                if (!Regex("password|secret|token|credential|api.?key|senha", RegexOption.IGNORE_CASE).containsMatchIn("$key ${preference.title}")) fields.put(key, type)
            }
        }
        visit(screen)
        registry.edit().putString(sourceId.toString(), JSONObject().put("package", packageName).put("fields", fields).toString()).apply()
    }

    fun backup(): JSONObject = JSONObject().apply {
        registry.all.forEach { (sourceId, encoded) ->
            if (sourceId.toLongOrNull() == null) return@forEach
            val schema = runCatching { JSONObject(encoded as String) }.getOrNull() ?: return@forEach
            val fields = schema.optJSONObject("fields") ?: return@forEach
            val saved = preferences(sourceId.toLong()).all
            val values = JSONObject()
            fields.keys().forEach { key ->
                val value = saved[key] ?: return@forEach
                val type = fields.getString(key)
                if (validValue(type, value)) values.put(key, JSONObject().put("type", type).put("value", if (value is Set<*>) JSONArray(value.toList().sortedBy { it.toString() }) else value))
            }
            put(sourceId, JSONObject().put("package", schema.getString("package")).put("values", values))
        }
    }

    fun restore(backup: JSONObject): Set<String> = buildSet {
        backup.keys().forEach { sourceId ->
            val id = sourceId.toLongOrNull() ?: return@forEach
            val entry = backup.optJSONObject(sourceId) ?: return@forEach
            val packageName = entry.optString("package")
            if (!isSupportedExtensionPackageName(packageName)) return@forEach
            val values = entry.optJSONObject("values") ?: return@forEach
            val fields = JSONObject()
            val edit = preferences(id).edit()
            values.keys().forEach { key ->
                val item = values.optJSONObject(key) ?: return@forEach
                val type = item.optString("type")
                val raw = item.opt("value")
                val value = if (type == "set" && raw is JSONArray && (0 until raw.length()).all { raw.opt(it) is String })
                    (0 until raw.length()).map { raw.getString(it) }.toSet() else raw
                if (!validValue(type, value)) return@forEach
                when (type) {
                    "boolean" -> edit.putBoolean(key, value as Boolean)
                    "string" -> edit.putString(key, value as String)
                    "set" -> { @Suppress("UNCHECKED_CAST") edit.putStringSet(key, value as Set<String>) }
                    "int" -> edit.putInt(key, value as Int)
                }
                fields.put(key, type)
            }
            check(edit.commit())
            registry.edit().putString(sourceId, JSONObject().put("package", packageName).put("fields", fields).toString()).apply()
            add(packageName)
        }
    }

    private fun validValue(type: String, value: Any?): Boolean = when (type) {
        "boolean" -> value is Boolean
        "string" -> value is String && value.length <= 16_384
        "set" -> value is Set<*> && value.size <= 1024 && value.all { it is String && it.length <= 16_384 }
        "int" -> value is Int
        else -> false
    }
}
