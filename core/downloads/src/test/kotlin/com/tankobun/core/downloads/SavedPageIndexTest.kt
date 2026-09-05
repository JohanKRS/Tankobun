package com.tankobun.core.downloads

import com.tankobun.core.model.ReaderPage
import org.junit.Assert.*
import org.junit.Test

class SavedPageIndexTest {
    @Test fun legacyDownloadsRequireMatchingOrderAndImageIdentity() {
        val pages = listOf(ReaderPage(0, "first", null, sourcePageIndex = 0), ReaderPage(1, "second", null, sourcePageIndex = 1))
        assertEquals(setOf(0), reusablePageIndexes(pages, listOf(SavedPageIndex(0, "first", false), SavedPageIndex(1, "wrong", false))))
    }
    @Test fun normalizedOrDuplicateIndexesNeverReuseAmbiguousLegacyFiles() {
        val pages = listOf(ReaderPage(0, "first", null, sourcePageIndex = 9), ReaderPage(1, "second", null, sourcePageIndex = 9))
        assertTrue(reusablePageIndexes(pages, listOf(SavedPageIndex(0, "first", false))).isEmpty())
        val duplicatedImages = listOf(ReaderPage(0, "shared-endpoint", null), ReaderPage(1, "shared-endpoint", null))
        assertTrue(reusablePageIndexes(duplicatedImages, listOf(SavedPageIndex(0, "shared-endpoint", false))).isEmpty())
    }
    @Test fun newDownloadsResumeByListPositionEvenWithArbitraryExtensionIndexes() {
        val pages = listOf(ReaderPage(0, "first", null, sourcePageIndex = 9), ReaderPage(1, "second", null, sourcePageIndex = 9))
        assertEquals(setOf(1), reusablePageIndexes(pages, listOf(SavedPageIndex(1, "second", true), SavedPageIndex(9, "obsolete", true))))
    }
}
