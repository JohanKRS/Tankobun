package com.tankobun.app.ui.library

import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.app.ui.icons.genreIcon

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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.tankobun.app.sharing.RECOMMENDATION_MESSAGE_MAX_LENGTH
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
    val selectedLibraryItems = remember(state.libraryItems, state.selectedLibraryMediaIds, state.selectedLibraryBatchMedia) {
        state.selectedLibraryBatchItems()
    }
    val selectedCount = state.selectedLibraryMediaIds.size
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

    BackHandler(enabled = selectedCount > 0) {
        viewModel.clearLibraryBatchSelection()
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
                Box(Modifier.padding(horizontal = LibraryContentPadding)) {
                    TankobunMessageBanner(it)
                }
            }

            if (state.libraryMode == LibraryMode.ANILIST && !state.loggedIn) {
                Box(Modifier.padding(horizontal = LibraryContentPadding)) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        val chromeInsets = LocalTankobunChromeInsets.current
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
            selectedMediaIds = state.selectedLibraryMediaIds,
            selectionMode = selectedCount > 0,
            onToggleMediaSelection = viewModel::toggleLibraryBatchSelection,
            onLongPressMedia = viewModel::startLibraryBatchSelection,
        )
        AnimatedVisibility(
            visible = selectedCount > 0,
            modifier = Modifier
                .align(state.dockAlignment.libraryBatchBarAlignment())
                .padding(horizontal = 14.dp)
                .padding(bottom = chromeInsets.bottom + 8.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LibraryBatchActionBar(
                selectedCount = selectedCount,
                canRemoveCustomList = selectedLibraryItems.any { item -> item.entry.customLists.isNotEmpty() },
                onShare = viewModel::showLibraryShareDialog,
                onChangeStatus = viewModel::showLibraryBatchStatusDialog,
                onAddCustomList = viewModel::showLibraryBatchAddCustomListDialog,
                onRemoveCustomList = viewModel::showLibraryBatchRemoveCustomListDialog,
                onDelete = viewModel::showLibraryBatchDeleteDialog,
                onClose = viewModel::clearLibraryBatchSelection,
                modifier = Modifier.widthIn(max = 560.dp),
            )
        }
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

    if (state.libraryShareDialogVisible) {
        LibraryShareDialog(
            selectedItems = selectedLibraryItems,
            onShare = { name, messagesByMediaId ->
                viewModel.shareSelectedRecommendations(context, name, messagesByMediaId)
            },
            onDismiss = viewModel::dismissLibraryBatchDialogs,
        )
    }

    if (state.libraryBatchStatusDialogVisible) {
        LibraryBatchStatusDialog(
            selectedCount = selectedCount,
            onSelectStatus = viewModel::applyLibraryBatchStatus,
            onDismiss = viewModel::dismissLibraryBatchDialogs,
        )
    }

    if (state.libraryBatchCustomListDialogVisible) {
        val removableLists = selectedLibraryItems
            .flatMap { item -> item.entry.customLists }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
        LibraryBatchCustomListDialog(
            selectedCount = selectedCount,
            remove = state.libraryBatchRemoveCustomList,
            availableLists = if (state.libraryBatchRemoveCustomList) removableLists else state.anilistCustomLists,
            onConfirm = { name -> viewModel.applyLibraryBatchCustomList(name, state.libraryBatchRemoveCustomList) },
            onDismiss = viewModel::dismissLibraryBatchDialogs,
        )
    }

    if (state.libraryBatchDeleteDialogVisible) {
        LibraryBatchDeleteDialog(
            selectedCount = selectedCount,
            onDeleteLibraryOnly = { viewModel.deleteSelectedLibraryEntries(deleteLocalData = false) },
            onDeleteWithLocalData = { viewModel.deleteSelectedLibraryEntries(deleteLocalData = true) },
            onDismiss = viewModel::dismissLibraryBatchDialogs,
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
        Box(Modifier.padding(horizontal = LibraryContentPadding)) {
            TankobunSearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = tankobunString(R.string.library_search_placeholder),
                showSearchAction = false,
            )
        }
        TankobunHorizontalFilterRow(contentPadding = PaddingValues(horizontal = LibraryContentPadding)) {
            item {
                BrowseFilterPill(
                    label = tankobunString(R.string.common_genres),
                    value = if (genres.isEmpty()) tankobunString(R.string.common_any) else genres.size.toString(),
                    selected = genres.isNotEmpty(),
                    icon = TankobunIcons.Category,
                    onClick = onOpenGenres,
                )
            }
            item {
                BrowseFilterPill(
                    label = tankobunString(R.string.common_tags),
                    value = if (tags.isEmpty()) tankobunString(R.string.common_any) else tags.size.toString(),
                    selected = tags.isNotEmpty(),
                    icon = TankobunIcons.LocalOffer,
                    onClick = onOpenTags,
                )
            }
            item {
                BrowseFilterPill(
                    label = tankobunString(R.string.common_format),
                    value = formatOptions.labelFor(format),
                    selected = format != null,
                    icon = TankobunIcons.MenuBook,
                    onClick = { onOpenPicker(LibraryPicker.FORMAT) },
                )
            }
            item {
                BrowseFilterPill(
                    label = tankobunString(R.string.common_status),
                    value = statusOptions.labelFor(publishingStatus),
                    selected = publishingStatus != null,
                    icon = TankobunIcons.Flag,
                    onClick = { onOpenPicker(LibraryPicker.STATUS) },
                )
            }
            item {
                BrowseFilterPill(
                    label = tankobunString(R.string.common_country),
                    value = countryOptions.labelFor(countryOfOrigin),
                    selected = countryOfOrigin != null,
                    icon = TankobunIcons.Public,
                    onClick = { onOpenPicker(LibraryPicker.COUNTRY) },
                )
            }
            item {
                BrowseFilterPill(
                    label = tankobunString(R.string.common_year),
                    value = yearOptions.labelFor(year),
                    selected = year != null,
                    icon = TankobunIcons.CalendarMonth,
                    onClick = { onOpenPicker(LibraryPicker.YEAR) },
                )
            }
            item {
                BrowseIconFilterPill(
                    contentDescription = tankobunString(R.string.library_options),
                    onClick = onOpenOptions,
                )
            }
            if (filtersOrSortActive) {
                item {
                    TankobunClearFiltersChip(onClick = onReset)
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
            Icon(TankobunIcons.Link, contentDescription = null)
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
    selectedMediaIds: Set<Int> = emptySet(),
    selectionMode: Boolean = false,
    onToggleMediaSelection: (AnilistMedia) -> Unit = {},
    onLongPressMedia: (AnilistMedia) -> Unit = {},
) {
    val chromeInsets = LocalTankobunChromeInsets.current
    if (sections.isEmpty()) {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                top = chromeInsets.top + LibraryContentPadding,
                bottom = chromeInsets.bottom + LibraryContentPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "library-header") {
                header()
            }
            item(key = "library-empty") {
                Box(Modifier.padding(horizontal = LibraryContentPadding)) {
                    TankobunEmptyState(title = tankobunString(R.string.library_empty))
                }
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
    var headerCollapsePx by remember { mutableFloatStateOf(0f) }
    val sectionKeys = remember(sections) { sections.map { it.key } }
    val pageListStates = remember(sectionKeys) { List(sections.size) { LazyListState() } }
    val pageGridStates = remember(sectionKeys) { List(sections.size) { LazyGridState() } }
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
    val searchHeaderCollapsePx = headerCollapsePx.coerceIn(0f, searchHeaderHeightPx.toFloat())
    val headerTranslationY = -searchHeaderCollapsePx
    val visibleHeaderHeight = with(density) {
        (searchHeaderHeightPx + tabHeaderHeightPx - searchHeaderCollapsePx.roundToInt()).toDp()
    }
    val headerScrollConnection = remember(searchHeaderHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (searchHeaderHeightPx <= 0) return Offset.Zero
                return when {
                    available.y < 0f && headerCollapsePx < searchHeaderHeightPx -> {
                        val consumed = minOf(-available.y, searchHeaderHeightPx - headerCollapsePx)
                        headerCollapsePx += consumed
                        Offset(x = 0f, y = -consumed)
                    }
                    available.y > 0f && headerCollapsePx > 0f -> {
                        val consumed = minOf(available.y, headerCollapsePx)
                        headerCollapsePx -= consumed
                        Offset(x = 0f, y = consumed)
                    }
                    else -> Offset.Zero
                }
            }
        }
    }

    LaunchedEffect(searchHeaderHeightPx) {
        headerCollapsePx = headerCollapsePx.coerceIn(0f, searchHeaderHeightPx.toFloat())
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(headerScrollConnection),
    ) {
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
                top = chromeInsets.top + visibleHeaderHeight + LibraryContentPadding,
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
                providedListState = pageListStates[page],
                providedGridState = pageGridStates[page],
                selectedMediaIds = selectedMediaIds,
                selectionMode = selectionMode,
                onToggleMediaSelection = onToggleMediaSelection,
                onLongPressMedia = onLongPressMedia,
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
                header()
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

@Composable
internal fun LibraryBatchActionBar(
    selectedCount: Int,
    canRemoveCustomList: Boolean,
    onShare: () -> Unit,
    onChangeStatus: () -> Unit,
    onAddCustomList: () -> Unit,
    onRemoveCustomList: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val useCompactCount = configuration.screenWidthDp < 600
    val selectedCountText = if (useCompactCount) {
        tankobunString(R.string.library_batch_selected_count_compact, selectedCount)
    } else if (selectedCount == 1) {
        tankobunString(R.string.library_batch_selected_count_one)
    } else {
        tankobunString(R.string.library_batch_selected_count, selectedCount)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = LocalTankobunStyle.current.colors.panel.copy(alpha = 0.96f),
        contentColor = LocalTankobunStyle.current.colors.panelContent,
        tonalElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            LocalTankobunStyle.current.colors.outline.copy(alpha = 0.22f),
        ),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                selectedCountText,
                modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LibraryBatchSeparator()
            LibraryBatchIconAction(
                icon = TankobunIcons.Share,
                contentDescription = tankobunString(R.string.library_batch_share),
                onClick = onShare,
            )
            LibraryBatchIconAction(
                icon = TankobunIcons.Label,
                contentDescription = tankobunString(R.string.library_batch_status),
                onClick = onChangeStatus,
            )
            LibraryBatchIconAction(
                icon = TankobunIcons.PlaylistAdd,
                contentDescription = tankobunString(R.string.library_batch_add_list),
                onClick = onAddCustomList,
            )
            LibraryBatchIconAction(
                icon = TankobunIcons.PlaylistRemove,
                contentDescription = tankobunString(R.string.library_batch_remove_list),
                enabled = canRemoveCustomList,
                onClick = onRemoveCustomList,
            )
            LibraryBatchIconAction(
                icon = TankobunIcons.Delete,
                contentDescription = tankobunString(R.string.library_batch_delete),
                onClick = onDelete,
            )
            LibraryBatchSeparator()
            LibraryBatchIconAction(
                icon = TankobunIcons.Close,
                contentDescription = tankobunString(R.string.common_close),
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun LibraryBatchSeparator() {
    Box(
        modifier = Modifier
            .height(26.dp)
            .width(1.dp)
            .background(LocalTankobunStyle.current.colors.outline.copy(alpha = 0.24f)),
    )
}

@Composable
internal fun LibraryBatchIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(38.dp),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(19.dp))
    }
}

private fun DockAlignment.libraryBatchBarAlignment(): Alignment =
    when (this) {
        DockAlignment.LEFT -> Alignment.BottomStart
        DockAlignment.CENTER -> Alignment.BottomCenter
        DockAlignment.RIGHT -> Alignment.BottomEnd
    }

@Composable
internal fun LibraryShareDialog(
    selectedItems: List<LibraryItem>,
    onShare: (String, Map<Int, String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val defaultName = tankobunString(R.string.recommendations_default_list_name)
    val shareItems = remember(selectedItems) {
        selectedItems.sortedBy { item -> item.media.title.userPreferred.lowercase(Locale.ROOT) }
    }
    var listName by remember(defaultName) { mutableStateOf(defaultName) }
    var expandedMessageIds by remember(shareItems) { mutableStateOf(emptySet<Int>()) }
    var messagesByMediaId by remember(shareItems) {
        mutableStateOf(shareItems.associate { item -> item.media.id to "" })
    }
    TankobunDialog(
        onDismiss = onDismiss,
        maxWidth = 680.dp,
        fillMaxHeightFraction = 0.86f,
        scrollable = false,
    ) {
        TankobunDialogHeader(title = tankobunString(R.string.library_batch_share_title), onDismiss = onDismiss)
        Text(
            tankobunString(R.string.library_batch_share_desc, shareItems.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = listName,
            onValueChange = { listName = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(tankobunString(R.string.library_batch_list_name)) },
            shape = LocalTankobunStyle.current.themeShapes.control,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (shareItems.isEmpty()) {
                Text(
                    tankobunString(R.string.recommendations_import_empty),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = shareItems,
                        key = { item -> item.media.id },
                    ) { item ->
                        val mediaId = item.media.id
                        val message = messagesByMediaId[mediaId].orEmpty()
                        val expanded = mediaId in expandedMessageIds
                        RecommendationShareItemRow(
                            item = item,
                            message = message,
                            expanded = expanded,
                            onToggleExpanded = {
                                expandedMessageIds = if (expanded) {
                                    expandedMessageIds - mediaId
                                } else {
                                    expandedMessageIds + mediaId
                                }
                            },
                            onMessageChange = { next ->
                                messagesByMediaId = messagesByMediaId + (mediaId to next.take(RECOMMENDATION_MESSAGE_MAX_LENGTH))
                            },
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss) {
                Text(tankobunString(R.string.common_cancel))
            }
            Spacer(Modifier.weight(1f))
            TankobunActionButton(
                label = tankobunString(R.string.library_batch_share),
                icon = TankobunIcons.Share,
                enabled = listName.trim().isNotBlank() && shareItems.isNotEmpty(),
                onClick = { onShare(listName, messagesByMediaId) },
            )
        }
    }
}

@Composable
private fun RecommendationShareItemRow(
    item: LibraryItem,
    message: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onMessageChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalTankobunStyle.current.themeShapes.panel,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverImage(
                    url = item.media.coverImage,
                    title = item.media.title.userPreferred,
                    modifier = Modifier.size(width = 42.dp, height = 60.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        item.media.title.userPreferred,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        listOfNotNull(
                            item.media.mediaTypeLabel(),
                            item.media.startDateYear?.toString(),
                        ).joinToString(" / "),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!expanded && message.isNotBlank()) {
                        Text(
                            message,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onToggleExpanded) {
                    Text(
                        when {
                            expanded -> tankobunString(R.string.recommendations_share_message_done)
                            message.isBlank() -> tankobunString(R.string.recommendations_share_add_message)
                            else -> tankobunString(R.string.recommendations_share_edit_message)
                        },
                        maxLines = 1,
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    label = { Text(tankobunString(R.string.recommendations_share_message_label)) },
                    placeholder = { Text(tankobunString(R.string.recommendations_share_message_placeholder)) },
                    supportingText = {
                        Text(
                            tankobunString(
                                R.string.recommendations_share_message_counter,
                                message.length,
                                RECOMMENDATION_MESSAGE_MAX_LENGTH,
                            ),
                        )
                    },
                    shape = LocalTankobunStyle.current.themeShapes.control,
                )
            }
        }
    }
}

@Composable
internal fun LibraryBatchStatusDialog(
    selectedCount: Int,
    onSelectStatus: (MediaStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    TankobunDialog(onDismiss = onDismiss) {
        TankobunDialogHeader(title = tankobunString(R.string.library_batch_status_title), onDismiss = onDismiss)
        Text(
            tankobunString(R.string.library_batch_status_desc, selectedCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            trackingStatuses().forEach { status ->
                OutlinedButton(
                    onClick = { onSelectStatus(status) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = LocalTankobunStyle.current.themeShapes.control,
                ) {
                    Icon(
                        trackingStatusIcon(status),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(status.displayName(), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun LibraryBatchCustomListDialog(
    selectedCount: Int,
    remove: Boolean,
    availableLists: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialName = if (remove) availableLists.firstOrNull().orEmpty() else ""
    var listName by remember(remove, availableLists) { mutableStateOf(initialName) }
    val title = if (remove) {
        tankobunString(R.string.library_batch_custom_list_remove_title)
    } else {
        tankobunString(R.string.library_batch_custom_list_add_title)
    }
    TankobunDialog(onDismiss = onDismiss) {
        TankobunDialogHeader(title = title, onDismiss = onDismiss)
        Text(
            tankobunString(R.string.library_batch_custom_list_desc, selectedCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!remove) {
            OutlinedTextField(
                value = listName,
                onValueChange = { listName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(tankobunString(R.string.library_batch_list_name)) },
                shape = LocalTankobunStyle.current.themeShapes.control,
            )
        }
        if (availableLists.isNotEmpty()) {
            FlowRowCompat {
                availableLists.forEach { customList ->
                    TankobunChip(
                        selected = customList.equals(listName, ignoreCase = true),
                        onClick = { listName = customList },
                        leadingIcon = {
                            TankobunChipIcon(
                                if (remove) TankobunIcons.PlaylistRemove else TankobunIcons.PlaylistAdd,
                            )
                        },
                        label = { Text(customList, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        } else if (remove) {
            Text(
                tankobunString(R.string.library_batch_no_custom_lists),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss) {
                Text(tankobunString(R.string.common_cancel))
            }
            Spacer(Modifier.weight(1f))
            TankobunActionButton(
                label = if (remove) {
                    tankobunString(R.string.library_batch_remove_list)
                } else {
                    tankobunString(R.string.library_batch_add_list)
                },
                icon = if (remove) TankobunIcons.PlaylistRemove else TankobunIcons.PlaylistAdd,
                enabled = listName.trim().isNotBlank(),
                onClick = { onConfirm(listName) },
            )
        }
    }
}

@Composable
internal fun LibraryBatchDeleteDialog(
    selectedCount: Int,
    onDeleteLibraryOnly: () -> Unit,
    onDeleteWithLocalData: () -> Unit,
    onDismiss: () -> Unit,
) {
    TankobunDialog(onDismiss = onDismiss) {
        TankobunDialogHeader(title = tankobunString(R.string.library_batch_delete_title), onDismiss = onDismiss)
        Text(
            tankobunString(R.string.library_batch_delete_desc, selectedCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TankobunActionButton(
                label = tankobunString(R.string.library_batch_delete_library_only),
                icon = TankobunIcons.Delete,
                filled = false,
                modifier = Modifier.fillMaxWidth(),
                onClick = onDeleteLibraryOnly,
            )
            TankobunActionButton(
                label = tankobunString(R.string.library_batch_delete_with_local_data),
                icon = TankobunIcons.Delete,
                modifier = Modifier.fillMaxWidth(),
                onClick = onDeleteWithLocalData,
            )
        }
    }
}

@Composable
internal fun RecommendationImportDialog(
    state: TankobunUiState,
    viewModel: MainViewModel,
) {
    val preview = state.recommendationImportPreview ?: return
    val selectedCount = state.selectedRecommendationImportMediaIds.size
    TankobunDialog(
        onDismiss = viewModel::dismissRecommendationImport,
        maxWidth = 680.dp,
        fillMaxHeightFraction = 0.86f,
        scrollable = false,
    ) {
        TankobunDialogHeader(
            title = tankobunString(R.string.recommendations_import_title),
            onDismiss = viewModel::dismissRecommendationImport,
        )
        Text(
            tankobunString(R.string.recommendations_import_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.recommendationImportListName,
            onValueChange = viewModel::setRecommendationImportListName,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(tankobunString(R.string.library_batch_list_name)) },
            shape = LocalTankobunStyle.current.themeShapes.control,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                tankobunString(
                    R.string.recommendations_import_selected_count,
                    selectedCount,
                    preview.items.size,
                ),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { viewModel.setAllRecommendationImportItemsSelected(true) }) {
                    Text(tankobunString(R.string.recommendations_import_select_all))
                }
                TextButton(onClick = { viewModel.setAllRecommendationImportItemsSelected(false) }) {
                    Text(tankobunString(R.string.recommendations_import_select_none))
                }
            }
            if (state.recommendationImportLoadingDetails) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        tankobunString(R.string.recommendations_import_loading_details),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (preview.items.isEmpty()) {
                    Text(
                        tankobunString(R.string.recommendations_import_empty),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = preview.items,
                            key = { item -> item.media.id },
                        ) { item ->
                            val selected = item.media.id in state.selectedRecommendationImportMediaIds
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(LocalTankobunStyle.current.themeShapes.panel)
                                    .clickable { viewModel.toggleRecommendationImportItem(item.media.id) },
                                shape = LocalTankobunStyle.current.themeShapes.panel,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) {
                                        LocalTankobunStyle.current.colors.accent.copy(alpha = 0.58f)
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                                    },
                                ),
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            item.media.title.userPreferred,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    },
                                    supportingContent = {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                listOfNotNull(
                                                    item.media.mediaTypeLabel(),
                                                    item.media.startDateYear?.toString(),
                                                ).joinToString(" / "),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            if (item.alreadyInLibrary) {
                                                Text(
                                                    tankobunString(R.string.recommendations_import_existing),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = LocalTankobunStyle.current.colors.accent,
                                                )
                                            }
                                            item.message?.takeIf { it.isNotBlank() }?.let { message ->
                                                Text(
                                                    tankobunString(R.string.recommendations_import_message_label),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                Text(
                                                    message,
                                                    maxLines = 4,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    },
                                    leadingContent = {
                                        CoverImage(
                                            url = item.media.coverImage,
                                            title = item.media.title.userPreferred,
                                            modifier = Modifier.size(width = 46.dp, height = 66.dp),
                                        )
                                    },
                                    trailingContent = {
                                        Checkbox(
                                            checked = selected,
                                            onCheckedChange = { viewModel.toggleRecommendationImportItem(item.media.id) },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = viewModel::dismissRecommendationImport) {
                Text(tankobunString(R.string.common_cancel))
            }
            Spacer(Modifier.weight(1f))
            TankobunActionButton(
                label = tankobunString(R.string.recommendations_import_button),
                icon = TankobunIcons.PlaylistAdd,
                enabled = selectedCount > 0 && state.recommendationImportListName.trim().isNotBlank(),
                onClick = viewModel::importSelectedRecommendations,
            )
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
                            leadingIcon = { TankobunChipIcon(genreIcon(genre)) },
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
                                leadingIcon = { TankobunChipIcon(TankobunIcons.LocalOffer) },
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
