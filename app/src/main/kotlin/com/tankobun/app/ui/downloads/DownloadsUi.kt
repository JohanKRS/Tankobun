package com.tankobun.app.ui.downloads

import com.tankobun.app.ui.icons.TankobunIcons

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
internal fun DownloadsScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onOpenStorageManager: () -> Unit,
) {
    val chromeInsets = LocalTankobunChromeInsets.current
    val hasActiveDownloads = state.downloads.any { it.state == DownloadState.QUEUED || it.state == DownloadState.RUNNING }
    val hasPausedDownloads = state.downloads.any { it.state == DownloadState.PAUSED }
    val hasFailedDownloads = state.downloads.any { it.state == DownloadState.FAILED }
    val compactLayout = LocalConfiguration.current.smallestScreenWidthDp in 1 until 600
    val downloadSummary = state.downloadSummaryLabel()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = chromeInsets.top + 16.dp,
            bottom = chromeInsets.bottom + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (compactLayout) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        Text(
                            downloadSummary,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            color = LocalTankobunStyle.current.colors.mutedContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TankobunActionButton(
                        label = tankobunString(R.string.downloads_storage_manager),
                        icon = TankobunIcons.Settings,
                        onClick = onOpenStorageManager,
                        modifier = Modifier.widthIn(max = 190.dp),
                        filled = false,
                    )
                }
                if (compactLayout) {
                    Text(
                        downloadSummary,
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalTankobunStyle.current.colors.mutedContent,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (hasActiveDownloads || hasPausedDownloads || hasFailedDownloads) item {
            FlowRowCompat {
                if (hasActiveDownloads) {
                    TankobunActionButton(
                        label = tankobunString(R.string.downloads_pause_active),
                        icon = TankobunIcons.Pause,
                        onClick = viewModel::pauseActiveDownloads,
                        filled = false,
                    )
                }
                if (hasPausedDownloads) {
                    TankobunActionButton(
                        label = tankobunString(R.string.downloads_resume_paused),
                        icon = TankobunIcons.PlayArrow,
                        onClick = viewModel::resumePausedDownloads,
                    )
                }
                if (hasFailedDownloads) {
                    TankobunActionButton(
                        label = tankobunString(R.string.downloads_retry_failed),
                        icon = TankobunIcons.Replay,
                        onClick = viewModel::retryFailedDownloads,
                    )
                }
            }
        }
        if (state.downloads.isEmpty()) {
            item {
                TankobunEmptyState(title = tankobunString(R.string.downloads_empty))
            }
        } else {
            items(state.downloads, key = { it.id }) { job ->
                DownloadJobRow(
                    job = job,
                    onPause = { viewModel.pauseDownload(job.id) },
                    onResume = { viewModel.resumeDownload(job.id) },
                    onRetry = { viewModel.retryDownload(job.id) },
                    onRemove = { viewModel.removeDownload(job.id) },
                )
            }
        }
    }
}

@Composable
internal fun DownloadJobRow(
    job: DownloadJob,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    ElevatedCard(shape = LocalTankobunStyle.current.themeShapes.panel) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text(job.chapterName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            job.downloadStatusLine(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (job.pageCount > 0 && job.state != DownloadState.COMPLETE) {
                            LinearProgressIndicator(
                                progress = { (job.completedPages.toFloat() / job.pageCount).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                },
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        when (job.state) {
                            DownloadState.QUEUED,
                            DownloadState.RUNNING -> IconButton(onClick = onPause) {
                                Icon(TankobunIcons.Pause, contentDescription = tankobunString(R.string.downloads_pause_download))
                            }

                            DownloadState.PAUSED -> IconButton(onClick = onResume) {
                                Icon(TankobunIcons.PlayArrow, contentDescription = tankobunString(R.string.downloads_resume_download))
                            }

                            DownloadState.FAILED -> IconButton(onClick = onRetry) {
                                Icon(TankobunIcons.Replay, contentDescription = tankobunString(R.string.downloads_retry_download))
                            }

                            DownloadState.COMPLETE -> Unit
                        }
                        IconButton(onClick = onRemove) {
                            Icon(TankobunIcons.Delete, contentDescription = tankobunString(R.string.downloads_remove_download))
                        }
                    }
                },
            )
        }
    }
}

@Composable
internal fun DownloadJob.downloadStatusLine(): String {
    val status = state.statusLabel()
    val progress = when {
        pageCount > 0 -> " / ${tankobunString(R.string.downloads_pages_progress, completedPages, pageCount)}"
        completedPages > 0 -> " / ${tankobunQuantityString(R.plurals.page_count, completedPages, completedPages)}"
        else -> ""
    }
    val retries = retryCount.takeIf { it > 0 }
        ?.let { " / ${tankobunString(R.string.downloads_retries, it)}" }
        .orEmpty()
    return "$status$progress$retries"
}

@Composable
internal fun TankobunUiState.downloadSummaryLabel(): String {
    if (downloads.isEmpty()) return tankobunString(R.string.downloads_zero_jobs)
    val running = downloads.count { it.state == DownloadState.RUNNING }
    val queued = downloads.count { it.state == DownloadState.QUEUED }
    val complete = downloads.count { it.state == DownloadState.COMPLETE }
    val failed = downloads.count { it.state == DownloadState.FAILED }
    val paused = downloads.count { it.state == DownloadState.PAUSED }
    return listOfNotNull(
        running.takeIf { it > 0 }?.let { tankobunString(R.string.downloads_running_count, it) },
        queued.takeIf { it > 0 }?.let { tankobunString(R.string.downloads_queued_count, it) },
        paused.takeIf { it > 0 }?.let { tankobunString(R.string.downloads_paused_count, it) },
        failed.takeIf { it > 0 }?.let { tankobunString(R.string.downloads_failed_count, it) },
        complete.takeIf { it > 0 }?.let { tankobunString(R.string.downloads_complete_count, it) },
    ).joinToString(" / ")
}
