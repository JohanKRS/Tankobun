package com.tankobun.app.ui.media

import com.tankobun.app.logic.isChapterRead
import com.tankobun.app.logic.readingOrder

import com.tankobun.app.ui.icons.TankobunIcons

import android.content.Context
import android.content.Intent
import android.app.Activity
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Stable
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import com.tankobun.app.TankobunDisplayFontFamily
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
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
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.sharpestMangaBakaCover


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
internal fun MangaDetailScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    media: AnilistMedia,
    onSelectMedia: (AnilistMedia) -> Unit,
    onBrowseTag: (String) -> Unit,
    onBrowseAuthor: (String) -> Unit,
    onOpenSourceRepository: () -> Unit,
    chrome: MediaDetailChromeState = remember(media.id) { MediaDetailChromeState() },
) {
    val backdrop = mediaDetailBackdropColor()
    val context = LocalContext.current
    var coverBackdropFailed by remember(media.id, media.coverImage) { mutableStateOf(false) }
    // The page is tinted with the cover's own colors; the banner is only a fallback.
    val heroBackdropImage = media.coverImage?.takeUnless { coverBackdropFailed } ?: media.bannerImage
    val heroBackdropRequest = remember(context, heroBackdropImage) {
        ImageRequest.Builder(context)
            .data(heroBackdropImage)
            .crossfade(450)
            .build()
    }
    val listState = rememberLazyListState()
    val orderedChapters = remember(state.readingChapters) { state.readingChapters.readingOrder() }
    // Chapters the update check found and that are still unread get the "new" dot.
    val newChapterUrls = remember(state.libraryUpdates, media.id) {
        state.libraryUpdates.firstOrNull { it.media.id == media.id }
            ?.chapters.orEmpty()
            .filterNot { it.read }
            .mapTo(hashSetOf()) { it.chapter.url }
    }
    val trackedStatuses = remember(state.libraryItems) { state.libraryItems.trackedMediaStatuses() }
    var coverZoomOpen by remember(media.id) { mutableStateOf(false) }
    var detailShareOpen by remember(media.id) { mutableStateOf(false) }
    var sourceSetupOpen by androidx.compose.runtime.saveable.rememberSaveable(media.id) { mutableStateOf(false) }
    val onSetupSources = {
        viewModel.closeSourcePicker()
        sourceSetupOpen = true
    }
    val detailBlur by animateDpAsState(
        targetValue = when {
            state.sourcePickerOpen || sourceSetupOpen -> 8.dp
            coverZoomOpen -> ChromeBackdropBlurDp.dp
            else -> 0.dp
        },
        animationSpec = tween(durationMillis = ChromeTransitionMillis),
        label = "Manga detail backdrop blur",
    )
    val coverZoomScrimAlpha by animateFloatAsState(
        targetValue = if (coverZoomOpen) CoverZoomScrimAlpha else 0f,
        animationSpec = tween(durationMillis = ChromeTransitionMillis),
        label = "Cover zoom scrim",
    )
    LaunchedEffect(media.id) {
        listState.scrollToItem(0)
    }
    val chapterRowActions = rememberChapterRowActions(viewModel)
    val actions = rememberMediaDetailUiActions(
        viewModel = viewModel,
        onOpenTracking = { chrome.trackingSheetOpen = true },
        onShareMedia = { detailShareOpen = true },
    )
    // The floating bar takes over once the hero actions slide under the top bar.
    var heroActionsBottom by remember(media.id) { mutableFloatStateOf(Float.MAX_VALUE) }
    val topChromePx = with(LocalDensity.current) { LocalTankobunChromeInsets.current.top.toPx() }
    LaunchedEffect(listState, chrome, topChromePx) {
        snapshotFlow { listState.firstVisibleItemIndex == 0 && heroActionsBottom > topChromePx }
            .distinctUntilChanged()
            .collect { chrome.heroActionsVisible = it }
    }
    DisposableEffect(chrome) {
        onDispose { chrome.heroActionsVisible = true }
    }
    LaunchedEffect(media.id, state.loggedIn, state.libraryMode, chrome.trackingSheetOpen) {
        viewModel.refreshTrackingEntry(media.id)
    }
    BackHandler(enabled = coverZoomOpen) {
        coverZoomOpen = false
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(backdrop),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .blur(detailBlur),
        ) {
            AsyncImage(
                model = heroBackdropRequest,
                onError = { coverBackdropFailed = true },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .blur(18.dp)
                    .graphicsLayer {
                        alpha = 0.32f
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Black,
                                0.70f to Color.Black,
                                1f to Color.Transparent,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to backdrop.copy(alpha = 0.22f),
                            0.52f to backdrop.copy(alpha = 0.82f),
                            1f to backdrop,
                        ),
                    ),
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = MediaDetailTopOverlayPadding,
                    bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() +
                        MediaDetailBottomDockClearance,
                ),
                // Chapters sit close together; sections add their own breathing room on top.
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    Column(Modifier.padding(horizontal = MediaDetailContentPadding)) {
                        Spacer(Modifier.height(4.dp))
                        MangaHeroSection(
                            state = state,
                            media = media,
                            onTagClick = onBrowseTag,
                            onAuthorClick = onBrowseAuthor,
                            onCoverClick = { coverZoomOpen = true },
                            actions = actions,
                            actionsModifier = Modifier.onGloballyPositioned { coordinates ->
                                heroActionsBottom = coordinates.positionInRoot().y + coordinates.size.height
                            },
                        )
                    }
                }

                if (state.selectedSourceAwaitingTrust != null) {
                    item(key = "source-review-required") {
                        Box(Modifier.padding(start = MediaDetailContentPadding, end = MediaDetailContentPadding, top = MediaDetailSectionGap)) {
                            SourceSummarySection(state, viewModel, onSetupSources)
                        }
                    }
                }

                state.message?.let { message ->
                    item(key = "message") {
                        Box(Modifier.padding(start = MediaDetailContentPadding, end = MediaDetailContentPadding, top = MediaDetailSectionGap)) {
                            Text(message, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                if (state.selectedSourceAwaitingTrust == null) {
                    item(key = "source-summary") {
                        Box(Modifier.padding(start = MediaDetailContentPadding, end = MediaDetailContentPadding, top = MediaDetailSectionGap)) {
                            SourceSummarySection(state, viewModel, onSetupSources)
                        }
                    }
                }

                item(key = "chapters-header") {
                    Box(Modifier.padding(start = MediaDetailContentPadding, end = MediaDetailContentPadding, top = MediaDetailSectionGap, bottom = 4.dp)) {
                        var downloadActionsOpen by remember { mutableStateOf(false) }
                        val chaptersReady = state.selectedSourceManga != null &&
                            !(state.selectedSourceAwaitingTrust != null && state.sourceChapters.isEmpty())
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Title, order and download share one row; refresh and groups live in the ⋮ menu.
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                DetailSectionTitle(tankobunString(R.string.common_chapters), Modifier.weight(1f))
                                if (chaptersReady) {
                                    val hasChapters = state.sourceChapters.isNotEmpty()
                                    IconButton(onClick = viewModel::toggleChapterListOrder, enabled = hasChapters) {
                                        Icon(
                                            TankobunIcons.Sort,
                                            contentDescription = tankobunString(
                                                if (state.chapterListStartsAtFirst) {
                                                    R.string.chapter_show_latest_first
                                                } else {
                                                    R.string.chapter_show_first_first
                                                },
                                            ),
                                        )
                                    }
                                    IconButton(onClick = { downloadActionsOpen = true }, enabled = hasChapters) {
                                        Icon(TankobunIcons.Download, contentDescription = tankobunString(R.string.chapter_download_chapters))
                                    }
                                }
                            }
                            if (state.selectedSourceManga == null) {
                                DetailPlaceholderCard(
                                    icon = TankobunIcons.MenuBook,
                                    title = tankobunString(R.string.detail_chapters_empty_title),
                                    subtitle = tankobunString(R.string.detail_chapters_empty_select_source),
                                )
                            } else if (state.selectedSourceAwaitingTrust != null && state.sourceChapters.isEmpty()) {
                                DetailPlaceholderCard(
                                    icon = TankobunIcons.Lock,
                                    title = tankobunString(R.string.sources_trust_status),
                                    subtitle = tankobunString(R.string.source_trust_chapters_pending),
                                )
                            } else if (state.selectingDownloadChapters) {
                                ChapterManualDownloadBar(
                                    selectedCount = state.selectedDownloadChapterUrls.size,
                                    onDownloadSelected = viewModel::downloadSelectedChapters,
                                    onCancel = viewModel::cancelManualDownloadSelection,
                                )
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
                }

                if (state.selectedSourceManga != null && state.sourceChapters.isEmpty() && state.selectedSourceAwaitingTrust == null) {
                    item {
                        Box(Modifier.padding(horizontal = MediaDetailContentPadding)) {
                            DetailPlaceholderCard(
                                icon = TankobunIcons.MenuBook,
                                title = tankobunString(R.string.detail_chapters_empty_title),
                                subtitle = tankobunString(R.string.detail_chapters_empty_load),
                            )
                        }
                    }
                } else {
                    val visibleChapters = if (state.chapterListStartsAtFirst) {
                        orderedChapters
                    } else {
                        orderedChapters.asReversed()
                    }
                    items(visibleChapters, key = { "${it.sourceId}:${it.url}" }) { chapter ->
                        ChapterRow(
                            chapter = chapter,
                            actions = chapterRowActions,
                            read = state.isChapterRead(chapter),
                            download = state.downloadForChapter(chapter),
                            selectingForDownload = state.selectingDownloadChapters,
                            selectedForDownload = chapter.url in state.selectedDownloadChapterUrls,
                            onToggleDownloadSelection = { viewModel.toggleDownloadChapterSelection(chapter) },
                            progress = state.chapterProgress[chapter.url],
                            isNew = chapter.url in newChapterUrls,
                        )
                    }
                }

                if (state.selectedRecommendations.isNotEmpty()) {
                    item(key = "recommendations") {
                        RecommendationsSection(
                            recommendations = state.selectedRecommendations,
                            hasMore = state.selectedRecommendationsHasMore,
                            loadingMore = state.recommendationsLoading,
                            onLoadMore = viewModel::loadMoreRecommendations,
                            onSelectMedia = onSelectMedia,
                            trackedStatuses = trackedStatuses,
                            modifier = Modifier.padding(top = MediaDetailSectionGap + 10.dp),
                        )
                    }
                }
            }
        }

        if (coverZoomScrimAlpha > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = coverZoomScrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { coverZoomOpen = false },
                    ),
            )
        }

        AnimatedVisibility(
            visible = coverZoomOpen,
            enter = fadeIn(animationSpec = tween(durationMillis = ChromeTransitionMillis)) +
                scaleIn(initialScale = 0.90f, animationSpec = tween(durationMillis = ChromeTransitionMillis)),
            exit = fadeOut(animationSpec = tween(durationMillis = ChromeTransitionMillis)) +
                scaleOut(targetScale = 0.92f, animationSpec = tween(durationMillis = ChromeTransitionMillis)),
        ) {
            CoverZoomOverlay(media = media, onDismiss = { coverZoomOpen = false })
        }

        if (state.sourcePickerOpen) {
            SourcePickerDialog(state, viewModel, media, onSetupSources)
        }

        if (sourceSetupOpen) {
            SourceRepositoryHelpDialog(
                onDismiss = { sourceSetupOpen = false },
                onOpenRepository = {
                    sourceSetupOpen = false
                    onOpenSourceRepository()
                },
            )
        }

        if (chrome.chapterGroupsOpen) {
            ChapterGroupsDialog(
                groups = state.chapterGroupSelection.groups,
                preference = state.chapterGroupPreference,
                onChange = viewModel::setChapterGroupPreference,
                onDismiss = { chrome.chapterGroupsOpen = false },
            )
        }

        if (chrome.trackingSheetOpen) {
            MediaTrackingSheet(
                state = state,
                viewModel = viewModel,
                media = media,
                onDismiss = { chrome.trackingSheetOpen = false },
            )
        }

        if (detailShareOpen) {
            LibraryShareDialog(
                selectedItems = listOf(state.detailShareItem(media)),
                onShare = { name, messagesByMediaId ->
                    detailShareOpen = false
                    viewModel.shareMediaRecommendation(context, media, name, messagesByMediaId)
                },
                onDismiss = { detailShareOpen = false },
            )
        }
    }
}

