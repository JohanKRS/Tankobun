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
internal fun SourcesSettingsScreen(state: TankobunUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedLanguageFilter by remember { mutableStateOf(SOURCE_LANGUAGE_FILTER_ACTIVE) }
    var selectedRepositoryLanguageFilter by remember { mutableStateOf(SOURCE_LANGUAGE_FILTER_ACTIVE) }
    var sourceSettingsQuery by remember { mutableStateOf("") }
    var launchedInstallRequest by remember { mutableStateOf<ExtensionInstallRequest?>(null) }
    val installLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        launchedInstallRequest?.let(viewModel::refreshInstalledSourcesAfterExtensionInstall)
        launchedInstallRequest = null
    }
    LaunchedEffect(state.extensionInstallRequest?.apkUri) {
        val installRequest = state.extensionInstallRequest ?: return@LaunchedEffect
        launchedInstallRequest = installRequest
        installLauncher.launch(downloadedExtensionInstallIntent(installRequest))
        viewModel.consumeExtensionInstallRequest()
    }
    val repositoryByPackage = remember(state.availableExtensions) {
        state.availableExtensions
            .groupBy { it.packageName }
            .mapValues { (_, entries) -> entries.maxBy { it.versionCode } }
    }
    val installedByPackage = remember(state.allInstalledSources) {
        state.allInstalledSources.groupBy { it.packageName }
    }
    val normalizedSourceSettingsQuery = remember(sourceSettingsQuery) {
        sourceSettingsQuery.trim().lowercase()
    }
    val searchableInstalledSources = remember(
        state.allInstalledSources,
        repositoryByPackage,
        normalizedSourceSettingsQuery,
    ) {
        state.allInstalledSources.filter { source ->
            source.matchesSourceSettingsQuery(
                query = normalizedSourceSettingsQuery,
                extension = repositoryByPackage[source.packageName],
            )
        }
    }
    val activeInstalledSources = remember(searchableInstalledSources, state.installedSources) {
        searchableInstalledSources.filter { source -> state.sourceActive(source) }
    }
    val visibleInstalledSourceList = remember(
        searchableInstalledSources,
        activeInstalledSources,
        selectedLanguageFilter,
    ) {
        when (selectedLanguageFilter) {
            SOURCE_LANGUAGE_FILTER_ACTIVE -> activeInstalledSources
            SOURCE_LANGUAGE_FILTER_ALL -> searchableInstalledSources
            else -> searchableInstalledSources.filter { it.lang.normalizedSourceLanguage() == selectedLanguageFilter }
        }
    }
    val sourceGroups = remember(visibleInstalledSourceList) {
        visibleInstalledSourceList
            .groupBy { it.lang.normalizedSourceLanguage() }
            .map { (language, sources) ->
                language to sources.sortedWith(
                    compareBy<SourceDescriptor> { it.name.extensionDisplayName().lowercase() }
                        .thenBy { it.id },
                )
            }
            .sortedWith(
                compareBy<Pair<String, List<SourceDescriptor>>> { sourceLanguageSortPriority(it.first) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { sourceLanguageLabel(it.first) },
            )
    }
    val languageOptions = remember(searchableInstalledSources) {
        searchableInstalledSources
            .map { it.lang.normalizedSourceLanguage() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(compareBy<String> { sourceLanguageSortPriority(it) }.thenBy { sourceLanguageLabel(it) })
    }
    val installedLanguageCounts = remember(searchableInstalledSources) {
        searchableInstalledSources
            .groupingBy { it.lang.normalizedSourceLanguage() }
            .eachCount()
    }
    val visibleInstalledSources = visibleInstalledSourceList
    val repositoryEntries = remember(state.availableExtensions) {
        state.availableExtensions.sortedWith(
            compareBy<ExtensionIndexEntry> { sourceLanguageSortPriority(it.lang.normalizedSourceLanguage()) }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name.extensionDisplayName() }
                .thenByDescending { it.versionCode },
        )
    }
    val searchableRepositoryEntries = remember(repositoryEntries, normalizedSourceSettingsQuery) {
        repositoryEntries.filter { it.matchesSourceSettingsQuery(normalizedSourceSettingsQuery) }
    }
    val activeRepositoryEntries = remember(searchableRepositoryEntries, state.sourceLanguages) {
        searchableRepositoryEntries.filter {
            val language = it.lang.normalizedSourceLanguage()
            language in state.sourceLanguages || language == UNIVERSAL_SOURCE_LANGUAGE
        }
    }
    val repositoryLanguageOptions = remember(searchableRepositoryEntries) {
        searchableRepositoryEntries
            .map { it.lang.normalizedSourceLanguage() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(compareBy<String> { sourceLanguageSortPriority(it) }.thenBy { sourceLanguageLabel(it) })
    }
    val repositoryLanguageCounts = remember(searchableRepositoryEntries) {
        searchableRepositoryEntries
            .groupingBy { it.lang.normalizedSourceLanguage() }
            .eachCount()
    }
    val visibleRepositoryEntries = remember(
        searchableRepositoryEntries,
        activeRepositoryEntries,
        selectedRepositoryLanguageFilter,
    ) {
        when (selectedRepositoryLanguageFilter) {
            SOURCE_LANGUAGE_FILTER_ACTIVE -> activeRepositoryEntries
            SOURCE_LANGUAGE_FILTER_ALL -> searchableRepositoryEntries
            else -> searchableRepositoryEntries.filter { it.lang.normalizedSourceLanguage() == selectedRepositoryLanguageFilter }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SourceSettingsSummary(
                state = state,
                repositoryCount = state.availableExtensions.size,
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                onRefreshInstalled = viewModel::refreshInstalledSources,
                onRefreshRepository = viewModel::refreshExtensionIndex,
            )
        }
        item {
            SourceSettingsSearchField(
                query = sourceSettingsQuery,
                selectedTab = selectedTab,
                onQueryChange = { sourceSettingsQuery = it },
            )
        }
        state.message?.let { message ->
            item {
                TankobunMessageBanner(message)
            }
        }

        if (selectedTab == 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Installed",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { viewModel.setSourcesEnabled(visibleInstalledSources, enabled = true) }) {
                        Text("Visible on")
                    }
                    TextButton(onClick = { viewModel.setSourcesEnabled(visibleInstalledSources, enabled = false) }) {
                        Text("Visible off")
                    }
                }
            }
            item {
                SourceLanguageFilters(
                    selectedFilter = selectedLanguageFilter,
                    languageOptions = languageOptions,
                    activeLabel = "Enabled",
                    activeCount = activeInstalledSources.size,
                    allCount = searchableInstalledSources.size,
                    languageCounts = installedLanguageCounts,
                    onSelectFilter = { selectedLanguageFilter = it },
                )
            }

            if (state.allInstalledSources.isEmpty()) {
                item {
                    Text("No installed Tachiyomi-compatible source extensions found.")
                }
            } else if (sourceGroups.isEmpty()) {
                item {
                    Text(
                        "No installed sources match this search and language filter.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(sourceGroups, key = { it.first }) { (language, sources) ->
                    SourceLanguageGroupSection(
                        language = language,
                        sources = sources,
                        state = state,
                        repositoryByPackage = repositoryByPackage,
                        onSourceEnabledChange = viewModel::setSourceEnabled,
                        onGroupEnabledChange = viewModel::setSourcesEnabled,
                        installingPackageName = state.installingExtensionPackageName,
                        iconUrlFor = viewModel::extensionIconUrl,
                        onInstall = { entry -> requestExtensionInstall(context, viewModel, entry) },
                        onUninstall = { packageName -> openExtensionUninstall(context, packageName) },
                    )
                }
            }

        } else {
            item {
                Text("Extension Repository", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.extensionRepositoryUrl,
                    onValueChange = viewModel::setExtensionRepositoryUrl,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("User-provided repository index URL") },
                    shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TankobunActionButton(label = "Load", icon = Icons.Default.Refresh, onClick = viewModel::refreshExtensionIndex)
                    TankobunActionButton(
                        label = "Rescan",
                        icon = Icons.Default.Tune,
                        onClick = viewModel::refreshInstalledSources,
                        filled = false,
                    )
                }
                if (state.availableExtensions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${state.availableExtensions.size} extensions loaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (repositoryEntries.isEmpty()) {
                item {
                    Text(
                        "Load a repository index to browse installable extensions.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    SourceLanguageFilters(
                        selectedFilter = selectedRepositoryLanguageFilter,
                        languageOptions = repositoryLanguageOptions,
                        activeLabel = "Preferred",
                        activeCount = activeRepositoryEntries.size,
                        allCount = searchableRepositoryEntries.size,
                        languageCounts = repositoryLanguageCounts,
                        onSelectFilter = { selectedRepositoryLanguageFilter = it },
                    )
                }
                if (visibleRepositoryEntries.isEmpty()) {
                    item {
                        Text(
                            "No repository extensions match this search and language filter.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(visibleRepositoryEntries, key = { "${it.packageName}:${it.versionCode}" }) { extension ->
                    ExtensionRepositoryRow(
                        extension = extension,
                        installedSources = installedByPackage[extension.packageName].orEmpty(),
                        iconUrl = viewModel.extensionIconUrl(extension),
                        installing = state.installingExtensionPackageName == extension.packageName,
                        onInstall = { requestExtensionInstall(context, viewModel, extension) },
                        onUninstall = { openExtensionUninstall(context, extension.packageName) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun SourceSettingsSummary(
    state: TankobunUiState,
    repositoryCount: Int,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onRefreshInstalled: () -> Unit,
    onRefreshRepository: () -> Unit,
) {
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sources", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${state.installedSources.size} active / ${state.allInstalledSources.size} installed / $repositoryCount in repository",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TankobunIconActionButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "Refresh installed sources",
                    onClick = onRefreshInstalled,
                )
                TankobunIconActionButton(
                    icon = Icons.Default.Download,
                    contentDescription = "Load extension repository",
                    onClick = onRefreshRepository,
                )
            }
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
            ) {
                listOf("Installed", "Repository").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onSelectTab(index) },
                        text = { Text(label) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun SourceSettingsSearchField(
    query: String,
    selectedTab: Int,
    onQueryChange: (String) -> Unit,
) {
    TankobunSearchField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = if (selectedTab == 0) "Search installed sources" else "Search extensions",
        showSearchAction = false,
    )
}

@Composable
internal fun SourceLanguageFilters(
    selectedFilter: String,
    languageOptions: List<String>,
    activeLabel: String,
    activeCount: Int,
    allCount: Int,
    languageCounts: Map<String, Int>,
    onSelectFilter: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Languages", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TankobunFilterRow {
            TankobunChip(
                selected = selectedFilter == SOURCE_LANGUAGE_FILTER_ACTIVE,
                onClick = { onSelectFilter(SOURCE_LANGUAGE_FILTER_ACTIVE) },
                label = { Text("$activeLabel $activeCount") },
            )
            TankobunChip(
                selected = selectedFilter == SOURCE_LANGUAGE_FILTER_ALL,
                onClick = { onSelectFilter(SOURCE_LANGUAGE_FILTER_ALL) },
                label = { Text("All $allCount") },
            )
            languageOptions.forEach { language ->
                val count = languageCounts[language] ?: 0
                TankobunChip(
                    selected = selectedFilter == language,
                    onClick = { onSelectFilter(language) },
                    label = { Text("${sourceLanguageDisplay(language)} $count") },
                )
            }
        }
    }
}

@Composable
internal fun SourceLanguageGroupSection(
    language: String,
    sources: List<SourceDescriptor>,
    state: TankobunUiState,
    repositoryByPackage: Map<String, ExtensionIndexEntry>,
    onSourceEnabledChange: (SourceDescriptor, Boolean) -> Unit,
    onGroupEnabledChange: (Collection<SourceDescriptor>, Boolean) -> Unit,
    installingPackageName: String?,
    iconUrlFor: (ExtensionIndexEntry) -> String?,
    onInstall: (ExtensionIndexEntry) -> Unit,
    onUninstall: (String) -> Unit,
) {
    val activeCount = sources.count { source -> state.sourceActive(source) }
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(sourceLanguageDisplay(language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "$activeCount of ${sources.size} active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onGroupEnabledChange(sources, true) }) {
                    Text("All on")
                }
                TextButton(onClick = { onGroupEnabledChange(sources, false) }) {
                    Text("All off")
                }
            }
            sources.forEach { source ->
                SourceSettingsRow(
                    source = source,
                    active = state.sourceActive(source),
                    extension = repositoryByPackage[source.packageName],
                    iconUrl = repositoryByPackage[source.packageName]?.let(iconUrlFor),
                    installing = installingPackageName == source.packageName,
                    onEnabledChange = { enabled -> onSourceEnabledChange(source, enabled) },
                    onInstall = onInstall,
                    onUninstall = onUninstall,
                )
            }
        }
    }
}

@Composable
internal fun SourceSettingsRow(
    source: SourceDescriptor,
    active: Boolean,
    extension: ExtensionIndexEntry?,
    iconUrl: String?,
    installing: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onInstall: (ExtensionIndexEntry) -> Unit,
    onUninstall: (String) -> Unit,
) {
    val displayName = source.name.extensionDisplayName()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExtensionIcon(
            packageName = source.packageName,
            name = displayName,
            iconUrl = iconUrl,
            modifier = Modifier.size(34.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(
                sourceMetadata(source, active),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val updateAvailable = extension?.let { entry ->
            source.versionCode?.let { installedVersion -> entry.versionCode > installedVersion } == true
        } == true
        if (extension != null && updateAvailable) {
            IconButton(
                enabled = !installing,
                onClick = { onInstall(extension) },
                modifier = Modifier.size(40.dp),
            ) {
                if (installing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Update $displayName",
                    )
                }
            }
        }
        IconButton(
            onClick = { onUninstall(source.packageName) },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Uninstall $displayName")
        }
        Switch(
            checked = active,
            onCheckedChange = onEnabledChange,
        )
    }
}

@Composable
internal fun ExtensionRepositoryRow(
    extension: ExtensionIndexEntry,
    installedSources: List<SourceDescriptor>,
    iconUrl: String?,
    installing: Boolean,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
) {
    val displayName = extension.name.extensionDisplayName()
    val installedVersionCode = installedSources.mapNotNull { it.versionCode }.maxOrNull()
    val installed = installedSources.isNotEmpty()
    val updateAvailable = installedVersionCode?.let { extension.versionCode > it } == true
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExtensionIcon(
                packageName = installedSources.firstOrNull()?.packageName,
                name = displayName,
                iconUrl = iconUrl,
                modifier = Modifier.size(36.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        sourceLanguageDisplay(extension.lang.normalizedSourceLanguage()),
                        "v${extension.versionName}",
                        if (extension.isNsfw) "NSFW" else null,
                        if (installed) "${installedSources.size} source${if (installedSources.size == 1) "" else "s"} installed" else null,
                    ).joinToString(" / "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                enabled = !installing,
                onClick = onInstall,
                shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
            ) {
                if (installing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            updateAvailable -> "Update"
                            installed -> "Reinstall"
                            else -> "Install"
                        },
                    )
                }
            }
            if (installed) {
                IconButton(
                    onClick = onUninstall,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Uninstall $displayName")
                }
            }
        }
    }
}

internal fun TankobunUiState.sourceActive(source: SourceDescriptor): Boolean =
    installedSources.any { it.sourceSettingsKey() == source.sourceSettingsKey() }

internal fun SourceDescriptor.matchesSourceSettingsQuery(
    query: String,
    extension: ExtensionIndexEntry?,
): Boolean {
    if (query.isBlank()) return true
    return listOfNotNull(
        name,
        name.extensionDisplayName(),
        packageName,
        packageName.substringAfterLast('.'),
        lang,
        sourceLanguageLabel(lang),
        extension?.name,
        extension?.name?.extensionDisplayName(),
        extension?.packageName,
    ).any { it.matchesSourceSettingsQuery(query) } ||
        extension?.sources.orEmpty().any { source ->
            listOfNotNull(source.name, source.name.extensionDisplayName(), source.lang, source.lang?.let(::sourceLanguageLabel))
                .any { it.matchesSourceSettingsQuery(query) }
        }
}

internal fun ExtensionIndexEntry.matchesSourceSettingsQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return listOfNotNull(
        name,
        name.extensionDisplayName(),
        packageName,
        packageName.substringAfterLast('.'),
        lang,
        sourceLanguageLabel(lang),
        versionName,
    ).any { it.matchesSourceSettingsQuery(query) } ||
        sources.any { source ->
            listOfNotNull(source.name, source.name.extensionDisplayName(), source.lang, source.lang?.let(::sourceLanguageLabel))
                .any { it.matchesSourceSettingsQuery(query) }
        }
}

