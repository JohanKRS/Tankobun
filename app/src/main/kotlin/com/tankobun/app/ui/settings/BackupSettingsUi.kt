package com.tankobun.app.ui.settings

import com.tankobun.app.ui.icons.TankobunIcons

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
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
import androidx.compose.runtime.ReadOnlyComposable
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
internal fun BackupsSettingsScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(if (state.libraryMode == LibraryMode.LOCAL) "application/json" else "text/xml"),
    ) { uri ->
        uri?.let(viewModel::saveAniListBackup)
    }
    val appSettingsBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let(viewModel::saveAppSettingsBackup)
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::restoreAniListBackup)
    }
    val appSettingsRestoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::restoreAppSettingsBackup)
    }
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let(viewModel::setScheduledBackupFolder)
    }
    val context = LocalContext.current
    val totalItems = state.libraryItems.size
    val malMatchedItems = state.libraryItems.count { it.media.idMal != null }
    val missingMalItems = totalItems - malMatchedItems
    val backupFolderLabel = remember(context, state.backupFolderUri) {
        backupFolderDisplayLabel(context, state.backupFolderUri)
    }

    SettingsDetailPanel(
        title = tankobunString(R.string.settings_backups),
        subtitle = tankobunString(R.string.settings_backups_subtitle),
        modifier = modifier,
    ) {
        Text(
            if (state.libraryMode == LibraryMode.LOCAL) {
                tankobunString(R.string.backup_local_library)
            } else {
                tankobunString(R.string.backup_anilist_manga)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
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
                        Text(
                            if (state.libraryMode == LibraryMode.LOCAL) {
                                tankobunString(R.string.backup_tankobun_json)
                            } else {
                                tankobunString(R.string.backup_mal_xml)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            backupCoverageLabel(totalItems, malMatchedItems, missingMalItems),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        state.librarySyncedAtEpochMillis.takeIf { it > 0 }?.let {
                            Text(
                                tankobunString(R.string.settings_library_cache, cacheAgeLabel(it)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text(tankobunString(R.string.backup_matched_count, malMatchedItems, totalItems)) },
                        enabled = false,
                    )
                }
                Text(
                    if (state.libraryMode == LibraryMode.LOCAL) {
                        tankobunString(R.string.backup_local_restore_desc)
                    } else {
                        tankobunString(R.string.backup_restore_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.libraryMode == LibraryMode.ANILIST) {
                        TankobunActionButton(
                            label = tankobunString(R.string.backup_sync_first),
                            onClick = viewModel::refreshLibrary,
                            enabled = state.loggedIn,
                            filled = false,
                        )
                    }
                    TankobunActionButton(
                        label = tankobunString(R.string.backup_save),
                        onClick = {
                            backupLauncher.launch(
                                if (state.libraryMode == LibraryMode.LOCAL) {
                                    suggestedTankobunLibraryBackupFileName()
                                } else {
                                    suggestedAniListBackupFileName(state.viewerName)
                                },
                            )
                        },
                        enabled = totalItems > 0,
                    )
                    TankobunActionButton(
                        label = tankobunString(R.string.backup_restore),
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/json", "text/xml", "application/xml", "*/*"))
                        },
                        enabled = state.libraryMode == LibraryMode.LOCAL || state.loggedIn,
                        filled = false,
                    )
                }
            }
        }
        Text(tankobunString(R.string.backup_app_settings), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                Text(tankobunString(R.string.backup_app_settings_json), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    tankobunString(R.string.backup_app_settings_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TankobunActionButton(
                        label = tankobunString(R.string.backup_save_settings),
                        onClick = {
                            appSettingsBackupLauncher.launch(suggestedAppSettingsBackupFileName())
                        },
                    )
                    TankobunActionButton(
                        label = tankobunString(R.string.backup_restore_settings),
                        onClick = {
                            appSettingsRestoreLauncher.launch(arrayOf("application/json", "text/json", "*/*"))
                        },
                        filled = false,
                    )
                }
            }
        }
        if (state.backupMissingSources.isNotEmpty()) {
            MissingBackupSourcesPanel(
                missingSources = state.backupMissingSources,
                availableExtensions = state.availableExtensions,
                installingPackageName = state.installingExtensionPackageName,
                iconUrlFor = viewModel::extensionIconUrl,
                onInstall = viewModel::installBackupMissingSource,
                onDismiss = viewModel::dismissBackupMissingSource,
            )
        }
        Text(tankobunString(R.string.backup_scheduled), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                Text(
                    tankobunString(R.string.backup_scheduled_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.backupFolderUri?.let {
                    ScheduledBackupFolderSummary(folderLabel = backupFolderLabel ?: tankobunString(R.string.backup_selected_folder))
                }
                Text(tankobunString(R.string.backup_scheduled_content), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                BackupContentPicker(
                    selected = state.backupContent,
                    onSelect = viewModel::setBackupContent,
                )
                Text(tankobunString(R.string.backup_scheduled_frequency), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                BackupSchedulePicker(
                    selected = state.backupSchedule,
                    onSelect = viewModel::setBackupSchedule,
                )
                Text(tankobunString(R.string.backup_scheduled_retention), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                ScheduledBackupRetentionPicker(
                    selected = state.scheduledBackupRetentionCount,
                    onSelect = viewModel::setScheduledBackupRetentionCount,
                )
                Text(
                    tankobunString(R.string.backup_scheduled_retention_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TankobunActionButton(
                        label = if (state.backupFolderUri == null) {
                            tankobunString(R.string.backup_choose_folder)
                        } else {
                            tankobunString(R.string.backup_change_folder)
                        },
                        onClick = { folderLauncher.launch(null) },
                        filled = false,
                    )
                    TankobunActionButton(
                        label = tankobunString(R.string.backup_run_now),
                        onClick = viewModel::runScheduledAniListBackupNow,
                        enabled = state.backupFolderUri != null && totalItems > 0,
                    )
                }
                val lastRun = state.lastScheduledBackupAtEpochMillis
                Text(
                    if (lastRun > 0L) {
                        tankobunString(R.string.backup_last_backup, cacheAgeLabel(lastRun))
                    } else {
                        tankobunString(R.string.backup_no_scheduled)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MissingBackupSourcesPanel(
    missingSources: List<BackupMissingSource>,
    availableExtensions: List<ExtensionIndexEntry>,
    installingPackageName: String?,
    iconUrlFor: (ExtensionIndexEntry) -> String?,
    onInstall: (BackupMissingSource) -> Unit,
    onDismiss: (String) -> Unit,
) {
    val extensionByPackage = remember(availableExtensions) {
        availableExtensions.associateBy { it.packageName }
    }
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tankobunString(R.string.backup_missing_sources), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    tankobunString(R.string.backup_missing_sources_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            missingSources.forEach { source ->
                val extension = extensionByPackage[source.packageName]
                MissingBackupSourceRow(
                    source = source,
                    extension = extension,
                    iconUrl = extension?.let(iconUrlFor),
                    installing = installingPackageName == source.packageName,
                    onInstall = { onInstall(source) },
                    onDismiss = { onDismiss(source.packageName) },
                )
            }
        }
    }
}

@Composable
private fun MissingBackupSourceRow(
    source: BackupMissingSource,
    extension: ExtensionIndexEntry?,
    iconUrl: String?,
    installing: Boolean,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ExtensionIcon(
            packageName = null,
            name = source.name,
            iconUrl = iconUrl,
            modifier = Modifier.size(42.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(source.name.extensionDisplayName(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull(
                    source.lang.takeIf { it.isNotBlank() }?.let { sourceLanguageDisplay(it) },
                    source.versionName?.let { "v$it" },
                    source.packageName,
                ).joinToString(" / "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (extension == null) {
                Text(
                    tankobunString(R.string.backup_missing_source_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Button(
            onClick = onInstall,
            enabled = extension != null && !installing,
            shape = LocalTankobunStyle.current.themeShapes.control,
        ) {
            if (installing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(TankobunIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(tankobunString(R.string.common_install))
            }
        }
        TextButton(onClick = onDismiss) {
            Text(tankobunString(R.string.common_dismiss))
        }
    }
}

@Composable
private fun ScheduledBackupFolderSummary(folderLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LocalTankobunStyle.current.themeShapes.control)
            .background(LocalTankobunStyle.current.colors.selectedChip.copy(alpha = 0.18f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            tankobunString(R.string.backup_selected_folder),
            style = MaterialTheme.typography.labelMedium,
            color = LocalTankobunStyle.current.colors.accent,
            fontWeight = FontWeight.Bold,
        )
        Text(
            folderLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            tankobunString(R.string.backup_run_now_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun BackupContentPicker(
    selected: BackupContent,
    onSelect: (BackupContent) -> Unit,
) {
    val contents = listOf(
        BackupContent.BOTH,
        BackupContent.LIBRARY,
        BackupContent.SETTINGS,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        contents.chunked(2).forEach { rowContents ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowContents.forEach { content ->
                    TankobunChip(
                        selected = selected == content,
                        onClick = { onSelect(content) },
                        label = { Text(content.label()) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowContents.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun BackupSchedulePicker(
    selected: BackupSchedule,
    onSelect: (BackupSchedule) -> Unit,
) {
    val schedules = listOf(
        BackupSchedule.OFF,
        BackupSchedule.DAILY,
        BackupSchedule.WEEKLY,
        BackupSchedule.MONTHLY,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        schedules.chunked(2).forEach { rowSchedules ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowSchedules.forEach { schedule ->
                    TankobunChip(
                        selected = selected == schedule,
                        onClick = { onSelect(schedule) },
                        label = { Text(schedule.label()) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowSchedules.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun ScheduledBackupRetentionPicker(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val options = (listOf(3, 5, 10, 20, SCHEDULED_BACKUP_RETENTION_UNLIMITED) + selected)
        .distinct()
        .sortedWith(compareBy<Int> { if (it == SCHEDULED_BACKUP_RETENTION_UNLIMITED) Int.MAX_VALUE else it })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowOptions.forEach { count ->
                    TankobunChip(
                        selected = selected == count,
                        onClick = { onSelect(count) },
                        label = { Text(count.backupRetentionLabel()) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowOptions.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun Int.backupRetentionLabel(): String =
    if (this == SCHEDULED_BACKUP_RETENTION_UNLIMITED) {
        tankobunString(R.string.backup_retention_unlimited)
    } else {
        tankobunString(R.string.backup_retention_count, this)
    }

private fun backupFolderDisplayLabel(context: Context, uriString: String?): String? {
    val uri = uriString
        ?.let { runCatching { Uri.parse(it) }.getOrNull() }
        ?: return null
    val treeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
    val documentUri = treeDocumentId?.let { documentId ->
        runCatching { DocumentsContract.buildDocumentUriUsingTree(uri, documentId) }.getOrNull()
    }
    val queriedName = documentUri?.let { folderUri ->
        runCatching {
            context.contentResolver.query(
                folderUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }?.trim()?.takeIf { it.isNotBlank() }
    return queriedName
        ?: treeDocumentId
            ?.substringAfterLast(':')
            ?.substringAfterLast('/')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        ?: uri.authority
}

internal fun suggestedTankobunLibraryBackupFileName(): String =
    "tankobun_library_${System.currentTimeMillis()}.json"
