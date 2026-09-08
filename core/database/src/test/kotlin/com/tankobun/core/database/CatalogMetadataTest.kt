package com.tankobun.core.database

import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitle
import org.junit.Assert.*
import org.junit.Test

class CatalogMetadataTest {
    @Test fun aniListRefreshRetainsSavedMangaBakaClassification() {
        val incoming = AnilistMedia(
            id = 1, idMal = null, title = AnilistTitle(null, null, null, "Fixture"), description = null,
            coverImage = "anilist-cover", bannerImage = null, chapters = null, volumes = null, format = "MANGA",
            status = null, averageScore = null, popularity = null, startDateYear = null, endDateYear = null,
            siteUrl = null, genres = listOf("Action"), synonyms = emptyList(), isAdult = false,
            updatedAtEpochSeconds = null, tags = listOf("School"),
        ).toEntity(200)
        val cached = incoming.copy(mangaBakaId = 10, mangaBakaTagIds = listOf(29, 515),
            genres = listOf("action", "Martial Arts"), tags = listOf("School", "Chibi"), fetchedAtEpochMillis = 100)
        val saved = incoming.withFallbackDetails(cached)
        assertEquals(listOf(29, 515), saved.mangaBakaTagIds)
        assertEquals(listOf("Action", "Martial Arts"), saved.genres)
        assertEquals(listOf("School", "Chibi"), saved.tags)
        assertEquals("anilist-cover", saved.coverImage)
        assertEquals(200L, saved.fetchedAtEpochMillis)
        val smallCover = incoming.copy(coverImage = "https://cdn.mangabaka.dev/imgproxy/plain/x350@1/asset")
        val largeCover = cached.copy(coverImage = "https://cdn.mangabaka.dev/imgproxy/plain/x350@3/asset")
        assertEquals(largeCover.coverImage, smallCover.withFallbackDetails(largeCover).coverImage)
        assertEquals(smallCover.coverImage, smallCover.withFallbackDetails(largeCover.copy(coverImage = "https://cdn.mangabaka.dev/imgproxy/plain/x350@3/other")).coverImage)
        val converters = TankobunTypeConverters()
        assertEquals(saved.mangaBakaTagIds, converters.intListFromDb(converters.intListToDb(saved.mangaBakaTagIds)))
        assertEquals(emptyList<Int>(), converters.intListFromDb(""))
    }
}
