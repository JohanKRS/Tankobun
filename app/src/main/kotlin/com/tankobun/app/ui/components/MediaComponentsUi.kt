package com.tankobun.app.ui.components

import com.tankobun.app.ui.icons.TankobunIcons

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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.unit.sp
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
internal fun MediaViewModeRow(
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
            MediaViewMode.COVER_GRID,
            MediaViewMode.COVER_WITH_INFO,
            MediaViewMode.LIST,
        ).forEach { mode ->
            TankobunChip(
                selected = selectedMode == mode,
                onClick = { onSelect(mode) },
                leadingIcon = { TankobunChipIcon(mode.mediaViewIcon()) },
                label = { Text(mode.mediaViewLabel()) },
            )
        }
    }
}

private fun MediaViewMode.mediaViewIcon(): ImageVector =
    when (this) {
        MediaViewMode.COVER_GRID -> TankobunIcons.ViewModule
        MediaViewMode.COVER_WITH_INFO -> TankobunIcons.ViewAgenda
        MediaViewMode.LIST -> TankobunIcons.ViewList
        else -> TankobunIcons.ViewModule
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MediaCollection(
    media: List<AnilistMedia>,
    viewMode: MediaViewMode,
    coverColumns: Int,
    showWholeCovers: Boolean,
    onSelectMedia: (AnilistMedia) -> Unit,
    modifier: Modifier = Modifier,
    trackedStatuses: Map<Int, MediaStatus> = emptyMap(),
    header: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    emptyMessage: String? = null,
    isLoadingMore: Boolean = false,
    onNearEnd: (() -> Unit)? = null,
    nearEndThreshold: Int = 8,
    onScrollOffsetChange: ((Int) -> Unit)? = null,
    scrollToContentOffsetPx: Int? = null,
    scrollToContentOffsetRequest: Int = 0,
    onContentHeightMeasured: ((Int) -> Unit)? = null,
    providedListState: LazyListState? = null,
    providedGridState: LazyGridState? = null,
    selectedMediaIds: Set<Int> = emptySet(),
    selectionMode: Boolean = false,
    onToggleMediaSelection: ((AnilistMedia) -> Unit)? = null,
    onLongPressMedia: ((AnilistMedia) -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val supportedCoverColumns = coverColumns
        .supportedCoverColumns()
        .coerceAtMost(if (configuration.smallestScreenWidthDp in 1 until 600) 4 else 8)

    if (media.isEmpty()) {
        val listState = providedListState ?: rememberLazyListState()
        val latestOnScrollOffsetChange by rememberUpdatedState(onScrollOffsetChange)
        val latestOnContentHeightMeasured by rememberUpdatedState(onContentHeightMeasured)
        LaunchedEffect(listState, latestOnScrollOffsetChange != null) {
            if (latestOnScrollOffsetChange == null) return@LaunchedEffect
            snapshotFlow {
                if (listState.firstVisibleItemIndex == 0) {
                    listState.firstVisibleItemScrollOffset
                } else {
                    Int.MAX_VALUE
                }
            }
                .distinctUntilChanged()
                .collect { scrollOffset -> latestOnScrollOffsetChange?.invoke(scrollOffset) }
        }
        LaunchedEffect(listState, latestOnContentHeightMeasured != null) {
            if (latestOnContentHeightMeasured == null) return@LaunchedEffect
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                val items = layoutInfo.visibleItemsInfo
                if (items.isNotEmpty() && items.size == layoutInfo.totalItemsCount) {
                    val top = items.minOf { it.offset }
                    val bottom = items.maxOf { it.offset + it.size }
                    bottom - top
                } else {
                    null
                }
            }
                .distinctUntilChanged()
                .collect { height -> height?.let { latestOnContentHeightMeasured?.invoke(it) } }
        }
        LaunchedEffect(scrollToContentOffsetRequest, scrollToContentOffsetPx) {
            val targetOffset = scrollToContentOffsetPx ?: return@LaunchedEffect
            if (scrollToContentOffsetRequest > 0) {
                listState.scrollToItem(0, targetOffset)
            }
        }
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            header?.let { headerContent ->
                item(key = "media-header") {
                    headerContent()
                }
            }
            item(key = "media-empty") {
                TankobunEmptyState(title = emptyMessage ?: tankobunString(R.string.empty_no_manga_here))
            }
        }
        return
    }

    val supportedViewMode = viewMode.supportedMediaViewMode()
    when (supportedViewMode) {
        MediaViewMode.LIST -> {
            val listState = providedListState ?: rememberLazyListState()
            val latestOnNearEnd by rememberUpdatedState(onNearEnd)
            val latestOnScrollOffsetChange by rememberUpdatedState(onScrollOffsetChange)
            val latestOnContentHeightMeasured by rememberUpdatedState(onContentHeightMeasured)
            LaunchedEffect(listState, media.size, latestOnNearEnd != null) {
                if (latestOnNearEnd == null) return@LaunchedEffect
                snapshotFlow {
                    val layoutInfo = listState.layoutInfo
                    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    layoutInfo.totalItemsCount > 0 &&
                        lastVisibleIndex >= layoutInfo.totalItemsCount - nearEndThreshold - 1
                }
                    .distinctUntilChanged()
                    .collect { nearEnd ->
                        if (nearEnd) latestOnNearEnd?.invoke()
                    }
            }
            LaunchedEffect(listState, latestOnScrollOffsetChange != null) {
                if (latestOnScrollOffsetChange == null) return@LaunchedEffect
                snapshotFlow {
                    if (listState.firstVisibleItemIndex == 0) {
                        listState.firstVisibleItemScrollOffset
                    } else {
                        Int.MAX_VALUE
                    }
                }
                    .distinctUntilChanged()
                    .collect { scrollOffset -> latestOnScrollOffsetChange?.invoke(scrollOffset) }
            }
            LaunchedEffect(listState, media.size, header != null, latestOnContentHeightMeasured != null) {
                if (latestOnContentHeightMeasured == null) return@LaunchedEffect
                snapshotFlow {
                    val layoutInfo = listState.layoutInfo
                    val mediaStartIndex = if (header == null) 0 else 1
                    val mediaEndIndex = mediaStartIndex + media.size
                    val items = layoutInfo.visibleItemsInfo.filter { it.index in mediaStartIndex until mediaEndIndex }
                    if (items.size == media.size) {
                        val top = items.minOf { it.offset }
                        val bottom = items.maxOf { it.offset + it.size }
                        bottom - top
                    } else {
                        null
                    }
                }
                    .distinctUntilChanged()
                    .collect { height -> height?.let { latestOnContentHeightMeasured?.invoke(it) } }
            }
            LaunchedEffect(scrollToContentOffsetRequest, scrollToContentOffsetPx, media.size) {
                val targetOffset = scrollToContentOffsetPx ?: return@LaunchedEffect
                if (scrollToContentOffsetRequest > 0 && media.isNotEmpty()) {
                    listState.scrollToItem(0, targetOffset)
                }
            }
            LazyColumn(
                state = listState,
                modifier = modifier,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                header?.let { headerContent ->
                    item(key = "media-header") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            headerContent()
                        }
                    }
                }
                items(
                    items = media,
                    key = { it.id },
                    contentType = { "media-row" },
                ) { item ->
                    MediaRow(
                        media = item,
                        trackedStatus = trackedStatuses[item.id],
                        selected = item.id in selectedMediaIds,
                        selectionMode = selectionMode,
                        onClick = {
                            if (selectionMode) {
                                onToggleMediaSelection?.invoke(item)
                            } else {
                                onSelectMedia(item)
                            }
                        },
                        onLongClick = { onLongPressMedia?.invoke(item) },
                    )
                }
                if (isLoadingMore) {
                    item(key = "media-loading-more") {
                        MediaLoadingMoreFooter()
                    }
                }
            }
        }
        else -> {
            val gridState = providedGridState ?: rememberLazyGridState()
            val latestOnNearEnd by rememberUpdatedState(onNearEnd)
            val latestOnScrollOffsetChange by rememberUpdatedState(onScrollOffsetChange)
            val latestOnContentHeightMeasured by rememberUpdatedState(onContentHeightMeasured)
            LaunchedEffect(gridState, media.size, latestOnNearEnd != null) {
                if (latestOnNearEnd == null) return@LaunchedEffect
                snapshotFlow {
                    val layoutInfo = gridState.layoutInfo
                    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    layoutInfo.totalItemsCount > 0 &&
                        lastVisibleIndex >= layoutInfo.totalItemsCount - nearEndThreshold - 1
                }
                    .distinctUntilChanged()
                    .collect { nearEnd ->
                        if (nearEnd) latestOnNearEnd?.invoke()
                    }
            }
            LaunchedEffect(gridState, latestOnScrollOffsetChange != null) {
                if (latestOnScrollOffsetChange == null) return@LaunchedEffect
                snapshotFlow {
                    if (gridState.firstVisibleItemIndex == 0) {
                        gridState.firstVisibleItemScrollOffset
                    } else {
                        Int.MAX_VALUE
                    }
                }
                    .distinctUntilChanged()
                    .collect { scrollOffset -> latestOnScrollOffsetChange?.invoke(scrollOffset) }
            }
            LaunchedEffect(gridState, media.size, header != null, latestOnContentHeightMeasured != null) {
                if (latestOnContentHeightMeasured == null) return@LaunchedEffect
                snapshotFlow {
                    val layoutInfo = gridState.layoutInfo
                    val mediaStartIndex = if (header == null) 0 else 1
                    val mediaEndIndex = mediaStartIndex + media.size
                    val items = layoutInfo.visibleItemsInfo.filter { it.index in mediaStartIndex until mediaEndIndex }
                    if (items.size == media.size) {
                        val top = items.minOf { it.offset.y }
                        val bottom = items.maxOf { it.offset.y + it.size.height }
                        bottom - top
                    } else {
                        null
                    }
                }
                    .distinctUntilChanged()
                    .collect { height -> height?.let { latestOnContentHeightMeasured?.invoke(it) } }
            }
            LaunchedEffect(scrollToContentOffsetRequest, scrollToContentOffsetPx, media.size) {
                val targetOffset = scrollToContentOffsetPx ?: return@LaunchedEffect
                if (scrollToContentOffsetRequest > 0 && media.isNotEmpty()) {
                    gridState.scrollToItem(0, targetOffset)
                }
            }
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(supportedCoverColumns),
                modifier = modifier,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                header?.let { headerContent ->
                    item(
                        key = "media-header",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        headerContent()
                    }
                }
                gridItems(
                    items = media,
                    key = { it.id },
                    contentType = { "media-cover-${supportedViewMode.name}" },
                ) { item ->
                    MediaCoverTile(
                        media = item,
                        viewMode = supportedViewMode,
                        showWholeCover = showWholeCovers,
                        trackedStatus = trackedStatuses[item.id],
                        selected = item.id in selectedMediaIds,
                        selectionMode = selectionMode,
                        onClick = {
                            if (selectionMode) {
                                onToggleMediaSelection?.invoke(item)
                            } else {
                                onSelectMedia(item)
                            }
                        },
                        onLongClick = { onLongPressMedia?.invoke(item) },
                    )
                }
                if (isLoadingMore) {
                    item(
                        key = "media-loading-more",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        MediaLoadingMoreFooter()
                    }
                }
            }
        }
    }
}

