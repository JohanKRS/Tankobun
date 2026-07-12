package com.tankobun.app.ui.shell

import com.tankobun.app.ui.icons.TankobunIcons

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
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
import kotlinx.coroutines.joinAll
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
import com.tankobun.app.ui.home.*
import com.tankobun.app.ui.library.*
import com.tankobun.app.ui.media.*
import com.tankobun.app.ui.reader.*
import com.tankobun.app.ui.settings.*
import com.tankobun.app.ui.shell.*

internal enum class SettingsRoute {
    MAIN,
    PROFILE,
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
internal const val TankobunAniListUrl = "https://anilist.co"

internal val SettingsDetailRoutes = listOf(
    SettingsRoute.PROFILE,
    SettingsRoute.APPEARANCE,
    SettingsRoute.LANGUAGES,
    SettingsRoute.LIBRARY,
    SettingsRoute.BROWSE,
    SettingsRoute.READER,
    SettingsRoute.SOURCES,
    SettingsRoute.DOWNLOADS,
    SettingsRoute.CUSTOM_LISTS,
    SettingsRoute.BACKUPS,
    SettingsRoute.ABOUT,
)

@Composable
internal fun SettingsRoute.settingsTitle(): String =
    when (this) {
        SettingsRoute.MAIN -> tankobunString(R.string.common_settings)
        SettingsRoute.PROFILE -> tankobunString(R.string.settings_profile)
        SettingsRoute.APPEARANCE -> tankobunString(R.string.settings_appearance)
        SettingsRoute.LANGUAGES -> tankobunString(R.string.settings_languages)
        SettingsRoute.LIBRARY -> tankobunString(R.string.common_library)
        SettingsRoute.BROWSE -> tankobunString(R.string.common_browse)
        SettingsRoute.READER -> tankobunString(R.string.common_reader)
        SettingsRoute.DOWNLOADS -> tankobunString(R.string.common_downloads)
        SettingsRoute.ANILIST -> "AniList"
        SettingsRoute.CUSTOM_LISTS -> tankobunString(R.string.settings_custom_lists)
        SettingsRoute.BACKUPS -> tankobunString(R.string.settings_backups)
        SettingsRoute.ABOUT -> tankobunString(R.string.common_about)
        SettingsRoute.SOURCES -> tankobunString(R.string.settings_sources)
    }

internal enum class QuickDrawerMode {
    CLOSED,
    OVERLAY,
}

private const val BackPressRepeatWindowMillis = 1800L

private data class TankobunRoute(
    val tab: Int,
    val settingsRoute: SettingsRoute = SettingsRoute.MAIN,
    val media: AnilistMedia? = null,
) {
    fun normalized(): TankobunRoute =
        copy(settingsRoute = if (tab == 4 && media == null) settingsRoute else SettingsRoute.MAIN)

    fun sameDestination(other: TankobunRoute): Boolean =
        tab == other.tab &&
            settingsRoute == other.settingsRoute &&
            media?.id == other.media?.id
}

internal val QuickDrawerOverlayWidth = 340.dp
internal val QuickDrawerHandleSlotWidth = 40.dp
private val FrostedDockHorizontalMargin = 16.dp
private val FrostedDockTopMargin = 8.dp
private val FrostedDockBottomMargin = 8.dp
private val FrostedDockHeight = 56.dp
private val FrostedDockWidth = 272.dp
private val FrostedDockWithQuickActionsWidth = 336.dp
private val FrostedGlassBlur = 88.dp
private val FrostedTopBarShape = RoundedCornerShape(0.dp)
private val FrostedDockShape = RoundedCornerShape(percent = 50)
private const val FrostedGlassInputScale = 0.33f
private const val FrostedGlassTintAlpha = 0.34f
private const val FrostedGlassDimAlpha = 0.30f
private const val FrostedGlassWashAlpha = 0.18f
private const val FrostedGlassNoiseFactor = 0f
internal const val QuickDrawerSnapMillis = 240
internal const val QuickDrawerScrimAlpha = 0.20f
internal const val QuickDrawerBackdropBlurDp = 6f

internal data class TankobunChromeInsets(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp,
)

internal val LocalTankobunChromeInsets = staticCompositionLocalOf { TankobunChromeInsets() }
internal const val QuickDrawerElasticLimitDp = 36f

internal enum class LibraryPicker {
    FORMAT,
    STATUS,
    COUNTRY,
    YEAR,
}

internal const val SOURCE_LANGUAGE_FILTER_ACTIVE = "__active__"
internal const val SOURCE_LANGUAGE_FILTER_ALL = "__all__"
internal const val LIBRARY_SORT_LIST_ORDER = "LIST_ORDER"
internal const val LIBRARY_SORT_TITLE = "TITLE"
internal const val LIBRARY_SORT_UPDATED = "UPDATED"
internal const val LIBRARY_SORT_PROGRESS = "PROGRESS"
internal const val LIBRARY_SORT_SCORE = "SCORE"

internal val LibrarySortOptions = listOf(
    BrowseOption(R.string.library_sort_list_order, LIBRARY_SORT_LIST_ORDER),
    BrowseOption(R.string.browse_sort_title, LIBRARY_SORT_TITLE),
    BrowseOption(R.string.library_sort_recently_updated, LIBRARY_SORT_UPDATED),
    BrowseOption(R.string.common_progress, LIBRARY_SORT_PROGRESS),
    BrowseOption(R.string.common_score, LIBRARY_SORT_SCORE),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TankobunAppRoot(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TankobunLocalizedContent(state.appLanguage) {
        TankobunAppRootContent(
            state = state,
            viewModel = viewModel,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TankobunAppRootContent(
    state: TankobunUiState,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val readerBackToast = tankobunString(R.string.toast_back_exit_reader)
    val appBackToast = tankobunString(R.string.toast_back_exit)
    var selectedTab by remember { mutableIntStateOf(0) }
    var settingsRoute by remember { mutableStateOf(SettingsRoute.MAIN) }
    var quickDrawerMode by remember { mutableStateOf(QuickDrawerMode.CLOSED) }
    var routeHistory by remember { mutableStateOf<List<TankobunRoute>>(emptyList()) }
    var lastHomeBackPressAt by remember { mutableLongStateOf(0L) }
    var lastReaderBackPressAt by remember { mutableLongStateOf(0L) }
    val compactLayout = LocalConfiguration.current.smallestScreenWidthDp in 1 until 600
    val selectedMedia = state.selectedMedia
    val readerOpen = state.activeChapter != null
    val browseCanNavigateBack = selectedTab == 2 &&
        selectedMedia == null &&
        (state.hasBrowseQueryOrFilters() || state.browseSearched)
    val appStatusBarVisible = state.showAppStatusBar && !readerOpen
    val useDarkStatusBarIcons = state.themeMode.useDarkStatusBarIcons(isSystemInDarkTheme())
    val currentRoute = TankobunRoute(
        tab = selectedTab,
        settingsRoute = settingsRoute,
        media = selectedMedia,
    ).normalized()
    val latestCurrentRoute = rememberUpdatedState(currentRoute)
    val latestSelectedMedia = rememberUpdatedState(selectedMedia)
    val tourExampleMedia = state.browsePopular.firstOrNull()

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
        if (normalized.tab == 0 && normalized.media == null) {
            viewModel.loadHomeFeed()
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
        if (tab == 0) {
            viewModel.loadHomeFeed()
        }
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

    fun showTourStep(step: AppTourStep) {
        val exampleMedia = tourExampleMedia
        routeHistory = emptyList()
        applyRoute(
            when (step) {
                AppTourStep.LIBRARY -> TankobunRoute(tab = 1)
                AppTourStep.BROWSE -> TankobunRoute(tab = 2)
                AppTourStep.TRACKING,
                AppTourStep.QUICK_ACTIONS,
                AppTourStep.READER -> TankobunRoute(tab = 2, media = exampleMedia)
                AppTourStep.SOURCES -> TankobunRoute(tab = 4, settingsRoute = SettingsRoute.SOURCES)
                AppTourStep.BACKUPS -> TankobunRoute(tab = 4, settingsRoute = SettingsRoute.BACKUPS)
                AppTourStep.PROFILE -> TankobunRoute(tab = 4, settingsRoute = SettingsRoute.PROFILE)
            },
        )
        quickDrawerMode = if (step == AppTourStep.QUICK_ACTIONS && exampleMedia != null) {
            QuickDrawerMode.OVERLAY
        } else {
            QuickDrawerMode.CLOSED
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
            Toast.makeText(context, readerBackToast, Toast.LENGTH_SHORT).show()
        }
    }

    fun handleAppBack() {
        if (quickDrawerMode != QuickDrawerMode.CLOSED) {
            quickDrawerMode = QuickDrawerMode.CLOSED
            return
        }
        if (browseCanNavigateBack && viewModel.navigateBrowseBack()) {
            resetBackPressWindows()
            return
        }
        if (popRoute()) return
        when {
            selectedMedia != null -> {
                viewModel.clearSelectedMedia()
                resetBackPressWindows()
            }
            selectedTab == 4 && settingsRoute != SettingsRoute.MAIN -> {
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
                    Toast.makeText(context, appBackToast, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(readerOpen) {
        if (!readerOpen) {
            lastReaderBackPressAt = 0L
        }
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        delay(10_000L)
        viewModel.dismissMessage(message)
    }

    StatusBarVisibilityEffect(visible = appStatusBarVisible, useDarkIcons = useDarkStatusBarIcons)
    ReaderOrientationEffect(
        active = readerOpen,
        orientation = state.readerScreenOrientation,
    )

    BackHandler(enabled = readerOpen) {
        handleReaderBack()
    }

    BackHandler(enabled = !readerOpen) {
        handleAppBack()
    }

    TankobunTheme(themeMode = state.themeMode) {
        val effectiveIgnoreDisplayCutout = effectiveIgnoreDisplayCutout(state.ignoreDisplayCutout)
        val cutoutEndPadding = displayCutoutEndPadding(ignoreDisplayCutout = effectiveIgnoreDisplayCutout)
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
                canNavigateBack = selectedMedia != null ||
                    browseCanNavigateBack ||
                    (selectedTab == 4 && settingsRoute != SettingsRoute.MAIN),
                onNavigateBack = { handleAppBack() },
                onSelectMedia = { media -> navigateTo(TankobunRoute(tab = selectedTab, media = media)) },
                selectedMedia = selectedMedia,
                settingsRoute = settingsRoute,
                onOpenSettingsRoute = { navigateTo(TankobunRoute(tab = 4, settingsRoute = it)) },
                onBrowseTag = { tag ->
                    viewModel.browseByTag(tag)
                    navigateTo(TankobunRoute(tab = 2))
                },
                onBrowseAuthor = { author ->
                    viewModel.browseByAuthor(author)
                    navigateTo(TankobunRoute(tab = 2))
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
                ignoreDisplayCutout = effectiveIgnoreDisplayCutout,
                showQuickActionsButton = !readerOpen,
                onOpenQuickDrawer = { quickDrawerMode = QuickDrawerMode.OVERLAY },
                onCloseQuickDrawer = { quickDrawerMode = QuickDrawerMode.CLOSED },
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
                OnboardingDialog(
                    initialLibraryMode = state.libraryMode,
                    initialThemeMode = state.themeMode,
                    onPrepareBrowse = viewModel::prepareOnboardingBrowseContent,
                    onThemeSelected = viewModel::setThemeMode,
                    onComplete = viewModel::completeOnboardingSetup,
                )
            }
            if (state.appTourVisible && !state.onboardingVisible && !readerOpen) {
                AppTourOverlay(
                    libraryMode = state.libraryMode,
                    tourExampleMediaId = tourExampleMedia?.id,
                    onStepChanged = ::showTourStep,
                    onDismiss = viewModel::dismissAppTour,
                )
            }
            if (state.anilistMergePromptVisible && !readerOpen) {
                AniListMergeDialog(
                    onMerge = viewModel::mergeLocalLibraryWithAniList,
                    onUseAniList = viewModel::replaceLocalLibraryWithAniList,
                    onDismiss = viewModel::dismissAniListMergePrompt,
                )
            }
        }
    }
}

@Composable
private fun AniListMergeDialog(
    onMerge: () -> Unit,
    onUseAniList: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
            color = LocalTankobunStyle.current.colors.panel,
            contentColor = LocalTankobunStyle.current.colors.panelContent,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    tankobunString(R.string.anilist_merge_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    tankobunString(R.string.anilist_merge_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(tankobunString(R.string.common_later))
                    }
                    Spacer(Modifier.weight(1f))
                    TankobunActionButton(
                        label = tankobunString(R.string.anilist_merge_use_anilist),
                        onClick = onUseAniList,
                        filled = false,
                    )
                    TankobunActionButton(
                        label = tankobunString(R.string.anilist_merge_keep_local),
                        onClick = onMerge,
                    )
                }
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

@Composable
internal fun ReaderOrientationEffect(
    active: Boolean,
    orientation: ReaderScreenOrientation,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context, view) {
        context.findActivity() ?: view.context.findActivity()
    }
    val requestedOrientation = if (active) {
        orientation.requestedOrientation()
    } else {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    SideEffect {
        activity?.requestedOrientation = requestedOrientation
    }

    DisposableEffect(activity) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }
}

private fun ReaderScreenOrientation.requestedOrientation(): Int =
    when (this) {
        ReaderScreenOrientation.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        ReaderScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ReaderScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
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
    ignoreDisplayCutout: Boolean,
    showQuickActionsButton: Boolean,
    onOpenQuickDrawer: () -> Unit,
    onCloseQuickDrawer: () -> Unit,
) {
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
    val routeBackdropColor = LocalTankobunTokens.current.appBackdrop
    val routeContentColor = MaterialTheme.colorScheme.onSurface
    val statusBarInset = if (showStatusBar) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }
    val topChromeInset = statusBarInset + if (compactLayout) 48.dp else 72.dp
    val bottomChromeInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() +
        FrostedDockBottomMargin +
        FrostedDockHeight +
        FrostedDockTopMargin
    val chromeInsets = TankobunChromeInsets(top = topChromeInset, bottom = bottomChromeInset)
    val chromeHazeState = remember { HazeState() }

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

    fun toggleQuickDrawer() {
        when (quickDrawerMode) {
            QuickDrawerMode.CLOSED -> openQuickDrawerFromClosed()
            QuickDrawerMode.OVERLAY -> closeQuickDrawerFromOverlay()
        }
    }

    CompositionLocalProvider(LocalTankobunChromeInsets provides chromeInsets) {
        Scaffold(
            containerColor = routeBackdropColor,
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                TankobunTopBar(
                    title = selectedMedia?.title?.userPreferred
                        ?: when (selectedTab) {
                            0 -> tankobunString(R.string.nav_home)
                            1 -> tankobunString(R.string.common_library)
                            2 -> tankobunString(R.string.common_browse)
                            3 -> tankobunString(R.string.common_downloads)
                            4 -> settingsRoute.settingsTitle()
                            else -> tankobunString(R.string.app_name)
                        },
                    pageIcon = when (selectedTab) {
                        1 -> TankobunIcons.LibraryBooks
                        2 -> TankobunIcons.Explore
                        3 -> TankobunIcons.Download
                        4 -> TankobunIcons.Settings
                        else -> TankobunIcons.Home
                    },
                    hazeState = chromeHazeState,
                    showBack = canNavigateBack,
                    ignoreDisplayCutout = ignoreDisplayCutout,
                    showStatusBar = showStatusBar,
                    mediaDetailActive = mediaDetailActive,
                    onBack = onNavigateBack,
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = displayCutoutStartPadding(ignoreDisplayCutout = ignoreDisplayCutout),
                            end = displayCutoutEndPadding(ignoreDisplayCutout = ignoreDisplayCutout),
                        )
                        .padding(
                            start = FrostedDockHorizontalMargin,
                            top = FrostedDockTopMargin,
                            end = FrostedDockHorizontalMargin,
                            bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() +
                                FrostedDockBottomMargin,
                        ),
                    contentAlignment = state.dockAlignment.dockContentAlignment(),
                ) {
                    TankobunGlassChrome(
                        modifier = Modifier
                            .width(if (showQuickActionsButton) FrostedDockWithQuickActionsWidth else FrostedDockWidth)
                            .height(FrostedDockHeight),
                        shape = FrostedDockShape,
                        hazeState = chromeHazeState,
                        contentColor = routeContentColor,
                        tintAlpha = FrostedGlassTintAlpha,
                        blurLayerAlpha = FrostedGlassDimAlpha,
                        borderAlpha = 0f,
                        washAlpha = FrostedGlassWashAlpha,
                        shadowElevation = 0.dp,
                    ) {
                        TankobunBottomNavigationBar(
                            selectedTab = selectedTab,
                            onSelectTab = onSelectTab,
                            quickActionsVisible = showQuickActionsButton,
                            quickActionsOpen = quickDrawerMode != QuickDrawerMode.CLOSED,
                            onToggleQuickActions = ::toggleQuickDrawer,
                            indicatorAnimation = state.dockIndicatorAnimation,
                            modifier = Modifier.height(FrostedDockHeight),
                        )
                    }
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = padding.calculateStartPadding(LocalLayoutDirection.current),
                        end = padding.calculateEndPadding(LocalLayoutDirection.current),
                    ),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(start = cutoutStartPadding, end = cutoutEndPadding)
                        .blur(drawerBackdropBlur)
                        .background(routeBackdropColor)
                        .hazeSource(state = chromeHazeState),
                ) {
                    Row(Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            when (selectedTab) {
                                0 -> HomeScreen(
                                    state = state,
                                    onSelectMedia = onSelectMedia,
                                    onOpenRecentProgress = onOpenRecentProgress,
                                    onOpenLibrary = { onSelectTab(1) },
                                    onOpenBrowse = { onSelectTab(2) },
                                )
                                1 -> LibraryScreen(state, viewModel, onSelectMedia = onSelectMedia)
                                2 -> BrowseScreen(state, viewModel, onSelectMedia = onSelectMedia)
                                3 -> DownloadsScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    onOpenStorageManager = { onOpenSettingsRoute(SettingsRoute.DOWNLOADS) },
                                )
                                4 -> SettingsScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    route = settingsRoute,
                                    onOpenRoute = onOpenSettingsRoute,
                                )
                            }
                            selectedMedia?.let { media ->
                                MangaDetailScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    media = media,
                                    onSelectMedia = onSelectMedia,
                                    onBrowseTag = onBrowseTag,
                                    onBrowseAuthor = onBrowseAuthor,
                                )
                            }
                        }
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
                        onClose = { closeQuickDrawerFromOverlay() },
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
                            .fillMaxHeight()
                            .graphicsLayer { translationX = overlayDrawerTranslationPx },
                    )
                }
                if (state.recommendationImportPreview != null) {
                    RecommendationImportDialog(state = state, viewModel = viewModel)
                }
                if (state.busy && !(state.sourcePickerOpen && state.sourcePickerLoading)) {
                    LinearProgressIndicator(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = chromeInsets.top),
                    )
                }
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
internal fun hasTabletDisplayCutout(): Boolean {
    val configuration = LocalConfiguration.current
    if (configuration.smallestScreenWidthDp in 1 until 600) return false
    return hasDisplayCutout()
}

@Composable
internal fun effectiveIgnoreDisplayCutout(ignoreDisplayCutout: Boolean): Boolean =
    ignoreDisplayCutout || !hasTabletDisplayCutout()

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
    pageIcon: ImageVector,
    hazeState: HazeState,
    showBack: Boolean,
    ignoreDisplayCutout: Boolean,
    showStatusBar: Boolean,
    mediaDetailActive: Boolean = false,
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
    val horizontalPadding = 18.dp
    val iconSize = if (compact) 18.dp else 24.dp
    val spacing = if (compact) 7.dp else 12.dp
    val contentColor = LocalTankobunStyle.current.colors.panelContent
    TankobunGlassChrome(
        contentColor = contentColor,
        shape = FrostedTopBarShape,
        hazeState = hazeState,
        tintAlpha = FrostedGlassTintAlpha,
        blurLayerAlpha = FrostedGlassDimAlpha,
        borderAlpha = 0f,
        washAlpha = FrostedGlassWashAlpha,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight + statusBarInset),
        ) {
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
                            TankobunIcons.ArrowBack,
                            contentDescription = tankobunString(R.string.common_back),
                            modifier = Modifier.size(iconSize),
                        )
                    }
                }
                if (!showBack) {
                    Icon(
                        imageVector = pageIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(27.dp),
                    )
                }
                Text(
                    text = if (mediaDetailActive) title else title.uppercase(Locale.ROOT),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = when {
                        mediaDetailActive && compact -> MaterialTheme.typography.titleMedium
                        mediaDetailActive -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = TankobunDisplayFontFamily,
                            fontSize = 25.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.2.sp,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun TankobunGlassChrome(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    contentColor: Color = LocalTankobunStyle.current.colors.panelContent,
    tintAlpha: Float = 0.88f,
    blurLayerAlpha: Float = 0.46f,
    hazeState: HazeState? = null,
    borderAlpha: Float = 0.22f,
    washAlpha: Float = 0.18f,
    blurRadius: Dp = FrostedGlassBlur,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val colors = LocalTankobunStyle.current.colors
    val lightBackdrop = colors.backdrop.luminance() >= 0.5f
    val surfaceTint = colors.panel.copy(alpha = (tintAlpha * 2.2f).coerceIn(0.48f, 0.86f))
    val colorBleedTint = colors.panel.copy(alpha = tintAlpha.coerceIn(0f, 0.56f))
    val dimTint = if (lightBackdrop) {
        Color.White.copy(alpha = (blurLayerAlpha * 0.56f).coerceIn(0f, 0.28f))
    } else {
        Color.Black.copy(alpha = (blurLayerAlpha * 0.8f).coerceIn(0f, 0.36f))
    }
    val glassWash = colors.panel.copy(alpha = if (hazeState != null) washAlpha.coerceIn(0f, 1f) else 0f)
    val sheen = Color.White.copy(alpha = if (lightBackdrop) 0.08f else 0.05f)
    val border = BorderStroke(1.dp, colors.outline.copy(alpha = borderAlpha.coerceIn(0f, 1f)))
    val hazeStyle = HazeStyle(
        backgroundColor = colors.panel,
        tints = listOf(
            HazeTint(colorBleedTint),
            HazeTint(dimTint),
        ),
        blurRadius = blurRadius,
        noiseFactor = FrostedGlassNoiseFactor,
        fallbackTint = HazeTint(surfaceTint),
    )
    val chromeModifier = if (hazeState != null) {
        modifier
            .clip(shape)
            .hazeEffect(state = hazeState, style = hazeStyle) {
                inputScale = HazeInputScale.Fixed(FrostedGlassInputScale)
                forceInvalidateOnPreDraw = true
            }
    } else {
        modifier.clip(shape)
    }

    Surface(
        modifier = chromeModifier,
        shape = shape,
        color = if (hazeState != null) Color.Transparent else surfaceTint,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = shadowElevation,
        border = border.takeIf { borderAlpha > 0f },
    ) {
        Box(
            modifier = Modifier.clip(shape),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(glassWash),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(sheen),
            )
            content()
        }
    }
}

private fun DockAlignment.dockContentAlignment(): Alignment =
    when (this) {
        DockAlignment.LEFT -> Alignment.CenterStart
        DockAlignment.CENTER -> Alignment.Center
        DockAlignment.RIGHT -> Alignment.CenterEnd
    }

@Composable
internal fun AnimatedHamburgerCloseIcon(
    close: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Icon(
        imageVector = if (close) TankobunIcons.Close else TankobunIcons.Menu,
        contentDescription = contentDescription,
        tint = iconColor,
        modifier = modifier,
    )
}

@Composable
internal fun TankobunBottomNavigationBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    quickActionsVisible: Boolean = false,
    quickActionsOpen: Boolean = false,
    onToggleQuickActions: (() -> Unit)? = null,
    indicatorAnimation: DockIndicatorAnimation = DockIndicatorAnimation.POP,
    modifier: Modifier = Modifier,
) {
    val styleColors = LocalTankobunStyle.current.colors
    val items = listOf(
        Triple(tankobunString(R.string.nav_home), TankobunIcons.Home, 0),
        Triple(tankobunString(R.string.nav_library), TankobunIcons.LibraryBooks, 1),
        Triple(tankobunString(R.string.nav_browse), TankobunIcons.Explore, 2),
        Triple(tankobunString(R.string.nav_downloads), TankobunIcons.Download, 3),
        Triple(tankobunString(R.string.nav_settings), TankobunIcons.Settings, 4),
    )
    val selectedIndex = selectedTab.coerceIn(0, items.lastIndex)
    val itemSize = 44.dp
    val itemSpacing = 8.dp
    val indicatorSize = 40.dp

    Box(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        DockSelectionIndicator(
            selectedIndex = selectedIndex,
            animation = indicatorAnimation,
            itemSize = itemSize,
            itemSpacing = itemSpacing,
            indicatorSize = indicatorSize,
            color = styleColors.accent.copy(alpha = 0.88f),
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { (label, icon, index) ->
                val selected = selectedTab == index
                val iconColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        styleColors.panelContent.copy(alpha = 0.72f)
                    },
                    animationSpec = tween(durationMillis = 160),
                    label = "Dock icon color",
                )
                Box(
                    modifier = Modifier
                        .size(itemSize)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectTab(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            if (quickActionsVisible && onToggleQuickActions != null) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(styleColors.panelContent.copy(alpha = 0.22f)),
                )
                val quickActionIconColor by animateColorAsState(
                    targetValue = styleColors.panelContent.copy(alpha = if (quickActionsOpen) 0.96f else 0.72f),
                    animationSpec = tween(durationMillis = 160),
                    label = "Quick actions dock icon color",
                )
                Box(
                    modifier = Modifier
                        .size(itemSize)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleQuickActions,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedHamburgerCloseIcon(
                        close = quickActionsOpen,
                        contentDescription = if (quickActionsOpen) tankobunString(R.string.common_close) else tankobunString(R.string.common_options),
                        iconColor = quickActionIconColor,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DockSelectionIndicator(
    selectedIndex: Int,
    animation: DockIndicatorAnimation,
    itemSize: Dp,
    itemSpacing: Dp,
    indicatorSize: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val itemSizePx = with(density) { itemSize.toPx() }
    val itemStridePx = with(density) { (itemSize + itemSpacing).toPx() }
    val indicatorSizePx = with(density) { indicatorSize.toPx() }
    val maxStretchPx = with(density) { 32.dp.toPx() }
    val initialCenterPx = itemSizePx / 2f + itemStridePx * selectedIndex
    val centerPx = remember { Animatable(initialCenterPx) }
    val trailCenterPx = remember { Animatable(initialCenterPx) }
    val stretchPx = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }
    val leftEdgePx = remember { Animatable(initialCenterPx - indicatorSizePx / 2f) }
    val rightEdgePx = remember { Animatable(initialCenterPx + indicatorSizePx / 2f) }

    LaunchedEffect(selectedIndex, itemStridePx, indicatorSizePx, animation) {
        val targetCenterPx = itemSizePx / 2f + itemStridePx * selectedIndex
        when (animation) {
            DockIndicatorAnimation.BOUNCY -> {
                alpha.snapTo(1f)
                scale.snapTo(1f)
                val travelPx = abs(targetCenterPx - centerPx.value)
                val stretchTargetPx = (travelPx * 0.42f).coerceIn(0f, maxStretchPx * 0.82f)
                val slide = launch {
                    centerPx.animateTo(
                        targetValue = targetCenterPx,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
                val stretch = launch {
                    if (stretchTargetPx > 0f) {
                        stretchPx.animateTo(stretchTargetPx, tween(durationMillis = 90))
                    }
                    stretchPx.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
                joinAll(slide, stretch)
                trailCenterPx.snapTo(targetCenterPx)
                leftEdgePx.snapTo(targetCenterPx - indicatorSizePx / 2f)
                rightEdgePx.snapTo(targetCenterPx + indicatorSizePx / 2f)
            }

            DockIndicatorAnimation.INCHWORM -> {
                alpha.snapTo(1f)
                scale.snapTo(1f)
                stretchPx.snapTo(0f)
                val currentCenterPx = (leftEdgePx.value + rightEdgePx.value) / 2f
                if (abs(currentCenterPx - centerPx.value) > itemStridePx) {
                    leftEdgePx.snapTo(centerPx.value - indicatorSizePx / 2f)
                    rightEdgePx.snapTo(centerPx.value + indicatorSizePx / 2f)
                }
                val targetLeftPx = targetCenterPx - indicatorSizePx / 2f
                val targetRightPx = targetCenterPx + indicatorSizePx / 2f
                if (targetCenterPx >= (leftEdgePx.value + rightEdgePx.value) / 2f) {
                    rightEdgePx.animateTo(targetRightPx, tween(durationMillis = 155))
                    delay(35L)
                    leftEdgePx.animateTo(
                        targetValue = targetLeftPx,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                } else {
                    leftEdgePx.animateTo(targetLeftPx, tween(durationMillis = 155))
                    delay(35L)
                    rightEdgePx.animateTo(
                        targetValue = targetRightPx,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
                centerPx.snapTo(targetCenterPx)
                trailCenterPx.snapTo(targetCenterPx)
            }

            DockIndicatorAnimation.RUBBER_BAND -> {
                alpha.snapTo(1f)
                scale.snapTo(1f)
                val travelPx = abs(targetCenterPx - centerPx.value)
                val stretchTargetPx = (travelPx * 0.58f).coerceIn(0f, maxStretchPx)
                val slide = launch {
                    centerPx.animateTo(
                        targetValue = targetCenterPx,
                        animationSpec = spring(
                            dampingRatio = 0.58f,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
                val stretch = launch {
                    if (stretchTargetPx > 0f) {
                        stretchPx.animateTo(stretchTargetPx, tween(durationMillis = 105))
                    }
                    stretchPx.animateTo(-indicatorSizePx * 0.08f, tween(durationMillis = 80))
                    stretchPx.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
                joinAll(slide, stretch)
                trailCenterPx.snapTo(targetCenterPx)
                leftEdgePx.snapTo(targetCenterPx - indicatorSizePx / 2f)
                rightEdgePx.snapTo(targetCenterPx + indicatorSizePx / 2f)
            }

            DockIndicatorAnimation.POP -> {
                stretchPx.snapTo(0f)
                val shrink = launch { scale.animateTo(0.56f, tween(durationMillis = 65)) }
                val fade = launch { alpha.animateTo(0.48f, tween(durationMillis = 65)) }
                joinAll(shrink, fade)
                centerPx.snapTo(targetCenterPx)
                trailCenterPx.snapTo(targetCenterPx)
                leftEdgePx.snapTo(targetCenterPx - indicatorSizePx / 2f)
                rightEdgePx.snapTo(targetCenterPx + indicatorSizePx / 2f)
                val grow = launch { scale.animateTo(1.14f, tween(durationMillis = 115)) }
                val appear = launch { alpha.animateTo(1f, tween(durationMillis = 90)) }
                joinAll(grow, appear)
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }

            DockIndicatorAnimation.COMET -> {
                alpha.snapTo(1f)
                scale.snapTo(1f)
                stretchPx.snapTo(0f)
                val slide = launch {
                    centerPx.animateTo(
                        targetValue = targetCenterPx,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
                val tail = launch {
                    delay(30L)
                    trailCenterPx.animateTo(
                        targetValue = targetCenterPx,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 105f,
                        ),
                    )
                }
                joinAll(slide, tail)
                leftEdgePx.snapTo(targetCenterPx - indicatorSizePx / 2f)
                rightEdgePx.snapTo(targetCenterPx + indicatorSizePx / 2f)
            }
        }
    }

    Canvas(modifier = modifier) {
        val centerY = size.height / 2f
        val base = indicatorSizePx
        when (animation) {
            DockIndicatorAnimation.BOUNCY -> {
                val width = base + stretchPx.value.coerceAtLeast(0f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(centerPx.value - width / 2f, centerY - base / 2f),
                    size = Size(width, base),
                    cornerRadius = CornerRadius(base / 2f, base / 2f),
                )
            }

            DockIndicatorAnimation.INCHWORM -> {
                val left = minOf(leftEdgePx.value, rightEdgePx.value)
                val right = maxOf(leftEdgePx.value, rightEdgePx.value)
                val height = base
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, centerY - height / 2f),
                    size = Size((right - left).coerceAtLeast(base * 0.76f), height),
                    cornerRadius = CornerRadius(height / 2f, height / 2f),
                )
            }

            DockIndicatorAnimation.RUBBER_BAND -> {
                val stretch = stretchPx.value.coerceAtLeast(0f)
                val stretchRatio = (stretch / maxStretchPx).coerceIn(0f, 1f)
                val width = base + stretch
                val height = base * (1f - 0.16f * stretchRatio)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(centerPx.value - width / 2f, centerY - height / 2f),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(height / 2f, height / 2f),
                )
            }

            DockIndicatorAnimation.POP -> {
                drawCircle(
                    color = color.copy(alpha = color.alpha * alpha.value),
                    radius = base / 2f * scale.value,
                    center = Offset(centerPx.value, centerY),
                )
            }

            DockIndicatorAnimation.COMET -> {
                val start = trailCenterPx.value
                val end = centerPx.value
                val left = minOf(start, end)
                val right = maxOf(start, end)
                if (right - left > base * 0.22f) {
                    drawRoundRect(
                        color = color.copy(alpha = color.alpha * 0.16f),
                        topLeft = Offset(left, centerY - base * 0.08f),
                        size = Size(right - left, base * 0.16f),
                        cornerRadius = CornerRadius(base * 0.08f, base * 0.08f),
                    )
                }
                (5 downTo 1).forEach { index ->
                    val amount = index / 6f
                    val x = start + (end - start) * amount
                    drawCircle(
                        color = color.copy(alpha = color.alpha * (0.1f + 0.06f * index)),
                        radius = base * (0.08f + 0.035f * index),
                        center = Offset(x, centerY),
                    )
                }
                drawCircle(color = color, radius = base / 2f, center = Offset(end, centerY))
            }
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
    onClose: () -> Unit,
    drawerWidth: Dp,
    endPadding: Dp,
    showHandle: Boolean = true,
    handleCenterOffset: Dp = 0.dp,
    onHandleDragOffset: (Float) -> Unit = {},
    onHandleDragEnd: (Float) -> Unit = {},
    handleDragLocally: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val handleSlotWidth = if (showHandle) QuickDrawerHandleSlotWidth else 0.dp
    val chromeInsets = LocalTankobunChromeInsets.current
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
            shadowElevation = 10.dp,
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(drawerWidth)
                        .fillMaxHeight()
                        .padding(
                            top = chromeInsets.top,
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = 18.dp,
                                top = 18.dp,
                                end = 18.dp,
                                bottom = 18.dp + chromeInsets.bottom,
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        QuickDrawerSection(title = tankobunString(R.string.detail_track_manga)) {
                            if (selectedMedia != null) {
                                AniListTrackingSection(state, viewModel, selectedMedia)
                            } else {
                                Text(
                                    tankobunString(R.string.quick_open_manga_tracking),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        QuickDrawerSection(title = tankobunString(R.string.chapter_resume_reading)) {
                            if (state.recentReadingProgress.isNotEmpty()) {
                                state.recentReadingProgress.forEach { item ->
                                    RecentReadingAction(item = item, onClick = { onOpenRecentProgress(item) })
                                }
                            } else {
                                Text(
                                    tankobunString(R.string.quick_start_reading_resume),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        QuickDrawerSection(title = tankobunString(R.string.common_downloads)) {
                            if (state.downloads.isEmpty()) {
                                Text(tankobunString(R.string.downloads_no_queued), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                state.downloads.take(4).forEach { job ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(TankobunIcons.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(job.chapterName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                job.state.statusLabel(),
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
        }
        if (showHandle) {
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
                item.chapter?.name
                    ?: item.progress.chapterNumber.takeIf { it > 0 }?.let { tankobunString(R.string.reader_chapter_number, it.toString()) }
                    ?: tankobunString(R.string.reader_saved_chapter),
                tankobunString(R.string.reader_page_fraction, item.progress.pageIndex + 1, item.progress.totalPages),
            ).joinToString(" / "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        TankobunActionButton(
            label = if (item.chapter == null) tankobunString(R.string.reader_open_manga) else tankobunString(R.string.chapter_resume),
            icon = TankobunIcons.MenuBook,
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
