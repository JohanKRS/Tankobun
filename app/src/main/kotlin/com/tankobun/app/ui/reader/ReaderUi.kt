package com.tankobun.app.ui.reader

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
import coil3.compose.SubcomposeAsyncImage
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
internal fun FullScreenReader(state: TankobunUiState, viewModel: MainViewModel) {
    val chapter = state.activeChapter ?: return
    if (state.readerPages.isEmpty()) {
        ReaderLoadingScreen(chapter = chapter, onClose = viewModel::closeReader)
        return
    }
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
    var suppressWebtoonPositionUpdates by remember { mutableStateOf(false) }
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
        currentWebtoonStartIndex,
    ) {
        if (state.readerMode == ReaderMode.WEBTOON) {
            if (preserveWebtoonScrollOnChapterChange) {
                preserveWebtoonScrollOnChapterChange = false
                webtoonInitialScrollDoneFor = chapter.url
                suppressWebtoonPositionUpdates = false
            } else {
                val targetIndex = currentWebtoonStartIndex + state.currentPageIndex.coerceIn(0, lastPageIndex)
                val targetScrollOffset = state.currentPageScrollOffset.coerceAtLeast(0)
                suppressWebtoonPositionUpdates = true
                webtoonInitialScrollDoneFor = null
                webtoonListState.scrollToItem(targetIndex, 0)
                var restored = false
                var attempts = 0
                while (!restored && attempts < WEBTOON_RESTORE_MAX_ATTEMPTS) {
                    withFrameNanos { }
                    val targetItemInfo = webtoonListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == targetIndex }
                    when {
                        targetItemInfo == null -> webtoonListState.scrollToItem(targetIndex, 0)
                        targetItemInfo.size > 0 -> {
                            webtoonListState.scrollToItem(
                                index = targetIndex,
                                scrollOffset = targetScrollOffset.coerceAtMost((targetItemInfo.size - 1).coerceAtLeast(0)),
                            )
                            restored = true
                        }
                    }
                    attempts += 1
                    if (!restored) delay(WEBTOON_RESTORE_RETRY_DELAY_MILLIS)
                }
                if (!restored) {
                    webtoonListState.scrollToItem(targetIndex, 0)
                }
                withFrameNanos { }
                webtoonInitialScrollDoneFor = chapter.url
                suppressWebtoonPositionUpdates = false
            }
        }
    }

    LaunchedEffect(chapter.url, state.readerMode, webtoonPageItems) {
        if (state.readerMode == ReaderMode.WEBTOON) {
            snapshotFlow {
                webtoonPageItems.getOrNull(webtoonListState.firstVisibleItemIndex)
                    ?.let { it to webtoonListState.firstVisibleItemScrollOffset }
            }
                .distinctUntilChanged()
                .collect { visiblePage ->
                    val (item, scrollOffset) = visiblePage ?: return@collect
                    if (
                        !scrubberSeeking &&
                        !suppressWebtoonPositionUpdates &&
                        webtoonInitialScrollDoneFor == chapter.url
                    ) {
                        if (item.chapter.url != chapter.url) {
                            preserveWebtoonScrollOnChapterChange = true
                        }
                        viewModel.setWebtoonReaderPosition(item.chapter.url, item.pageIndex, scrollOffset)
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
                    ReaderPageImage(
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
                ReaderPageImage(
                    model = readerImageRequest(page),
                    contentDescription = chapter.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (state.readerPageGapLevel == 0) 8.dp else pageGap),
                    contentScale = if (state.readerFitWidth) ContentScale.FillWidth else ContentScale.Fit,
                    fillViewportWhileLoading = true,
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
                    shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
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
                    shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
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
                                TankobunChip(
                                    selected = state.readerMode == ReaderMode.PAGED,
                                    onClick = {
                                        resetZoom()
                                        viewModel.setReaderMode(ReaderMode.PAGED)
                                    },
                                    label = { Text("Paged") },
                                )
                                TankobunChip(
                                    selected = state.readerMode == ReaderMode.WEBTOON,
                                    onClick = {
                                        resetZoom()
                                        viewModel.setReaderMode(ReaderMode.WEBTOON)
                                    },
                                    label = { Text("Webtoon") },
                                )
                                TankobunChip(
                                    selected = state.readerFitWidth,
                                    enabled = state.readerMode == ReaderMode.PAGED,
                                    onClick = {
                                        viewModel.setReaderFitWidth(!state.readerFitWidth)
                                        resetZoom()
                                    },
                                    label = { Text("Fit width") },
                                )
                                TankobunChip(
                                    selected = state.readerPageGapLevel > 0,
                                    onClick = { viewModel.setReaderPageGapLevel((state.readerPageGapLevel + 1) % 4) },
                                    label = { Text(readerGapLabel(state.readerPageGapLevel)) },
                                )
                                TankobunChip(
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
internal fun ReaderLoadingScreen(chapter: SourceChapter, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(WindowInsets.safeDrawing.asPaddingValues()),
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close reader",
                tint = Color.White,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(color = Color.White.copy(alpha = 0.86f))
            Text(
                "Loading ${chapter.name}",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun ReaderPageImage(
    model: ImageRequest,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale,
    fillViewportWhileLoading: Boolean = false,
) {
    val loadingModifier = if (fillViewportWhileLoading) {
        Modifier.fillMaxSize()
    } else {
        Modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp)
    }
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            ReaderImagePlaceholder(
                modifier = loadingModifier,
                label = null,
            )
        },
        error = {
            ReaderImagePlaceholder(
                modifier = loadingModifier,
                label = "Page failed to load",
            )
        },
    )
}

@Composable
internal fun ReaderImagePlaceholder(modifier: Modifier, label: String?) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (label == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
                color = Color.White.copy(alpha = 0.86f),
            )
        } else {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
internal fun WebtoonNextChapterFooter(nextChapter: SourceChapter) {
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

internal fun readerPageGap(level: Int): Dp = when (level) {
    1 -> 8.dp
    2 -> 16.dp
    3 -> 24.dp
    else -> 0.dp
}

internal fun readerGapLabel(level: Int): String = when (level) {
    1 -> "Small gaps"
    2 -> "Medium gaps"
    3 -> "Large gaps"
    else -> "No gaps"
}

internal fun readerLerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

internal fun readerDoubleTapOffset(
    tapOffset: Offset,
    scale: Float,
    width: Float,
    height: Float,
): Offset {
    val center = Offset(width / 2f, height / 2f)
    return ((center - tapOffset) * (scale - 1f)).clampedReaderOffset(scale, width, height)
}

internal fun readerTransformOffset(
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

internal fun Offset.clampedReaderOffset(scale: Float, width: Float, height: Float): Offset {
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

internal fun Offset.readerPanForAxis(axis: ReaderPanAxis): Offset = when (axis) {
    ReaderPanAxis.BOTH -> this
    ReaderPanAxis.HORIZONTAL -> Offset(x, 0f)
    ReaderPanAxis.WEBTOON -> this
}

internal fun Offset.readerVelocityPosition(axis: ReaderPanAxis): Offset = when (axis) {
    ReaderPanAxis.BOTH -> this
    ReaderPanAxis.HORIZONTAL -> Offset(x, 0f)
    ReaderPanAxis.WEBTOON -> this
}

internal fun Velocity.readerVelocityForAxis(axis: ReaderPanAxis): Velocity = when (axis) {
    ReaderPanAxis.BOTH -> this
    ReaderPanAxis.HORIZONTAL -> Velocity(x, 0f)
    ReaderPanAxis.WEBTOON -> this
}

@Composable
internal fun readerImageRequest(page: ReaderPage): ImageRequest {
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
