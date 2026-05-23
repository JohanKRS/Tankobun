package com.tankobun.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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

private enum class SettingsRoute {
    MAIN,
    APPEARANCE,
    LIBRARY,
    BROWSE,
    READER,
    DOWNLOADS,
    ANILIST,
    BACKUPS,
    ABOUT,
    SOURCES,
}

private val SettingsDetailRoutes = listOf(
    SettingsRoute.APPEARANCE,
    SettingsRoute.LIBRARY,
    SettingsRoute.BROWSE,
    SettingsRoute.READER,
    SettingsRoute.DOWNLOADS,
    SettingsRoute.ANILIST,
    SettingsRoute.BACKUPS,
    SettingsRoute.ABOUT,
    SettingsRoute.SOURCES,
)

private fun SettingsRoute.settingsTitle(): String =
    when (this) {
        SettingsRoute.MAIN -> "Settings"
        SettingsRoute.APPEARANCE -> "Appearance"
        SettingsRoute.LIBRARY -> "Library"
        SettingsRoute.BROWSE -> "Browse"
        SettingsRoute.READER -> "Reader"
        SettingsRoute.DOWNLOADS -> "Downloads"
        SettingsRoute.ANILIST -> "AniList"
        SettingsRoute.BACKUPS -> "Backups"
        SettingsRoute.ABOUT -> "About"
        SettingsRoute.SOURCES -> "Sources"
    }

private enum class QuickDrawerMode {
    CLOSED,
    OVERLAY,
    PINNED,
}

private val QuickDrawerOverlayWidth = 340.dp
private val QuickDrawerPinnedWidth = 320.dp
private val QuickDrawerHandleSlotWidth = 40.dp
private const val QuickDrawerSnapMillis = 240
private const val QuickDrawerScrimAlpha = 0.20f
private const val QuickDrawerBackdropBlurDp = 6f
private const val QuickDrawerElasticLimitDp = 36f

private enum class LibraryPicker {
    FORMAT,
    STATUS,
    YEAR,
}

private enum class ReaderPanAxis {
    BOTH,
    HORIZONTAL,
    WEBTOON,
}

private data class WebtoonReaderPageItem(
    val chapter: SourceChapter,
    val page: ReaderPage,
    val pageIndex: Int,
)

private const val SOURCE_LANGUAGE_FILTER_ACTIVE = "__active__"
private const val SOURCE_LANGUAGE_FILTER_ALL = "__all__"
private const val LIBRARY_SORT_LIST_ORDER = "LIST_ORDER"
private const val LIBRARY_SORT_TITLE = "TITLE"
private const val LIBRARY_SORT_UPDATED = "UPDATED"
private const val LIBRARY_SORT_PROGRESS = "PROGRESS"
private const val LIBRARY_SORT_SCORE = "SCORE"

