package com.tankobun.app.ui.shell

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
import android.widget.Toast
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

internal enum class SettingsRoute {
    MAIN,
    APPEARANCE,
    LANGUAGES,
    LIBRARY,
    BROWSE,
    READER,
    DOWNLOADS,
    ANILIST,
    CUSTOM_LISTS,
    BACKUPS,
    ABOUT,
    SOURCES,
}

internal const val TankobunGithubUrl = "https://github.com/JohanKRS/Tankobun"

internal val SettingsDetailRoutes = listOf(
    SettingsRoute.APPEARANCE,
    SettingsRoute.LANGUAGES,
    SettingsRoute.LIBRARY,
    SettingsRoute.BROWSE,
    SettingsRoute.READER,
    SettingsRoute.SOURCES,
    SettingsRoute.DOWNLOADS,
    SettingsRoute.ANILIST,
    SettingsRoute.CUSTOM_LISTS,
    SettingsRoute.BACKUPS,
    SettingsRoute.ABOUT,
)

internal fun SettingsRoute.settingsTitle(): String =
    when (this) {
        SettingsRoute.MAIN -> "Settings"
        SettingsRoute.APPEARANCE -> "Appearance"
        SettingsRoute.LANGUAGES -> "Languages"
        SettingsRoute.LIBRARY -> "Library"
        SettingsRoute.BROWSE -> "Browse"
        SettingsRoute.READER -> "Reader"
        SettingsRoute.DOWNLOADS -> "Downloads"
        SettingsRoute.ANILIST -> "AniList"
        SettingsRoute.CUSTOM_LISTS -> "Custom Lists"
        SettingsRoute.BACKUPS -> "Backups"
        SettingsRoute.ABOUT -> "About"
        SettingsRoute.SOURCES -> "Sources"
    }

internal enum class QuickDrawerMode {
    CLOSED,
    OVERLAY,
    PINNED,
}

private const val BackPressRepeatWindowMillis = 1800L

private data class TankobunRoute(
    val tab: Int,
    val settingsRoute: SettingsRoute = SettingsRoute.MAIN,
    val media: AnilistMedia? = null,
) {
    fun normalized(): TankobunRoute =
        copy(settingsRoute = if (tab == 3 && media == null) settingsRoute else SettingsRoute.MAIN)

    fun sameDestination(other: TankobunRoute): Boolean =
        tab == other.tab &&
            settingsRoute == other.settingsRoute &&
            media?.id == other.media?.id
}

internal val QuickDrawerOverlayWidth = 340.dp
internal val QuickDrawerPinnedWidth = 320.dp
internal val QuickDrawerHandleSlotWidth = 40.dp
internal const val QuickDrawerSnapMillis = 240
internal const val QuickDrawerScrimAlpha = 0.20f
internal const val QuickDrawerBackdropBlurDp = 6f
internal const val QuickDrawerElasticLimitDp = 36f

internal enum class LibraryPicker {
    FORMAT,
    STATUS,
    COUNTRY,
    YEAR,
}

internal enum class ReaderPanAxis {
    BOTH,
    HORIZONTAL,
    WEBTOON,
}

internal data class WebtoonReaderPageItem(
    val chapter: SourceChapter,
    val page: ReaderPage,
    val pageIndex: Int,
)

internal const val SOURCE_LANGUAGE_FILTER_ACTIVE = "__active__"
internal const val SOURCE_LANGUAGE_FILTER_ALL = "__all__"
internal const val LIBRARY_SORT_LIST_ORDER = "LIST_ORDER"
internal const val LIBRARY_SORT_TITLE = "TITLE"
internal const val LIBRARY_SORT_UPDATED = "UPDATED"
internal const val LIBRARY_SORT_PROGRESS = "PROGRESS"
internal const val LIBRARY_SORT_SCORE = "SCORE"