internal fun String.matchesSourceSettingsQuery(query: String): Boolean =
    lowercase().contains(query)

internal fun sourceMetadata(source: SourceDescriptor, active: Boolean): String =
    listOfNotNull(
        sourceLanguageDisplay(source.lang.normalizedSourceLanguage()),
        source.versionName?.let { "v$it" },
        if (source.isNsfw) "NSFW" else null,
        if (active) "active" else "off",
    ).joinToString(" / ")

@Composable
internal fun ExtensionIcon(
    packageName: String?,
    name: String,
    iconUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val packageIcon = remember(packageName) {
        packageName
            ?.let { pkg ->
                runCatching {
                    context.packageManager.getApplicationIcon(pkg).toImageBitmap()
                }.getOrNull()
            }
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 1.dp,
    ) {
        when {
            packageIcon != null -> {
                Image(
                    bitmap = packageIcon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(3.dp),
                )
            }
            iconUrl != null -> {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(3.dp),
                )
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        extensionInitials(name),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

internal fun Drawable.toImageBitmap(): ImageBitmap {
    val width = intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = intrinsicHeight.takeIf { it > 0 } ?: 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

internal fun extensionInitials(name: String): String =
    name.extensionDisplayName()
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

internal val TachiyomiNamePrefix = Regex("^\\s*tachiyomi\\s*:?\\s*", RegexOption.IGNORE_CASE)

internal fun String.extensionDisplayName(): String {
    val cleaned = replace(TachiyomiNamePrefix, "").trim()
    return cleaned.ifBlank { trim() }
}

internal fun requestExtensionInstall(
    context: Context,
    viewModel: MainViewModel,
    extension: ExtensionIndexEntry,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
        viewModel.requireExtensionInstallPermission()
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ),
        )
        return
    }
    viewModel.installExtension(extension)
}

internal fun downloadedExtensionInstallIntent(installRequest: ExtensionInstallRequest): Intent =
    Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(installRequest.apkUri), "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

internal fun openExtensionUninstall(context: Context, packageName: String) {
    context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))
}