@Composable
internal fun MediaLoadingMoreFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = LocalTankobunStyle.current.colors.accent,
        )
    }
}

@Composable
internal fun MediaCoverTile(
    media: AnilistMedia,
    viewMode: MediaViewMode,
    showWholeCover: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trackedStatus: MediaStatus? = null,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val supportedViewMode = viewMode.supportedMediaViewMode()
    val coverModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(2f / 3f)
    val coverCornerRadius = if (showWholeCover) 0.dp else 8.dp
    val titleGap = if (supportedViewMode == MediaViewMode.COVER_WITH_INFO) 0.dp else 6.dp

    Column(
        modifier = modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(coverCornerRadius),
                color = if (showWholeCover) Color.Transparent else MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = if (selected) {
                    androidx.compose.foundation.BorderStroke(2.dp, LocalTankobunStyle.current.colors.accent)
                } else {
                    null
                },
            ) {
                TrackedCoverImage(
                    url = media.coverImage,
                    title = media.title.userPreferred,
                    trackedStatus = trackedStatus,
                    modifier = coverModifier,
                    contentScale = if (showWholeCover) ContentScale.Fit else ContentScale.Crop,
                    imageAlignment = if (showWholeCover) Alignment.BottomCenter else Alignment.Center,
                    cornerRadius = coverCornerRadius,
                )
            }
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                )
            }
        }
        if (supportedViewMode != MediaViewMode.COVER_GRID) {
            Column(verticalArrangement = Arrangement.spacedBy(titleGap)) {
                Text(
                    media.title.userPreferred,
                    style = MaterialTheme.typography.labelLarge.copy(lineHeight = 16.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (supportedViewMode == MediaViewMode.COVER_WITH_INFO) {
                    TankobunMediaStatusLabel(text = media.status.statusLabel())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MediaRow(
    media: AnilistMedia,
    trackedStatus: MediaStatus? = null,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    ElevatedCard(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        shape = LocalTankobunStyle.current.themeShapes.panel,
    ) {
        ListItem(
            headlineContent = {
                Text(
                    media.title.userPreferred,
                    style = MaterialTheme.typography.labelLarge.copy(lineHeight = 16.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "${media.mediaTypeLabel()} / ${media.status.statusLabel()}".uppercase(Locale.ROOT),
                        style = LocalTankobunStyle.current.typography.compactStatus,
                        color = LocalTankobunStyle.current.colors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    media.chapters?.let { chapters ->
                        Text(
                            tankobunQuantityString(R.plurals.chapter_count, chapters, chapters),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            leadingContent = {
                Box {
                    TrackedCoverImage(
                        url = media.coverImage,
                        title = media.title.userPreferred,
                        trackedStatus = trackedStatus,
                        modifier = Modifier.size(width = 56.dp, height = 78.dp),
                    )
                    if (selectionMode) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = null,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(32.dp),
                        )
                    }
                }
            },
        )
    }
}

@Composable
internal fun TrackedCoverImage(
    url: String?,
    title: String,
    trackedStatus: MediaStatus?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    imageAlignment: Alignment = Alignment.Center,
    cornerRadius: Dp = 8.dp,
) {
    Box(modifier = modifier) {
        CoverImage(
            url = url,
            title = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            imageAlignment = imageAlignment,
            cornerRadius = cornerRadius,
        )
        trackedStatus
            ?.takeUnless { it == MediaStatus.UNKNOWN }
            ?.let { status ->
                TrackedMediaStatusBadge(
                    status = status,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                )
            }
    }
}

@Composable
internal fun TrackedMediaStatusBadge(
    status: MediaStatus,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (status) {
        MediaStatus.CURRENT -> LocalTankobunStyle.current.colors.selectedChip
        MediaStatus.PLANNING -> MaterialTheme.colorScheme.tertiaryContainer
        MediaStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
        MediaStatus.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
        MediaStatus.DROPPED -> MaterialTheme.colorScheme.errorContainer
        MediaStatus.REPEATING -> LocalTankobunStyle.current.colors.accent
        MediaStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
    }.copy(alpha = 0.94f)
    val contentColor = when (status) {
        MediaStatus.CURRENT -> LocalTankobunStyle.current.colors.selectedChipContent
        MediaStatus.PLANNING -> MaterialTheme.colorScheme.onTertiaryContainer
        MediaStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimaryContainer
        MediaStatus.PAUSED -> MaterialTheme.colorScheme.onSecondaryContainer
        MediaStatus.DROPPED -> MaterialTheme.colorScheme.onErrorContainer
        MediaStatus.REPEATING -> MaterialTheme.colorScheme.onPrimary
        MediaStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (status) {
        MediaStatus.CURRENT -> TankobunIcons.PlayArrow
        MediaStatus.PLANNING -> TankobunIcons.StarBorder
        MediaStatus.COMPLETED -> TankobunIcons.Check
        MediaStatus.PAUSED -> TankobunIcons.Pause
        MediaStatus.DROPPED -> TankobunIcons.Close
        MediaStatus.REPEATING -> TankobunIcons.Replay
        MediaStatus.UNKNOWN -> TankobunIcons.Check
    }
    Surface(
        modifier = modifier.size(24.dp),
        shape = LocalTankobunStyle.current.themeShapes.indicator,
        color = containerColor,
        contentColor = contentColor,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        ),
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = tankobunString(R.string.media_tracked_status_cd, status.trackedBadgeLabel()),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

internal fun List<LibraryItem>.trackedMediaStatuses(): Map<Int, MediaStatus> =
    associate { item -> item.media.id to item.entry.status }
