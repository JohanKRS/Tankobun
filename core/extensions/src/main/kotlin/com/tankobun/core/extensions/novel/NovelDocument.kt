package com.tankobun.core.extensions.novel

import com.tankobun.core.model.NovelBlock
import com.tankobun.core.model.ReaderPage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.safety.Safelist

/** Text is split by content, never by screen geometry. Reflow keeps the saved block/character anchor. */
object NovelDocument {
    private val json = Json { ignoreUnknownKeys = true }
    private val allowed = Safelist.basic().addTags("ruby", "rt", "rp", "h1", "h2", "h3", "h4", "h5", "h6", "img", "div", "section", "article")
        .addAttributes(":all", "id").addAttributes("img", "src", "alt").addProtocols("img", "src", "https", "http")
        .removeTags("a").preserveRelativeLinks(false)

    fun parse(html: String, baseUrl: String, headers: Map<String, String> = emptyMap()): List<ReaderPage> {
        val document = Jsoup.parseBodyFragment(html, baseUrl)
        document.select("script,style,iframe,form,button,input,nav,header,footer,object,embed,svg,canvas").remove()
        val clean = Jsoup.parseBodyFragment(Jsoup.clean(document.body().html(), baseUrl, allowed), baseUrl)
        val output = mutableListOf<ReaderPage>()
        val buffer = StringBuilder()
        fun flush(heading: Int = 0) {
            val text = Jsoup.parseBodyFragment(buffer.toString()).text()
            if (text.isNotBlank()) {
                val block = NovelBlock(html = buffer.toString(), text = text, headingLevel = heading)
                output += ReaderPage(output.size, "novel-text:${output.size}", null, novelBlock = block)
            }
            buffer.clear()
        }
        fun visit(node: Node) {
            if (node is TextNode) { buffer.append(node.outerHtml()); return }
            if (node !is Element) return
            when (node.normalName()) {
                "img" -> {
                    flush()
                    val url = node.absUrl("src")
                    if (url.startsWith("https://") || url.startsWith("http://")) output += ReaderPage(output.size, url, null, headers, sourcePageUrl = baseUrl)
                }
                "br" -> { flush() }
                "p", "div", "section", "article", "blockquote", "li", "ul", "ol", "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    flush()
                    node.childNodes().forEach(::visit)
                    flush(node.normalName().removePrefix("h").toIntOrNull() ?: 0)
                }
                else -> { buffer.append('<').append(node.normalName()).append('>'); node.childNodes().forEach(::visit); buffer.append("</").append(node.normalName()).append('>') }
            }
        }
        clean.body().childNodes().forEach(::visit)
        flush()
        check(output.any { it.novelBlock?.text?.isNotBlank() == true }) { "The source returned an empty text chapter" }
        output += ReaderPage(output.size, "novel-text:end", null, novelBlock = NovelBlock(endOfChapter = true))
        return output
    }
    fun encodePages(pages: List<ReaderPage>): ByteArray = json.encodeToString(pages).toByteArray()
    fun decodePages(bytes: ByteArray): List<ReaderPage> = json.decodeFromString(bytes.decodeToString())
    fun encodeBlock(block: NovelBlock): ByteArray = json.encodeToString(block).toByteArray()
    fun decodeBlock(bytes: ByteArray): NovelBlock = json.decodeFromString(bytes.decodeToString())
    fun isText(page: ReaderPage): Boolean = page.imageUrl.startsWith("novel-text:")
}
