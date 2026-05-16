package com.tankobun.core.extensions

import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceManga
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceMatcherTest {
    private val matcher = SourceMatcher()
    private val source = SourceDescriptor(1, "Example", "en", "pkg", "1", 1, false, true)

    @Test
    fun exactTitleBeatsLooseMatch() {
        val media = AnilistMedia(
            id = 1,
            idMal = null,
            title = AnilistTitle("Sousou no Frieren", "Frieren: Beyond Journey's End", null, "Frieren"),
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
            synonyms = listOf("Frieren at the Funeral"),
            isAdult = false,
            updatedAtEpochSeconds = null,
        )
        val candidates = listOf(
            SourceManga(1, "/a", "Random Fantasy", null, null, null, null, null),
            SourceManga(1, "/b", "Frieren", null, null, null, null, null),
        )

        val ranked = matcher.rank(media, source, candidates, searchedAtEpochMillis = 0)

        assertEquals("/b", ranked.first().manga.url)
        assertTrue(ranked.first().score > 0.95)
    }

    @Test
    fun accentedLatinTitlesMatchPlainSourceTitles() {
        val ranked = matcher.rank(
            media = media("Pokémon Adventures"),
            source = source,
            candidates = listOf(SourceManga(1, "/pokemon", "Pokemon Adventures", null, null, null, null, null)),
            searchedAtEpochMillis = 0,
        )

        assertEquals("/pokemon", ranked.first().manga.url)
        assertTrue(ranked.first().score > 0.95)
    }

    @Test
    fun acronymTitlesCanMatchLongTitles() {
        val ranked = matcher.rank(
            media = media("The Beginning After the End"),
            source = source,
            candidates = listOf(SourceManga(1, "/tbate", "TBATE", null, null, null, null, null)),
            searchedAtEpochMillis = 0,
        )

        assertEquals("/tbate", ranked.first().manga.url)
        assertTrue(ranked.first().score >= 0.9)
    }

    @Test
    fun nonLatinLetterTitlesArePreserved() {
        val ranked = matcher.rank(
            media = media("나 혼자만 레벨업"),
            source = source,
            candidates = listOf(SourceManga(1, "/solo-leveling", "나 혼자만 레벨업", null, null, null, null, null)),
            searchedAtEpochMillis = 0,
        )

        assertEquals("/solo-leveling", ranked.first().manga.url)
        assertTrue(ranked.first().score > 0.95)
    }

    private fun media(title: String): AnilistMedia =
        AnilistMedia(
            id = 1,
            idMal = null,
            title = AnilistTitle(null, null, null, title),
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
}
