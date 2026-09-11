package com.tankobun.app.logic

import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.extensions.UntrustedExtension
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.ReadingContentKind
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceManga
import com.tankobun.core.model.SourceSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceSearchLogicTest {
    @Test fun novelAndMangaSourceSearchesStaySeparateEvenWithIdenticalNames() {
        val manga = source(id = 1, name = "Same site")
        val novel = source(id = 2, name = "Same site").copy(contentKind = ReadingContentKind.NOVEL)
        val mixed = TankobunUiState(installedSources = listOf(manga, novel), selectedMedia = media(userPreferred = "A story").copy(format = "NOVEL"))
        assertEquals(listOf(novel), mixed.sourcePickerSources())
        assertEquals(listOf(manga), mixed.copy(selectedMedia = mixed.selectedMedia!!.copy(format = "MANGA")).sourcePickerSources())
    }

    @Test fun pickerExcludesCachedMatchesOfTheOtherTypeEvenWhenSelected() {
        val manga = match(source = source(id = 1, name = "Same site"))
        val novel = match(source = source(id = 2, name = "Same site").copy(contentKind = ReadingContentKind.NOVEL))
        val mixed = TankobunUiState(
            selectedMedia = media(userPreferred = "A story").copy(format = "NOVEL"),
            sourceMatches = listOf(manga, novel),
            sourceMatchChapterCounts = mapOf(manga.sourceMatchKey() to 8, novel.sourceMatchKey() to 10),
            selectedSourceId = manga.source.id,
            selectedSourcePackageName = manga.source.packageName,
            selectedSourceManga = manga.manga,
        )
        assertEquals(listOf(novel), mixed.sourcePickerMatches())
        assertEquals(listOf(manga), mixed.copy(selectedMedia = mixed.selectedMedia!!.copy(format = "MANGA")).sourcePickerMatches())
        val completed = mixed.withSourcePickerSearchCompleted(VerifiedSourceMatches(listOf(novel), emptyMap()), null)
        assertEquals(listOf(novel), completed.sourceMatches)
        assertEquals("Found 1 readable sources", completed.sourcePickerMessage)
    }

    @Test fun compatibleMatchesStillRequireInstallationTrustAndVerifiedChapters() {
        val good = match(source = source(id = 1))
        val missing = match(source = source(id = 2).copy(installed = false))
        val untrusted = match(source = source(id = 3))
        val unverified = match(source = source(id = 4))
        val state = TankobunUiState(
            selectedMedia = media(userPreferred = "A story").copy(format = "MANGA"),
            sourceMatches = listOf(good, missing, untrusted, unverified),
            sourceMatchChapterCounts = listOf(good, missing, untrusted).associate { it.sourceMatchKey() to 8 },
            untrustedExtensions = listOf(UntrustedExtension(untrusted.source, setOf("fixture"))),
        )
        assertEquals(listOf(good), state.sourcePickerMatches())
    }

    @Test fun incompatiblePendingExtensionsDoNotHideTheNoSourcesState() {
        val manga = UntrustedExtension(source(id = 1), setOf("fixture"))
        val novel = UntrustedExtension(source(id = 2).copy(contentKind = ReadingContentKind.NOVEL), setOf("fixture"))
        val state = TankobunUiState(
            selectedMedia = media(userPreferred = "A story").copy(format = "NOVEL"),
            sourcePickerOpen = true,
            installedSources = listOf(manga.descriptor),
            untrustedExtensions = listOf(manga),
        )
        assertTrue(state.sourcePickerSources().isEmpty())
        assertTrue(state.sourcePickerUntrustedExtensions().isEmpty())
        assertEquals("Enable or install a source extension first", state.withSourcePickerNoSources().sourcePickerMessage)
        val withNovels = state.copy(untrustedExtensions = listOf(manga, novel))
        assertEquals(listOf(novel), withNovels.sourcePickerUntrustedExtensions())
        assertNull(withNovels.withSourcePickerNoSources().sourcePickerMessage)
        assertEquals(listOf(manga), withNovels.copy(selectedMedia = state.selectedMedia!!.copy(format = "MANGA")).sourcePickerUntrustedExtensions())
    }

    @Test
    fun buildsSearchQueriesFromTitleVariants() {
        val media = media(
            userPreferred = "No.6: Side <b>Story</b> (Novel)",
            romaji = "No6",
            english = "Number Six",
            synonyms = listOf("No. 6"),
        )

        val queries = sourceSearchQueries(media)

        assertTrue("No 6 Side Story" in queries)
        assertTrue("No 6" in queries)
        assertTrue("6 Side Story Novel" in queries)
        assertTrue("Number Six" in queries)
        assertTrue(queries.size <= 6)
        assertTrue(queries.indexOf("Number Six") < queries.indexOf("6 Side Story Novel"))
        assertEquals(queries.distinctBy { it.lowercase() }, queries)
    }

    @Test
    fun titleOverrideIgnoresMediaTitles() {
        val media = media(userPreferred = "Original Title", english = "English Title")

        val queries = sourceSearchQueries(media, titleOverride = "Override: Search")

        assertTrue("Override Search" in queries)
        assertTrue("Override" in queries)
        assertFalse("Original Title" in queries)
        assertFalse("English Title" in queries)
    }

    @Test
    fun formatsKnownSourcePickerErrors() {
        val directUrlError = IllegalArgumentException("Please enter a valid URL")
        val forbiddenError = IllegalStateException("HTTP error 403")

        assertEquals(
            "Demo needs a direct source URL instead of a title search. Paste a supported URL or choose another source.",
            sourcePickerErrorMessage("Demo", directUrlError),
        )
        assertEquals("requires a direct URL", sourcePickerDiagnosticDetail(directUrlError))
        assertTrue(isFatalSourceSearchError(forbiddenError))
        assertTrue(
            isFatalSourceSearchError(
                IllegalStateException("Source search failed", UnsupportedOperationException()),
            ),
        )
    }

    @Test
    fun sourcePickerSourcesDedupesAndPrioritizesSelectedSource() {
        val selected = source(id = 2, name = "Beta", lang = "ja")
        val duplicate = selected.copy(name = "Beta Duplicate")
        val english = source(id = 1, name = "Alpha", lang = "en")

        val sources = TankobunUiState(
            installedSources = listOf(english, selected, duplicate),
            selectedSourceId = selected.id,
        ).sourcePickerSources()

        assertEquals(listOf(selected.id, english.id), sources.map { it.id })
    }

    @Test
    fun sourcePickerSourcesPrioritizesSelectedPackageWhenIdsCollide() {
        val english = source(id = 1, name = "Same", lang = "en", packageName = "pkg.en")
        val portuguese = source(id = 1, name = "Same", lang = "pt-BR", packageName = "pkg.pt")

        val sources = TankobunUiState(
            installedSources = listOf(english, portuguese),
            selectedSourceId = portuguese.id,
            selectedSourcePackageName = portuguese.packageName,
        ).sourcePickerSources()

        assertEquals(portuguese, sources.first())
    }

    @Test
    fun sourcePickerSearchCompletedPreservesSelectedMatch() {
        val selectedSource = source(id = 1, name = "Selected")
        val selectedMatch = match(source = selectedSource, manga = manga(selectedSource, "selected"), score = 0.1)
        val foundSource = source(id = 2, name = "Found")
        val foundMatch = match(source = foundSource, manga = manga(foundSource, "found"), score = 0.95)

        val next = TankobunUiState(
            sourceMatches = listOf(selectedMatch),
            selectedSourceId = selectedSource.id,
            selectedSourceManga = selectedMatch.manga,
            sourcePickerLoading = true,
        ).withSourcePickerSearchCompleted(
            verified = VerifiedSourceMatches(
                matches = listOf(foundMatch),
                chapterCounts = mapOf(foundMatch.sourceMatchKey() to 7),
            ),
            editedTitle = null,
        )

        assertEquals(listOf(foundMatch, selectedMatch), next.sourceMatches)
        assertEquals(7, next.sourceMatchChapterCounts[foundMatch.sourceMatchKey()])
        assertFalse(next.sourcePickerLoading)
        assertEquals("Found 2 readable sources", next.sourcePickerMessage)
    }

    @Test
    fun sourcePickerDiagnosticDoesNotDuplicateMessages() {
        val source = source(name = "Demo")
        val state = TankobunUiState()
            .withSourcePickerDiagnostic(source, "timed out")
            .withSourcePickerDiagnostic(source, "timed out")

        assertEquals(listOf("Demo: timed out"), state.sourcePickerDiagnostics)
    }

    @Test
    fun sourcePickerSourceSelectedCanKeepExistingMatches() {
        val source = source(id = 5, name = "Chosen")
        val existing = match(source = source(id = 2, name = "Existing"))
        val chosen = match(source = source, manga = manga(source, "chosen"))

        val next = TankobunUiState(sourceMatches = listOf(existing))
            .withSourcePickerSourceSelected(chosen, addToMatches = false)

        assertEquals(listOf(existing), next.sourceMatches)
        assertEquals(source.id, next.selectedSourceId)
        assertEquals(chosen.manga, next.selectedSourceManga)
        assertFalse(next.sourcePickerOpen)
        assertEquals("Source selected for chosen", next.message)
    }

    @Test
    fun selectedSourceChapterSelectionPrefersExactSelectedMangaMatch() {
        val selectedSource = source(id = 1, name = "Selected")
        val otherSource = source(id = 2, name = "Other")
        val selectedManga = manga(selectedSource, "selected")
        val otherManga = manga(otherSource, "other")
        val selectedMatch = match(source = selectedSource, manga = selectedManga)
        val otherMatch = match(source = otherSource, manga = otherManga)

        val selection = TankobunUiState(
            installedSources = listOf(selectedSource, otherSource),
            sourceMatches = listOf(otherMatch, selectedMatch),
            selectedSourceId = selectedSource.id,
            selectedSourceManga = selectedManga,
        ).selectedSourceChapterSelection()

        assertEquals(selectedSource, selection?.source)
        assertEquals(selectedManga, selection?.manga)
    }

    @Test
    fun selectedSourceChapterSelectionUsesPackageWhenSourceIdsCollide() {
        val english = source(id = 1, name = "Same", lang = "en", packageName = "pkg.en")
        val portuguese = source(id = 1, name = "Same", lang = "pt-BR", packageName = "pkg.pt")
        val englishManga = manga(english, "english")
        val portugueseManga = manga(portuguese, "portuguese")

        val selection = TankobunUiState(
            installedSources = listOf(english, portuguese),
            sourceMatches = listOf(
                match(source = english, manga = englishManga),
                match(source = portuguese, manga = portugueseManga),
            ),
            selectedSourceId = portuguese.id,
            selectedSourcePackageName = portuguese.packageName,
            selectedSourceManga = portugueseManga,
        ).selectedSourceChapterSelection()

        assertEquals(portuguese, selection?.source)
        assertEquals(portugueseManga, selection?.manga)
    }

    @Test
    fun selectedSourceChapterSelectionReturnsNullWithoutSourceAndManga() {
        assertNull(TankobunUiState().selectedSourceChapterSelection())
    }

    @Test
    fun sourceChaptersLoadedResetsManualSelectionAndStoresCount() {
        val source = source(id = 7)
        val manga = manga(source, "series")
        val chapters = listOf(chapter(source, manga, "chapter-1"))

        val next = TankobunUiState(
            busy = true,
            selectingDownloadChapters = true,
            selectedDownloadChapterUrls = setOf("chapter-1"),
        ).withSourceChaptersLoaded(
            source = source,
            manga = manga,
            chapters = chapters,
            chapterProgress = emptyMap(),
        )

        assertEquals(manga, next.selectedSourceManga)
        assertEquals(chapters, next.sourceChapters)
        assertFalse(next.selectingDownloadChapters)
        assertTrue(next.selectedDownloadChapterUrls.isEmpty())
        assertEquals(1, next.sourceMatchChapterCounts[source.sourceMatchKey(manga.url)])
        assertFalse(next.busy)
    }

    private fun media(
        userPreferred: String,
        romaji: String? = null,
        english: String? = null,
        native: String? = null,
        synonyms: List<String> = emptyList(),
    ): AnilistMedia =
        AnilistMedia(
            id = 1,
            idMal = null,
            title = AnilistTitle(
                romaji = romaji,
                english = english,
                native = native,
                userPreferred = userPreferred,
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
            synonyms = synonyms,
            isAdult = false,
            updatedAtEpochSeconds = null,
        )

    private fun source(
        id: Long = 1L,
        name: String = "Source",
        lang: String = "en",
        packageName: String = "pkg.$id",
    ): SourceDescriptor =
        SourceDescriptor(
            id = id,
            name = name,
            lang = lang,
            packageName = packageName,
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

    private fun match(
        source: SourceDescriptor = source(),
        manga: SourceManga = manga(source, "manga"),
        score: Double = 0.9,
    ): SourceSearchResult =
        SourceSearchResult(
            mediaId = 1,
            source = source,
            manga = manga,
            score = score,
            reasons = emptyList(),
            searchedAtEpochMillis = 1L,
        )

    private fun chapter(source: SourceDescriptor, manga: SourceManga, url: String): SourceChapter =
        SourceChapter(
            sourceId = source.id,
            mangaUrl = manga.url,
            url = url,
            name = url,
            chapterNumber = 1f,
            scanlator = null,
            uploadedAtEpochMillis = null,
        )
}
