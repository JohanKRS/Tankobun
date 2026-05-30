package com.tankobun.app.ui.library

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
internal fun LibraryScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var picker by remember { mutableStateOf<LibraryPicker?>(null) }
    var format by remember { mutableStateOf<String?>(null) }
    var publishingStatus by remember { mutableStateOf<String?>(null) }
    var year by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf(LIBRARY_SORT_LIST_ORDER) }
    var optionsOpen by remember { mutableStateOf(false) }
    val sections = state.librarySections
    val formatOptions = remember(sections) { libraryFormatOptions(sections) }
    val statusOptions = remember(sections) { libraryStatusOptions(sections) }
    val yearOptions = remember(sections) { libraryYearOptions(sections) }
    val resetLibraryControls = {
        query = ""
        format = null
        publishingStatus = null
        year = null
        sort = LIBRARY_SORT_LIST_ORDER
    }

    val libraryHeader: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LibraryFilterBar(
                query = query,
                onQueryChange = { query = it },
                format = format,
                publishingStatus = publishingStatus,
                year = year,
                sort = sort,
                formatOptions = formatOptions,
                statusOptions = statusOptions,
                yearOptions = yearOptions,
                onOpenPicker = { picker = it },
                onOpenOptions = { optionsOpen = true },
                onReset = resetLibraryControls,
            )

            state.message?.let {
                TankobunMessageBanner(it)
            }

            if (!state.loggedIn) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
    ) {
        LibraryPager(
            sections = sections,
            query = query,
            format = format,
            publishingStatus = publishingStatus,
            year = year,
            sort = sort,
            viewMode = state.libraryViewMode,
            coverColumns = state.libraryCoverColumns,
            showWholeCovers = state.libraryShowWholeCovers,
            modifier = Modifier.fillMaxSize(),
            header = libraryHeader,
            onSelectMedia = onSelectMedia,
        )
    }

    picker?.let { activePicker ->
        BrowseOptionDialog(
            title = when (activePicker) {
                LibraryPicker.FORMAT -> "Format"
                LibraryPicker.STATUS -> "Publishing Status"
                LibraryPicker.YEAR -> "Year"
            },
            options = when (activePicker) {
                LibraryPicker.FORMAT -> formatOptions
                LibraryPicker.STATUS -> statusOptions
                LibraryPicker.YEAR -> yearOptions
            },
            selectedValue = when (activePicker) {
                LibraryPicker.FORMAT -> format
                LibraryPicker.STATUS -> publishingStatus
                LibraryPicker.YEAR -> year
            },
            onSelect = { value ->
                when (activePicker) {
                    LibraryPicker.FORMAT -> format = value
                    LibraryPicker.STATUS -> publishingStatus = value
                    LibraryPicker.YEAR -> year = value
                }
                picker = null
            },
            onDismiss = { picker = null },
        )
    }

    if (optionsOpen) {
        LibraryOptionsDialog(
            state = state,
            viewModel = viewModel,
            sort = sort,
            onSortChange = { sort = it ?: LIBRARY_SORT_LIST_ORDER },
            onReset = resetLibraryControls,
            onDismiss = { optionsOpen = false },
        )
    }
}

@Composable
internal fun LibraryFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    format: String?,
    publishingStatus: String?,
    year: String?,
    sort: String,
    formatOptions: List<BrowseOption>,
    statusOptions: List<BrowseOption>,
    yearOptions: List<BrowseOption>,
    onOpenPicker: (LibraryPicker) -> Unit,
    onOpenOptions: () -> Unit,
    onReset: () -> Unit,
) {
    val controlsActive = query.isNotBlank() ||
        format != null ||
        publishingStatus != null ||
        year != null ||
        sort != LIBRARY_SORT_LIST_ORDER

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TankobunSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Search your library",
            showSearchAction = false,
        )
        TankobunFilterRow {
            BrowseFilterPill(
                label = "Format",
                value = formatOptions.labelFor(format),
                selected = format != null,
                onClick = { onOpenPicker(LibraryPicker.FORMAT) },
            )
            BrowseFilterPill(
                label = "Status",
                value = statusOptions.labelFor(publishingStatus),
                selected = publishingStatus != null,
                onClick = { onOpenPicker(LibraryPicker.STATUS) },
            )
            BrowseFilterPill(
                label = "Year",
                value = yearOptions.labelFor(year),
                selected = year != null,
                onClick = { onOpenPicker(LibraryPicker.YEAR) },
            )
            BrowseFilterPill(
                label = "Sort",
                value = LibrarySortOptions.labelFor(sort),
                selected = sort != LIBRARY_SORT_LIST_ORDER,
                onClick = onOpenOptions,
            )
            BrowseIconFilterPill(
                contentDescription = "Library options",
                onClick = onOpenOptions,
            )
            if (controlsActive) {
                TextButton(onClick = onReset) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
internal fun LibraryConnectPrompt(
    clientConfigured: Boolean,
    onConnect: () -> Unit,
) {
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Link, contentDescription = null)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (clientConfigured) "Connect AniList" else "AniList setup needed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (clientConfigured) {
                        "Sign in to show your lists here."
                    } else {
                        "Add AniList credentials in settings before connecting."
                    },
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TankobunActionButton(label = "Connect", enabled = clientConfigured, onClick = onConnect)
        }
    }
}

