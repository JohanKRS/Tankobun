package com.tankobun.app.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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

@Composable
internal fun SourcesSettingsScreen(state: TankobunUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val chromeInsets = LocalTankobunChromeInsets.current
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
    val visibleInstalledSourceList = remember(
        searchableInstalledSources,
        state.sourceLanguages,
    ) {
        searchableInstalledSources.filter { source ->
            val language = source.lang.normalizedSourceLanguage()
            language in state.sourceLanguages || language == UNIVERSAL_SOURCE_LANGUAGE
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
    val visibleRepositoryEntries = activeRepositoryEntries
    val launchUninstall: (String) -> Unit = { packageName ->
        requestExtensionUninstall(context, packageName)
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var sourceHeaderHeightPx by remember { mutableIntStateOf(0) }
    var tabHeaderHeightPx by remember { mutableIntStateOf(0) }
    var pageScrollOffsets by remember { mutableStateOf(List(2) { 0 }) }
    var pageScrollRequestOffsets by remember { mutableStateOf(List<Int?>(2) { null }) }
    var pageScrollRequestTokens by remember { mutableStateOf(List(2) { 0 }) }
    val currentPage = pagerState.currentPage.coerceIn(0, 1)
    val targetPage = pagerState.targetPage.coerceIn(0, 1)
    val currentScrollOffsetPx = pageScrollOffsets.getOrElse(currentPage) { 0 }
    val targetScrollOffsetPx = pageScrollOffsets.getOrElse(targetPage) { 0 }
    val currentHeaderIsPinned = sourceHeaderHeightPx > 0 && currentScrollOffsetPx >= sourceHeaderHeightPx
    val scrollOffsetForHeader = if (
        pagerState.isScrollInProgress &&
        targetPage != currentPage &&
        currentHeaderIsPinned
    ) {
        maxOf(currentScrollOffsetPx, targetScrollOffsetPx)
    } else {
        currentScrollOffsetPx
    }
    val headerCollapsePx = scrollOffsetForHeader.coerceAtMost(sourceHeaderHeightPx)
    val headerTranslationY = -headerCollapsePx.toFloat()
    val sharedHeaderHeight = with(density) { (sourceHeaderHeightPx + tabHeaderHeightPx).toDp() }
    var lastObservedPage by remember { mutableIntStateOf(currentPage) }

    fun requestPageScrollOffset(index: Int, offsetPx: Int) {
        if (sourceHeaderHeightPx <= 0) return
        val requestedOffsetPx = offsetPx.coerceAtLeast(0)
        pageScrollOffsets = pageScrollOffsets.toMutableList().also { offsets ->
            if (index in offsets.indices) offsets[index] = requestedOffsetPx
        }
        pageScrollRequestOffsets = pageScrollRequestOffsets.toMutableList().also { offsets ->
            if (index in offsets.indices) offsets[index] = requestedOffsetPx
        }
        pageScrollRequestTokens = pageScrollRequestTokens.toMutableList().also { tokens ->
            if (index in tokens.indices) tokens[index] = tokens[index] + 1
        }
    }

    fun pinPageAtContentTop(index: Int) {
        requestPageScrollOffset(index = index, offsetPx = sourceHeaderHeightPx)
    }

    LaunchedEffect(targetPage, sourceHeaderHeightPx) {
        if (targetPage != currentPage) {
            if (currentHeaderIsPinned) {
                pinPageAtContentTop(targetPage)
            } else {
                requestPageScrollOffset(
                    index = targetPage,
                    offsetPx = currentScrollOffsetPx.coerceAtMost(sourceHeaderHeightPx),
                )
            }
        }
    }

    LaunchedEffect(currentPage, sourceHeaderHeightPx) {
        val previousPage = lastObservedPage
        val previousPageWasPinned = sourceHeaderHeightPx > 0 &&
            pageScrollOffsets.getOrElse(previousPage) { 0 } >= sourceHeaderHeightPx
        lastObservedPage = currentPage
        if (previousPage != currentPage && previousPageWasPinned) {
            pinPageAtContentTop(currentPage)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val contentPaddingPx = with(density) { SourceSettingsContentPadding.roundToPx() }
        val topChromeInsetPx = with(density) { chromeInsets.top.roundToPx() }
        val bottomChromeInsetPx = with(density) { chromeInsets.bottom.roundToPx() }
        val sourceGroupGapPx = with(density) { 12.dp.roundToPx() }
        val sourceGroupHeaderHeightPx = with(density) { 58.dp.roundToPx() }
        val sourceRowHeightPx = with(density) { 58.dp.roundToPx() }
        val emptyStateHeightPx = with(density) { 160.dp.roundToPx() }
        val installedContentHeightPx = if (state.allInstalledSources.isEmpty() || sourceGroups.isEmpty()) {
            emptyStateHeightPx
        } else {
            sourceGroups.sumOf { (_, sources) ->
                sourceGroupHeaderHeightPx + sourceRowHeightPx * sources.size
            } + sourceGroupGapPx * (sourceGroups.size - 1).coerceAtLeast(0)
        }
        val availablePinnedHeightPx = constraints.maxHeight - topChromeInsetPx - bottomChromeInsetPx
        val installedPinnedBottomPaddingPx = availablePinnedHeightPx -
            tabHeaderHeightPx -
            contentPaddingPx -
            installedContentHeightPx
        val installedBottomPadding = with(density) {
            maxOf(contentPaddingPx, installedPinnedBottomPaddingPx).toDp()
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val listState = rememberLazyListState()
            val pageBottomPadding = if (page == 0) installedBottomPadding else SourceSettingsContentPadding
            val pageContentPadding = PaddingValues(
                start = SourceSettingsContentPadding,
                top = chromeInsets.top + sharedHeaderHeight + SourceSettingsContentPadding,
                end = SourceSettingsContentPadding,
                bottom = chromeInsets.bottom + pageBottomPadding,
            )
            LaunchedEffect(listState) {
                snapshotFlow {
                    if (listState.firstVisibleItemIndex == 0) {
                        listState.firstVisibleItemScrollOffset
                    } else {
                        Int.MAX_VALUE
                    }
                }
                    .distinctUntilChanged()
                    .collect { scrollOffset ->
                        pageScrollOffsets = pageScrollOffsets.toMutableList().also { offsets ->
                            if (page in offsets.indices) offsets[page] = scrollOffset
                        }
                    }
            }
            LaunchedEffect(
                pageScrollRequestTokens.getOrElse(page) { 0 },
                pageScrollRequestOffsets.getOrNull(page),
            ) {
                val targetOffset = pageScrollRequestOffsets.getOrNull(page) ?: return@LaunchedEffect
                if (pageScrollRequestTokens.getOrElse(page) { 0 } > 0) {
                    listState.scrollToItem(0, targetOffset)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = pageContentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (page == 0) {
                    if (state.allInstalledSources.isEmpty()) {
                        item(key = "installed-empty") {
                            TankobunEmptyState(title = "No installed Tachiyomi-compatible source extensions found.")
                        }
                    } else if (sourceGroups.isEmpty()) {
                        item(key = "installed-filter-empty") {
                            TankobunEmptyState(title = "No installed sources match this search and language filter.")
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
                                onUninstall = launchUninstall,
                            )
                        }
                    }
                } else {
                    item(key = "repository-controls") {
                        SourceRepositoryControls(
                            repositoryUrl = state.extensionRepositoryUrl,
                            repositoryCount = visibleRepositoryEntries.size,
                            onRepositoryUrlChange = viewModel::setExtensionRepositoryUrl,
                            onRefreshRepository = viewModel::refreshExtensionIndex,
                        )
                    }
                    if (repositoryEntries.isEmpty()) {
                        item(key = "repository-empty") {
                            TankobunEmptyState(title = "Load a repository index to browse installable extensions.")
                        }
                    } else {
                        if (visibleRepositoryEntries.isEmpty()) {
                            item(key = "repository-filter-empty") {
                                TankobunEmptyState(title = "No repository extensions match this search and language filter.")
                            }
                        }
                        items(visibleRepositoryEntries, key = { "${it.packageName}:${it.versionCode}" }) { extension ->
                            ExtensionRepositoryRow(
                                extension = extension,
                                installedSources = installedByPackage[extension.packageName].orEmpty(),
                                iconUrl = viewModel.extensionIconUrl(extension),
                                installing = state.installingExtensionPackageName == extension.packageName,
                                onInstall = { requestExtensionInstall(context, viewModel, extension) },
                                onUninstall = { launchUninstall(extension.packageName) },
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = headerTranslationY }
                .background(LocalTankobunStyle.current.colors.backdrop)
                .padding(top = chromeInsets.top),
        ) {
            Column(
                modifier = Modifier.onSizeChanged { sourceHeaderHeightPx = it.height },
            ) {
                Spacer(Modifier.height(SourceSettingsContentPadding))
                SourceSettingsHeader(
                    activeInstalledCount = state.installedSources.size,
                    visibleInstalledCount = visibleInstalledSourceList.size,
                    query = sourceSettingsQuery,
                    selectedTab = currentPage,
                    onQueryChange = { sourceSettingsQuery = it },
                    modifier = Modifier.padding(horizontal = SourceSettingsContentPadding),
                )
                state.message?.let { message ->
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.padding(horizontal = SourceSettingsContentPadding)) {
                        TankobunMessageBanner(message)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            Column(
                modifier = Modifier.onSizeChanged { tabHeaderHeightPx = it.height },
            ) {
                SourceSettingsTabRow(
                    selectedTab = currentPage,
                    installedCount = visibleInstalledSourceList.size,
                    repositoryCount = visibleRepositoryEntries.size,
                    onSelectTab = { index ->
                        if (currentHeaderIsPinned) {
                            pinPageAtContentTop(index)
                        } else {
                            requestPageScrollOffset(
                                index = index,
                                offsetPx = currentScrollOffsetPx.coerceAtMost(sourceHeaderHeightPx),
                            )
                        }
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f)),
                )
            }
        }
    }
}

@Composable
internal fun SourceSettingsHeader(
    activeInstalledCount: Int,
    visibleInstalledCount: Int,
    query: String,
    selectedTab: Int,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Source extensions",
                    style = LocalTankobunStyle.current.typography.sectionLabel,
                    color = LocalTankobunStyle.current.colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "$activeInstalledCount active / $visibleInstalledCount installed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TankobunSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = if (selectedTab == 0) "Search installed sources" else "Search extensions",
            showSearchAction = false,
        )
    }
}

@Composable
internal fun SourceSettingsTabRow(
    selectedTab: Int,
    installedCount: Int,
    repositoryCount: Int,
    onSelectTab: (Int) -> Unit,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTab,
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        listOf("Installed" to installedCount, "Repository" to repositoryCount).forEachIndexed { index, (label, count) ->
            Tab(
                selected = selectedTab == index,
                onClick = { onSelectTab(index) },
                text = {
                    val tabTextColor = LocalContentColor.current
                    Text(
                        buildAnnotatedString {
                            append(label)
                            append(" ")
                            pushStyle(SpanStyle(color = tabTextColor.copy(alpha = 0.74f)))
                            append(count.toString())
                            pop()
                        },
                        style = LocalTankobunStyle.current.typography.sectionLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
internal fun SourceRepositoryControls(
    repositoryUrl: String,
    repositoryCount: Int,
    onRepositoryUrlChange: (String) -> Unit,
    onRefreshRepository: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Extension repository",
            style = LocalTankobunStyle.current.typography.sectionLabel,
            color = LocalTankobunStyle.current.colors.accent,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = repositoryUrl,
                onValueChange = onRepositoryUrlChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Repository index URL") },
                shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
            )
            TankobunActionButton(
                label = "Load",
                icon = Icons.Default.Refresh,
                onClick = onRefreshRepository,
                modifier = Modifier
                    .height(56.dp)
                    .widthIn(min = 96.dp),
            )
        }
        if (repositoryCount > 0) {
            Text(
                "$repositoryCount extensions shown",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val SourceSettingsContentPadding = 18.dp

@Composable
internal fun SourceLanguageGroupSection(
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
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
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
                    Text(
                        sourceLanguageDisplay(language),
                        style = LocalTankobunStyle.current.typography.sectionLabel,
                        color = LocalTankobunStyle.current.colors.accent,
                    )
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
internal fun SourceSettingsRow(
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
            modifier = Modifier.size(42.dp),
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
            SourceSettingsIconActionButton(
                icon = Icons.Default.Download,
                contentDescription = "Update $displayName",
                enabled = !installing,
                onClick = { onInstall(extension) },
            )
        }
        SourceSettingsIconActionButton(
            icon = Icons.Default.Delete,
            contentDescription = "Uninstall $displayName",
            onClick = { onUninstall(source.packageName) },
        )
        Switch(
            checked = active,
            onCheckedChange = onEnabledChange,
        )
    }
}

@Composable
internal fun SourceSettingsIconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control)
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(LocalTankobunStyle.current.sizes.iconAction)
            .clip(shape),
        shape = shape,
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
internal fun ExtensionRepositoryRow(
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
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
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
                modifier = Modifier.size(42.dp),
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
                shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
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
                SourceSettingsIconActionButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Uninstall $displayName",
                    onClick = onUninstall,
                )
            }
        }
    }
}

internal fun TankobunUiState.sourceActive(source: SourceDescriptor): Boolean =
    installedSources.any { it.sourceSettingsKey() == source.sourceSettingsKey() }

internal fun SourceDescriptor.matchesSourceSettingsQuery(
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

internal fun ExtensionIndexEntry.matchesSourceSettingsQuery(query: String): Boolean {
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

internal fun String.matchesSourceSettingsQuery(query: String): Boolean =
    lowercase().contains(query)

internal fun sourceMetadata(source: SourceDescriptor, active: Boolean): String =
    listOfNotNull(
        sourceLanguageDisplay(source.lang.normalizedSourceLanguage()),
        source.versionName?.let { "v$it" },
        if (source.isNsfw) "NSFW" else null,
        if (active) "active" else "off",
    ).joinToString(" / ")

@Composable
internal fun ExtensionIcon(
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
    Box(
        modifier = modifier.clip(RoundedCornerShape(LocalTankobunStyle.current.radii.control)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            packageIcon != null -> {
                Image(
                    bitmap = packageIcon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            iconUrl != null -> {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            else -> {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
                    color = LocalTankobunStyle.current.colors.accent.copy(alpha = 0.16f),
                    contentColor = LocalTankobunStyle.current.colors.accent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            extensionInitials(name),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

internal fun Drawable.toImageBitmap(): ImageBitmap {
    val drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this is AdaptiveIconDrawable) {
        foreground ?: this
    } else {
        this
    }
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 192
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 192
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    val visibleBounds = bitmap.visibleAlphaBounds()
    val trimmed = if (visibleBounds != null && visibleBounds.width() > 0 && visibleBounds.height() > 0) {
        Bitmap.createBitmap(bitmap, visibleBounds.left, visibleBounds.top, visibleBounds.width(), visibleBounds.height())
    } else {
        bitmap
    }
    return trimmed.asImageBitmap()
}

private fun Bitmap.visibleAlphaBounds(alphaThreshold: Int = 8): Rect? {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val alpha = pixels[rowOffset + x] ushr 24
            if (alpha > alphaThreshold) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }
    if (right < left || bottom < top) return null
    return Rect(left, top, right + 1, bottom + 1)
}

internal fun extensionInitials(name: String): String =
    name.extensionDisplayName()
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

internal val TachiyomiNamePrefix = Regex("^\\s*tachiyomi\\s*:?\\s*", RegexOption.IGNORE_CASE)

internal fun String.extensionDisplayName(): String {
    val cleaned = replace(TachiyomiNamePrefix, "").trim()
    return cleaned.ifBlank { trim() }
}

internal fun requestExtensionInstall(
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

internal fun requestExtensionUninstall(context: Context, packageName: String) {
    if (packageName.isBlank()) {
        Log.w("TankobunSources", "Extension uninstall requested with a blank package name")
        return
    }
    val intent = extensionUninstallIntent(packageName)
    runCatching {
        context.startActivity(intent)
    }.onFailure { error ->
        Log.w("TankobunSources", "Failed to launch extension uninstall for $packageName", error)
    }
}

internal fun downloadedExtensionInstallIntent(installRequest: ExtensionInstallRequest): Intent =
    Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(installRequest.apkUri), "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

internal fun extensionUninstallIntent(packageName: String): Intent =
    Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

internal fun sourceLanguageSortPriority(language: String): Int =
    when (language.normalizedSourceLanguage()) {
        "en" -> 0
        "all" -> 1
        else -> 2
    }

internal fun cacheAgeLabel(syncedAtEpochMillis: Long): String {
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

internal fun String.normalizedSourceLanguage(): String =
    trim().lowercase().replace('_', '-')

internal fun sourceLanguageLabel(language: String): String =
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

internal fun sourceLanguageDisplay(language: String): String =
    sourceLanguageFlag(language)?.let { flag -> "$flag ${sourceLanguageLabel(language)}" }
        ?: sourceLanguageLabel(language)

internal fun sourceLanguageFlag(language: String): String? {
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

internal fun countryFlagEmoji(countryCode: String): String? {
    val normalized = countryCode.uppercase()
        .takeIf { it.length == 2 && it.all { char -> char in 'A'..'Z' } }
        ?: return null
    val codePoints = normalized.map { char -> 0x1F1E6 + (char - 'A') }.toIntArray()
    return String(codePoints, 0, codePoints.size)
}
