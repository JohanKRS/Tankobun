package com.tankobun.app.logic

import org.json.JSONObject

internal fun JSONObject.nullableString(name: String): String? =
    if (!has(name) || isNull(name)) null else optString(name)

internal fun JSONObject.nullableInt(name: String): Int? =
    if (!has(name) || isNull(name)) null else optInt(name)

internal fun JSONObject.nullableDouble(name: String): Double? =
    if (!has(name) || isNull(name)) null else optDouble(name)

internal fun JSONObject.nullableBoolean(name: String): Boolean? =
    if (!has(name) || isNull(name)) null else optBoolean(name)

internal fun JSONObject.nullableStringList(name: String): List<String>? {
    if (!has(name) || isNull(name)) return null
    val array = optJSONArray(name) ?: return null
    return buildList {
        for (index in 0 until array.length()) {
            val value = array.optString(index).trim()
            if (value.isNotBlank()) add(value)
        }
    }
}
