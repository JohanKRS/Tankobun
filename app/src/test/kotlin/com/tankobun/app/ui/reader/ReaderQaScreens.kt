package com.tankobun.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tankobun.app.AppLanguage
import com.tankobun.app.ReaderBackground
import com.tankobun.app.ReaderDirection
import com.tankobun.app.ReaderPreferences
import com.tankobun.app.qa.LayoutQaScreens
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.SourceChapter
import org.junit.Test

class ReaderQaScreens(language: AppLanguage) : LayoutQaScreens(language) {
    private val now = System.currentTimeMillis()
    private fun chapter(number: Float, name: String) =
        SourceChapter(1L, "/manga", "/ch/$number", name, number, null, now - 3 * 24 * 60 * 60 * 1000L)

    private val noActions = ReaderSettingsActions({}, {}, {}, {}, {})

    @Test
    fun readerChrome() = capture("reader-chrome") {
        Column(
            modifier = Modifier.background(Color(0xFF606060)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReaderTopBar(
                chapterName = "Chapter 130 - The Long Way Back to the Harbor Town",
                mediaTitle = "Frieren: Beyond Journey's End",
                onClose = {},
                onOpenChapters = {},
                onOpenSettings = {},
            )
            listOf(false, true).forEach { rtl ->
                ReaderBottomBar(
                    pageIndex = 4,
                    pageCount = 20,
                    rightToLeft = rtl,
                    hasPreviousChapter = true,
                    hasNextChapter = true,
                    zoomed = rtl,
                    onScrub = {},
                    onScrubFinished = {},
                    scrubValue = 4f,
                    onPreviousChapter = {},
                    onNextChapter = {},
                    onResetZoom = {},
                )
            }
            ReaderBottomBar(
                pageIndex = 0,
                pageCount = 1,
                rightToLeft = false,
                hasPreviousChapter = false,
                hasNextChapter = false,
                zoomed = false,
                onScrub = {},
                onScrubFinished = {},
                scrubValue = 0f,
                onPreviousChapter = {},
                onNextChapter = {},
                onResetZoom = {},
            )
        }
    }

    @Test
    fun chapterEnd() = capture("reader-chapter-end") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(ReaderBackground.BLACK, ReaderBackground.WHITE).forEachIndexed { index, background ->
                CompositionLocalProvider(LocalReaderInk provides background.ink()) {
                    Box(Modifier.height(420.dp).background(background.color())) {
                        ReaderChapterEndPage(
                            chapter = chapter(12f, "Chapter 12"),
                            nextChapter = chapter(13f, "Chapter 13 - A Very Long Chapter Title That Wraps").takeIf { index == 0 },
                            onNextChapter = {},
                            onOpenChapters = {},
                            onClose = {},
                        )
                    }
                }
            }
            Box(Modifier.background(Color.Black)) {
                WebtoonChapterTransitionCard("Chapter 12", "Chapter 13 - A Very Long Chapter Title That Wraps")
            }
        }
    }

    @Test
    fun readerSettings() = capture("reader-settings") {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ReaderSettingsControls(
                state = TankobunUiState(
                    readerMode = ReaderMode.PAGED,
                    readerPreferences = ReaderPreferences(direction = ReaderDirection.RIGHT_TO_LEFT),
                ),
                actions = noActions,
                showAllModes = true,
            )
        }
    }
}