private fun TankobunUiState.detailShareItem(media: AnilistMedia): LibraryItem {
    val entry = selectedListEntry
        ?.takeIf { it.mediaId == media.id }
        ?: AnilistListEntry(
            id = -kotlin.math.abs(media.id),
            mediaId = media.id,
            status = trackingStatus,
            progress = trackingProgress.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            score = trackingScore.toAniListScore(anilistScoreFormat),
            notes = trackingNotes.trim().ifBlank { null },
            private = trackingPrivate,
            customLists = trackingCustomLists.normalizedCustomLists(),
            updatedAtEpochSeconds = 0L,
            hiddenFromStatusLists = false,
        )
    return LibraryItem(media, entry)
}

@Composable
internal fun mediaDetailBackdropColor(): Color = LocalTankobunTokens.current.appBackdrop

@Composable
internal fun mediaDetailPanelColor(): Color = LocalTankobunTokens.current.elevatedSurface.copy(alpha = 0.88f)

@Composable
internal fun mediaDetailForegroundColor(): Color = MaterialTheme.colorScheme.onBackground

@Composable
internal fun mediaDetailAccentColor(): Color = MaterialTheme.colorScheme.primary

@Composable
internal fun mediaDetailActionColor(): Color = MaterialTheme.colorScheme.secondary

private val MediaDetailContentPadding = 16.dp
private val MediaDetailSectionGap = 12.dp
private val MediaDetailTopOverlayPadding = 92.dp
private val MediaDetailBottomDockClearance = 112.dp
private const val CoverZoomScrimAlpha = 0.34f

@Composable
internal fun DetailSectionTitle(text: String, modifier: Modifier = Modifier) {
    TankobunSectionHeader(text, modifier = modifier)
}

