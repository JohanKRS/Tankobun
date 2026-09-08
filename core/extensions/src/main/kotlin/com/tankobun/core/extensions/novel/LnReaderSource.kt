package com.tankobun.core.extensions.novel

import android.content.Context
import eu.kanade.tachiyomi.source.NovelSource
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Headers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

class LnReaderSource(private val context: Context, private val plugin: LnReaderPlugin) : HttpSource(), NovelSource {
    override val id = plugin.sourceId
    override val name = plugin.name
    override val lang = plugin.language
    private var resolvedSite: String? = null
    override val baseUrl get() = resolvedSite ?: plugin.site
    override val isNovelSource = true
    private val runtime = LnReaderRuntime(context)
    private var imageHeaders: Headers? = null
    private val metadataLock = Mutex()
    private var hasResolveUrl = false
    private val chapterUrls = java.util.concurrent.ConcurrentHashMap<String, String>()
    private data class DetailsSnapshot(val value: JSONObject, val completeChapters: Boolean, val savedAt: Long)
    private val recentDetails = LinkedHashMap<String, DetailsSnapshot>()
    override val headers: Headers get() = imageHeaders ?: super.headers
    private suspend fun call(operation: String, vararg args: Any): Any? {
        val (_, code) = LnReaderPluginStore(context).record(plugin.packageName) ?: error("Novel plugin is not installed")
        return runtime.call(plugin, code, operation, JSONArray(args.toList()))
    }
    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage = list(call("searchNovels", query, page))
    override suspend fun getPopularManga(page: Int): MangasPage = list(call("popularNovels", page))
    override suspend fun getLatestUpdates(page: Int): MangasPage = list(call("popularNovels", page, JSONObject().put("showLatestNovels", true)))
    private fun list(value: Any?): MangasPage {
        val entries = value as? JSONArray ?: error("Plugin returned an invalid search result")
        return MangasPage((0 until entries.length()).map { entries.getJSONObject(it).manga() }, entries.length() > 0)
    }
    override suspend fun getMangaUpdate(manga: SManga, chapters: List<SChapter>, fetchDetails: Boolean, fetchChapters: Boolean): SMangaUpdate {
        val now = System.currentTimeMillis()
        val cached = synchronized(recentDetails) { recentDetails[manga.url] }
            ?.takeIf { now - it.savedAt < 60_000L && (!fetchChapters || it.completeChapters) }
        val details = cached?.value ?: (call("details", manga.url, fetchChapters) as JSONObject).also { value ->
            synchronized(recentDetails) {
                recentDetails[manga.url] = DetailsSnapshot(value, fetchChapters, now)
                while (recentDetails.size > 4) recentDetails.remove(recentDetails.keys.first())
            }
        }
        val items = details.optJSONArray("chapters") ?: JSONArray()
        return SMangaUpdate(if (fetchDetails) details.manga().apply { url = manga.url; initialized = true } else manga,
            if (fetchChapters) items.lnReaderChapters() else chapters)
    }
    override fun getChapterUrl(chapter: SChapter): String = chapterUrls[chapter.url] ?: java.net.URI(baseUrl.trimEnd('/') + "/").resolve(chapter.url).toString()
    override suspend fun getPageList(chapter: SChapter): List<Page> {
        ensureImageHeaders()
        if (hasResolveUrl) (call("resolveUrl", chapter.url, false) as? String)?.let { chapterUrls[chapter.url] = it }
        return listOf(Page(0, chapter.url))
    }
    suspend fun ensureImageHeaders() = metadataLock.withLock {
        if (imageHeaders == null) {
            val metadata = call("metadata") as JSONObject
            resolvedSite = metadata.optString("site").takeIf { webOrigin(it) != null }
            hasResolveUrl = metadata.optBoolean("hasResolveUrl")
            imageHeaders = headersBuilder().apply {
                metadata.optJSONObject("imageRequestInit")?.optJSONObject("headers")?.let { values -> values.keys().forEach { set(it, values.getString(it)) } }
            }.build()
        }
    }
    override suspend fun fetchPageText(page: Page): String {
        ensureImageHeaders()
        return call("parseChapter", page.url) as? String ?: error("Plugin returned an invalid text chapter")
    }
    private fun JSONObject.manga() = SManga.create().apply {
        url = getString("path"); title = getString("name"); thumbnail_url = optString("cover").takeIf { it.isNotBlank() }
        description = optString("summary").takeIf { it.isNotBlank() }; author = optString("author").takeIf { it.isNotBlank() }
        artist = optString("artist").takeIf { it.isNotBlank() }; genre = optString("genres").takeIf { it.isNotBlank() }
        status = when (optString("status")) { "Ongoing" -> SManga.ONGOING; "Completed" -> SManga.COMPLETED; "Publishing Finished" -> SManga.PUBLISHING_FINISHED; "Cancelled" -> SManga.CANCELLED; "On Hiatus" -> SManga.ON_HIATUS; "Licensed" -> SManga.LICENSED; else -> SManga.UNKNOWN }
    }
}
