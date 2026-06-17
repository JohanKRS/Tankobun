package com.tankobun.app.sharing

import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RecommendationShareJsonTest {
    @Test
    fun recommendationShareJsonRoundTripsMetadataOnly() {
        val media = media(42, "Shared Manga")

        val json = buildRecommendationShareJson(
            suggestedListName = "Friend picks",
            items = listOf(media),
            createdAtEpochMillis = 1234L,
        )

        assertTrue(json.contains("\"type\": \"tankobun.recommendations\""))
        assertFalse(json.contains("\"progress\""))
        assertFalse(json.contains("\"score\""))
        assertFalse(json.contains("\"notes\""))
        assertFalse(json.contains("\"private\""))
        assertFalse(json.contains("\"sourcePackageName\""))
        assertFalse(json.contains("\"mangaUrl\""))
        assertFalse(json.contains("\"chapterUrl\""))
        assertFalse(json.contains("\"token\""))

        val parsed = parseRecommendationShareJson(json)
        assertEquals("Friend picks", parsed.suggestedListName)
        assertEquals(1234L, parsed.createdAtEpochMillis)
        assertEquals(media, parsed.items.single())
    }

    @Test
    fun parserRejectsUnsupportedTypeAndVersion() {
        assertInvalid("""{"type":"other","version":1,"items":[{"mediaId":1,"title":{"userPreferred":"A"}}]}""")
        assertInvalid("""{"type":"tankobun.recommendations","version":2,"items":[{"mediaId":1,"title":{"userPreferred":"A"}}]}""")
    }

    @Test
    fun importPreviewMarksExistingItems() {
        val payload = RecommendationSharePayload(
            suggestedListName = "Picks",
            createdAtEpochMillis = 1L,
            items = listOf(media(1, "Existing"), media(2, "New")),
        )

        val preview = payload.toImportPreview(existingMediaIds = setOf(1))

        assertEquals("Picks", preview.suggestedListName)
        assertTrue(preview.items.first { it.media.id == 1 }.alreadyInLibrary)
        assertFalse(preview.items.first { it.media.id == 2 }.alreadyInLibrary)
    }

    private fun assertInvalid(json: String) {
        try {
            parseRecommendationShareJson(json)
            fail("Expected invalid recommendations JSON to be rejected")
        } catch (_: IllegalStateException) {
            // Expected.
        }
    }

    private fun media(id: Int, title: String): AnilistMedia =
        AnilistMedia(
            id = id,
            idMal = id + 100,
            title = AnilistTitle(
                romaji = title,
                english = "$title EN",
                native = null,
                userPreferred = title,
            ),
            description = null,
            coverImage = "https://example.test/$id.jpg",
            bannerImage = "https://example.test/$id-banner.jpg",
            chapters = 12,
            volumes = 2,
            format = "MANGA",
            status = "FINISHED",
            averageScore = 81,
            popularity = 1000 + id,
            startDateYear = 2021,
            endDateYear = 2023,
            siteUrl = "https://anilist.co/manga/$id",
            genres = listOf("Drama", "Mystery"),
            synonyms = listOf("$title Alt"),
            isAdult = false,
            updatedAtEpochSeconds = 99L,
            staff = listOf("Original Author"),
            tags = listOf("Found Family"),
            countryOfOrigin = "JP",
        )
}
