package com.tankobun.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tankobun.app.AppLanguage
import com.tankobun.app.MediaViewMode
import com.tankobun.app.logic.ChapterStanding
import com.tankobun.app.progressLabel
import com.tankobun.app.compactProgressLabel
import com.tankobun.app.qa.LayoutQaScreens
import com.tankobun.app.state.LibraryUpdateChapter
import com.tankobun.app.state.LibraryUpdateGroup
import com.tankobun.app.ui.components.MediaCoverTile
import com.tankobun.app.ui.components.TankobunMediaStatusLabel
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitle
import com.tankobun.core.model.SourceChapter
import org.junit.Test

class LibraryUpdatesQaScreens(language: AppLanguage) : LayoutQaScreens(language) {
    private val now = System.currentTimeMillis()
    private val hour = 60L * 60 * 1000

    private fun media(id: Int, title: String) = AnilistMedia(
        id = id, idMal = null,
        title = AnilistTitle(romaji = null, english = null, native = null, userPreferred = title),
        description = null, coverImage = null, bannerImage = null, chapters = null, volumes = null, format = null,
        status = "RELEASING", averageScore = null, popularity = null, startDateYear = null, endDateYear = null,
        siteUrl = null, genres = emptyList(), synonyms = emptyList(), isAdult = false, updatedAtEpochSeconds = null,
    )

    private fun chapter(number: Float, name: String) =
        SourceChapter(1L, "/m", "/c/$number", name, number, null, now - 2 * hour)

    private val groups = listOf(
        LibraryUpdateGroup(
            media = media(1, "Frieren: Beyond Journey's End"),
            chapters = listOf(
                LibraryUpdateChapter(chapter(141f, "Chapter 141 - The Northern Plateau"), now - 2 * hour, read = false),
                LibraryUpdateChapter(chapter(140f, "Chapter 140"), now - 2 * hour, read = false),
                LibraryUpdateChapter(chapter(139f, "Chapter 139"), now - 26 * hour, read = true),
                LibraryUpdateChapter(chapter(138f, "Chapter 138"), now - 26 * hour, read = true),
            ),
            newestFoundAtEpochMillis = now - 2 * hour,
            newChapterCount = 4,
        ),
        LibraryUpdateGroup(
            media = media(2, "Dungeon Meshi"),
            chapters = listOf(LibraryUpdateChapter(chapter(98f, "Chapter 98"), now - 80 * hour, read = false)),
            newestFoundAtEpochMillis = now - 80 * hour,
            newChapterCount = 1,
        ),
    )

    @Test
    fun updatesSheet() = capture("library-updates") {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LibraryUpdatesContent(groups, now - 24 * hour, checksEnabled = true, {}, {}, {}, {})
            LibraryUpdatesContent(emptyList(), 0L, checksEnabled = false, {}, {}, {}, {})
        }
    }

    @Test
    fun coverCaptionsAndBadges() = capture("library-covers") {
        val standings = listOf(
            ChapterStanding(4f, remaining = 3, currentCompleted = false),
            ChapterStanding(128f, remaining = 12, currentCompleted = true),
            ChapterStanding(12f, remaining = 0, currentCompleted = true),
            ChapterStanding(1f, remaining = 1, currentCompleted = false),
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Four phone columns leave about 70dp per cover.
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                standings.forEachIndexed { index, standing ->
                    MediaCoverTile(
                        media = media(index, "Frieren: Beyond Journey's End"),
                        viewMode = MediaViewMode.COVER_WITH_INFO,
                        showWholeCover = false,
                        onClick = {},
                        modifier = Modifier.width(70.dp),
                        caption = standing.compactProgressLabel(),
                        badgeCount = standing.remaining,
                    )
                }
            }
            // Continue Reading cards are 190dp wide.
            Column(modifier = Modifier.width(190.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                standings.forEach { TankobunMediaStatusLabel(text = it.progressLabel()) }
                TankobunMediaStatusLabel(text = ChapterStanding(1128.5f, remaining = 128, currentCompleted = false).progressLabel())
            }
        }
    }
}