private val LibrarySortOptions = listOf(
    BrowseOption("List Order", LIBRARY_SORT_LIST_ORDER),
    BrowseOption("Title", LIBRARY_SORT_TITLE),
    BrowseOption("Recently Updated", LIBRARY_SORT_UPDATED),
    BrowseOption("Progress", LIBRARY_SORT_PROGRESS),
    BrowseOption("Score", LIBRARY_SORT_SCORE),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TankobunApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var settingsRoute by remember { mutableStateOf(SettingsRoute.MAIN) }
    var quickDrawerMode by remember { mutableStateOf(QuickDrawerMode.CLOSED) }
    val selectedMedia = state.selectedMedia
    val readerOpen = state.activeChapter != null && state.readerPages.isNotEmpty()

    BackHandler(enabled = readerOpen) {
        viewModel.closeReader()
    }

    BackHandler(enabled = !readerOpen && quickDrawerMode == QuickDrawerMode.OVERLAY) {
        quickDrawerMode = QuickDrawerMode.CLOSED
    }

    BackHandler(
        enabled = !readerOpen &&
            quickDrawerMode != QuickDrawerMode.OVERLAY &&
            (selectedMedia != null || (selectedTab == 3 && settingsRoute != SettingsRoute.MAIN)),
    ) {
        if (selectedMedia != null) {
            viewModel.clearSelectedMedia()
        } else {
            settingsRoute = SettingsRoute.MAIN
        }
    }

    TankobunTheme(themeMode = state.themeMode) {
        val cutoutEndPadding = displayCutoutEndPadding(ignoreDisplayCutout = state.ignoreDisplayCutout)
        Box(
            Modifier
                .fillMaxSize()
                .background(LocalTankobunTokens.current.appBackdrop),
        ) {
            TankobunScaffold(
                state = state,
                viewModel = viewModel,
                selectedTab = selectedTab,
                onSelectTab = {
                    selectedTab = it
                    settingsRoute = SettingsRoute.MAIN
                },
                selectedMedia = selectedMedia,
                settingsRoute = settingsRoute,
                onOpenSettingsRoute = { settingsRoute = it },
                quickDrawerMode = quickDrawerMode,
                onOpenQuickDrawer = { quickDrawerMode = QuickDrawerMode.OVERLAY },
                onCloseQuickDrawer = { quickDrawerMode = QuickDrawerMode.CLOSED },
                onToggleQuickDrawerPin = {
                    quickDrawerMode = if (quickDrawerMode == QuickDrawerMode.PINNED) {
                        QuickDrawerMode.OVERLAY
                    } else {
                        QuickDrawerMode.PINNED
                    }
                },
            )
            if (!readerOpen && quickDrawerMode != QuickDrawerMode.CLOSED && cutoutEndPadding > 0.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(cutoutEndPadding)
                        .background(LocalTankobunTokens.current.elevatedSurface),
                )
            }
            if (readerOpen) {
                FullScreenReader(state, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TankobunScaffold(
    state: TankobunUiState,
    viewModel: MainViewModel,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    selectedMedia: AnilistMedia?,
    settingsRoute: SettingsRoute,
    onOpenSettingsRoute: (SettingsRoute) -> Unit,
    quickDrawerMode: QuickDrawerMode,
    onOpenQuickDrawer: () -> Unit,
    onCloseQuickDrawer: () -> Unit,
    onToggleQuickDrawerPin: () -> Unit,
) {
    val ignoreDisplayCutout = state.ignoreDisplayCutout
    val cutoutStartPadding = displayCutoutStartPadding(ignoreDisplayCutout = ignoreDisplayCutout)
    val cutoutEndPadding = displayCutoutEndPadding(ignoreDisplayCutout = ignoreDisplayCutout)
    val drawerSafeEndPadding = maxOf(cutoutEndPadding, 10.dp)
    val density = LocalDensity.current
    val drawerTravelPx = with(density) { (QuickDrawerOverlayWidth + cutoutEndPadding).toPx() }
    val drawerSnapThresholdPx = with(density) { 72.dp.toPx() }
    val drawerElasticLimitPx = with(density) { QuickDrawerElasticLimitDp.dp.toPx() }
    val drawerScope = rememberCoroutineScope()
    var overlayDrawerDragOffsetPx by remember { mutableFloatStateOf(0f) }
    var closedDrawerDragOffsetPx by remember { mutableFloatStateOf(0f) }
    var overlayDrawerDragging by remember { mutableStateOf(false) }
    var closedDrawerDragging by remember { mutableStateOf(false) }
    val overlayDrawerOffsetPx by animateFloatAsState(
        targetValue = overlayDrawerDragOffsetPx,
        animationSpec = tween(durationMillis = if (overlayDrawerDragging) 0 else QuickDrawerSnapMillis),
        label = "Overlay drawer drag offset",
    )
    val closedDrawerTranslationPx by animateFloatAsState(
        targetValue = drawerTravelPx + closedDrawerDragOffsetPx,
        animationSpec = tween(durationMillis = if (closedDrawerDragging) 0 else QuickDrawerSnapMillis),
        label = "Closed drawer peek offset",
    )
    val overlayDrawerTranslationPx = if (overlayDrawerDragging) {
        overlayDrawerDragOffsetPx
    } else {
        overlayDrawerOffsetPx
    }
    val overlayDrawerRevealFraction = if (quickDrawerMode == QuickDrawerMode.OVERLAY && drawerTravelPx > 0f) {
        (1f - (overlayDrawerTranslationPx / drawerTravelPx)).coerceIn(0f, 1f)
    } else {
        0f
    }
    val closedDrawerRevealFraction = if (
        quickDrawerMode == QuickDrawerMode.CLOSED &&
        closedDrawerDragOffsetPx < -1f &&
        drawerTravelPx > 0f
    ) {
        (1f - (closedDrawerTranslationPx / drawerTravelPx)).coerceIn(0f, 1f)
    } else {
        0f
    }
    val quickDrawerBackdropRevealFraction = maxOf(overlayDrawerRevealFraction, closedDrawerRevealFraction)
    val drawerScrimAlpha = QuickDrawerScrimAlpha * quickDrawerBackdropRevealFraction
    val drawerBackdropBlur = (QuickDrawerBackdropBlurDp * quickDrawerBackdropRevealFraction).dp

    LaunchedEffect(quickDrawerMode) {
        if (quickDrawerMode != QuickDrawerMode.OVERLAY) {
            overlayDrawerDragging = false
            overlayDrawerDragOffsetPx = 0f
        }
        if (quickDrawerMode != QuickDrawerMode.CLOSED) {
            closedDrawerDragging = false
            closedDrawerDragOffsetPx = 0f
        }
    }

    fun openQuickDrawerFromClosed(initialTranslationPx: Float = drawerTravelPx) {
        drawerScope.launch {
            overlayDrawerDragging = true
            overlayDrawerDragOffsetPx = initialTranslationPx
            closedDrawerDragging = false
            closedDrawerDragOffsetPx = 0f
            onOpenQuickDrawer()
            withFrameNanos { }
            withFrameNanos { }
            overlayDrawerDragging = false
            overlayDrawerDragOffsetPx = 0f
        }
    }

    fun settleOpenQuickDrawerFromDrag() {
        closedDrawerDragging = false
        val releaseTranslationPx = drawerTravelPx + closedDrawerDragOffsetPx
        closedDrawerDragOffsetPx = 0f
        openQuickDrawerFromClosed(releaseTranslationPx)
    }

    fun closeQuickDrawerFromOverlay(targetTranslationPx: Float = drawerTravelPx) {
        overlayDrawerDragging = false
        overlayDrawerDragOffsetPx = targetTranslationPx
        drawerScope.launch {
            delay(QuickDrawerSnapMillis.toLong())
            onCloseQuickDrawer()
            overlayDrawerDragOffsetPx = 0f
        }
    }

    Scaffold(
        containerColor = LocalTankobunTokens.current.appBackdrop,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TankobunTopBar(
                title = selectedMedia?.title?.userPreferred
                    ?: if (selectedTab == 3 && settingsRoute != SettingsRoute.MAIN) settingsRoute.settingsTitle() else "Tankobun",
                showBack = selectedMedia != null || (selectedTab == 3 && settingsRoute != SettingsRoute.MAIN),
                ignoreDisplayCutout = ignoreDisplayCutout,
                onBack = {
                    if (selectedMedia != null) {
                        viewModel.clearSelectedMedia()
                    } else {
                        onOpenSettingsRoute(SettingsRoute.MAIN)
                    }
                },
            )
        },
        bottomBar = {
            if (selectedMedia == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = LocalTankobunTokens.current.elevatedSurface,
                    tonalElevation = 2.dp,
                ) {
                    TankobunBottomNavigationBar(
                        selectedTab = selectedTab,
                        onSelectTab = onSelectTab,
                        modifier = Modifier.padding(
                            start = displayCutoutStartPadding(ignoreDisplayCutout = ignoreDisplayCutout),
                            end = displayCutoutEndPadding(ignoreDisplayCutout = ignoreDisplayCutout),
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val handleScreenCenterOffset = (padding.calculateBottomPadding() - padding.calculateTopPadding()) / 2f
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(start = cutoutStartPadding, end = cutoutEndPadding)
                    .blur(drawerBackdropBlur)
                    .background(LocalTankobunTokens.current.appBackdrop),
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (selectedMedia != null) {
                        MangaDetailScreen(state, viewModel, selectedMedia)
                    } else {
                        when (selectedTab) {
                            0 -> LibraryScreen(state, viewModel)
                            1 -> BrowseScreen(state, viewModel)
                            2 -> DownloadsScreen(state, viewModel)
                            3 -> SettingsScreen(
                                state = state,
                                viewModel = viewModel,
                                route = settingsRoute,
                                onOpenRoute = onOpenSettingsRoute,
                            )
                        }
                    }
                }
                if (quickDrawerMode == QuickDrawerMode.PINNED) {
                    val pinnedWidth by animateDpAsState(
                        targetValue = QuickDrawerPinnedWidth,
                        animationSpec = tween(durationMillis = 220),
                        label = "Pinned drawer width",
                    )
                    QuickDrawer(
                        state = state,
                        viewModel = viewModel,
                        selectedMedia = selectedMedia,
                        pinned = true,
                        onClose = onCloseQuickDrawer,
                        onTogglePin = onToggleQuickDrawerPin,
                        drawerWidth = pinnedWidth,
                        endPadding = cutoutEndPadding,
                        modifier = Modifier.fillMaxHeight(),
                    )
                }
            }
            if (quickDrawerMode == QuickDrawerMode.CLOSED && closedDrawerRevealFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = drawerScrimAlpha)),
                )
            }
            if (quickDrawerMode == QuickDrawerMode.CLOSED && closedDrawerDragOffsetPx < -1f) {
                QuickDrawer(
                    state = state,
                    viewModel = viewModel,
                    selectedMedia = selectedMedia,
                    pinned = false,
                    onClose = onCloseQuickDrawer,
                    onTogglePin = onToggleQuickDrawerPin,
                    drawerWidth = QuickDrawerOverlayWidth,
                    endPadding = cutoutEndPadding,
                    showHandle = false,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .graphicsLayer { translationX = closedDrawerTranslationPx },
                )
            }
            if (quickDrawerMode == QuickDrawerMode.CLOSED) {
                QuickDrawerHandle(
                    expanded = false,
                    onClick = { openQuickDrawerFromClosed() },
                    onSwipeIn = { openQuickDrawerFromClosed() },
                    localDragOffset = {
                        quickDrawerOpeningDragOffset(
                            totalX = it,
                            drawerTravelPx = drawerTravelPx,
                            elasticLimitPx = drawerElasticLimitPx,
                        )
                    },
                    onDragOffset = {
                        closedDrawerDragging = true
                        closedDrawerDragOffsetPx = quickDrawerOpeningDragOffset(
                            totalX = it,
                            drawerTravelPx = drawerTravelPx,
                            elasticLimitPx = drawerElasticLimitPx,
                        )
                    },
                    onDragEnd = { totalX ->
                        closedDrawerDragging = false
                        if (totalX < -drawerSnapThresholdPx) {
                            settleOpenQuickDrawerFromDrag()
                        } else {
                            closedDrawerDragOffsetPx = 0f
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(y = handleScreenCenterOffset)
                        .padding(end = drawerSafeEndPadding),
                )
            }
            if (quickDrawerMode == QuickDrawerMode.OVERLAY) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = drawerScrimAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { closeQuickDrawerFromOverlay() },
                        ),
                )
            }
            if (quickDrawerMode == QuickDrawerMode.OVERLAY) {
                QuickDrawer(
                    state = state,
                    viewModel = viewModel,
                    selectedMedia = selectedMedia,
                    pinned = false,
                    onClose = { closeQuickDrawerFromOverlay() },
                    onTogglePin = onToggleQuickDrawerPin,
                    drawerWidth = QuickDrawerOverlayWidth,
                    endPadding = cutoutEndPadding,
                    handleCenterOffset = handleScreenCenterOffset,
                    onHandleDragOffset = {
                        overlayDrawerDragging = true
                        overlayDrawerDragOffsetPx = quickDrawerClosingDragOffset(
                            totalX = it,
                            drawerTravelPx = drawerTravelPx,
                            elasticLimitPx = drawerElasticLimitPx,
                        )
                    },
                    onHandleDragEnd = { totalX ->
                        overlayDrawerDragging = false
                        if (totalX > drawerSnapThresholdPx) {
                            closeQuickDrawerFromOverlay()
                        } else {
                            overlayDrawerDragOffsetPx = 0f
                        }
                    },
                    handleDragLocally = false,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .graphicsLayer { translationX = overlayDrawerTranslationPx },
                )
            }
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

private suspend fun PointerInputScope.detectQuickDrawerHandleSwipe(
    expanded: Boolean,
    onSwipeIn: () -> Unit,
    onSwipeOut: () -> Unit,
    onDragOffset: (Float) -> Unit,
    onDragEnd: (Float) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var totalX = 0f
        var totalY = 0f
        var swipingHorizontally = false
        do {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: break
            val delta = change.position - change.previousPosition
            totalX += delta.x
            totalY += delta.y
            if (!swipingHorizontally && abs(totalX) > 16f && abs(totalX) > abs(totalY)) {
                swipingHorizontally = true
            }
            if (swipingHorizontally) {
                change.consume()
                onDragOffset(totalX)
            }
        } while (event.changes.any { it.pressed })

        if (swipingHorizontally) {
            onDragEnd(totalX)
        }
    }
}

private fun quickDrawerOpeningDragOffset(totalX: Float, drawerTravelPx: Float, elasticLimitPx: Float): Float {
    val pullingOpenPx = totalX.coerceAtMost(0f)
    return if (pullingOpenPx >= -drawerTravelPx) {
        pullingOpenPx
    } else {
        -drawerTravelPx - quickDrawerElasticOvershoot(
            overshootPx = (-drawerTravelPx - pullingOpenPx).coerceAtLeast(0f),
            elasticLimitPx = elasticLimitPx,
        )
    }
}

private fun quickDrawerClosingDragOffset(totalX: Float, drawerTravelPx: Float, elasticLimitPx: Float): Float {
    val pushingClosedPx = totalX.coerceAtLeast(0f)
    return if (pushingClosedPx <= drawerTravelPx) {
        pushingClosedPx
    } else {
        drawerTravelPx + quickDrawerElasticOvershoot(
            overshootPx = (pushingClosedPx - drawerTravelPx).coerceAtLeast(0f),
            elasticLimitPx = elasticLimitPx,
        )
    }
}

private fun quickDrawerElasticOvershoot(overshootPx: Float, elasticLimitPx: Float): Float =
    if (elasticLimitPx <= 0f) {
        0f
    } else {
        elasticLimitPx * overshootPx / (overshootPx + elasticLimitPx)
    }

@Composable
private fun hasDisplayCutout(): Boolean {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val cutoutInsets = WindowInsets.displayCutout
    return cutoutInsets.getLeft(density, layoutDirection) > 0 ||
        cutoutInsets.getRight(density, layoutDirection) > 0 ||
        cutoutInsets.getTop(density) > 0 ||
        cutoutInsets.getBottom(density) > 0
}

@Composable
private fun displayCutoutStartPadding(ignoreDisplayCutout: Boolean): Dp {
    if (ignoreDisplayCutout) return 0.dp
    val layoutDirection = LocalLayoutDirection.current
    return maxOf(
        WindowInsets.displayCutout.asPaddingValues().calculateStartPadding(layoutDirection),
        WindowInsets.safeDrawing.asPaddingValues().calculateStartPadding(layoutDirection),
    )
}

@Composable
private fun displayCutoutEndPadding(ignoreDisplayCutout: Boolean): Dp {
    if (ignoreDisplayCutout) return 0.dp
    val layoutDirection = LocalLayoutDirection.current
    return maxOf(
        WindowInsets.displayCutout.asPaddingValues().calculateEndPadding(layoutDirection),
        WindowInsets.safeDrawing.asPaddingValues().calculateEndPadding(layoutDirection),
    )
}

@Composable
private fun TankobunTopBar(
    title: String,
    showBack: Boolean,
    ignoreDisplayCutout: Boolean,
    onBack: () -> Unit,
) {
    val startInset = displayCutoutStartPadding(ignoreDisplayCutout = ignoreDisplayCutout)
    val endInset = displayCutoutEndPadding(ignoreDisplayCutout = ignoreDisplayCutout)
    Surface(color = LocalTankobunTokens.current.elevatedSurface, tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(start = 18.dp + startInset, end = 18.dp + endInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
            )
            Text(
                title,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun TankobunBottomNavigationBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
    ) {
        listOf(
            Triple("Library", Icons.AutoMirrored.Filled.LibraryBooks, 0),
            Triple("Browse", Icons.Default.Explore, 1),
            Triple("Downloads", Icons.Default.Download, 2),
            Triple("Settings", Icons.Default.Settings, 3),
        ).forEach { (label, icon, index) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onSelectTab(index) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun QuickDrawerHandle(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeIn: () -> Unit = onClick,
    onSwipeOut: () -> Unit = onClick,
    dragLocally: Boolean = true,
    localDragOffset: (Float) -> Float = { totalX -> if (expanded) totalX.coerceAtLeast(0f) else totalX.coerceAtMost(0f) },
    onDragOffset: (Float) -> Unit = {},
    onDragEnd: (Float) -> Unit = { totalX ->
        when {
            expanded && totalX > 48f -> onSwipeOut()
            !expanded && totalX < -48f -> onSwipeIn()
        }
    },
) {
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var handleDragging by remember { mutableStateOf(false) }
    val handleDragOffsetPx by animateFloatAsState(
        targetValue = dragOffsetPx,
        animationSpec = tween(durationMillis = if (handleDragging) 0 else QuickDrawerSnapMillis),
        label = "Quick drawer handle drag",
    )
    val handleWidth by animateDpAsState(
        targetValue = if (expanded) 40.dp else 36.dp,
        animationSpec = tween(durationMillis = 220),
        label = "Quick drawer handle width",
    )
    val handleHeight by animateDpAsState(
        targetValue = if (expanded) 104.dp else 88.dp,
        animationSpec = tween(durationMillis = 220),
        label = "Quick drawer handle height",
    )
    Box(
        modifier = modifier
            .width(handleWidth)
            .height(handleHeight)
            .graphicsLayer { translationX = if (dragLocally) handleDragOffsetPx else 0f }
            .pointerInput(expanded) {
                detectQuickDrawerHandleSwipe(
                    expanded = expanded,
                    onSwipeIn = onSwipeIn,
                    onSwipeOut = onSwipeOut,
                    onDragOffset = {
                        handleDragging = true
                        if (dragLocally) {
                            dragOffsetPx = localDragOffset(it)
                        }
                        onDragOffset(it)
                    },
                    onDragEnd = {
                        handleDragging = false
                        if (dragLocally) dragOffsetPx = 0f
                        onDragEnd(it)
                    },
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .width(4.dp)
                .height(if (expanded) 42.dp else 36.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f),
        ) {}
    }
}

@Composable
private fun QuickDrawer(
    state: TankobunUiState,
    viewModel: MainViewModel,
    selectedMedia: AnilistMedia?,
    pinned: Boolean,
    onClose: () -> Unit,
    onTogglePin: () -> Unit,
    drawerWidth: Dp,
    endPadding: Dp,
    showHandle: Boolean = !pinned,
    handleCenterOffset: Dp = 0.dp,
    onHandleDragOffset: (Float) -> Unit = {},
    onHandleDragEnd: (Float) -> Unit = {},
    handleDragLocally: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val handleSlotWidth = if (pinned) 0.dp else QuickDrawerHandleSlotWidth
    Box(modifier = modifier.width(handleSlotWidth + drawerWidth + endPadding)) {
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(drawerWidth + endPadding)
                .fillMaxHeight(),
            shape = RoundedCornerShape(0.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            tonalElevation = 0.dp,
            shadowElevation = if (pinned) 0.dp else 10.dp,
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(drawerWidth)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quick Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onTogglePin) {
                            Icon(
                                imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (pinned) "Unpin quick actions" else "Pin quick actions",
                                tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    QuickDrawerSection(title = "Sync") {
                        Text(
                            state.librarySyncedAtEpochMillis.takeIf { it > 0 }?.let { "Library cache: ${cacheAgeLabel(it)}" }
                                ?: "Library has not synced yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = viewModel::refreshLibrary, enabled = state.loggedIn, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Sync AniList")
                        }
                    }

                    if (selectedMedia != null) {
                        QuickDrawerSection(title = "AniList") {
                            AniListTrackingSection(state, viewModel, selectedMedia)
                        }
                    }

                    QuickDrawerSection(title = "Continue Reading") {
                        if (state.recentReadingProgress.isNotEmpty()) {
                            state.recentReadingProgress.forEach { item ->
                                RecentReadingAction(item = item, onClick = { viewModel.openRecentProgress(item) })
                            }
                        } else {
                            Text(
                                "Start reading a manga from your Reading list to create resume points.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    QuickDrawerSection(title = "Downloads") {
                        if (state.downloads.isEmpty()) {
                            Text("No downloads queued.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.downloads.take(4).forEach { job ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(job.chapterName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            job.state.name.lowercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!pinned && showHandle) {
            QuickDrawerHandle(
                expanded = true,
                onClick = onClose,
                onSwipeOut = onClose,
                dragLocally = handleDragLocally,
                onDragOffset = onHandleDragOffset,
                onDragEnd = onHandleDragEnd,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(y = handleCenterOffset),
            )
        }
    }
}

@Composable
private fun RecentReadingAction(item: RecentReadingProgress, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            item.media.title.userPreferred,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOf(
                item.chapter?.name ?: item.progress.chapterNumber.takeIf { it > 0 }?.let { "Chapter $it" } ?: "Saved chapter",
                "Page ${item.progress.pageIndex + 1}/${item.progress.totalPages}",
            ).joinToString(" / "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (item.chapter == null) "Open manga" else "Resume")
        }
    }
}

@Composable
private fun QuickDrawerSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun LibraryScreen(state: TankobunUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var picker by remember { mutableStateOf<LibraryPicker?>(null) }
    var format by remember { mutableStateOf<String?>(null) }
    var publishingStatus by remember { mutableStateOf<String?>(null) }
    var year by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf(LIBRARY_SORT_LIST_ORDER) }
    var optionsOpen by remember { mutableStateOf(false) }
    val sections = state.librarySections
    val formatOptions = remember(sections) { libraryFormatOptions(sections) }
    val statusOptions = remember(sections) { libraryStatusOptions(sections) }
    val yearOptions = remember(sections) { libraryYearOptions(sections) }
    val resetLibraryControls = {
        query = ""
        format = null
        publishingStatus = null
        year = null
        sort = LIBRARY_SORT_LIST_ORDER
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LibraryFilterBar(
            query = query,
            onQueryChange = { query = it },
            format = format,
            publishingStatus = publishingStatus,
            year = year,
            sort = sort,
            formatOptions = formatOptions,
            statusOptions = statusOptions,
            yearOptions = yearOptions,
            onOpenPicker = { picker = it },
            onOpenOptions = { optionsOpen = true },
            onReset = resetLibraryControls,
        )

        state.message?.let {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(it, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
            }
        }

        if (!state.loggedIn) {
            LibraryConnectPrompt(
                clientConfigured = state.clientConfigured,
                onConnect = {
                    viewModel.loginUrl()?.let { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
            )
        }

        LibraryPager(
            sections = sections,
            query = query,
            format = format,
            publishingStatus = publishingStatus,
            year = year,
            sort = sort,
            viewMode = state.libraryViewMode,
            coverColumns = state.libraryCoverColumns,
            showWholeCovers = state.libraryShowWholeCovers,
            modifier = Modifier.weight(1f),
            onSelectMedia = viewModel::selectMedia,
        )
    }

    picker?.let { activePicker ->
        BrowseOptionDialog(
            title = when (activePicker) {
                LibraryPicker.FORMAT -> "Format"
                LibraryPicker.STATUS -> "Publishing Status"
                LibraryPicker.YEAR -> "Year"
            },
            options = when (activePicker) {
                LibraryPicker.FORMAT -> formatOptions
                LibraryPicker.STATUS -> statusOptions
                LibraryPicker.YEAR -> yearOptions
            },
            selectedValue = when (activePicker) {
                LibraryPicker.FORMAT -> format
                LibraryPicker.STATUS -> publishingStatus
                LibraryPicker.YEAR -> year
            },
            onSelect = { value ->
                when (activePicker) {
                    LibraryPicker.FORMAT -> format = value
                    LibraryPicker.STATUS -> publishingStatus = value
                    LibraryPicker.YEAR -> year = value
                }
                picker = null
            },
            onDismiss = { picker = null },
        )
    }

    if (optionsOpen) {
        LibraryOptionsDialog(
            state = state,
            viewModel = viewModel,
            sort = sort,
            onSortChange = { sort = it ?: LIBRARY_SORT_LIST_ORDER },
            onReset = resetLibraryControls,
            onDismiss = { optionsOpen = false },
        )
    }
}

@Composable
private fun LibraryFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    format: String?,
    publishingStatus: String?,
    year: String?,
    sort: String,
    formatOptions: List<BrowseOption>,
    statusOptions: List<BrowseOption>,
    yearOptions: List<BrowseOption>,
    onOpenPicker: (LibraryPicker) -> Unit,
    onOpenOptions: () -> Unit,
    onReset: () -> Unit,
) {
    val controlsActive = query.isNotBlank() ||
        format != null ||
        publishingStatus != null ||
        year != null ||
        sort != LIBRARY_SORT_LIST_ORDER

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear library search")
                    }
                }
            },
            placeholder = { Text("Search your library") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(18.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrowseFilterPill(
                label = "Format",
                value = formatOptions.labelFor(format),
                selected = format != null,
                onClick = { onOpenPicker(LibraryPicker.FORMAT) },
            )
            BrowseFilterPill(
                label = "Status",
                value = statusOptions.labelFor(publishingStatus),
                selected = publishingStatus != null,
                onClick = { onOpenPicker(LibraryPicker.STATUS) },
            )
            BrowseFilterPill(
                label = "Year",
                value = yearOptions.labelFor(year),
                selected = year != null,
                onClick = { onOpenPicker(LibraryPicker.YEAR) },
            )
            BrowseFilterPill(
                label = "Sort",
                value = LibrarySortOptions.labelFor(sort),
                selected = sort != LIBRARY_SORT_LIST_ORDER,
                onClick = onOpenOptions,
            )
            IconButton(
                onClick = onOpenOptions,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LocalTankobunTokens.current.elevatedSurface),
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Library options")
            }
            if (controlsActive) {
                TextButton(onClick = onReset) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun LibraryConnectPrompt(
    clientConfigured: Boolean,
    onConnect: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Link, contentDescription = null)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (clientConfigured) "Connect AniList" else "AniList setup needed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (clientConfigured) {
                        "Sign in to show your lists here."
                    } else {
                        "Add AniList credentials in settings before connecting."
                    },
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(enabled = clientConfigured, onClick = onConnect) {
                Text("Connect")
            }
        }
    }
}

@Composable
private fun LibraryPager(
    sections: List<LibrarySection>,
    query: String,
    format: String?,
    publishingStatus: String?,
    year: String?,
    sort: String,
    viewMode: MediaViewMode,
    coverColumns: Int,
    showWholeCovers: Boolean,
    modifier: Modifier,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    if (sections.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            Text("No manga in your AniList library yet.")
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { sections.size })
    val scope = rememberCoroutineScope()
    val visibleSections = remember(sections, query, format, publishingStatus, year, sort) {
        sections.map { section ->
            section.copy(
                items = section.items
                    .filterLibraryItems(
                        query = query,
                        format = format,
                        publishingStatus = publishingStatus,
                        year = year,
                    )
                    .sortLibraryItems(sort),
            )
        }
    }
    val filtersActive = query.isNotBlank() || format != null || publishingStatus != null || year != null

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage.coerceAtMost(sections.lastIndex),
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            sections.forEachIndexed { index, section ->
                val count = visibleSections[index].items.size
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            "${section.title} $count",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            MediaCollection(
                media = visibleSections[page].items.map { it.media },
                viewMode = viewMode,
                coverColumns = coverColumns,
                showWholeCovers = showWholeCovers,
                onSelectMedia = onSelectMedia,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
                emptyMessage = if (filtersActive) {
                    "No titles match these library filters."
                } else {
                    "No manga in this AniList list yet."
                },
            )
        }
    }
}

private fun libraryFormatOptions(sections: List<LibrarySection>): List<BrowseOption> =
    listOf(BrowseOption("Any", null)) +
        sections.flatMap { section -> section.items }
            .mapNotNull { it.media.format }
            .distinct()
            .sorted()
            .map { BrowseOption(it.mediaFormatLabel(), it) }

private fun libraryStatusOptions(sections: List<LibrarySection>): List<BrowseOption> =
    listOf(BrowseOption("Any", null)) +
        sections.flatMap { section -> section.items }
            .mapNotNull { it.media.status }
            .distinct()
            .sorted()
            .map { BrowseOption(it.statusLabel(), it) }

private fun libraryYearOptions(sections: List<LibrarySection>): List<BrowseOption> =
    listOf(BrowseOption("Any", null)) +
        sections.flatMap { section -> section.items }
            .mapNotNull { it.media.startDateYear }
            .distinct()
            .sortedDescending()
            .map { BrowseOption(it.toString(), it.toString()) }

private fun List<LibraryItem>.filterLibraryItems(
    query: String,
    format: String?,
    publishingStatus: String?,
    year: String?,
): List<LibraryItem> {
    val normalizedQuery = query.trim().lowercase()
    return filter { item ->
        val media = item.media
        val queryMatches = normalizedQuery.isBlank() || media.librarySearchText().contains(normalizedQuery)
        val formatMatches = format == null || media.format == format
        val statusMatches = publishingStatus == null || media.status == publishingStatus
        val yearMatches = year == null || media.startDateYear?.toString() == year
        queryMatches && formatMatches && statusMatches && yearMatches
    }
}

private fun List<LibraryItem>.sortLibraryItems(sort: String): List<LibraryItem> =
    when (sort) {
        LIBRARY_SORT_TITLE -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.media.title.userPreferred })
        LIBRARY_SORT_UPDATED -> sortedByDescending { it.entry.updatedAtEpochSeconds ?: it.media.updatedAtEpochSeconds ?: 0L }
        LIBRARY_SORT_PROGRESS -> sortedByDescending { it.entry.progress }
        LIBRARY_SORT_SCORE -> sortedByDescending { it.entry.score ?: 0.0 }
        else -> this
    }

private fun AnilistMedia.librarySearchText(): String =
    buildList {
        add(title.userPreferred)
        title.romaji?.let(::add)
        title.english?.let(::add)
        title.native?.let(::add)
        genres.forEach(::add)
        synonyms.forEach(::add)
    }.joinToString(" ").lowercase()

private enum class BrowsePicker {
    FORMAT,
    STATUS,
    COUNTRY,
    YEAR,
}

private data class BrowseOption(
    val label: String,
    val value: String?,
)

private const val BROWSE_SORT_SEARCH_MATCH_UI = "SEARCH_MATCH"

private val BrowseGenres = listOf(
    "Action",
    "Adventure",
    "Comedy",
    "Drama",
    "Ecchi",
    "Fantasy",
    "Horror",
    "Mahou Shoujo",
    "Mecha",
    "Music",
    "Mystery",
    "Psychological",
    "Romance",
    "Sci-Fi",
    "Slice of Life",
    "Sports",
    "Supernatural",
    "Thriller",
)

private val BrowseFormatOptions = listOf(
    BrowseOption("Any", null),
    BrowseOption("Manga", "MANGA"),
    BrowseOption("Novel", "NOVEL"),
    BrowseOption("One Shot", "ONE_SHOT"),
)

private val BrowseStatusOptions = listOf(
    BrowseOption("Any", null),
    BrowseOption("Releasing", "RELEASING"),
    BrowseOption("Finished", "FINISHED"),
    BrowseOption("Not Yet Released", "NOT_YET_RELEASED"),
    BrowseOption("Cancelled", "CANCELLED"),
    BrowseOption("Hiatus", "HIATUS"),
)

private val BrowseCountryOptions = listOf(
    BrowseOption("Any", null),
    BrowseOption("Japan", "JP"),
    BrowseOption("South Korea", "KR"),
    BrowseOption("China", "CN"),
    BrowseOption("Taiwan", "TW"),
)

private val BrowseSortOptions = listOf(
    BrowseOption("Search Match", "SEARCH_MATCH"),
    BrowseOption("Trending", "TRENDING_DESC"),
    BrowseOption("Popularity", "POPULARITY_DESC"),
    BrowseOption("Favorites", "FAVOURITES_DESC"),
    BrowseOption("Average Score", "SCORE_DESC"),
    BrowseOption("Recently Updated", "UPDATED_AT_DESC"),
    BrowseOption("Newest", "START_DATE_DESC"),
    BrowseOption("Title", "TITLE_ROMAJI"),
)

@Composable
private fun BrowseScreen(state: TankobunUiState, viewModel: MainViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadBrowseLanding()
        viewModel.loadBrowseTags()
    }

    var picker by remember { mutableStateOf<BrowsePicker?>(null) }
    var genresOpen by remember { mutableStateOf(false) }
    var tagsOpen by remember { mutableStateOf(false) }
    var advancedOpen by remember { mutableStateOf(false) }
    val controlsActive = state.browseControlsActive()

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BrowseFilterBar(
                state = state,
                viewModel = viewModel,
                onOpenGenres = { genresOpen = true },
                onOpenTags = { tagsOpen = true },
                onOpenPicker = { picker = it },
                onOpenAdvanced = { advancedOpen = true },
            )

            state.message?.let {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(it, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
            }

            if (controlsActive || state.browseSearched) {
                BrowseResults(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                )
            } else {
                BrowseLanding(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (genresOpen) {
            BrowseGenreDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { genresOpen = false },
            )
        }

        if (tagsOpen) {
            BrowseTagDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { tagsOpen = false },
            )
        }

        picker?.let { activePicker ->
            BrowseOptionDialog(
                title = when (activePicker) {
                    BrowsePicker.FORMAT -> "Format"
                    BrowsePicker.STATUS -> "Publishing Status"
                    BrowsePicker.COUNTRY -> "Country Of Origin"
                    BrowsePicker.YEAR -> "Year"
                },
                options = when (activePicker) {
                    BrowsePicker.FORMAT -> BrowseFormatOptions
                    BrowsePicker.STATUS -> BrowseStatusOptions
                    BrowsePicker.COUNTRY -> BrowseCountryOptions
                    BrowsePicker.YEAR -> browseYearOptions()
                },
                selectedValue = when (activePicker) {
                    BrowsePicker.FORMAT -> state.browseFormat
                    BrowsePicker.STATUS -> state.browsePublishingStatus
                    BrowsePicker.COUNTRY -> state.browseCountryOfOrigin
                    BrowsePicker.YEAR -> state.browseYear?.toString()
                },
                onSelect = { value ->
                    when (activePicker) {
                        BrowsePicker.FORMAT -> viewModel.setBrowseFormat(value)
                        BrowsePicker.STATUS -> viewModel.setBrowsePublishingStatus(value)
                        BrowsePicker.COUNTRY -> viewModel.setBrowseCountryOfOrigin(value)
                        BrowsePicker.YEAR -> viewModel.setBrowseYear(value?.toIntOrNull())
                    }
                    picker = null
                    viewModel.searchAniList()
                },
                onDismiss = { picker = null },
            )
        }

        if (advancedOpen) {
            BrowseAdvancedDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { advancedOpen = false },
            )
        }
    }
}

@Composable
private fun BrowseFilterBar(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onOpenGenres: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenPicker: (BrowsePicker) -> Unit,
    onOpenAdvanced: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = viewModel::searchAniList) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            },
            placeholder = { Text("Search AniList manga") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.searchAniList() }),
            shape = RoundedCornerShape(18.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrowseFilterPill(
                label = "Genres",
                value = if (state.browseGenres.isEmpty()) "Any" else state.browseGenres.size.toString(),
                selected = state.browseGenres.isNotEmpty(),
                onClick = onOpenGenres,
            )
            BrowseFilterPill(
                label = "Tags",
                value = if (state.browseTags.isEmpty()) "Any" else state.browseTags.size.toString(),
                selected = state.browseTags.isNotEmpty(),
                onClick = onOpenTags,
            )
            BrowseFilterPill(
                label = "Format",
                value = BrowseFormatOptions.labelFor(state.browseFormat),
                selected = state.browseFormat != null,
                onClick = { onOpenPicker(BrowsePicker.FORMAT) },
            )
            BrowseFilterPill(
                label = "Status",
                value = BrowseStatusOptions.labelFor(state.browsePublishingStatus),
                selected = state.browsePublishingStatus != null,
                onClick = { onOpenPicker(BrowsePicker.STATUS) },
            )
            BrowseFilterPill(
                label = "Country",
                value = BrowseCountryOptions.labelFor(state.browseCountryOfOrigin),
                selected = state.browseCountryOfOrigin != null,
                onClick = { onOpenPicker(BrowsePicker.COUNTRY) },
            )
            BrowseFilterPill(
                label = "Year",
                value = state.browseYear?.toString() ?: "Any",
                selected = state.browseYear != null,
                onClick = { onOpenPicker(BrowsePicker.YEAR) },
            )
            BrowseFilterPill(
                label = "Sort",
                value = BrowseSortOptions.labelFor(state.browseSort),
                selected = state.browseSort != BROWSE_SORT_SEARCH_MATCH_UI,
                onClick = onOpenAdvanced,
            )
            IconButton(
                onClick = onOpenAdvanced,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LocalTankobunTokens.current.elevatedSurface),
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Browse options")
            }
        }
    }
}

