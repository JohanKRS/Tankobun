package com.tankobun.app.ui.settings

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
internal fun DownloadsSettingsScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<PendingDownloadDelete?>(null) }
    LaunchedEffect(Unit) {
        viewModel.refreshCacheStorageSummary()
    }
    SettingsDetailPanel(
        title = tankobunString(R.string.common_downloads),
        subtitle = tankobunString(R.string.settings_downloads_subtitle),
        modifier = modifier,
    ) {
        val summary = state.downloadStorageSummary
        TankobunPanel(
            modifier = Modifier.fillMaxWidth(),
            color = LocalTankobunStyle.current.colors.panel,
            contentColor = LocalTankobunStyle.current.colors.panelContent,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tankobunString(R.string.downloads_local_storage), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        tankobunString(
                            R.string.downloads_storage_summary,
                            summary.items.size,
                            summary.items.sumOf { it.chapterCount },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    summary.totalBytes.formatFileSize(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        CacheStoragePanel(
            summary = state.cacheStorageSummary,
            onClearAnilistImages = viewModel::clearAnilistAndImageCache,
            onClearSourceNetwork = viewModel::clearSourceNetworkCache,
            onClearReaderPages = viewModel::clearReaderPageCache,
            onClearTemporary = viewModel::clearTemporaryCache,
            onClearAll = viewModel::clearAllCaches,
        )

        if (summary.items.isEmpty()) {
            Text(tankobunString(R.string.downloads_no_downloaded_chapters), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val allDownloadsLabel = tankobunString(R.string.downloads_all_downloads)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    tankobunString(R.string.downloads_by_manga_source),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedButton(
                    onClick = {
                        pendingDelete = PendingDownloadDelete(
                            mediaId = null,
                            title = allDownloadsLabel,
                            detail = summary.totalBytes.formatFileSize(),
                        )
                    },
                    shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tankobunString(R.string.downloads_delete_all))
                }
            }
            summary.items.forEach { item ->
                val title = state.downloadedMediaTitle(item.mediaId)
                val sourceLabel = state.downloadedSourceLabel(item.sourceId)
                val detail = "${item.bytes.formatFileSize()} / ${item.downloadStorageDetailLine()}"
                DownloadStorageRow(
                    title = title,
                    sourceLabel = sourceLabel,
                    item = item,
                    onDelete = {
                        pendingDelete = PendingDownloadDelete(
                            mediaId = item.mediaId,
                            sourceId = item.sourceId,
                            title = title,
                            sourceLabel = sourceLabel,
                            detail = detail,
                        )
                    },
                )
            }
        }
    }
    pendingDelete?.let { target ->
        DeleteDownloadsDialog(
            target = target,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                if (target.mediaId == null) {
                    viewModel.removeAllDownloads()
                } else {
                    target.sourceId?.let { sourceId ->
                        viewModel.removeDownloadsForMediaSource(target.mediaId, sourceId)
                    } ?: viewModel.removeDownloadsForMedia(target.mediaId)
                }
                pendingDelete = null
            },
        )
    }
}

@Composable
internal fun CacheStoragePanel(
    summary: CacheStorageSummary,
    onClearAnilistImages: () -> Unit,
    onClearSourceNetwork: () -> Unit,
    onClearReaderPages: () -> Unit,
    onClearTemporary: () -> Unit,
    onClearAll: () -> Unit,
) {
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tankobunString(R.string.cache_storage_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        tankobunString(R.string.cache_storage_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    summary.totalBytes.formatFileSize(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            CacheStorageRow(
                label = tankobunString(R.string.cache_storage_anilist_images),
                bytes = summary.anilistAndImageBytes,
                onClear = onClearAnilistImages,
            )
            CacheStorageRow(
                label = tankobunString(R.string.cache_storage_source_network),
                bytes = summary.sourceNetworkBytes,
                onClear = onClearSourceNetwork,
            )
            CacheStorageRow(
                label = tankobunString(R.string.cache_storage_reader_pages),
                bytes = summary.readerPageBytes,
                onClear = onClearReaderPages,
            )
            CacheStorageRow(
                label = tankobunString(R.string.cache_storage_temporary),
                bytes = summary.temporaryBytes,
                onClear = onClearTemporary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TankobunActionButton(
                    label = tankobunString(R.string.cache_clear_all),
                    icon = Icons.Default.Delete,
                    onClick = onClearAll,
                    enabled = summary.totalBytes > 0L,
                    filled = false,
                )
            }
        }
    }
}

@Composable
private fun CacheStorageRow(
    label: String,
    bytes: Long,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                bytes.formatFileSize(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = onClear,
            enabled = bytes > 0L,
        ) {
            Text(tankobunString(R.string.common_clear))
        }
    }
}