@Composable
internal fun LibraryPager(
    sections: List<LibrarySection>,
    query: String,
    format: String?,
    publishingStatus: String?,
    year: String?,
    sort: String,
    viewMode: MediaViewMode,
    coverColumns: Int,
    showWholeCovers: Boolean,
    modifier: Modifier,
    header: @Composable () -> Unit,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    if (sections.isEmpty()) {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "library-header") {
                header()
            }
            item(key = "library-empty") {
                TankobunEmptyState(title = "No manga in your AniList library yet.")
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { sections.size })
    val scope = rememberCoroutineScope()
    val visibleSections = remember(sections, query, format, publishingStatus, year, sort) {
        sections.map { section ->
            section.copy(
                items = section.items
                    .filterLibraryItems(
                        query = query,
                        format = format,
                        publishingStatus = publishingStatus,
                        year = year,
                    )
                    .sortLibraryItems(sort),
            )
        }
    }
    val filtersActive = query.isNotBlank() || format != null || publishingStatus != null || year != null

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage.coerceAtMost(sections.lastIndex),
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            sections.forEachIndexed { index, section ->
                val count = visibleSections[index].items.size
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            "${section.title} $count",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            MediaCollection(
                media = visibleSections[page].items.map { it.media },
                viewMode = viewMode,
                coverColumns = coverColumns,
                showWholeCovers = showWholeCovers,
                onSelectMedia = onSelectMedia,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
                header = header,
                emptyMessage = if (filtersActive) {
                    "No titles match these library filters."
                } else {
                    "No manga in this AniList list yet."
                },
            )
        }
    }
}

internal fun libraryFormatOptions(sections: List<LibrarySection>): List<BrowseOption> =
    listOf(BrowseOption("Any", null)) +
        sections.flatMap { section -> section.items }
            .mapNotNull { it.media.format }
            .distinct()
            .sorted()
            .map { BrowseOption(it.mediaFormatLabel(), it) }

internal fun libraryStatusOptions(sections: List<LibrarySection>): List<BrowseOption> =
    listOf(BrowseOption("Any", null)) +
        sections.flatMap { section -> section.items }
            .mapNotNull { it.media.status }
            .distinct()
            .sorted()
            .map { BrowseOption(it.statusLabel(), it) }

internal fun libraryYearOptions(sections: List<LibrarySection>): List<BrowseOption> =
    listOf(BrowseOption("Any", null)) +
        sections.flatMap { section -> section.items }
            .mapNotNull { it.media.startDateYear }
            .distinct()
            .sortedDescending()
            .map { BrowseOption(it.toString(), it.toString()) }

internal fun List<LibraryItem>.filterLibraryItems(
    query: String,
    format: String?,
    publishingStatus: String?,
    year: String?,
): List<LibraryItem> {
    val normalizedQuery = query.trim().lowercase()
    return filter { item ->
        val media = item.media
        val queryMatches = normalizedQuery.isBlank() || media.librarySearchText().contains(normalizedQuery)
        val formatMatches = format == null || media.format == format
        val statusMatches = publishingStatus == null || media.status == publishingStatus
        val yearMatches = year == null || media.startDateYear?.toString() == year
        queryMatches && formatMatches && statusMatches && yearMatches
    }
}

internal fun List<LibraryItem>.sortLibraryItems(sort: String): List<LibraryItem> =
    when (sort) {
        LIBRARY_SORT_TITLE -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.media.title.userPreferred })
        LIBRARY_SORT_UPDATED -> sortedByDescending { it.entry.updatedAtEpochSeconds ?: it.media.updatedAtEpochSeconds ?: 0L }
        LIBRARY_SORT_PROGRESS -> sortedByDescending { it.entry.progress }
        LIBRARY_SORT_SCORE -> sortedByDescending { it.entry.score ?: 0.0 }
        else -> this
    }

internal fun AnilistMedia.librarySearchText(): String =
    buildList {
        add(title.userPreferred)
        title.romaji?.let(::add)
        title.english?.let(::add)
        title.native?.let(::add)
        genres.forEach(::add)
        synonyms.forEach(::add)
    }.joinToString(" ").lowercase()
