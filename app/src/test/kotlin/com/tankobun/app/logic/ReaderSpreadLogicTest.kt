package com.tankobun.app.logic

import com.tankobun.core.model.ReaderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSpreadLogicTest {
    private val unknownSizes: (Int) -> Float? = { null }

    @Test
    fun singlePagesWhenPairingIsOff() {
        assertEquals(
            listOf(spread(0), spread(1), spread(2)),
            readerPageSpreads(pageCount = 3, pairPages = false) { 0.7f },
        )
    }

    @Test
    fun noPagesMakeNoSpreads() {
        assertEquals(emptyList<ReaderSpread>(), readerPageSpreads(pageCount = 0, pairPages = true, aspectRatioAt = unknownSizes))
    }

    @Test
    fun coverStandsAloneAndTheRestPairInOrder() {
        assertEquals(
            listOf(spread(0), spread(1, 2), spread(3, 4)),
            readerPageSpreads(pageCount = 5, pairPages = true, aspectRatioAt = unknownSizes),
        )
    }

    @Test
    fun trailingPageWithoutPartnerStandsAlone() {
        assertEquals(
            listOf(spread(0), spread(1, 2), spread(3)),
            readerPageSpreads(pageCount = 4, pairPages = true) { 0.7f },
        )
        assertEquals(listOf(spread(0)), readerPageSpreads(pageCount = 1, pairPages = true, aspectRatioAt = unknownSizes))
        assertEquals(listOf(spread(0), spread(1)), readerPageSpreads(pageCount = 2, pairPages = true, aspectRatioAt = unknownSizes))
    }

    @Test
    fun widePagesStandAloneAndShiftTheFollowingPairs() {
        val ratios = mapOf(3 to 1.4f)

        assertEquals(
            listOf(spread(0), spread(1, 2), spread(3), spread(4, 5), spread(6)),
            readerPageSpreads(pageCount = 7, pairPages = true) { ratios[it] ?: 0.7f },
        )
    }

    @Test
    fun widePageNeverJoinsThePageBeforeIt() {
        val ratios = mapOf(2 to 1.6f)

        assertEquals(
            listOf(spread(0), spread(1), spread(2), spread(3, 4)),
            readerPageSpreads(pageCount = 5, pairPages = true) { ratios[it] },
        )
    }

    @Test
    fun squarePagesStillPair() {
        assertEquals(
            listOf(spread(0), spread(1, 2)),
            readerPageSpreads(pageCount = 3, pairPages = true) { 1f },
        )
    }

    @Test
    fun everyPageBelongsToExactlyOneSpread() {
        val ratios = mapOf(4 to 2f, 5 to 1.2f, 9 to 1.5f)
        val spreads = readerPageSpreads(pageCount = 12, pairPages = true) { ratios[it] }

        assertEquals((0 until 12).toList(), spreads.flatMap { it.first..it.last })
    }

    @Test
    fun findsTheSpreadShowingAnyPage() {
        val spreads = readerPageSpreads(pageCount = 6, pairPages = true, aspectRatioAt = unknownSizes)

        assertEquals(0, spreads.indexOfSpreadContaining(0))
        assertEquals(1, spreads.indexOfSpreadContaining(1))
        assertEquals(1, spreads.indexOfSpreadContaining(2))
        assertEquals(2, spreads.indexOfSpreadContaining(4))
        assertEquals(3, spreads.indexOfSpreadContaining(5))
    }

    @Test
    fun pagesOutsideTheChapterClampToTheEnds() {
        val spreads = readerPageSpreads(pageCount = 4, pairPages = true, aspectRatioAt = unknownSizes)

        assertEquals(0, spreads.indexOfSpreadContaining(-3))
        assertEquals(spreads.lastIndex, spreads.indexOfSpreadContaining(40))
        assertEquals(-1, emptyList<ReaderSpread>().indexOfSpreadContaining(0))
    }

    @Test
    fun rightToLeftPutsTheEarlierPageOnTheRight() {
        assertEquals(listOf(3, 4), spread(3, 4).screenOrder(rightToLeft = false))
        assertEquals(listOf(4, 3), spread(3, 4).screenOrder(rightToLeft = true))
        assertEquals(listOf(0), spread(0).screenOrder(rightToLeft = true))
    }

    @Test
    fun spreadAspectRatioAddsPageWidthsAtTheSameHeight() {
        val ratios = mapOf(1 to 0.7f, 2 to 0.65f)

        assertEquals(1.35f, spread(1, 2).combinedAspectRatio { ratios[it] }!!, 0.0001f)
        assertEquals(0.7f, spread(1).combinedAspectRatio { ratios[it] }!!, 0.0001f)
        assertNull(spread(2, 3).combinedAspectRatio { ratios[it] })
    }

    @Test
    fun spreadsOnlyShowInLandscapePagedReading() {
        assertTrue(readerShowsSpreads(enabled = true, readerMode = ReaderMode.PAGED, windowWidth = 1280, windowHeight = 800))
        assertFalse(readerShowsSpreads(enabled = true, readerMode = ReaderMode.PAGED, windowWidth = 800, windowHeight = 1280))
        assertFalse(readerShowsSpreads(enabled = true, readerMode = ReaderMode.PAGED, windowWidth = 800, windowHeight = 800))
        assertFalse(readerShowsSpreads(enabled = true, readerMode = ReaderMode.WEBTOON, windowWidth = 1280, windowHeight = 800))
        assertFalse(readerShowsSpreads(enabled = false, readerMode = ReaderMode.PAGED, windowWidth = 1280, windowHeight = 800))
    }

    @Test
    fun doubleSpreadsKnowTheirPages() {
        assertTrue(spread(1, 2).isDouble)
        assertFalse(spread(0).isDouble)
        assertTrue(2 in spread(1, 2))
        assertFalse(3 in spread(1, 2))
    }

    private fun spread(first: Int, last: Int = first) = ReaderSpread(first, last)
}
