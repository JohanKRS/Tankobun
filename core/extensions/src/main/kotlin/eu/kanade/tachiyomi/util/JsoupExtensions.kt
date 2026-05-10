package eu.kanade.tachiyomi.util

import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun Response.asJsoup(html: String = body?.string().orEmpty()): Document {
    return Jsoup.parse(html, request.url.toString())
}
