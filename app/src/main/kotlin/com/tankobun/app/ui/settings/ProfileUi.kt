package com.tankobun.app.ui.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.tankobun.app.ui.components.TankobunPanel
import com.tankobun.app.ui.icons.TankobunIcons
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = chromeInsets.top + 16.dp,
            bottom = chromeInsets.bottom + 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (state.loggedIn) {
            val stats = state.anilistMangaStats ?: state.localMangaStats()
            item { ProfileHeaderCard(state = state) }
            item { ProfileStatisticsDashboard(stats = stats) }
        } else {
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
    }
}

private data class ProfileMetricSpec(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val emphasized: Boolean = false,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileStatisticsDashboard(stats: AnilistMangaStats) {
    val chaptersPerManga = if (stats.count > 0) stats.chaptersRead.toDouble() / stats.count else 0.0
    val metrics = listOf(
        ProfileMetricSpec(
            label = tankobunString(R.string.profile_metric_manga),
            value = stats.count.formatProfileNumber(),
            icon = TankobunIcons.LibraryBooks,
            emphasized = true,
        ),
        ProfileMetricSpec(
            label = tankobunString(R.string.profile_metric_chapters),
            value = stats.chaptersRead.formatProfileNumber(),
            icon = TankobunIcons.Hash,
        ),
        ProfileMetricSpec(
            label = tankobunString(R.string.profile_metric_volumes),
            value = stats.volumesRead.formatProfileNumber(),
            icon = TankobunIcons.BooksStack,
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

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = when {
                maxWidth >= 720.dp -> 5
                maxWidth >= 430.dp -> 3
                else -> 2
            }
            val gap = 10.dp
            val tileWidth = (maxWidth - gap * (columns - 1)) / columns
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                maxItemsInEachRow = columns,
            ) {
                metrics.forEach { metric ->
                    ProfileMetricTile(
                        metric = metric,
                        modifier = Modifier.width(tileWidth),
                    )
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 700.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    ProfileStatusPanel(
                        statuses = stats.statuses,
                        modifier = Modifier.weight(0.44f),
                    )
                    ProfileGenrePanel(
                        genres = stats.genres,
                        modifier = Modifier.weight(0.56f),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
        accent.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    }
    Surface(
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
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
                    modifier = Modifier.size(17.dp),
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
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDonut(
                    statuses = visible,
                    colors = colors,
                    total = total,
                    modifier = Modifier.size(116.dp),
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
            val gap = 3f
            statuses.forEachIndexed { index, item ->
                val rawSweep = 360f * item.count / total.toFloat()
                val sweep = (rawSweep - gap).coerceAtLeast(0f) * progress
                if (sweep > 0f) {
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
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
            modifier = Modifier.size(15.dp),
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
                imageVector = TankobunIcons.Category,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileTagsPanel(tags: List<AnilistStatItem>) {
    val visible = tags
        .filter { it.count > 0 && it.name.isNotBlank() }
        .sortedWith(compareByDescending<AnilistStatItem> { it.count }.thenByDescending { it.chaptersRead })
        .take(6)
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = TankobunIcons.Tag,
                contentDescription = null,
                tint = LocalTankobunStyle.current.colors.accent,
                modifier = Modifier.size(15.dp),
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
}

@Composable
private fun ProfileSectionPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TankobunPanel(
        modifier = modifier.fillMaxWidth(),
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                .padding(top = 1.dp)
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

private fun Int.formatProfileNumber(): String = String.format(Locale.getDefault(), "%,d", this)

private fun Double.formatProfileDecimal(): String {
    val rounded = (this * 10).roundToInt() / 10.0
    return if (rounded == rounded.roundToInt().toDouble()) {
        rounded.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", rounded)
    }
}
