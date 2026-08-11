package com.tankobun.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.MainViewModel
import com.tankobun.app.R
import com.tankobun.app.statusLabel
import com.tankobun.app.tankobunQuantityString
import com.tankobun.app.tankobunString
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.state.LocalReadingActivity
import com.tankobun.app.state.LibraryItem
import com.tankobun.app.LibraryMode
import com.tankobun.app.ui.components.TankobunPanel
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.app.ui.icons.genreIcon
import com.tankobun.app.ui.library.LibraryConnectPrompt
import com.tankobun.app.ui.shell.LocalTankobunChromeInsets
import com.tankobun.core.model.AnilistMangaStats
import com.tankobun.core.model.AnilistStatItem
import com.tankobun.core.model.MediaStatus
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun ProfileScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
) {
    val chromeInsets = LocalTankobunChromeInsets.current
    val context = LocalContext.current
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.setCustomProfileAvatarUri(it.toString()) }
    }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.setCustomProfileBannerUri(it.toString()) }
    }
    LaunchedEffect(Unit) { viewModel.refreshLocalReadingActivity() }
    val stats = if (state.libraryMode == LibraryMode.ANILIST && state.anilistMangaStats != null) {
        state.anilistMangaStats
    } else {
        state.localMangaStats()
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = chromeInsets.top + 16.dp,
            bottom = chromeInsets.bottom + 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            ProfileHeaderCard(
                state = state,
                onEditAvatar = { avatarPicker.launch(arrayOf("image/*")) },
                onEditBanner = { bannerPicker.launch(arrayOf("image/*")) },
                onClearAvatar = { viewModel.setCustomProfileAvatarUri(null) },
                onClearBanner = { viewModel.setCustomProfileBannerUri(null) },
            )
        }
        if (!state.loggedIn) {
            item {
                LibraryConnectPrompt(
                    clientConfigured = state.clientConfigured,
                    onConnect = {
                        viewModel.loginUrl()?.let { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                )
            }
        }
        item {
            ProfileStatisticsDashboard(
                stats = stats,
                activity = state.localReadingActivity,
                libraryItems = state.libraryItems,
            )
        }
    }
}

private data class ProfileMetricSpec(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val secondary: String? = null,
    val emphasized: Boolean = false,
)

