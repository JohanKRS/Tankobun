package com.tankobun.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.tankobun.app.TankobunDisplayFontFamily
import com.tankobun.app.tankobunLocale
import com.tankobun.app.ui.components.shrinkToFit
import com.tankobun.app.ui.media.shortNumberLabel
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
// Stays dark until the title row ends, so the chapter line still reads over a white page.
private val ReaderTopScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.88f),
    0.7f to Color.Black.copy(alpha = 0.76f),
    1f to Color.Transparent,
)
private val ReaderBottomScrim = Brush.verticalGradient(
    0f to Color.Transparent,
    0.4f to Color.Black.copy(alpha = 0.7f),
    1f to Color.Black.copy(alpha = 0.9f),
)

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

/**
 * Title over a scrim that fades into the page, so controls read on any artwork without a box
 * cutting the page. [contentModifier] carries the window insets; the scrim runs under them.
 */
@Composable
internal fun ReaderTopBar(
    chapterName: String,
    mediaTitle: String?,
    onClose: () -> Unit,
    onOpenChapters: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().background(ReaderTopScrim)) {
        CompositionLocalProvider(LocalContentColor provides ReaderChromeContent) {
            Row(
                modifier = contentModifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(TankobunIcons.ArrowBack, contentDescription = tankobunString(R.string.reader_close))
                }
                // The work names the screen; the chapter is the detail under it.
                val title = mediaTitle?.takeIf { it.isNotBlank() }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        title ?: chapterName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (title != null) {
                        Text(
                            chapterName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ReaderChromeContent.copy(alpha = 0.72f),
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
}

/** The four settings readers change mid-chapter, one tap each, showing their current value. */
internal class ReaderQuickActions(
    val paged: Boolean,
    val rightToLeft: Boolean,
    val fitScreen: Boolean,
    val orientation: ReaderScreenOrientation,
    val onToggleMode: () -> Unit,
    val onToggleDirection: () -> Unit,
    val onToggleFit: () -> Unit,
    val onCycleOrientation: () -> Unit,
)

/**
 * Page scrubber with chapter jumps and quick settings. In right-to-left reading the scrubber
 * row mirrors, so it fills from the right, the current page sits on the right and "next
 * chapter" on the left, matching the page flow.
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
    quickActions: ReaderQuickActions,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val lastIndex = (pageCount - 1).coerceAtLeast(0)
    Box(modifier = modifier.fillMaxWidth().background(ReaderBottomScrim)) {
        CompositionLocalProvider(LocalContentColor provides ReaderChromeContent) {
            Column(
                modifier = contentModifier.padding(start = 10.dp, end = 10.dp, top = 48.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (zoomed) {
                    ReaderChromeSurface {
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
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (rightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ReaderChapterJumpButton(
                            icon = TankobunIcons.ChapterPrevious,
                            label = tankobunString(R.string.reader_previous_chapter),
                            enabled = hasPreviousChapter,
                            mirrored = rightToLeft,
                            onClick = onPreviousChapter,
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ReaderPageNumber((pageIndex + 1).coerceAtMost(pageCount.coerceAtLeast(1)), strong = true)
                            Box(modifier = Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                                if (pageCount > 1) {
                                    ReaderPageScrubber(
                                        value = scrubValue.coerceIn(0f, lastIndex.toFloat()),
                                        lastIndex = lastIndex,
                                        onScrub = onScrub,
                                        onScrubFinished = onScrubFinished,
                                    )
                                } else {
                                    // A single-page chapter is complete as soon as it is open.
                                    LinearProgressIndicator(
                                        progress = { 1f },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = ReaderChromeContent.copy(alpha = 0.22f),
                                        drawStopIndicator = {},
                                    )
                                }
                            }
                            ReaderPageNumber(pageCount.coerceAtLeast(1), strong = false)
                        }
                        ReaderChapterJumpButton(
                            icon = TankobunIcons.ChapterNext,
                            label = tankobunString(R.string.reader_next_chapter),
                            enabled = hasNextChapter,
                            mirrored = rightToLeft,
                            onClick = onNextChapter,
                        )
                    }
                }
                ReaderQuickActionRow(quickActions)
            }
        }
    }
}

@Composable
private fun ReaderPageNumber(number: Int, strong: Boolean) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            number.toString(),
            modifier = Modifier.widthIn(min = 22.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = ReaderChromeContent.copy(alpha = if (strong) 1f else 0.72f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderPageScrubber(
    value: Float,
    lastIndex: Int,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
) {
    val colors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = ReaderChromeContent.copy(alpha = 0.22f),
    )
    Slider(
        value = value,
        onValueChange = onScrub,
        onValueChangeFinished = onScrubFinished,
        valueRange = 0f..lastIndex.toFloat(),
        colors = colors,
        thumb = {
            // A slim bar with a dark ring, so it stays visible over a bright page.
            Box(
                Modifier
                    .size(width = 16.dp, height = 34.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 8.dp, height = 26.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                )
            }
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(6.dp),
                colors = colors,
                drawStopIndicator = null,
                thumbTrackGapSize = 0.dp,
                trackInsideCornerSize = 3.dp,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReaderChapterJumpButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    mirrored: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(ReaderChromeContent.copy(alpha = 0.08f)),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = ReaderChromeContent.copy(alpha = if (enabled) 1f else 0.34f),
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { scaleX = if (mirrored) -1f else 1f },
        )
    }
}

@Composable
private fun ReaderQuickActionRow(actions: ReaderQuickActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ReaderQuickAction(
            icon = if (actions.paged) TankobunIcons.MenuBook else TankobunIcons.ViewStream,
            label = if (actions.paged) ReaderMode.PAGED.readerModeLabel() else ReaderMode.WEBTOON.readerModeLabel(),
            onClick = actions.onToggleMode,
        )
        // Direction and fit only shape paged reading; webtoon always scrolls down at full width.
        ReaderQuickAction(
            icon = if (actions.rightToLeft) TankobunIcons.ReadingRightToLeft else TankobunIcons.ReadingLeftToRight,
            label = tankobunString(
                if (actions.rightToLeft) R.string.reader_direction_rtl_short else R.string.reader_direction_ltr_short,
            ),
            active = actions.rightToLeft && actions.paged,
            enabled = actions.paged,
            onClick = actions.onToggleDirection,
        )
        ReaderQuickAction(
            icon = if (actions.fitScreen) TankobunIcons.FitPage else TankobunIcons.FitWidth,
            label = tankobunString(if (actions.fitScreen) R.string.reader_fit_screen else R.string.reader_fit_width),
            enabled = actions.paged,
            onClick = actions.onToggleFit,
        )
        ReaderQuickAction(
            icon = when (actions.orientation) {
                ReaderScreenOrientation.PORTRAIT -> TankobunIcons.StayCurrentPortrait
                ReaderScreenOrientation.LANDSCAPE -> TankobunIcons.StayCurrentLandscape
                ReaderScreenOrientation.SYSTEM -> TankobunIcons.ScreenRotation
            },
            label = actions.orientation.readerOrientationLabel(),
            onClick = actions.onCycleOrientation,
        )
    }
}

@Composable
private fun RowScope.ReaderQuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 64.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f },
        shape = RoundedCornerShape(18.dp),
        color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else ReaderChromeContent,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            val labelStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 14.sp)
            Text(
                label,
                style = labelStyle,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** What the reader shows about the chapter that comes next: its cover, group and offline state. */
internal class ReaderNextChapterInfo(
    val chapter: SourceChapter,
    val coverUrl: String?,
    val group: String?,
    val downloaded: Boolean,
)

/** Paged-mode interstitial between chapters; swiping or tapping forward again continues. */
@Composable
internal fun ReaderChapterEndPage(
    chapter: SourceChapter,
    next: ReaderNextChapterInfo?,
    onNextChapter: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = LocalReaderInk.current
    val locale = tankobunLocale()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                tankobunString(R.string.reader_end_of_chapter).uppercase(locale),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                fontWeight = FontWeight.Bold,
                color = ink.copy(alpha = 0.6f),
            )
            Text(
                chapter.name,
                style = TextStyle(fontFamily = TankobunDisplayFontFamily, fontSize = 40.sp, lineHeight = 40.sp),
                color = ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(ink.copy(alpha = 0.16f)))
        if (next != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ReaderChapterCover(next.coverUrl, width = 64.dp, height = 96.dp, corner = 10.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        tankobunString(R.string.reader_up_next_label).uppercase(locale),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        next.chapter.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Medium,
                        color = ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ReaderNextChapterMeta(next, ink)
                }
            }
            Button(
                onClick = onNextChapter,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = CircleShape,
            ) {
                val number = next.chapter.shortNumberLabel()
                Text(
                    if (number != null) {
                        tankobunString(R.string.reader_read_chapter_number, number)
                    } else {
                        tankobunString(R.string.reader_read_next_chapter)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(8.dp))
                Icon(TankobunIcons.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(
                tankobunString(R.string.reader_end_continue_hint),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = ink.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    tankobunString(R.string.reader_chapter_end_caught_up),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                )
                Text(
                    tankobunString(R.string.reader_chapter_end_caught_up_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink.copy(alpha = 0.66f),
                )
            }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp), shape = CircleShape) {
                Text(tankobunString(R.string.reader_back_to_details), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ReaderChapterCover(url: String?, width: Dp, height: Dp, corner: Dp) {
    val ink = LocalReaderInk.current
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(corner))
            .background(ink.copy(alpha = 0.08f)),
    )
}

@Composable
private fun ReaderNextChapterMeta(next: ReaderNextChapterInfo, ink: Color) {
    if (!next.downloaded && next.group == null) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (next.downloaded) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ink.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(TankobunIcons.Downloaded, contentDescription = null, tint = ink.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                Text(tankobunString(R.string.common_downloaded), style = MaterialTheme.typography.labelMedium, color = ink.copy(alpha = 0.7f))
            }
        }
        next.group?.let { group ->
            Text(
                group,
                style = MaterialTheme.typography.bodyMedium,
                color = ink.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Compact webtoon transition between two chapters scrolling in one strip. */
@Composable
internal fun WebtoonChapterTransitionCard(previousChapter: SourceChapter, next: ReaderNextChapterInfo) {
    val ink = LocalReaderInk.current
    val locale = tankobunLocale()
    val finishedNumber = previousChapter.shortNumberLabel()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(ink.copy(alpha = 0.07f))
                .border(1.dp, ink.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReaderChapterCover(next.coverUrl, width = 36.dp, height = 54.dp, corner = 6.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val captionStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.8.sp)
                Text(
                    if (finishedNumber != null) {
                        tankobunString(R.string.reader_webtoon_after_chapter, finishedNumber)
                    } else {
                        tankobunString(R.string.reader_webtoon_up_next)
                    }.uppercase(locale),
                    style = captionStyle,
                    fontWeight = FontWeight.Bold,
                    color = ink.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = captionStyle.shrinkToFit(),
                )
                Text(
                    next.chapter.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Medium,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                next.group?.let { group ->
                    Text(
                        group,
                        style = MaterialTheme.typography.bodySmall,
                        color = ink.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (next.downloaded) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        TankobunIcons.Downloaded,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        tankobunString(R.string.common_downloaded),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                    )
                }
            }
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
@Composable
internal fun ColumnScope.ReaderSettingsControls(
    state: TankobunUiState,
    actions: ReaderSettingsActions,
    showAllModes: Boolean = false,
) {
    val onReaderModeChange = actions.onReaderModeChange
    val preferences = state.readerPreferences
    val paged = state.readerMode == ReaderMode.PAGED
    ReaderSegmentedSetting(
        title = tankobunString(R.string.settings_reading_mode),
        options = listOf(ReaderMode.PAGED, ReaderMode.WEBTOON),
        selected = state.readerMode,
        label = { it.readerModeLabel() },
        onSelect = onReaderModeChange,
    )
    if (paged || showAllModes) {
        ReaderSegmentedSetting(
            title = tankobunString(R.string.reader_direction),
            options = ReaderDirection.entries,
            selected = preferences.direction,
            label = {
                tankobunString(
                    if (it == ReaderDirection.RIGHT_TO_LEFT) R.string.reader_direction_rtl_short else R.string.reader_direction_ltr_short,
                )
            },
            description = {
                tankobunString(if (it == ReaderDirection.RIGHT_TO_LEFT) R.string.reader_direction_rtl else R.string.reader_direction_ltr)
            },
            onSelect = { direction -> actions.onUpdatePreferences { it.copy(direction = direction) } },
        )
        ReaderSegmentedSetting(
            title = tankobunString(R.string.reader_fit),
            options = listOf(ReaderFit.WIDTH, ReaderFit.SCREEN),
            selected = preferences.fit,
            label = { tankobunString(if (it == ReaderFit.SCREEN) R.string.reader_fit_screen_short else R.string.reader_fit_width_short) },
            description = { tankobunString(if (it == ReaderFit.SCREEN) R.string.reader_fit_screen else R.string.reader_fit_width) },
            onSelect = { fit -> actions.onUpdatePreferences { it.copy(fit = fit) } },
        )
    }
    ReaderSegmentedSetting(
        title = tankobunString(R.string.settings_page_gaps),
        options = (0..3).toList(),
        selected = state.readerPageGapLevel.coerceIn(0, 3),
        label = { level -> readerGapShortLabel(level) },
        description = { level -> readerGapLabel(level) },
        onSelect = actions.onPageGapChange,
    )
    ReaderSettingGroup(tankobunString(R.string.reader_background)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReaderBackground.entries.forEach { background ->
                ReaderBackgroundSwatch(background, selected = preferences.background == background) {
                    actions.onUpdatePreferences { it.copy(background = background) }
                }
            }
        }
    }
    ReaderSegmentedSetting(
        title = tankobunString(R.string.settings_screen_orientation),
        options = ReaderScreenOrientation.entries,
        selected = state.readerScreenOrientation,
        label = { it.readerOrientationLabel() },
        onSelect = actions.onOrientationChange,
    )
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
    // One switch for both modes: a full page when paged, a compact card between webtoon chapters.
    SettingsToggleRow(
        title = tankobunString(R.string.reader_chapter_end_page),
        subtitle = tankobunString(R.string.reader_chapter_end_page_desc),
        checked = preferences.chapterEndPage,
        onCheckedChange = { enabled -> actions.onUpdatePreferences { it.copy(chapterEndPage = enabled) } },
    )
}

internal class ReaderSettingsActions(
    val onReaderModeChange: (ReaderMode) -> Unit,
    val onUpdatePreferences: ((ReaderPreferences) -> ReaderPreferences) -> Unit,
    val onPageGapChange: (Int) -> Unit,
    val onOrientationChange: (ReaderScreenOrientation) -> Unit,
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
        )
    }
}

@Composable
private fun ReaderBackground.label(): String = when (this) {
    ReaderBackground.BLACK -> tankobunString(R.string.reader_background_black)
    ReaderBackground.GRAY -> tankobunString(R.string.reader_background_gray)
    ReaderBackground.WHITE -> tankobunString(R.string.reader_background_white)
}

@Composable
private fun ReaderSettingGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** A full-width segmented choice; the selected segment carries a check, as in the design. */
@Composable
private fun <T> ReaderSegmentedSetting(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    description: (@Composable (T) -> String)? = null,
) {
    ReaderSettingGroup(title) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                val spoken = description?.invoke(option)
                val labelStyle = MaterialTheme.typography.labelLarge
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    // Quarter-width segments ("Nenhum | P | M | G") have no room for the check;
                    // their fill alone marks the choice.
                    icon = {
                        if (options.size < 4) SegmentedButtonDefaults.Icon(active = option == selected)
                    },
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .then(if (spoken != null) Modifier.semantics { contentDescription = spoken } else Modifier),
                    label = {
                        Text(
                            label(option),
                            style = labelStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            autoSize = labelStyle.shrinkToFit(),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ReaderBackgroundSwatch(background: ReaderBackground, selected: Boolean, onClick: () -> Unit) {
    val label = background.label()
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                )
                .padding(if (selected) 3.dp else 1.dp)
                .background(background.color(), CircleShape),
        )
    }
}
