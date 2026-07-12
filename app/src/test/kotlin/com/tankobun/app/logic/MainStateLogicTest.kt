package com.tankobun.app.logic

import com.tankobun.app.LibraryMode
import com.tankobun.app.state.LibraryItem
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceManga
import com.tankobun.core.model.SourceSearchResult
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
    fun refreshedTrackingEntryReplacesStaleScoreAndMetadata() {
        val media = media(42, "Manga")
        val staleEntry = entry(mediaId = 42, progress = 3, score = null)
        val refreshedEntry = entry(
            mediaId = 42,
            status = MediaStatus.COMPLETED,
            progress = 24,
            score = 92.0,
            notes = "updated on AniList",
            private = true,
            customLists = listOf("Favorites"),
        )

        val next = TankobunUiState(
            selectedMedia = media,
            selectedListEntry = staleEntry,
            libraryItems = listOf(LibraryItem(media, staleEntry)),
            trackingProgress = "3",
            anilistScoreFormat = AnilistScoreFormat.POINT_100,
        ).withRefreshedTrackingEntry(mediaId = 42, entry = refreshedEntry)

        assertEquals(refreshedEntry, next.selectedListEntry)
        assertEquals(refreshedEntry, next.libraryItems.single().entry)
        assertEquals(MediaStatus.COMPLETED, next.trackingStatus)
        assertEquals("24", next.trackingProgress)
        assertEquals("92", next.trackingScore)
        assertEquals("updated on AniList", next.trackingNotes)
        assertTrue(next.trackingPrivate)
        assertEquals(setOf("Favorites"), next.trackingCustomLists)
    }

    @Test
    fun refreshedTrackingEntryDoesNotOverwriteUnsavedEdits() {
        val media = media(42, "Manga")
        val refreshedEntry = entry(mediaId = 42, progress = 24, score = 92.0)

        val next = TankobunUiState(
            selectedMedia = media,
            trackingProgress = "8",
            trackingScore = "75",
            trackingNotes = "draft",
            trackingDirty = true,
        ).withRefreshedTrackingEntry(mediaId = 42, entry = refreshedEntry)

        assertEquals(refreshedEntry, next.selectedListEntry)
        assertEquals("8", next.trackingProgress)
        assertEquals("75", next.trackingScore)
        assertEquals("draft", next.trackingNotes)
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
    fun selectedAniListDetailsDoNotDowngradeEnrichedAuthorAndHeroImage() {
        val enriched = media(42, "Initial").copy(
            staff = listOf("Creator Name"),
            mainCharacterImage = "https://example.com/character.jpg",
        )

        val next = TankobunUiState(selectedMedia = enriched).withSelectedAniListDetails(
            mediaId = 42,
            media = media(42, "Updated"),
            entry = null,
            recommendations = emptyList(),
            recommendationsPage = 0,
            recommendationsHasMore = false,
        )

        assertEquals("Updated", next.selectedMedia?.title?.userPreferred)
        assertEquals(listOf("Creator Name"), next.selectedMedia?.staff)
        assertEquals("https://example.com/character.jpg", next.selectedMedia?.mainCharacterImage)
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

    @Test
    fun renamedAniListCustomListUpdatesLibrarySelectionAndTrackingForm() {
        val selectedEntry = entry(mediaId = 42, customLists = listOf("Favorites", "Keep"))
        val serverEntry = selectedEntry.copy(customLists = listOf("Best", "Keep"))
        val otherEntry = entry(mediaId = 7, customLists = listOf("Favorites"))

        val next = TankobunUiState(
            selectedMedia = media(42, "Selected"),
            selectedListEntry = selectedEntry,
            libraryItems = listOf(
                LibraryItem(media(42, "Selected"), selectedEntry),
                LibraryItem(media(7, "Other"), otherEntry),
            ),
            trackingCustomLists = setOf("Favorites", "Draft"),
            busy = true,
        ).withRenamedAniListCustomList(
            customLists = listOf("Best"),
            updatedEntries = mapOf(42 to serverEntry),
            oldName = "Favorites",
            newName = "Best",
            successMessage = "Custom list renamed",
        )

        assertEquals(listOf("Best"), next.anilistCustomLists)
        assertEquals(serverEntry, next.selectedListEntry)
        assertEquals(listOf("Best"), next.libraryItems.first { it.media.id == 7 }.entry.customLists)
        assertEquals(setOf("Best", "Draft"), next.trackingCustomLists)
        assertFalse(next.busy)
        assertEquals("Custom list renamed", next.message)
    }

    @Test
    fun trackingSaveResultPreservesEditsMadeWhileSaveWasInFlight() {
        val media = media(42, "Manga")
        val serverEntry = entry(
            mediaId = 42,
            status = MediaStatus.COMPLETED,
            progress = 12,
            score = 90.0,
            notes = "server",
            private = true,
            customLists = listOf("Server"),
        )

        val next = TankobunUiState(
            selectedMedia = media,
            trackingProgress = "17",
            trackingScore = "55",
            trackingNotes = "local draft",
            trackingPrivate = false,
            trackingCustomLists = setOf("Draft"),
            trackingDirty = true,
            trackingSaveInProgress = true,
            busy = true,
            anilistScoreFormat = AnilistScoreFormat.POINT_100,
        ).withTrackingSaveResult(
            media = media,
            entry = serverEntry,
            knownCustomLists = listOf("Server"),
            autoSave = false,
            successMessage = "AniList tracking saved",
        )

        assertEquals(serverEntry, next.selectedListEntry)
        assertEquals("17", next.trackingProgress)
        assertEquals("55", next.trackingScore)
        assertEquals("local draft", next.trackingNotes)
        assertFalse(next.trackingPrivate)
        assertEquals(setOf("Draft"), next.trackingCustomLists)
        assertTrue(next.trackingDirty)
        assertFalse(next.trackingSaveInProgress)
        assertFalse(next.trackingSaveFailed)
        assertFalse(next.busy)
        assertEquals("AniList tracking saved", next.message)
    }

    @Test
    fun localTrackingSaveResultUpdatesLibraryAndClearsDirtyState() {
        val media = media(42, "Manga")
        val localEntry = entry(
            mediaId = 42,
            status = MediaStatus.CURRENT,
            progress = 12,
            score = 87.0,
            notes = "local note",
            private = true,
            customLists = listOf("Favorites"),
        ).copy(id = -42)

        val next = TankobunUiState(
            libraryMode = LibraryMode.LOCAL,
            loggedIn = false,
            selectedMedia = media,
            trackingDirty = false,
            trackingSaveInProgress = true,
            busy = true,
            anilistScoreFormat = AnilistScoreFormat.POINT_100,
        ).withTrackingSaveResult(
            media = media,
            entry = localEntry,
            knownCustomLists = listOf("Favorites"),
            autoSave = false,
            successMessage = "Tracking saved",
        )

        assertEquals(localEntry, next.selectedListEntry)
        assertEquals(listOf(42), next.libraryItems.map { it.media.id })
        assertEquals(listOf("Favorites"), next.anilistCustomLists)
        assertFalse(next.trackingDirty)
        assertFalse(next.trackingSaveInProgress)
        assertFalse(next.trackingSaveFailed)
        assertFalse(next.busy)
        assertEquals("Tracking saved", next.message)
    }

    @Test
    fun addedTrackingCustomListDoesNotMarkListAsKnownBeforeServerSave() {
        val entry = entry(mediaId = 42, customLists = emptyList())

        val next = TankobunUiState(
            selectedListEntry = entry,
            anilistCustomLists = listOf("Existing"),
        ).withAddedTrackingCustomList("New List")

        assertEquals(listOf("Existing"), next.anilistCustomLists)
        assertEquals(setOf("New List"), next.trackingCustomLists)
        assertTrue(next.trackingDirty)
    }

    @Test
    fun selectedMediaResetsDetailSourceReaderAndDownloadState() {
        val media = media(42, "Manga")
        val entry = entry(
            mediaId = 42,
            status = MediaStatus.CURRENT,
            progress = 9,
            score = 80.0,
            notes = "note",
            private = true,
            customLists = listOf("Favorites"),
        )

        val next = TankobunUiState(
            sourceMatches = listOf(match(source())),
            sourcePickerOpen = true,
            selectedRecommendations = listOf(AnilistRecommendation(media(7, "Other"), rating = 1)),
            trackingDirty = true,
            selectedSourceManga = manga(source(), "old"),
            sourceChapters = listOf(chapter(source(), "old")),
            activeChapter = chapter(source(), "active"),
            readerPages = listOf(ReaderPage(0, "https://example.test/0.jpg", null)),
            selectingDownloadChapters = true,
            selectedDownloadChapterUrls = setOf("old"),
            message = "old message",
            anilistScoreFormat = AnilistScoreFormat.POINT_100,
        ).withSelectedMedia(media, entry)

        assertEquals(media, next.selectedMedia)
        assertEquals(entry, next.selectedListEntry)
        assertEquals(MediaStatus.CURRENT, next.trackingStatus)
        assertEquals("9", next.trackingProgress)
        assertEquals("80", next.trackingScore)
        assertEquals("note", next.trackingNotes)
        assertTrue(next.trackingPrivate)
        assertEquals(setOf("Favorites"), next.trackingCustomLists)
        assertTrue(next.sourceMatches.isEmpty())
        assertFalse(next.sourcePickerOpen)
        assertTrue(next.sourceChapters.isEmpty())
        assertEquals(null, next.activeChapter)
        assertTrue(next.readerPages.isEmpty())
        assertFalse(next.selectingDownloadChapters)
        assertTrue(next.selectedDownloadChapterUrls.isEmpty())
        assertEquals(null, next.message)
    }

    @Test
    fun selectedSourceKeepsChaptersWhenSourceIsUnchanged() {
        val source = source(id = 3)
        val manga = manga(source, "manga")
        val chapters = listOf(chapter(source, "chapter"))

        val next = TankobunUiState(
            selectedSourceId = source.id,
            selectedSourceManga = manga,
            sourceChapters = chapters,
            chapterProgress = mapOf("chapter" to progress()),
            activeChapter = chapters.first(),
            readerPages = listOf(ReaderPage(0, "https://example.test/0.jpg", null)),
            selectingDownloadChapters = true,
            selectedDownloadChapterUrls = setOf("chapter"),
            message = "keep me",
        ).withSelectedSource(source.id)

        assertEquals(manga, next.selectedSourceManga)
        assertEquals(chapters, next.sourceChapters)
        assertEquals(setOf("chapter"), next.chapterProgress.keys)
        assertEquals(null, next.activeChapter)
        assertTrue(next.readerPages.isEmpty())
        assertFalse(next.selectingDownloadChapters)
        assertTrue(next.selectedDownloadChapterUrls.isEmpty())
        assertEquals("keep me", next.message)
    }

    @Test
    fun selectedSourceSwitchClearsPreviousChaptersAndUsesMatchingManga() {
        val oldSource = source(id = 1)
        val newSource = source(id = 2)
        val newMatch = match(newSource, manga(newSource, "new"))

        val next = TankobunUiState(
            selectedSourceId = oldSource.id,
            sourceMatches = listOf(newMatch),
            selectedSourceManga = manga(oldSource, "old"),
            sourceChapters = listOf(chapter(oldSource, "old-chapter")),
            chapterProgress = mapOf("old-chapter" to progress()),
        ).withSelectedSource(newSource.id)

        assertEquals(newSource.id, next.selectedSourceId)
        assertEquals(newMatch.manga, next.selectedSourceManga)
        assertTrue(next.sourceChapters.isEmpty())
        assertTrue(next.chapterProgress.isEmpty())
    }

    @Test
    fun withoutSelectedMediaIsNoopWhenNothingSelected() {
        val state = TankobunUiState(message = "still here")

        assertSame(state, state.withoutSelectedMedia())
    }

    @Test
    fun titleLanguagePreferenceUpdatesCachedLocalSurfaces() {
        val media = media(42, "Romaji").copy(
            title = AnilistTitle(
                romaji = "Romaji",
                english = "English",
                native = "Native",
                userPreferred = "Romaji",
            ),
        )
        val recommendation = AnilistRecommendation(
            media = media(7, "Neighbor Romaji").copy(
                title = AnilistTitle(
                    romaji = "Neighbor Romaji",
                    english = "Neighbor English",
                    native = null,
                    userPreferred = "Neighbor Romaji",
                ),
            ),
            rating = 1,
        )

        val next = TankobunUiState(
            libraryMode = LibraryMode.LOCAL,
            library = listOf(media),
            libraryItems = listOf(LibraryItem(media, entry(mediaId = 42))),
            searchResults = listOf(media),
            selectedMedia = media,
            selectedRecommendations = listOf(recommendation),
        ).withAniListTitleLanguage(AnilistTitleLanguage.ENGLISH)

        assertEquals(AnilistTitleLanguage.ENGLISH, next.anilistTitleLanguage)
        assertEquals("English", next.library.single().title.userPreferred)
        assertEquals("English", next.libraryItems.single().media.title.userPreferred)
        assertEquals("English", next.searchResults.single().title.userPreferred)
        assertEquals("English", next.selectedMedia!!.title.userPreferred)
        assertEquals("Neighbor English", next.selectedRecommendations.single().media.title.userPreferred)
    }

    @Test
    fun librarySectionsMapCustomListsBesideAniListStatuses() {
        val current = LibraryItem(
            media = media(42, "Reading"),
            entry = entry(mediaId = 42, status = MediaStatus.CURRENT, customLists = listOf("Favorites")),
        )
        val planning = LibraryItem(
            media = media(7, "Planning"),
            entry = entry(mediaId = 7, status = MediaStatus.PLANNING, customLists = listOf("Favorites", "Reread")),
        )
        val customOnly = LibraryItem(
            media = media(99, "Recommendations"),
            entry = entry(
                mediaId = 99,
                status = MediaStatus.PLANNING,
                customLists = listOf("Favorites"),
                hiddenFromStatusLists = true,
            ),
        )

        val sections = TankobunUiState(
            libraryMode = LibraryMode.LOCAL,
            libraryItems = listOf(planning, current, customOnly),
        ).librarySections

        assertEquals(MediaStatus.CURRENT, sections.first { it.key == MediaStatus.CURRENT.name }.status)
        assertEquals(listOf(42), sections.first { it.key == MediaStatus.CURRENT.name }.items.map { it.media.id })
        assertEquals(listOf(7), sections.first { it.key == MediaStatus.PLANNING.name }.items.map { it.media.id })
        assertEquals(listOf(7, 42, 99), sections.first { it.key == "custom:Favorites" }.items.map { it.media.id }.sorted())
        assertEquals(listOf(7), sections.first { it.key == "custom:Reread" }.items.map { it.media.id })
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
        hiddenFromStatusLists: Boolean = false,
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
            hiddenFromStatusLists = hiddenFromStatusLists,
        )

    private fun source(id: Long = 1L, name: String = "Source"): SourceDescriptor =
        SourceDescriptor(
            id = id,
            name = name,
            lang = "en",
            packageName = "pkg.$id",
            versionName = null,
            versionCode = null,
            isNsfw = false,
            installed = true,
        )

    private fun manga(source: SourceDescriptor, title: String): SourceManga =
        SourceManga(
            sourceId = source.id,
            url = title,
            title = title,
            thumbnailUrl = null,
            description = null,
            author = null,
            artist = null,
            status = null,
        )

    private fun match(source: SourceDescriptor, manga: SourceManga = manga(source, "manga")): SourceSearchResult =
        SourceSearchResult(
            mediaId = 42,
            source = source,
            manga = manga,
            score = 0.9,
            reasons = emptyList(),
            searchedAtEpochMillis = 1L,
        )

    private fun chapter(source: SourceDescriptor, url: String): SourceChapter =
        SourceChapter(
            sourceId = source.id,
            mangaUrl = "manga",
            url = url,
            name = url,
            chapterNumber = 1f,
            scanlator = null,
            uploadedAtEpochMillis = null,
        )

    private fun progress() =
        com.tankobun.core.model.ReadingProgress(
            mediaId = 42,
            chapterUrl = "chapter",
            chapterNumber = 1f,
            pageIndex = 0,
            pageScrollOffset = 0,
            totalPages = 1,
            readerMode = com.tankobun.core.model.ReaderMode.PAGED,
            completed = false,
            updatedAtEpochMillis = 1L,
        )
}
