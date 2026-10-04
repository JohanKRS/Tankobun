package com.tankobun.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tankobun.app.AppLanguage
import com.tankobun.app.LibraryMode
import com.tankobun.app.qa.LayoutQaScreens
import com.tankobun.app.state.DownloadStorageSummary
import com.tankobun.app.state.LocalReadingActivity
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.ui.library.LibraryConnectPrompt
import com.tankobun.core.model.AnilistMangaStats
import com.tankobun.core.model.AnilistStatItem
import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.DownloadState
import org.junit.Test

/** The top of the You tab on a 384dp phone at 115% font scale, signed out with zeros and synced with data. */
class YouQaScreens(language: AppLanguage) : LayoutQaScreens(language) {
    @Test
    fun youEmpty() = capture("you-empty", width = 384.dp) {
        YouTop(TankobunUiState())
    }

    @Test
    fun youSynced() = capture("you-synced", width = 384.dp) {
        YouTop(syncedState())
    }

    @Test
    fun youSyncedDark() = capture("you-synced", width = 384.dp, dark = true) {
        YouTop(syncedState())
    }

    @Test
    fun youNarrow() = capture("you-narrow", width = 320.dp) {
        YouTop(syncedState(libraryMode = LibraryMode.ANILIST, loggedIn = false))
    }

    @Composable
    private fun YouTop(state: TankobunUiState) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1.15f)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                YouProfileCard(state = state, onOpenProfile = {})
                YouQuickStats(
                    stats = state.anilistMangaStats ?: state.localMangaStats(),
                    activity = state.localReadingActivity,
                    libraryItems = state.libraryItems,
                )
                if (!state.loggedIn) {
                    LibraryConnectPrompt(clientConfigured = true, onConnect = {})
                }
                YouDownloadsEntry(state = state, onClick = {})
                YouSettingsSection(state = state, onOpenSettingsRoute = {})
            }
        }
    }

    private fun syncedState(
        libraryMode: LibraryMode = LibraryMode.ANILIST,
        loggedIn: Boolean = true,
    ): TankobunUiState {
        fun job(id: String, state: DownloadState) =
            DownloadJob(id, 1, 1L, "/manga", "/ch/$id", "Chapter $id", state, 20, 4, 0, 0L, 0L)
        return TankobunUiState(
            loggedIn = loggedIn,
            libraryMode = libraryMode,
            viewerName = if (loggedIn) "Marisol_Kawashima88" else null,
            anilistMangaStats = AnilistMangaStats(
                count = 214,
                chaptersRead = 12_840,
                volumesRead = 96,
                meanScore = 8.1,
                statuses = listOf(AnilistStatItem("CURRENT", 14, 900), AnilistStatItem("REPEATING", 2, 40)),
            ),
            localReadingActivity = LocalReadingActivity(currentStreakDays = 381, longestStreakDays = 381),
            downloads = listOf(job("1", DownloadState.RUNNING), job("2", DownloadState.QUEUED), job("3", DownloadState.COMPLETE)),
            downloadStorageSummary = DownloadStorageSummary(totalBytes = 1_288_490_189L),
        )
    }
}
