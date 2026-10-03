package com.tankobun.app.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tankobun.app.AppLanguage
import com.tankobun.app.qa.LayoutQaScreens
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.app.ui.media.MediaDetailFloatingActions
import com.tankobun.app.ui.media.MediaDetailQuickActions
import com.tankobun.app.ui.media.MediaDetailUiActions
import com.tankobun.app.ui.settings.YouDownloadsEntry
import com.tankobun.app.ui.settings.YouSettingsSection
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceManga
import org.junit.Test
import org.robolectric.annotation.Config

class NavigationQaScreens(language: AppLanguage) : LayoutQaScreens(language) {
    private val badges = TankobunDestinationBadges(libraryUpdates = 12, downloadsActive = true)

    @Test
    fun dock() = capture("dock") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TankobunDestination.entries.forEach { destination ->
                TankobunNavigationDock(selected = destination, badges = badges, hazeState = null, onSelect = {})
            }
        }
    }

    @Test
    fun dockNarrow() = capture("dock-narrow", width = 320.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TankobunDestination.entries.forEach { destination ->
                TankobunNavigationDock(selected = destination, badges = badges, hazeState = null, onSelect = {})
            }
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h1600dp-xxhdpi")
    fun rail() = capture("rail", width = 420.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(TankobunDestination.HOME, TankobunDestination.YOU).forEach { destination ->
                TankobunNavigationRail(selected = destination, badges = badges, hazeState = null, onSelect = {})
            }
        }
    }

    @Test
    fun topBar() = capture("top-bar", width = 360.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TankobunTopBar(
                title = "Library",
                pageIcon = TankobunIcons.LibraryBooks,
                hazeState = null,
                showBack = false,
                ignoreDisplayCutout = true,
                showStatusBar = false,
                onBack = {},
                actions = {
                    TopBarActionButton(TankobunIcons.Bell, "Updates", onClick = {}, badgeCount = 8)
                    TopBarActionButton(TankobunIcons.Download, "Downloads", onClick = {}, badgeDot = true)
                },
            )
        }
    }

    @Test
    fun you() = capture("you") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            YouDownloadsEntry(state = TankobunUiState(), onClick = {})
            YouSettingsSection(state = TankobunUiState(), onOpenSettingsRoute = {})
        }
    }

    @Test
    fun detailActions() = capture("detail-actions") {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            detailStates().forEach { state ->
                MediaDetailQuickActions(state = state, compact = true, actions = noActions)
            }
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h1600dp-xxhdpi")
    fun detailActionsTablet() = capture("detail-actions-tablet", width = 720.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            detailStates().forEach { state ->
                MediaDetailQuickActions(state = state, compact = false, actions = noActions)
            }
        }
    }

    @Test
    fun detailFloatingActions() = capture("detail-floating") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            detailStates().forEach { state ->
                TankobunGlassFloat(hazeState = null, shape = RoundedCornerShape(percent = 50)) {
                    MediaDetailFloatingActions(state = state, actions = noActions)
                }
            }
        }
    }

    private val noActions = MediaDetailUiActions(
        onOpenChapter = {},
        onChooseSource = {},
        onLoadChapters = {},
        onAddToLibrary = {},
        onOpenTracking = {},
        onShareMedia = {},
    )

    private fun detailStates(): List<TankobunUiState> {
        val manga = SourceManga(1L, "/manga", "Manga", null, null, null, null, null)
        fun chapter(number: Float) = SourceChapter(1L, "/manga", "/ch/$number", "Chapter $number", number, null, null)
        val chapters = listOf(chapter(1f), chapter(12f), chapter(128.5f))
        val entry = AnilistListEntry(1, 1, MediaStatus.REPEATING, 12, null, null, false, emptyList(), null)
        val progress = ReadingProgress(1, "/ch/128.5", 128.5f, 3, 0, 20, ReaderMode.PAGED, false, 0L)
        return listOf(
            TankobunUiState(),
            TankobunUiState(selectedSourceManga = manga, sourceChapters = chapters),
            TankobunUiState(
                selectedSourceManga = manga,
                sourceChapters = chapters,
                latestProgress = progress,
                selectedListEntry = entry,
                trackingStatus = MediaStatus.REPEATING,
            ),
            TankobunUiState(
                selectedSourceManga = manga,
                sourceChapters = chapters,
                selectedListEntry = entry,
                trackingStatus = MediaStatus.PLANNING,
                trackingSaveInProgress = true,
            ),
        )
    }
}
