package com.tankobun.app.logic

import com.tankobun.app.state.LibraryItem
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.MediaStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MainStateLogicTest {
    @Test
    fun selectedAniListDetailsUpdateLibraryAndTrackingFormWhenClean() {
        val media = media(42, "Updated")
        val entry = entry(
            mediaId = 42,
            status = MediaStatus.CURRENT,
            progress = 12,
            score = 87.0,
            notes = "nearly caught up",
            private = true,
            customLists = listOf("Favorites"),
        )
        val recommendation = AnilistRecommendation(media = media(9, "Neighbor"), rating = 3)

        val next = TankobunUiState(
            selectedMedia = media(42, "Initial"),
            libraryItems = listOf(LibraryItem(media(1, "Other"), entry(mediaId = 1))),
            anilistScoreFormat = AnilistScoreFormat.POINT_100,
        ).withSelectedAniListDetails(
            mediaId = 42,
            media = media,
            entry = entry,
            recommendations = listOf(recommendation),
            recommendationsPage = 1,
            recommendationsHasMore = true,
        )

        assertEquals(media, next.selectedMedia)
        assertEquals(entry, next.selectedListEntry)
        assertEquals(listOf(recommendation), next.selectedRecommendations)
        assertEquals(1, next.selectedRecommendationsPage)
        assertTrue(next.selectedRecommendationsHasMore)
        assertFalse(next.recommendationsLoading)
        assertEquals(listOf(1, 42), next.libraryItems.map { it.media.id })
        assertEquals(MediaStatus.CURRENT, next.trackingStatus)
        assertEquals("12", next.trackingProgress)
        assertEquals("87", next.trackingScore)
        assertEquals("nearly caught up", next.trackingNotes)
        assertTrue(next.trackingPrivate)
        assertEquals(setOf("Favorites"), next.trackingCustomLists)
    }

    @Test
    fun selectedAniListDetailsPreserveDirtyTrackingForm() {
        val originalEntry = entry(mediaId = 42, progress = 2)
        val fetchedEntry = entry(mediaId = 42, progress = 50, notes = "server note")
        val state = TankobunUiState(
            selectedMedia = media(42, "Manga"),
            selectedListEntry = originalEntry,
            trackingProgress = "7",
            trackingScore = "55",
            trackingNotes = "local draft",
            trackingPrivate = true,
            trackingCustomLists = setOf("Draft"),
            trackingDirty = true,
        )

        val next = state.withSelectedAniListDetails(
            mediaId = 42,
            media = media(42, "Manga"),
            entry = fetchedEntry,
            recommendations = emptyList(),
            recommendationsPage = 0,
            recommendationsHasMore = false,
        )

        assertEquals(fetchedEntry, next.selectedListEntry)
        assertEquals("7", next.trackingProgress)
        assertEquals("55", next.trackingScore)
        assertEquals("local draft", next.trackingNotes)
        assertTrue(next.trackingPrivate)
        assertEquals(setOf("Draft"), next.trackingCustomLists)
        assertTrue(next.trackingDirty)
    }

    @Test
    fun selectedAniListDetailsIgnoreStaleSelection() {
        val state = TankobunUiState(selectedMedia = media(42, "Manga"))

        val next = state.withSelectedAniListDetails(
            mediaId = 7,
            media = media(7, "Different"),
            entry = entry(mediaId = 7),
            recommendations = emptyList(),
            recommendationsPage = 0,
            recommendationsHasMore = false,
        )

        assertSame(state, next)
    }

    @Test
    fun syncedListEntryMergesLibraryAndKeepsHigherLocalProgress() {
        val media = media(42, "Manga")
        val entry = entry(mediaId = 42, progress = 4)

        val next = TankobunUiState(
            selectedMedia = media,
            trackingProgress = "8",
        ).withSyncedListEntry(
            media = media,
            entry = entry,
            updateTrackingForm = false,
        )

        assertEquals(listOf(42), next.libraryItems.map { it.media.id })
        assertEquals(entry, next.selectedListEntry)
        assertEquals("8", next.trackingProgress)
        assertFalse(next.trackingSaveInProgress)
    }

    @Test
    fun syncedListEntryCanRefreshTrackingForm() {
        val media = media(42, "Manga")
        val entry = entry(
            mediaId = 42,
            status = MediaStatus.COMPLETED,
            progress = 22,
            score = 90.0,
            notes = "done",
            private = true,
            customLists = listOf("Favorites"),
        )

        val next = TankobunUiState(
            selectedMedia = media,
            trackingDirty = true,
            trackingSaveInProgress = true,
            trackingSaveFailed = true,
            anilistScoreFormat = AnilistScoreFormat.POINT_100,
        ).withSyncedListEntry(
            media = media,
            entry = entry,
            updateTrackingForm = true,
        )

        assertEquals(MediaStatus.COMPLETED, next.trackingStatus)
        assertEquals("22", next.trackingProgress)
        assertEquals("90", next.trackingScore)
        assertEquals("done", next.trackingNotes)
        assertTrue(next.trackingPrivate)
        assertEquals(setOf("Favorites"), next.trackingCustomLists)
        assertFalse(next.trackingDirty)
        assertFalse(next.trackingSaveInProgress)
        assertFalse(next.trackingSaveFailed)
    }

    private fun media(id: Int, title: String): AnilistMedia =
        AnilistMedia(
            id = id,
            idMal = null,
            title = AnilistTitle(
                romaji = title,
                english = null,
                native = null,
                userPreferred = title,
            ),
            description = null,
            coverImage = null,
            bannerImage = null,
            chapters = null,
            volumes = null,
            format = null,
            status = null,
            averageScore = null,
            popularity = null,
            startDateYear = null,
            endDateYear = null,
            siteUrl = null,
            genres = emptyList(),
            synonyms = emptyList(),
            isAdult = false,
            updatedAtEpochSeconds = null,
        )

    private fun entry(
        mediaId: Int,
        status: MediaStatus = MediaStatus.PLANNING,
        progress: Int = 0,
        score: Double? = null,
        notes: String? = null,
        private: Boolean = false,
        customLists: List<String> = emptyList(),
    ): AnilistListEntry =
        AnilistListEntry(
            id = mediaId * 10,
            mediaId = mediaId,
            status = status,
            progress = progress,
            score = score,
            notes = notes,
            private = private,
            customLists = customLists,
            updatedAtEpochSeconds = null,
        )
}
