package com.tankobun.core.mangabaka

import com.tankobun.core.model.*
import com.tankobun.core.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** Small on-demand cache; deliberately does not mirror either provider's database. */
class MangaBakaRepository(
    private val client: OkHttpClient,
    private val identity: CatalogIdentity = CatalogIdentity.Default,
    private val baseUrl: String = "https://api.mangabaka.org",
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val searchLimiter = RespectfulRateLimiter(minSpacingMillis = 2_200L)
    private val limiter = RespectfulRateLimiter(minSpacingMillis = 400L)
    private val mutex = Mutex()
    private data class CachedResponse(val fetchedAt: Long, val body: JsonObject, val bytes: Int)
    private val cache = LinkedHashMap<String, CachedResponse>()
    private var cacheBytes = 0
    private var cacheGeneration = 0L
    private val locks = Array(32) { Mutex() }

    suspend fun clearCache() = mutex.withLock {
        cache.clear()
        cacheBytes = 0
        cacheGeneration++
    }

    internal suspend fun request(path: String, params: List<Pair<String, String>> = emptyList(), token: String? = null,
        method: String = "GET", body: JsonElement? = null, forceRefresh: Boolean = false): JsonObject = withContext(Dispatchers.IO) {
        val url = baseUrl.toHttpUrl().newBuilder().addPathSegments(path.trimStart('/')).apply {
            params.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        val key = url.toString()
        // Private responses never enter the catalog cache or shared HTTP cache.
        val cacheable = token == null && method == "GET"
        suspend fun cached() = mutex.withLock { cache[key]?.takeIf { System.currentTimeMillis() - it.fetchedAt < 15 * 60_000L }?.body }
        if (cacheable && !forceRefresh) cached()?.let { return@withContext it }
        val requestGeneration = mutex.withLock { cacheGeneration }
        val lock = locks[(key.hashCode() and Int.MAX_VALUE) % locks.size]
        lock.withLock {
            if (cacheable && !forceRefresh) cached()?.let { return@withLock it }
            val rate = if (path.endsWith("/search")) searchLimiter else limiter
            rate.awaitTurn()
            val request = Request.Builder().url(url).header("User-Agent", "Tankobun (https://github.com/JohanKRS/Tankobun)")
                .header("Accept", "application/json").apply {
                    if (token != null) { header("Authorization", "Bearer $token"); cacheControl(CacheControl.Builder().noCache().noStore().build()) }
                    else if (forceRefresh) cacheControl(CacheControl.FORCE_NETWORK)
                    if (method != "GET") method(method, body?.toString()?.toRequestBody("application/json".toMediaType()))
                }.build()
            var responseBytes = 0
            val response = client.newCall(request).awaitResponse().use { response ->
                rate.recordResponse(response.headers, response.code)
                if (!response.isSuccessful) throw IOException("MangaBaka HTTP ${response.code}")
                if (response.code == 204) return@use JsonObject(emptyMap())
                val bytes = requireNotNull(response.body).byteStream().use { it.readBytesLimited(8 * 1024 * 1024) }
                responseBytes = bytes.size
                json.parseToJsonElement(bytes.decodeToString()).jsonObject
            }
            if (cacheable) mutex.withLock {
                // A request started before manual cleanup must not refill the cleared cache.
                if (requestGeneration != cacheGeneration) return@withLock
                cache.remove(key)?.let { cacheBytes -= it.bytes }
                // Raw provider responses can be much larger than search pages. Bound the
                // aggregate payload as well as the count instead of retaining 80 large trees.
                if (responseBytes <= 4 * 1024 * 1024) {
                    cache[key] = CachedResponse(System.currentTimeMillis(), response, responseBytes)
                    cacheBytes += responseBytes
                }
                while (cache.size > 80 || cacheBytes > 4 * 1024 * 1024) {
                    cache.remove(cache.keys.first())?.let { cacheBytes -= it.bytes }
                }
            }
            response
        }
    }

    suspend fun search(query: String = "", page: Int = 1, limit: Int = 30, includeAdult: Boolean = false,
        sort: String = "relevance_desc", staff: String? = null, genres: Set<String> = emptySet(), tags: Set<String> = emptySet(),
        format: String? = null, status: String? = null, country: String? = null, year: Int? = null): AnilistMediaPage {
        val params = mutableListOf("page" to page.toString(), "limit" to limit.coerceIn(1, 100).toString(), "schema" to "full", "sort_by" to sort)
        if (query.isNotBlank()) params += "q" to query
        if (!staff.isNullOrBlank()) params += "staff" to staff
        if (!includeAdult) { params += "content_rating" to "safe"; params += "content_rating" to "suggestive" }
        if (format == "NOVEL") params += "type" to "novel"
        else if (country != null) when (country) { "JP" -> params += "type" to "manga"; "KR" -> params += "type" to "manhwa"; "CN" -> params += "type" to "manhua"; else -> return AnilistMediaPage(emptyList(), page, false) }
        else if (format != null) params += "type_not" to "novel"
        status?.let { mapOf("FINISHED" to "completed", "RELEASING" to "releasing", "HIATUS" to "hiatus", "CANCELLED" to "cancelled", "NOT_YET_RELEASED" to "upcoming")[it] }?.let { params += "status" to it }
        if (year != null) { params += "published_start_date_lower" to "$year-01-01"; params += "published_start_date_upper" to "$year-12-31" }
        val names = genres + tags + if (format == "ONE_SHOT") setOf("One Shot") else emptySet()
        if (names.isNotEmpty()) {
            val available = request("v1/tags").array("data").mapNotNull { it as? JsonObject }
            for (name in names) {
                val id = available.firstOrNull { it.text("name").equals(name, ignoreCase = true) }?.text("id")
                    ?: return AnilistMediaPage(emptyList(), page, false)
                params += "tag" to id
            }
            params += "tag_mode" to "and"
        }
        val result = request("v2/series/search", params)
        return mapPage(result, page, includeAdult)
    }

    private suspend fun mapPage(result: JsonObject, page: Int, includeAdult: Boolean): AnilistMediaPage {
        val media = result.array("data").mapNotNull { MangaBakaMapper.media(it.jsonObject) }
            .filter { includeAdult || !it.isAdult }.map { identity.resolve(it) }
        return AnilistMediaPage(media.distinctBy { it.id }, page, result.obj("pagination")["next"].let { it != null && it !is JsonNull })
    }

    suspend fun findByAniList(id: Int): AnilistMedia? {
        val candidates = request("v1/source/anilist/$id", listOf("with_source_response" to "false", "with_series" to "true"))
            .obj("data").array("series").mapNotNull { MangaBakaMapper.media(it.jsonObject) }
            .filter { it.anilistId == id }
        return candidates.singleOrNull()?.let { identity.resolve(it) }
    }

    suspend fun details(id: Int, includeAdult: Boolean = false): AnilistMedia? {
        val data = request("v2/series/$id", listOf("schema" to "full")).obj("data")
        val media = MangaBakaMapper.media(data) ?: return null
        if (media.isAdult && !includeAdult) return null
        val banner = try { MangaBakaMapper.banner(request("v1/series/$id/images", listOf("type" to "banner", "limit" to "50")), includeAdult) }
            catch (error: Exception) { if (error is CancellationException) throw error; null }
        val base = media.copy(bannerImage = banner)
        val enriched = if (base.staff.isEmpty() || base.startDateYear == null || base.chapters == null || base.volumes == null || base.bannerImage == null) {
            try { base.withMangaBakaSourceMetadata(request("v1/series/$id/full").obj("data")) }
            catch (error: Exception) { if (error is CancellationException) throw error; base }
        } else base
        return identity.resolve(enriched)
    }

    suspend fun recommendations(id: Int, page: Int, includeAdult: Boolean): AnilistRecommendationPage {
        // The similarity endpoint is a bounded list, not a paginated search endpoint.
        if (page != 1) return AnilistRecommendationPage(emptyList(), page, false)
        val result = request("v2/series/$id/similar", recommendationParams(includeAdult) + ("limit" to "24"))
        return AnilistRecommendationPage(rankedSeries(result, includeAdult).map { AnilistRecommendation(it, null) }, 1, false)
    }

    suspend fun mix(seeds: List<Int>, includeAdult: Boolean, limit: Int = 36, forceRefresh: Boolean = false): List<AnilistMedia> {
        val ids = seeds.filter { it > 0 }.distinct().take(5)
        if (ids.isEmpty()) return emptyList()
        val params = recommendationParams(includeAdult) + ids.map { "series" to it.toString() } +
            listOf("strict" to "true", "limit" to limit.coerceIn(1, 50).toString())
        return rankedSeries(request("v2/series/mix", params, forceRefresh = forceRefresh), includeAdult).filter { it.mangaBakaId !in ids }
    }

    private fun recommendationParams(includeAdult: Boolean) = buildList {
        add("schema" to "full")
        if (!includeAdult) { add("content_rating" to "safe"); add("content_rating" to "suggestive") }
    }

    private suspend fun rankedSeries(result: JsonObject, includeAdult: Boolean): List<AnilistMedia> = result.array("data")
        .mapNotNull { (it as? JsonObject)?.obj("series")?.let(MangaBakaMapper::media) }
        .filter { includeAdult || !it.isAdult }.map { identity.resolve(it) }.distinctBy { it.id }

    suspend fun profile(token: String): MangaBakaProfile {
        val data = request("v1/my/profile", token = token).obj("data")
        require(data.strings("scopes").containsAll(listOf("library.read", "library.write"))) { "Token needs library.read and library.write" }
        return MangaBakaProfile(requireNotNull(data.text("id")), data.text("nickname") ?: data.text("preferred_username") ?: "MangaBaka")
    }

    suspend fun library(token: String, page: Int): MangaBakaLibraryPage {
        val response = request("v2/my/library", listOf("page" to page.toString(), "limit" to "100", "schema" to "full"), token)
        val entries = response.array("data").mapNotNull { raw ->
            val row = raw.jsonObject
            val media = MangaBakaMapper.media(row.obj("series"))?.let { identity.resolve(it) } ?: return@mapNotNull null
            val entry = row.obj("entry")
            media to AnilistListEntry(
                id = -kotlin.math.abs(media.id), mediaId = media.id,
                status = when (entry.text("state")) { "reading" -> MediaStatus.CURRENT; "completed" -> MediaStatus.COMPLETED; "dropped" -> MediaStatus.DROPPED; "paused" -> MediaStatus.PAUSED; "rereading" -> MediaStatus.REPEATING; else -> MediaStatus.PLANNING },
                progress = entry.number("progress_chapter")?.toInt()?.coerceAtLeast(0) ?: 0,
                score = entry.number("rating"), notes = entry.text("note"), private = entry["is_private"] == JsonPrimitive(true),
                customLists = emptyList(), updatedAtEpochSeconds = null,
            )
        }
        return MangaBakaLibraryPage(entries, response.obj("pagination")["next"].let { it != null && it !is JsonNull })
    }

    suspend fun updateTracking(token: String, id: Int, payload: String) {
        // Batch with one entry is the documented atomic create-or-patch operation.
        val fields = json.parseToJsonElement(payload).jsonObject
        val body = JsonArray(listOf(JsonObject(fields + ("series_id" to JsonPrimitive(id)))))
        request("v1/my/library/batch", token = token, method = "POST", body = body)
    }
    suspend fun deleteTracking(token: String, id: Int) {
        try { request("v1/my/library/$id", token = token, method = "DELETE") }
        catch (e: IOException) { if (e.message != "MangaBaka HTTP 404") throw e }
    }
}

data class MangaBakaProfile(val id: String, val name: String)
data class MangaBakaLibraryPage(val entries: List<Pair<AnilistMedia, AnilistListEntry>>, val hasNextPage: Boolean)
