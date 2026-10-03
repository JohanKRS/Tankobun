package com.tankobun.app.ui.shell

import com.tankobun.core.model.readingContentKind

import androidx.compose.material3.AlertDialog

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

import com.tankobun.app.ui.icons.TankobunIcons

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import dev.chrisbanes.haze.hazeSource
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.withTransform
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
    SOURCE_REPOSITORY,
}

internal const val TankobunGithubUrl = "https://github.com/JohanKRS/Tankobun"
internal const val TankobunAniListUrl = "https://anilist.co"

internal val SettingsDetailRoutes = listOf(
    SettingsRoute.ANILIST,
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
        SettingsRoute.ANILIST -> tankobunString(R.string.catalog_accounts)
        SettingsRoute.CUSTOM_LISTS -> tankobunString(R.string.settings_custom_lists)
        SettingsRoute.BACKUPS -> tankobunString(R.string.settings_backups)
        SettingsRoute.ABOUT -> tankobunString(R.string.common_about)
        SettingsRoute.SOURCES, SettingsRoute.SOURCE_REPOSITORY -> tankobunString(R.string.settings_sources)
    }

internal fun SettingsRoute.pageIcon(): ImageVector =
    when (this) {
        SettingsRoute.MAIN -> TankobunIcons.Settings
        SettingsRoute.PROFILE -> TankobunIcons.AccountCircle
        SettingsRoute.APPEARANCE -> TankobunIcons.Palette
        SettingsRoute.LANGUAGES -> TankobunIcons.Translate
        SettingsRoute.LIBRARY -> TankobunIcons.CollectionsBookmark
        SettingsRoute.BROWSE -> TankobunIcons.Explore
        SettingsRoute.READER -> TankobunIcons.MenuBook
        SettingsRoute.DOWNLOADS -> TankobunIcons.Download
        SettingsRoute.ANILIST -> TankobunIcons.Link
        SettingsRoute.CUSTOM_LISTS -> TankobunIcons.FormatListBulleted
        SettingsRoute.BACKUPS -> TankobunIcons.Backup
        SettingsRoute.ABOUT -> TankobunIcons.Info
        SettingsRoute.SOURCES, SettingsRoute.SOURCE_REPOSITORY -> TankobunIcons.Extension
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

private val DockTopMargin = 8.dp
private val DockBottomMargin = 12.dp
private val DockSideMargin = 12.dp
private val RailStartMargin = 12.dp
private val TopBarFadeHeight = 18.dp
internal const val ChromeTransitionMillis = 240
internal const val ChromeBackdropBlurDp = 6f

internal data class TankobunChromeInsets(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp,
)

internal val LocalTankobunChromeInsets = staticCompositionLocalOf { TankobunChromeInsets() }

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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var settingsRoute by rememberSaveable { mutableStateOf(SettingsRoute.MAIN) }
    var routeHistory by remember { mutableStateOf<List<TankobunRoute>>(emptyList()) }
    var lastHomeBackPressAt by remember { mutableLongStateOf(0L) }
    var lastReaderBackPressAt by remember { mutableLongStateOf(0L) }
    val compactLayout = LocalConfiguration.current.smallestScreenWidthDp in 1 until 600
    val selectedMedia = state.selectedMedia
    val readerOpen = state.activeChapter != null
    val browseCanNavigateBack = selectedTab == 2 &&
        selectedMedia == null &&
        (state.hasBrowseQueryOrFilters() || state.browseSearched || state.browseForYouOpen)
    val appStatusBarVisible = state.showAppStatusBar && !readerOpen
    val useDarkStatusBarIcons = !state.themePreference.isDark(isSystemInDarkTheme())
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
        if (normalized.sameDestination(routeNow)) return
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
            resetBackPressWindows()
        } else {
            applyRoute(normalized)
        }
    }

    fun showTourStep(step: AppTourStep) {
        routeHistory = emptyList()
        applyRoute(
            when (step) {
                AppTourStep.HOME -> TankobunRoute(tab = 0)
                AppTourStep.LIBRARY -> TankobunRoute(tab = 1)
                AppTourStep.BROWSE -> TankobunRoute(tab = 2)
                AppTourStep.PROFILE -> TankobunRoute(tab = 3)
                AppTourStep.SETTINGS -> TankobunRoute(tab = 4, settingsRoute = SettingsRoute.MAIN)
            },
        )
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
            selectedTab == 4 -> {
                applyRoute(TankobunRoute(tab = 3))
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

    TankobunTheme(preference = state.themePreference) {
        val effectiveIgnoreDisplayCutout = effectiveIgnoreDisplayCutout(state.ignoreDisplayCutout)
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
                    selectedTab == 4,
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
                    resetBackPressWindows()
                    viewModel.openRecentProgress(item)
                },
                showStatusBar = state.showAppStatusBar,
                ignoreDisplayCutout = effectiveIgnoreDisplayCutout,
                showNavigation = !readerOpen,
            )
            if (readerOpen) {
                FullScreenReader(state, viewModel)
            }
            if (state.onboardingVisible && !readerOpen) {
                OnboardingDialog(
                    initialLibraryMode = state.libraryMode,
                    initialThemePreference = state.themePreference,
                    onPrepareContent = viewModel::prepareOnboardingContent,
                    onThemeSelected = viewModel::setThemePreference,
                    onComplete = viewModel::completeOnboardingSetup,
                )
            }
            if (state.appTourVisible && !state.onboardingVisible && !readerOpen) {
                AppTourOverlay(
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
            shape = LocalTankobunStyle.current.themeShapes.dialog,
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
    showStatusBar: Boolean,
    ignoreDisplayCutout: Boolean,
    showNavigation: Boolean,
) {
    val cutoutStartPadding = displayCutoutStartPadding(ignoreDisplayCutout = ignoreDisplayCutout)
    val cutoutEndPadding = displayCutoutEndPadding(ignoreDisplayCutout = ignoreDisplayCutout)
    val tabStateHolder = rememberSaveableStateHolder()
    val compactLayout = LocalConfiguration.current.smallestScreenWidthDp in 1 until 600
    val configuration = LocalConfiguration.current
    // The rail only earns its width in landscape; a portrait tablet keeps the bottom dock.
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    val useRail = showNavigation && state.useNavigationRail && !compactLayout && landscape
    val routeBackdropColor = LocalTankobunTokens.current.appBackdrop
    val statusBarInset = if (showStatusBar) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }
    val safeBottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    val railInset = if (useRail) RailStartMargin + GlassRailWidth else 0.dp
    val topChromeInset = statusBarInset + if (compactLayout) 48.dp else 72.dp
    val bottomChromeInset = if (useRail) {
        safeBottomInset + 16.dp
    } else {
        safeBottomInset + DockBottomMargin + GlassDockHeight + DockTopMargin
    }
    val chromeInsets = TankobunChromeInsets(top = topChromeInset, bottom = bottomChromeInset)
    val chromeHazeState = remember { HazeState() }
    val selectedDestination = TankobunDestination.forTab(selectedTab)
    val badges = TankobunDestinationBadges(
        downloadsActive = state.downloads.any { it.state == DownloadState.QUEUED || it.state == DownloadState.RUNNING },
    )

    // The dock steps aside while content scrolls down and returns as soon as it scrolls back.
    var dockHidden by remember { mutableStateOf(false) }
    LaunchedEffect(selectedTab, selectedMedia?.id, settingsRoute) { dockHidden = false }
    val dockScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                when {
                    consumed.y < -2f -> dockHidden = true
                    consumed.y > 2f -> dockHidden = false
                }
                return Offset.Zero
            }
        }
    }
    val detailChrome = remember(selectedMedia?.id) { MediaDetailChromeState() }
    val detailActions = rememberMediaDetailUiActions(
        viewModel = viewModel,
        onOpenTracking = { detailChrome.trackingSheetOpen = true },
        onShareMedia = {},
    )
    val showDetailActions = selectedMedia != null && !detailChrome.heroActionsVisible
    val dockOffset by animateDpAsState(
        targetValue = if (dockHidden && !useRail && !showDetailActions) GlassDockHeight + DockBottomMargin + safeBottomInset + 16.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "Dock scroll offset",
    )

    CompositionLocalProvider(LocalTankobunChromeInsets provides chromeInsets) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(routeBackdropColor),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = cutoutStartPadding + railInset, end = cutoutEndPadding)
                    .background(routeBackdropColor)
                    .hazeSource(state = chromeHazeState)
                    .nestedScroll(dockScrollConnection),
            ) {
                tabStateHolder.SaveableStateProvider(selectedTab) {
                    when (selectedTab) {
                        0 -> HomeScreen(
                            state = state,
                            onSelectMedia = onSelectMedia,
                            onOpenRecentProgress = onOpenRecentProgress,
                            onOpenLibrary = {
                                // Explicitly opening the reading list must not restore an old category or filters.
                                tabStateHolder.removeState(1)
                                viewModel.clearLibraryBatchSelection()
                                onSelectTab(1)
                            },
                            onOpenBrowse = { onSelectTab(2) },
                        )
                        1 -> LibraryScreen(state, viewModel, onOpenBrowse = { onSelectTab(2) }, onSelectMedia = onSelectMedia)
                        2 -> BrowseScreen(state, viewModel, onSelectMedia = onSelectMedia)
                        3 -> ProfileScreen(
                            state = state,
                            viewModel = viewModel,
                            onOpenSettingsRoute = onOpenSettingsRoute,
                        )
                        4 -> SettingsScreen(
                            state = state,
                            viewModel = viewModel,
                            route = settingsRoute,
                            onOpenRoute = onOpenSettingsRoute,
                        )
                    }
                }
                selectedMedia?.let { media ->
                    MangaDetailScreen(
                        state = state,
                        viewModel = viewModel,
                        media = media,
                        onSelectMedia = onSelectMedia,
                        onBrowseTag = onBrowseTag,
                        onBrowseAuthor = onBrowseAuthor,
                        onOpenSourceRepository = { onOpenSettingsRoute(SettingsRoute.SOURCE_REPOSITORY) },
                        chrome = detailChrome,
                    )
                }
            }

            TankobunTopBar(
                title = selectedMedia?.title?.userPreferred
                    ?: when (selectedTab) {
                        0 -> tankobunString(R.string.nav_home)
                        1 -> tankobunString(R.string.common_library)
                        2 -> tankobunString(R.string.common_browse)
                        3 -> tankobunString(R.string.nav_you)
                        4 -> settingsRoute.settingsTitle()
                        else -> tankobunString(R.string.app_name)
                    },
                pageIcon = when (selectedTab) {
                    1 -> TankobunIcons.LibraryBooks
                    2 -> TankobunIcons.Explore
                    3 -> TankobunIcons.AccountCircle
                    4 -> settingsRoute.pageIcon()
                    else -> TankobunIcons.Home
                },
                hazeState = chromeHazeState,
                showBack = canNavigateBack,
                ignoreDisplayCutout = ignoreDisplayCutout,
                showStatusBar = showStatusBar,
                mediaDetailActive = selectedMedia != null,
                startInsetExtra = railInset,
                onBack = onNavigateBack,
                actions = {
                    if (selectedMedia == null && selectedTab == 1) {
                        TopBarActionButton(
                            icon = TankobunIcons.Download,
                            contentDescription = if (badges.downloadsActive) {
                                tankobunString(R.string.nav_you_downloads_badge, tankobunString(R.string.common_downloads))
                            } else {
                                tankobunString(R.string.common_downloads)
                            },
                            onClick = { onOpenSettingsRoute(SettingsRoute.DOWNLOADS) },
                            badgeDot = badges.downloadsActive,
                        )
                    }
                },
            )

            if (showNavigation) {
                if (useRail) {
                    TankobunNavigationRail(
                        selected = selectedDestination,
                        badges = badges,
                        hazeState = chromeHazeState,
                        onSelect = { onSelectTab(it.tab) },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = cutoutStartPadding + RailStartMargin),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = cutoutStartPadding + DockSideMargin, end = cutoutEndPadding + DockSideMargin)
                            .padding(bottom = safeBottomInset + DockBottomMargin)
                            .offset { IntOffset(0, dockOffset.roundToPx()) },
                    ) {
                        AnimatedContent(
                            targetState = showDetailActions,
                            transitionSpec = {
                                (fadeIn(tween(ChromeTransitionMillis)) + scaleIn(tween(ChromeTransitionMillis), initialScale = 0.92f))
                                    .togetherWith(fadeOut(tween(ChromeTransitionMillis / 2)))
                                    .using(SizeTransform(clip = false))
                            },
                            contentAlignment = Alignment.BottomCenter,
                            label = "Dock or detail actions",
                        ) { detailActionsVisible ->
                            if (detailActionsVisible) {
                                TankobunGlassFloat(
                                    hazeState = chromeHazeState,
                                    shape = RoundedCornerShape(percent = 50),
                                ) {
                                    MediaDetailFloatingActions(state = state, actions = detailActions)
                                }
                            } else {
                                TankobunNavigationDock(
                                    selected = selectedDestination,
                                    badges = badges,
                                    hazeState = chromeHazeState,
                                    onSelect = { onSelectTab(it.tab) },
                                )
                            }
                        }
                    }
                }
            }
            if (useRail && showDetailActions) {
                TankobunGlassFloat(
                    hazeState = chromeHazeState,
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = cutoutStartPadding + railInset, end = cutoutEndPadding)
                        .padding(bottom = safeBottomInset + DockBottomMargin),
                ) {
                    MediaDetailFloatingActions(state = state, actions = detailActions)
                }
            }

            state.extensionTrustReview?.let { candidate ->
                ExtensionTrustDialog(
                    candidate = candidate,
                    onDismiss = viewModel::dismissExtensionTrustReview,
                    onConfirm = { viewModel.trustExtension(candidate) },
                )
            }
            if (state.recommendationImportPreview != null) {
                RecommendationImportDialog(state = state, viewModel = viewModel)
            }
            state.recommendationImportError?.let { error ->
                AlertDialog(
                    shape = LocalTankobunStyle.current.themeShapes.dialog,
                    containerColor = LocalTankobunStyle.current.colors.panel,
                    titleContentColor = LocalTankobunStyle.current.colors.panelContent,
                    textContentColor = LocalTankobunStyle.current.colors.mutedContent,
                    tonalElevation = 0.dp,
                    onDismissRequest = viewModel::dismissRecommendationImportError,
                    title = { Text(tankobunString(R.string.msg_recommendations_import_failed)) },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = viewModel::dismissRecommendationImportError) {
                            Text(tankobunString(R.string.common_close))
                        }
                    },
                )
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
    hazeState: HazeState?,
    showBack: Boolean,
    ignoreDisplayCutout: Boolean,
    showStatusBar: Boolean,
    mediaDetailActive: Boolean = false,
    startInsetExtra: Dp = 0.dp,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
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
    val iconSize = if (compact) 18.dp else 24.dp
    val spacing = if (compact) 7.dp else 12.dp
    val tokens = LocalTankobunTokens.current
    val contentColor = LocalTankobunStyle.current.colors.panelContent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight + statusBarInset + TopBarFadeHeight)
            .tankobunGlass(
                hazeState = hazeState,
                surface = tokens.topBarSurface,
                bleed = tokens.topBarBleed,
                backdrop = tokens.appBackdrop,
                dark = tokens.dark,
                fadeOut = true,
            ),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight + statusBarInset)
                    .padding(
                        start = 18.dp + startInset + startInsetExtra,
                        top = statusBarInset,
                        end = 6.dp + endInset,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                if (showBack) {
                    IconButton(onClick = onBack, modifier = Modifier.size(if (compact) 40.dp else 48.dp)) {
                        Icon(
                            TankobunIcons.ArrowBack,
                            contentDescription = tankobunString(R.string.common_back),
                            modifier = Modifier.size(iconSize + 2.dp),
                        )
                    }
                }
                TankobunHeadingLead(
                    title = if (mediaDetailActive) title else title.uppercase(Locale.ROOT),
                    icon = pageIcon,
                    modifier = Modifier.weight(1f),
                    titleColor = contentColor,
                    textStyle = when {
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
                actions()
            }
        }
    }
}
