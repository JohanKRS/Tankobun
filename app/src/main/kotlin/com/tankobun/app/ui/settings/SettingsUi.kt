package com.tankobun.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.app.Activity
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
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
import coil3.request.crossfade
import com.tankobun.app.logic.nextInReadingOrderAfter
import com.tankobun.app.logic.sourceSettingsKey
import com.tankobun.app.state.DownloadStorageItem
import com.tankobun.app.state.ExtensionInstallRequest
import com.tankobun.app.state.LibraryItem
import com.tankobun.app.state.LibrarySection
import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.model.AnilistMangaStats
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistStatItem
import com.tankobun.core.model.AnilistTitleLanguage
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
        val detailRoute = if (route == SettingsRoute.MAIN) SettingsRoute.PROFILE else route

        if (tabletLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
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
                TankobunPanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    color = LocalTankobunStyle.current.colors.panel,
                    contentColor = LocalTankobunStyle.current.colors.panelContent,
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
                        .padding(horizontal = 18.dp),
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
    val chromeInsets = LocalTankobunChromeInsets.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = chromeInsets.top + 18.dp, bottom = chromeInsets.bottom + 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                tankobunString(R.string.common_settings),
                style = LocalTankobunStyle.current.typography.sectionLabel,
                color = LocalTankobunStyle.current.colors.accent,
            )
            Text(
                tankobunString(R.string.settings_index_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
}

@Composable
internal fun SettingsRouteRow(
    route: SettingsRoute,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val routeColor = LocalTankobunStyle.current.colors.accent
    val rowColor = if (selected) {
        LocalTankobunStyle.current.colors.selectedChip.copy(alpha = 0.92f)
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        LocalTankobunStyle.current.colors.selectedChipContent
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LocalTankobunStyle.current.radii.control))
            .background(rowColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsRouteIcon(route = route)
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
                modifier = Modifier.size(width = 3.dp, height = 28.dp),
                shape = RoundedCornerShape(LocalTankobunStyle.current.radii.pill),
                color = routeColor,
            ) {}
        }
    }
}

