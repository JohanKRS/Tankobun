package com.tankobun.app.home

import com.tankobun.core.model.withCoverFallback
import org.junit.Assert.*
import org.junit.Test

class HomeArtworkTest {
    @Test fun searchThumbnailsCannotReplaceTheCachedLargerVersionOfTheSameCover() {
        val thumbnail = "https://cdn.mangabaka.dev/imgproxy/plain/x350@1/asset"
        val large = "https://cdn.mangabaka.dev/imgproxy/plain/x350@3/asset"
        assertEquals(large, thumbnail.withCoverFallback(large))
        assertEquals(large, large.withCoverFallback(thumbnail))
        assertEquals(thumbnail, thumbnail.withCoverFallback("https://cdn.mangabaka.dev/imgproxy/plain/x350@3/different-asset"))
        assertEquals("https://anilist.test/new.jpg", "https://anilist.test/new.jpg".withCoverFallback(large))
    }

    @Test fun successfulChecksIncludingNoBannerAreCachedForAWeek() {
        val week = 7 * 24 * 60 * 60_000L
        assertFalse(homeArtworkNeedsRefresh(100, null, week, week))
        assertTrue(homeArtworkNeedsRefresh(100, null, week + 100, week))
    }

    @Test fun failedRequestsBackOffButDoNotWaitAWeekToRetry() {
        val week = 7 * 24 * 60 * 60_000L
        assertTrue(homeArtworkNeedsRefresh(null, null, 100, week))
        assertFalse(homeArtworkNeedsRefresh(null, 100, HOME_ARTWORK_RETRY_MILLIS, week))
        assertTrue(homeArtworkNeedsRefresh(null, 100, HOME_ARTWORK_RETRY_MILLIS + 100, week))
    }
}
