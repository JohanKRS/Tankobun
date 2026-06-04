package com.tankobun.app.logic

import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceSearchLogicTest {
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
}
