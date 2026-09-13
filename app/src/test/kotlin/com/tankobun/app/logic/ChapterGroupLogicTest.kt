package com.tankobun.app.logic

import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.*
import org.junit.Assert.*
import org.junit.Test

class ChapterGroupLogicTest {
    private fun chapter(number: Float, group: String?, suffix: String = group.orEmpty()) =
        SourceChapter(1, "work", "$number/$suffix", "Chapter $number", number, group, null)
    private val all = listOf(chapter(1f, "Paper"), chapter(1f, "Ink"), chapter(2f, "Ink"), chapter(3f, "Paper"), chapter(3f, "Ink"))
    private fun selection(chapters: List<SourceChapter> = all, group: String? = "Paper") =
        selectChapterGroups(chapters, ChapterGroupPreference(true, group))

    @Test fun defaultKeepsEveryVersionAndTheOriginalList() {
        assertSame(all, selectChapterGroups(all, ChapterGroupPreference()).chapters)
    }
    @Test fun preferredGroupFallsBackWithoutHolesOrInventedChapters() {
        val selected = selection().chapters
        assertEquals(listOf(1f, 2f, 3f), selected.map { it.chapterNumber })
        assertEquals(listOf("Paper", "Ink", "Paper"), selected.map { it.scanlator })
        assertTrue(all.containsAll(selected))
        assertEquals(5, all.size)
    }
    @Test fun automaticPrefersCoverageAndIsStableWhenSourceOrderChanges() {
        assertEquals(listOf("Ink", "Ink", "Ink"), selection(group = null).chapters.map { it.scanlator })
        assertEquals(selection(group = null).chapters.toSet(), selection(all.reversed(), null).chapters.toSet())
    }
    @Test fun absentGroupStillShowsAllAvailableChapterNumbers() {
        assertEquals(setOf(1f, 2f, 3f), selection(group = "Missing").chapters.map { it.chapterNumber }.toSet())
    }
    @Test fun explicitMultipleTeamsAreCreditedAndEachCanBePreferred() {
        val joint = chapter(1f, null, "joint").copy(scanlators = listOf(" Paper ", "Ink"))
        assertEquals("Paper · Ink", joint.translationCredit())
        assertEquals(joint, selection(listOf(chapter(1f, "Other"), joint), "Ink").chapters.single())
    }
    @Test fun legacyCreditsAreNotSplitOnPunctuation() {
        assertEquals(listOf("A & B, C"), chapter(1f, "A & B, C").translationGroups().map { it.label })
    }
    @Test fun normalizationIgnoresCaseWhitespaceAndUnicodeWidth() {
        val first = chapter(1f, "ＰＡＰＥＲ\n Team")
        assertEquals(first, selection(listOf(first, chapter(1f, "Ink")), "paper team").chapters.single())
        assertNull(chapter(1f, " \n ").translationCredit())
    }
    @Test fun unknownCreditsCanFillAChapterGap() {
        val chapters = all + chapter(4f, null)
        assertEquals(4, selection(chapters).chapters.size)
        assertNull(selection(chapters).chapters.last().translationCredit())
    }
    @Test fun unknownZeroNanAndSpecialChaptersAreNeverCollapsed() {
        val chapters = listOf(-1f, 0f, Float.NaN, Float.POSITIVE_INFINITY).flatMap { listOf(chapter(it, "Paper"), chapter(it, "Ink")) } +
            listOf(chapter(5f, "Paper"), chapter(5f, "Ink")).map { it.copy(name = "Chapter 5 Special") }
        assertEquals(chapters, selection(chapters).chapters)
    }
    @Test fun repeatedNumbersFromSameTeamStaySeparateToPreserveParts() {
        val chapters = listOf(chapter(1f, "Paper", "a"), chapter(1f, "Paper", "b"), chapter(1f, "Ink"))
        assertEquals(chapters, selection(chapters).chapters)
        val parts = listOf(chapter(1f, "Paper").copy(name = "Chapter 1 Part 1"), chapter(1f, "Ink").copy(name = "Chapter 1 Part 2"))
        assertEquals(parts, selection(parts).chapters)
    }
    @Test fun titleNumberContradictionsDoNotHideRoundedParts() {
        val chapters = listOf(chapter(12f, "Paper").copy(name = "Capítulo 12.1"),
            chapter(12f, "Ink").copy(name = "Capítulo 12.2"))
        assertEquals(chapters, selection(chapters).chapters)
    }
    @Test fun volumesFractionsAndExplicitNumberSuffixesStaySeparate() {
        val chapters = listOf(chapter(1f, "Paper").copy(volume = "1"), chapter(1f, "Ink").copy(volume = "2"),
            chapter(1.5f, "Paper"), chapter(1.6f, "Ink"), chapter(2f, "Paper").copy(chapterNumberText = "2a"), chapter(2f, "Ink").copy(chapterNumberText = "2b"))
        assertEquals(chapters, selection(chapters).chapters)
        val named = listOf(chapter(1f, "Paper").copy(name = "Vol. 1 Chapter 1"), chapter(1f, "Ink").copy(name = "Vol. 2 Chapter 1"))
        assertEquals(named, selection(named).chapters)
    }
    @Test fun explicitPrecisionIsNotLostToFloatRounding() {
        val chapters = listOf(chapter(16777216f, "Paper").copy(chapterNumberText = "16777216"),
            chapter(16777216f, "Ink").copy(chapterNumberText = "16777217"))
        assertEquals(chapters, selection(chapters).chapters)
        assertEquals(chapters.last(), chapters.nextInReadingOrderAfter(chapters.first()))
    }
    @Test fun cachePreferencesAreScopedToSourceAndWorkAndSurviveStateCopies() {
        val key = chapterGroupPreferenceKey("package", 1, "work")!!
        val state = TankobunUiState(selectedSourceId = 1, selectedSourcePackageName = "package",
            selectedSourceManga = SourceManga(1, "work", "Work", null, null, null, null, null), sourceChapters = all,
            chapterGroupPreferences = mapOf(key to ChapterGroupPreference(true, "Paper")))
        assertEquals(3, state.readingChapters.size)
        assertEquals(3, state.copy(message = "changed").readingChapters.size)
        assertSame(state.chapterGroupSelection, state.copy(currentPageIndex = 2).chapterGroupSelection)
        assertEquals(5, state.copy(selectedSourceId = 2).readingChapters.size)
        assertEquals(5, state.copy(selectedSourceManga = state.selectedSourceManga!!.copy(url = "other")).readingChapters.size)
        assertEquals(listOf("Ink", "Ink", "Ink"), state.copy(chapterGroupPreferences = mapOf(key to ChapterGroupPreference(true, "Ink"))).readingChapters.map { it.scanlator })
    }
    @Test fun readStateAndNextDownloadsRespectHiddenReadEditions() {
        val progress = ReadingProgress(1, all[1].url, 1f, 2, 0, 3, ReaderMode.PAGED, true, 10)
        val key = chapterGroupPreferenceKey("package", 1, "work")!!
        val state = TankobunUiState(selectedSourceId = 1, selectedSourcePackageName = "package",
            selectedSourceManga = SourceManga(1, "work", "Work", null, null, null, null, null), sourceChapters = all,
            chapterGroupPreferences = mapOf(key to ChapterGroupPreference(true, "Paper")),
            chapterProgress = mapOf(progress.chapterUrl to progress), latestProgress = progress)
        assertTrue(state.isChapterRead(all[0]))
        assertEquals(listOf(2f, 3f), nextTenDownloadCandidates(state).map { it.chapterNumber })
        assertEquals("Ink", state.readingChapters.nextInReadingOrderAfter(all[1])!!.scanlator)
        assertEquals("Paper", state.readingChapters.nextInReadingOrderAfter(all[2])!!.scanlator)
    }
    @Test fun readerCanCrossVolumesWithRestartedNumbers() {
        val chapters = listOf(chapter(9f, "Paper").copy(volume = "1"), chapter(1f, "Paper").copy(volume = "2"))
        assertEquals(chapters, chapters.reversed().readingOrder())
        assertEquals(chapters[1], chapters.nextInReadingOrderAfter(chapters[0]))
        assertEquals(chapters[0], chapters.previousInReadingOrderBefore(chapters[1]))
    }
}
