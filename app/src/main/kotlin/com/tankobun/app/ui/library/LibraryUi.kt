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
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
    LaunchedEffect(Unit) {
        viewModel.loadBrowseTags()
    }

    var query by remember { mutableStateOf("") }
    var picker by remember { mutableStateOf<LibraryPicker?>(null) }
    var genresOpen by remember { mutableStateOf(false) }
    var tagsOpen by remember { mutableStateOf(false) }
    var genres by remember { mutableStateOf<Set<String>>(emptySet()) }
    var tags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var format by remember { mutableStateOf<String?>(null) }
    var publishingStatus by remember { mutableStateOf<String?>(null) }
    var countryOfOrigin by remember { mutableStateOf<String?>(null) }
    var year by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf(LIBRARY_SORT_LIST_ORDER) }
    var optionsOpen by remember { mutableStateOf(false) }
    val sections = state.librarySections
    val tagOptions = remember(sections, state.browseAvailableTags) { libraryTagOptions(sections, state.browseAvailableTags) }
    val formatOptions = remember(sections) { libraryFormatOptions(sections) }
    val statusOptions = remember(sections) { libraryStatusOptions(sections) }
    val countryOptions = remember(sections) { libraryCountryOptions(sections) }
    val yearOptions = remember(sections) { libraryYearOptions(sections) }
    val resetLibraryControls = {
        query = ""
        genres = emptySet()
        tags = emptySet()
        format = null
        publishingStatus = null
        countryOfOrigin = null
        year = null
        sort = LIBRARY_SORT_LIST_ORDER
    }

    val libraryHeader: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LibraryFilterBar(
                query = query,
                onQueryChange = { query = it },
                genres = genres,
                tags = tags,
                format = format,
                publishingStatus = publishingStatus,
                countryOfOrigin = countryOfOrigin,
                year = year,
                sort = sort,
                formatOptions = formatOptions,
                statusOptions = statusOptions,
                countryOptions = countryOptions,
                yearOptions = yearOptions,
                onOpenGenres = { genresOpen = true },
                onOpenTags = { tagsOpen = true },
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

    Box(modifier = Modifier.fillMaxSize()) {
        LibraryPager(
            sections = sections,
            query = query,
            genres = genres,
            tags = tags,
            format = format,
            publishingStatus = publishingStatus,
            countryOfOrigin = countryOfOrigin,
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
                LibraryPicker.FORMAT -> tankobunString(R.string.common_format)
                LibraryPicker.STATUS -> tankobunString(R.string.browse_publishing_status)
                LibraryPicker.COUNTRY -> tankobunString(R.string.browse_country_of_origin)
                LibraryPicker.YEAR -> tankobunString(R.string.common_year)
            },
            options = when (activePicker) {
                LibraryPicker.FORMAT -> formatOptions
                LibraryPicker.STATUS -> statusOptions
                LibraryPicker.COUNTRY -> countryOptions
                LibraryPicker.YEAR -> yearOptions
            },
            selectedValue = when (activePicker) {
                LibraryPicker.FORMAT -> format
                LibraryPicker.STATUS -> publishingStatus
                LibraryPicker.COUNTRY -> countryOfOrigin
                LibraryPicker.YEAR -> year
            },
            onSelect = { value ->
                when (activePicker) {
                    LibraryPicker.FORMAT -> format = value
                    LibraryPicker.STATUS -> publishingStatus = value
                    LibraryPicker.COUNTRY -> countryOfOrigin = value
                    LibraryPicker.YEAR -> year = value
                }
                picker = null
            },
            onDismiss = { picker = null },
        )
    }

    if (genresOpen) {
        LibraryGenreDialog(
            selectedGenres = genres,
            onGenreSelected = { genre, selected ->
                genres = if (selected) genres + genre else genres - genre
            },
            onClear = { genres = emptySet() },
            onDismiss = { genresOpen = false },
        )
    }

    if (tagsOpen) {
        LibraryTagDialog(
            availableTags = tagOptions,
            selectedTags = tags,
            includeAdultTags = state.showNsfwContent,
            onTagSelected = { tag, selected ->
                tags = if (selected) tags + tag else tags - tag
            },
            onClear = { tags = emptySet() },
            onDismiss = { tagsOpen = false },
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
    genres: Set<String>,
    tags: Set<String>,
    format: String?,
    publishingStatus: String?,
    countryOfOrigin: String?,
    year: String?,
    sort: String,
    formatOptions: List<BrowseOption>,
    statusOptions: List<BrowseOption>,
    countryOptions: List<BrowseOption>,
    yearOptions: List<BrowseOption>,
    onOpenGenres: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenPicker: (LibraryPicker) -> Unit,
    onOpenOptions: () -> Unit,
    onReset: () -> Unit,
) {
    val filtersOrSortActive = genres.isNotEmpty() ||
        tags.isNotEmpty() ||
        format != null ||
        publishingStatus != null ||
        countryOfOrigin != null ||
        year != null ||
        sort != LIBRARY_SORT_LIST_ORDER

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TankobunSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = tankobunString(R.string.library_search_placeholder),
            showSearchAction = false,
        )
        TankobunFilterRow {
            BrowseFilterPill(
                label = tankobunString(R.string.common_genres),
                value = if (genres.isEmpty()) tankobunString(R.string.common_any) else genres.size.toString(),
                selected = genres.isNotEmpty(),
                onClick = onOpenGenres,
            )
            BrowseFilterPill(
                label = tankobunString(R.string.common_tags),
                value = if (tags.isEmpty()) tankobunString(R.string.common_any) else tags.size.toString(),
                selected = tags.isNotEmpty(),
                onClick = onOpenTags,
            )
            BrowseFilterPill(
                label = tankobunString(R.string.common_format),
                value = formatOptions.labelFor(format),
                selected = format != null,
                onClick = { onOpenPicker(LibraryPicker.FORMAT) },
            )
            BrowseFilterPill(
                label = tankobunString(R.string.common_status),
                value = statusOptions.labelFor(publishingStatus),
                selected = publishingStatus != null,
                onClick = { onOpenPicker(LibraryPicker.STATUS) },
            )
            BrowseFilterPill(
                label = tankobunString(R.string.common_country),
                value = countryOptions.labelFor(countryOfOrigin),
                selected = countryOfOrigin != null,
                onClick = { onOpenPicker(LibraryPicker.COUNTRY) },
            )
            BrowseFilterPill(
                label = tankobunString(R.string.common_year),
                value = yearOptions.labelFor(year),
                selected = year != null,
                onClick = { onOpenPicker(LibraryPicker.YEAR) },
            )
            BrowseIconFilterPill(
                contentDescription = tankobunString(R.string.library_options),
                onClick = onOpenOptions,
            )
            if (filtersOrSortActive) {
                TankobunClearFiltersChip(onClick = onReset)
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
                    if (clientConfigured) {
                        tankobunString(R.string.library_connect_anilist)
                    } else {
                        tankobunString(R.string.library_anilist_setup_needed)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (clientConfigured) {
                        tankobunString(R.string.library_sign_in_desc)
                    } else {
                        tankobunString(R.string.library_credentials_desc)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TankobunActionButton(label = tankobunString(R.string.common_connect), enabled = clientConfigured, onClick = onConnect)
        }
    }
}

@Composable
internal fun LibraryPager(
    sections: List<LibrarySection>,
    query: String,
    genres: Set<String>,
    tags: Set<String>,
    format: String?,
    publishingStatus: String?,
    countryOfOrigin: String?,
    year: String?,
    sort: String,
    viewMode: MediaViewMode,
    coverColumns: Int,
    showWholeCovers: Boolean,
    modifier: Modifier,
    header: @Composable () -> Unit,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    val chromeInsets = LocalTankobunChromeInsets.current
    if (sections.isEmpty()) {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = LibraryContentPadding,
                top = chromeInsets.top + LibraryContentPadding,
                end = LibraryContentPadding,
                bottom = chromeInsets.bottom + LibraryContentPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "library-header") {
                header()
            }
            item(key = "library-empty") {
                TankobunEmptyState(title = tankobunString(R.string.library_empty))
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { sections.size })
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var searchHeaderHeightPx by remember { mutableIntStateOf(0) }
    var tabHeaderHeightPx by remember { mutableIntStateOf(0) }
    var pageScrollOffsets by remember(sections.size) { mutableStateOf(List(sections.size) { 0 }) }
    var pageScrollRequestOffsets by remember(sections.size) { mutableStateOf(List<Int?>(sections.size) { null }) }
    var pageScrollRequestTokens by remember(sections.size) { mutableStateOf(List(sections.size) { 0 }) }
    val visibleSections = remember(sections, query, genres, tags, format, publishingStatus, countryOfOrigin, year, sort) {
        sections.map { section ->
            section.copy(
                items = section.items
                    .filterLibraryItems(
                        query = query,
                        genres = genres,
                        tags = tags,
                        format = format,
                        publishingStatus = publishingStatus,
                        countryOfOrigin = countryOfOrigin,
                        year = year,
                    )
                    .sortLibraryItems(sort),
            )
        }
    }
    val filtersActive = query.isNotBlank() ||
        genres.isNotEmpty() ||
        tags.isNotEmpty() ||
        format != null ||
        publishingStatus != null ||
        countryOfOrigin != null ||
        year != null
    val currentPage = pagerState.currentPage.coerceIn(0, sections.lastIndex)
    val targetPage = pagerState.targetPage.coerceIn(0, sections.lastIndex)
    val currentScrollOffsetPx = pageScrollOffsets.getOrElse(currentPage) { 0 }
    val targetScrollOffsetPx = pageScrollOffsets.getOrElse(targetPage) { 0 }
    val currentHeaderIsPinned = searchHeaderHeightPx > 0 && currentScrollOffsetPx >= searchHeaderHeightPx
    val scrollOffsetForHeader = if (
        pagerState.isScrollInProgress &&
        targetPage != currentPage &&
        currentHeaderIsPinned
    ) {
        maxOf(currentScrollOffsetPx, targetScrollOffsetPx)
    } else {
        currentScrollOffsetPx
    }
    val searchHeaderCollapsePx = scrollOffsetForHeader.coerceAtMost(searchHeaderHeightPx)
    val headerTranslationY = -searchHeaderCollapsePx.toFloat()
    val sharedHeaderHeight = with(density) { (searchHeaderHeightPx + tabHeaderHeightPx).toDp() }
    var lastObservedPage by remember(sections.size) { mutableIntStateOf(currentPage) }

    fun requestPageScrollOffset(index: Int, offsetPx: Int) {
        if (searchHeaderHeightPx <= 0) return
        val requestedOffsetPx = offsetPx.coerceAtLeast(0)
        pageScrollOffsets = pageScrollOffsets.toMutableList().also { offsets ->
            if (index in offsets.indices) {
                offsets[index] = requestedOffsetPx
            }
        }
        pageScrollRequestOffsets = pageScrollRequestOffsets.toMutableList().also { offsets ->
            if (index in offsets.indices) {
                offsets[index] = requestedOffsetPx
            }
        }
        pageScrollRequestTokens = pageScrollRequestTokens.toMutableList().also { tokens ->
            if (index in tokens.indices) {
                tokens[index] = tokens[index] + 1
            }
        }
    }

    fun pinPageAtContentTop(index: Int) {
        requestPageScrollOffset(index = index, offsetPx = searchHeaderHeightPx)
    }

    LaunchedEffect(targetPage, searchHeaderHeightPx) {
        if (targetPage != currentPage) {
            if (currentHeaderIsPinned) {
                pinPageAtContentTop(targetPage)
            } else {
                requestPageScrollOffset(
                    index = targetPage,
                    offsetPx = currentScrollOffsetPx.coerceAtMost(searchHeaderHeightPx),
                )
            }
        }
    }

    LaunchedEffect(currentPage, searchHeaderHeightPx) {
        val previousPage = lastObservedPage
        val previousPageWasPinned = searchHeaderHeightPx > 0 &&
            pageScrollOffsets.getOrElse(previousPage) { 0 } >= searchHeaderHeightPx
        lastObservedPage = currentPage
        if (previousPage != currentPage && previousPageWasPinned) {
            pinPageAtContentTop(currentPage)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val supportedViewMode = viewMode.supportedMediaViewMode()
        val supportedCoverColumns = coverColumns
            .supportedCoverColumns()
            .coerceAtMost(if (configuration.smallestScreenWidthDp in 1 until 600) 4 else 8)
        val contentPaddingPx = with(density) { LibraryContentPadding.roundToPx() }
        val topChromeInsetPx = with(density) { chromeInsets.top.roundToPx() }
        val bottomChromeInsetPx = with(density) { chromeInsets.bottom.roundToPx() }
        val gridGapPx = with(density) { 16.dp.roundToPx() }
        val listGapPx = with(density) { 8.dp.roundToPx() }
        val availableGridWidthPx = (constraints.maxWidth - (contentPaddingPx * 2) -
            (gridGapPx * (supportedCoverColumns - 1))).coerceAtLeast(0)
        val gridCellWidthPx = if (supportedCoverColumns > 0) {
            availableGridWidthPx / supportedCoverColumns
        } else {
            availableGridWidthPx
        }
        val gridCoverHeightPx = (gridCellWidthPx * 3) / 2
        val gridInfoHeightPx = with(density) {
            if (supportedViewMode == MediaViewMode.COVER_WITH_INFO) 36.dp.roundToPx() else 0
        }
        val listItemHeightPx = with(density) { 96.dp.roundToPx() }
        val emptyStateHeightPx = with(density) { 160.dp.roundToPx() }
        var measuredPageContentHeightsPx by remember(
            visibleSections,
            supportedViewMode,
            supportedCoverColumns,
            showWholeCovers,
            constraints.maxWidth,
        ) {
            mutableStateOf(List<Int?>(sections.size) { null })
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val itemCount = visibleSections[page].items.size
            val rowCount = if (itemCount == 0) {
                1
            } else if (supportedViewMode == MediaViewMode.LIST) {
                itemCount
            } else {
                (itemCount + supportedCoverColumns - 1) / supportedCoverColumns
            }
            val estimatedContentHeightPx = when {
                itemCount == 0 -> emptyStateHeightPx
                supportedViewMode == MediaViewMode.LIST ->
                    (rowCount * listItemHeightPx) + ((rowCount - 1).coerceAtLeast(0) * listGapPx)
                else -> {
                    val rowHeightPx = gridCoverHeightPx + gridInfoHeightPx
                    (rowCount * rowHeightPx) + ((rowCount - 1).coerceAtLeast(0) * gridGapPx)
                }
            }
            val contentHeightPx = measuredPageContentHeightsPx.getOrNull(page) ?: estimatedContentHeightPx
            val availablePinnedHeightPx = constraints.maxHeight -
                topChromeInsetPx -
                bottomChromeInsetPx
            val requiredPinnedBottomPaddingPx = availablePinnedHeightPx -
                tabHeaderHeightPx -
                contentPaddingPx -
                contentHeightPx
            val shouldMeasureShortPage = estimatedContentHeightPx < constraints.maxHeight
            val pageBottomPadding = with(density) {
                maxOf(contentPaddingPx, requiredPinnedBottomPaddingPx).toDp()
            }
            val pageContentPadding = PaddingValues(
                start = LibraryContentPadding,
                top = chromeInsets.top + sharedHeaderHeight + LibraryContentPadding,
                end = LibraryContentPadding,
                bottom = pageBottomPadding + chromeInsets.bottom,
            )
            MediaCollection(
                media = visibleSections[page].items.map { it.media },
                viewMode = viewMode,
                coverColumns = coverColumns,
                showWholeCovers = showWholeCovers,
                onSelectMedia = onSelectMedia,
                modifier = Modifier.fillMaxSize(),
                contentPadding = pageContentPadding,
                onScrollOffsetChange = { scrollOffset ->
                    pageScrollOffsets = pageScrollOffsets.toMutableList().also { offsets ->
                        if (page in offsets.indices) {
                            offsets[page] = scrollOffset
                        }
                    }
                },
                scrollToContentOffsetPx = pageScrollRequestOffsets.getOrNull(page),
                scrollToContentOffsetRequest = pageScrollRequestTokens.getOrElse(page) { 0 },
                onContentHeightMeasured = if (shouldMeasureShortPage) {
                    { heightPx ->
                        measuredPageContentHeightsPx = measuredPageContentHeightsPx.toMutableList().also { heights ->
                            if (page in heights.indices) {
                                heights[page] = heightPx
                            }
                        }
                    }
                } else {
                    null
                },
                emptyMessage = if (filtersActive) {
                    tankobunString(R.string.library_filter_empty)
                } else {
                    tankobunString(R.string.library_list_empty)
                },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = headerTranslationY }
                .background(LocalTankobunStyle.current.colors.backdrop)
                .padding(top = chromeInsets.top)
        ) {
            Column(
                modifier = Modifier.onSizeChanged { searchHeaderHeightPx = it.height },
            ) {
                Spacer(Modifier.height(LibraryContentPadding))
                Box(Modifier.padding(horizontal = LibraryContentPadding)) {
                    header()
                }
                Spacer(Modifier.height(14.dp))
            }
            Column(
                modifier = Modifier.onSizeChanged { tabHeaderHeightPx = it.height },
            ) {
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
                            onClick = {
                                if (currentHeaderIsPinned) {
                                    pinPageAtContentTop(index)
                                } else {
                                    requestPageScrollOffset(
                                        index = index,
                                        offsetPx = currentScrollOffsetPx.coerceAtMost(searchHeaderHeightPx),
                                    )
                                }
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                val tabTextColor = LocalContentColor.current
                                Text(
                                    buildAnnotatedString {
                                        append(section.status?.let { tankobunString(it.sectionTitleRes()) } ?: section.title)
                                        append(" ")
                                        pushStyle(SpanStyle(color = tabTextColor.copy(alpha = 0.74f)))
                                        append(count.toString())
                                        pop()
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = LocalTankobunStyle.current.typography.sectionLabel,
                                )
                            },
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f)),
                )
            }
        }
    }
}

private val LibraryContentPadding = 18.dp

@Composable
internal fun LibraryGenreDialog(
    selectedGenres: Set<String>,
    onGenreSelected: (String, Boolean) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        TankobunDialogSurface(fillMaxHeightFraction = 0.78f, scrollable = false) {
            TankobunDialogHeader(title = tankobunString(R.string.common_genres), onDismiss = onDismiss)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                FlowRowCompat {
                    BrowseGenres.forEach { genre ->
                        TankobunChip(
                            selected = genre in selectedGenres,
                            onClick = { onGenreSelected(genre, genre !in selectedGenres) },
                            label = {
                                Text(browseGenreLabel(genre), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClear) {
                    Text(tankobunString(R.string.common_clear))
                }
                Spacer(Modifier.weight(1f))
                TankobunActionButton(label = tankobunString(R.string.common_apply), onClick = onDismiss)
            }
        }
    }
}

@Composable
internal fun LibraryTagDialog(
    availableTags: List<AnilistMediaTag>,
    selectedTags: Set<String>,
    includeAdultTags: Boolean,
    onTagSelected: (String, Boolean) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visibleTags = availableTags.visibleTags(query, includeAdult = includeAdultTags)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        TankobunDialogSurface(fillMaxHeightFraction = 0.82f, scrollable = false) {
            TankobunDialogHeader(title = tankobunString(R.string.common_tags), onDismiss = onDismiss)
            TankobunSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = tankobunString(R.string.browse_find_tag),
                showSearchAction = false,
            )
            if (availableTags.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        tankobunString(R.string.library_tags_after_sync),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    FlowRowCompat {
                        visibleTags.forEach { tag ->
                            TankobunChip(
                                selected = tag.name in selectedTags,
                                onClick = { onTagSelected(tag.name, tag.name !in selectedTags) },
                                label = {
                                    Text(
                                        tag.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClear) {
                    Text(tankobunString(R.string.common_clear))
                }
                Spacer(Modifier.weight(1f))
                TankobunActionButton(label = tankobunString(R.string.common_apply), onClick = onDismiss)
            }
        }
    }
}

internal fun libraryTagOptions(
    sections: List<LibrarySection>,
    anilistTags: List<AnilistMediaTag>,
): List<AnilistMediaTag> {
    val tagsInLibrary = sections
        .flatMap { section -> section.items }
        .flatMap { item -> item.media.tags }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
    val knownTagsByName = anilistTags.associateBy { it.name.lowercase(Locale.ROOT) }
    return tagsInLibrary.map { tagName ->
        knownTagsByName[tagName.lowercase(Locale.ROOT)] ?: AnilistMediaTag(
            name = tagName,
            category = null,
            isAdult = false,
        )
    }
}

internal fun libraryFormatOptions(sections: List<LibrarySection>): List<BrowseOption> =
    listOf(BrowseOption(R.string.common_any, null)) +
        sections.flatMap { section -> section.items }
            .mapNotNull { it.media.format }
            .distinct()
            .sorted()
            .map { BrowseOption(it.mediaFormatLabelRes(), it) }

internal fun libraryStatusOptions(sections: List<LibrarySection>): List<BrowseOption> =
    listOf(BrowseOption(R.string.common_any, null)) +
        sections.flatMap { section -> section.items }
            .mapNotNull { it.media.status }
            .distinct()
            .sorted()
            .map { BrowseOption(it.publishingStatusLabelRes(), it) }

internal fun libraryCountryOptions(sections: List<LibrarySection>): List<BrowseOption> {
    val countriesInLibrary = sections
        .flatMap { section -> section.items }
        .mapNotNull { it.media.countryOfOrigin }
        .toSet()
    val countryOptions = BrowseCountryOptions.filter { option ->
        option.value == null || option.value in countriesInLibrary
    }
    return if (countryOptions.size > 1) countryOptions else BrowseCountryOptions
}

internal fun libraryYearOptions(sections: List<LibrarySection>): List<BrowseOption> =
    listOf(BrowseOption(R.string.common_any, null)) +
        sections.flatMap { section -> section.items }
            .mapNotNull { it.media.startDateYear }
            .distinct()
            .sortedDescending()
            .map { BrowseOption(it.toString(), it.toString()) }

internal fun List<LibraryItem>.filterLibraryItems(
    query: String,
    genres: Set<String>,
    tags: Set<String>,
    format: String?,
    publishingStatus: String?,
    countryOfOrigin: String?,
    year: String?,
): List<LibraryItem> {
    val normalizedQuery = query.trim().lowercase()
    return filter { item ->
        val media = item.media
        val queryMatches = normalizedQuery.isBlank() || media.librarySearchText().contains(normalizedQuery)
        val genreMatches = genres.isEmpty() || genres.any { genre ->
            media.genres.any { it.equals(genre, ignoreCase = true) }
        }
        val tagMatches = tags.isEmpty() || tags.any { tag ->
            media.tags.any { it.equals(tag, ignoreCase = true) }
        }
        val formatMatches = format == null || media.format == format
        val statusMatches = publishingStatus == null || media.status == publishingStatus
        val countryMatches = countryOfOrigin == null || media.countryOfOrigin == countryOfOrigin
        val yearMatches = year == null || media.startDateYear?.toString() == year
        queryMatches && genreMatches && tagMatches && formatMatches && statusMatches && countryMatches && yearMatches
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
        tags.forEach(::add)
        countryOfOrigin?.let(::add)
        synonyms.forEach(::add)
    }.joinToString(" ").lowercase()
