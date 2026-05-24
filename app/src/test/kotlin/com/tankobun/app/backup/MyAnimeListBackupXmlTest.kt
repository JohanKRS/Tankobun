package com.tankobun.app.backup

import com.tankobun.app.state.LibraryItem
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.MediaStatus
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyAnimeListBackupXmlTest {
    @Test
    fun buildMyAnimeListBackupXmlIncludesAniListMetadataAndMalScore() {
        val xml = buildMyAnimeListBackupXml(
            items = listOf(libraryItem(status = MediaStatus.CURRENT, score = 87.0, private = true)),
            viewerName = "reader",
            scoreFormat = AnilistScoreFormat.POINT_100,
        )

        assertTrue(xml.contains("<user_name>reader</user_name>"))
        assertTrue(xml.contains("AniList media id: 42"))
        assertTrue(xml.contains("private: true"))
        assertTrue(xml.contains("<my_status>Reading</my_status>"))
        assertTrue(xml.contains("<my_score>9</my_score>"))
    }

    @Test
    fun parseMyAnimeListBackupXmlRestoresAniListCommentMetadata() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <myanimelist>
                <!-- AniList media id: 123; private: true -->
                <manga>
                    <manga_mangadb_id>456</manga_mangadb_id>
                    <my_status>Completed</my_status>
                    <my_read_chapters>12</my_read_chapters>
                    <my_score>8</my_score>
                    <my_comments>nice</my_comments>
                    <my_tags>Favorites, Reread</my_tags>
                </manga>
            </myanimelist>
        """.trimIndent()

        val entries = parseMyAnimeListBackupXml(
            input = ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)),
            scoreFormat = AnilistScoreFormat.POINT_100,
        )

        assertEquals(1, entries.size)
        val entry = entries.single()
        assertEquals(123, entry.mediaId)
        assertEquals(456, entry.idMal)
        assertEquals(MediaStatus.COMPLETED, entry.status)
        assertEquals(12, entry.progress)
        assertEquals(80.0, entry.score!!, 0.0)
        assertEquals("nice", entry.notes)
        assertEquals(true, entry.private)
        assertEquals(listOf("Favorites", "Reread"), entry.customLists)
    }

    private fun libraryItem(
        status: MediaStatus,
        score: Double?,
        private: Boolean,
    ): LibraryItem =
        LibraryItem(
            media = AnilistMedia(
                id = 42,
                idMal = 24,
                title = AnilistTitle(
                    romaji = "Sample",
                    english = null,
                    native = null,
                    userPreferred = "Sample",
                ),
                description = null,
                coverImage = null,
                bannerImage = null,
                chapters = 10,
                volumes = 2,
                format = "MANGA",
                status = "FINISHED",
                averageScore = null,
                popularity = null,
                startDateYear = null,
                endDateYear = null,
                siteUrl = "https://anilist.co/manga/42",
                genres = emptyList(),
                synonyms = emptyList(),
                isAdult = false,
                updatedAtEpochSeconds = null,
            ),
            entry = AnilistListEntry(
                id = 7,
                mediaId = 42,
                status = status,
                progress = 5,
                score = score,
                notes = "remember this",
                private = private,
                customLists = listOf("Favorites"),
                updatedAtEpochSeconds = null,
            ),
        )
}
