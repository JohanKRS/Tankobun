package com.tankobun.app.ui.reader

import androidx.compose.ui.unit.Velocity
import com.tankobun.app.state.ReaderChapterSegment
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceChapter
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderMotionUiTest {
    @Test
    fun boundedReaderFlingVelocityCapsBadTrackerSpikes() {
        val velocity = Velocity(x = 120_000f, y = -85_000f).boundedReaderFlingVelocity()

        assertEquals(READER_MAX_FLING_VELOCITY_PX_PER_SECOND, velocity.x, 0.01f)
        assertEquals(-READER_MAX_FLING_VELOCITY_PX_PER_SECOND, velocity.y, 0.01f)
    }

    @Test
    fun boundedReaderFlingVelocityDropsNonFiniteValues() {
        val velocity = Velocity(x = Float.POSITIVE_INFINITY, y = Float.NaN).boundedReaderFlingVelocity()

        assertEquals(0f, velocity.x, 0.01f)
        assertEquals(0f, velocity.y, 0.01f)
    }

    @Test
    fun zoomedWebtoonScrollVelocityCapsAndScalesVerticalMomentum() {
        val velocity = zoomedWebtoonScrollVelocity(velocityY = -120_000f, scale = 3f)

        assertEquals(READER_MAX_FLING_VELOCITY_PX_PER_SECOND / 3f, velocity, 0.01f)
    }

    @Test
    fun webtoonReaderPageItemsKeepsPreviousCurrentNextOrder() {
        val previousChapter = chapter("previous")
        val currentChapter = chapter("current")
        val nextChapter = chapter("next")

        val items = webtoonReaderPageItems(
            previousSegment = ReaderChapterSegment(previousChapter, listOf(page(0), page(1))),
            chapter = currentChapter,
            pages = listOf(page(0)),
            nextSegment = ReaderChapterSegment(nextChapter, listOf(page(0))),
        )

        assertEquals(listOf("previous", "previous", "current", "next"), items.map { it.chapter.url })
        assertEquals(listOf(0, 1, 0, 0), items.map { it.pageIndex })
    }

    private fun chapter(url: String): SourceChapter =
        SourceChapter(
            sourceId = 1L,
            mangaUrl = "manga",
            url = url,
            name = url,
            chapterNumber = 1f,
            scanlator = null,
            uploadedAtEpochMillis = null,
        )

    private fun page(index: Int): ReaderPage =
        ReaderPage(index = index, imageUrl = "https://example.test/$index.jpg", cachedFilePath = null)
}
