package com.tankobun.app.logic

import com.tankobun.core.model.AnilistScoreFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackingScoreLogicTest {
    @Test
    fun filteredScoreInputAllowsSingleDecimalOnlyForDecimalFormat() {
        assertEquals("10.5", "10.5.9x".filteredScoreInput(AnilistScoreFormat.POINT_10_DECIMAL))
        assertEquals("105", "10.5.9x".filteredScoreInput(AnilistScoreFormat.POINT_10))
    }

    @Test
    fun toAniListScoreClampsAndRoundsForConfiguredFormat() {
        assertEquals(100.0, "120".toAniListScore(AnilistScoreFormat.POINT_100)!!, 0.0)
        assertEquals(7.4, "7.36".toAniListScore(AnilistScoreFormat.POINT_10_DECIMAL)!!, 0.0)
        assertEquals(5.0, "9".toAniListScore(AnilistScoreFormat.POINT_5)!!, 0.0)
        assertNull("not a score".toAniListScore(AnilistScoreFormat.POINT_100))
    }

    @Test
    fun formatTrackingScoreMatchesScoreFormat() {
        assertEquals("8.5", 8.46.formatTrackingScore(AnilistScoreFormat.POINT_10_DECIMAL))
        assertEquals("85", 85.2.formatTrackingScore(AnilistScoreFormat.POINT_100))
        assertEquals("", null.formatTrackingScore(AnilistScoreFormat.POINT_100))
    }
}
