package com.tankobun.app.ui.media

import com.tankobun.app.ui.icons.TankobunIcons

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.MainViewModel
import com.tankobun.app.R
import com.tankobun.app.TankobunDisplayFontFamily
import com.tankobun.app.tankobunString
import com.tankobun.app.logic.nextInReadingOrderAfter
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.ui.components.TankobunActionButton
import com.tankobun.app.ui.components.TankobunDialog
import com.tankobun.app.ui.components.TankobunDialogHeader
import com.tankobun.app.ui.components.TankobunIconActionButton
import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.SourceChapter
import kotlin.math.abs

@Composable
internal fun ChapterActionsBar(
    readingActionChapter: SourceChapter?,
    hasProgress: Boolean,
    hasChapters: Boolean,
    chapterListStartsAtFirst: Boolean,
    onOpenChapter: (SourceChapter) -> Unit,
    onLoadChapters: () -> Unit,
    onToggleChapterListOrder: () -> Unit,
    onOpenDownloadActions: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val tight = maxWidth < 380.dp
        val actionHeight = LocalTankobunStyle.current.sizes.iconAction
        val startReadingButton: @Composable (Modifier) -> Unit = { modifier ->
            if (readingActionChapter != null) {
                if (tight) {
                    TankobunIconActionButton(
                        icon = TankobunIcons.PlayArrow,
                        contentDescription = if (hasProgress) {
                            tankobunString(R.string.chapter_resume_reading)
                        } else {
                            tankobunString(R.string.chapter_start_reading)
                        },
                        onClick = { onOpenChapter(readingActionChapter) },
                        modifier = modifier,
                        filled = true,
                    )
                } else {
                    TankobunActionButton(
                        label = if (hasProgress) tankobunString(R.string.chapter_resume) else tankobunString(R.string.common_start),
                        icon = TankobunIcons.PlayArrow,
                        onClick = { onOpenChapter(readingActionChapter) },
                        modifier = modifier,
                    )
                }
            }
        }
        val refreshButton: @Composable (Modifier) -> Unit = { modifier ->
            TankobunIconActionButton(
                icon = TankobunIcons.Refresh,
                contentDescription = if (hasChapters) {
                    tankobunString(R.string.chapter_refresh_chapters)
                } else {
                    tankobunString(R.string.chapter_load_chapters)
                },
                onClick = onLoadChapters,
                modifier = modifier,
            )
        }
        val orderButtonDescription = if (chapterListStartsAtFirst) {
            tankobunString(R.string.chapter_show_latest_first)
        } else {
            tankobunString(R.string.chapter_show_first_first)
        }
        val orderButton: @Composable (Modifier) -> Unit = { modifier ->
            TankobunIconActionButton(
                icon = TankobunIcons.SwapVert,
                contentDescription = orderButtonDescription,
                onClick = onToggleChapterListOrder,
                enabled = hasChapters,
                modifier = modifier,
            )
        }
        val downloadButton: @Composable (Modifier) -> Unit = { modifier ->
            if (tight) {
                TankobunIconActionButton(
                    icon = TankobunIcons.Download,
                    contentDescription = tankobunString(R.string.chapter_download_chapters),
                    onClick = onOpenDownloadActions,
                    enabled = hasChapters,
                    modifier = modifier,
                )
            } else {
                TankobunActionButton(
                    label = tankobunString(R.string.common_download),
                    icon = TankobunIcons.Download,
                    onClick = onOpenDownloadActions,
                    enabled = hasChapters,
                    filled = false,
                    modifier = modifier,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (readingActionChapter != null) {
                startReadingButton(
                    if (tight) Modifier.width(actionHeight) else Modifier.widthIn(min = 122.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            refreshButton(Modifier)
            orderButton(Modifier.width(actionHeight))
            downloadButton(if (tight) Modifier.width(actionHeight) else Modifier.widthIn(min = 116.dp))
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
        shape = LocalTankobunStyle.current.themeShapes.panel,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.70f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                tankobunString(R.string.chapter_selected_count, selectedCount),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onCancel) {
                Text(tankobunString(R.string.common_cancel))
            }
            Button(
                onClick = onDownloadSelected,
                enabled = selectedCount > 0,
                shape = LocalTankobunStyle.current.themeShapes.control,
            ) {
                Icon(TankobunIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(tankobunString(R.string.common_download))
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
    TankobunDialog(onDismiss = onDismiss, maxHeight = 640.dp) {
        TankobunDialogHeader(title = tankobunString(R.string.chapter_download_dialog_title), onDismiss = onDismiss)
        ChapterDownloadActionRow(
            title = tankobunString(R.string.chapter_all_chapters),
            subtitle = tankobunString(R.string.chapter_all_chapters_desc),
            onClick = onDownloadAll,
        )
        ChapterDownloadActionRow(
            title = tankobunString(R.string.chapter_unread_only),
            subtitle = tankobunString(R.string.chapter_unread_only_desc),
            onClick = onDownloadUnread,
        )
        ChapterDownloadActionRow(
            title = tankobunString(R.string.chapter_next_10),
            subtitle = tankobunString(R.string.chapter_next_10_desc),
            onClick = onDownloadNextTen,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = LocalTankobunStyle.current.themeShapes.panel,
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
                    Text(tankobunString(R.string.chapter_keep_next_10), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        tankobunString(R.string.chapter_keep_next_10_desc),
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
            title = tankobunString(R.string.chapter_select_manually),
            subtitle = tankobunString(R.string.chapter_select_manually_desc),
            onClick = onSelectManually,
        )
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
        shape = LocalTankobunStyle.current.themeShapes.panel,
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LocalTankobunStyle.current.themeShapes.control)
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(TankobunIcons.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
    key(chapter.url, download?.state, download?.completedPages, download?.pageCount, selectingForDownload, selectedForDownload) {
        val chapterShape = RoundedCornerShape(8.dp)
        val latestRead by rememberUpdatedState(read)
        var swipeActionRead by remember(chapter.url) { mutableStateOf(read) }
        var dragOffset by remember(chapter.url) { mutableFloatStateOf(0f) }
        var dragging by remember(chapter.url) { mutableStateOf(false) }
        val swipeActionLabel = if (swipeActionRead) {
            tankobunString(R.string.chapter_mark_unread)
        } else {
            tankobunString(R.string.chapter_mark_read)
        }
        val swipeActionIcon = if (swipeActionRead) TankobunIcons.Replay else TankobunIcons.Check
        val swipeActionColor = LocalTankobunStyle.current.colors.accent
        val density = LocalDensity.current
        val maxRevealPx = with(density) { 132.dp.toPx() }
        val hardRevealPx = with(density) { 164.dp.toPx() }
        val actionThresholdPx = with(density) { 76.dp.toPx() }
        fun resistedSwipeOffset(proposedOffset: Float): Float {
            val distance = abs(proposedOffset)
            if (distance <= maxRevealPx) return proposedOffset
            val direction = if (proposedOffset < 0f) -1f else 1f
            val extra = ((distance - maxRevealPx) * 0.22f).coerceAtMost(hardRevealPx - maxRevealPx)
            return direction * (maxRevealPx + extra)
        }
        val draggableState = rememberDraggableState { delta ->
            dragOffset = resistedSwipeOffset(dragOffset + delta)
        }
        val animatedDragOffset by animateFloatAsState(
            targetValue = dragOffset,
            animationSpec = if (dragging) {
                tween(durationMillis = 0)
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
            },
            label = "chapterSwipeOffset",
        )
        LaunchedEffect(read, dragging, dragOffset) {
            if (!dragging && dragOffset == 0f) {
                swipeActionRead = read
            }
        }
        @Composable
        fun ChapterCard() {
            ElevatedCard(
                shape = chapterShape,
                onClick = {
                    if (selectingForDownload) {
                        onToggleDownloadSelection()
                    } else {
                        viewModel.openChapter(chapter)
                    }
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
                        Text(
                            chapter.name,
                            style = bebasNeueChapterTitleStyle(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (read) 0.66f else 1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
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
            val actionAlignment = if (animatedDragOffset < 0f) Alignment.CenterEnd else Alignment.CenterStart
            val actionAlpha = (abs(animatedDragOffset) / with(density) { 72.dp.toPx() }).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(chapterShape)
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Horizontal,
                        onDragStarted = {
                            swipeActionRead = latestRead
                            dragging = true
                        },
                        onDragStopped = {
                            val shouldToggle = abs(dragOffset) >= actionThresholdPx
                            val targetRead = !swipeActionRead
                            dragging = false
                            dragOffset = 0f
                            if (shouldToggle) {
                                viewModel.setChapterRead(chapter, read = targetRead)
                            }
                        },
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(swipeActionColor.copy(alpha = 0.18f))
                        .padding(horizontal = 18.dp),
                ) {
                    ChapterSwipeAction(
                        label = swipeActionLabel,
                        icon = swipeActionIcon,
                        color = swipeActionColor,
                        modifier = Modifier
                            .align(actionAlignment)
                            .graphicsLayer { alpha = actionAlpha },
                    )
                }
                Box(
                    modifier = Modifier.graphicsLayer {
                        translationX = animatedDragOffset
                    },
                ) {
                    ChapterCard()
                }
            }
        }
    }
}

@Composable
private fun bebasNeueChapterTitleStyle(): TextStyle =
    MaterialTheme.typography.titleMedium.copy(
        fontFamily = TankobunDisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    )

@Composable
internal fun ChapterSwipeAction(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
}

@Composable
internal fun ChapterDownloadIndicator(
    download: DownloadJob?,
    onDownload: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
) {
    val actionSize = LocalTankobunStyle.current.sizes.iconAction
    when {
        download == null -> IconButton(onClick = onDownload, modifier = Modifier.size(actionSize)) {
            Icon(TankobunIcons.Download, contentDescription = tankobunString(R.string.common_download))
        }

        download.state == DownloadState.COMPLETE -> Box(
            modifier = Modifier.size(actionSize),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        TankobunIcons.Check,
                        contentDescription = tankobunString(R.string.common_downloaded),
                        modifier = Modifier.size(20.dp),
                    )
                }
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
                modifier = Modifier.size(actionSize),
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

        download.state == DownloadState.PAUSED -> IconButton(onClick = onResume, modifier = Modifier.size(actionSize)) {
            Icon(TankobunIcons.PlayArrow, contentDescription = tankobunString(R.string.downloads_resume_download))
        }

        download.state == DownloadState.FAILED -> IconButton(onClick = onRetry, modifier = Modifier.size(actionSize)) {
            Icon(TankobunIcons.Replay, contentDescription = tankobunString(R.string.downloads_retry_download))
        }
    }
}
