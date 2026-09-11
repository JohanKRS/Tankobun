package com.tankobun.core.extensions.novel

import org.junit.Assert.*
import org.junit.Test

class NovelDocumentTest {
    @Test fun sanitizesScriptsAndPreservesParagraphsIllustrationsAndEmphasis() {
        val pages = NovelDocument.parse("""<h2>A fictional morning</h2><div>First <b>paragraph</b>.</div><p>Second.</p><script>alert(1)</script><img src="/illustration.png" onerror="alert(2)"><iframe src="https://example.invalid"></iframe>""", "https://example.invalid/chapter")
        assertEquals(5, pages.size)
        assertEquals(2, pages[0].novelBlock!!.headingLevel)
        assertTrue(pages[1].novelBlock!!.html.contains("<b>"))
        assertEquals("https://example.invalid/illustration.png", pages[3].imageUrl)
        assertTrue(pages.last().novelBlock!!.endOfChapter)
        assertFalse(pages.any { it.novelBlock?.html?.contains("alert") == true })
    }
    @Test fun cachedDocumentAndOfflineBlocksRoundTripWithoutLayoutDependence() {
        val pages = NovelDocument.parse("<p>The observatory opened.</p><p>Its paper stars moved slowly.</p>", "https://example.invalid/chapter")
        assertEquals(pages, NovelDocument.decodePages(NovelDocument.encodePages(pages)))
        pages.mapNotNull { it.novelBlock }.forEach { assertEquals(it, NovelDocument.decodeBlock(NovelDocument.encodeBlock(it))) }
        assertEquals(listOf(0, 1, 2), pages.map { it.index })
    }
    @Test fun emptyOrScriptOnlyContentNeverMarksAChapterCompleted() {
        assertThrows(IllegalStateException::class.java) { NovelDocument.parse("<script>document.write('text')</script>", "https://example.invalid") }
    }
    @Test fun pluginIdentityIncludesRepositoryAndIsStableAcrossVersions() {
        val a = LnReaderPlugin("fixture", "Paper", "https://example.invalid", "English", "1.0.1", "https://example.invalid/p.js", repositoryUrl = "https://example.invalid/plugins.json")
        assertEquals(a.sourceId, a.copy(version = "2.0.0", name = "Renamed").sourceId)
        assertNotEquals(a.sourceId, a.copy(repositoryUrl = "https://another.invalid/plugins.json").sourceId)
        assertEquals("en", a.language)
    }
}
