package com.tankobun.app.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class TagRowLogicTest {
    private fun List<List<TagSlot>>.indices() = map { row -> row.map { it.index } }
    private fun List<TagSlot>.span(spacing: Int) = sumOf { it.width } + spacing * (size - 1)

    @Test
    fun laterTagFillsTheGapLeftByOneThatDoesNotFit() {
        // 60 + 30 fit in 100; 50 does not, so the 10-wide tag after it moves up.
        val rows = justifiedTagRows(listOf(60, 30, 50, 10), maxWidth = 100, spacing = 0)
        assertEquals(listOf(listOf(0, 1, 3), listOf(2)), rows.indices())
    }

    @Test
    fun fullRowsReachBothEdgesAndShareTheExtraEqually() {
        val rows = justifiedTagRows(listOf(30, 30, 60), maxWidth = 100, spacing = 8)
        assertEquals(100, rows.first().span(8))
        assertEquals(listOf(46, 46), rows.first().map { it.width })
    }

    @Test
    fun lastRowStaysNaturalUnlessAlmostFull() {
        val sparse = justifiedTagRows(listOf(90, 20), maxWidth = 100, spacing = 0)
        assertEquals(listOf(20), sparse.last().map { it.width })
        val nearlyFull = justifiedTagRows(listOf(90, 80), maxWidth = 100, spacing = 0)
        assertEquals(listOf(100), nearlyFull.last().map { it.width })
    }

    @Test
    fun overlongTagsAreClampedToTheRow() {
        val rows = justifiedTagRows(listOf(500, 20), maxWidth = 100, spacing = 0)
        assertEquals(listOf(listOf(100), listOf(20)), rows.map { row -> row.map { it.width } })
    }
}