@Composable
private fun BrowseFilterPill(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                if (selected) "$label: $value" else label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun BrowseLanding(
    state: TankobunUiState,
    viewModel: MainViewModel,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(34.dp),
    ) {
        item {
            BrowseMangaShelf(
                title = "TRENDING NOW",
                media = state.browseTrending,
                onViewAll = { viewModel.viewAllBrowseSection("TRENDING_DESC") },
                onSelectMedia = viewModel::selectMedia,
            )
        }
        item {
            BrowseMangaShelf(
                title = "ALL TIME POPULAR",
                media = state.browsePopular,
                onViewAll = { viewModel.viewAllBrowseSection("POPULARITY_DESC") },
                onSelectMedia = viewModel::selectMedia,
            )
        }
        item {
            BrowseMangaShelf(
                title = "POPULAR MANHWA",
                media = state.browsePopularManhwa,
                onViewAll = viewModel::viewAllPopularManhwa,
                onSelectMedia = viewModel::selectMedia,
            )
        }
        item {
            BrowseTopMangaSection(
                media = state.browseTopManga,
                onViewAll = { viewModel.viewAllBrowseSection("SCORE_DESC") },
                onSelectMedia = viewModel::selectMedia,
            )
        }
    }
}

@Composable
private fun BrowseMangaShelf(
    title: String,
    media: List<AnilistMedia>,
    onViewAll: () -> Unit,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onViewAll) {
                Text("View All")
            }
        }
        if (media.isEmpty()) {
            Text(
                "Cached discovery will appear here after AniList responds.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                items(media, key = { it.id }) { item ->
                    BrowseShelfTile(media = item, onClick = { onSelectMedia(item) })
                }
            }
        }
    }
}