internal data class PendingDownloadDelete(
    val mediaId: Int?,
    val sourceId: Long? = null,
    val title: String,
    val sourceLabel: String? = null,
    val detail: String,
)

@Composable
internal fun DeleteDownloadsDialog(
    target: PendingDownloadDelete,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    TankobunDialog(onDismiss = onDismiss, maxWidth = 520.dp, maxHeight = 520.dp) {
        TankobunDialogHeader(title = tankobunString(R.string.downloads_delete_title), onDismiss = onDismiss)
        Text(
            buildList {
                add(target.title)
                target.sourceLabel?.let { add(it) }
                add(target.detail)
            }.joinToString(" / "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text(tankobunString(R.string.common_cancel))
            }
            TankobunActionButton(
                label = tankobunString(R.string.common_delete),
                icon = Icons.Default.Delete,
                onClick = onConfirm,
            )
        }
    }
}

@Composable
internal fun DownloadStorageRow(
    title: String,
    sourceLabel: String,
    item: DownloadStorageItem,
    onDelete: () -> Unit,
) {
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    sourceLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalTankobunStyle.current.colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.downloadStorageDetailLine(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                item.bytes.formatFileSize(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = tankobunString(R.string.downloads_delete_cd, title, sourceLabel),
                )
            }
        }
    }
}

@Composable
internal fun SettingsDetailPanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val chromeInsets = LocalTankobunChromeInsets.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = chromeInsets.top + 20.dp, bottom = chromeInsets.bottom + 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = LocalTankobunStyle.current.typography.sectionLabel,
                color = LocalTankobunStyle.current.colors.accent,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
internal fun CutoutLayoutToggle(
    ignoreDisplayCutout: Boolean,
    onIgnoreDisplayCutoutChange: (Boolean) -> Unit,
) {
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIgnoreDisplayCutoutChange(!ignoreDisplayCutout) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tankobunString(R.string.settings_ignore_camera_cutout), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    tankobunString(R.string.settings_ignore_camera_cutout_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = ignoreDisplayCutout,
                onCheckedChange = onIgnoreDisplayCutoutChange,
            )
        }
    }
}

@Composable
internal fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        }
    }
}

@Composable
internal fun SettingsRoute.settingsSummary(state: TankobunUiState): String =
    when (this) {
        SettingsRoute.MAIN -> tankobunString(R.string.common_settings)
        SettingsRoute.PROFILE -> buildList {
            add(
                state.viewerName?.let { tankobunString(R.string.settings_signed_in_as, it) }
                    ?: tankobunString(R.string.settings_profile_local),
            )
            add(
                if (state.showNsfwContent) {
                    tankobunString(R.string.settings_nsfw_visible)
                } else {
                    tankobunString(R.string.settings_nsfw_hidden)
                },
            )
        }.joinToString(" / ")
        SettingsRoute.APPEARANCE -> buildList {
            add(tankobunThemeChoices().firstOrNull { it.mode == state.themeMode }?.name ?: "Neon Koi")
            add(tankobunString(R.string.dock_summary, state.dockAlignment.settingsLabel()))
        }.joinToString(" / ")
        SettingsRoute.LANGUAGES -> state.sourceLanguages.count { it != UNIVERSAL_SOURCE_LANGUAGE }.let { count ->
            tankobunQuantityString(R.plurals.source_language_count, count, count)
        }
        SettingsRoute.LIBRARY -> buildList {
            add(
                state.libraryViewMode.mediaViewSettingsSummary(
                    columns = state.libraryCoverColumns,
                    showWholeCovers = state.libraryShowWholeCovers,
                ),
            )
            if (state.newChapterChecksEnabled) add(tankobunString(R.string.settings_daily_chapter_check_short))
        }.joinToString(" / ")
        SettingsRoute.BROWSE -> state.browseViewMode.mediaViewSettingsSummary(
            columns = state.browseCoverColumns,
            showWholeCovers = state.browseShowWholeCovers,
        )
        SettingsRoute.READER -> buildList {
            add(state.readerMode.readerModeLabel())
            add(readerGapLabel(state.readerPageGapLevel))
        }.joinToString(" / ")
        SettingsRoute.DOWNLOADS -> state.downloadStorageSummary.totalBytes.formatFileSize()
        SettingsRoute.ANILIST -> buildList {
            add(
                state.viewerName?.let { tankobunString(R.string.settings_signed_in_as, it) }
                    ?: if (state.clientConfigured) {
                        tankobunString(R.string.settings_anilist_ready)
                    } else {
                        tankobunString(R.string.settings_anilist_client_setup_needed)
                    },
            )
            add(state.anilistTitleLanguage.settingsLabel())
            add(state.anilistScoreFormat.settingsLabel())
            if (state.anilistAutoSaveTrackingChanges) add(tankobunString(R.string.settings_auto_save_tracking_edits))
            if (state.anilistAutoSyncReaderProgress) add(tankobunString(R.string.settings_update_progress_from_reading))
        }.joinToString(" / ")
        SettingsRoute.CUSTOM_LISTS -> tankobunQuantityString(
            R.plurals.list_count,
            state.anilistCustomLists.size,
            state.anilistCustomLists.size,
        )
        SettingsRoute.BACKUPS -> tankobunString(
            R.string.backup_mal_matched_summary,
            state.libraryItems.count { it.media.idMal != null },
            state.libraryItems.size,
        )
        SettingsRoute.ABOUT -> tankobunString(R.string.about_summary)
        SettingsRoute.SOURCES -> tankobunString(
            R.string.sources_active_installed_count,
            state.installedSources.size,
            state.visibleInstalledSourceCount(),
        )
    }