internal fun sourceLanguageSortPriority(language: String): Int =
    when (language.normalizedSourceLanguage()) {
        "en" -> 0
        "all" -> 1
        else -> 2
    }

internal fun cacheAgeLabel(syncedAtEpochMillis: Long): String {
    val ageMillis = (System.currentTimeMillis() - syncedAtEpochMillis).coerceAtLeast(0L)
    val minutes = ageMillis / 60_000L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        minutes < 1 -> "just now"
        hours < 1 -> "${minutes}m ago"
        days < 1 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}

internal fun String.normalizedSourceLanguage(): String =
    trim().lowercase().replace('_', '-')

internal fun sourceLanguageLabel(language: String): String =
    when (language.normalizedSourceLanguage()) {
        "all" -> "Multilingual"
        "af" -> "Afrikaans"
        "ar" -> "Arabic"
        "az" -> "Azerbaijani"
        "be" -> "Belarusian"
        "bg" -> "Bulgarian"
        "bn" -> "Bengali"
        "ca" -> "Catalan"
        "cs" -> "Czech"
        "da" -> "Danish"
        "de" -> "German"
        "el" -> "Greek"
        "en" -> "English"
        "eo" -> "Esperanto"
        "pt" -> "Portuguese"
        "pt-br" -> "Portuguese (BR)"
        "es" -> "Spanish"
        "eu" -> "Basque"
        "fa" -> "Persian"
        "fi" -> "Finnish"
        "fr" -> "French"
        "ga" -> "Irish"
        "gl" -> "Galician"
        "he" -> "Hebrew"
        "hi" -> "Hindi"
        "hr" -> "Croatian"
        "hu" -> "Hungarian"
        "id" -> "Indonesian"
        "it" -> "Italian"
        "ja" -> "Japanese"
        "ka" -> "Georgian"
        "kk" -> "Kazakh"
        "ko" -> "Korean"
        "la" -> "Latin"
        "lt" -> "Lithuanian"
        "ms" -> "Malay"
        "mn" -> "Mongolian"
        "my" -> "Burmese"
        "ne" -> "Nepali"
        "nl" -> "Dutch"
        "no" -> "Norwegian"
        "pl" -> "Polish"
        "ro" -> "Romanian"
        "ru" -> "Russian"
        "sr" -> "Serbian"
        "sv" -> "Swedish"
        "ta" -> "Tamil"
        "te" -> "Telugu"
        "th" -> "Thai"
        "tr" -> "Turkish"
        "uk" -> "Ukrainian"
        "ur" -> "Urdu"
        "vi" -> "Vietnamese"
        "zh" -> "Chinese"
        "zh-hans" -> "Chinese (Simplified)"
        "zh-hant" -> "Chinese (Traditional)"
        else -> language.uppercase()
    }