@Composable
internal fun DetailPlaceholderCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = LocalTankobunStyle.current.themeShapes.panel,
        color = mediaDetailPanelColor(),
        contentColor = mediaDetailForegroundColor(),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailIconBadge(icon = icon)
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
internal fun DetailIconBadge(icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(LocalTankobunStyle.current.sizes.iconAction),
        shape = RoundedCornerShape(999.dp),
        color = mediaDetailAccentColor().copy(alpha = 0.16f),
        contentColor = mediaDetailAccentColor(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
internal fun MangaHeroSection(
    state: TankobunUiState,
    media: AnilistMedia,
    onTagClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onCoverClick: () -> Unit,
    actions: MediaDetailUiActions,
    actionsModifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 620.dp
        val coverWidth = if (compact) {
            (maxWidth * 0.40f).coerceIn(122.dp, 168.dp)
        } else {
            (maxWidth * 0.34f).coerceIn(220.dp, 300.dp)
        }
        val coverHeight = coverWidth * 1.5f
        val titleHeight = if (compact) {
            (coverHeight * 0.62f).coerceIn(118.dp, 158.dp)
        } else {
            (coverHeight * 0.62f).coerceIn(180.dp, 250.dp)
        }

        Column(
            modifier = Modifier.padding(top = if (compact) 0.dp else 32.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 28.dp),
                verticalAlignment = Alignment.Top,
            ) {
                MangaCoverFrame(
                    media = media,
                    onClick = onCoverClick,
                    modifier = Modifier.size(width = coverWidth, height = coverHeight),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(coverHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoResizingMangaTitle(
                                title = media.title.userPreferred,
                                compact = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(titleHeight),
                            )
                            MangaHeroMetaLine(media = media, compact = true)
                        }
                    } else {
                        AutoResizingMangaTitle(
                            title = media.title.userPreferred,
                            compact = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(titleHeight),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 10.dp)) {
                        if (!compact) {
                            MangaHeroMetaLine(media = media, compact = false)
                        }
                        MangaStatRow(media = media, compact = compact)
                    }
                }
            }
            MangaInfoRow(media = media, compact = compact, onAuthorClick = onAuthorClick)
            MediaDetailQuickActions(
                state = state,
                compact = compact,
                actions = actions,
                modifier = actionsModifier,
            )
            MangaDescriptionAndTags(media = media, compact = compact, onTagClick = onTagClick)
        }
    }
}

/** Detail chrome shared with the shell: the floating action bar replaces the dock once hero actions scroll away. */
@Stable
internal class MediaDetailChromeState {
    var heroActionsVisible by mutableStateOf(true)
    var trackingSheetOpen by mutableStateOf(false)
    var chapterGroupsOpen by mutableStateOf(false)
}

internal sealed interface MediaReadingAction {
    data class Read(val chapter: SourceChapter, val resume: Boolean, val page: Int? = null) : MediaReadingAction
    data object ChooseSource : MediaReadingAction
    data object LoadChapters : MediaReadingAction
}

internal fun TankobunUiState.mediaReadingAction(): MediaReadingAction? = when {
    selectedSourceManga == null -> MediaReadingAction.ChooseSource
    selectedSourceAwaitingTrust != null -> null
    else -> primaryReadingActionChapter()
        ?.let { chapter ->
            MediaReadingAction.Read(
                chapter = chapter,
                resume = latestProgress != null,
                // Resuming mid-chapter also says where: "Continuar · Cap. 4  pág. 12".
                page = latestProgress
                    ?.takeIf { it.chapterUrl == chapter.url && !it.completed && it.pageIndex > 0 }
                    ?.let { it.pageIndex + 1 },
            )
        }
        ?: MediaReadingAction.LoadChapters
}

/** The number a reader recognizes ("12", "12.5"); unnumbered and broken entries have none. */
internal fun SourceChapter.shortNumberLabel(): String? {
    chapterNumberText?.trim()?.takeIf { it.isNotEmpty() && it.length <= 8 }?.let { return it }
    if (!chapterNumber.isFinite() || chapterNumber < 0f) return null
    return chapterNumber.toBigDecimal().stripTrailingZeros().toPlainString()
}

private fun MediaDetailUiActions.perform(action: MediaReadingAction) {
    when (action) {
        is MediaReadingAction.Read -> onOpenChapter(action.chapter)
        MediaReadingAction.ChooseSource -> onChooseSource()
        MediaReadingAction.LoadChapters -> onLoadChapters()
    }
}

internal class MediaDetailUiActions(
    val onOpenChapter: (SourceChapter) -> Unit,
    val onChooseSource: () -> Unit,
    val onLoadChapters: () -> Unit,
    val onAddToLibrary: () -> Unit,
    val onOpenTracking: () -> Unit,
    val onShareMedia: () -> Unit,
)

@Composable
internal fun rememberMediaDetailUiActions(
    viewModel: MainViewModel,
    onOpenTracking: () -> Unit,
    onShareMedia: () -> Unit,
): MediaDetailUiActions {
    val currentOpenTracking by rememberUpdatedState(onOpenTracking)
    val currentShareMedia by rememberUpdatedState(onShareMedia)
    return remember(viewModel) {
        MediaDetailUiActions(
            onOpenChapter = viewModel::openChapter,
            onChooseSource = viewModel::openSourcePicker,
            onLoadChapters = viewModel::loadChaptersForCurrentMatch,
            onAddToLibrary = viewModel::saveTracking,
            onOpenTracking = { currentOpenTracking() },
            onShareMedia = { currentShareMedia() },
        )
    }
}

@Composable
private fun MediaReadingAction.label(): String = when (this) {
    is MediaReadingAction.Read -> {
        val number = chapter.shortNumberLabel()
        when {
            number == null && resume -> tankobunString(R.string.chapter_resume_reading)
            number == null -> tankobunString(R.string.chapter_start_reading)
            resume -> tankobunString(R.string.detail_continue_chapter, number)
            else -> tankobunString(R.string.detail_start_chapter, number)
        }
    }
    MediaReadingAction.ChooseSource -> tankobunString(R.string.source_find_source)
    MediaReadingAction.LoadChapters -> tankobunString(R.string.chapter_load_chapters)
}

private fun MediaReadingAction.icon(): ImageVector = when (this) {
    is MediaReadingAction.Read -> TankobunIcons.PlayArrow
    MediaReadingAction.ChooseSource -> TankobunIcons.Search
    MediaReadingAction.LoadChapters -> TankobunIcons.Refresh
}

@Composable
private fun MediaReadingActionButton(
    action: MediaReadingAction,
    actions: MediaDetailUiActions,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    // The floating bar has no room for the page; the chapter number matters more there.
    showPage: Boolean = true,
) {
    TankobunActionButton(
        label = action.label(),
        icon = action.icon(),
        onClick = { actions.perform(action) },
        modifier = modifier,
        shape = shape,
        supportingLabel = (action as? MediaReadingAction.Read)?.page
            ?.takeIf { showPage }
            ?.let { tankobunString(R.string.detail_continue_page, it) },
    )
}

/** What the tracking control says and does for the media on screen. */
private data class TrackingControlState(
    val inLibrary: Boolean,
    val canAdd: Boolean,
    val saving: Boolean,
    val failed: Boolean,
    val status: MediaStatus?,
    val progress: Int? = null,
    val totalChapters: Int? = null,
    val score: String? = null,
)

