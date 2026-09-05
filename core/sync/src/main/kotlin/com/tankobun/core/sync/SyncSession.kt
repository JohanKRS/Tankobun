package com.tankobun.core.sync

import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A one-way session binding, never a stored bearer credential. */
fun syncSessionKey(accessToken: String?): String? = accessToken?.takeIf { it.isNotBlank() }?.let {
    MessageDigest.getInstance("SHA-256").digest(it.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}

fun belongsToSyncSession(payloadJson: String, sessionKey: String): Boolean =
    runCatching {
        Json.parseToJsonElement(payloadJson).jsonObject["sessionKey"]?.jsonPrimitive?.content == sessionKey
    }.getOrDefault(false)
