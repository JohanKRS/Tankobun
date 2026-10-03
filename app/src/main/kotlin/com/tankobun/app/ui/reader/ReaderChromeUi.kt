package com.tankobun.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.MainViewModel
import com.tankobun.app.R
import com.tankobun.app.ReaderBackground
import com.tankobun.app.ReaderDirection
import com.tankobun.app.ReaderFit
import com.tankobun.app.ReaderScreenOrientation
import com.tankobun.app.ReaderPreferences
import com.tankobun.app.logic.isChapterRead
import com.tankobun.app.readerModeLabel
import com.tankobun.app.readerOrientationLabel
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.tankobunString
import com.tankobun.app.ui.components.TankobunChip
import com.tankobun.app.ui.components.TankobunChipIcon
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.app.ui.media.chapterDateLabel
import com.tankobun.app.ui.settings.SettingsToggleRow
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.SourceChapter

/** Text and spinner color that reads on the chosen reader background. */
internal val LocalReaderInk = staticCompositionLocalOf { Color.White }

internal fun ReaderBackground.color(): Color = when (this) {
    ReaderBackground.BLACK -> Color.Black
    ReaderBackground.GRAY -> Color(0xFF2A2A2D)
    ReaderBackground.WHITE -> Color(0xFFF7F7F5)
}

internal fun ReaderBackground.ink(): Color = when (this) {
    ReaderBackground.WHITE -> Color(0xFF1B1B1F)
    else -> Color.White
}

private val ReaderChromeShape = RoundedCornerShape(22.dp)
private val ReaderChromeColor = Color(0xE6141418)
private val ReaderChromeContent = Color.White

@Composable
private fun ReaderChromeSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = ReaderChromeShape,
        color = ReaderChromeColor,
        contentColor = ReaderChromeContent,
        content = content,
    )
}

@Composable
internal fun ReaderTopBar(
    chapterName: String,
    mediaTitle: String?,
    onClose: () -> Unit,
    onOpenChapters: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReaderChromeSurface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(TankobunIcons.ArrowBack, contentDescription = tankobunString(R.string.reader_close))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    chapterName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!mediaTitle.isNullOrBlank()) {
                    Text(
                        mediaTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = ReaderChromeContent.copy(alpha = 0.68f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onOpenChapters) {
                Icon(TankobunIcons.FormatListBulleted, contentDescription = tankobunString(R.string.reader_chapter_list))
            }
            IconButton(onClick = onOpenSettings) {
                Icon(TankobunIcons.Tune, contentDescription = tankobunString(R.string.reader_settings))
            }
        }
    }
}

/**
 * Page scrubber with chapter jumps. In right-to-left reading the whole row mirrors, so the
 * slider fills from the right and "next chapter" sits on the left, matching the page flow.
 */
@Composable
internal fun ReaderBottomBar(
    pageIndex: Int,
    pageCount: Int,
    rightToLeft: Boolean,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    zoomed: Boolean,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    scrubValue: Float,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastIndex = (pageCount - 1).coerceAtLeast(0)
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (zoomed) {
            ReaderChromeSurface(modifier = Modifier.padding(bottom = 8.dp)) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onResetZoom)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(TankobunIcons.FitScreen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(tankobunString(R.string.reader_reset_zoom), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        ReaderChromeSurface(modifier = Modifier.fillMaxWidth()) {
            CompositionLocalProvider(
                LocalLayoutDirection provides if (rightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val mirror = Modifier.graphicsLayer { scaleX = if (rightToLeft) -1f else 1f }
                    IconButton(onClick = onPreviousChapter, enabled = hasPreviousChapter) {
                        Icon(
                            TankobunIcons.ChapterPrevious,
                            contentDescription = tankobunString(R.string.reader_previous_chapter),
                            tint = ReaderChromeContent.copy(alpha = if (hasPreviousChapter) 1f else 0.34f),
                            modifier = mirror,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                "${pageIndex + 1} / ${pageCount.coerceAtLeast(1)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = ReaderChromeContent.copy(alpha = 0.86f),
                            )
                        }
                        if (pageCount > 1) {
                            Slider(
                                value = scrubValue.coerceIn(0f, lastIndex.toFloat()),
                                onValueChange = onScrub,
                                onValueChangeFinished = onScrubFinished,
                                valueRange = 0f..lastIndex.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = ReaderChromeContent.copy(alpha = 0.22f),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                            )
                        } else {
                            // A single-page chapter is complete as soon as it is open.
                            LinearProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = ReaderChromeContent.copy(alpha = 0.22f),
                                drawStopIndicator = {},
                            )
                        }
                    }
                    IconButton(onClick = onNextChapter, enabled = hasNextChapter) {
                        Icon(
                            TankobunIcons.ChapterNext,
                            contentDescription = tankobunString(R.string.reader_next_chapter),
                            tint = ReaderChromeContent.copy(alpha = if (hasNextChapter) 1f else 0.34f),
                            modifier = mirror,
                        )
                    }
                }
            }
        }
    }
}

