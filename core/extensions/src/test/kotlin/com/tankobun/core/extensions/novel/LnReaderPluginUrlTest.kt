package com.tankobun.core.extensions.novel

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.net.URI

class LnReaderPluginUrlTest {
    private val repositoryUrl = "https://example.invalid/plugins/.dist/index.json"

    @Test fun indexAcceptsLiteralBracketsAndSpacesWithoutDroppingPlugins() {
        val payload = JSONArray()
            .put(plugin("paper", "https://example.invalid/scripts/PaperNovel[engine].js"))
            .put(plugin("words", "../scripts/Paper Words[engine].js", "../icons/Paper Words[1].png"))
        val entries = requireNotNull(parseLnReaderIndex(payload.toString(), repositoryUrl))
        assertEquals(2, entries.size)
        assertEquals("https://example.invalid/scripts/PaperNovel%5Bengine%5D.js", entries[0].apkName)
        assertEquals("https://example.invalid/plugins/scripts/Paper%20Words%5Bengine%5D.js", entries[1].apkName)
        assertEquals("https://example.invalid/plugins/icons/Paper%20Words%5B1%5D.png", entries[1].iconUrl)
        entries.forEach { assertNotNull(URI(it.apkName).host) }
    }

    @Test fun encodedPathsQueriesAndPluginIdentityRemainStable() {
        val original = "https://example.invalid/scripts/Paper%20Words%5Bengine%5D.js?part=a%2Fb&v=1"
        val entry = requireNotNull(parseLnReaderIndex(JSONArray().put(plugin("paper", original)).toString(), repositoryUrl)).single()
        assertEquals(original, entry.apkName)
        val expected = LnReaderPlugin("paper", "Paper Words", "https://example.invalid", "English", "1.0.0", original,
            repositoryUrl = repositoryUrl)
        assertEquals(expected.packageName, entry.packageName)
        assertEquals(expected.sourceId, entry.lnReaderPlugin!!.sourceId)
        assertEquals(repositoryUrl, entry.repositoryUrl)
    }

    @Test fun chapterAssetsHandleUnicodeAndExistingEscapesWithoutDoubleEncoding() {
        assertEquals("https://example.invalid/plugins/css/Caf%C3%A9%20%5Bnight%5D.css",
            resolveLnReaderUrl(repositoryUrl, "../css/Café [night].css"))
        assertEquals("https://example.invalid/js/Paper%5Bengine%5D.js?key=a%2Fb",
            resolveLnReaderUrl(repositoryUrl, "//example.invalid/js/Paper%5Bengine%5D.js?key=a%2Fb"))
        assertEquals("https://example.invalid/assets/Paper%5Bengine%5D.js?v=1#chapter",
            resolveLnReaderUrl(repositoryUrl, "/assets/Paper[engine].js?v=1#chapter"))
    }

    private fun plugin(id: String, url: String, iconUrl: String? = null) = JSONObject()
        .put("id", id).put("name", "Paper Words").put("site", "https://example.invalid")
        .put("lang", "English").put("version", "1.0.0").put("url", url).apply {
            if (iconUrl != null) put("iconUrl", iconUrl)
        }
}
