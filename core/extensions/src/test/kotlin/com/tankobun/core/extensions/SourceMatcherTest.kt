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
            status = null,
            siteUrl = null,
            genres = emptyList(),
            synonyms = listOf("Frieren at the Funeral"),
            isAdult = false,
            updatedAtEpochSeconds = null,
        )
        val source = SourceDescriptor(1, "Example", "en", "pkg", "1", 1, false, true)
        val candidates = listOf(
            SourceManga(1, "/a", "Random Fantasy", null, null, null, null, null),
            SourceManga(1, "/b", "Frieren", null, null, null, null, null),
        )

        val ranked = matcher.rank(media, source, candidates, searchedAtEpochMillis = 0)

        assertEquals("/b", ranked.first().manga.url)
        assertTrue(ranked.first().score > 0.95)
    }
}
