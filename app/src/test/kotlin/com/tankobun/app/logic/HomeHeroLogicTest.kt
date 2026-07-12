package com.tankobun.app.logic

import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeHeroLogicTest {
    @Test
    fun mobilePlacesMainCharacterOnTheRight() {
        val images = media(characterCount = 4).mobileHeroCharacterImages()

        assertEquals(listOf("character-2", "character-1"), images)
    }

    @Test
    fun mobileKeepsSingleCharacterFallback() {
        assertTrue(media(characterCount = 1).mobileHeroCharacterImages().isEmpty())
    }

    @Test
    fun bannerKeepsExistingTabletHeroInsteadOfMosaic() {
        val media = media(characterCount = 8).copy(bannerImage = "banner")

        assertTrue(media.tabletHeroCharacterImages(landscape = true, screenWidthDp = 1_280).isEmpty())
    }

    @Test
    fun singleCharacterKeepsCoverFallback() {
        val media = media(characterCount = 1)

        assertTrue(media.tabletHeroCharacterImages(landscape = false, screenWidthDp = 800).isEmpty())
    }

    @Test
    fun portraitPlacesMostRelevantCharactersInTheMiddle() {
        val images = media(characterCount = 8)
            .tabletHeroCharacterImages(landscape = false, screenWidthDp = 800)

        assertEquals(listOf("character-3", "character-1", "character-2", "character-4"), images)
    }

    @Test
    fun wideLandscapeUsesUpToSevenCharacters() {
        val images = media(characterCount = 8)
            .tabletHeroCharacterImages(landscape = true, screenWidthDp = 1_280)

        assertEquals(7, images.size)
        assertEquals("character-1", images[3])
        assertEquals("character-7", images.first())
        assertEquals("character-6", images.last())
    }

    private fun media(characterCount: Int): AnilistMedia =
        AnilistMedia(
            id = 1,
            idMal = null,
            title = AnilistTitle("Manga", null, null, "Manga"),
            description = null,
            coverImage = "cover",
            bannerImage = null,
            chapters = null,
            volumes = null,
            format = null,
            status = "RELEASING",
            averageScore = null,
            popularity = null,
            startDateYear = null,
            endDateYear = null,
            siteUrl = null,
            genres = emptyList(),
            synonyms = emptyList(),
            isAdult = false,
            updatedAtEpochSeconds = null,
            mainCharacterImage = "character-1",
            characterImages = (1..characterCount).map { "character-$it" },
        )
}
