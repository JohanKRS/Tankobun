package com.tankobun.app.ui.media

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
) {
    val backdrop = mediaDetailBackdropColor()
    val context = LocalContext.current
    val heroBackdropImage = media.bannerImage ?: media.coverImage
    val heroBackdropRequest = remember(context, heroBackdropImage) {
        ImageRequest.Builder(context)
            .data(heroBackdropImage)
            .crossfade(450)
            .build()
    }
    val listState = rememberLazyListState()
    val trackedStatuses = remember(state.libraryItems) { state.libraryItems.trackedMediaStatuses() }
    var coverZoomOpen by remember(media.id) { mutableStateOf(false) }
    var detailShareOpen by remember(media.id) { mutableStateOf(false) }
    val detailBlur by animateDpAsState(
        targetValue = when {
            state.sourcePickerOpen -> 8.dp
            coverZoomOpen -> QuickDrawerBackdropBlurDp.dp
            else -> 0.dp
        },
        animationSpec = tween(durationMillis = QuickDrawerSnapMillis),
        label = "Manga detail backdrop blur",
    )
    val coverZoomScrimAlpha by animateFloatAsState(
        targetValue = if (coverZoomOpen) CoverZoomScrimAlpha else 0f,
        animationSpec = tween(durationMillis = QuickDrawerSnapMillis),
        label = "Cover zoom scrim",
    )
    LaunchedEffect(media.id) {
        listState.scrollToItem(0)
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
                verticalArrangement = Arrangement.spacedBy(18.dp),
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
                            onAddToLibrary = viewModel::saveTracking,
                            onTrackingStatusSelected = viewModel::setTrackingStatus,
                            onShareMedia = { detailShareOpen = true },
                        )
                    }
                }

                if (state.selectedRecommendations.isNotEmpty()) {
                    item {
                        RecommendationsSection(
                            recommendations = state.selectedRecommendations,
                            hasMore = state.selectedRecommendationsHasMore,
                            loadingMore = state.recommendationsLoading,
                            onLoadMore = viewModel::loadMoreRecommendations,
                            onSelectMedia = onSelectMedia,
                            trackedStatuses = trackedStatuses,
                        )
                    }
                }

                item {
                    Box(Modifier.padding(horizontal = MediaDetailContentPadding)) {
                        state.message?.let {
                            Text(it, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                item {
                    Box(Modifier.padding(horizontal = MediaDetailContentPadding)) {
                        SourceSummarySection(state, viewModel)
                    }
                }

                item {
                    Box(Modifier.padding(horizontal = MediaDetailContentPadding)) {
                        var downloadActionsOpen by remember { mutableStateOf(false) }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailSectionTitle(tankobunString(R.string.common_chapters))
                            if (state.selectedSourceManga == null) {
                                DetailPlaceholderCard(
                                    icon = TankobunIcons.MenuBook,
                                    title = tankobunString(R.string.detail_chapters_empty_title),
                                    subtitle = tankobunString(R.string.detail_chapters_empty_select_source),
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ChapterActionsBar(
                                        readingActionChapter = state.primaryReadingActionChapter(),
                                        hasProgress = state.latestProgress != null,
                                        hasChapters = state.sourceChapters.isNotEmpty(),
                                        chapterListStartsAtFirst = state.chapterListStartsAtFirst,
                                        onOpenChapter = viewModel::openChapter,
                                        onLoadChapters = viewModel::loadChaptersForCurrentMatch,
                                        onToggleChapterListOrder = viewModel::toggleChapterListOrder,
                                        onOpenDownloadActions = { downloadActionsOpen = true },
                                    )
                                    if (state.selectingDownloadChapters) {
                                        ChapterManualDownloadBar(
                                            selectedCount = state.selectedDownloadChapterUrls.size,
                                            onDownloadSelected = viewModel::downloadSelectedChapters,
                                            onCancel = viewModel::cancelManualDownloadSelection,
                                        )
                                    }
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
                }

                if (state.selectedSourceManga != null && state.sourceChapters.isEmpty()) {
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
                        state.sourceChapters
                    } else {
                        state.sourceChapters.asReversed()
                    }
                    items(visibleChapters, key = { "${it.sourceId}:${it.url}" }) { chapter ->
                        Box(Modifier.padding(horizontal = MediaDetailContentPadding)) {
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
            enter = fadeIn(animationSpec = tween(durationMillis = QuickDrawerSnapMillis)) +
                scaleIn(initialScale = 0.90f, animationSpec = tween(durationMillis = QuickDrawerSnapMillis)),
            exit = fadeOut(animationSpec = tween(durationMillis = QuickDrawerSnapMillis)) +
                scaleOut(targetScale = 0.92f, animationSpec = tween(durationMillis = QuickDrawerSnapMillis)),
        ) {
            CoverZoomOverlay(media = media, onDismiss = { coverZoomOpen = false })
        }

        if (state.sourcePickerOpen) {
            SourcePickerDialog(state, viewModel, media)
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
            id = -media.id,
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
        shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
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
    onAddToLibrary: () -> Unit,
    onTrackingStatusSelected: (MediaStatus) -> Unit,
    onShareMedia: () -> Unit,
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
                onAddToLibrary = onAddToLibrary,
                onTrackingStatusSelected = onTrackingStatusSelected,
                onShareMedia = onShareMedia,
            )
            MangaDescriptionAndTags(media = media, compact = compact, onTagClick = onTagClick)
        }
    }
}

@Composable
internal fun MediaDetailQuickActions(
    state: TankobunUiState,
    compact: Boolean,
    onAddToLibrary: () -> Unit,
    onTrackingStatusSelected: (MediaStatus) -> Unit,
    onShareMedia: () -> Unit,
) {
    val fixedStatusSelected = state.selectedListEntry?.hiddenFromStatusLists != true ||
        state.trackingStatus != MediaStatus.UNKNOWN
    val selectedFixedStatus = if (fixedStatusSelected) state.trackingStatus else null
    val canSaveTracking = (state.libraryMode == LibraryMode.LOCAL || state.loggedIn) &&
        !state.trackingSaveInProgress &&
        fixedStatusSelected &&
        (state.selectedListEntry == null || state.trackingDirty || state.trackingSaveFailed)
    val actionLabel = when {
        !fixedStatusSelected -> tankobunString(R.string.detail_choose_status)
        state.trackingSaveInProgress -> tankobunString(R.string.detail_saving)
        state.trackingSaveFailed -> tankobunString(R.string.common_retry_save)
        state.selectedListEntry == null -> {
            if (compact) {
                tankobunString(R.string.detail_add_to_library_compact)
            } else {
                tankobunString(R.string.detail_add_to_library)
            }
        }
        state.trackingDirty -> tankobunString(R.string.detail_save_tracking)
        else -> tankobunString(R.string.common_saved)
    }
    val actionIcon = when {
        state.trackingSaveInProgress || state.trackingSaveFailed -> TankobunIcons.Refresh
        state.selectedListEntry == null -> TankobunIcons.Add
        !state.trackingDirty -> TankobunIcons.Check
        else -> TankobunIcons.LibraryBooks
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TankobunActionButton(
            label = actionLabel,
            onClick = onAddToLibrary,
            modifier = Modifier.weight(1.16f),
            icon = actionIcon,
            enabled = canSaveTracking,
            disabledContainerColor = if (state.trackingSaveInProgress || state.selectedListEntry != null && !state.trackingDirty) {
                LocalTankobunStyle.current.colors.selectedChip
            } else {
                null
            },
            disabledContentColor = if (state.trackingSaveInProgress || state.selectedListEntry != null && !state.trackingDirty) {
                LocalTankobunStyle.current.colors.selectedChipContent
            } else {
                null
            },
        )
        MediaDetailStatusDropdown(
            selected = selectedFixedStatus,
            onSelected = onTrackingStatusSelected,
            modifier = Modifier.weight(0.96f),
        )
        if (compact) {
            TankobunIconActionButton(
                icon = TankobunIcons.Share,
                contentDescription = tankobunString(R.string.detail_share_manga),
                onClick = onShareMedia,
                enabled = !state.busy,
            )
        } else {
            TankobunActionButton(
                label = tankobunString(R.string.library_batch_share),
                icon = TankobunIcons.Share,
                onClick = onShareMedia,
                enabled = !state.busy,
                filled = false,
            )
        }
    }
}

@Composable
private fun MediaDetailStatusDropdown(
    selected: MediaStatus?,
    onSelected: (MediaStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LocalTankobunStyle.current.sizes.iconAction),
            shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
            contentPadding = PaddingValues(horizontal = 10.dp),
            border = BorderStroke(
                width = 1.dp,
                color = LocalTankobunStyle.current.colors.outline.copy(alpha = 0.42f),
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = mediaDetailForegroundColor(),
            ),
        ) {
            if (selected != null) {
                Icon(
                    trackingStatusIcon(selected),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = mediaDetailAccentColor(),
                )
                Spacer(Modifier.width(7.dp))
            }
            Text(
                selected?.displayName() ?: tankobunString(R.string.detail_choose_status),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                TankobunIcons.ExpandMore,
                contentDescription = tankobunString(R.string.detail_category_dropdown_cd),
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 196.dp),
        ) {
            trackingStatuses().forEach { status ->
                val isSelected = status == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            status.displayName(),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            trackingStatusIcon(status),
                            contentDescription = null,
                            tint = if (isSelected) mediaDetailAccentColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                TankobunIcons.Check,
                                contentDescription = null,
                                tint = mediaDetailAccentColor(),
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        onSelected(status)
                        expanded = false
                    },
                )
            }
        }
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
        shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
        color = Color.Transparent,
        shadowElevation = 8.dp,
    ) {
        CoverImage(
            url = media.coverImage,
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
    Text(
        listOfNotNull(
            media.mediaTypeLabel(),
            media.status.statusLabel(),
        ).joinToString("  /  ").uppercase(Locale.ROOT),
        style = if (compact) {
            MaterialTheme.typography.labelMedium
        } else {
            MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, lineHeight = 16.sp)
        },
        color = mediaDetailAccentColor(),
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun MangaStatRow(media: AnilistMedia, compact: Boolean) {
    val stats = listOf(
        (if (compact) tankobunString(R.string.detail_chapter_stat_short) else tankobunString(R.string.detail_chapter_stat)) to (media.chapters?.toString() ?: "--"),
        (if (compact) tankobunString(R.string.detail_volume_stat_short) else tankobunString(R.string.detail_volume_stat)) to (media.volumes?.toString() ?: "--"),
        tankobunString(R.string.common_score) to (media.averageScore?.let { "$it%" } ?: "--"),
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
                Text(
                    label.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
        shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
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
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 10.sp else 11.sp, lineHeight = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
            FlowRowCompat {
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

internal fun String?.plainMediaDescription(): String =
    this
        ?.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
        ?.replace(Regex("<[^>]*>"), "")
        ?.replace("&quot;", "\"")
        ?.replace("&#039;", "'")
        ?.replace("&amp;", "&")
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        .orEmpty()

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
internal fun AniListTrackingSection(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
    val fixedStatusSelected = state.selectedListEntry?.hiddenFromStatusLists != true ||
        state.trackingStatus != MediaStatus.UNKNOWN
    val selectedFixedStatus = if (fixedStatusSelected) state.trackingStatus else null
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AniListStatusSelector(
            selected = selectedFixedStatus,
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
                label = { Text(tankobunString(R.string.common_progress)) },
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
            label = { Text(tankobunString(R.string.common_notes)) },
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tankobunString(R.string.common_private), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = state.trackingPrivate, onCheckedChange = viewModel::setTrackingPrivate)
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
            val actionIcon = when {
                state.trackingSaveInProgress || state.trackingSaveFailed -> TankobunIcons.Refresh
                else -> TankobunIcons.Check
            }
            TankobunActionButton(
                label = actionLabel,
                icon = actionIcon,
                onClick = viewModel::saveTracking,
                enabled = canSaveTracking,
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
        if (state.libraryMode == LibraryMode.ANILIST && !state.loggedIn) {
            Text(
                tankobunString(R.string.detail_connect_anilist_tracking),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun AniListStatusSelector(selected: MediaStatus?, onSelected: (MediaStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
        ) {
            if (selected != null) {
                Icon(
                    trackingStatusIcon(selected),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                selected?.displayName() ?: tankobunString(R.string.detail_choose_status),
                modifier = Modifier.weight(1f),
            )
            Icon(TankobunIcons.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(visible = expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
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
                                .clip(RoundedCornerShape(LocalTankobunStyle.current.radii.control))
                                .clickable {
                                    onSelected(status)
                                    expanded = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                trackingStatusIcon(status),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (status == selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Text(
                                status.displayName(),
                                modifier = Modifier.weight(1f),
                                fontWeight = if (status == selected) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (status == selected) {
                                Icon(
                                    TankobunIcons.Check,
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
internal fun AniListCustomListSelector(
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
        ?: tankobunString(R.string.detail_custom_lists)

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
        ) {
            Text(selectedLabel, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(TankobunIcons.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(visible = expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
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
                            tankobunString(R.string.detail_no_custom_lists),
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
                                    .clip(RoundedCornerShape(LocalTankobunStyle.current.radii.control))
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
                            label = { Text(tankobunString(R.string.detail_new_list)) },
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
                                Text(tankobunString(R.string.common_add))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AniListScoreInput(
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
internal fun StarScoreInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val selected = value.toDoubleOrNull()?.roundToInt() ?: 0
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(tankobunString(R.string.common_score), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            (1..5).forEach { star ->
                IconButton(
                    onClick = { onValueChange(if (selected == star) "" else star.toString()) },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        if (star <= selected) TankobunIcons.Star else TankobunIcons.StarBorder,
                        contentDescription = tankobunString(R.string.detail_star_score_cd, star),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
            shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
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
) {
    Surface(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clickable(enabled = !loading, onClick = onClick),
        shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
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
                Icon(TankobunIcons.Refresh, contentDescription = null)
                Text(
                    tankobunString(R.string.detail_load_more),
                    style = MaterialTheme.typography.labelMedium,
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
                shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
                color = Color.Transparent,
                shadowElevation = 18.dp,
            ) {
                CoverImage(
                    url = media.coverImage,
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
    MediaStatus.CURRENT -> TankobunIcons.PlayArrow
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
