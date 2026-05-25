package com.tankobun.app.ui.media

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
internal fun MangaDetailScreen(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (state.sourcePickerOpen) 8.dp else 0.dp),
            contentPadding = PaddingValues(vertical = MediaDetailContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = MediaDetailContentPadding)) {
                    Spacer(Modifier.height(4.dp))
                    MangaHeroSection(media)
                }
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
                Box(Modifier.padding(horizontal = MediaDetailContentPadding)) {
                    state.message?.let {
                        Text(it, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            item {
                Box(Modifier.padding(horizontal = MediaDetailContentPadding)) {
                    SourceSummarySection(state, viewModel, media)
                }
            }

            item {
                Box(Modifier.padding(horizontal = MediaDetailContentPadding)) {
                    var downloadActionsOpen by remember { mutableStateOf(false) }
                    DetailSectionTitle("Chapters")
                    Spacer(Modifier.height(8.dp))
                    if (state.selectedSourceManga == null) {
                        Text("Choose a source first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ChapterActionsBar(
                                readingActionChapter = state.primaryReadingActionChapter(),
                                hasProgress = state.latestProgress != null,
                                hasChapters = state.sourceChapters.isNotEmpty(),
                                onOpenChapter = viewModel::openChapter,
                                onLoadChapters = viewModel::loadChaptersForCurrentMatch,
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

            if (state.sourceChapters.isEmpty()) {
                item {
                    Text(
                        "No chapters loaded yet.",
                        modifier = Modifier.padding(horizontal = MediaDetailContentPadding),
                    )
                }
            } else {
                items(state.sourceChapters, key = { "${it.sourceId}:${it.url}" }) { chapter ->
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

        if (state.sourcePickerOpen) {
            SourcePickerDialog(state, viewModel, media)
        }
    }
}

private val MediaDetailContentPadding = 16.dp

@Composable
internal fun DetailSectionTitle(text: String, modifier: Modifier = Modifier) {
    val compact = LocalConfiguration.current.smallestScreenWidthDp in 1 until 600
    Text(
        text,
        modifier = modifier,
        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChapterActionsBar(
    readingActionChapter: SourceChapter?,
    hasProgress: Boolean,
    hasChapters: Boolean,
    onOpenChapter: (SourceChapter) -> Unit,
    onLoadChapters: () -> Unit,
    onOpenDownloadActions: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 430.dp
        val startReadingButton: @Composable () -> Unit = {
            if (readingActionChapter != null) {
                Button(onClick = { onOpenChapter(readingActionChapter) }) {
                    Text(if (hasProgress) "Resume" else "Start reading")
                }
            }
        }
        val refreshButton: @Composable () -> Unit = {
            OutlinedButton(onClick = onLoadChapters) {
                Text(if (hasChapters) "Refresh chapters" else "Load chapters")
            }
        }
        val downloadButton: @Composable () -> Unit = {
            OutlinedButton(
                onClick = onOpenDownloadActions,
                enabled = hasChapters,
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download")
            }
        }

        if (compact) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                startReadingButton()
                refreshButton()
                downloadButton()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                startReadingButton()
                refreshButton()
                Spacer(Modifier.weight(1f))
                downloadButton()
            }
        }
    }
}

@Composable
internal fun ChapterManualDownloadBar(
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
internal fun ChapterDownloadActionsDialog(
    keepNextTenDownloads: Boolean,
    onDismiss: () -> Unit,
    onDownloadAll: () -> Unit,
    onDownloadUnread: () -> Unit,
    onDownloadNextTen: () -> Unit,
    onKeepNextTenChange: (Boolean) -> Unit,
    onSelectManually: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(16.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 3.dp,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
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
                            .padding(12.dp),
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
internal fun ChapterDownloadActionRow(
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
                .padding(12.dp),
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
internal fun MangaHeroSection(media: AnilistMedia) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = LocalTankobunTokens.current.elevatedSurface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 620.dp
            if (compact) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CoverImage(
                        url = media.coverImage,
                        title = media.title.userPreferred,
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .widthIn(max = 260.dp)
                            .aspectRatio(2f / 3f),
                        contentScale = ContentScale.Fit,
                        imageAlignment = Alignment.TopCenter,
                    )
                    MangaHeroInfo(
                        media = media,
                        compact = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
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
                    MangaHeroInfo(
                        media = media,
                        compact = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun MangaHeroInfo(
    media: AnilistMedia,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
    ) {
        Text(
            media.title.userPreferred,
            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
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
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRowCompat {
            mangaInfoPill("Author", media.staff.authorLabel())
            mangaInfoPill("Published", media.publishingYearLabel())
            media.popularity?.let { mangaInfoPill("Readers", it.formatCompact()) }
        }
        Text(
            media.description?.replace(Regex("<[^>]*>"), "").orEmpty(),
            maxLines = if (compact) 5 else 8,
            overflow = TextOverflow.Ellipsis,
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        )
        val tags = media.tags.ifEmpty { media.genres }
        if (tags.isNotEmpty()) {
            FlowRowCompat {
                tags.take(if (compact) 6 else 10).forEach { tag ->
                    mangaTagPill(tag = tag, compact = compact)
                }
            }
        }
    }
}

@Composable
internal fun mangaTagPill(tag: String, compact: Boolean) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        ),
    ) {
        Text(
            tag,
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 4.dp else 6.dp,
            ),
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun mangaInfoPill(label: String, value: String) {
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
internal fun AniListTrackingSection(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
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
internal fun AniListStatusSelector(selected: MediaStatus, onSelected: (MediaStatus) -> Unit) {
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
internal fun MoodScoreInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val selected = value.toDoubleOrNull()?.roundToInt() ?: 0
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(1 to ":(", 2 to ":|", 3 to ":)").forEach { (score, label) ->
            FilterChip(
                selected = selected == score,
                onClick = { onValueChange(if (selected == score) "" else score.toString()) },
                colors = tankobunFilterChipColors(),
                label = { Text(label) },
            )
        }
    }
}

internal fun AnilistScoreFormat.scoreLabel(): String = when (this) {
    AnilistScoreFormat.POINT_100 -> "Score"
    AnilistScoreFormat.POINT_10_DECIMAL -> "Score"
    AnilistScoreFormat.POINT_10 -> "Score"
    AnilistScoreFormat.POINT_5 -> "Score"
    AnilistScoreFormat.POINT_3 -> "Score"
}

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
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MediaDetailContentPadding),
        ) {
            val compact = maxWidth < 430.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    DetailSectionTitle("Recommendations")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${recommendations.size} shown",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        if (hasMore) {
                            TextButton(onClick = onLoadMore, enabled = !loadingMore) {
                                Text(if (loadingMore) "Loading" else "Load more")
                            }
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailSectionTitle("Recommendations", modifier = Modifier.weight(1f))
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
            }
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
            LazyRow(
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(tileSpacing),
            ) {
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

private val RecommendationTitleHeight = 36.dp

@Composable
internal fun RecommendationTile(
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
            modifier = Modifier.height(RecommendationTitleHeight),
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
internal fun LoadMoreRecommendationsTile(
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
internal fun SourceSummarySection(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
    val selectedManga = state.selectedSourceManga
    val selectedSource = state.selectedSource
    val latestProgress = state.latestProgress

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailSectionTitle("Source")
        if (selectedManga == null) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 430.dp
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = viewModel::openSourcePicker,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Find source")
                        }
                        Text(
                            if (state.allInstalledSources.isEmpty()) "Install source extensions in Settings." else "No source selected.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = viewModel::openSourcePicker) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Find source")
                        }
                        Text(
                            if (state.allInstalledSources.isEmpty()) "Install source extensions in Settings." else "No source selected.",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
internal fun SourcePickerDialog(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
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
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
internal fun SourceDiagnosticRow(message: String) {
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
internal fun SourceMatchRow(
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
internal fun SourceCandidateRow(
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
internal fun sourceMatchKey(sourceId: Long, mangaUrl: String): String =
    "$sourceId:$mangaUrl"

internal fun trackingStatuses(): List<MediaStatus> = listOf(
    MediaStatus.CURRENT,
    MediaStatus.PLANNING,
    MediaStatus.COMPLETED,
    MediaStatus.PAUSED,
    MediaStatus.DROPPED,
    MediaStatus.REPEATING,
)

internal fun MediaStatus.displayName(): String = when (this) {
    MediaStatus.CURRENT -> "Reading"
    MediaStatus.PLANNING -> "Plan"
    MediaStatus.COMPLETED -> "Completed"
    MediaStatus.PAUSED -> "Paused"
    MediaStatus.DROPPED -> "Dropped"
    MediaStatus.REPEATING -> "Rereading"
    MediaStatus.UNKNOWN -> "Unknown"
}

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

internal fun TankobunUiState.primaryReadingActionChapter(): SourceChapter? =
    latestProgress?.let { progress ->
        val exactChapter = sourceChapters.firstOrNull { it.url == progress.chapterUrl }
        if (exactChapter != null) exactChapter else {
            sourceChapters.chapterNearProgress(progress)
        }
    } ?: sourceChapters.firstInReadingOrder()

internal fun TankobunUiState.nextReaderChapter(): SourceChapter? =
    sourceChapters.nextInReadingOrderAfter(activeChapter ?: return null)

internal fun List<SourceChapter>.chapterNearProgress(progress: ReadingProgress): SourceChapter? {
    val chapterNumber = progress.chapterNumber
    if (chapterNumber > 0f) {
        val nextChapter = if (progress.completed) {
            filter { it.chapterNumber > chapterNumber }.minByOrNull { it.chapterNumber }
        } else {
            filter { it.chapterNumber >= chapterNumber }.minByOrNull { it.chapterNumber }
        }
        if (nextChapter != null) return nextChapter
        return minByOrNull { abs((it.chapterNumber.takeIf { number -> number > 0f } ?: chapterNumber) - chapterNumber) }
    }
    return firstInReadingOrder()
}

internal fun List<SourceChapter>.firstInReadingOrder(): SourceChapter? =
    filter { it.chapterNumber > 0f }
        .minByOrNull { it.chapterNumber }
        ?: lastOrNull()

internal fun SourceChapter.isReadBy(progressByChapter: Map<String, ReadingProgress>): Boolean =
    progressByChapter[url]?.completed == true

internal fun TankobunUiState.downloadForChapter(chapter: SourceChapter): DownloadJob? {
    val mediaId = selectedMedia?.id ?: return null
    return downloads
        .filter { it.mediaId == mediaId && it.sourceId == chapter.sourceId && it.chapterUrl == chapter.url }
        .maxByOrNull { it.updatedAtEpochMillis }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChapterRow(
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
internal fun ChapterDownloadIndicator(
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