private fun TankobunUiState.visibleInstalledSourceCount(): Int =
    allInstalledSources.count { source ->
        val language = source.lang.normalizedSourceLanguage()
        language in sourceLanguages || language == UNIVERSAL_SOURCE_LANGUAGE
    }

@Composable
internal fun backupCoverageLabel(totalItems: Int, malMatchedItems: Int, missingMalItems: Int): String =
    when {
        totalItems == 0 -> tankobunString(R.string.backup_no_cached_manga)
        missingMalItems == 0 -> tankobunString(R.string.backup_ready_for_mal, totalItems)
        else -> tankobunString(R.string.backup_missing_mal, malMatchedItems, missingMalItems)
    }

internal fun suggestedAniListBackupFileName(viewerName: String?): String {
    val userPart = viewerName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
        ?: "user"
    return "tankobun_anilist_backup_${userPart}_${System.currentTimeMillis()}.xml"
}

internal fun suggestedAppSettingsBackupFileName(): String =
    "tankobun_settings_${System.currentTimeMillis()}.json"

@Composable
internal fun TankobunUiState.downloadedMediaTitle(mediaId: Int): String =
    libraryItems.firstOrNull { it.media.id == mediaId }?.media?.title?.userPreferred
        ?: library.firstOrNull { it.id == mediaId }?.title?.userPreferred
        ?: selectedMedia?.takeIf { it.id == mediaId }?.title?.userPreferred
        ?: tankobunString(R.string.sources_manga_fallback, mediaId)

@Composable
internal fun TankobunUiState.downloadedSourceLabel(sourceId: Long): String {
    val source = installedSources.firstOrNull { it.id == sourceId }
        ?: allInstalledSources.firstOrNull { it.id == sourceId }
        ?: selectedSource?.takeIf { it.id == sourceId }
    return source?.let {
        "${it.name.extensionDisplayName()} / ${sourceLanguageDisplay(it.lang)}"
    } ?: tankobunString(R.string.sources_source_fallback, sourceId)
}

@Composable
internal fun DownloadStorageItem.downloadStorageDetailLine(): String =
    buildList {
        add(tankobunQuantityString(R.plurals.chapter_count, chapterCount, chapterCount))
        if (completedChapterCount > 0) {
            add(tankobunString(R.string.downloads_storage_detail_complete, completedChapterCount))
        }
        if (activeChapterCount > 0) {
            add(tankobunString(R.string.downloads_storage_detail_active, activeChapterCount))
        }
        if (pageCount > 0) {
            add(tankobunQuantityString(R.plurals.page_count, pageCount, pageCount))
        }
    }.joinToString(" / ")

internal fun Long.formatFileSize(): String {
    if (this < 1024L) return "$this B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = toDouble() / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    return "%.1f %s".format(Locale.US, value, units[unitIndex])
}

@Composable
internal fun MediaViewMode.mediaViewSettingsSummary(columns: Int, showWholeCovers: Boolean): String {
    val supportedMode = supportedMediaViewMode()
    return if (supportedMode == MediaViewMode.LIST) {
        mediaViewLabel()
    } else {
        val framing = if (showWholeCovers) {
            tankobunString(R.string.cover_framing_whole_summary)
        } else {
            tankobunString(R.string.cover_framing_filled_summary)
        }
        tankobunString(
            R.string.media_view_summary,
            mediaViewLabel(),
            columns.supportedCoverColumns(),
            framing,
        )
    }
}