private fun TankobunUiState.trackingControlState(): TrackingControlState {
    val fixedStatusSelected = selectedListEntry?.hiddenFromStatusLists != true ||
        trackingStatus != MediaStatus.UNKNOWN
    return TrackingControlState(
        inLibrary = selectedListEntry != null,
        canAdd = (libraryMode == LibraryMode.LOCAL || loggedIn) && !trackingSaveInProgress && fixedStatusSelected,
        saving = trackingSaveInProgress,
        failed = trackingSaveFailed,
        status = if (fixedStatusSelected && selectedListEntry != null) trackingStatus else null,
        progress = selectedListEntry?.progress?.takeIf { it > 0 },
        totalChapters = selectedMedia?.chapters?.takeIf { it > 0 },
        score = selectedListEntry?.score?.takeIf { it > 0.0 }?.formatTrackingScore(anilistScoreFormat),
    )
}

private fun TrackingControlState.onClick(actions: MediaDetailUiActions) {
    // One tap still adds a manga; editing status, progress or score happens in the sheet.
    if (!inLibrary && canAdd) actions.onAddToLibrary() else actions.onOpenTracking()
}

@Composable
private fun TrackingControlState.label(): String = when {
    saving -> tankobunString(R.string.detail_saving)
    failed -> tankobunString(R.string.common_retry_save)
    !inLibrary -> tankobunString(R.string.detail_add_to_library)
    status == null -> tankobunString(R.string.detail_choose_status)
    else -> status.displayName()
}

/** "5/214", or just "5" while the total is unknown. */
private fun TrackingControlState.progressLabel(): String? =
    progress?.let { read -> totalChapters?.let { "$read/$it" } ?: read.toString() }

/** The hero button's full line: "Lendo · 5/214 · ★ 80". */
@Composable
private fun TrackingControlState.summary(): String {
    if (saving || failed || !inLibrary || status == null) return label()
    return listOfNotNull(label(), progressLabel(), score?.let { "★ $it" }).joinToString(" · ")
}

private fun TrackingControlState.icon(): ImageVector = when {
    saving || failed -> TankobunIcons.Refresh
    !inLibrary -> TankobunIcons.Add
    status == null -> TankobunIcons.LibraryBooks
    else -> trackingStatusIcon(status)
}