@Composable
private fun BrowseShelfTile(media: AnilistMedia, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(190.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
        ) {
            CoverImage(
                url = media.coverImage,
                title = media.title.userPreferred,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(12.dp),
                shape = RoundedCornerShape(12.dp),
                color = media.status.statusColor(),
            ) {}
            Text(
                text = media.title.userPreferred,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BrowseTopMangaSection(
    media: List<AnilistMedia>,
    onViewAll: () -> Unit,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "TOP 100 MANGA",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onViewAll) {
                Text("View All")
            }
        }
        if (media.isEmpty()) {
            Text(
                "Cached rankings will appear here after AniList responds.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                media.forEachIndexed { index, item ->
                    BrowseRankedMangaRow(
                        rank = index + 1,
                        media = item,
                        onClick = { onSelectMedia(item) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseRankedMangaRow(rank: Int, media: AnilistMedia, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "#$rank",
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ElevatedCard(
            onClick = onClick,
            modifier = Modifier.weight(1f),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 720.dp) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            BrowseRankCover(media)
                            BrowseRankTitle(media = media, modifier = Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            BrowseRankMeta(
                                primary = media.averageScore?.let { "$it%" } ?: "-",
                                secondary = media.popularity?.let { "${it.formatCompact()} users" } ?: "users unknown",
                                modifier = Modifier.weight(1f),
                            )
                            BrowseRankMeta(
                                primary = media.format.mediaFormatLabel(),
                                secondary = media.chapters?.let { "$it chapters" } ?: media.status.statusLabel(),
                                modifier = Modifier.weight(1f),
                            )
                            BrowseRankMeta(
                                primary = media.publishingYearLabel(),
                                secondary = media.status.statusLabel(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        BrowseRankCover(media)
                        BrowseRankTitle(
                            media = media,
                            modifier = Modifier
                                .weight(1.5f)
                                .widthIn(min = 220.dp),
                        )
                        BrowseRankMeta(
                            primary = media.averageScore?.let { "$it%" } ?: "-",
                            secondary = media.popularity?.let { "${it.formatCompact()} users" } ?: "users unknown",
                            modifier = Modifier.weight(0.85f),
                        )
                        BrowseRankMeta(
                            primary = media.format.mediaFormatLabel(),
                            secondary = media.chapters?.let { "$it chapters" } ?: media.status.statusLabel(),
                            modifier = Modifier.weight(0.85f),
                        )
                        BrowseRankMeta(
                            primary = media.publishingYearLabel(),
                            secondary = media.status.statusLabel(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseRankCover(media: AnilistMedia) {
    CoverImage(
        url = media.coverImage,
        title = media.title.userPreferred,
        modifier = Modifier.size(width = 64.dp, height = 90.dp),
    )
}

@Composable
private fun BrowseRankTitle(media: AnilistMedia, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            media.title.userPreferred,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            media.genres.take(5).forEach { genre ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ) {
                    Text(
                        genre.lowercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseRankMeta(primary: String, secondary: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            secondary,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BrowseResults(
    state: TankobunUiState,
    viewModel: MainViewModel,
    modifier: Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (state.searchQuery.isBlank()) "Browse Manga" else "Search Results",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    browseSummary(state),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = viewModel::resetBrowseFilters) {
                Text("Reset")
            }
        }
        MediaCollection(
            media = state.searchResults,
            viewMode = state.browseViewMode,
            coverColumns = state.browseCoverColumns,
            showWholeCovers = state.browseShowWholeCovers,
            modifier = Modifier.weight(1f),
            onSelectMedia = viewModel::selectMedia,
            emptyMessage = if (state.busy) {
                "Searching AniList..."
            } else {
                "No AniList manga found for these filters."
            },
        )
    }
}

@Composable
private fun BrowseGenreDialog(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .fillMaxHeight(0.72f),
            shape = RoundedCornerShape(12.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DialogHeader(title = "Genres", onDismiss = onDismiss)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    gridItems(BrowseGenres, key = { it }) { genre ->
                        FilterChip(
                            selected = genre in state.browseGenres,
                            onClick = { viewModel.setBrowseGenre(genre, genre !in state.browseGenres) },
                            label = {
                                Text(genre, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            state.browseGenres.forEach { viewModel.setBrowseGenre(it, false) }
                        },
                    ) {
                        Text("Clear")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            onDismiss()
                            viewModel.searchAniList()
                        },
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseTagDialog(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visibleTags = state.browseAvailableTags.visibleTags(query)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .fillMaxHeight(0.78f),
            shape = RoundedCornerShape(18.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DialogHeader(title = "Tags", onDismiss = onDismiss)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Find a tag") },
                    shape = RoundedCornerShape(16.dp),
                )
                if (state.browseAvailableTags.isEmpty()) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "Tags will appear here after AniList responds.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = { viewModel.loadBrowseTags(force = true) }) {
                            Text("Refresh tags")
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(142.dp),
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        gridItems(visibleTags, key = { it.name }) { tag ->
                            FilterChip(
                                selected = tag.name in state.browseTags,
                                onClick = { viewModel.setBrowseTag(tag.name, tag.name !in state.browseTags) },
                                label = {
                                    Text(
                                        tag.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            state.browseTags.forEach { viewModel.setBrowseTag(it, false) }
                        },
                    ) {
                        Text("Clear")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            onDismiss()
                            viewModel.searchAniList()
                        },
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseOptionDialog(
    title: String,
    options: List<BrowseOption>,
    selectedValue: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(380.dp)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(18.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DialogHeader(title = title, onDismiss = onDismiss)
                options.forEach { option ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(option.value) },
                        color = if (option.value == selectedValue) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                    )
                    {
                        ListItem(
                            headlineContent = { Text(option.label) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryOptionsDialog(
    state: TankobunUiState,
    viewModel: MainViewModel,
    sort: String,
    onSortChange: (String?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.72f),
            shape = RoundedCornerShape(12.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                DialogHeader(title = "Library Options", onDismiss = onDismiss)
                Text("Sort", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRowCompat {
                    LibrarySortOptions.forEach { option ->
                        FilterChip(
                            selected = sort == option.value,
                            onClick = { onSortChange(option.value) },
                            label = { Text(option.label) },
                        )
                    }
                }
                Text("View", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                MediaViewModeRow(
                    selected = state.libraryViewMode,
                    onSelect = viewModel::setLibraryViewMode,
                )
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onReset) {
                        Text("Reset")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = onDismiss) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseAdvancedDialog(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.72f),
            shape = RoundedCornerShape(12.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                DialogHeader(title = "Browse Options", onDismiss = onDismiss)
                Text("Sort", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRowCompat {
                    BrowseSortOptions.forEach { option ->
                        FilterChip(
                            selected = state.browseSort == option.value,
                            onClick = { option.value?.let(viewModel::setBrowseSort) },
                            label = { Text(option.label) },
                        )
                    }
                }
                Text("View", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                MediaViewModeRow(
                    selected = state.browseViewMode,
                    onSelect = viewModel::setBrowseViewMode,
                )
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = viewModel::resetBrowseFilters) {
                        Text("Reset")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            onDismiss()
                            viewModel.searchAniList()
                        },
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverColumnsRow(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedColumns = selected.supportedCoverColumns()
    FlowRowCompat {
        (2..8).forEach { count ->
            FilterChip(
                selected = selectedColumns == count,
                onClick = { onSelect(count) },
                label = { Text(count.toString()) },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun CoverFramingRow(
    showWholeCover: Boolean,
    onShowWholeCoverChange: (Boolean) -> Unit,
) {
    FlowRowCompat {
        FilterChip(
            selected = !showWholeCover,
            onClick = { onShowWholeCoverChange(false) },
            label = { Text("Fill frame") },
        )
        FilterChip(
            selected = showWholeCover,
            onClick = { onShowWholeCoverChange(true) },
            label = { Text("Show whole cover") },
        )
    }
}

@Composable
private fun DialogHeader(title: String, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

private fun List<BrowseOption>.labelFor(value: String?): String =
    firstOrNull { it.value == value }?.label ?: "Any"

private fun List<AnilistMediaTag>.visibleTags(query: String): List<AnilistMediaTag> {
    val normalizedQuery = query.trim().lowercase()
    return asSequence()
        .filter { !it.isAdult }
        .filter { tag ->
            normalizedQuery.isBlank() ||
                tag.name.lowercase().contains(normalizedQuery) ||
                tag.category.orEmpty().lowercase().contains(normalizedQuery)
        }
        .sortedWith(compareBy<AnilistMediaTag> { it.category.orEmpty() }.thenBy { it.name })
        .toList()
}

private fun browseYearOptions(): List<BrowseOption> {
    val currentYear = java.time.Year.now().value
    return listOf(BrowseOption("Any", null)) +
        (currentYear downTo 1970).map { BrowseOption(it.toString(), it.toString()) }
}

private fun TankobunUiState.browseControlsActive(): Boolean =
    searchQuery.isNotBlank() ||
        browseGenres.isNotEmpty() ||
        browseTags.isNotEmpty() ||
        browseFormat != null ||
        browsePublishingStatus != null ||
        browseCountryOfOrigin != null ||
        browseYear != null ||
        browseSort != BROWSE_SORT_SEARCH_MATCH_UI

private fun browseSummary(state: TankobunUiState): String {
    val parts = buildList {
        state.searchQuery.trim().takeIf { it.isNotBlank() }?.let { add("Search \"$it\"") }
        if (state.browseGenres.isNotEmpty()) add(state.browseGenres.sorted().joinToString(", "))
        if (state.browseTags.isNotEmpty()) add(state.browseTags.sorted().joinToString(", "))
        state.browseFormat?.let { add(BrowseFormatOptions.labelFor(it)) }
        state.browsePublishingStatus?.let { add(BrowseStatusOptions.labelFor(it)) }
        state.browseCountryOfOrigin?.let { add(BrowseCountryOptions.labelFor(it)) }
        state.browseYear?.let { add(it.toString()) }
        BrowseSortOptions.labelFor(state.browseSort).takeIf { it != "Search Match" }?.let { add("Sort: $it") }
    }
    return parts.ifEmpty { listOf("AniList manga database") }.joinToString(" / ")
}

@Composable
private fun String?.statusColor(): Color = when (this) {
    "RELEASING" -> Color(0xFF7ED957)
    "FINISHED" -> Color(0xFFFF7A7A)
    "HIATUS" -> Color(0xFFFFB15C)
    "NOT_YET_RELEASED" -> MaterialTheme.colorScheme.tertiary
    "CANCELLED" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun String?.statusLabel(): String = when (this) {
    "RELEASING" -> "Releasing"
    "FINISHED" -> "Finished"
    "HIATUS" -> "Hiatus"
    "NOT_YET_RELEASED" -> "Not yet released"
    "CANCELLED" -> "Cancelled"
    else -> "Status unknown"
}

private fun String?.mediaFormatLabel(): String = when (this) {
    "MANGA" -> "Manga"
    "NOVEL" -> "Novel"
    "ONE_SHOT" -> "One Shot"
    else -> "Manga"
}

private fun AnilistMedia.publishingYearLabel(): String = when {
    startDateYear != null && endDateYear != null && startDateYear != endDateYear -> "$startDateYear - $endDateYear"
    startDateYear != null && status == "RELEASING" -> "Since $startDateYear"
    startDateYear != null -> startDateYear.toString()
    else -> "Date unknown"
}

private fun List<String>.authorLabel(): String =
    take(3).joinToString(", ").ifBlank { "Unknown" }

private fun Int.formatCompact(): String =
    when {
        this >= 1_000_000 -> "${this / 1_000_000}m"
        this >= 10_000 -> "${this / 1_000}k"
        else -> toString()
    }

@Composable
private fun MediaViewModeRow(
    selected: MediaViewMode,
    onSelect: (MediaViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedMode = selected.supportedMediaViewMode()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            MediaViewMode.COVER_GRID to "Cover only",
            MediaViewMode.COVER_WITH_INFO to "Cover + info",
            MediaViewMode.LIST to "List",
        ).forEach { (mode, label) ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onSelect(mode) },
                label = { Text(label) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaCollection(
    media: List<AnilistMedia>,
    viewMode: MediaViewMode,
    coverColumns: Int,
    showWholeCovers: Boolean,
    onSelectMedia: (AnilistMedia) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No manga here yet.",
) {
    if (media.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            Text(emptyMessage)
        }
        return
    }

    val supportedViewMode = viewMode.supportedMediaViewMode()
    when (supportedViewMode) {
        MediaViewMode.LIST -> LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(media, key = { it.id }) { item ->
                MediaRow(media = item, onClick = { onSelectMedia(item) })
            }
        }
        else -> LazyVerticalGrid(
            columns = GridCells.Fixed(coverColumns.supportedCoverColumns()),
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            gridItems(media, key = { it.id }) { item ->
                MediaCoverTile(
                    media = item,
                    viewMode = supportedViewMode,
                    showWholeCover = showWholeCovers,
                    onClick = { onSelectMedia(item) },
                )
            }
        }
    }
}

@Composable
private fun MediaCoverTile(
    media: AnilistMedia,
    viewMode: MediaViewMode,
    showWholeCover: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "Cover press scale",
    )
    val supportedViewMode = viewMode.supportedMediaViewMode()
    val coverModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(2f / 3f)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (showWholeCover) Color.Transparent else MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = if (pressed) 1.dp else 3.dp,
        ) {
            CoverImage(
                url = media.coverImage,
                title = media.title.userPreferred,
                modifier = coverModifier,
                contentScale = if (showWholeCover) ContentScale.Fit else ContentScale.Crop,
                imageAlignment = if (showWholeCover) Alignment.BottomCenter else Alignment.Center,
            )
        }
        if (supportedViewMode != MediaViewMode.COVER_GRID) {
            Text(
                media.title.userPreferred,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (supportedViewMode == MediaViewMode.COVER_WITH_INFO) {
                Text(
                    listOfNotNull(media.status, media.chapters?.let { "$it ch" }).joinToString(" / "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaRow(media: AnilistMedia, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick) {
        ListItem(
            headlineContent = {
                Text(media.title.userPreferred, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(
                    listOfNotNull(media.status, media.chapters?.let { "$it chapters" })
                        .joinToString(" / "),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                CoverImage(
                    url = media.coverImage,
                    title = media.title.userPreferred,
                    modifier = Modifier.size(width = 56.dp, height = 78.dp),
                )
            },
        )
    }
}

@Composable
private fun MangaDetailScreen(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (state.sourcePickerOpen) 8.dp else 0.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                MangaHeroSection(media)
            }

            if (state.selectedRecommendations.isNotEmpty()) {
                item {
                    RecommendationsSection(
                        recommendations = state.selectedRecommendations,
                        hasMore = state.selectedRecommendationsHasMore,
                        loadingMore = state.recommendationsLoading,
                        onLoadMore = viewModel::loadMoreRecommendations,
                        onSelectMedia = viewModel::selectMedia,
                    )
                }
            }

            item {
                state.message?.let {
                    Text(it, color = MaterialTheme.colorScheme.secondary)
                }
            }

            item {
                SourceSummarySection(state, viewModel, media)
            }

            item {
                var downloadActionsOpen by remember { mutableStateOf(false) }
                Text("Chapters", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                if (state.selectedSourceManga == null) {
                    Text("Choose a source first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val readingActionChapter = state.primaryReadingActionChapter()
                            if (readingActionChapter != null) {
                                Button(onClick = { viewModel.openChapter(readingActionChapter) }) {
                                    Text(if (state.latestProgress == null) "Start reading" else "Resume")
                                }
                            }
                            OutlinedButton(onClick = viewModel::loadChaptersForCurrentMatch) {
                                Text(if (state.sourceChapters.isEmpty()) "Load chapters" else "Refresh chapters")
                            }
                            Spacer(Modifier.weight(1f))
                            OutlinedButton(
                                onClick = { downloadActionsOpen = true },
                                enabled = state.sourceChapters.isNotEmpty(),
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Download")
                            }
                        }
                        if (state.selectingDownloadChapters) {
                            ChapterManualDownloadBar(
                                selectedCount = state.selectedDownloadChapterUrls.size,
                                onDownloadSelected = viewModel::downloadSelectedChapters,
                                onCancel = viewModel::cancelManualDownloadSelection,
                            )
                        }
                    }
                }
                if (downloadActionsOpen) {
                    ChapterDownloadActionsDialog(
                        keepNextTenDownloads = state.keepNextTenDownloads,
                        onDismiss = { downloadActionsOpen = false },
                        onDownloadAll = {
                            downloadActionsOpen = false
                            viewModel.downloadAllChapters()
                        },
                        onDownloadUnread = {
                            downloadActionsOpen = false
                            viewModel.downloadUnreadChapters()
                        },
                        onDownloadNextTen = {
                            downloadActionsOpen = false
                            viewModel.downloadNextTenChapters()
                        },
                        onKeepNextTenChange = viewModel::setKeepNextTenDownloads,
                        onSelectManually = {
                            downloadActionsOpen = false
                            viewModel.startManualDownloadSelection()
                        },
                    )
                }
            }

            if (state.sourceChapters.isEmpty()) {
                item {
                    Text("No chapters loaded yet.")
                }
            } else {
                items(state.sourceChapters, key = { "${it.sourceId}:${it.url}" }) { chapter ->
                    ChapterRow(
                        chapter = chapter,
                        viewModel = viewModel,
                        read = chapter.isReadBy(state.chapterProgress),
                        download = state.downloadForChapter(chapter),
                        selectingForDownload = state.selectingDownloadChapters,
                        selectedForDownload = chapter.url in state.selectedDownloadChapterUrls,
                        onToggleDownloadSelection = { viewModel.toggleDownloadChapterSelection(chapter) },
                    )
                }
            }
        }

        if (state.sourcePickerOpen) {
            SourcePickerDialog(state, viewModel, media)
        }
    }
}

@Composable
private fun ChapterManualDownloadBar(
    selectedCount: Int,
    onDownloadSelected: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.70f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "$selectedCount selected",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Button(
                onClick = onDownloadSelected,
                enabled = selectedCount > 0,
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download")
            }
        }
    }
}

@Composable
private fun ChapterDownloadActionsDialog(
    keepNextTenDownloads: Boolean,
    onDismiss: () -> Unit,
    onDownloadAll: () -> Unit,
    onDownloadUnread: () -> Unit,
    onDownloadNextTen: () -> Unit,
    onKeepNextTenChange: (Boolean) -> Unit,
    onSelectManually: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 440.dp),
            shape = RoundedCornerShape(16.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            tonalElevation = 3.dp,
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogHeader(title = "Download Chapters", onDismiss = onDismiss)
                ChapterDownloadActionRow(
                    title = "All chapters",
                    subtitle = "Queue every chapter from this source.",
                    onClick = onDownloadAll,
                )
                ChapterDownloadActionRow(
                    title = "Unread only",
                    subtitle = "Skip chapters already marked as read.",
                    onClick = onDownloadUnread,
                )
                ChapterDownloadActionRow(
                    title = "Next 10",
                    subtitle = "Queue the next unread chapters from your current progress.",
                    onClick = onDownloadNextTen,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onKeepNextTenChange(!keepNextTenDownloads) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Always keep next 10", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Automatically queue the next unread batch as you move through chapters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = keepNextTenDownloads,
                            onCheckedChange = onKeepNextTenChange,
                        )
                    }
                }
                ChapterDownloadActionRow(
                    title = "Select manually",
                    subtitle = "Choose chapters directly from the list.",
                    onClick = onSelectManually,
                )
            }
        }
    }
}

@Composable
private fun ChapterDownloadActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MangaHeroSection(media: AnilistMedia) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = LocalTankobunTokens.current.elevatedSurface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CoverImage(
                url = media.coverImage,
                title = media.title.userPreferred,
                modifier = Modifier
                    .width(250.dp)
                    .aspectRatio(2f / 3f),
                contentScale = ContentScale.Fit,
                imageAlignment = Alignment.TopCenter,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    media.title.userPreferred,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    listOfNotNull(
                        media.format.mediaFormatLabel(),
                        media.status.statusLabel(),
                        media.chapters?.let { "$it chapters" },
                        media.volumes?.let { "$it volumes" },
                        media.averageScore?.let { "$it% score" },
                    ).joinToString(" / "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRowCompat {
                    mangaInfoPill("Author", media.staff.authorLabel())
                    mangaInfoPill("Published", media.publishingYearLabel())
                    media.popularity?.let { mangaInfoPill("Readers", it.formatCompact()) }
                }
                Text(
                    media.description?.replace(Regex("<[^>]*>"), "").orEmpty(),
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                )
                val tags = media.tags.ifEmpty { media.genres }
                if (tags.isNotEmpty()) {
                    FlowRowCompat {
                        tags.take(10).forEach { tag ->
                            AssistChip(onClick = {}, label = { Text(tag) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun mangaInfoPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            "$label: $value",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AniListTrackingSection(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AniListStatusSelector(
            selected = state.trackingStatus,
            onSelected = viewModel::setTrackingStatus,
        )

        AniListCustomListSelector(
            availableLists = (state.anilistCustomLists + state.trackingCustomLists).distinctBy { it.lowercase() },
            selectedLists = state.trackingCustomLists,
            onListSelected = viewModel::setTrackingCustomListSelected,
            onAddList = viewModel::addTrackingCustomList,
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.trackingProgress,
                onValueChange = viewModel::setTrackingProgress,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Progress") },
                suffix = { Text("/ ${media.chapters ?: "?"}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            AniListScoreInput(
                scoreFormat = state.anilistScoreFormat,
                value = state.trackingScore,
                onValueChange = viewModel::setTrackingScore,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        OutlinedTextField(
            value = state.trackingNotes,
            onValueChange = viewModel::setTrackingNotes,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            label = { Text("Notes") },
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Private", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = state.trackingPrivate, onCheckedChange = viewModel::setTrackingPrivate)
            Spacer(Modifier.weight(1f))
            Button(onClick = viewModel::saveTracking, enabled = state.loggedIn) {
                Text(if (state.selectedListEntry == null) "Track manga" else "Save AniList")
            }
        }
        if (!state.loggedIn) {
            Text(
                "Connect AniList to track, rate, and organize this manga.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AniListStatusSelector(selected: MediaStatus, onSelected: (MediaStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(selected.displayName(), modifier = Modifier.weight(1f))
            Icon(Icons.Default.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(visible = expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                ) {
                    trackingStatuses().forEach { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelected(status)
                                    expanded = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                status.displayName(),
                                modifier = Modifier.weight(1f),
                                fontWeight = if (status == selected) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (status == selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AniListCustomListSelector(
    availableLists: List<String>,
    selectedLists: Set<String>,
    onListSelected: (String, Boolean) -> Unit,
    onAddList: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    val selectedLabel = selectedLists
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")
        ?: "Custom lists"

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedLabel, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(visible = expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    if (availableLists.isEmpty()) {
                        Text(
                            "No custom lists yet.",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        availableLists.forEach { listName ->
                            val selected = selectedLists.any { it.equals(listName, ignoreCase = true) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onListSelected(listName, !selected) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Checkbox(checked = selected, onCheckedChange = { onListSelected(listName, it) })
                                Text(
                                    listName,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = newListName,
                            onValueChange = { newListName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("New list") },
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    onAddList(newListName)
                                    newListName = ""
                                },
                                enabled = newListName.isNotBlank(),
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AniListScoreInput(
    scoreFormat: AnilistScoreFormat,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (scoreFormat) {
        AnilistScoreFormat.POINT_5 -> StarScoreInput(value, onValueChange, modifier)
        AnilistScoreFormat.POINT_3 -> MoodScoreInput(value, onValueChange, modifier)
        else -> OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            singleLine = true,
            label = { Text(scoreFormat.scoreLabel()) },
            suffix = { Text(scoreFormat.scoreSuffix()) },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (scoreFormat == AnilistScoreFormat.POINT_10_DECIMAL) {
                    KeyboardType.Decimal
                } else {
                    KeyboardType.Number
                },
            ),
        )
    }
}

@Composable
private fun StarScoreInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val selected = value.toDoubleOrNull()?.roundToInt() ?: 0
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Score", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            (1..5).forEach { star ->
                IconButton(
                    onClick = { onValueChange(if (selected == star) "" else star.toString()) },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        if (star <= selected) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "$star star score",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodScoreInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val selected = value.toDoubleOrNull()?.roundToInt() ?: 0
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(1 to ":(", 2 to ":|", 3 to ":)").forEach { (score, label) ->
            FilterChip(
                selected = selected == score,
                onClick = { onValueChange(if (selected == score) "" else score.toString()) },
                label = { Text(label) },
            )
        }
    }
}

private fun AnilistScoreFormat.scoreLabel(): String = when (this) {
    AnilistScoreFormat.POINT_100 -> "Score"
    AnilistScoreFormat.POINT_10_DECIMAL -> "Score"
    AnilistScoreFormat.POINT_10 -> "Score"
    AnilistScoreFormat.POINT_5 -> "Score"
    AnilistScoreFormat.POINT_3 -> "Score"
}

private fun AnilistScoreFormat.scoreSuffix(): String = when (this) {
    AnilistScoreFormat.POINT_100 -> "/ 100"
    AnilistScoreFormat.POINT_10_DECIMAL,
    AnilistScoreFormat.POINT_10 -> "/ 10"
    AnilistScoreFormat.POINT_5 -> "/ 5"
    AnilistScoreFormat.POINT_3 -> "/ 3"
}

@Composable
private fun RecommendationsSection(
    recommendations: List<AnilistRecommendation>,
    hasMore: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Recommendations",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${recommendations.size} shown",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasMore) {
                TextButton(onClick = onLoadMore, enabled = !loadingMore) {
                    Text(if (loadingMore) "Loading" else "Load more")
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val visibleCount = when {
                maxWidth >= 840.dp -> 7
                maxWidth >= 600.dp -> 5
                else -> 3
            }
            val tileSpacing = 12.dp
            val tileWidth = ((maxWidth - tileSpacing * (visibleCount - 1).toFloat()) / visibleCount.toFloat())
                .coerceIn(92.dp, 132.dp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(tileSpacing)) {
                items(recommendations, key = { it.media.id }) { recommendation ->
                    RecommendationTile(
                        recommendation = recommendation,
                        onClick = { onSelectMedia(recommendation.media) },
                        modifier = Modifier.width(tileWidth),
                    )
                }
                if (hasMore) {
                    item {
                        LoadMoreRecommendationsTile(
                            loading = loadingMore,
                            onClick = onLoadMore,
                            modifier = Modifier.width(tileWidth),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationTile(
    recommendation: AnilistRecommendation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val media = recommendation.media
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(7.dp),
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
        ) {
            CoverImage(
                url = media.coverImage,
                title = media.title.userPreferred,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
            )
        }
        Text(
            media.title.userPreferred,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = media.status.statusColor(),
            ) {}
            Text(
                listOfNotNull(
                    media.format.mediaFormatLabel(),
                    recommendation.rating?.let { "$it votes" },
                ).joinToString(" / "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LoadMoreRecommendationsTile(
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clickable(enabled = !loading, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Loading", style = MaterialTheme.typography.labelMedium)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(
                    "Load more",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SourceSummarySection(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
    val selectedManga = state.selectedSourceManga
    val selectedSource = state.selectedSource
    val latestProgress = state.latestProgress

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Source", style = MaterialTheme.typography.titleLarge)
        if (selectedManga == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = viewModel::openSourcePicker) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Find source")
                }
                Text(
                    if (state.allInstalledSources.isEmpty()) "Install source extensions in Settings." else "No source selected.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            ElevatedCard {
                ListItem(
                    headlineContent = {
                        Text(selectedManga.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    supportingContent = {
                        Column {
                            Text(
                                listOfNotNull(
                                    selectedSource?.let { "${it.name} (${it.lang})" },
                                    state.sourceChapters.takeIf { it.isNotEmpty() }?.let { "${it.size} chapters" },
                                ).joinToString(" / "),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (latestProgress != null) {
                                Text(
                                    "Last read: chapter ${latestProgress.chapterNumber.takeIf { it > 0 } ?: "?"}, page ${latestProgress.pageIndex + 1}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    leadingContent = {
                        CoverImage(
                            url = selectedManga.thumbnailUrl ?: media.coverImage,
                            title = selectedManga.title,
                            modifier = Modifier.size(width = 50.dp, height = 70.dp),
                        )
                    },
                    trailingContent = {
                        Button(onClick = viewModel::openSourcePicker) {
                            Text("Change")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SourcePickerDialog(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
    val matches = state.sourceMatches.filter { match ->
        state.sourceMatchChapterCounts[sourceMatchKey(match.source.id, match.manga.url)] != null
    }
    val availableSources = remember(state.installedSources, state.selectedSourceId) {
        state.installedSources
            .distinctBy { it.sourceSettingsKey() }
            .sortedWith(
                compareBy<SourceDescriptor> { if (it.id == state.selectedSourceId) 0 else 1 }
                    .thenBy { sourceLanguageSortPriority(it.lang.normalizedSourceLanguage()) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.lang },
            )
    }
    val matchSourceKeys = remember(matches) {
        matches.mapTo(mutableSetOf()) { it.source.sourceSettingsKey() }
    }
    val diagnostics = state.sourcePickerDiagnostics

    Dialog(
        onDismissRequest = viewModel::closeSourcePicker,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Find source", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${media.title.userPreferred} / ${availableSources.size} enabled sources",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = viewModel::closeSourcePicker) {
                        Text("Close")
                    }
                }

                if (state.sourcePickerLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                state.sourcePickerMessage?.let { pickerMessage ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(
                            pickerMessage,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (matches.isEmpty() && availableSources.isEmpty() && !state.sourcePickerLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No enabled sources.", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Enable or install sources from Settings.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (matches.isNotEmpty()) {
                            item {
                                Text("Readable matches", style = MaterialTheme.typography.titleMedium)
                            }
                            items(matches, key = { "match:${it.source.id}:${it.manga.url}" }) { match ->
                                val count = state.sourceMatchChapterCounts[sourceMatchKey(match.source.id, match.manga.url)] ?: 0
                                SourceMatchRow(
                                    match = match,
                                    chapterCount = count,
                                    current = state.selectedSourceId == match.source.id &&
                                        state.selectedSourceManga?.url == match.manga.url,
                                    mediaCover = media.coverImage,
                                    onClick = { viewModel.bindSourceMatch(match) },
                                )
                            }
                        }
                        val fallbackSources = availableSources.filterNot { it.sourceSettingsKey() in matchSourceKeys }
                        if (fallbackSources.isNotEmpty()) {
                            item {
                                Text("Try a specific source", style = MaterialTheme.typography.titleMedium)
                            }
                            items(fallbackSources, key = { "source:${it.sourceSettingsKey()}" }) { source ->
                                SourceCandidateRow(
                                    source = source,
                                    current = state.selectedSourceId == source.id,
                                    onClick = { viewModel.bindSource(source) },
                                )
                            }
                        }
                        if (diagnostics.isNotEmpty()) {
                            item {
                                Text("Skipped sources", style = MaterialTheme.typography.titleMedium)
                            }
                            items(diagnostics, key = { "diagnostic:$it" }) { diagnostic ->
                                SourceDiagnosticRow(diagnostic)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { viewModel.findSourceMatches(forceRefresh = true) }) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Refresh")
                    }
                    if (state.sourcePickerLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                    Text(
                        "${matches.size} readable / ${availableSources.size} enabled",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceDiagnosticRow(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SourceMatchRow(
    match: SourceSearchResult,
    chapterCount: Int,
    current: Boolean,
    mediaCover: String?,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick) {
        ListItem(
            headlineContent = { Text(match.manga.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Text(
                    "${match.source.name} (${match.source.lang}) / $chapterCount chapters / ${(match.score * 100).toInt()}% match",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                CoverImage(
                    url = match.manga.thumbnailUrl ?: mediaCover,
                    title = match.manga.title,
                    modifier = Modifier.size(width = 48.dp, height = 68.dp),
                )
            },
            trailingContent = {
                if (current) {
                    Text("Current", color = MaterialTheme.colorScheme.secondary)
                }
            },
        )
    }
}

@Composable
private fun SourceCandidateRow(
    source: SourceDescriptor,
    current: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick) {
        ListItem(
            headlineContent = { Text(source.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Text(
                    sourceMetadata(source, active = true),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                ExtensionIcon(
                    packageName = source.packageName,
                    name = source.name,
                    iconUrl = null,
                    modifier = Modifier.size(42.dp),
                )
            },
            trailingContent = {
                Text(if (current) "Selected" else "Try", color = MaterialTheme.colorScheme.secondary)
            },
        )
    }
}
private fun sourceMatchKey(sourceId: Long, mangaUrl: String): String =
    "$sourceId:$mangaUrl"

private fun trackingStatuses(): List<MediaStatus> = listOf(
    MediaStatus.CURRENT,
    MediaStatus.PLANNING,
    MediaStatus.COMPLETED,
    MediaStatus.PAUSED,
    MediaStatus.DROPPED,
    MediaStatus.REPEATING,
)

private fun MediaStatus.displayName(): String = when (this) {
    MediaStatus.CURRENT -> "Reading"
    MediaStatus.PLANNING -> "Plan"
    MediaStatus.COMPLETED -> "Completed"
    MediaStatus.PAUSED -> "Paused"
    MediaStatus.DROPPED -> "Dropped"
    MediaStatus.REPEATING -> "Rereading"
    MediaStatus.UNKNOWN -> "Unknown"
}

@Composable
private fun CoverImage(
    url: String?,
    title: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    imageAlignment: Alignment = Alignment.Center,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (url.isNullOrBlank()) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            alignment = imageAlignment,
        )
        if (url.isNullOrBlank()) {
            Text(
                title.take(1),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun TankobunUiState.primaryReadingActionChapter(): SourceChapter? =
    latestProgress
        ?.let { progress -> sourceChapters.firstOrNull { it.url == progress.chapterUrl } }
        ?: sourceChapters
            .filter { it.chapterNumber > 0f }
            .minByOrNull { it.chapterNumber }
        ?: sourceChapters.lastOrNull()

private fun TankobunUiState.nextReaderChapter(): SourceChapter? =
    sourceChapters.nextInReadingOrderAfter(activeChapter ?: return null)

private fun SourceChapter.isReadBy(progressByChapter: Map<String, ReadingProgress>): Boolean =
    progressByChapter[url]?.completed == true

private fun TankobunUiState.downloadForChapter(chapter: SourceChapter): DownloadJob? {
    val mediaId = selectedMedia?.id ?: return null
    return downloads
        .filter { it.mediaId == mediaId && it.sourceId == chapter.sourceId && it.chapterUrl == chapter.url }
        .maxByOrNull { it.updatedAtEpochMillis }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterRow(
    chapter: SourceChapter,
    viewModel: MainViewModel,
    read: Boolean,
    download: DownloadJob?,
    selectingForDownload: Boolean,
    selectedForDownload: Boolean,
    onToggleDownloadSelection: () -> Unit,
) {
    key(chapter.url, read, download?.state, download?.completedPages, download?.pageCount, selectingForDownload, selectedForDownload) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd,
                    SwipeToDismissBoxValue.EndToStart -> {
                        viewModel.setChapterRead(chapter, read = !read)
                        false
                    }

                    SwipeToDismissBoxValue.Settled -> false
                }
            },
            positionalThreshold = { distance -> distance * 0.32f },
        )
        @Composable
        fun ChapterCard() {
            ElevatedCard(
                onClick = {
                    if (selectingForDownload) {
                        onToggleDownloadSelection()
                    } else {
                        viewModel.openChapter(chapter)
                    }
                },
                modifier = Modifier.graphicsLayer {
                    alpha = if (read) 0.70f else 1f
                },
            ) {
                ListItem(
                    leadingContent = if (selectingForDownload) {
                        {
                            Checkbox(
                                checked = selectedForDownload,
                                onCheckedChange = { onToggleDownloadSelection() },
                            )
                        }
                    } else {
                        null
                    },
                    headlineContent = {
                        Text(chapter.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    trailingContent = {
                        ChapterDownloadIndicator(
                            download = download,
                            onDownload = { viewModel.enqueueDownload(chapter) },
                            onResume = { download?.let { viewModel.resumeDownload(it.id) } },
                            onRetry = { download?.let { viewModel.retryDownload(it.id) } },
                        )
                    },
                )
            }
        }
        if (selectingForDownload) {
            ChapterCard()
        } else {
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = true,
                backgroundContent = {},
            ) {
                ChapterCard()
            }
        }
    }
}

@Composable
private fun ChapterDownloadIndicator(
    download: DownloadJob?,
    onDownload: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        download == null -> IconButton(onClick = onDownload) {
            Icon(Icons.Default.Download, contentDescription = "Download")
        }

        download.state == DownloadState.COMPLETE -> Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Downloaded",
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        download.state == DownloadState.QUEUED || download.state == DownloadState.RUNNING -> {
            val progress = remember(download.completedPages, download.pageCount) {
                if (download.pageCount > 0) {
                    (download.completedPages.toFloat() / download.pageCount).coerceIn(0f, 1f)
                } else {
                    null
                }
            }
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (progress == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                    )
                }
            }
        }

        download.state == DownloadState.PAUSED -> IconButton(onClick = onResume) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Resume download")
        }

        download.state == DownloadState.FAILED -> IconButton(onClick = onRetry) {
            Icon(Icons.Default.Replay, contentDescription = "Retry download")
        }
    }
}

@Composable
private fun FullScreenReader(state: TankobunUiState, viewModel: MainViewModel) {
    val chapter = state.activeChapter ?: return
    if (state.readerPages.isEmpty()) return
    var controlsVisible by remember { mutableStateOf(false) }
    val transformKey = if (state.readerMode == ReaderMode.WEBTOON) {
        "${state.selectedMedia?.id}:${state.selectedSourceId}:webtoon"
    } else {
        "${chapter.url}:${state.readerMode}:${state.currentPageIndex}"
    }
    var readerScale by remember(transformKey) { mutableStateOf(1f) }
    var readerOffset by remember(transformKey) { mutableStateOf(Offset.Zero) }
    val coroutineScope = rememberCoroutineScope()
    var flingJob by remember(transformKey) { mutableStateOf<Job?>(null) }
    var zoomAnimationJob by remember(transformKey) { mutableStateOf<Job?>(null) }
    val pageGap = readerPageGap(state.readerPageGapLevel)
    val webtoonListState = rememberLazyListState()
    val zoomPercent = (readerScale * 100).toInt()
    val pageCount = state.readerPages.size
    val lastPageIndex = (pageCount - 1).coerceAtLeast(0)
    val nextChapter = state.nextReaderChapter()
    val webtoonPageItems = remember(
        state.readerPreviousSegment,
        chapter,
        state.readerPages,
        state.readerNextSegment,
    ) {
        buildList {
            state.readerPreviousSegment?.let { segment ->
                segment.pages.forEachIndexed { index, page ->
                    add(WebtoonReaderPageItem(segment.chapter, page, index))
                }
            }
            state.readerPages.forEachIndexed { index, page ->
                add(WebtoonReaderPageItem(chapter, page, index))
            }
            state.readerNextSegment?.let { segment ->
                segment.pages.forEachIndexed { index, page ->
                    add(WebtoonReaderPageItem(segment.chapter, page, index))
                }
            }
        }
    }
    val currentWebtoonStartIndex = state.readerPreviousSegment?.pages?.size ?: 0
    val canGoForward = state.currentPageIndex < lastPageIndex || nextChapter != null
    var scrubberValue by remember(chapter.url, pageCount) {
        mutableStateOf(state.currentPageIndex.coerceIn(0, lastPageIndex).toFloat())
    }
    var scrubberSeeking by remember(chapter.url) { mutableStateOf(false) }
    var webtoonInitialScrollDoneFor by remember { mutableStateOf<String?>(null) }
    var preserveWebtoonScrollOnChapterChange by remember { mutableStateOf(false) }
    val displayedPageIndex = scrubberValue.roundToInt().coerceIn(0, lastPageIndex)
    fun cancelFling() {
        flingJob?.cancel()
        flingJob = null
    }
    fun cancelZoomAnimation() {
        zoomAnimationJob?.cancel()
        zoomAnimationJob = null
    }
    fun stopReaderMotion() {
        cancelFling()
        cancelZoomAnimation()
    }
    fun animateReaderTransform(targetScale: Float, targetOffset: Offset) {
        stopReaderMotion()
        zoomAnimationJob = coroutineScope.launch {
            val startScale = readerScale
            val startOffset = readerOffset
            val startNanos = withFrameNanos { it }
            val durationNanos = 180_000_000L
            do {
                val frameNanos = withFrameNanos { it }
                val progress = ((frameNanos - startNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
                val eased = 1f - (1f - progress).pow(3)
                readerScale = readerLerp(startScale, targetScale, eased)
                readerOffset = Offset(
                    x = readerLerp(startOffset.x, targetOffset.x, eased),
                    y = readerLerp(startOffset.y, targetOffset.y, eased),
                )
            } while (progress < 1f)
            readerScale = targetScale
            readerOffset = targetOffset
            zoomAnimationJob = null
        }
    }
    fun resetZoom() {
        animateReaderTransform(1f, Offset.Zero)
    }
    fun launchReaderFling(velocity: Velocity, width: Float, height: Float, panAxis: ReaderPanAxis) {
        val initialVelocity = when (panAxis) {
            ReaderPanAxis.BOTH -> Offset(velocity.x, velocity.y)
            ReaderPanAxis.HORIZONTAL,
            ReaderPanAxis.WEBTOON -> Offset(velocity.x, 0f)
        }
        if (readerScale <= 1.01f || (abs(initialVelocity.x) < 90f && abs(initialVelocity.y) < 90f)) return
        stopReaderMotion()
        flingJob = coroutineScope.launch {
            var velocityOffset = initialVelocity
            var lastFrameNanos = 0L
            while (abs(velocityOffset.x) > 20f || abs(velocityOffset.y) > 20f) {
                val frameNanos = withFrameNanos { it }
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameNanos
                    continue
                }

                val deltaSeconds = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastFrameNanos = frameNanos
                val proposedOffset = readerOffset + velocityOffset * deltaSeconds
                val clampedOffset = proposedOffset.clampedReaderOffset(readerScale, width, height)
                readerOffset = when (panAxis) {
                    ReaderPanAxis.BOTH -> clampedOffset
                    ReaderPanAxis.HORIZONTAL,
                    ReaderPanAxis.WEBTOON -> Offset(clampedOffset.x, 0f)
                }

                velocityOffset = Offset(
                    x = if (clampedOffset.x != proposedOffset.x) 0f else velocityOffset.x,
                    y = if (clampedOffset.y != proposedOffset.y || panAxis != ReaderPanAxis.BOTH) {
                        0f
                    } else {
                        velocityOffset.y
                    },
                )
                val decay = 0.88f.pow(deltaSeconds * 60f)
                velocityOffset *= decay
            }
        }
    }
    fun launchWebtoonFling(velocity: Velocity, width: Float, height: Float) {
        val horizontalVelocity = velocity.x
        val verticalVelocity = -velocity.y / readerScale.coerceAtLeast(1f)
        if (readerScale <= 1.01f || (abs(horizontalVelocity) < 90f && abs(verticalVelocity) < 90f)) return
        stopReaderMotion()
        flingJob = coroutineScope.launch {
            var velocityX = horizontalVelocity
            var scrollVelocityY = verticalVelocity
            var lastFrameNanos = 0L
            while (abs(velocityX) > 20f || abs(scrollVelocityY) > 20f) {
                val frameNanos = withFrameNanos { it }
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameNanos
                    continue
                }

                val deltaSeconds = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastFrameNanos = frameNanos

                if (abs(velocityX) > 20f) {
                    val proposedOffset = readerOffset + Offset(velocityX * deltaSeconds, 0f)
                    val clampedOffset = proposedOffset.clampedReaderOffset(readerScale, width, height)
                    readerOffset = Offset(clampedOffset.x, 0f)
                    if (clampedOffset.x != proposedOffset.x) velocityX = 0f
                }

                if (abs(scrollVelocityY) > 20f) {
                    webtoonListState.dispatchRawDelta(scrollVelocityY * deltaSeconds)
                }

                val decay = 0.88f.pow(deltaSeconds * 60f)
                velocityX *= decay
                scrollVelocityY *= decay
            }
        }
    }
    fun goToReaderPage(index: Int) {
        val targetIndex = index.coerceIn(0, lastPageIndex)
        scrubberValue = targetIndex.toFloat()
        resetZoom()
        viewModel.setReaderPage(targetIndex)
        if (state.readerMode == ReaderMode.WEBTOON) {
            coroutineScope.launch {
                webtoonListState.animateScrollToItem(currentWebtoonStartIndex + targetIndex)
            }
        }
    }
    fun commitScrubbedPage() {
        goToReaderPage(scrubberValue.roundToInt())
    }
    fun moveReaderPageFromControls(delta: Int) {
        val targetIndex = state.currentPageIndex + delta
        if (delta > 0 && targetIndex > lastPageIndex && nextChapter != null) {
            resetZoom()
            viewModel.openNextChapter()
        } else {
            goToReaderPage(targetIndex)
        }
    }

    DisposableEffect(transformKey) {
        onDispose { stopReaderMotion() }
    }

    LaunchedEffect(chapter.url, state.currentPageIndex, pageCount, scrubberSeeking) {
        if (!scrubberSeeking) {
            scrubberValue = state.currentPageIndex.coerceIn(0, lastPageIndex).toFloat()
        }
    }

    LaunchedEffect(scrubberSeeking, scrubberValue) {
        if (scrubberSeeking) {
            delay(420L)
            scrubberSeeking = false
        }
    }

    LaunchedEffect(
        chapter.url,
        state.readerMode,
    ) {
        if (state.readerMode == ReaderMode.WEBTOON && state.currentPageIndex > 0) {
            if (preserveWebtoonScrollOnChapterChange) {
                preserveWebtoonScrollOnChapterChange = false
                webtoonInitialScrollDoneFor = chapter.url
            } else if (webtoonInitialScrollDoneFor != chapter.url) {
                webtoonInitialScrollDoneFor = chapter.url
                webtoonListState.scrollToItem(currentWebtoonStartIndex + state.currentPageIndex.coerceIn(0, lastPageIndex))
            }
        } else if (state.readerMode == ReaderMode.WEBTOON) {
            if (preserveWebtoonScrollOnChapterChange) {
                preserveWebtoonScrollOnChapterChange = false
                webtoonInitialScrollDoneFor = chapter.url
            } else if (webtoonInitialScrollDoneFor != chapter.url) {
                webtoonInitialScrollDoneFor = chapter.url
                if (currentWebtoonStartIndex > 0) {
                    webtoonListState.scrollToItem(currentWebtoonStartIndex)
                }
            }
        }
    }

    LaunchedEffect(chapter.url, state.readerMode, webtoonPageItems) {
        if (state.readerMode == ReaderMode.WEBTOON) {
            snapshotFlow {
                webtoonPageItems.getOrNull(webtoonListState.firstVisibleItemIndex)
            }
                .distinctUntilChanged()
                .collect { item ->
                    if (item != null && !scrubberSeeking) {
                        if (item.chapter.url != chapter.url) {
                            preserveWebtoonScrollOnChapterChange = true
                        }
                        viewModel.setWebtoonReaderPosition(item.chapter.url, item.pageIndex)
                    }
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(transformKey, controlsVisible, readerScale) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        val nextScale = if (readerScale > 1.05f) 1f else 2.5f
                        val nextOffset = if (nextScale == 1f) {
                            Offset.Zero
                        } else {
                            val zoomOffset = readerDoubleTapOffset(
                                tapOffset = tapOffset,
                                scale = nextScale,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                            )
                            if (state.readerMode == ReaderMode.WEBTOON) Offset(zoomOffset.x, 0f) else zoomOffset
                        }
                        animateReaderTransform(nextScale, nextOffset)
                    },
                    onTap = { offset ->
                        val centerX = size.width / 3f..size.width * 2f / 3f
                        val centerY = size.height / 3f..size.height * 2f / 3f
                        when {
                            offset.x in centerX && offset.y in centerY -> controlsVisible = !controlsVisible
                            !controlsVisible &&
                                readerScale <= 1.05f &&
                                state.readerMode == ReaderMode.PAGED &&
                                offset.x < size.width / 3f -> viewModel.moveReaderPage(-1)
                            !controlsVisible &&
                                readerScale <= 1.05f &&
                                state.readerMode == ReaderMode.PAGED &&
                                offset.x > size.width * 2f / 3f -> viewModel.moveReaderPage(1)
                        }
                    },
                )
            },
    ) {
        if (state.readerMode == ReaderMode.WEBTOON) {
            LazyColumn(
                state = webtoonListState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(transformKey) {
                        detectReaderTransformGestures(
                            scaleProvider = { readerScale },
                            panAxis = ReaderPanAxis.WEBTOON,
                            onGestureStart = {
                                stopReaderMotion()
                            },
                            onGestureEnd = { velocity, width, height ->
                                launchWebtoonFling(velocity, width, height)
                            },
                        ) { centroid, pan, zoom ->
                            val nextScale = (readerScale * zoom).coerceIn(1f, 5f)
                            val nextOffset = readerTransformOffset(
                                currentOffset = readerOffset,
                                centroid = centroid,
                                pan = Offset(pan.x, 0f),
                                scale = readerScale,
                                nextScale = nextScale,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                            )
                            readerScale = nextScale
                            readerOffset = Offset(nextOffset.x, 0f)
                            if (zoom == 1f && pan.y != 0f) {
                                webtoonListState.dispatchRawDelta(-pan.y / nextScale.coerceAtLeast(1f))
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = readerScale
                        scaleY = readerScale
                        translationX = readerOffset.x
                        translationY = 0f
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(pageGap),
            ) {
                itemsIndexed(
                    webtoonPageItems,
                    key = { _, item -> "${item.chapter.url}:${item.page.index}:${item.page.imageUrl}" },
                ) { _, item ->
                    AsyncImage(
                        model = readerImageRequest(item.page),
                        contentDescription = item.chapter.name,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth,
                    )
                }
                if (nextChapter != null && state.readerNextSegment == null) {
                    item(key = "next:${nextChapter.url}") {
                        WebtoonNextChapterFooter(nextChapter = nextChapter)
                    }
                }
            }
        } else {
            val page = state.readerPages[state.currentPageIndex]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(transformKey) {
                        detectReaderTransformGestures(
                            scaleProvider = { readerScale },
                            panAxis = ReaderPanAxis.BOTH,
                            onGestureStart = ::stopReaderMotion,
                            onGestureEnd = { velocity, width, height ->
                                launchReaderFling(velocity, width, height, ReaderPanAxis.BOTH)
                            },
                        ) { centroid, pan, zoom ->
                            val nextScale = (readerScale * zoom).coerceIn(1f, 5f)
                            val nextOffset = readerTransformOffset(
                                currentOffset = readerOffset,
                                centroid = centroid,
                                pan = pan,
                                scale = readerScale,
                                nextScale = nextScale,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                            )
                            readerScale = nextScale
                            readerOffset = nextOffset
                        }
                    }
                    .graphicsLayer {
                        scaleX = readerScale
                        scaleY = readerScale
                        translationX = readerOffset.x
                        translationY = readerOffset.y
                    },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = readerImageRequest(page),
                    contentDescription = chapter.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (state.readerPageGapLevel == 0) 8.dp else pageGap),
                    contentScale = if (state.readerFitWidth) ContentScale.FillWidth else ContentScale.Fit,
                )
            }
        }

        if (controlsVisible) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.safeDrawing.asPaddingValues()),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = LocalTankobunTokens.current.readerOverlay,
                ) {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = viewModel::closeReader) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close reader",
                                tint = Color.White,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                chapter.name,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${if (state.readerMode == ReaderMode.WEBTOON) "Webtoon" else "Paged"} / Page ${state.currentPageIndex + 1} of $pageCount",
                                color = Color.White.copy(alpha = 0.74f),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = LocalTankobunTokens.current.readerOverlay,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                enabled = state.currentPageIndex > 0,
                                onClick = { moveReaderPageFromControls(-1) },
                            ) {
                                Icon(
                                    Icons.Default.SkipPrevious,
                                    contentDescription = "Previous page",
                                    tint = Color.White.copy(alpha = if (state.currentPageIndex > 0) 1f else 0.34f),
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Page ${displayedPageIndex + 1}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    Text(
                                        "$pageCount pages",
                                        color = Color.White.copy(alpha = 0.68f),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                                if (pageCount > 1) {
                                    Slider(
                                        value = scrubberValue.coerceIn(0f, lastPageIndex.toFloat()),
                                        onValueChange = {
                                            val nextValue = it.coerceIn(0f, lastPageIndex.toFloat())
                                            val nextIndex = nextValue.roundToInt().coerceIn(0, lastPageIndex)
                                            scrubberSeeking = true
                                            scrubberValue = nextValue
                                            if (nextIndex != state.currentPageIndex) {
                                                resetZoom()
                                                viewModel.setReaderPage(nextIndex)
                                                if (state.readerMode == ReaderMode.WEBTOON) {
                                                    coroutineScope.launch {
                                                        webtoonListState.scrollToItem(currentWebtoonStartIndex + nextIndex)
                                                    }
                                                }
                                            }
                                        },
                                        onValueChangeFinished = {
                                            scrubberSeeking = false
                                            commitScrubbedPage()
                                        },
                                        valueRange = 0f..lastPageIndex.toFloat(),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                } else {
                                    LinearProgressIndicator(Modifier.fillMaxWidth())
                                }
                            }
                            IconButton(
                                enabled = canGoForward,
                                onClick = { moveReaderPageFromControls(1) },
                            ) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    contentDescription = if (state.currentPageIndex >= lastPageIndex && nextChapter != null) {
                                        "Next chapter"
                                    } else {
                                        "Next page"
                                    },
                                    tint = Color.White.copy(alpha = if (canGoForward) 1f else 0.34f),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FilterChip(
                                    selected = state.readerMode == ReaderMode.PAGED,
                                    onClick = {
                                        resetZoom()
                                        viewModel.setReaderMode(ReaderMode.PAGED)
                                    },
                                    label = { Text("Paged") },
                                )
                                FilterChip(
                                    selected = state.readerMode == ReaderMode.WEBTOON,
                                    onClick = {
                                        resetZoom()
                                        viewModel.setReaderMode(ReaderMode.WEBTOON)
                                    },
                                    label = { Text("Webtoon") },
                                )
                                FilterChip(
                                    selected = state.readerFitWidth,
                                    enabled = state.readerMode == ReaderMode.PAGED,
                                    onClick = {
                                        viewModel.setReaderFitWidth(!state.readerFitWidth)
                                        resetZoom()
                                    },
                                    label = { Text("Fit width") },
                                )
                                FilterChip(
                                    selected = state.readerPageGapLevel > 0,
                                    onClick = { viewModel.setReaderPageGapLevel((state.readerPageGapLevel + 1) % 4) },
                                    label = { Text(readerGapLabel(state.readerPageGapLevel)) },
                                )
                                FilterChip(
                                    selected = readerScale > 1.05f,
                                    onClick = { resetZoom() },
                                    label = { Text("Reset zoom") },
                                )
                            }
                            Text(
                                "zoom: $zoomPercent%",
                                color = Color.White.copy(alpha = 0.78f),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebtoonNextChapterFooter(nextChapter: SourceChapter) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
            color = Color.White.copy(alpha = 0.86f),
        )
        Text(
            "Loading ${nextChapter.name}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun readerPageGap(level: Int): Dp = when (level) {
    1 -> 8.dp
    2 -> 16.dp
    3 -> 24.dp
    else -> 0.dp
}

private fun readerGapLabel(level: Int): String = when (level) {
    1 -> "Small gaps"
    2 -> "Medium gaps"
    3 -> "Large gaps"
    else -> "No gaps"
}

private fun readerLerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

private fun readerDoubleTapOffset(
    tapOffset: Offset,
    scale: Float,
    width: Float,
    height: Float,
): Offset {
    val center = Offset(width / 2f, height / 2f)
    return ((center - tapOffset) * (scale - 1f)).clampedReaderOffset(scale, width, height)
}

private fun readerTransformOffset(
    currentOffset: Offset,
    centroid: Offset,
    pan: Offset,
    scale: Float,
    nextScale: Float,
    width: Float,
    height: Float,
): Offset {
    if (nextScale <= 1.01f) return Offset.Zero
    val center = Offset(width / 2f, height / 2f)
    val scaleChange = nextScale / scale.coerceAtLeast(0.01f)
    return (currentOffset * scaleChange + (centroid - center) * (1f - scaleChange) + pan)
        .clampedReaderOffset(nextScale, width, height)
}

private fun Offset.clampedReaderOffset(scale: Float, width: Float, height: Float): Offset {
    if (scale <= 1.01f) return Offset.Zero
    val maxX = width * (scale - 1f) / 2f
    val maxY = height * (scale - 1f) / 2f
    return Offset(
        x = x.coerceIn(-maxX, maxX),
        y = y.coerceIn(-maxY, maxY),
    )
}

private suspend fun PointerInputScope.detectReaderTransformGestures(
    scaleProvider: () -> Float,
    panAxis: ReaderPanAxis,
    onGestureStart: () -> Unit = {},
    onGestureEnd: (velocity: Velocity, width: Float, height: Float) -> Unit = { _, _, _ -> },
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
) {
    awaitEachGesture {
        val velocityTracker = VelocityTracker()
        awaitFirstDown(requireUnconsumed = false)
        var transforming = false
        var trackingVelocity = false
        do {
            val event = awaitPointerEvent()
            val pressedPointers = event.changes.count { it.pressed }
            if (pressedPointers == 0) break

            val currentScale = scaleProvider()
            val multiTouch = pressedPointers > 1
            val rawPan = event.calculatePan()
            val oneFingerZoomPan = currentScale > 1.01f && !multiTouch
            val singleFingerPanAllowed = when (panAxis) {
                ReaderPanAxis.BOTH,
                ReaderPanAxis.WEBTOON -> oneFingerZoomPan
                ReaderPanAxis.HORIZONTAL -> oneFingerZoomPan && abs(rawPan.x) > abs(rawPan.y)
            }
            val shouldTransform = multiTouch || singleFingerPanAllowed
            if (shouldTransform) {
                val zoom = if (multiTouch) event.calculateZoom() else 1f
                val pan = if (currentScale > 1.01f || transforming) rawPan.readerPanForAxis(panAxis) else Offset.Zero
                if (!transforming) {
                    transforming = true
                    onGestureStart()
                }
                if (singleFingerPanAllowed) {
                    val velocityChange = event.changes.firstOrNull { it.pressed }
                    if (velocityChange != null) {
                        if (!trackingVelocity) {
                            velocityTracker.resetTracking()
                            velocityTracker.addPosition(
                                velocityChange.previousUptimeMillis,
                                velocityChange.previousPosition.readerVelocityPosition(panAxis),
                            )
                            trackingVelocity = true
                        }
                        velocityTracker.addPosition(
                            velocityChange.uptimeMillis,
                            velocityChange.position.readerVelocityPosition(panAxis),
                        )
                    }
                } else {
                    trackingVelocity = false
                    velocityTracker.resetTracking()
                }
                onGesture(event.calculateCentroid(true), pan, zoom)
                event.changes.forEach { change -> change.consume() }
            }
        } while (event.changes.any { it.pressed })

        if (transforming && trackingVelocity) {
            val velocity = velocityTracker.calculateVelocity().readerVelocityForAxis(panAxis)
            onGestureEnd(velocity, size.width.toFloat(), size.height.toFloat())
        }
    }
}

private fun Offset.readerPanForAxis(axis: ReaderPanAxis): Offset = when (axis) {
    ReaderPanAxis.BOTH -> this
    ReaderPanAxis.HORIZONTAL -> Offset(x, 0f)
    ReaderPanAxis.WEBTOON -> this
}

private fun Offset.readerVelocityPosition(axis: ReaderPanAxis): Offset = when (axis) {
    ReaderPanAxis.BOTH -> this
    ReaderPanAxis.HORIZONTAL -> Offset(x, 0f)
    ReaderPanAxis.WEBTOON -> this
}

private fun Velocity.readerVelocityForAxis(axis: ReaderPanAxis): Velocity = when (axis) {
    ReaderPanAxis.BOTH -> this
    ReaderPanAxis.HORIZONTAL -> Velocity(x, 0f)
    ReaderPanAxis.WEBTOON -> this
}

@Composable
private fun readerImageRequest(page: ReaderPage): ImageRequest {
    val context = LocalContext.current
    return remember(page.imageUrl, page.cachedFilePath, page.headers) {
        val headers = NetworkHeaders.Builder().apply {
            page.headers.forEach { (name, value) ->
                if (name.isNotBlank() && value.isNotBlank()) {
                    set(name, value)
                }
            }
        }.build()

        ImageRequest.Builder(context)
            .data(page.cachedFilePath ?: page.imageUrl)
            .httpHeaders(headers)
            .listener(
                onError = { _, result ->
                    Log.w(
                        "TankobunMain",
                        "Reader image failed index=${page.index} host=${Uri.parse(page.imageUrl).host} headers=${page.headers.keys}",
                        result.throwable,
                    )
                },
            )
            .build()
    }
}

@Composable
private fun DownloadsScreen(state: TankobunUiState, viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Downloads",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    state.downloadSummaryLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.downloads.any { it.state == DownloadState.FAILED || it.state == DownloadState.PAUSED || it.state == DownloadState.QUEUED || it.state == DownloadState.RUNNING }) {
            item {
                FlowRowCompat {
                    if (state.downloads.any { it.state == DownloadState.QUEUED || it.state == DownloadState.RUNNING }) {
                        OutlinedButton(onClick = viewModel::pauseActiveDownloads) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pause active")
                        }
                    }
                    if (state.downloads.any { it.state == DownloadState.PAUSED }) {
                        Button(onClick = viewModel::resumePausedDownloads) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Resume paused")
                        }
                    }
                    if (state.downloads.any { it.state == DownloadState.FAILED }) {
                        Button(onClick = viewModel::retryFailedDownloads) {
                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retry failed")
                        }
                    }
                }
            }
        }
        if (state.downloads.isEmpty()) {
            item {
                Text("No downloads yet.")
            }
        } else {
            items(state.downloads, key = { it.id }) { job ->
                DownloadJobRow(
                    job = job,
                    onPause = { viewModel.pauseDownload(job.id) },
                    onResume = { viewModel.resumeDownload(job.id) },
                    onRetry = { viewModel.retryDownload(job.id) },
                    onRemove = { viewModel.removeDownload(job.id) },
                )
            }
        }
    }
}

@Composable
private fun DownloadJobRow(
    job: DownloadJob,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    ElevatedCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text(job.chapterName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            job.downloadStatusLine(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (job.pageCount > 0 && job.state != DownloadState.COMPLETE) {
                            LinearProgressIndicator(
                                progress = { (job.completedPages.toFloat() / job.pageCount).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                },
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        when (job.state) {
                            DownloadState.QUEUED,
                            DownloadState.RUNNING -> IconButton(onClick = onPause) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause download")
                            }

                            DownloadState.PAUSED -> IconButton(onClick = onResume) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume download")
                            }

                            DownloadState.FAILED -> IconButton(onClick = onRetry) {
                                Icon(Icons.Default.Replay, contentDescription = "Retry download")
                            }

                            DownloadState.COMPLETE -> Unit
                        }
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove download")
                        }
                    }
                },
            )
        }
    }
}

private fun DownloadJob.downloadStatusLine(): String {
    val status = when (state) {
        DownloadState.QUEUED -> "Queued"
        DownloadState.RUNNING -> "Downloading"
        DownloadState.PAUSED -> "Paused"
        DownloadState.COMPLETE -> "Complete"
        DownloadState.FAILED -> "Failed"
    }
    val progress = when {
        pageCount > 0 -> " / $completedPages of $pageCount pages"
        completedPages > 0 -> " / $completedPages pages"
        else -> ""
    }
    val retries = retryCount.takeIf { it > 0 }?.let { " / $it retries" }.orEmpty()
    return "$status$progress$retries"
}

private fun TankobunUiState.downloadSummaryLabel(): String {
    if (downloads.isEmpty()) return "0 jobs"
    val running = downloads.count { it.state == DownloadState.RUNNING }
    val queued = downloads.count { it.state == DownloadState.QUEUED }
    val complete = downloads.count { it.state == DownloadState.COMPLETE }
    val failed = downloads.count { it.state == DownloadState.FAILED }
    val paused = downloads.count { it.state == DownloadState.PAUSED }
    return listOfNotNull(
        running.takeIf { it > 0 }?.let { "$it running" },
        queued.takeIf { it > 0 }?.let { "$it queued" },
        paused.takeIf { it > 0 }?.let { "$it paused" },
        failed.takeIf { it > 0 }?.let { "$it failed" },
        complete.takeIf { it > 0 }?.let { "$it complete" },
    ).joinToString(" / ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
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
private fun SettingsIndexPane(
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
private fun SettingsRouteRow(
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
private fun SettingsRouteIcon(route: SettingsRoute, selected: Boolean) {
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
private fun SettingsDetailContent(
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
                    label = { Text("Paged") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                )
                FilterChip(
                    selected = state.readerMode == ReaderMode.WEBTOON,
                    onClick = { viewModel.setReaderMode(ReaderMode.WEBTOON) },
                    label = { Text("Webtoon") },
                )
            }
            Text("Page gaps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRowCompat {
                (0..3).forEach { level ->
                    FilterChip(
                        selected = state.readerPageGapLevel == level,
                        onClick = { viewModel.setReaderPageGapLevel(level) },
                        label = { Text(readerGapLabel(level)) },
                    )
                }
                FilterChip(
                    selected = state.readerFitWidth,
                    onClick = { viewModel.setReaderFitWidth(!state.readerFitWidth) },
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
        SettingsRoute.ABOUT -> AboutSettingsScreen(modifier)
        SettingsRoute.SOURCES -> SourcesSettingsScreen(state, viewModel)
    }
}

@Composable
private fun BackupsSettingsScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/xml"),
    ) { uri ->
        uri?.let(viewModel::saveAniListBackup)
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::restoreAniListBackup)
    }
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let(viewModel::setScheduledBackupFolder)
    }
    val totalItems = state.libraryItems.size
    val malMatchedItems = state.libraryItems.count { it.media.idMal != null }
    val missingMalItems = totalItems - malMatchedItems

    SettingsDetailPanel(
        title = "Backups",
        subtitle = "Save and restore your AniList manga list as MyAnimeList-compatible XML.",
        modifier = modifier,
    ) {
        Text("AniList manga backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ElevatedCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("MyAnimeList XML", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            backupCoverageLabel(totalItems, malMatchedItems, missingMalItems),
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
                    AssistChip(
                        onClick = {},
                        label = { Text("$malMatchedItems / $totalItems matched") },
                        enabled = false,
                    )
                }
                Text(
                    "You can restore this backup from AniList's web import page or directly in Tankobun.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::refreshLibrary,
                        enabled = state.loggedIn,
                    ) {
                        Text("Sync first")
                    }
                    Button(
                        onClick = {
                            backupLauncher.launch(suggestedAniListBackupFileName(state.viewerName))
                        },
                        enabled = totalItems > 0,
                    ) {
                        Text("Save backup")
                    }
                    OutlinedButton(
                        onClick = {
                            restoreLauncher.launch(arrayOf("text/xml", "application/xml", "*/*"))
                        },
                        enabled = state.loggedIn,
                    ) {
                        Text("Restore")
                    }
                }
            }
        }
        Text("Scheduled backups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ElevatedCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Choose a folder and Tankobun will keep dated XML backups there.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BackupSchedulePicker(
                    selected = state.backupSchedule,
                    onSelect = viewModel::setBackupSchedule,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { folderLauncher.launch(null) }) {
                        Text(if (state.backupFolderUri == null) "Choose folder" else "Change folder")
                    }
                    Button(
                        onClick = viewModel::runScheduledAniListBackupNow,
                        enabled = state.backupFolderUri != null && totalItems > 0,
                    ) {
                        Text("Run now")
                    }
                }
                val lastRun = state.lastScheduledBackupAtEpochMillis
                Text(
                    if (lastRun > 0L) "Last backup: ${cacheAgeLabel(lastRun)}" else "No scheduled backup saved yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BackupSchedulePicker(
    selected: BackupSchedule,
    onSelect: (BackupSchedule) -> Unit,
) {
    val schedules = listOf(
        BackupSchedule.OFF,
        BackupSchedule.DAILY,
        BackupSchedule.WEEKLY,
        BackupSchedule.MONTHLY,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        schedules.chunked(2).forEach { rowSchedules ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowSchedules.forEach { schedule ->
                    FilterChip(
                        selected = selected == schedule,
                        onClick = { onSelect(schedule) },
                        label = { Text(schedule.label()) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowSchedules.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AboutSettingsScreen(modifier: Modifier = Modifier) {
    SettingsDetailPanel(
        title = "About",
        subtitle = "A little context about what Tankobun is and is not.",
        modifier = modifier,
    ) {
        ElevatedCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Tankobun", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Tankobun is an unofficial manga reader and is not affiliated with AniList.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "AniList data is accessed through the user-authorized AniList API. Tankobun does not host, upload, or provide manga or chapter content.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Sources and extensions are third-party. Tankobun only works with compatible extension formats and local user configuration.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DownloadsSettingsScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<PendingDownloadDelete?>(null) }
    SettingsDetailPanel(
        title = "Downloads",
        subtitle = "Review local chapter storage and remove downloaded manga.",
        modifier = modifier,
    ) {
        val summary = state.downloadStorageSummary
        ElevatedCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Local storage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${summary.items.size} manga / ${summary.items.sumOf { it.chapterCount }} chapters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    summary.totalBytes.formatFileSize(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (summary.items.isEmpty()) {
            Text("No downloaded chapters yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "By manga",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedButton(
                    onClick = {
                        pendingDelete = PendingDownloadDelete(
                            mediaId = null,
                            title = "All downloads",
                            detail = summary.totalBytes.formatFileSize(),
                        )
                    },
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete all")
                }
            }
            summary.items.forEach { item ->
                val title = state.downloadedMediaTitle(item.mediaId)
                DownloadStorageRow(
                    title = title,
                    item = item,
                    onDelete = {
                        pendingDelete = PendingDownloadDelete(
                            mediaId = item.mediaId,
                            title = title,
                            detail = item.bytes.formatFileSize(),
                        )
                    },
                )
            }
        }
    }
    pendingDelete?.let { target ->
        DeleteDownloadsDialog(
            target = target,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                if (target.mediaId == null) {
                    viewModel.removeAllDownloads()
                } else {
                    viewModel.removeDownloadsForMedia(target.mediaId)
                }
                pendingDelete = null
            },
        )
    }
}

private data class PendingDownloadDelete(
    val mediaId: Int?,
    val title: String,
    val detail: String,
)

@Composable
private fun DeleteDownloadsDialog(
    target: PendingDownloadDelete,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 420.dp),
            shape = RoundedCornerShape(16.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            tonalElevation = 3.dp,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Delete downloads?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${target.title} / ${target.detail}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(onClick = onConfirm) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadStorageRow(
    title: String,
    item: DownloadStorageItem,
    onDelete: () -> Unit,
) {
    ElevatedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.downloadStorageDetailLine(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                item.bytes.formatFileSize(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete downloads for $title")
            }
        }
    }
}

@Composable
private fun SettingsDetailPanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun CutoutLayoutToggle(
    ignoreDisplayCutout: Boolean,
    onIgnoreDisplayCutoutChange: (Boolean) -> Unit,
) {
    ElevatedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIgnoreDisplayCutoutChange(!ignoreDisplayCutout) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Ignore camera cutout", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Let content use the full display width as if this tablet had no notch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = ignoreDisplayCutout,
                onCheckedChange = onIgnoreDisplayCutoutChange,
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    ElevatedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        }
    }
}

private fun SettingsRoute.settingsSummary(state: TankobunUiState): String =
    when (this) {
        SettingsRoute.MAIN -> "Settings"
        SettingsRoute.APPEARANCE -> tankobunThemeChoices().firstOrNull { it.mode == state.themeMode }?.name ?: "Bunny Mochi"
        SettingsRoute.LIBRARY -> state.libraryViewMode.mediaViewSettingsSummary(
            columns = state.libraryCoverColumns,
            showWholeCovers = state.libraryShowWholeCovers,
        )
        SettingsRoute.BROWSE -> state.browseViewMode.mediaViewSettingsSummary(
            columns = state.browseCoverColumns,
            showWholeCovers = state.browseShowWholeCovers,
        )
        SettingsRoute.READER -> buildList {
            add(if (state.readerMode == ReaderMode.WEBTOON) "Webtoon" else "Paged")
            add(readerGapLabel(state.readerPageGapLevel))
            if (state.readerFitWidth) add("Fit width")
        }.joinToString(" / ")
        SettingsRoute.DOWNLOADS -> state.downloadStorageSummary.totalBytes.formatFileSize()
        SettingsRoute.ANILIST -> buildList {
            add(
                state.viewerName?.let { "Signed in as $it" }
                    ?: if (state.clientConfigured) "Ready to connect" else "Client setup needed",
            )
            if (state.anilistAutoSaveTrackingChanges) add("Auto-save edits")
            if (state.anilistAutoSyncReaderProgress) add("Auto progress")
        }.joinToString(" / ")
        SettingsRoute.BACKUPS -> "${state.libraryItems.count { it.media.idMal != null }} / ${state.libraryItems.size} MAL matched"
        SettingsRoute.ABOUT -> "Credits and notices"
        SettingsRoute.SOURCES -> "${state.installedSources.size} active / ${state.allInstalledSources.size} installed"
    }

private fun BackupSchedule.label(): String =
    when (this) {
        BackupSchedule.OFF -> "Off"
        BackupSchedule.DAILY -> "Daily"
        BackupSchedule.WEEKLY -> "Weekly"
        BackupSchedule.MONTHLY -> "Monthly"
    }

private fun backupCoverageLabel(totalItems: Int, malMatchedItems: Int, missingMalItems: Int): String =
    when {
        totalItems == 0 -> "No cached AniList manga yet. Sync before exporting."
        missingMalItems == 0 -> "$totalItems manga ready for MAL-ID based import."
        else -> "$malMatchedItems manga have MAL IDs; $missingMalItems included without a MAL match."
    }

private fun suggestedAniListBackupFileName(viewerName: String?): String {
    val userPart = viewerName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
        ?: "user"
    return "tankobun_anilist_backup_${userPart}_${System.currentTimeMillis()}.xml"
}

private fun TankobunUiState.downloadedMediaTitle(mediaId: Int): String =
    libraryItems.firstOrNull { it.media.id == mediaId }?.media?.title?.userPreferred
        ?: library.firstOrNull { it.id == mediaId }?.title?.userPreferred
        ?: selectedMedia?.takeIf { it.id == mediaId }?.title?.userPreferred
        ?: "Manga $mediaId"

private fun DownloadStorageItem.downloadStorageDetailLine(): String =
    buildList {
        add("$chapterCount ${if (chapterCount == 1) "chapter" else "chapters"}")
        if (completedChapterCount > 0) {
            add("$completedChapterCount complete")
        }
        if (activeChapterCount > 0) {
            add("$activeChapterCount active")
        }
        if (pageCount > 0) {
            add("$pageCount pages")
        }
    }.joinToString(" / ")

private fun Long.formatFileSize(): String {
    if (this < 1024L) return "$this B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = toDouble() / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    return "%.1f %s".format(Locale.US, value, units[unitIndex])
}

private fun MediaViewMode.mediaViewLabel(): String =
    when (supportedMediaViewMode()) {
        MediaViewMode.COVER_GRID -> "Cover only"
        MediaViewMode.COVER_WITH_INFO -> "Cover + info"
        MediaViewMode.LIST -> "List"
        MediaViewMode.MASONRY,
        MediaViewMode.JUSTIFIED -> "Cover only"
    }

private fun MediaViewMode.mediaViewSettingsSummary(columns: Int, showWholeCovers: Boolean): String {
    val supportedMode = supportedMediaViewMode()
    return if (supportedMode == MediaViewMode.LIST) {
        mediaViewLabel()
    } else {
        val framing = if (showWholeCovers) "whole covers" else "filled covers"
        "${mediaViewLabel()} / ${columns.supportedCoverColumns()} per row / $framing"
    }
}

@Composable
private fun SourcesSettingsScreen(state: TankobunUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedLanguageFilter by remember { mutableStateOf(SOURCE_LANGUAGE_FILTER_ACTIVE) }
    var selectedRepositoryLanguageFilter by remember { mutableStateOf(SOURCE_LANGUAGE_FILTER_ACTIVE) }
    var sourceSettingsQuery by remember { mutableStateOf("") }
    var launchedInstallRequest by remember { mutableStateOf<ExtensionInstallRequest?>(null) }
    val installLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        launchedInstallRequest?.let(viewModel::refreshInstalledSourcesAfterExtensionInstall)
        launchedInstallRequest = null
    }
    LaunchedEffect(state.extensionInstallRequest?.apkUri) {
        val installRequest = state.extensionInstallRequest ?: return@LaunchedEffect
        launchedInstallRequest = installRequest
        installLauncher.launch(downloadedExtensionInstallIntent(installRequest))
        viewModel.consumeExtensionInstallRequest()
    }
    val repositoryByPackage = remember(state.availableExtensions) {
        state.availableExtensions
            .groupBy { it.packageName }
            .mapValues { (_, entries) -> entries.maxBy { it.versionCode } }
    }
    val installedByPackage = remember(state.allInstalledSources) {
        state.allInstalledSources.groupBy { it.packageName }
    }
    val normalizedSourceSettingsQuery = remember(sourceSettingsQuery) {
        sourceSettingsQuery.trim().lowercase()
    }
    val searchableInstalledSources = remember(
        state.allInstalledSources,
        repositoryByPackage,
        normalizedSourceSettingsQuery,
    ) {
        state.allInstalledSources.filter { source ->
            source.matchesSourceSettingsQuery(
                query = normalizedSourceSettingsQuery,
                extension = repositoryByPackage[source.packageName],
            )
        }
    }
    val activeInstalledSources = remember(searchableInstalledSources, state.installedSources) {
        searchableInstalledSources.filter { source -> state.sourceActive(source) }
    }
    val visibleInstalledSourceList = remember(
        searchableInstalledSources,
        activeInstalledSources,
        selectedLanguageFilter,
    ) {
        when (selectedLanguageFilter) {
            SOURCE_LANGUAGE_FILTER_ACTIVE -> activeInstalledSources
            SOURCE_LANGUAGE_FILTER_ALL -> searchableInstalledSources
            else -> searchableInstalledSources.filter { it.lang.normalizedSourceLanguage() == selectedLanguageFilter }
        }
    }
    val sourceGroups = remember(visibleInstalledSourceList) {
        visibleInstalledSourceList
            .groupBy { it.lang.normalizedSourceLanguage() }
            .map { (language, sources) ->
                language to sources.sortedWith(
                    compareBy<SourceDescriptor> { it.name.extensionDisplayName().lowercase() }
                        .thenBy { it.id },
                )
            }
            .sortedWith(
                compareBy<Pair<String, List<SourceDescriptor>>> { sourceLanguageSortPriority(it.first) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { sourceLanguageLabel(it.first) },
            )
    }
    val languageOptions = remember(searchableInstalledSources) {
        searchableInstalledSources
            .map { it.lang.normalizedSourceLanguage() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(compareBy<String> { sourceLanguageSortPriority(it) }.thenBy { sourceLanguageLabel(it) })
    }
    val installedLanguageCounts = remember(searchableInstalledSources) {
        searchableInstalledSources
            .groupingBy { it.lang.normalizedSourceLanguage() }
            .eachCount()
    }
    val visibleInstalledSources = visibleInstalledSourceList
    val repositoryEntries = remember(state.availableExtensions) {
        state.availableExtensions.sortedWith(
            compareBy<ExtensionIndexEntry> { sourceLanguageSortPriority(it.lang.normalizedSourceLanguage()) }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name.extensionDisplayName() }
                .thenByDescending { it.versionCode },
        )
    }
    val searchableRepositoryEntries = remember(repositoryEntries, normalizedSourceSettingsQuery) {
        repositoryEntries.filter { it.matchesSourceSettingsQuery(normalizedSourceSettingsQuery) }
    }
    val activeRepositoryEntries = remember(searchableRepositoryEntries, state.sourceLanguages) {
        searchableRepositoryEntries.filter {
            val language = it.lang.normalizedSourceLanguage()
            language in state.sourceLanguages || language == UNIVERSAL_SOURCE_LANGUAGE
        }
    }
    val repositoryLanguageOptions = remember(searchableRepositoryEntries) {
        searchableRepositoryEntries
            .map { it.lang.normalizedSourceLanguage() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(compareBy<String> { sourceLanguageSortPriority(it) }.thenBy { sourceLanguageLabel(it) })
    }
    val repositoryLanguageCounts = remember(searchableRepositoryEntries) {
        searchableRepositoryEntries
            .groupingBy { it.lang.normalizedSourceLanguage() }
            .eachCount()
    }
    val visibleRepositoryEntries = remember(
        searchableRepositoryEntries,
        activeRepositoryEntries,
        selectedRepositoryLanguageFilter,
    ) {
        when (selectedRepositoryLanguageFilter) {
            SOURCE_LANGUAGE_FILTER_ACTIVE -> activeRepositoryEntries
            SOURCE_LANGUAGE_FILTER_ALL -> searchableRepositoryEntries
            else -> searchableRepositoryEntries.filter { it.lang.normalizedSourceLanguage() == selectedRepositoryLanguageFilter }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SourceSettingsSummary(
                state = state,
                repositoryCount = state.availableExtensions.size,
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                onRefreshInstalled = viewModel::refreshInstalledSources,
                onRefreshRepository = viewModel::refreshExtensionIndex,
            )
        }
        item {
            SourceSettingsSearchField(
                query = sourceSettingsQuery,
                selectedTab = selectedTab,
                onQueryChange = { sourceSettingsQuery = it },
            )
        }
        state.message?.let { message ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(message, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
        }

        if (selectedTab == 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Installed",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { viewModel.setSourcesEnabled(visibleInstalledSources, enabled = true) }) {
                        Text("Visible on")
                    }
                    TextButton(onClick = { viewModel.setSourcesEnabled(visibleInstalledSources, enabled = false) }) {
                        Text("Visible off")
                    }
                }
            }
            item {
                SourceLanguageFilters(
                    selectedFilter = selectedLanguageFilter,
                    languageOptions = languageOptions,
                    activeLabel = "Enabled",
                    activeCount = activeInstalledSources.size,
                    allCount = searchableInstalledSources.size,
                    languageCounts = installedLanguageCounts,
                    onSelectFilter = { selectedLanguageFilter = it },
                )
            }

            if (state.allInstalledSources.isEmpty()) {
                item {
                    Text("No installed Tachiyomi-compatible source extensions found.")
                }
            } else if (sourceGroups.isEmpty()) {
                item {
                    Text(
                        "No installed sources match this search and language filter.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(sourceGroups, key = { it.first }) { (language, sources) ->
                    SourceLanguageGroupSection(
                        language = language,
                        sources = sources,
                        state = state,
                        repositoryByPackage = repositoryByPackage,
                        onSourceEnabledChange = viewModel::setSourceEnabled,
                        onGroupEnabledChange = viewModel::setSourcesEnabled,
                        installingPackageName = state.installingExtensionPackageName,
                        iconUrlFor = viewModel::extensionIconUrl,
                        onInstall = { entry -> requestExtensionInstall(context, viewModel, entry) },
                        onUninstall = { packageName -> openExtensionUninstall(context, packageName) },
                    )
                }
            }

        } else {
            item {
                Text("Extension Repository", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.extensionRepositoryUrl,
                    onValueChange = viewModel::setExtensionRepositoryUrl,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("User-provided repository index URL") },
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = viewModel::refreshExtensionIndex) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Load")
                    }
                    OutlinedButton(onClick = viewModel::refreshInstalledSources) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Rescan")
                    }
                }
                if (state.availableExtensions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${state.availableExtensions.size} extensions loaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (repositoryEntries.isEmpty()) {
                item {
                    Text(
                        "Load a repository index to browse installable extensions.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    SourceLanguageFilters(
                        selectedFilter = selectedRepositoryLanguageFilter,
                        languageOptions = repositoryLanguageOptions,
                        activeLabel = "Preferred",
                        activeCount = activeRepositoryEntries.size,
                        allCount = searchableRepositoryEntries.size,
                        languageCounts = repositoryLanguageCounts,
                        onSelectFilter = { selectedRepositoryLanguageFilter = it },
                    )
                }
                if (visibleRepositoryEntries.isEmpty()) {
                    item {
                        Text(
                            "No repository extensions match this search and language filter.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(visibleRepositoryEntries, key = { "${it.packageName}:${it.versionCode}" }) { extension ->
                    ExtensionRepositoryRow(
                        extension = extension,
                        installedSources = installedByPackage[extension.packageName].orEmpty(),
                        iconUrl = viewModel.extensionIconUrl(extension),
                        installing = state.installingExtensionPackageName == extension.packageName,
                        onInstall = { requestExtensionInstall(context, viewModel, extension) },
                        onUninstall = { openExtensionUninstall(context, extension.packageName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceSettingsSummary(
    state: TankobunUiState,
    repositoryCount: Int,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onRefreshInstalled: () -> Unit,
    onRefreshRepository: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = LocalTankobunTokens.current.elevatedSurface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sources", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${state.installedSources.size} active / ${state.allInstalledSources.size} installed / $repositoryCount in repository",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRefreshInstalled) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh installed sources")
                }
                IconButton(onClick = onRefreshRepository) {
                    Icon(Icons.Default.Download, contentDescription = "Load extension repository")
                }
            }
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
            ) {
                listOf("Installed", "Repository").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onSelectTab(index) },
                        text = { Text(label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceSettingsSearchField(
    query: String,
    selectedTab: Int,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(if (selectedTab == 0) "Search installed sources" else "Search extensions") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear source search")
                }
            }
        },
    )
}

@Composable
private fun SourceLanguageFilters(
    selectedFilter: String,
    languageOptions: List<String>,
    activeLabel: String,
    activeCount: Int,
    allCount: Int,
    languageCounts: Map<String, Int>,
    onSelectFilter: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Languages", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRowCompat {
            FilterChip(
                selected = selectedFilter == SOURCE_LANGUAGE_FILTER_ACTIVE,
                onClick = { onSelectFilter(SOURCE_LANGUAGE_FILTER_ACTIVE) },
                label = { Text("$activeLabel $activeCount") },
            )
            FilterChip(
                selected = selectedFilter == SOURCE_LANGUAGE_FILTER_ALL,
                onClick = { onSelectFilter(SOURCE_LANGUAGE_FILTER_ALL) },
                label = { Text("All $allCount") },
            )
            languageOptions.forEach { language ->
                val count = languageCounts[language] ?: 0
                FilterChip(
                    selected = selectedFilter == language,
                    onClick = { onSelectFilter(language) },
                    label = { Text("${sourceLanguageDisplay(language)} $count") },
                )
            }
        }
    }
}

@Composable
private fun SourceLanguageGroupSection(
    language: String,
    sources: List<SourceDescriptor>,
    state: TankobunUiState,
    repositoryByPackage: Map<String, ExtensionIndexEntry>,
    onSourceEnabledChange: (SourceDescriptor, Boolean) -> Unit,
    onGroupEnabledChange: (Collection<SourceDescriptor>, Boolean) -> Unit,
    installingPackageName: String?,
    iconUrlFor: (ExtensionIndexEntry) -> String?,
    onInstall: (ExtensionIndexEntry) -> Unit,
    onUninstall: (String) -> Unit,
) {
    val activeCount = sources.count { source -> state.sourceActive(source) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(sourceLanguageDisplay(language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "$activeCount of ${sources.size} active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onGroupEnabledChange(sources, true) }) {
                    Text("All on")
                }
                TextButton(onClick = { onGroupEnabledChange(sources, false) }) {
                    Text("All off")
                }
            }
            sources.forEach { source ->
                SourceSettingsRow(
                    source = source,
                    active = state.sourceActive(source),
                    extension = repositoryByPackage[source.packageName],
                    iconUrl = repositoryByPackage[source.packageName]?.let(iconUrlFor),
                    installing = installingPackageName == source.packageName,
                    onEnabledChange = { enabled -> onSourceEnabledChange(source, enabled) },
                    onInstall = onInstall,
                    onUninstall = onUninstall,
                )
            }
        }
    }
}

@Composable
private fun SourceSettingsRow(
    source: SourceDescriptor,
    active: Boolean,
    extension: ExtensionIndexEntry?,
    iconUrl: String?,
    installing: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onInstall: (ExtensionIndexEntry) -> Unit,
    onUninstall: (String) -> Unit,
) {
    val displayName = source.name.extensionDisplayName()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExtensionIcon(
            packageName = source.packageName,
            name = displayName,
            iconUrl = iconUrl,
            modifier = Modifier.size(34.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(
                sourceMetadata(source, active),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val updateAvailable = extension?.let { entry ->
            source.versionCode?.let { installedVersion -> entry.versionCode > installedVersion } == true
        } == true
        if (extension != null && updateAvailable) {
            IconButton(
                enabled = !installing,
                onClick = { onInstall(extension) },
                modifier = Modifier.size(40.dp),
            ) {
                if (installing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Update $displayName",
                    )
                }
            }
        }
        IconButton(
            onClick = { onUninstall(source.packageName) },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Uninstall $displayName")
        }
        Switch(
            checked = active,
            onCheckedChange = onEnabledChange,
        )
    }
}

@Composable
private fun ExtensionRepositoryRow(
    extension: ExtensionIndexEntry,
    installedSources: List<SourceDescriptor>,
    iconUrl: String?,
    installing: Boolean,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
) {
    val displayName = extension.name.extensionDisplayName()
    val installedVersionCode = installedSources.mapNotNull { it.versionCode }.maxOrNull()
    val installed = installedSources.isNotEmpty()
    val updateAvailable = installedVersionCode?.let { extension.versionCode > it } == true
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = LocalTankobunTokens.current.elevatedSurface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExtensionIcon(
                packageName = installedSources.firstOrNull()?.packageName,
                name = displayName,
                iconUrl = iconUrl,
                modifier = Modifier.size(36.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        sourceLanguageDisplay(extension.lang.normalizedSourceLanguage()),
                        "v${extension.versionName}",
                        if (extension.isNsfw) "NSFW" else null,
                        if (installed) "${installedSources.size} source${if (installedSources.size == 1) "" else "s"} installed" else null,
                    ).joinToString(" / "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                enabled = !installing,
                onClick = onInstall,
            ) {
                if (installing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            updateAvailable -> "Update"
                            installed -> "Reinstall"
                            else -> "Install"
                        },
                    )
                }
            }
            if (installed) {
                IconButton(
                    onClick = onUninstall,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Uninstall $displayName")
                }
            }
        }
    }
}

private fun TankobunUiState.sourceActive(source: SourceDescriptor): Boolean =
    installedSources.any { it.sourceSettingsKey() == source.sourceSettingsKey() }

private fun SourceDescriptor.matchesSourceSettingsQuery(
    query: String,
    extension: ExtensionIndexEntry?,
): Boolean {
    if (query.isBlank()) return true
    return listOfNotNull(
        name,
        name.extensionDisplayName(),
        packageName,
        packageName.substringAfterLast('.'),
        lang,
        sourceLanguageLabel(lang),
        extension?.name,
        extension?.name?.extensionDisplayName(),
        extension?.packageName,
    ).any { it.matchesSourceSettingsQuery(query) } ||
        extension?.sources.orEmpty().any { source ->
            listOfNotNull(source.name, source.name.extensionDisplayName(), source.lang, source.lang?.let(::sourceLanguageLabel))
                .any { it.matchesSourceSettingsQuery(query) }
        }
}

private fun ExtensionIndexEntry.matchesSourceSettingsQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return listOfNotNull(
        name,
        name.extensionDisplayName(),
        packageName,
        packageName.substringAfterLast('.'),
        lang,
        sourceLanguageLabel(lang),
        versionName,
    ).any { it.matchesSourceSettingsQuery(query) } ||
        sources.any { source ->
            listOfNotNull(source.name, source.name.extensionDisplayName(), source.lang, source.lang?.let(::sourceLanguageLabel))
                .any { it.matchesSourceSettingsQuery(query) }
        }
}

private fun String.matchesSourceSettingsQuery(query: String): Boolean =
    lowercase().contains(query)

private fun sourceMetadata(source: SourceDescriptor, active: Boolean): String =
    listOfNotNull(
        sourceLanguageDisplay(source.lang.normalizedSourceLanguage()),
        source.versionName?.let { "v$it" },
        if (source.isNsfw) "NSFW" else null,
        if (active) "active" else "off",
    ).joinToString(" / ")

@Composable
private fun ExtensionIcon(
    packageName: String?,
    name: String,
    iconUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val packageIcon = remember(packageName) {
        packageName
            ?.let { pkg ->
                runCatching {
                    context.packageManager.getApplicationIcon(pkg).toImageBitmap()
                }.getOrNull()
            }
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 1.dp,
    ) {
        when {
            packageIcon != null -> {
                Image(
                    bitmap = packageIcon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(3.dp),
                )
            }
            iconUrl != null -> {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(3.dp),
                )
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        extensionInitials(name),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap {
    val width = intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = intrinsicHeight.takeIf { it > 0 } ?: 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

private fun extensionInitials(name: String): String =
    name.extensionDisplayName()
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

private val TachiyomiNamePrefix = Regex("^\\s*tachiyomi\\s*:?\\s*", RegexOption.IGNORE_CASE)

private fun String.extensionDisplayName(): String {
    val cleaned = replace(TachiyomiNamePrefix, "").trim()
    return cleaned.ifBlank { trim() }
}

private fun requestExtensionInstall(
    context: Context,
    viewModel: MainViewModel,
    extension: ExtensionIndexEntry,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
        viewModel.requireExtensionInstallPermission()
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ),
        )
        return
    }
    viewModel.installExtension(extension)
}

private fun downloadedExtensionInstallIntent(installRequest: ExtensionInstallRequest): Intent =
    Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(installRequest.apkUri), "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

private fun openExtensionUninstall(context: Context, packageName: String) {
    context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))
}

private fun sourceLanguageSortPriority(language: String): Int =
    when (language.normalizedSourceLanguage()) {
        "en" -> 0
        "all" -> 1
        else -> 2
    }

private fun cacheAgeLabel(syncedAtEpochMillis: Long): String {
    val ageMillis = (System.currentTimeMillis() - syncedAtEpochMillis).coerceAtLeast(0L)
    val minutes = ageMillis / 60_000L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        minutes < 1 -> "just now"
        hours < 1 -> "${minutes}m ago"
        days < 1 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}

private fun String.normalizedSourceLanguage(): String =
    trim().lowercase().replace('_', '-')

private fun sourceLanguageLabel(language: String): String =
    when (language.normalizedSourceLanguage()) {
        "all" -> "Multilingual"
        "af" -> "Afrikaans"
        "ar" -> "Arabic"
        "az" -> "Azerbaijani"
        "be" -> "Belarusian"
        "bg" -> "Bulgarian"
        "bn" -> "Bengali"
        "ca" -> "Catalan"
        "cs" -> "Czech"
        "da" -> "Danish"
        "de" -> "German"
        "el" -> "Greek"
        "en" -> "English"
        "eo" -> "Esperanto"
        "pt" -> "Portuguese"
        "pt-br" -> "Portuguese (BR)"
        "es" -> "Spanish"
        "eu" -> "Basque"
        "fa" -> "Persian"
        "fi" -> "Finnish"
        "fr" -> "French"
        "ga" -> "Irish"
        "gl" -> "Galician"
        "he" -> "Hebrew"
        "hi" -> "Hindi"
        "hr" -> "Croatian"
        "hu" -> "Hungarian"
        "id" -> "Indonesian"
        "it" -> "Italian"
        "ja" -> "Japanese"
        "ka" -> "Georgian"
        "kk" -> "Kazakh"
        "ko" -> "Korean"
        "la" -> "Latin"
        "lt" -> "Lithuanian"
        "ms" -> "Malay"
        "mn" -> "Mongolian"
        "my" -> "Burmese"
        "ne" -> "Nepali"
        "nl" -> "Dutch"
        "no" -> "Norwegian"
        "pl" -> "Polish"
        "ro" -> "Romanian"
        "ru" -> "Russian"
        "sr" -> "Serbian"
        "sv" -> "Swedish"
        "ta" -> "Tamil"
        "te" -> "Telugu"
        "th" -> "Thai"
        "tr" -> "Turkish"
        "uk" -> "Ukrainian"
        "ur" -> "Urdu"
        "vi" -> "Vietnamese"
        "zh" -> "Chinese"
        "zh-hans" -> "Chinese (Simplified)"
        "zh-hant" -> "Chinese (Traditional)"
        else -> language.uppercase()
    }

private fun sourceLanguageDisplay(language: String): String =
    sourceLanguageFlag(language)?.let { flag -> "$flag ${sourceLanguageLabel(language)}" }
        ?: sourceLanguageLabel(language)

private fun sourceLanguageFlag(language: String): String? {
    val normalized = language.normalizedSourceLanguage()
    if (normalized == UNIVERSAL_SOURCE_LANGUAGE) {
        return String(Character.toChars(0x1F310))
    }
    val region = normalized.substringAfter('-', "")
        .takeIf { it.length == 2 && it.all { char -> char in 'a'..'z' } }
    val countryCode = region ?: when (normalized.substringBefore('-')) {
        "af" -> "za"
        "ar" -> "sa"
        "az" -> "az"
        "be" -> "by"
        "bn" -> "bd"
        "bg" -> "bg"
        "ca" -> "es"
        "cs" -> "cz"
        "da" -> "dk"
        "de" -> "de"
        "el" -> "gr"
        "en" -> "gb"
        "es" -> "es"
        "eu" -> "es"
        "fa" -> "ir"
        "fi" -> "fi"
        "fr" -> "fr"
        "ga" -> "ie"
        "gl" -> "es"
        "he" -> "il"
        "hi" -> "in"
        "hr" -> "hr"
        "hu" -> "hu"
        "id" -> "id"
        "it" -> "it"
        "ja" -> "jp"
        "ka" -> "ge"
        "kk" -> "kz"
        "ko" -> "kr"
        "la" -> "va"
        "lt" -> "lt"
        "ms" -> "my"
        "mn" -> "mn"
        "my" -> "mm"
        "ne" -> "np"
        "nl" -> "nl"
        "no" -> "no"
        "pl" -> "pl"
        "pt" -> "pt"
        "ro" -> "ro"
        "ru" -> "ru"
        "sr" -> "rs"
        "sv" -> "se"
        "ta" -> "in"
        "te" -> "in"
        "th" -> "th"
        "tr" -> "tr"
        "uk" -> "ua"
        "ur" -> "pk"
        "vi" -> "vn"
        "zh" -> "cn"
        else -> null
    }
    return countryCode?.let(::countryFlagEmoji)
}

private fun countryFlagEmoji(countryCode: String): String? {
    val normalized = countryCode.uppercase()
        .takeIf { it.length == 2 && it.all { char -> char in 'A'..'Z' } }
        ?: return null
    val codePoints = normalized.map { char -> 0x1F1E6 + (char - 'A') }.toIntArray()
    return String(codePoints, 0, codePoints.size)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThemePicker(
    selected: TankobunThemeMode,
    onSelect: (TankobunThemeMode) -> Unit,
) {
    val choices = tankobunThemeChoices()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                choices.firstOrNull { it.mode == selected }?.name ?: "Bunny Mochi",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val minCellWidth = 210.dp
            val columnCount = (maxWidth / minCellWidth).toInt().coerceAtLeast(1)
            val rowCount = ((choices.size + columnCount - 1) / columnCount).coerceAtLeast(1)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minCellWidth),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp * rowCount.toFloat()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false,
            ) {
                gridItems(choices, key = { it.mode.name }) { choice ->
                    ThemeChoiceCard(
                        choice = choice,
                        selected = selected == choice.mode,
                        onClick = { onSelect(choice.mode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeChoiceCard(
    choice: TankobunThemeChoice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = tween(durationMillis = 160),
        label = "Theme card scale",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else LocalTankobunTokens.current.elevatedSurface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        tonalElevation = if (selected) 4.dp else 1.dp,
        shadowElevation = if (selected) 3.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeSwatches(choice.swatches)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        choice.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        choice.description,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when (choice.dark) {
                        true -> "Dark"
                        false -> "Light"
                        null -> "Auto"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selected) {
                    Text("Selected", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatches(colors: List<Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp), verticalAlignment = Alignment.CenterVertically) {
        colors.take(3).forEach { color ->
            Surface(
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(999.dp),
                color = color,
                tonalElevation = 1.dp,
                shadowElevation = 1.dp,
            ) {}
        }
    }
}

@Composable
private fun FlowRowCompat(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}