internal val LibrarySortOptions = listOf(
    BrowseOption("List Order", LIBRARY_SORT_LIST_ORDER),
    BrowseOption("Title", LIBRARY_SORT_TITLE),
    BrowseOption("Recently Updated", LIBRARY_SORT_UPDATED),
    BrowseOption("Progress", LIBRARY_SORT_PROGRESS),
    BrowseOption("Score", LIBRARY_SORT_SCORE),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TankobunAppRoot(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var settingsRoute by remember { mutableStateOf(SettingsRoute.MAIN) }
    var quickDrawerMode by remember { mutableStateOf(QuickDrawerMode.CLOSED) }
    var routeHistory by remember { mutableStateOf<List<TankobunRoute>>(emptyList()) }
    var lastHomeBackPressAt by remember { mutableLongStateOf(0L) }
    var lastReaderBackPressAt by remember { mutableLongStateOf(0L) }
    val compactLayout = LocalConfiguration.current.smallestScreenWidthDp in 1 until 600
    val selectedMedia = state.selectedMedia
    val readerOpen = state.activeChapter != null && state.readerPages.isNotEmpty()
    val appStatusBarVisible = state.showAppStatusBar && !readerOpen
    val useDarkStatusBarIcons = state.themeMode.useDarkStatusBarIcons(isSystemInDarkTheme())
    val currentRoute = TankobunRoute(
        tab = selectedTab,
        settingsRoute = settingsRoute,
        media = selectedMedia,
    ).normalized()
    val latestCurrentRoute = rememberUpdatedState(currentRoute)
    val latestSelectedMedia = rememberUpdatedState(selectedMedia)

    fun resetBackPressWindows() {
        lastHomeBackPressAt = 0L
        lastReaderBackPressAt = 0L
    }

    fun applyRoute(route: TankobunRoute) {
        val normalized = route.normalized()
        val currentMedia = latestSelectedMedia.value
        selectedTab = normalized.tab
        settingsRoute = normalized.settingsRoute
        quickDrawerMode = QuickDrawerMode.CLOSED
        when {
            normalized.media == null -> viewModel.clearSelectedMedia()
            currentMedia?.id != normalized.media.id -> viewModel.selectMedia(normalized.media)
        }
        resetBackPressWindows()
    }

    fun navigateTo(route: TankobunRoute) {
        val normalized = route.normalized()
        val routeNow = latestCurrentRoute.value
        if (normalized.sameDestination(routeNow)) {
            quickDrawerMode = QuickDrawerMode.CLOSED
            return
        }
        routeHistory = routeHistory + routeNow
        applyRoute(normalized)
    }

    fun navigateToRootTab(tab: Int) {
        val normalized = TankobunRoute(tab = tab).normalized()
        val routeNow = latestCurrentRoute.value
        routeHistory = emptyList()
        if (normalized.sameDestination(routeNow)) {
            quickDrawerMode = QuickDrawerMode.CLOSED
            resetBackPressWindows()
        } else {
            applyRoute(normalized)
        }
    }

    fun popRoute(): Boolean {
        val previousRoute = routeHistory.lastOrNull() ?: return false
        routeHistory = routeHistory.dropLast(1)
        applyRoute(previousRoute)
        return true
    }

    fun handleReaderBack() {
        val now = System.currentTimeMillis()
        if (now - lastReaderBackPressAt <= BackPressRepeatWindowMillis) {
            lastReaderBackPressAt = 0L
            viewModel.closeReader()
        } else {
            lastReaderBackPressAt = now
            Toast.makeText(context, "Press back again to exit reader", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleAppBack() {
        if (quickDrawerMode != QuickDrawerMode.CLOSED) {
            quickDrawerMode = QuickDrawerMode.CLOSED
            return
        }
        if (popRoute()) return
        when {
            selectedMedia != null -> {
                viewModel.clearSelectedMedia()
                resetBackPressWindows()
            }
            selectedTab == 3 && settingsRoute != SettingsRoute.MAIN -> {
                settingsRoute = SettingsRoute.MAIN
                resetBackPressWindows()
            }
            selectedTab != 0 -> {
                applyRoute(TankobunRoute(tab = 0))
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastHomeBackPressAt <= BackPressRepeatWindowMillis) {
                    (context as? Activity)?.finish()
                } else {
                    lastHomeBackPressAt = now
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(compactLayout, quickDrawerMode) {
        if (compactLayout && quickDrawerMode == QuickDrawerMode.PINNED) {
            quickDrawerMode = QuickDrawerMode.OVERLAY
        }
    }

    LaunchedEffect(readerOpen) {
        if (!readerOpen) {
            lastReaderBackPressAt = 0L
        }
    }

    StatusBarVisibilityEffect(visible = appStatusBarVisible, useDarkIcons = useDarkStatusBarIcons)

    BackHandler(enabled = readerOpen) {
        handleReaderBack()
    }

    BackHandler(enabled = !readerOpen) {
        handleAppBack()
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
                onSelectTab = ::navigateToRootTab,
                canNavigateBack = selectedMedia != null || (selectedTab == 3 && settingsRoute != SettingsRoute.MAIN),
                onNavigateBack = { handleAppBack() },
                onSelectMedia = { media -> navigateTo(TankobunRoute(tab = selectedTab, media = media)) },
                selectedMedia = selectedMedia,
                settingsRoute = settingsRoute,
                onOpenSettingsRoute = { navigateTo(TankobunRoute(tab = 3, settingsRoute = it)) },
                onBrowseTag = { tag ->
                    viewModel.browseByTag(tag)
                    navigateTo(TankobunRoute(tab = 1))
                },
                onBrowseAuthor = { author ->
                    viewModel.browseByAuthor(author)
                    navigateTo(TankobunRoute(tab = 1))
                },
                onOpenRecentProgress = { item ->
                    val recentRoute = TankobunRoute(tab = selectedTab, media = item.media).normalized()
                    val routeNow = latestCurrentRoute.value
                    if (!recentRoute.sameDestination(routeNow)) {
                        routeHistory = routeHistory + routeNow
                    }
                    quickDrawerMode = QuickDrawerMode.CLOSED
                    resetBackPressWindows()
                    viewModel.openRecentProgress(item)
                },
                quickDrawerMode = quickDrawerMode,
                showStatusBar = state.showAppStatusBar,
                showQuickActionsButton = !readerOpen,
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
            if (state.onboardingVisible && !readerOpen) {
                OnboardingDialog(onDismiss = viewModel::dismissOnboarding)
            }
        }
    }
}

@Composable
internal fun StatusBarVisibilityEffect(visible: Boolean, useDarkIcons: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController?.let { controller ->
                controller.setSystemBarsAppearance(
                    if (useDarkIcons) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                )
                if (visible) {
                    controller.show(AndroidWindowInsets.Type.statusBars())
                } else {
                    controller.hide(AndroidWindowInsets.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        } else {
            @Suppress("DEPRECATION")
            if (visible) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val lightStatusFlag = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                    window.addFlags(lightStatusFlag)
                    window.decorView.systemUiVisibility = if (useDarkIcons) {
                        window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    } else {
                        window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                    }
                }
            } else {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                )
            }
        }
    }
}

internal fun TankobunThemeMode.useDarkStatusBarIcons(systemDark: Boolean): Boolean =
    when (this) {
        TankobunThemeMode.SYSTEM -> !systemDark
        TankobunThemeMode.LIGHT,
        TankobunThemeMode.BUNNY_MOCHI,
        TankobunThemeMode.PEACH_SODA,
        TankobunThemeMode.MATCHA_MILK,
        TankobunThemeMode.SAKURA_MINT,
        TankobunThemeMode.CLOUDBERRY_POP,
        TankobunThemeMode.YUZU_GARDEN -> true
        TankobunThemeMode.DARK,
        TankobunThemeMode.MIDNIGHT_RAMEN,
        TankobunThemeMode.STARRY_INK,
        TankobunThemeMode.PLUM_NIGHT,
        TankobunThemeMode.NEON_KOI,
        TankobunThemeMode.MOON_JELLY,
        TankobunThemeMode.INKBERRY_FIZZ,
        TankobunThemeMode.CHARCOAL_GOLD -> false
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TankobunScaffold(
    state: TankobunUiState,
    viewModel: MainViewModel,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    onSelectMedia: (AnilistMedia) -> Unit,
    selectedMedia: AnilistMedia?,
    settingsRoute: SettingsRoute,
    onOpenSettingsRoute: (SettingsRoute) -> Unit,
    onBrowseTag: (String) -> Unit,
    onBrowseAuthor: (String) -> Unit,
    onOpenRecentProgress: (RecentReadingProgress) -> Unit,
    quickDrawerMode: QuickDrawerMode,
    showStatusBar: Boolean,
    showQuickActionsButton: Boolean,
    onOpenQuickDrawer: () -> Unit,
    onCloseQuickDrawer: () -> Unit,
    onToggleQuickDrawerPin: () -> Unit,
) {
    val ignoreDisplayCutout = state.ignoreDisplayCutout
    val cutoutStartPadding = displayCutoutStartPadding(ignoreDisplayCutout = ignoreDisplayCutout)
    val cutoutEndPadding = displayCutoutEndPadding(ignoreDisplayCutout = ignoreDisplayCutout)
    val density = LocalDensity.current
    val drawerTravelPx = with(density) { (QuickDrawerOverlayWidth + cutoutEndPadding).toPx() }
    val drawerSnapThresholdPx = with(density) { 72.dp.toPx() }
    val drawerElasticLimitPx = with(density) { QuickDrawerElasticLimitDp.dp.toPx() }
    val drawerScope = rememberCoroutineScope()
    var overlayDrawerDragOffsetPx by remember { mutableFloatStateOf(0f) }
    var overlayDrawerDragging by remember { mutableStateOf(false) }
    val overlayDrawerOffsetPx by animateFloatAsState(
        targetValue = overlayDrawerDragOffsetPx,
        animationSpec = tween(durationMillis = if (overlayDrawerDragging) 0 else QuickDrawerSnapMillis),
        label = "Overlay drawer drag offset",
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
    val quickDrawerBackdropRevealFraction = overlayDrawerRevealFraction
    val drawerScrimAlpha = QuickDrawerScrimAlpha * quickDrawerBackdropRevealFraction
    val drawerBackdropBlur = (QuickDrawerBackdropBlurDp * quickDrawerBackdropRevealFraction).dp
    val mediaDetailActive = selectedMedia != null
    val compactLayout = LocalConfiguration.current.smallestScreenWidthDp in 1 until 600
    val mediaDetailTopBarInset = if (mediaDetailActive) {
        val statusBarInset = if (showStatusBar) {
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        } else {
            0.dp
        }
        statusBarInset + if (compactLayout) 48.dp else 72.dp
    } else {
        0.dp
    }
    val routeBackdropColor = LocalTankobunTokens.current.appBackdrop
    val routeBarColor = LocalTankobunTokens.current.elevatedSurface
    val routeContentColor = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(quickDrawerMode) {
        if (quickDrawerMode != QuickDrawerMode.OVERLAY) {
            overlayDrawerDragging = false
            overlayDrawerDragOffsetPx = 0f
        }
    }

    fun openQuickDrawerFromClosed(initialTranslationPx: Float = drawerTravelPx) {
        drawerScope.launch {
            overlayDrawerDragging = true
            overlayDrawerDragOffsetPx = initialTranslationPx
            onOpenQuickDrawer()
            withFrameNanos { }
            withFrameNanos { }
            overlayDrawerDragging = false
            overlayDrawerDragOffsetPx = 0f
        }
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
        containerColor = routeBackdropColor,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TankobunTopBar(
                title = selectedMedia?.title?.userPreferred
                    ?: if (selectedTab == 3 && settingsRoute != SettingsRoute.MAIN) settingsRoute.settingsTitle() else "Tankobun",
                showBack = canNavigateBack,
                ignoreDisplayCutout = ignoreDisplayCutout,
                showStatusBar = showStatusBar,
                mediaDetailActive = mediaDetailActive,
                quickActionsVisible = showQuickActionsButton,
                quickActionsOpen = quickDrawerMode != QuickDrawerMode.CLOSED,
                onToggleQuickActions = if (showQuickActionsButton) {
                    {
                        when (quickDrawerMode) {
                            QuickDrawerMode.CLOSED -> openQuickDrawerFromClosed()
                            QuickDrawerMode.OVERLAY -> closeQuickDrawerFromOverlay()
                            QuickDrawerMode.PINNED -> onCloseQuickDrawer()
                        }
                    }
                } else {
                    null
                },
                onBack = onNavigateBack,
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = routeBarColor,
                contentColor = routeContentColor,
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
        },
    ) { padding ->
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = padding.calculateStartPadding(LocalLayoutDirection.current),
                        top = if (mediaDetailActive) 0.dp else padding.calculateTopPadding(),
                        end = padding.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = padding.calculateBottomPadding(),
                    ),
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(start = cutoutStartPadding, end = cutoutEndPadding)
                    .blur(drawerBackdropBlur)
                    .background(routeBackdropColor),
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (selectedMedia != null) {
                        MangaDetailScreen(
                            state = state,
                            viewModel = viewModel,
                            media = selectedMedia,
                            onSelectMedia = onSelectMedia,
                            onBrowseTag = onBrowseTag,
                            onBrowseAuthor = onBrowseAuthor,
                        )
                    } else {
                        when (selectedTab) {
                            0 -> LibraryScreen(state, viewModel, onSelectMedia = onSelectMedia)
                            1 -> BrowseScreen(state, viewModel, onSelectMedia = onSelectMedia)
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
                        onOpenRecentProgress = onOpenRecentProgress,
                        pinned = true,
                        onClose = onCloseQuickDrawer,
                        onTogglePin = onToggleQuickDrawerPin,
                        drawerWidth = pinnedWidth,
                        endPadding = cutoutEndPadding,
                        modifier = Modifier
                            .padding(top = mediaDetailTopBarInset)
                            .fillMaxHeight(),
                    )
                }
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
                    onOpenRecentProgress = onOpenRecentProgress,
                    pinned = false,
                    onClose = { closeQuickDrawerFromOverlay() },
                    onTogglePin = onToggleQuickDrawerPin,
                    drawerWidth = QuickDrawerOverlayWidth,
                    endPadding = cutoutEndPadding,
                    showHandle = false,
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
                        .padding(top = mediaDetailTopBarInset)
                        .fillMaxHeight()
                        .graphicsLayer { translationX = overlayDrawerTranslationPx },
                )
            }
            if (state.busy && !(state.sourcePickerOpen && state.sourcePickerLoading)) {
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

internal fun quickDrawerOpeningDragOffset(totalX: Float, drawerTravelPx: Float, elasticLimitPx: Float): Float {
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

internal fun quickDrawerClosingDragOffset(totalX: Float, drawerTravelPx: Float, elasticLimitPx: Float): Float {
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

internal fun quickDrawerElasticOvershoot(overshootPx: Float, elasticLimitPx: Float): Float =
    if (elasticLimitPx <= 0f) {
        0f
    } else {
        elasticLimitPx * overshootPx / (overshootPx + elasticLimitPx)
    }

@Composable
internal fun hasDisplayCutout(): Boolean {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val cutoutInsets = WindowInsets.displayCutout
    return cutoutInsets.getLeft(density, layoutDirection) > 0 ||
        cutoutInsets.getRight(density, layoutDirection) > 0 ||
        cutoutInsets.getTop(density) > 0 ||
        cutoutInsets.getBottom(density) > 0
}

@Composable
internal fun displayCutoutStartPadding(ignoreDisplayCutout: Boolean): Dp {
    if (ignoreDisplayCutout) return 0.dp
    val layoutDirection = LocalLayoutDirection.current
    return maxOf(
        WindowInsets.displayCutout.asPaddingValues().calculateStartPadding(layoutDirection),
        WindowInsets.safeDrawing.asPaddingValues().calculateStartPadding(layoutDirection),
    )
}

@Composable
internal fun displayCutoutEndPadding(ignoreDisplayCutout: Boolean): Dp {
    if (ignoreDisplayCutout) return 0.dp
    val layoutDirection = LocalLayoutDirection.current
    return maxOf(
        WindowInsets.displayCutout.asPaddingValues().calculateEndPadding(layoutDirection),
        WindowInsets.safeDrawing.asPaddingValues().calculateEndPadding(layoutDirection),
    )
}

@Composable
internal fun TankobunTopBar(
    title: String,
    showBack: Boolean,
    ignoreDisplayCutout: Boolean,
    showStatusBar: Boolean,
    mediaDetailActive: Boolean = false,
    quickActionsVisible: Boolean = true,
    quickActionsOpen: Boolean = false,
    onToggleQuickActions: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val compact = configuration.smallestScreenWidthDp in 1 until 600
    val startInset = displayCutoutStartPadding(ignoreDisplayCutout = ignoreDisplayCutout)
    val endInset = displayCutoutEndPadding(ignoreDisplayCutout = ignoreDisplayCutout)
    val statusBarInset = if (showStatusBar) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }
    val barHeight = if (compact) 48.dp else 72.dp
    val horizontalPadding = if (compact) 10.dp else 18.dp
    val iconSize = if (compact) 18.dp else 24.dp
    val logoSize = if (compact) 36.dp else 56.dp
    val spacing = if (compact) 7.dp else 12.dp
    val barColor = if (mediaDetailActive) Color.Transparent else LocalTankobunStyle.current.colors.panel
    val contentColor = LocalTankobunStyle.current.colors.panelContent
    Surface(
        color = barColor,
        contentColor = contentColor,
        tonalElevation = if (mediaDetailActive) 0.dp else 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight + statusBarInset),
        ) {
            if (mediaDetailActive) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(42.dp)
                        .background(LocalTankobunStyle.current.colors.backdrop.copy(alpha = 0.96f)),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(LocalTankobunStyle.current.colors.panel.copy(alpha = 0.72f)),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                    start = horizontalPadding + startInset,
                    top = statusBarInset,
                    end = horizontalPadding + endInset,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                if (showBack) {
                    IconButton(onClick = onBack, modifier = Modifier.size(if (compact) 36.dp else 48.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(iconSize),
                        )
                    }
                }
                if (!showBack) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(logoSize),
                    )
                }
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    style = when {
                        mediaDetailActive && compact -> MaterialTheme.typography.titleMedium
                        mediaDetailActive -> MaterialTheme.typography.titleLarge
                        compact -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.titleLarge
                    },
                )
                if (quickActionsVisible && onToggleQuickActions != null) {
                    IconButton(
                        onClick = onToggleQuickActions,
                        modifier = Modifier.size(if (compact) 36.dp else 48.dp),
                    ) {
                        AnimatedHamburgerCloseIcon(
                            close = quickActionsOpen,
                            contentDescription = if (quickActionsOpen) "Close quick actions" else "Open quick actions",
                            modifier = Modifier.size(iconSize + 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AnimatedHamburgerCloseIcon(
    close: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (close) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "Quick actions menu icon",
    )
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        },
    ) {
        val strokeWidth = 2.dp.toPx()
        val center = this.center
        val half = size.minDimension * 0.36f
        val gap = size.minDimension * 0.22f * (1f - progress)
        val diagonal = size.minDimension * 0.32f
        val middleAlpha = 1f - progress

        fun point(x: Float, y: Float): Offset = Offset(center.x + x, center.y + y)
        fun lerp(start: Offset, end: Offset): Offset =
            Offset(
                x = start.x + (end.x - start.x) * progress,
                y = start.y + (end.y - start.y) * progress,
            )

        drawLine(
            color = color,
            start = lerp(point(-half, -gap), point(-diagonal, -diagonal)),
            end = lerp(point(half, -gap), point(diagonal, diagonal)),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        if (middleAlpha > 0.01f) {
            drawLine(
                color = color.copy(alpha = middleAlpha),
                start = point(-half, 0f),
                end = point(half, 0f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
        drawLine(
            color = color,
            start = lerp(point(-half, gap), point(-diagonal, diagonal)),
            end = lerp(point(half, gap), point(diagonal, -diagonal)),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun TankobunBottomNavigationBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = LocalTankobunStyle.current.colors.panel.copy(alpha = 0.92f),
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
internal fun QuickDrawerHandle(
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
internal fun QuickDrawer(
    state: TankobunUiState,
    viewModel: MainViewModel,
    selectedMedia: AnilistMedia?,
    onOpenRecentProgress: (RecentReadingProgress) -> Unit,
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
    val handleSlotWidth = if (pinned || !showHandle) 0.dp else QuickDrawerHandleSlotWidth
    val compactLayout = LocalConfiguration.current.smallestScreenWidthDp in 1 until 600
    Box(modifier = modifier.width(handleSlotWidth + drawerWidth + endPadding)) {
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(drawerWidth + endPadding)
                .fillMaxHeight(),
            shape = RoundedCornerShape(0.dp),
            color = LocalTankobunStyle.current.colors.panel,
            contentColor = LocalTankobunStyle.current.colors.panelContent,
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
                    if (!compactLayout) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onTogglePin) {
                                Icon(
                                    imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (pinned) "Unpin quick actions" else "Pin quick actions",
                                    tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    QuickDrawerSection(title = "AniList Tracking") {
                        if (selectedMedia != null) {
                            AniListTrackingSection(state, viewModel, selectedMedia)
                        } else {
                            Text(
                                "Open a manga to track it here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    QuickDrawerSection(title = "Continue Reading") {
                        if (state.recentReadingProgress.isNotEmpty()) {
                            state.recentReadingProgress.forEach { item ->
                                RecentReadingAction(item = item, onClick = { onOpenRecentProgress(item) })
                            }
                        } else {
                            Text(
                                "Start reading any manga to create resume points.",
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
internal fun RecentReadingAction(item: RecentReadingProgress, onClick: () -> Unit) {
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
        TankobunActionButton(
            label = if (item.chapter == null) "Open manga" else "Resume",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            filled = false,
        )
    }
}

@Composable
internal fun QuickDrawerSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TankobunSectionHeader(title = title)
            content()
        }
    }
}
