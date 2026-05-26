package com.tankobun.app.ui.settings

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowInsets as AndroidWindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.tankobun.app.logic.nextInReadingOrderAfter
import com.tankobun.app.logic.sourceSettingsKey
import com.tankobun.app.state.DownloadStorageItem
import com.tankobun.app.state.ExtensionInstallRequest
import com.tankobun.app.state.LibraryItem
import com.tankobun.app.state.LibrarySection
import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt


import com.tankobun.app.*
import com.tankobun.app.logic.*
import com.tankobun.app.state.*
import com.tankobun.app.ui.browse.*
import com.tankobun.app.ui.components.*
import com.tankobun.app.ui.downloads.*
import com.tankobun.app.ui.library.*
import com.tankobun.app.ui.media.*
import com.tankobun.app.ui.reader.*
import com.tankobun.app.ui.settings.*
import com.tankobun.app.ui.shell.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    route: SettingsRoute,
    onOpenRoute: (SettingsRoute) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val tabletLayout = maxWidth >= 720.dp
        val detailRoute = if (route == SettingsRoute.MAIN) SettingsRoute.APPEARANCE else route

        if (tabletLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsIndexPane(
                    state = state,
                    selectedRoute = detailRoute,
                    onOpenRoute = onOpenRoute,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 288.dp, max = 340.dp),
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(12.dp),
                    color = LocalTankobunTokens.current.elevatedSurface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 1.dp,
                ) {
                    SettingsDetailContent(
                        state = state,
                        viewModel = viewModel,
                        route = detailRoute,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        } else if (route == SettingsRoute.MAIN) {
            SettingsIndexPane(
                state = state,
                selectedRoute = SettingsRoute.MAIN,
                onOpenRoute = onOpenRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            )
        } else {
            SettingsDetailContent(
                state = state,
                viewModel = viewModel,
                route = detailRoute,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun SettingsIndexPane(
    state: TankobunUiState,
    selectedRoute: SettingsRoute,
    onOpenRoute: (SettingsRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Tune Tankobun for how you read, browse, and sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                SettingsDetailRoutes.forEach { settingsRoute ->
                    SettingsRouteRow(
                        route = settingsRoute,
                        summary = settingsRoute.settingsSummary(state),
                        selected = settingsRoute == selectedRoute,
                        onClick = { onOpenRoute(settingsRoute) },
                    )
                }
            }
        }
        Text(
            "Unofficial app. Source extensions and content providers are third parties.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SettingsRouteRow(
    route: SettingsRoute,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val rowColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(rowColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsRouteIcon(route = route, selected = selected)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                route.settingsTitle(),
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            )
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) contentColor.copy(alpha = 0.76f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Surface(
                modifier = Modifier.size(width = 4.dp, height = 28.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {}
        }
    }
}

@Composable
internal fun SettingsRouteIcon(route: SettingsRoute, selected: Boolean) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Icon(
        imageVector = when (route) {
            SettingsRoute.MAIN,
            SettingsRoute.APPEARANCE -> Icons.Default.Settings
            SettingsRoute.LIBRARY -> Icons.AutoMirrored.Filled.LibraryBooks
            SettingsRoute.BROWSE -> Icons.Default.Explore
            SettingsRoute.READER -> Icons.AutoMirrored.Filled.MenuBook
            SettingsRoute.DOWNLOADS -> Icons.Default.Download
            SettingsRoute.ANILIST -> Icons.Default.Link
            SettingsRoute.BACKUPS -> Icons.AutoMirrored.Filled.LibraryBooks
            SettingsRoute.ABOUT -> Icons.Default.Settings
            SettingsRoute.SOURCES -> Icons.Default.Download
        },
        contentDescription = null,
        tint = tint,
    )
}

@Composable
internal fun SettingsDetailContent(
    state: TankobunUiState,
    viewModel: MainViewModel,
    route: SettingsRoute,
    modifier: Modifier = Modifier,
) {
    val deviceHasDisplayCutout = hasDisplayCutout()
    when (route) {
        SettingsRoute.MAIN,
        SettingsRoute.APPEARANCE -> SettingsDetailPanel(
            title = "Appearance",
            subtitle = "Choose a theme and layout behavior for your reading setup.",
            modifier = modifier,
        ) {
            ThemePicker(
                selected = state.themeMode,
                onSelect = viewModel::setThemeMode,
            )
            Text("System UI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SettingsToggleRow(
                title = "Show Android status bar",
                subtitle = "Keep the phone status bar visible in the app. The reader still hides it.",
                checked = state.showAppStatusBar,
                onCheckedChange = viewModel::setShowAppStatusBar,
            )
            if (deviceHasDisplayCutout) {
                Text("Layout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                CutoutLayoutToggle(
                    ignoreDisplayCutout = state.ignoreDisplayCutout,
                    onIgnoreDisplayCutoutChange = viewModel::setIgnoreDisplayCutout,
                )
            }
        }
        SettingsRoute.LIBRARY -> SettingsDetailPanel(
            title = "Library",
            subtitle = "Pick the default layout for your swipeable AniList lists.",
            modifier = modifier,
        ) {
            Text("Library view", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            MediaViewModeRow(selected = state.libraryViewMode, onSelect = viewModel::setLibraryViewMode)
            Text("Covers per row", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CoverColumnsRow(
                selected = state.libraryCoverColumns,
                onSelect = viewModel::setLibraryCoverColumns,
            )
            Text("Cover framing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CoverFramingRow(
                showWholeCover = state.libraryShowWholeCovers,
                onShowWholeCoverChange = viewModel::setLibraryShowWholeCovers,
            )
        }
        SettingsRoute.BROWSE -> SettingsDetailPanel(
            title = "Browse",
            subtitle = "Pick the default layout for AniList search and discovery.",
            modifier = modifier,
        ) {
            Text("Browse view", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            MediaViewModeRow(selected = state.browseViewMode, onSelect = viewModel::setBrowseViewMode)
            Text("Covers per row", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CoverColumnsRow(
                selected = state.browseCoverColumns,
                onSelect = viewModel::setBrowseCoverColumns,
            )
            Text("Cover framing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CoverFramingRow(
                showWholeCover = state.browseShowWholeCovers,
                onShowWholeCoverChange = viewModel::setBrowseShowWholeCovers,
            )
        }
        SettingsRoute.READER -> SettingsDetailPanel(
            title = "Reader",
            subtitle = "Adjust paging, scrolling, and spacing.",
            modifier = modifier,
        ) {
            Text("Reading mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRowCompat {
                FilterChip(
                    selected = state.readerMode == ReaderMode.PAGED,
                    onClick = { viewModel.setReaderMode(ReaderMode.PAGED) },
                    colors = tankobunFilterChipColors(),
                    label = { Text("Paged") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                )
                FilterChip(
                    selected = state.readerMode == ReaderMode.WEBTOON,
                    onClick = { viewModel.setReaderMode(ReaderMode.WEBTOON) },
                    colors = tankobunFilterChipColors(),
                    label = { Text("Webtoon") },
                )
            }
            Text("Page gaps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRowCompat {
                (0..3).forEach { level ->
                    FilterChip(
                        selected = state.readerPageGapLevel == level,
                        onClick = { viewModel.setReaderPageGapLevel(level) },
                        colors = tankobunFilterChipColors(),
                        label = { Text(readerGapLabel(level)) },
                    )
                }
                FilterChip(
                    selected = state.readerFitWidth,
                    onClick = { viewModel.setReaderFitWidth(!state.readerFitWidth) },
                    colors = tankobunFilterChipColors(),
                    label = { Text("Fit paged width") },
                )
            }
        }
        SettingsRoute.DOWNLOADS -> DownloadsSettingsScreen(state, viewModel, modifier)
        SettingsRoute.ANILIST -> SettingsDetailPanel(
            title = "AniList",
            subtitle = "Manage sync and account connection.",
            modifier = modifier,
        ) {
            Text("Connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        state.viewerName?.let { "Signed in as $it" } ?: if (state.clientConfigured) {
                            "AniList is ready to connect."
                        } else {
                            "AniList client setup needed."
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Redirect URI: ${BuildConfig.ANILIST_REDIRECT_URI}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.librarySyncedAtEpochMillis.takeIf { it > 0 }?.let {
                        Text(
                            "Library cache: ${cacheAgeLabel(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.loggedIn) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::refreshLibrary) {
                        Text("Sync AniList")
                    }
                    OutlinedButton(onClick = viewModel::signOut) {
                        Text("Sign out")
                    }
                }
            }
            Text("Sync behavior", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SettingsToggleRow(
                title = "Auto-save tracking edits",
                subtitle = "Status, score, progress, notes, privacy, and custom lists save after a short pause.",
                checked = state.anilistAutoSaveTrackingChanges,
                onCheckedChange = viewModel::setAnilistAutoSaveTrackingChanges,
                enabled = state.loggedIn,
            )
            SettingsToggleRow(
                title = "Update progress from reading",
                subtitle = "Completed chapters move AniList progress to that chapter number.",
                checked = state.anilistAutoSyncReaderProgress,
                onCheckedChange = viewModel::setAnilistAutoSyncReaderProgress,
            )
            SettingsToggleRow(
                title = "Include manual read marks",
                subtitle = "Mark-as-read actions can also move AniList progress forward.",
                checked = state.anilistSyncManualReadProgress,
                onCheckedChange = viewModel::setAnilistSyncManualReadProgress,
                enabled = state.anilistAutoSyncReaderProgress,
            )
        }
        SettingsRoute.BACKUPS -> BackupsSettingsScreen(state, viewModel, modifier)
        SettingsRoute.ABOUT -> AboutSettingsScreen(
            onReplayOnboarding = viewModel::showOnboarding,
            modifier = modifier,
        )
        SettingsRoute.SOURCES -> SourcesSettingsScreen(state, viewModel)
    }
}