/** Paged-mode interstitial between chapters; swiping or tapping forward again continues. */
@Composable
internal fun ReaderChapterEndPage(
    chapter: SourceChapter,
    nextChapter: SourceChapter?,
    onNextChapter: () -> Unit,
    onOpenChapters: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = LocalReaderInk.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Icon(
            if (nextChapter != null) TankobunIcons.ReadMark else TankobunIcons.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
        )
        Text(
            tankobunString(R.string.reader_chapter_end_title, chapter.name),
            style = MaterialTheme.typography.titleMedium,
            color = ink.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (nextChapter != null) {
            Text(
                tankobunString(R.string.reader_chapter_end_next, nextChapter.name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = ink,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            chapterDateLabel(nextChapter.uploadedAtEpochMillis)?.let { date ->
                Text(date, style = MaterialTheme.typography.bodyMedium, color = ink.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onNextChapter, modifier = Modifier.widthIn(min = 220.dp)) {
                Icon(TankobunIcons.ChapterNext, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(tankobunString(R.string.reader_next_chapter))
            }
        } else {
            Text(
                tankobunString(R.string.reader_chapter_end_caught_up),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = ink,
                textAlign = TextAlign.Center,
            )
            Text(
                tankobunString(R.string.reader_chapter_end_caught_up_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = ink.copy(alpha = 0.66f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClose, modifier = Modifier.widthIn(min = 220.dp)) {
                Text(tankobunString(R.string.reader_back_to_details))
            }
        }
        OutlinedButton(onClick = onOpenChapters, modifier = Modifier.widthIn(min = 220.dp)) {
            Icon(TankobunIcons.FormatListBulleted, contentDescription = null, modifier = Modifier.size(18.dp), tint = ink)
            Spacer(Modifier.size(8.dp))
            Text(tankobunString(R.string.reader_chapter_list), color = ink)
        }
    }
}

/** Compact webtoon transition between two chapters scrolling in one strip. */
@Composable
internal fun WebtoonChapterTransitionCard(previousChapterName: String, nextChapterName: String) {
    val ink = LocalReaderInk.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ink.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                tankobunString(R.string.reader_chapter_end_title, previousChapterName),
                style = MaterialTheme.typography.labelMedium,
                color = ink.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                nextChapterName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = ink.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderChapterListSheet(
    state: TankobunUiState,
    chapters: List<SourceChapter>,
    currentChapter: SourceChapter,
    onOpenChapter: (SourceChapter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val newestFirst = remember(chapters) { chapters.asReversed() }
    val currentIndex = newestFirst.indexOfFirst { it.url == currentChapter.url }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 2).coerceAtLeast(0))
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
    ) {
        Text(
            tankobunString(R.string.reader_chapter_list),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
        ) {
            items(newestFirst, key = { "${it.sourceId}:${it.url}" }) { chapter ->
                val current = chapter.url == currentChapter.url
                val read = state.isChapterRead(chapter)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !current) { onOpenChapter(chapter) }
                        .background(if (current) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            chapter.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                            color = if (current) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = if (read) 0.58f else 1f)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val meta = listOfNotNull(
                            tankobunString(R.string.reader_current_chapter).takeIf { current },
                            chapterDateLabel(chapter.uploadedAtEpochMillis),
                        ).joinToString(" · ")
                        if (meta.isNotEmpty()) {
                            Text(
                                meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (current) {
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (read && !current) {
                        Icon(
                            TankobunIcons.ReadMark,
                            contentDescription = tankobunString(R.string.chapter_read_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderSettingsSheet(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onReaderModeChange: (ReaderMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                tankobunString(R.string.reader_settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            ReaderSettingsControls(
                state = state,
                actions = rememberReaderSettingsActions(viewModel, onReaderModeChange),
            )
        }
    }
}

/** Shared by the in-reader sheet and Settings › Reader so both always offer the same options. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColumnScope.ReaderSettingsControls(
    state: TankobunUiState,
    actions: ReaderSettingsActions,
    showAllModes: Boolean = false,
) {
    val onReaderModeChange = actions.onReaderModeChange
    val preferences = state.readerPreferences
    val paged = state.readerMode == ReaderMode.PAGED
    ReaderSettingGroup(tankobunString(R.string.settings_reading_mode)) {
        ReaderOptionChip(ReaderMode.PAGED.readerModeLabel(), TankobunIcons.MenuBook, paged) {
            onReaderModeChange(ReaderMode.PAGED)
        }
        ReaderOptionChip(ReaderMode.WEBTOON.readerModeLabel(), TankobunIcons.ViewStream, !paged) {
            onReaderModeChange(ReaderMode.WEBTOON)
        }
    }
    if (paged || showAllModes) {
        ReaderSettingGroup(tankobunString(R.string.reader_direction)) {
            ReaderOptionChip(
                tankobunString(R.string.reader_direction_ltr),
                TankobunIcons.ReadingLeftToRight,
                preferences.direction == ReaderDirection.LEFT_TO_RIGHT,
            ) { actions.onUpdatePreferences { it.copy(direction = ReaderDirection.LEFT_TO_RIGHT) } }
            ReaderOptionChip(
                tankobunString(R.string.reader_direction_rtl),
                TankobunIcons.ReadingRightToLeft,
                preferences.direction == ReaderDirection.RIGHT_TO_LEFT,
            ) { actions.onUpdatePreferences { it.copy(direction = ReaderDirection.RIGHT_TO_LEFT) } }
        }
        ReaderSettingGroup(tankobunString(R.string.reader_fit)) {
            ReaderOptionChip(tankobunString(R.string.reader_fit_width), TankobunIcons.FitWidth, preferences.fit == ReaderFit.WIDTH) {
                actions.onUpdatePreferences { it.copy(fit = ReaderFit.WIDTH) }
            }
            ReaderOptionChip(tankobunString(R.string.reader_fit_screen), TankobunIcons.FitPage, preferences.fit == ReaderFit.SCREEN) {
                actions.onUpdatePreferences { it.copy(fit = ReaderFit.SCREEN) }
            }
        }
    }
    ReaderSettingGroup(tankobunString(R.string.settings_page_gaps)) {
        (0..3).forEach { level ->
            ReaderOptionChip(readerGapLabel(level), null, state.readerPageGapLevel == level) {
                actions.onPageGapChange(level)
            }
        }
    }
    ReaderSettingGroup(tankobunString(R.string.reader_background)) {
        ReaderBackground.entries.forEach { background ->
            ReaderOptionChip(background.label(), null, preferences.background == background) {
                actions.onUpdatePreferences { it.copy(background = background) }
            }
        }
    }
    ReaderSettingGroup(tankobunString(R.string.settings_screen_orientation)) {
        ReaderScreenOrientation.entries.forEach { orientation ->
            ReaderOptionChip(orientation.readerOrientationLabel(), null, state.readerScreenOrientation == orientation) {
                actions.onOrientationChange(orientation)
            }
        }
    }
    SettingsToggleRow(
        title = tankobunString(R.string.reader_keep_screen_on),
        subtitle = tankobunString(R.string.reader_keep_screen_on_desc),
        checked = preferences.keepScreenOn,
        onCheckedChange = { enabled -> actions.onUpdatePreferences { it.copy(keepScreenOn = enabled) } },
    )
    SettingsToggleRow(
        title = tankobunString(R.string.reader_volume_keys),
        subtitle = tankobunString(R.string.reader_volume_keys_desc),
        checked = preferences.volumeKeys,
        onCheckedChange = { enabled -> actions.onUpdatePreferences { it.copy(volumeKeys = enabled) } },
    )
    if (paged || showAllModes) {
        SettingsToggleRow(
            title = tankobunString(R.string.reader_chapter_end_page),
            subtitle = tankobunString(R.string.reader_chapter_end_page_desc),
            checked = preferences.chapterEndPage,
            onCheckedChange = { enabled -> actions.onUpdatePreferences { it.copy(chapterEndPage = enabled) } },
        )
    }
    if (!paged || showAllModes) {
        SettingsToggleRow(
            title = tankobunString(R.string.settings_webtoon_chapter_dividers),
            subtitle = tankobunString(R.string.settings_webtoon_chapter_dividers_desc),
            checked = state.showWebtoonChapterDividers,
            onCheckedChange = actions.onWebtoonDividersChange,
        )
    }
}

internal class ReaderSettingsActions(
    val onReaderModeChange: (ReaderMode) -> Unit,
    val onUpdatePreferences: ((ReaderPreferences) -> ReaderPreferences) -> Unit,
    val onPageGapChange: (Int) -> Unit,
    val onOrientationChange: (ReaderScreenOrientation) -> Unit,
    val onWebtoonDividersChange: (Boolean) -> Unit,
)

@Composable
internal fun rememberReaderSettingsActions(
    viewModel: MainViewModel,
    onReaderModeChange: (ReaderMode) -> Unit = viewModel::setReaderMode,
): ReaderSettingsActions {
    val latestModeChange by rememberUpdatedState(onReaderModeChange)
    return remember(viewModel) {
        ReaderSettingsActions(
            onReaderModeChange = { latestModeChange(it) },
            onUpdatePreferences = viewModel::updateReaderPreferences,
            onPageGapChange = viewModel::setReaderPageGapLevel,
            onOrientationChange = viewModel::setReaderScreenOrientation,
            onWebtoonDividersChange = viewModel::setShowWebtoonChapterDividers,
        )
    }
}

@Composable
private fun ReaderBackground.label(): String = when (this) {
    ReaderBackground.BLACK -> tankobunString(R.string.reader_background_black)
    ReaderBackground.GRAY -> tankobunString(R.string.reader_background_gray)
    ReaderBackground.WHITE -> tankobunString(R.string.reader_background_white)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderSettingGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun ReaderOptionChip(label: String, icon: ImageVector?, selected: Boolean, onClick: () -> Unit) {
    TankobunChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = icon?.let { { TankobunChipIcon(it) } },
    )
}