@Composable
private fun MediaTrackingButton(
    control: TrackingControlState,
    actions: MediaDetailUiActions,
    modifier: Modifier = Modifier,
) {
    val style = LocalTankobunStyle.current
    val label = control.summary()
    val tracked = control.inLibrary && control.status != null
    Surface(
        onClick = { control.onClick(actions) },
        enabled = !control.saving,
        modifier = modifier
            .heightIn(min = style.sizes.iconAction)
            .semantics { role = Role.Button },
        shape = style.themeShapes.control,
        color = if (tracked) style.colors.selectedChip else Color.Transparent,
        contentColor = if (tracked) style.colors.selectedChipContent else mediaDetailForegroundColor(),
        border = if (tracked) null else BorderStroke(1.dp, style.colors.outline.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (control.saving) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(control.icon(), contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (control.inLibrary) {
                Icon(TankobunIcons.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
internal fun MediaDetailQuickActions(
    state: TankobunUiState,
    compact: Boolean,
    actions: MediaDetailUiActions,
    modifier: Modifier = Modifier,
) {
    val readingAction = state.mediaReadingAction()
    val control = state.trackingControlState()
    val shareButton: @Composable () -> Unit = {
        TankobunIconActionButton(
            icon = TankobunIcons.Share,
            contentDescription = tankobunString(R.string.detail_share_manga),
            onClick = actions.onShareMedia,
            enabled = !state.busy,
        )
    }
    if (compact) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (readingAction != null) {
                MediaReadingActionButton(readingAction, actions, Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaTrackingButton(control, actions, Modifier.weight(1f))
                shareButton()
            }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (readingAction != null) {
                MediaReadingActionButton(readingAction, actions, Modifier.weight(1.2f))
            }
            MediaTrackingButton(control, actions, Modifier.weight(1f))
            shareButton()
        }
    }
}

/** Reading and tracking stay within thumb reach after the hero scrolls away. */
@Composable
internal fun MediaDetailFloatingActions(
    state: TankobunUiState,
    actions: MediaDetailUiActions,
    modifier: Modifier = Modifier,
) {
    val readingAction = state.mediaReadingAction()
    val control = state.trackingControlState()
    val style = LocalTankobunStyle.current
    Row(
        // Same inset and item height as the dock it replaces, so the pills sit concentric in the glass.
        modifier = modifier
            .height(GlassDockHeight)
            .padding(horizontal = DockPadding),
        horizontalArrangement = Arrangement.spacedBy(DockPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val trackingLabel = control.summary()
        val tracked = control.inLibrary && control.status != null
        // Tracked: a pill that edits it ("✎ 5/214 ⌃"); otherwise a one-tap add button.
        val pillText = control.progressLabel() ?: control.label()
        Surface(
            onClick = { control.onClick(actions) },
            enabled = !control.saving,
            modifier = Modifier
                .height(DockItemHeight)
                .then(if (tracked) Modifier.widthIn(min = DockItemHeight, max = 148.dp) else Modifier.width(DockItemHeight))
                .semantics {
                    role = Role.Button
                    contentDescription = trackingLabel
                },
            shape = CircleShape,
            color = if (tracked) style.colors.selectedChip else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (tracked) style.colors.selectedChipContent else MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (tracked) 14.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    control.saving -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    tracked -> {
                        Icon(TankobunIcons.Pencil, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            pillText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(
                            TankobunIcons.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = 180f },
                        )
                    }
                    else -> Icon(control.icon(), contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (readingAction != null) {
            MediaReadingActionButton(
                action = readingAction,
                actions = actions,
                modifier = Modifier.height(DockItemHeight).widthIn(min = 168.dp, max = 260.dp),
                shape = CircleShape,
                showPage = false,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MediaTrackingSheet(
    state: TankobunUiState,
    viewModel: MainViewModel,
    media: AnilistMedia,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var confirmRemove by remember(media.id) { mutableStateOf(false) }
    fun hideThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) action()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    tankobunString(R.string.detail_track_manga),
                    style = TextStyle(fontFamily = TankobunDisplayFontFamily, fontSize = 30.sp, lineHeight = 30.sp, letterSpacing = 1.sp),
                )
                Text(
                    tankobunString(
                        if (state.libraryMode == LibraryMode.ANILIST) R.string.tracking_syncs_anilist else R.string.tracking_saved_locally,
                        media.title.userPreferred,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AniListTrackingSection(
                state = state,
                viewModel = viewModel,
                media = media,
                onRemove = { confirmRemove = true },
            )
        }
    }
    if (confirmRemove) {
        AlertDialog(
            shape = LocalTankobunStyle.current.themeShapes.dialog,
            containerColor = LocalTankobunStyle.current.colors.panel,
            titleContentColor = LocalTankobunStyle.current.colors.panelContent,
            textContentColor = LocalTankobunStyle.current.colors.mutedContent,
            tonalElevation = 0.dp,
            onDismissRequest = { confirmRemove = false },
            title = { Text(tankobunString(R.string.detail_remove_from_library_confirm)) },
            text = { Text(tankobunString(R.string.detail_remove_from_library_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemove = false
                        hideThen {
                            onDismiss()
                            viewModel.removeSelectedMediaFromLibrary()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(tankobunString(R.string.common_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) {
                    Text(tankobunString(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
internal fun MangaCoverFrame(media: AnilistMedia, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val coverModifier = if (onClick != null) {
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else {
        modifier
    }
    Surface(
        modifier = coverModifier,
        shape = LocalTankobunStyle.current.themeShapes.panel,
        color = Color.Transparent,
        shadowElevation = 8.dp,
    ) {
        CoverImage(
            url = media.coverImage.sharpestMangaBakaCover(),
            title = media.title.userPreferred,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            imageAlignment = Alignment.TopCenter,
            cornerRadius = 10.dp,
        )
    }
}

@Composable
internal fun AutoResizingMangaTitle(
    title: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
    color: Color? = null,
    shadow: Shadow? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val maxFontSize = if (compact) 82f else 104f
        val minFontSize = if (compact) 16f else 22f
        val maxLines = if (compact) 5 else 6
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val verticalClipGuardPx = with(density) { if (compact) 0.dp.toPx() else 7.dp.toPx() }
        val layout = remember(title, compact, maxWidth, maxHeight, density.fontScale) {
            buildMangaTitleLayout(
                title = title,
                maxWidthPx = maxWidthPx,
                maxHeightPx = maxHeightPx,
                verticalClipGuardPx = verticalClipGuardPx,
                maxLines = maxLines,
                maxFontSize = maxFontSize,
                minFontSize = minFontSize,
                textMeasurer = textMeasurer,
            )
        }
        val style = tankobunMangaTitleTextStyle(layout.fontSize).copy(shadow = shadow)
        Text(
            layout.lines.joinToString("\n"),
            modifier = Modifier.fillMaxSize(),
            style = style,
            color = color ?: mediaDetailForegroundColor(),
            maxLines = layout.lines.size,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

private data class MangaTitleLayout(
    val fontSize: Float,
    val lines: List<String>,
)

@Composable
private fun bebasNeueStatTextStyle(compact: Boolean): TextStyle =
    (if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall).copy(
        fontFamily = TankobunDisplayFontFamily,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = if (compact) 32.sp else 44.sp,
    )

@Composable
private fun bebasNeueRecommendationMetricStyle(): TextStyle =
    MaterialTheme.typography.labelLarge.copy(
        fontFamily = TankobunDisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    )

private fun buildMangaTitleLayout(
    title: String,
    maxWidthPx: Float,
    maxHeightPx: Float,
    verticalClipGuardPx: Float,
    maxLines: Int,
    maxFontSize: Float,
    minFontSize: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): MangaTitleLayout {
    val words = title
        .uppercase(Locale.ROOT)
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .ifEmpty { listOf(title.uppercase(Locale.ROOT)) }
    var fontSize = maxFontSize
    while (fontSize >= minFontSize) {
        val style = tankobunMangaTitleTextStyle(fontSize)
        val lines = preferredTitleLinesForWidth(words, maxWidthPx, maxLines, style, textMeasurer)
        if (
            lines != null &&
            measuredTitleHeight(lines, style, textMeasurer) + verticalClipGuardPx <= maxHeightPx
        ) {
            return MangaTitleLayout(fontSize, lines)
        }
        fontSize -= 1f
    }
    val fallbackStyle = tankobunMangaTitleTextStyle(minFontSize)
    return MangaTitleLayout(
        minFontSize,
        truncatedTitleLinesForWidth(words, maxWidthPx, maxLines, fallbackStyle, textMeasurer),
    )
}

private fun preferredTitleLinesForWidth(
    words: List<String>,
    maxWidthPx: Float,
    maxLines: Int,
    style: TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): List<String>? {
    if (words.size == 2 && maxLines >= 2) {
        val stacked = words.takeIf { candidate ->
            candidate.all { measuredTitleWidth(it, style, textMeasurer) <= maxWidthPx }
        }
        if (stacked != null) return stacked
    }
    return titleLinesForWidth(words, maxWidthPx, maxLines, style, textMeasurer)
}

private fun titleLinesForWidth(
    words: List<String>,
    maxWidthPx: Float,
    maxLines: Int,
    style: TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): List<String>? {
    val lines = mutableListOf<String>()
    var currentLine = ""
    words.forEach { word ->
        val candidate = if (currentLine.isBlank()) word else "$currentLine $word"
        if (measuredTitleWidth(candidate, style, textMeasurer) <= maxWidthPx) {
            currentLine = candidate
        } else {
            if (currentLine.isBlank() || measuredTitleWidth(word, style, textMeasurer) > maxWidthPx) {
                return null
            }
            lines += currentLine
            if (lines.size == maxLines) return null
            currentLine = word
        }
    }
    if (currentLine.isNotBlank()) lines += currentLine
    return lines.takeIf { it.size <= maxLines }
}

private fun truncatedTitleLinesForWidth(
    words: List<String>,
    maxWidthPx: Float,
    maxLines: Int,
    style: TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): List<String> {
    if (words.isEmpty() || maxLines <= 0) return listOf("")
    val lines = mutableListOf<String>()
    var wordIndex = 0
    while (wordIndex < words.size && lines.size < maxLines) {
        val lastAllowedLine = lines.size == maxLines - 1
        var currentLine = ""
        var nextWordIndex = wordIndex
        while (nextWordIndex < words.size) {
            val candidate = if (currentLine.isBlank()) {
                words[nextWordIndex]
            } else {
                "$currentLine ${words[nextWordIndex]}"
            }
            val hasRemainingWords = nextWordIndex < words.lastIndex
            val measuredCandidate = if (lastAllowedLine && hasRemainingWords) {
                candidate.withTitleEllipsis()
            } else {
                candidate
            }
            if (measuredTitleWidth(measuredCandidate, style, textMeasurer) > maxWidthPx) {
                break
            }
            currentLine = candidate
            nextWordIndex += 1
        }
        if (currentLine.isBlank()) {
            lines += words[wordIndex].fitTitleLineWithEllipsis(maxWidthPx, style, textMeasurer)
            wordIndex += 1
        } else {
            wordIndex = nextWordIndex
            val hasMoreWords = wordIndex < words.size
            lines += if (lastAllowedLine && hasMoreWords) {
                currentLine.fitTitleLineWithEllipsis(maxWidthPx, style, textMeasurer)
            } else {
                currentLine
            }
        }
        if (lastAllowedLine) return lines
    }
    return lines.ifEmpty { listOf("") }
}

private fun String.withTitleEllipsis(): String = "$this..."

private fun String.fitTitleLineWithEllipsis(
    maxWidthPx: Float,
    style: TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return ""
    var candidate = trimmed.withTitleEllipsis()
    if (measuredTitleWidth(candidate, style, textMeasurer) <= maxWidthPx) return candidate
    var endIndex = trimmed.length
    while (endIndex > 0) {
        candidate = trimmed.take(endIndex).trimEnd().withTitleEllipsis()
        if (measuredTitleWidth(candidate, style, textMeasurer) <= maxWidthPx) return candidate
        endIndex -= 1
    }
    return if (measuredTitleWidth("...", style, textMeasurer) <= maxWidthPx) "..." else ""
}

private fun measuredTitleWidth(
    text: String,
    style: TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): Int =
    textMeasurer.measure(
        text = AnnotatedString(text),
        style = style,
        softWrap = false,
        maxLines = 1,
    ).size.width

private fun measuredTitleHeight(
    lines: List<String>,
    style: TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): Int =
    textMeasurer.measure(
        text = AnnotatedString(lines.joinToString("\n")),
        style = style,
        softWrap = false,
        maxLines = lines.size,
    ).size.height

@Composable
internal fun MangaHeroMetaLine(media: AnilistMedia, compact: Boolean) {
    val style = if (compact) {
        MaterialTheme.typography.labelMedium
    } else {
        MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, lineHeight = 16.sp)
    }
    Text(
        listOfNotNull(
            media.mediaTypeLabel(),
            media.status.statusLabel(),
        ).joinToString(" · ").uppercase(Locale.ROOT),
        style = style,
        color = mediaDetailAccentColor(),
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = style.shrinkToFit(),
    )
}

@Composable
internal fun MangaStatRow(media: AnilistMedia, compact: Boolean) {
    val stats = listOf(
        (if (compact) tankobunString(R.string.detail_chapter_stat_short) else tankobunString(R.string.detail_chapter_stat)) to (media.chapters?.toString() ?: "--"),
        (if (compact) tankobunString(R.string.detail_volume_stat_short) else tankobunString(R.string.detail_volume_stat)) to (media.volumes?.toString() ?: "--"),
        (if (compact) tankobunString(R.string.detail_score_stat_short) else tankobunString(R.string.common_score)) to (media.averageScore?.let { "$it%" } ?: "--"),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stats.forEachIndexed { index, (label, value) ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    value,
                    style = bebasNeueStatTextStyle(compact),
                    color = mediaDetailForegroundColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 12.sp)
                Text(
                    label.uppercase(Locale.ROOT),
                    style = labelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = labelStyle.shrinkToFit(),
                )
            }
            if (index < stats.lastIndex) {
                Box(
                    Modifier
                        .padding(horizontal = if (compact) 8.dp else 14.dp)
                        .width(1.dp)
                        .height(if (compact) 42.dp else 50.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f)),
                )
            }
        }
    }
}

private data class MangaInfoItem(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val onClick: (() -> Unit)? = null,
)

@Composable
internal fun MangaInfoRow(media: AnilistMedia, compact: Boolean, onAuthorClick: (String) -> Unit) {
    val authorName = media.staff.firstOrNull()
    val infoItems = listOfNotNull(
        MangaInfoItem(
            icon = TankobunIcons.Pencil,
            label = tankobunString(R.string.detail_author),
            value = media.staff.authorLabel(),
            onClick = authorName?.let { { onAuthorClick(it) } },
        ),
        MangaInfoItem(
            TankobunIcons.CalendarMonth,
            if (compact) tankobunString(R.string.detail_years) else tankobunString(R.string.detail_published),
            media.publishingYearLabel(compact),
        ),
        media.popularity?.let { MangaInfoItem(TankobunIcons.Groups, tankobunString(R.string.detail_readers), it.formatCompact()) },
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
    ) {
        infoItems.forEachIndexed { index, item ->
            val weight = if (compact) {
                when (index) {
                    0 -> 1.24f
                    1 -> 1.08f
                    else -> 0.92f
                }
            } else {
                1f
            }
            MangaInfoChip(item = item, compact = compact, modifier = Modifier.weight(weight))
        }
    }
}

@Composable
private fun MangaInfoChip(item: MangaInfoItem, compact: Boolean, modifier: Modifier = Modifier) {
    val onClick = item.onClick
    val chipModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    Surface(
        modifier = chipModifier.heightIn(min = if (compact) 48.dp else 56.dp),
        shape = LocalTankobunStyle.current.themeShapes.panel,
        color = mediaDetailPanelColor(),
        contentColor = mediaDetailForegroundColor(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 8.dp else 11.dp, vertical = if (compact) 8.dp else 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp),
        ) {
            DetailIconBadge(icon = item.icon, modifier = Modifier.size(if (compact) 26.dp else 30.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 10.sp else 11.sp, lineHeight = 12.sp)
                Text(
                    item.label,
                    style = labelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = labelStyle.shrinkToFit(),
                )
                Text(
                    item.value,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = if (compact) 13.sp else 14.sp, lineHeight = 15.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = mediaDetailForegroundColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun MangaDescriptionAndTags(
    media: AnilistMedia,
    compact: Boolean,
    onTagClick: (String) -> Unit,
) {
    val description = media.description.plainMediaDescription()
    val tags = media.tags.ifEmpty { media.genres }
    var descriptionExpanded by remember(media.id) { mutableStateOf(false) }
    var descriptionOverflow by remember(media.id, description) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (description.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    description,
                    maxLines = if (descriptionExpanded) Int.MAX_VALUE else if (compact) 4 else 5,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = if (compact) 15.sp else 16.sp,
                        lineHeight = if (compact) 21.sp else 22.sp,
                    ),
                    color = mediaDetailForegroundColor().copy(alpha = 0.92f),
                    onTextLayout = { descriptionOverflow = it.hasVisualOverflow },
                )
                if (descriptionOverflow || descriptionExpanded) {
                    Text(
                        if (descriptionExpanded) tankobunString(R.string.detail_show_less) else tankobunString(R.string.detail_read_more),
                        modifier = Modifier.clickable { descriptionExpanded = !descriptionExpanded },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = if (compact) 13.sp else 14.sp,
                            lineHeight = if (compact) 16.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = mediaDetailAccentColor(),
                    )
                }
            }
        }
        if (tags.isNotEmpty()) {
            JustifiedTagFlow {
                tags.take(if (compact) 7 else 12).forEach { tag ->
                    mangaTagPill(tag = tag, compact = compact, onClick = { onTagClick(tag) })
                }
            }
        }
    }
}

@Composable
internal fun mangaTagPill(tag: String, compact: Boolean, onClick: () -> Unit) {
    TankobunTag(label = tag, compact = compact, onClick = onClick)
}

@Composable
internal fun AnilistMedia.publishingYearLabel(compact: Boolean): String {
    val startYear = startDateYear
    val endYear = endDateYear
    return when {
        !compact -> publishingYearLabel()
        startYear != null && endYear != null && startYear != endYear -> {
            val compactEnd = if (startYear / 100 == endYear / 100) {
                (endYear % 100).toString().padStart(2, '0')
            } else {
                endYear.toString()
            }
            "$startYear-$compactEnd"
        }
        startYear != null && status == "RELEASING" -> "$startYear-"
        startYear != null -> startYear.toString()
        else -> tankobunString(R.string.common_unknown)
    }
}

@Composable
internal fun AniListTrackingSection(
    state: TankobunUiState,
    viewModel: MainViewModel,
    media: AnilistMedia,
    onRemove: () -> Unit,
) {
    val fixedStatusSelected = state.selectedListEntry?.hiddenFromStatusLists != true ||
        state.trackingStatus != MediaStatus.UNKNOWN
    val selectedFixedStatus = if (fixedStatusSelected) state.trackingStatus else null
    val anilistMode = state.libraryMode == LibraryMode.ANILIST
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        TrackingStatusChips(selected = selectedFixedStatus, onSelected = viewModel::setTrackingStatus)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            TrackingProgressCard(
                value = state.trackingProgress,
                total = media.chapters,
                onValueChange = viewModel::setTrackingProgress,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            TrackingScoreCard(
                scoreFormat = state.anilistScoreFormat,
                value = state.trackingScore,
                onValueChange = viewModel::setTrackingScore,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        TrackingCustomLists(
            availableLists = (state.anilistCustomLists + state.trackingCustomLists).distinctBy { it.lowercase() },
            selectedLists = state.trackingCustomLists,
            onListSelected = viewModel::setTrackingCustomListSelected,
            onAddList = viewModel::addTrackingCustomList,
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TrackingFieldLabel(tankobunString(R.string.common_notes))
            OutlinedTextField(
                value = state.trackingNotes,
                onValueChange = viewModel::setTrackingNotes,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                // AniList can show notes elsewhere; only the local library can promise they stay private.
                placeholder = if (anilistMode) null else ({ Text(tankobunString(R.string.tracking_notes_local_hint)) }),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(tankobunString(R.string.common_private), style = MaterialTheme.typography.bodyLarge)
                if (anilistMode) {
                    Text(
                        tankobunString(R.string.tracking_private_anilist_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = state.trackingPrivate, onCheckedChange = viewModel::setTrackingPrivate)
        }

        if (anilistMode && !state.loggedIn) {
            Text(
                tankobunString(R.string.detail_connect_anilist_tracking),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.selectedListEntry != null) {
                TextButton(
                    onClick = onRemove,
                    enabled = !state.busy && !state.trackingSaveInProgress,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(
                        tankobunString(R.string.detail_remove_from_library),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            val canSaveTracking = (state.libraryMode == LibraryMode.LOCAL || state.loggedIn) &&
                !state.trackingSaveInProgress &&
                fixedStatusSelected &&
                (state.selectedListEntry == null || state.trackingDirty || state.trackingSaveFailed)
            val actionLabel = when {
                !fixedStatusSelected -> tankobunString(R.string.detail_choose_status)
                state.trackingSaveInProgress -> tankobunString(R.string.detail_saving)
                state.trackingSaveFailed -> tankobunString(R.string.common_retry_save)
                state.selectedListEntry == null -> tankobunString(R.string.detail_track_manga)
                state.trackingDirty -> tankobunString(R.string.detail_save_tracking)
                else -> tankobunString(R.string.common_saved)
            }
            TankobunActionButton(
                label = actionLabel,
                icon = if (state.trackingSaveInProgress || state.trackingSaveFailed) TankobunIcons.Refresh else null,
                onClick = viewModel::saveTracking,
                enabled = canSaveTracking,
                shape = CircleShape,
                modifier = Modifier.heightIn(min = 48.dp),
                disabledContainerColor = if (state.trackingSaveInProgress) {
                    LocalTankobunStyle.current.colors.selectedChip
                } else {
                    null
                },
                disabledContentColor = if (state.trackingSaveInProgress) {
                    LocalTankobunStyle.current.colors.selectedChipContent
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun TrackingFieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackingStatusChips(selected: MediaStatus?, onSelected: (MediaStatus) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        trackingStatuses().forEach { status ->
            val active = status == selected
            TankobunChip(
                selected = active,
                onClick = { onSelected(status) },
                label = { Text(status.displayName(), maxLines = 1) },
                leadingIcon = if (active) ({ TankobunChipIcon(trackingStatusIcon(status)) }) else null,
            )
        }
    }
}

/** Chapters read as a stepper; the number itself stays editable for big jumps. */
@Composable
private fun TrackingProgressCard(
    value: String,
    total: Int?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = value.toIntOrNull() ?: 0
    TrackingCard(tankobunString(R.string.tracking_chapters_read), modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackingStepButton(
                icon = TankobunIcons.Remove,
                label = tankobunString(R.string.tracking_decrease),
                enabled = current > 0,
                emphasized = false,
                onClick = { onValueChange((current - 1).coerceAtLeast(0).toString()) },
            )
            TrackingNumberField(
                value = value,
                total = total?.toString(),
                onValueChange = onValueChange,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            TrackingStepButton(
                icon = TankobunIcons.Add,
                label = tankobunString(R.string.tracking_increase),
                enabled = total == null || current < total,
                emphasized = true,
                onClick = { onValueChange((current + 1).let { next -> total?.let { next.coerceAtMost(it) } ?: next }.toString()) },
            )
        }
    }
}

@Composable
private fun TrackingScoreCard(
    scoreFormat: AnilistScoreFormat,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackingCard(scoreFormat.scoreLabel(), modifier) {
        when (scoreFormat) {
            AnilistScoreFormat.POINT_3 -> MoodScoreInput(value, onValueChange)
            AnilistScoreFormat.POINT_5 -> TrackingStars(
                filled = value.toDoubleOrNull()?.roundToInt()?.coerceIn(0, 5) ?: 0,
                onSelect = { star -> onValueChange(if (value.toDoubleOrNull()?.roundToInt() == star) "" else star.toString()) },
            )
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                TrackingNumberField(
                    value = value,
                    total = scoreFormat.scoreSuffix().removePrefix("/").trim(),
                    onValueChange = onValueChange,
                    keyboardType = if (scoreFormat == AnilistScoreFormat.POINT_10_DECIMAL) KeyboardType.Decimal else KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                // Stars mirror the number at a glance; typing stays the way to set it.
                val max = when (scoreFormat) {
                    AnilistScoreFormat.POINT_100 -> 100.0
                    else -> 10.0
                }
                TrackingStars(filled = ((value.toDoubleOrNull() ?: 0.0) / max * 5).roundToInt().coerceIn(0, 5), size = 13.dp)
            }
        }
    }
}

@Composable
private fun TrackingCard(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TrackingFieldLabel(label)
            content()
        }
    }
}

@Composable
private fun TrackingStepButton(icon: ImageVector, label: String, enabled: Boolean, emphasized: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = label },
        shape = CircleShape,
        color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (emphasized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer { alpha = if (enabled) 1f else 0.38f }) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

/** A large display number the reader can tap and type over, with its maximum in small print below. */
@Composable
private fun TrackingNumberField(
    value: String,
    total: String?,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = TankobunDisplayFontFamily,
                fontSize = 30.sp,
                lineHeight = 30.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
        if (!total.isNullOrBlank()) {
            Text(
                "/ $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TrackingStars(filled: Int, size: Dp = 22.dp, onSelect: ((Int) -> Unit)? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { star ->
            val on = star <= filled
            val starModifier = if (onSelect != null) {
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onSelect(star) }
                    .semantics { selected = on }
            } else {
                Modifier
            }
            Box(starModifier, contentAlignment = Alignment.Center) {
                Icon(
                    TankobunIcons.Star,
                    contentDescription = if (onSelect != null) tankobunString(R.string.detail_star_score_cd, star) else null,
                    tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    modifier = Modifier.size(size),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackingCustomLists(
    availableLists: List<String>,
    selectedLists: Collection<String>,
    onListSelected: (String, Boolean) -> Unit,
    onAddList: (String) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TrackingFieldLabel(tankobunString(R.string.detail_custom_lists))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            availableLists.forEach { listName ->
                val active = selectedLists.any { it.equals(listName, ignoreCase = true) }
                TankobunChip(
                    selected = active,
                    onClick = { onListSelected(listName, !active) },
                    label = { Text(listName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = if (active) ({ TankobunChipIcon(TankobunIcons.Check) }) else null,
                )
            }
            // Dashed, like the design's "+ Adicionar lista": it adds rather than selects.
            Surface(
                onClick = { adding = true },
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .height(32.dp)
                    .dashedBorder(MaterialTheme.colorScheme.outline, 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(TankobunIcons.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(tankobunString(R.string.tracking_add_list), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (adding) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(tankobunString(R.string.detail_new_list)) },
                )
                TextButton(
                    onClick = {
                        onAddList(newListName)
                        newListName = ""
                        adding = false
                    },
                    enabled = newListName.isNotBlank(),
                ) {
                    Text(tankobunString(R.string.common_add))
                }
            }
        }
    }
}

/** A rounded dashed outline, which Material borders cannot draw. */
private fun Modifier.dashedBorder(color: Color, radius: Dp): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
    )
}

@Composable
internal fun MoodScoreInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val selected = value.toDoubleOrNull()?.roundToInt() ?: 0
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(1 to ":(", 2 to ":|", 3 to ":)").forEach { (score, label) ->
                TankobunChip(
                    selected = selected == score,
                    onClick = { onValueChange(if (selected == score) "" else score.toString()) },
                    label = { Text(label) },
                )
        }
    }
}

@Composable
internal fun AnilistScoreFormat.scoreLabel(): String = tankobunString(R.string.common_score)

internal fun AnilistScoreFormat.scoreSuffix(): String = when (this) {
    AnilistScoreFormat.POINT_100 -> "/ 100"
    AnilistScoreFormat.POINT_10_DECIMAL,
    AnilistScoreFormat.POINT_10 -> "/ 10"
    AnilistScoreFormat.POINT_5 -> "/ 5"
    AnilistScoreFormat.POINT_3 -> "/ 3"
}

@Composable
internal fun RecommendationsSection(
    recommendations: List<AnilistRecommendation>,
    hasMore: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onSelectMedia: (AnilistMedia) -> Unit,
    trackedStatuses: Map<Int, MediaStatus>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MediaDetailContentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailSectionTitle(tankobunString(R.string.detail_recommendations))
            Text(
                tankobunString(R.string.detail_shown_count, recommendations.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val horizontalPadding = MediaDetailContentPadding
            val visibleCount = when {
                maxWidth >= 840.dp -> 7
                maxWidth >= 600.dp -> 5
                else -> 3
            }
            val tileSpacing = 12.dp
            val tileWidth = ((maxWidth - horizontalPadding * 2 - tileSpacing * (visibleCount - 1).toFloat()) / visibleCount.toFloat())
                .coerceIn(92.dp, 132.dp)
            val tileHeight = tileWidth * 1.5f + 58.dp
            LazyRow(
                modifier = Modifier.height(tileHeight),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(tileSpacing),
            ) {
                items(recommendations, key = { it.media.id }) { recommendation ->
                    RecommendationTile(
                        recommendation = recommendation,
                        onClick = { onSelectMedia(recommendation.media) },
                        trackedStatus = trackedStatuses[recommendation.media.id],
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
internal fun RecommendationTile(
    recommendation: AnilistRecommendation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trackedStatus: MediaStatus? = null,
) {
    val media = recommendation.media
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            shape = LocalTankobunStyle.current.themeShapes.panel,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
        ) {
            TrackedCoverImage(
                url = media.coverImage,
                title = media.title.userPreferred,
                trackedStatus = trackedStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
            )
        }
        Column(
            modifier = Modifier.height(54.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                media.title.userPreferred,
                style = MaterialTheme.typography.labelMedium.copy(lineHeight = 16.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TankobunMediaStatusLabel(text = media.status.statusLabel())
        }
    }
}

@Composable
internal fun LoadMoreRecommendationsTile(
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = tankobunString(R.string.detail_load_more),
    icon: androidx.compose.ui.graphics.vector.ImageVector = TankobunIcons.Refresh,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    labelStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelMedium,
) {
    Surface(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clickable(enabled = !loading, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick),
        shape = LocalTankobunStyle.current.themeShapes.panel,
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
                Text(tankobunString(R.string.common_loading), style = MaterialTheme.typography.labelMedium)
            } else {
                Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
                Text(
                    label,
                    style = labelStyle,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
internal fun CoverZoomOverlay(media: AnilistMedia, onDismiss: () -> Unit) {
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = safeInsets.calculateStartPadding(layoutDirection) + 16.dp,
                    top = safeInsets.calculateTopPadding() + 88.dp,
                    end = safeInsets.calculateEndPadding(layoutDirection) + 16.dp,
                    bottom = safeInsets.calculateBottomPadding() + 96.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val availableWidth = maxWidth
            val availableHeight = maxHeight
            val widthFromHeight = availableHeight * (2f / 3f)
            val coverWidth = minOf(availableWidth, widthFromHeight).coerceAtLeast(180.dp)
            Surface(
                modifier = Modifier
                    .width(coverWidth)
                    .aspectRatio(2f / 3f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
                shape = LocalTankobunStyle.current.themeShapes.panel,
                color = Color.Transparent,
                shadowElevation = 18.dp,
            ) {
                CoverImage(
                    url = media.coverImage.sharpestMangaBakaCover(),
                    title = media.title.userPreferred,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    imageAlignment = Alignment.TopCenter,
                    cornerRadius = LocalTankobunStyle.current.radii.panel,
                )
            }
        }
    }
}

internal fun trackingStatusIcon(status: MediaStatus): ImageVector = when (status) {
    MediaStatus.CURRENT -> TankobunIcons.ReadingStatus
    MediaStatus.PLANNING -> TankobunIcons.StarBorder
    MediaStatus.COMPLETED -> TankobunIcons.Check
    MediaStatus.PAUSED -> TankobunIcons.Pause
    MediaStatus.DROPPED -> TankobunIcons.Close
    MediaStatus.REPEATING -> TankobunIcons.Replay
    MediaStatus.UNKNOWN -> TankobunIcons.Check
}

internal fun trackingStatuses(): List<MediaStatus> = listOf(
    MediaStatus.CURRENT,
    MediaStatus.PLANNING,
    MediaStatus.COMPLETED,
    MediaStatus.PAUSED,
    MediaStatus.DROPPED,
    MediaStatus.REPEATING,
)

@Composable
internal fun CoverImage(
    url: String?,
    title: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    imageAlignment: Alignment = Alignment.Center,
    cornerRadius: Dp = 8.dp,
    imageModel: Any? = url,
    onImageError: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
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
            model = imageModel,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            alignment = imageAlignment,
            onError = { onImageError?.invoke() },
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

/** The detail top bar's ⋮: the less frequent chapter and source actions the header no longer shows. */
@Composable
internal fun MediaDetailOverflowMenu(
    state: TankobunUiState,
    viewModel: MainViewModel,
    chrome: MediaDetailChromeState,
    actions: MediaDetailUiActions,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        TopBarActionButton(
            icon = TankobunIcons.MoreVertical,
            contentDescription = tankobunString(R.string.detail_more_options),
            onClick = { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.widthIn(min = 220.dp)) {
            @Composable
            fun item(label: String, icon: ImageVector, onClick: () -> Unit) {
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        open = false
                        onClick()
                    },
                )
            }
            if (state.selectedSourceManga != null) {
                item(tankobunString(R.string.chapter_refresh_chapters), TankobunIcons.Refresh, viewModel::loadChaptersForCurrentMatch)
            }
            val groupPreference = state.chapterGroupPreference
            if (state.chapterGroupSelection.groups.size >= 2 || groupPreference.oneVersionPerChapter || groupPreference.preferredGroup != null) {
                item(tankobunString(R.string.chapter_groups_title), TankobunIcons.Tune) { chrome.chapterGroupsOpen = true }
            }
            item(tankobunString(R.string.source_change_source), TankobunIcons.SwapHoriz, actions.onChooseSource)
        }
    }
}