internal fun sourceLanguageDisplay(language: String): String =
    sourceLanguageFlag(language)?.let { flag -> "$flag ${sourceLanguageLabel(language)}" }
        ?: sourceLanguageLabel(language)

internal fun sourceLanguageFlag(language: String): String? {
    val normalized = language.normalizedSourceLanguage()
    if (normalized == UNIVERSAL_SOURCE_LANGUAGE) {
        return String(Character.toChars(0x1F310))
    }
    val region = normalized.substringAfter('-', "")
        .takeIf { it.length == 2 && it.all { char -> char in 'a'..'z' } }
    val countryCode = region ?: when (normalized.substringBefore('-')) {
        "af" -> "za"
        "ar" -> "sa"
        "az" -> "az"
        "be" -> "by"
        "bn" -> "bd"
        "bg" -> "bg"
        "ca" -> "es"
        "cs" -> "cz"
        "da" -> "dk"
        "de" -> "de"
        "el" -> "gr"
        "en" -> "gb"
        "es" -> "es"
        "eu" -> "es"
        "fa" -> "ir"
        "fi" -> "fi"
        "fr" -> "fr"
        "ga" -> "ie"
        "gl" -> "es"
        "he" -> "il"
        "hi" -> "in"
        "hr" -> "hr"
        "hu" -> "hu"
        "id" -> "id"
        "it" -> "it"
        "ja" -> "jp"
        "ka" -> "ge"
        "kk" -> "kz"
        "ko" -> "kr"
        "la" -> "va"
        "lt" -> "lt"
        "ms" -> "my"
        "mn" -> "mn"
        "my" -> "mm"
        "ne" -> "np"
        "nl" -> "nl"
        "no" -> "no"
        "pl" -> "pl"
        "pt" -> "pt"
        "ro" -> "ro"
        "ru" -> "ru"
        "sr" -> "rs"
        "sv" -> "se"
        "ta" -> "in"
        "te" -> "in"
        "th" -> "th"
        "tr" -> "tr"
        "uk" -> "ua"
        "ur" -> "pk"
        "vi" -> "vn"
        "zh" -> "cn"
        else -> null
    }
    return countryCode?.let(::countryFlagEmoji)
}

internal fun countryFlagEmoji(countryCode: String): String? {
    val normalized = countryCode.uppercase()
        .takeIf { it.length == 2 && it.all { char -> char in 'A'..'Z' } }
        ?: return null
    val codePoints = normalized.map { char -> 0x1F1E6 + (char - 'A') }.toIntArray()
    return String(codePoints, 0, codePoints.size)
}