@Composable
internal fun SettingsRouteIcon(route: SettingsRoute) {
    val routeColor = LocalTankobunStyle.current.colors.accent
    Surface(
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
        color = routeColor.copy(alpha = 0.14f),
        contentColor = routeColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = when (route) {
                    SettingsRoute.MAIN -> Icons.Default.Settings
                    SettingsRoute.PROFILE -> Icons.Default.AccountCircle
                    SettingsRoute.APPEARANCE -> Icons.Default.Palette
                    SettingsRoute.LANGUAGES -> Icons.Default.Translate
                    SettingsRoute.LIBRARY -> Icons.Default.CollectionsBookmark
                    SettingsRoute.BROWSE -> Icons.Default.Explore
                    SettingsRoute.READER -> Icons.AutoMirrored.Filled.MenuBook
                    SettingsRoute.DOWNLOADS -> Icons.Default.Download
                    SettingsRoute.ANILIST -> Icons.Default.Link
                    SettingsRoute.CUSTOM_LISTS -> Icons.Default.FormatListBulleted
                    SettingsRoute.BACKUPS -> Icons.Default.Backup
                    SettingsRoute.ABOUT -> Icons.Default.Info
                    SettingsRoute.SOURCES -> Icons.Default.Extension
                },
                contentDescription = null,
                modifier = Modifier.size(19.dp),
            )
        }
    }
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
        SettingsRoute.PROFILE,
        SettingsRoute.ANILIST -> ProfileSettingsScreen(state, viewModel, modifier)
        SettingsRoute.APPEARANCE -> SettingsDetailPanel(
            title = tankobunString(R.string.settings_appearance),
            subtitle = tankobunString(R.string.settings_appearance_subtitle),
            modifier = modifier,
        ) {
            ThemePicker(
                selected = state.themeMode,
                onSelect = viewModel::setThemeMode,
            )
            Text(tankobunString(R.string.settings_dock_position), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            DockAlignmentRow(
                selected = state.dockAlignment,
                onSelect = viewModel::setDockAlignment,
            )
            Text(tankobunString(R.string.settings_system_ui), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SettingsToggleRow(
                title = tankobunString(R.string.settings_show_android_status_bar),
                subtitle = tankobunString(R.string.settings_show_android_status_bar_desc),
                checked = state.showAppStatusBar,
                onCheckedChange = viewModel::setShowAppStatusBar,
            )
            if (deviceHasDisplayCutout) {
                Text(tankobunString(R.string.settings_layout), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                CutoutLayoutToggle(
                    ignoreDisplayCutout = state.ignoreDisplayCutout,
                    onIgnoreDisplayCutoutChange = viewModel::setIgnoreDisplayCutout,
                )
            }
        }
        SettingsRoute.LANGUAGES -> LanguagesSettingsScreen(state, viewModel, modifier)
        SettingsRoute.LIBRARY -> SettingsDetailPanel(
            title = tankobunString(R.string.common_library),
            subtitle = tankobunString(R.string.settings_library_subtitle),
            modifier = modifier,
        ) {
            Text(tankobunString(R.string.settings_library_view), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            MediaViewModeRow(selected = state.libraryViewMode, onSelect = viewModel::setLibraryViewMode)
            Text(tankobunString(R.string.settings_covers_per_row), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CoverColumnsRow(
                selected = state.libraryCoverColumns,
                onSelect = viewModel::setLibraryCoverColumns,
            )
            Text(tankobunString(R.string.settings_cover_framing), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CoverFramingRow(
                showWholeCover = state.libraryShowWholeCovers,
                onShowWholeCoverChange = viewModel::setLibraryShowWholeCovers,
            )
            Text(tankobunString(R.string.settings_library_updates), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LibraryNewChapterChecksToggle(state = state, viewModel = viewModel)
        }
        SettingsRoute.BROWSE -> SettingsDetailPanel(
            title = tankobunString(R.string.common_browse),
            subtitle = tankobunString(R.string.settings_browse_subtitle),
            modifier = modifier,
        ) {
            Text(tankobunString(R.string.settings_browse_view), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            MediaViewModeRow(selected = state.browseViewMode, onSelect = viewModel::setBrowseViewMode)
            Text(tankobunString(R.string.settings_covers_per_row), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CoverColumnsRow(
                selected = state.browseCoverColumns,
                onSelect = viewModel::setBrowseCoverColumns,
            )
            Text(tankobunString(R.string.settings_cover_framing), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CoverFramingRow(
                showWholeCover = state.browseShowWholeCovers,
                onShowWholeCoverChange = viewModel::setBrowseShowWholeCovers,
            )
        }
        SettingsRoute.READER -> SettingsDetailPanel(
            title = tankobunString(R.string.common_reader),
            subtitle = tankobunString(R.string.settings_reader_subtitle),
            modifier = modifier,
        ) {
            Text(tankobunString(R.string.settings_reading_mode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRowCompat {
                TankobunChip(
                    selected = state.readerMode == ReaderMode.PAGED,
                    onClick = { viewModel.setReaderMode(ReaderMode.PAGED) },
                    label = { Text(tankobunString(R.string.reader_paged)) },
                )
                TankobunChip(
                    selected = state.readerMode == ReaderMode.WEBTOON,
                    onClick = { viewModel.setReaderMode(ReaderMode.WEBTOON) },
                    label = { Text(tankobunString(R.string.reader_webtoon)) },
                )
            }
            Text(tankobunString(R.string.settings_page_gaps), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRowCompat {
                (0..3).forEach { level ->
                    TankobunChip(
                        selected = state.readerPageGapLevel == level,
                        onClick = { viewModel.setReaderPageGapLevel(level) },
                        label = { Text(readerGapLabel(level)) },
                    )
                }
            }
        }
        SettingsRoute.DOWNLOADS -> DownloadsSettingsScreen(state, viewModel, modifier)
        SettingsRoute.CUSTOM_LISTS -> CustomListsSettingsScreen(state, viewModel, modifier)
        SettingsRoute.BACKUPS -> BackupsSettingsScreen(state, viewModel, modifier)
        SettingsRoute.ABOUT -> AboutSettingsScreen(
            onReplayOnboarding = viewModel::showOnboarding,
            modifier = modifier,
        )
        SettingsRoute.SOURCES -> SourcesSettingsScreen(state, viewModel)
    }
}

@Composable
private fun LibraryNewChapterChecksToggle(
    state: TankobunUiState,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.setNewChapterChecksEnabled(true)
        } else {
            viewModel.onNewChapterNotificationPermissionDenied()
        }
    }
    val subtitle = buildString {
        append(tankobunString(R.string.settings_daily_chapter_check_desc))
        state.lastNewChapterCheckAtEpochMillis.takeIf { it > 0 }?.let { lastCheck ->
            append('\n')
            append(tankobunString(R.string.settings_daily_chapter_check_last, cacheAgeLabel(lastCheck)))
        }
    }
    SettingsToggleRow(
        title = tankobunString(R.string.settings_daily_chapter_check),
        subtitle = subtitle,
        checked = state.newChapterChecksEnabled,
        onCheckedChange = { enabled ->
            if (!enabled) {
                viewModel.setNewChapterChecksEnabled(false)
            } else if (context.needsPostNotificationPermission()) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setNewChapterChecksEnabled(true)
            }
        },
    )
}

@Composable
internal fun ProfileSettingsScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    SettingsDetailPanel(
        title = tankobunString(R.string.settings_profile),
        subtitle = tankobunString(R.string.settings_profile_subtitle),
        modifier = modifier,
    ) {
        if (state.loggedIn) {
            val stats = remember(state.anilistMangaStats, state.libraryItems) {
                state.anilistMangaStats ?: state.localMangaStats()
            }
            ProfileHeaderCard(state = state)
            ProfileStatsSections(stats = stats)
        } else {
            LibraryConnectPrompt(
                clientConfigured = state.clientConfigured,
                onConnect = {
                    viewModel.loginUrl()?.let { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
            )
        }
        SettingsGroupDivider(label = "AniList")
        Text(tankobunString(R.string.settings_connection), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TankobunPanel(
            modifier = Modifier.fillMaxWidth(),
            color = LocalTankobunStyle.current.colors.panel,
            contentColor = LocalTankobunStyle.current.colors.panelContent,
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    state.viewerName?.let { tankobunString(R.string.settings_signed_in_as, it) } ?: if (state.clientConfigured) {
                        tankobunString(R.string.settings_anilist_ready)
                    } else {
                        tankobunString(R.string.settings_anilist_client_setup_needed)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    tankobunString(R.string.settings_redirect_uri, BuildConfig.ANILIST_REDIRECT_URI),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.librarySyncedAtEpochMillis.takeIf { it > 0 }?.let {
                    Text(
                        tankobunString(R.string.settings_library_cache, cacheAgeLabel(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (state.loggedIn) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TankobunActionButton(label = tankobunString(R.string.common_sync_anilist), onClick = viewModel::refreshLibrary)
                TankobunActionButton(label = tankobunString(R.string.common_sign_out), onClick = viewModel::signOut, filled = false)
            }
        }
        Text(tankobunString(R.string.settings_anilist_preferences), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TankobunPanel(
            modifier = Modifier.fillMaxWidth(),
            color = LocalTankobunStyle.current.colors.panel,
            contentColor = LocalTankobunStyle.current.colors.panelContent,
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    tankobunString(R.string.settings_anilist_preferences_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tankobunString(R.string.settings_title_language), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    FlowRowCompat {
                        AnilistTitleLanguage.entries.forEach { language ->
                            TankobunChip(
                                selected = state.anilistTitleLanguage == language,
                                onClick = { viewModel.setAnilistTitleLanguage(language) },
                                enabled = state.loggedIn && !state.busy,
                                label = { Text(language.settingsLabel()) },
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tankobunString(R.string.settings_rating_format), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    FlowRowCompat {
                        AnilistScoreFormat.entries.forEach { format ->
                            TankobunChip(
                                selected = state.anilistScoreFormat == format,
                                onClick = { viewModel.setAnilistScoreFormat(format) },
                                enabled = state.loggedIn && !state.busy,
                                label = { Text(format.settingsLabel()) },
                            )
                        }
                    }
                }
                if (!state.loggedIn) {
                    Text(
                        tankobunString(R.string.settings_connect_anilist_before_account_preferences),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        SettingsToggleRow(
            title = tankobunString(R.string.settings_show_nsfw_content),
            subtitle = tankobunString(R.string.settings_show_nsfw_content_desc),
            checked = state.showNsfwContent,
            onCheckedChange = viewModel::setShowNsfwContent,
        )
        Text(tankobunString(R.string.settings_sync_behavior), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SettingsToggleRow(
            title = tankobunString(R.string.settings_auto_save_tracking_edits),
            subtitle = tankobunString(R.string.settings_auto_save_tracking_edits_desc),
            checked = state.anilistAutoSaveTrackingChanges,
            onCheckedChange = viewModel::setAnilistAutoSaveTrackingChanges,
            enabled = state.loggedIn,
        )
        SettingsToggleRow(
            title = tankobunString(R.string.settings_update_progress_from_reading),
            subtitle = tankobunString(R.string.settings_update_progress_from_reading_desc),
            checked = state.anilistAutoSyncReaderProgress,
            onCheckedChange = viewModel::setAnilistAutoSyncReaderProgress,
        )
        SettingsToggleRow(
            title = tankobunString(R.string.settings_include_manual_read_marks),
            subtitle = tankobunString(R.string.settings_include_manual_read_marks_desc),
            checked = state.anilistSyncManualReadProgress,
            onCheckedChange = viewModel::setAnilistSyncManualReadProgress,
            enabled = state.anilistAutoSyncReaderProgress,
        )
    }
}

@Composable
private fun ProfileStatsSections(
    stats: AnilistMangaStats,
) {
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    tankobunString(R.string.profile_reading_stats),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stats.meanScore?.let { tankobunString(R.string.profile_score_summary, it.profileScoreLabel()) }
                        ?: tankobunString(R.string.profile_stats_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ProfileStatsGrid(stats = stats)
            ProfileBreakdownGrid(stats = stats)
        }
    }
}

@Composable
private fun SettingsGroupDivider(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            label,
            style = LocalTankobunStyle.current.typography.sectionLabel,
            color = LocalTankobunStyle.current.colors.accent,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        )
    }
}

@Composable
private fun ProfileHeaderCard(state: TankobunUiState) {
    val context = LocalContext.current
    val bannerUrl = state.viewerBannerImageUrl?.takeIf { it.isNotBlank() }
    val avatarUrl = state.viewerAvatarUrl?.takeIf { it.isNotBlank() }
    val profileName = state.viewerName ?: tankobunString(R.string.settings_profile_local_name)
    val hasBanner = bannerUrl != null
    val headerTextColor = if (hasBanner) Color.White else MaterialTheme.colorScheme.onSurface
    val headerSecondaryColor = if (hasBanner) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
    val headerTextShadow = if (hasBanner) {
        Shadow(
            color = Color.Black.copy(alpha = 0.72f),
            offset = Offset(x = 0f, y = 2f),
            blurRadius = 8f,
        )
    } else {
        null
    }
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
                .clip(RoundedCornerShape(LocalTankobunStyle.current.radii.panel)),
        ) {
            if (bannerUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(bannerUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = if (hasBanner) 0.34f else 0.18f)),
            )
            if (hasBanner) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(116.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.58f),
                                ),
                            ),
                        ),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(76.dp),
                    shape = RoundedCornerShape(LocalTankobunStyle.current.radii.pill),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = tankobunString(R.string.profile_avatar_cd),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = LocalTankobunStyle.current.colors.accent,
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        profileName,
                        style = MaterialTheme.typography.titleLarge.copy(shadow = headerTextShadow),
                        color = headerTextColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (state.loggedIn) {
                            tankobunString(R.string.settings_signed_in_as, profileName)
                        } else {
                            tankobunString(R.string.settings_profile_signed_out)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(shadow = headerTextShadow),
                        color = headerSecondaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatsGrid(stats: AnilistMangaStats) {
    val metrics = listOf(
        ProfileMetric(tankobunString(R.string.profile_metric_manga), stats.count.toString(), emphasized = true),
        ProfileMetric(tankobunString(R.string.profile_metric_chapters), stats.chaptersRead.toString()),
        ProfileMetric(tankobunString(R.string.profile_metric_volumes), stats.volumesRead.toString()),
        ProfileMetric(
            tankobunString(R.string.profile_metric_mean_score),
            stats.meanScore?.profileScoreLabel() ?: tankobunString(R.string.common_unknown),
        ),
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 560.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                metrics.forEach { metric ->
                    ProfileMetricCard(
                        label = metric.label,
                        value = metric.value,
                        emphasized = metric.emphasized,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            FlowRowCompat {
                metrics.forEach { metric ->
                    ProfileMetricCard(
                        label = metric.label,
                        value = metric.value,
                        emphasized = metric.emphasized,
                        modifier = Modifier.width(150.dp),
                    )
                }
            }
        }
    }
}

private data class ProfileMetric(
    val label: String,
    val value: String,
    val emphasized: Boolean = false,
)

@Composable
private fun ProfileMetricCard(
    label: String,
    value: String,
    emphasized: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control)
    val accent = LocalTankobunStyle.current.colors.accent
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    if (emphasized) {
                        listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.10f))
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        )
                    },
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProfileBreakdownGrid(stats: AnilistMangaStats) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 640.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileBreakdownSection(
                        title = tankobunString(R.string.profile_top_genres),
                        items = stats.genres,
                        modifier = Modifier.weight(1f),
                    )
                    ProfileBreakdownSection(
                        title = tankobunString(R.string.profile_top_tags),
                        items = stats.tags,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileBreakdownSection(
                        title = tankobunString(R.string.profile_formats),
                        items = stats.formats,
                        modifier = Modifier.weight(1f),
                        nameLabel = { name -> tankobunString(name.mediaFormatLabelRes()) },
                    )
                    ProfileBreakdownSection(
                        title = tankobunString(R.string.profile_statuses),
                        items = stats.statuses,
                        modifier = Modifier.weight(1f),
                        nameLabel = { name -> profileStatusLabel(name) },
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileBreakdownSection(
                    title = tankobunString(R.string.profile_top_genres),
                    items = stats.genres,
                    modifier = Modifier.fillMaxWidth(),
                )
                ProfileBreakdownSection(
                    title = tankobunString(R.string.profile_top_tags),
                    items = stats.tags,
                    modifier = Modifier.fillMaxWidth(),
                )
                ProfileBreakdownSection(
                    title = tankobunString(R.string.profile_formats),
                    items = stats.formats,
                    modifier = Modifier.fillMaxWidth(),
                    nameLabel = { name -> tankobunString(name.mediaFormatLabelRes()) },
                )
                ProfileBreakdownSection(
                    title = tankobunString(R.string.profile_statuses),
                    items = stats.statuses,
                    modifier = Modifier.fillMaxWidth(),
                    nameLabel = { name -> profileStatusLabel(name) },
                )
            }
        }
    }
}

@Composable
private fun ProfileBreakdownSection(
    title: String,
    items: List<AnilistStatItem>,
    modifier: Modifier = Modifier,
    nameLabel: @Composable (String) -> String = { it },
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            val visibleItems = items
                .filter { it.count > 0 && it.name.isNotBlank() }
                .sortedWith(compareByDescending<AnilistStatItem> { it.count }.thenBy { it.name.lowercase(Locale.ROOT) })
                .take(5)
            if (visibleItems.isEmpty()) {
                Text(
                    tankobunString(R.string.profile_stats_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                visibleItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 3.dp, height = 34.dp)
                                .clip(RoundedCornerShape(LocalTankobunStyle.current.radii.pill))
                                .background(LocalTankobunStyle.current.colors.accent.copy(alpha = 0.72f)),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                nameLabel(item.name),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                tankobunQuantityString(R.plurals.manga_count, item.count, item.count),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(LocalTankobunStyle.current.radii.pill),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Text(
                                tankobunQuantityString(R.plurals.chapter_count, item.chaptersRead, item.chaptersRead),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun TankobunUiState.localMangaStats(): AnilistMangaStats {
    val scores = libraryItems.mapNotNull { it.entry.score }.filter { it > 0.0 }
    return AnilistMangaStats(
        count = libraryItems.size,
        chaptersRead = libraryItems.sumOf { it.entry.progress.coerceAtLeast(0) },
        volumesRead = 0,
        meanScore = scores.takeIf { it.isNotEmpty() }?.average(),
        genres = libraryItems.flatMap { it.media.genres }.toProfileStatItems(),
        tags = libraryItems.flatMap { it.media.tags }.toProfileStatItems(),
        formats = libraryItems.mapNotNull { it.media.format }.toProfileStatItems(),
        statuses = libraryItems.map { it.entry.status.name }.toProfileStatItems(),
    )
}

private fun List<String>.toProfileStatItems(): List<AnilistStatItem> =
    mapNotNull { name ->
        name.trim().takeIf { it.isNotBlank() }
    }
        .groupBy { it.lowercase(Locale.ROOT) }
        .map { (_, names) ->
            AnilistStatItem(
                name = names.first(),
                count = names.size,
                chaptersRead = 0,
            )
        }
        .sortedWith(compareByDescending<AnilistStatItem> { it.count }.thenBy { it.name.lowercase(Locale.ROOT) })

private fun Double.profileScoreLabel(): String {
    val rounded = (this * 10).roundToInt() / 10.0
    return if (rounded == rounded.roundToInt().toDouble()) {
        rounded.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", rounded)
    }
}

@Composable
private fun profileStatusLabel(name: String): String =
    runCatching { MediaStatus.valueOf(name) }
        .getOrNull()
        ?.statusLabel()
        ?: name

@Composable
internal fun CustomListsSettingsScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    var creatingList by remember { mutableStateOf(false) }
    var renamingList by remember { mutableStateOf<String?>(null) }
    var deletingList by remember { mutableStateOf<String?>(null) }
    val listCounts = remember(state.libraryItems) {
        state.libraryItems
            .flatMap { item -> item.entry.customLists.map { listName -> listName to item.media.id } }
            .groupBy({ it.first.lowercase(Locale.ROOT) }, { it.second })
            .mapValues { (_, mediaIds) -> mediaIds.distinct().size }
    }

    SettingsDetailPanel(
        title = tankobunString(R.string.settings_custom_lists),
        subtitle = tankobunString(R.string.settings_custom_lists_subtitle),
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TankobunActionButton(
                label = tankobunString(R.string.settings_new_list),
                enabled = state.loggedIn && !state.busy,
                onClick = { creatingList = true },
            )
            TankobunActionButton(
                label = tankobunString(R.string.common_sync_anilist),
                icon = Icons.Default.Refresh,
                enabled = state.loggedIn && !state.busy,
                onClick = viewModel::refreshLibrary,
                filled = false,
            )
        }

        if (!state.loggedIn) {
            TankobunMessageBanner(tankobunString(R.string.settings_connect_anilist_before_custom_lists))
        }

        if (state.anilistCustomLists.isEmpty()) {
            TankobunEmptyState(title = tankobunString(R.string.settings_no_custom_lists))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.anilistCustomLists.forEach { listName ->
                    CustomListSettingsRow(
                        name = listName,
                        count = listCounts[listName.lowercase(Locale.ROOT)] ?: 0,
                        enabled = state.loggedIn && !state.busy,
                        onRename = { renamingList = listName },
                        onDelete = { deletingList = listName },
                    )
                }
            }
        }
    }

    if (creatingList) {
        CustomListNameDialog(
            title = tankobunString(R.string.settings_new_custom_list),
            confirmLabel = tankobunString(R.string.common_create),
            initialName = "",
            onConfirm = { name ->
                viewModel.createAnilistCustomList(name)
                creatingList = false
            },
            onDismiss = { creatingList = false },
        )
    }

    renamingList?.let { listName ->
        CustomListNameDialog(
            title = tankobunString(R.string.settings_rename_custom_list),
            confirmLabel = tankobunString(R.string.common_rename),
            initialName = listName,
            onConfirm = { name ->
                viewModel.renameAnilistCustomList(listName, name)
                renamingList = null
            },
            onDismiss = { renamingList = null },
        )
    }

    deletingList?.let { listName ->
        DeleteCustomListDialog(
            name = listName,
            count = listCounts[listName.lowercase(Locale.ROOT)] ?: 0,
            onConfirm = {
                viewModel.deleteAnilistCustomList(listName)
                deletingList = null
            },
            onDismiss = { deletingList = null },
        )
    }
}

@Composable
internal fun CustomListSettingsRow(
    name: String,
    count: Int,
    enabled: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    tankobunQuantityString(R.plurals.manga_count, count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(enabled = enabled, onClick = onRename) {
                Text(tankobunString(R.string.common_rename))
            }
            TankobunIconActionButton(
                icon = Icons.Default.Delete,
                contentDescription = tankobunString(R.string.settings_delete_custom_list_cd, name),
                enabled = enabled,
                onClick = onDelete,
            )
        }
    }
}

@Composable
internal fun CustomListNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    TankobunDialog(onDismiss = onDismiss) {
        TankobunDialogHeader(title = title, onDismiss = onDismiss)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(tankobunString(R.string.settings_list_name)) },
            shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss) {
                Text(tankobunString(R.string.common_cancel))
            }
            Spacer(Modifier.weight(1f))
            TankobunActionButton(
                label = confirmLabel,
                enabled = name.trim().isNotBlank(),
                onClick = { onConfirm(name) },
            )
        }
    }
}

@Composable
internal fun DeleteCustomListDialog(
    name: String,
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    TankobunDialog(onDismiss = onDismiss) {
        TankobunDialogHeader(title = tankobunString(R.string.settings_delete_custom_list), onDismiss = onDismiss)
        Text(
            tankobunString(R.string.settings_delete_custom_list_message, name, tankobunQuantityString(R.plurals.manga_count, count, count)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss) {
                Text(tankobunString(R.string.common_cancel))
            }
            Spacer(Modifier.weight(1f))
            TankobunActionButton(label = tankobunString(R.string.common_delete), icon = Icons.Default.Delete, onClick = onConfirm)
        }
    }
}

@Composable
internal fun LanguagesSettingsScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val sourceLanguageOptions = remember(state.allInstalledSources, state.availableExtensions, state.sourceLanguages) {
        (state.allInstalledSources.mapNotNull { it.lang } +
            state.availableExtensions.mapNotNull { it.lang } +
            state.sourceLanguages)
            .map { it.normalizedSourceLanguage() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(compareBy<String> { sourceLanguageSortPriority(it) }.thenBy { sourceLanguageLabel(it) })
    }
    SettingsDetailPanel(
        title = tankobunString(R.string.settings_languages),
        subtitle = tankobunString(R.string.settings_languages_subtitle),
        modifier = modifier,
    ) {
        Text(tankobunString(R.string.settings_app_language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TankobunPanel(
            modifier = Modifier.fillMaxWidth(),
            color = LocalTankobunStyle.current.colors.panel,
            contentColor = LocalTankobunStyle.current.colors.panelContent,
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRowCompat {
                    AppLanguage.entries.forEach { language ->
                        TankobunChip(
                            selected = state.appLanguage == language,
                            onClick = { viewModel.setAppLanguage(language) },
                            label = { Text(language.settingsLabel()) },
                        )
                    }
                }
                Text(
                    tankobunString(R.string.settings_language_applied),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(tankobunString(R.string.settings_source_languages), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TankobunPanel(
            modifier = Modifier.fillMaxWidth(),
            color = LocalTankobunStyle.current.colors.panel,
            contentColor = LocalTankobunStyle.current.colors.panelContent,
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    tankobunString(R.string.settings_source_languages_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRowCompat {
                    sourceLanguageOptions.forEach { language ->
                        val alwaysOn = language == UNIVERSAL_SOURCE_LANGUAGE
                        TankobunChip(
                            selected = language in state.sourceLanguages || alwaysOn,
                            onClick = {
                                if (!alwaysOn) {
                                    viewModel.setSourceLanguageEnabled(language, language !in state.sourceLanguages)
                                }
                            },
                            enabled = !alwaysOn,
                            label = {
                                Text(
                                    if (alwaysOn) tankobunString(R.string.settings_source_language_always, sourceLanguageDisplay(language)) else sourceLanguageDisplay(language),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun DockAlignmentRow(
    selected: DockAlignment,
    onSelect: (DockAlignment) -> Unit,
) {
    FlowRowCompat {
        DockAlignment.entries.forEach { alignment ->
            TankobunChip(
                selected = selected == alignment,
                onClick = { onSelect(alignment) },
                label = { Text(alignment.settingsLabel()) },
            )
        }
    }
}

private fun Context.needsPostNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