private data class ProfileReadingOverview(
    val mangaRead: Int,
    val mangaTotal: Int,
    val mangaRemaining: Int,
    val chaptersRead: Int,
    val chaptersKnownTotal: Int?,
    val chaptersKnownRemaining: Int?,
    val volumesRead: Int,
    val volumesKnownTotal: Int?,
    val volumesKnownRemaining: Int?,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileStatisticsDashboard(
    stats: AnilistMangaStats,
    activity: LocalReadingActivity,
    libraryItems: List<LibraryItem>,
) {
    val overview = profileReadingOverview(stats = stats, libraryItems = libraryItems)
    val chaptersPerManga = if (stats.count > 0) stats.chaptersRead.toDouble() / stats.count else 0.0
    val metrics = listOf(
        ProfileMetricSpec(
            label = tankobunString(R.string.profile_metric_manga_read),
            value = overview.mangaRead.formatProfileNumber(),
            icon = TankobunIcons.LibraryBooks,
            secondary = tankobunString(
                R.string.profile_metric_total_remaining,
                overview.mangaTotal.formatProfileNumber(),
                overview.mangaRemaining.formatProfileNumber(),
            ),
            emphasized = true,
        ),
        ProfileMetricSpec(
            label = tankobunString(R.string.profile_metric_chapters_read),
            value = overview.chaptersRead.formatProfileNumber(),
            icon = TankobunIcons.Hash,
            secondary = knownTotalSummary(overview.chaptersKnownTotal, overview.chaptersKnownRemaining),
        ),
        ProfileMetricSpec(
            label = tankobunString(R.string.profile_metric_volumes_read),
            value = overview.volumesRead.formatProfileNumber(),
            icon = TankobunIcons.BooksStack,
            secondary = knownTotalSummary(overview.volumesKnownTotal, overview.volumesKnownRemaining),
        ),
        ProfileMetricSpec(
            label = tankobunString(R.string.profile_metric_mean_score),
            value = stats.meanScore?.formatProfileDecimal() ?: tankobunString(R.string.common_unknown),
            icon = TankobunIcons.Star,
        ),
        ProfileMetricSpec(
            label = tankobunString(R.string.profile_metric_chapters_per_manga),
            value = chaptersPerManga.formatProfileDecimal(),
            icon = TankobunIcons.ChartBar,
        ),
    )

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = when {
                maxWidth >= 720.dp -> 5
                maxWidth >= 430.dp -> 3
                else -> 2
            }
            val gap = 12.dp
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                metrics.chunked(columns).forEach { rowMetrics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        rowMetrics.forEach { metric ->
                            ProfileMetricTile(
                                metric = metric,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - rowMetrics.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        ProfileLocalActivitySections(
            activity = activity,
            chaptersRead = maxOf(activity.chaptersTracked, stats.chaptersRead),
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 700.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    ProfileStatusPanel(
                        statuses = stats.statuses,
                        modifier = Modifier
                            .weight(0.44f)
                            .fillMaxHeight(),
                    )
                    ProfileGenrePanel(
                        genres = stats.genres,
                        modifier = Modifier
                            .weight(0.56f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ProfileStatusPanel(statuses = stats.statuses)
                    ProfileGenrePanel(genres = stats.genres)
                }
            }
        }

        ProfileTagsPanel(tags = stats.tags)
    }
}

@Composable
private fun ProfileMetricTile(
    metric: ProfileMetricSpec,
    modifier: Modifier = Modifier,
) {
    val accent = LocalTankobunStyle.current.colors.accent
    val background = if (metric.emphasized) {
        accent.copy(alpha = 0.13f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
    }
    Surface(
        modifier = modifier.height(96.dp),
        shape = LocalTankobunStyle.current.themeShapes.control,
        color = background,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    imageVector = metric.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .offset(y = (-1).dp)
                        .size(17.dp),
                )
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            metric.secondary?.let { secondary ->
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ProfileStatusPanel(
    statuses: List<AnilistStatItem>,
    modifier: Modifier = Modifier,
) {
    val visible = statuses
        .filter { it.count > 0 && it.name.isNotBlank() }
        .sortedByDescending { it.count }
    val total = visible.sumOf { it.count }
    val colors = listOf(
        LocalTankobunStyle.current.colors.accent,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.outline,
        MaterialTheme.colorScheme.error,
    )
    ProfileSectionPanel(modifier = modifier) {
        ProfileSectionHeading(
            icon = TankobunIcons.ChartDonut,
            title = tankobunString(R.string.profile_statuses),
            subtitle = tankobunString(R.string.profile_status_distribution_desc),
        )
        if (visible.isEmpty() || total <= 0) {
            ProfileEmptyText()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDonut(
                    statuses = visible,
                    colors = colors,
                    total = total,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(176.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    visible.take(6).forEachIndexed { index, item ->
                        StatusLegendRow(
                            item = item,
                            color = colors[index % colors.size],
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDonut(
    statuses: List<AnilistStatItem>,
    colors: List<Color>,
    total: Int,
    modifier: Modifier = Modifier,
) {
    var started by remember(statuses) { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 850),
        label = "Profile status donut",
    )
    LaunchedEffect(statuses) { started = true }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.12f
            drawCircle(
                color = track,
                style = Stroke(width = strokeWidth),
            )
            var startAngle = -90f
            val gap = 2f
            statuses.forEachIndexed { index, item ->
                val rawSweep = 360f * item.count / total.toFloat()
                val sweep = (rawSweep - gap).coerceAtLeast(0f) * progress
                if (sweep > 0f) {
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    )
                }
                startAngle += rawSweep * progress
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = total.formatProfileNumber(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = tankobunString(R.string.profile_status_entries_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusLegendRow(item: AnilistStatItem, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            imageVector = statusIcon(item.name),
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .offset(y = (-1).dp)
                .size(15.dp),
        )
        Text(
            text = profileStatusName(item.name),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.count.formatProfileNumber(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ProfileGenrePanel(
    genres: List<AnilistStatItem>,
    modifier: Modifier = Modifier,
) {
    val visible = genres
        .filter { it.count > 0 && it.name.isNotBlank() }
        .sortedWith(compareByDescending<AnilistStatItem> { it.count }.thenByDescending { it.chaptersRead })
        .take(5)
    val maxChapters = visible.maxOfOrNull { it.chaptersRead }?.coerceAtLeast(1) ?: 1
    ProfileSectionPanel(modifier = modifier) {
        ProfileSectionHeading(
            icon = TankobunIcons.Category,
            title = tankobunString(R.string.profile_genre_signature),
            subtitle = tankobunString(R.string.profile_genre_signature_desc),
        )
        if (visible.isEmpty()) {
            ProfileEmptyText()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                visible.forEach { item ->
                    ProfileGenreRow(item = item, maxChapters = maxChapters)
                }
            }
        }
    }
}

@Composable
private fun ProfileGenreRow(item: AnilistStatItem, maxChapters: Int) {
    var started by remember(item) { mutableStateOf(false) }
    val fraction = item.chaptersRead.toFloat() / maxChapters
    val animatedFraction by animateFloatAsState(
        targetValue = if (started) fraction.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "Genre depth ${item.name}",
    )
    LaunchedEffect(item) { started = true }
    val accent = LocalTankobunStyle.current.colors.accent
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = genreIcon(item.name),
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .offset(y = (-1).dp)
                    .size(14.dp),
            )
            Text(
                text = item.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tankobunString(R.string.profile_stat_detail, item.count, item.chaptersRead),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(99.dp),
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(4.dp)
                    .background(accent, RoundedCornerShape(99.dp)),
            )
        }
    }
}

private data class ActivityMetricSpec(
    val label: String,
    val value: String,
    val icon: ImageVector,
)

private data class AchievementSpec(
    val label: String,
    val unlocked: Boolean,
)

@Composable
private fun ProfileLocalActivitySections(
    activity: LocalReadingActivity,
    chaptersRead: Int,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 700.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ProfileActivityPanel(
                    activity = activity,
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight(),
                )
                ProfileAchievementsPanel(
                    activity = activity,
                    chaptersRead = chaptersRead,
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight(),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ProfileActivityPanel(activity = activity)
                ProfileAchievementsPanel(activity = activity, chaptersRead = chaptersRead)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileActivityPanel(
    activity: LocalReadingActivity,
    modifier: Modifier = Modifier,
) {
    val metrics = listOf(
        ActivityMetricSpec(
            label = tankobunString(R.string.profile_activity_today),
            value = activity.chaptersToday.formatProfileNumber(),
            icon = TankobunIcons.Bolt,
        ),
        ActivityMetricSpec(
            label = tankobunString(R.string.profile_activity_7_days),
            value = activity.chaptersLast7Days.formatProfileNumber(),
            icon = TankobunIcons.CalendarMonth,
        ),
        ActivityMetricSpec(
            label = tankobunString(R.string.profile_activity_30_days),
            value = activity.chaptersLast30Days.formatProfileNumber(),
            icon = TankobunIcons.CalendarStats,
        ),
        ActivityMetricSpec(
            label = tankobunString(R.string.profile_activity_average),
            value = activity.averagePerActiveDay30.formatProfileDecimal(),
            icon = TankobunIcons.ChartBar,
        ),
    )
    ProfileSectionPanel(modifier = modifier) {
        ProfileSectionHeading(
            icon = TankobunIcons.Activity,
            title = tankobunString(R.string.profile_activity_title),
            subtitle = tankobunString(R.string.profile_activity_desc),
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 480.dp) 4 else 2
            val gap = 8.dp
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                metrics.chunked(columns).forEach { rowMetrics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        rowMetrics.forEach { metric ->
                            ActivityMetricTile(metric = metric, modifier = Modifier.weight(1f))
                        }
                        repeat(columns - rowMetrics.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Text(
            text = tankobunString(R.string.profile_activity_last_14_days),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ProfileActivityBars(counts = activity.last14Days)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActivityStreakTile(
                label = tankobunString(R.string.profile_current_streak),
                value = tankobunQuantityString(
                    R.plurals.profile_days_value,
                    activity.currentStreakDays,
                    activity.currentStreakDays,
                ),
                icon = TankobunIcons.Whatshot,
                modifier = Modifier.weight(1f),
            )
            ActivityStreakTile(
                label = tankobunString(R.string.profile_best_streak),
                value = tankobunQuantityString(
                    R.plurals.profile_days_value,
                    activity.longestStreakDays,
                    activity.longestStreakDays,
                ),
                icon = TankobunIcons.Trophy,
                modifier = Modifier.weight(1f),
            )
            ActivityStreakTile(
                label = tankobunString(R.string.profile_reading_days),
                value = tankobunQuantityString(
                    R.plurals.profile_days_value,
                    activity.totalReadingDays,
                    activity.totalReadingDays,
                ),
                icon = TankobunIcons.CalendarStats,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActivityMetricTile(metric: ActivityMetricSpec, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(68.dp)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = metric.icon,
                contentDescription = null,
                tint = LocalTankobunStyle.current.colors.accent,
                modifier = Modifier
                    .offset(y = (-1).dp)
                    .size(16.dp),
            )
            Text(
                text = metric.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = metric.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfileActivityBars(counts: List<Int>) {
    val normalized = counts.takeLast(14).let { values -> List(14 - values.size) { 0 } + values }
    val max = normalized.maxOrNull()?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        normalized.forEachIndexed { index, count ->
            var started by remember(index, normalized) { mutableStateOf(false) }
            val target = count.toFloat() / max
            val progress by animateFloatAsState(
                targetValue = if (started) target else 0f,
                animationSpec = tween(durationMillis = 600, delayMillis = index * 24),
                label = "Reading activity day $index",
            )
            LaunchedEffect(index, normalized) { started = true }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((0.08f + progress * 0.92f).coerceIn(0f, 1f))
                        .background(
                            color = if (count > 0) {
                                LocalTankobunStyle.current.colors.accent
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun ActivityStreakTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalTankobunStyle.current.colors.accent,
                modifier = Modifier
                    .offset(y = (-1).dp)
                    .size(16.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileAchievementsPanel(
    activity: LocalReadingActivity,
    chaptersRead: Int,
    modifier: Modifier = Modifier,
) {
    val chapterAchievements = listOf(
        AchievementSpec(tankobunString(R.string.profile_achievement_first), chaptersRead >= 1),
        AchievementSpec(tankobunString(R.string.profile_achievement_chapters_100), chaptersRead >= 100),
        AchievementSpec(tankobunString(R.string.profile_achievement_chapters_500), chaptersRead >= 500),
        AchievementSpec(tankobunString(R.string.profile_achievement_chapters_5000), chaptersRead >= 5_000),
        AchievementSpec(tankobunString(R.string.profile_achievement_chapters_10000), chaptersRead >= 10_000),
    )
    val streakAchievements = listOf(
        AchievementSpec(tankobunString(R.string.profile_achievement_streak_3), activity.longestStreakDays >= 3),
        AchievementSpec(tankobunString(R.string.profile_achievement_streak_7), activity.longestStreakDays >= 7),
        AchievementSpec(tankobunString(R.string.profile_achievement_streak_30), activity.longestStreakDays >= 30),
        AchievementSpec(tankobunString(R.string.profile_achievement_streak_180), activity.longestStreakDays >= 180),
        AchievementSpec(tankobunString(R.string.profile_achievement_streak_365), activity.longestStreakDays >= 365),
    )
    ProfileSectionPanel(modifier = modifier) {
        ProfileSectionHeading(
            icon = TankobunIcons.Trophy,
            title = tankobunString(R.string.profile_achievements),
            subtitle = tankobunString(R.string.profile_achievements_desc),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AchievementColumn(
                title = tankobunString(R.string.profile_achievement_chapter_column),
                icon = TankobunIcons.Hash,
                achievements = chapterAchievements,
                modifier = Modifier.weight(1f),
            )
            AchievementColumn(
                title = tankobunString(R.string.profile_achievement_streak_column),
                icon = TankobunIcons.Whatshot,
                achievements = streakAchievements,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AchievementColumn(
    title: String,
    icon: ImageVector,
    achievements: List<AchievementSpec>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalTankobunStyle.current.colors.accent,
                modifier = Modifier
                    .offset(y = (-1).dp)
                    .size(15.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        achievements.forEach { achievement ->
            AchievementTile(achievement = achievement)
        }
    }
}

@Composable
private fun AchievementTile(
    achievement: AchievementSpec,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(
            imageVector = if (achievement.unlocked) TankobunIcons.Award else TankobunIcons.Lock,
            contentDescription = null,
            tint = if (achievement.unlocked) LocalTankobunStyle.current.colors.accent else LocalTankobunStyle.current.colors.mutedContent,
            modifier = Modifier
                .offset(y = (-1).dp)
                .size(18.dp),
        )
        Text(
            text = achievement.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (achievement.unlocked) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
            },
            fontWeight = if (achievement.unlocked) FontWeight.Bold else FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileTagsPanel(tags: List<AnilistStatItem>) {
    val visible = tags
        .filter { it.count > 0 && it.name.isNotBlank() }
        .sortedWith(compareByDescending<AnilistStatItem> { it.count }.thenByDescending { it.chaptersRead })
        .take(10)
    ProfileSectionPanel {
        ProfileSectionHeading(
            icon = TankobunIcons.Tag,
            title = tankobunString(R.string.profile_tag_affinities),
            subtitle = tankobunString(R.string.profile_tag_affinities_desc),
        )
        if (visible.isEmpty()) {
            ProfileEmptyText()
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 640.dp) 3 else if (maxWidth >= 380.dp) 2 else 1
                val gap = 8.dp
                val itemWidth = (maxWidth - gap * (columns - 1)) / columns
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = columns,
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    visible.forEach { item ->
                        ProfileTagTile(item = item, modifier = Modifier.width(itemWidth))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTagTile(item: AnilistStatItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = TankobunIcons.Tag,
            contentDescription = null,
            tint = LocalTankobunStyle.current.colors.accent,
            modifier = Modifier
                .offset(y = (-1).dp)
                .size(16.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tankobunString(R.string.profile_stat_detail, item.count, item.chaptersRead),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProfileSectionPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TankobunPanel(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        contentColor = LocalTankobunStyle.current.colors.panelContent,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ProfileSectionHeading(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LocalTankobunStyle.current.colors.accent,
            modifier = Modifier
                .offset(y = (-1).dp)
                .size(18.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = LocalTankobunStyle.current.typography.sectionLabel,
                color = LocalTankobunStyle.current.colors.accent,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileEmptyText() {
    Text(
        text = tankobunString(R.string.profile_stats_empty),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun profileStatusName(name: String): String =
    runCatching { MediaStatus.valueOf(name) }
        .getOrNull()
        ?.statusLabel()
        ?: name

private fun statusIcon(name: String): ImageVector =
    when (name.uppercase(Locale.ROOT)) {
        "COMPLETED" -> TankobunIcons.Check
        "CURRENT", "READING" -> TankobunIcons.MenuBook
        "PLANNING", "PLAN_TO_READ" -> TankobunIcons.CalendarMonth
        "PAUSED" -> TankobunIcons.Pause
        "DROPPED" -> TankobunIcons.Close
        else -> TankobunIcons.Category
    }

private fun profileReadingOverview(
    stats: AnilistMangaStats,
    libraryItems: List<LibraryItem>,
): ProfileReadingOverview {
    val mangaTotal = maxOf(stats.count, libraryItems.size)
    val mangaRead = if (libraryItems.isNotEmpty()) {
        libraryItems.count { item ->
            item.entry.status == MediaStatus.COMPLETED || item.entry.status == MediaStatus.REPEATING
        }
    } else {
        stats.statuses
            .filter { item -> item.name.equals(MediaStatus.COMPLETED.name, ignoreCase = true) }
            .sumOf { it.count }
    }
    val chaptersWithKnownTotal = libraryItems.filter { item -> item.media.chapters != null }
    val chaptersKnownTotal = chaptersWithKnownTotal
        .sumOf { item -> item.media.chapters?.coerceAtLeast(0) ?: 0 }
        .takeIf { chaptersWithKnownTotal.isNotEmpty() }
    val chaptersKnownRemaining = chaptersKnownTotal?.let {
        chaptersWithKnownTotal.sumOf { item ->
            ((item.media.chapters ?: 0) - item.entry.progress.coerceAtLeast(0)).coerceAtLeast(0)
        }
    }
    val volumesWithKnownTotal = libraryItems.filter { item -> item.media.volumes != null }
    val volumesKnownTotal = volumesWithKnownTotal
        .sumOf { item -> item.media.volumes?.coerceAtLeast(0) ?: 0 }
        .takeIf { volumesWithKnownTotal.isNotEmpty() }
    val volumesKnownRemaining = volumesKnownTotal?.let { total ->
        (total - stats.volumesRead.coerceAtLeast(0)).coerceAtLeast(0)
    }
    return ProfileReadingOverview(
        mangaRead = mangaRead,
        mangaTotal = mangaTotal,
        mangaRemaining = (mangaTotal - mangaRead).coerceAtLeast(0),
        chaptersRead = stats.chaptersRead.coerceAtLeast(0),
        chaptersKnownTotal = chaptersKnownTotal,
        chaptersKnownRemaining = chaptersKnownRemaining,
        volumesRead = stats.volumesRead.coerceAtLeast(0),
        volumesKnownTotal = volumesKnownTotal,
        volumesKnownRemaining = volumesKnownRemaining,
    )
}

@Composable
private fun knownTotalSummary(total: Int?, remaining: Int?): String =
    if (total == null || remaining == null) {
        tankobunString(R.string.profile_metric_known_total_unknown)
    } else {
        tankobunString(
            R.string.profile_metric_known_total_remaining,
            total.formatProfileNumber(),
            remaining.formatProfileNumber(),
        )
    }

private fun Int.formatProfileNumber(): String = String.format(Locale.getDefault(), "%,d", this)

private fun Double.formatProfileDecimal(): String {
    val rounded = (this * 10).roundToInt() / 10.0
    return if (rounded == rounded.roundToInt().toDouble()) {
        rounded.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", rounded)
    }
}
