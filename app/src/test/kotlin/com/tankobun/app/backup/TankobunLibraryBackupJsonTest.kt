package com.tankobun.app.backup

import com.tankobun.app.state.LibraryItem
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.AnilistTitleLanguage
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TankobunLibraryBackupJsonTest {
    @Test
    fun tankobunLibraryBackupRoundTripsLocalLibraryData() {
        val item = LibraryItem(
            media = media(),
            entry = AnilistListEntry(
                id = -42,
                mediaId = 42,
                status = MediaStatus.CURRENT,
                progress = 12,
                score = 87.0,
                notes = "local note",
                private = true,
                customLists = listOf("Favorites"),
                updatedAtEpochSeconds = 1234L,
            ),
        )
        val binding = SourceBinding(
            mediaId = 42,
            sourceId = 7L,
            sourcePackageName = "pkg.source",
            mangaUrl = "/manga/sample",
            mangaTitle = "Sample",
            thumbnailUrl = "https://example.test/cover.jpg",
            selectedAtEpochMillis = 55L,
        )
        val progress = ReadingProgress(
            mediaId = 42,
            chapterUrl = "/chapter/1",
            chapterNumber = 1f,
            pageIndex = 3,
            pageScrollOffset = 44,
            totalPages = 10,
            readerMode = ReaderMode.WEBTOON,
            completed = false,
            updatedAtEpochMillis = 99L,
        )

        val json = buildTankobunLibraryBackupJson(
            items = listOf(item),
            scoreFormat = AnilistScoreFormat.POINT_100,
            titleLanguage = AnilistTitleLanguage.ENGLISH,
            customLists = listOf("Favorites"),
            sourceBindings = listOf(binding),
            progress = listOf(progress),
        )

        assertTrue(isTankobunLibraryBackupJson(json))
        val parsed = parseTankobunLibraryBackupJson(json)
        assertEquals(AnilistScoreFormat.POINT_100, parsed.scoreFormat)
        assertEquals(AnilistTitleLanguage.ENGLISH, parsed.titleLanguage)
        assertEquals(listOf("Favorites"), parsed.customLists)
        assertEquals(item.media, parsed.items.single().media)
        assertEquals(item.entry, parsed.items.single().entry)
        assertEquals(binding, parsed.items.single().sourceBinding)
        assertEquals(listOf(progress), parsed.items.single().progress)
    }

    private fun media(): AnilistMedia =
        AnilistMedia(
            id = 42,
            idMal = 24,
            title = AnilistTitle(
                romaji = "Sample",
                english = "Sample EN",
                native = null,
                userPreferred = "Sample",
            ),
            description = "Description",
            coverImage = "https://example.test/cover.jpg",
            bannerImage = null,
            chapters = 20,
            volumes = 3,
            format = "MANGA",
            status = "FINISHED",
            averageScore = 80,
            popularity = 1000,
            startDateYear = 2020,
            endDateYear = 2022,
            siteUrl = "https://anilist.co/manga/42",
            genres = listOf("Action"),
            synonyms = listOf("Sample Synonym"),
            isAdult = false,
            updatedAtEpochSeconds = 500L,
            staff = listOf("Author"),
            tags = listOf("Adventure"),
            countryOfOrigin = "JP",
        )
}
