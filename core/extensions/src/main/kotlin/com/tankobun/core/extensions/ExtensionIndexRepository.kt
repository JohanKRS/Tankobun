package com.tankobun.core.extensions

import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI

class ExtensionIndexRepository(
    private val okHttpClient: OkHttpClient,
    private val rateLimiter: RespectfulRateLimiter,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun fetchIndex(indexUrl: String): List<ExtensionIndexEntry> = rateLimiter.run {
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(indexUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                rateLimiter.recordResponse(response.headers, response.code)
                if (!response.isSuccessful) {
                    error("Failed to fetch extension index: HTTP ${response.code}")
                }
                json.decodeFromString<List<ExtensionIndexEntry>>(response.body?.string().orEmpty())
            }
        }
    }

    fun apkUrl(indexUrl: String, entry: ExtensionIndexEntry): String {
        if (entry.apkName.startsWith("https://") || entry.apkName.startsWith("http://")) {
            return entry.apkName
        }
        val uri = URI(indexUrl)
        val indexPath = uri.path.substringBeforeLast('/', "")
        val apkPath = "$indexPath/apk/${entry.apkName}".replace("//", "/")
        return URI(uri.scheme, uri.authority, apkPath, null, null).toString()
    }

    fun iconUrl(indexUrl: String, entry: ExtensionIndexEntry): String {
        val uri = URI(indexUrl)
        val indexPath = uri.path.substringBeforeLast('/', "")
        val iconPath = "$indexPath/icon/${entry.packageName}.png".replace("//", "/")
        return URI(uri.scheme, uri.authority, iconPath, null, null).toString()
    }
}
