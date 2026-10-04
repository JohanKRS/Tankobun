package com.tankobun.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tankobun.app.AppLanguage
import com.tankobun.app.MediaViewMode
import com.tankobun.app.qa.LayoutQaScreens
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistTitle
import org.junit.Test
import org.robolectric.annotation.Config

class CoverGridQaScreens(language: AppLanguage) : LayoutQaScreens(language) {
    private val media = List(16) { index ->
        AnilistMedia(
            id = index + 1, idMal = null,
            title = AnilistTitle(romaji = null, english = null, native = null, userPreferred = "Series ${index + 1}"),
            description = null, coverImage = null, bannerImage = null, chapters = null, volumes = null, format = null,
            status = "RELEASING", averageScore = null, popularity = null, startDateYear = null, endDateYear = null,
            siteUrl = null, genres = emptyList(), synonyms = emptyList(), isAdult = false, updatedAtEpochSeconds = null,
        )
    }

    private fun grid(name: String, width: Dp, height: Dp, coverColumns: Int) = capture(name, width = width) {
        Box(Modifier.height(height)) {
            MediaCollection(
                media = media,
                viewMode = MediaViewMode.COVER_WITH_INFO,
                coverColumns = coverColumns,
                showWholeCovers = false,
                onSelectMedia = {},
            )
        }
    }

    @Test
    fun phone() = grid("cover-grid-phone", width = 360.dp, height = 640.dp, coverColumns = 4)

    // A 1280dp landscape tablet beside the 92dp navigation rail.
    @Test
    @Config(qualifiers = "w1280dp-h800dp-xxhdpi")
    fun tabletLandscape() = grid("cover-grid-tablet-landscape", width = 1188.dp, height = 640.dp, coverColumns = 4)

    @Test
    @Config(qualifiers = "w800dp-h1280dp-xxhdpi")
    fun tabletPortrait() = grid("cover-grid-tablet-portrait", width = 800.dp, height = 900.dp, coverColumns = 2)
}
