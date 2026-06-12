package com.tankobun.core.anilist

import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AnilistGraphQlClient(
    private val okHttpClient: OkHttpClient,
    private val rateLimiter: RespectfulRateLimiter,
    private val endpoint: String = "https://graphql.anilist.co",
    private val userAgent: String? = null,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun execute(
        query: String,
        variables: JsonObject = JsonObject(emptyMap()),
        accessToken: String? = null,
    ): JsonObject = rateLimiter.run {
        withContext(Dispatchers.IO) {
            val body = buildJsonObject {
                put("query", query)
                put("variables", variables)
            }.toString().toRequestBody("application/json".toMediaType())

            val requestBuilder = Request.Builder()
                .url(endpoint)
                .post(body)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")

            userAgent?.takeIf { it.isNotBlank() }?.let { agent ->
                requestBuilder.header("User-Agent", agent)
            }

            if (!accessToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $accessToken")
            }

            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                rateLimiter.recordResponse(response.headers, response.code)
                val raw = response.body?.string().orEmpty()
                val root = json.parseToJsonElement(raw).jsonObject
                if (!response.isSuccessful || root["errors"] != null) {
                    throw AnilistGraphQlException(
                        message = root["errors"]?.toString() ?: response.message,
                        statusCode = response.code,
                    )
                }
                root["data"]?.jsonObject ?: JsonObject(emptyMap())
            }
        }
    }
}

class AnilistGraphQlException(
    override val message: String,
    val statusCode: Int?,
) : RuntimeException(message)
