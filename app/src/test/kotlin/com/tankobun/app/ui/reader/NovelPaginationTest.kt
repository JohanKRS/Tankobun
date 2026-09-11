package com.tankobun.app.ui.reader

import org.junit.Assert.*
import org.junit.Test

class NovelPaginationTest {
    private fun paragraph(index: Int, count: Int, height: Float = 20f) = NovelMeasuredParagraph(index,
        List(count) { line -> NovelTextLine(line * 10, (line + 1) * 10, height) })

    @Test fun twoPagesRequireLandscapeRoomAndRespectThePreference() {
        assertEquals(2, novelColumnCount(true, 960f, 600f))
        assertEquals(1, novelColumnCount(true, 600f, 960f))
        assertEquals(1, novelColumnCount(true, 560f, 360f))
        assertEquals(1, novelColumnCount(false, 960f, 600f))
        val pages = paginateNovel(listOf(paragraph(0, 17)), 100f, 12f)
        val spreadStarts = pages.chunked(2).map { it.first() }
        // An anchor in the right-hand page stays in the same spread after reflow.
        assertEquals(0, spreadStarts.pageForAnchor(0, 69))
        assertEquals(1, spreadStarts.pageForAnchor(0, 169))
    }

    @Test fun longParagraphTurnsIntoScreenPagesWithoutMissingOrRepeatingCharacters() {
        val pages = paginateNovel(listOf(paragraph(0, 17)), 100f, 12f)
        assertEquals(listOf(5, 5, 5, 2), pages.map { it.parts.single().lineCount })
        assertEquals(listOf(0, 50, 100, 150), pages.map { it.character })
        assertEquals(170, pages.sumOf { page -> page.parts.sumOf { it.end - it.start } })
        assertEquals(2, pages.pageForAnchor(0, 109))
    }

    @Test fun spacingOnlyOccursBetweenParagraphsOnTheSamePage() {
        val pages = paginateNovel(listOf(paragraph(0, 2), paragraph(1, 2), paragraph(2, 1)), 100f, 12f)
        assertEquals(2, pages.size)
        assertEquals(listOf(0f, 12f), pages[0].parts.map { it.gapBefore })
        assertEquals(0f, pages[1].parts.single().gapBefore)
        assertEquals(0, pages.pageForAnchor(1, 10))
        assertEquals(1, pages.pageForAnchor(2, 0))
    }

    @Test fun illustrationsAndEndMarkersRemainSeparateAndNeverDisappear() {
        val pages = paginateNovel(listOf(paragraph(0, 2), NovelMeasuredParagraph(1, emptyList()), paragraph(2, 2), NovelMeasuredParagraph(3, emptyList())), 100f, 8f)
        assertEquals(listOf(0, 1, 2, 3), pages.map { it.blockIndex })
    }

    @Test fun smallViewportStillMakesProgressAndReflowResolvesTheSameAnchor() {
        val paragraphs = listOf(paragraph(0, 30, 28f))
        val small = paginateNovel(paragraphs, 10f, 8f)
        val large = paginateNovel(paragraphs, 150f, 8f)
        assertEquals(30, small.size)
        val anchor = 177
        val target = large[large.pageForAnchor(0, anchor)].parts.single()
        assertTrue(anchor in target.start until target.end)
        assertEquals(300, large.sumOf { it.parts.sumOf { part -> part.end - part.start } })
    }
}
