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
            title = AnilistTitle("Hoshikawa no Tabibito", "The Hoshikawa Traveler", null, "Hoshikawa"),
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
            synonyms = listOf("Traveler of Hoshikawa"),
            isAdult = false,
            updatedAtEpochSeconds = null,
        )
        val candidates = listOf(
            SourceManga(1, "/a", "Random Fantasy", null, null, null, null, null),
            SourceManga(1, "/b", "Hoshikawa", null, null, null, null, null),
        )

        val ranked = matcher.rank(media, source, candidates, searchedAtEpochMillis = 0)

        assertEquals("/b", ranked.first().manga.url)
        assertTrue(ranked.first().score > 0.95)
    }

    @Test
    fun accentedLatinTitlesMatchPlainSourceTitles() {
        val ranked = matcher.rank(
            media = media("Café Azul Adventures"),
            source = source,
            candidates = listOf(SourceManga(1, "/cafe-azul", "Cafe Azul Adventures", null, null, null, null, null)),
            searchedAtEpochMillis = 0,
        )

        assertEquals("/cafe-azul", ranked.first().manga.url)
        assertTrue(ranked.first().score > 0.95)
    }

    @Test
    fun acronymTitlesCanMatchLongTitles() {
        val ranked = matcher.rank(
            media = media("The Moon After the Rain"),
            source = source,
            candidates = listOf(SourceManga(1, "/tmatr", "TMATR", null, null, null, null, null)),
            searchedAtEpochMillis = 0,
        )

        assertEquals("/tmatr", ranked.first().manga.url)
        assertTrue(ranked.first().score >= 0.9)
    }

    @Test
    fun titleOverrideRanksAgainstTemporarySearchTitle() {
        val ranked = matcher.rank(
            media = media("Archive #8"),
            source = source,
            candidates = listOf(SourceManga(1, "/archive", "Archive No. 8", null, null, null, null, null)),
            searchedAtEpochMillis = 0,
            titleOverrides = listOf("Archive No. 8"),
        )

        assertEquals("/archive", ranked.first().manga.url)
        assertTrue(ranked.first().score > 0.95)
    }

    @Test
    fun singleWordInsideDifferentTitleIsLooseMatch() {
        val ranked = matcher.rank(
            media = media("Rift"),
            source = source,
            candidates = listOf(SourceManga(1, "/rift-princess", "Rift Princess", null, null, null, null, null)),
            searchedAtEpochMillis = 0,
        )

        assertTrue(ranked.first().score < 0.9)
    }

    @Test
    fun nonLatinLetterTitlesArePreserved() {
        val ranked = matcher.rank(
            media = media("별빛 기록실"),
            source = source,
            candidates = listOf(SourceManga(1, "/starlight-archive", "별빛 기록실", null, null, null, null, null)),
            searchedAtEpochMillis = 0,
        )

        assertEquals("/starlight-archive", ranked.first().manga.url)
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
