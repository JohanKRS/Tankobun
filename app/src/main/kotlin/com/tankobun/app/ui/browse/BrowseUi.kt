package com.tankobun.app.ui.browse

import com.tankobun.app.ui.filters.*
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
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.unit.sp
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

internal enum class BrowsePicker {
    FORMAT,
    STATUS,
    COUNTRY,
    YEAR,
}

internal data class BrowseOption(
    val label: String,
    val value: String?,
    @StringRes val labelRes: Int? = null,
) {
    constructor(@StringRes labelRes: Int, value: String?) : this("", value, labelRes)
}

internal const val BROWSE_SORT_SEARCH_MATCH_UI = "SEARCH_MATCH"
internal const val WEBTOON_RESTORE_MAX_ATTEMPTS = 18
internal const val WEBTOON_RESTORE_RETRY_DELAY_MILLIS = 80L

internal val BrowseGenres = listOf(
    "Action",
    "Adventure",
    "Comedy",
    "Drama",
    "Ecchi",
    "Fantasy",
    "Horror",
    "Mahou Shoujo",
    "Mecha",
    "Music",
    "Mystery",
    "Psychological",
    "Romance",
    "Sci-Fi",
    "Slice of Life",
    "Sports",
    "Supernatural",
    "Thriller",
)

internal val BrowseFormatOptions = listOf(
    BrowseOption(R.string.common_any, null),
    BrowseOption(R.string.media_type_manga, "MANGA"),
    BrowseOption(R.string.media_type_novel, "NOVEL"),
    BrowseOption(R.string.media_type_one_shot, "ONE_SHOT"),
)

internal val BrowseStatusOptions = listOf(
    BrowseOption(R.string.common_any, null),
    BrowseOption(R.string.publishing_releasing, "RELEASING"),
    BrowseOption(R.string.publishing_finished, "FINISHED"),
    BrowseOption(R.string.publishing_not_yet_released, "NOT_YET_RELEASED"),
    BrowseOption(R.string.publishing_cancelled, "CANCELLED"),
    BrowseOption(R.string.publishing_hiatus, "HIATUS"),
)

internal val BrowseCountryOptions = listOf(
    BrowseOption(R.string.common_any, null),
    BrowseOption(R.string.country_japan, "JP"),
    BrowseOption(R.string.country_south_korea, "KR"),
    BrowseOption(R.string.country_china, "CN"),
    BrowseOption(R.string.country_taiwan, "TW"),
)

internal val BrowseSortOptions = listOf(
    BrowseOption(R.string.browse_sort_search_match, "SEARCH_MATCH"),
    BrowseOption(R.string.browse_sort_trending, "TRENDING_DESC"),
    BrowseOption(R.string.browse_sort_popularity, "POPULARITY_DESC"),
    BrowseOption(R.string.browse_sort_favorites, "FAVOURITES_DESC"),
    BrowseOption(R.string.browse_sort_average_score, "SCORE_DESC"),
    BrowseOption(R.string.browse_sort_recently_updated, "UPDATED_AT_DESC"),
    BrowseOption(R.string.browse_sort_newest, "START_DATE_DESC"),
    BrowseOption(R.string.browse_sort_title, "TITLE_ROMAJI"),
)

@Composable
internal fun BrowseScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadBrowseLanding()
        viewModel.loadBrowseTags()
    }

    var picker by remember { mutableStateOf<BrowsePicker?>(null) }
    var genresOpen by remember { mutableStateOf(false) }
    var tagsOpen by remember { mutableStateOf(false) }
    var advancedOpen by remember { mutableStateOf(false) }
    val controlsActive = state.browseControlsActive()
    val trackedStatuses = remember(state.libraryItems) { state.libraryItems.trackedMediaStatuses() }
    val selectedLibraryItems = remember(state.libraryItems, state.selectedLibraryMediaIds, state.selectedLibraryBatchMedia) {
        state.selectedLibraryBatchItems()
    }
    val selectedCount = state.selectedLibraryMediaIds.size
    val chromeInsets = LocalTankobunChromeInsets.current

    BackHandler(enabled = selectedCount > 0) {
        viewModel.clearLibraryBatchSelection()
    }

    val browseHeader: @Composable (Dp) -> Unit = { horizontalPadding ->
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BrowseFilterBar(
                state = state,
                viewModel = viewModel,
                horizontalPadding = horizontalPadding,
                onOpenGenres = { genresOpen = true },
                onOpenTags = { tagsOpen = true },
                onOpenPicker = { picker = it },
                onOpenAdvanced = { advancedOpen = true },
            )

            state.message?.let {
                Box(Modifier.padding(horizontal = horizontalPadding)) {
                    TankobunMessageBanner(it)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (state.browseForYouOpen) {
            BrowseForYouResults(
                state = state,
                viewModel = viewModel,
                trackedStatuses = trackedStatuses,
                onSelectMedia = onSelectMedia,
            )
        } else if (controlsActive || state.browseSearched) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
            ) {
                BrowseResults(
                    state = state,
                    viewModel = viewModel,
                    onSelectMedia = onSelectMedia,
                    trackedStatuses = trackedStatuses,
                    selectedMediaIds = state.selectedLibraryMediaIds,
                    selectionMode = selectedCount > 0,
                    onToggleMediaSelection = viewModel::toggleLibraryBatchSelection,
                    onLongPressMedia = viewModel::startLibraryBatchSelection,
                    modifier = Modifier.fillMaxSize(),
                    header = { browseHeader(0.dp) },
                )
            }
        } else {
            BrowseLanding(
                state = state,
                viewModel = viewModel,
                onSelectMedia = onSelectMedia,
                trackedStatuses = trackedStatuses,
                selectedMediaIds = state.selectedLibraryMediaIds,
                selectionMode = selectedCount > 0,
                onToggleMediaSelection = viewModel::toggleLibraryBatchSelection,
                onLongPressMedia = viewModel::startLibraryBatchSelection,
                modifier = Modifier.fillMaxSize(),
                header = { browseHeader(BrowseLandingContentPadding) },
            )
        }

        AnimatedVisibility(
            visible = selectedCount > 0,
            modifier = Modifier
                .align(state.dockAlignment.browseBatchBarAlignment())
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

        if (genresOpen) {
            BrowseGenreDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { genresOpen = false },
            )
        }

        if (tagsOpen) {
            BrowseTagDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { tagsOpen = false },
            )
        }

        picker?.let { activePicker ->
            if (activePicker == BrowsePicker.YEAR) {
                FilterYearDialog(selected = state.browseSelection.years, onApply = {
                    viewModel.setBrowseYears(it)
                    viewModel.searchAniList()
                }, onDismiss = { picker = null })
            } else FilterChoiceDialog(
                title = when (activePicker) {
                    BrowsePicker.FORMAT -> tankobunString(R.string.common_format)
                    BrowsePicker.STATUS -> tankobunString(R.string.browse_publishing_status)
                    else -> tankobunString(R.string.browse_country_of_origin)
                },
                options = when (activePicker) {
                    BrowsePicker.FORMAT -> BrowseFormatOptions
                    BrowsePicker.STATUS -> BrowseStatusOptions
                    else -> BrowseCountryOptions
                },
                selected = when (activePicker) {
                    BrowsePicker.FORMAT -> state.browseSelection.formats
                    BrowsePicker.STATUS -> state.browseSelection.statuses
                    else -> state.browseSelection.countries
                },
                onApply = { values ->
                    when (activePicker) {
                        BrowsePicker.FORMAT -> viewModel.setBrowseFormats(values)
                        BrowsePicker.STATUS -> viewModel.setBrowseStatuses(values)
                        else -> viewModel.setBrowseCountries(values)
                    }
                    viewModel.searchAniList()
                },
                onDismiss = { picker = null },
            )
        }

        if (advancedOpen) {
            BrowseAdvancedDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { advancedOpen = false },
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
}

@Composable
internal fun BrowseFilterBar(
    state: TankobunUiState,
    viewModel: MainViewModel,
    horizontalPadding: Dp = BrowseLandingContentPadding,
    onOpenGenres: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenPicker: (BrowsePicker) -> Unit,
    onOpenAdvanced: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.padding(horizontal = horizontalPadding)) {
            TankobunSearchField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = tankobunString(R.string.browse_search_placeholder),
                onSearch = viewModel::searchAniList,
                showSearchAction = false,
            )
        }
        TankobunHorizontalFilterRow(contentPadding = PaddingValues(horizontal = horizontalPadding)) {
            item {
                BrowseFilterPill(
                    label = tankobunString(R.string.common_genres),
                    value = if (state.browseGenres.isEmpty()) tankobunString(R.string.common_any) else state.browseGenres.size.toString(),
                    selected = state.browseGenres.isNotEmpty(),
                    icon = TankobunIcons.Category,
                    onClick = onOpenGenres,
                )
            }
            item {
                BrowseFilterPill(
                    label = tankobunString(R.string.common_tags),
                    value = if (state.browseTags.isEmpty()) tankobunString(R.string.common_any) else state.browseTags.size.toString(),
                    selected = state.browseTags.isNotEmpty(),
                    icon = TankobunIcons.LocalOffer,
                    onClick = onOpenTags,
                )
            }
            item {
                BrowseFilterPill(
                    label = tankobunString(R.string.common_format),
                    value = BrowseFormatOptions.selectionLabel(state.browseSelection.formats),
                    selected = state.browseSelection.formats.isNotEmpty(),
                    icon = TankobunIcons.MenuBook,
                    onClick = { onOpenPicker(BrowsePicker.FORMAT) },
                )
            }
            item {
                BrowseFilterPill(
                    label = tankobunString(R.string.common_status),
                    value = BrowseStatusOptions.selectionLabel(state.browseSelection.statuses),
                    selected = state.browseSelection.statuses.isNotEmpty(),
                    icon = TankobunIcons.Flag,
                    onClick = { onOpenPicker(BrowsePicker.STATUS) },
                )
            }
            item {
                BrowseFilterPill(
                    label = tankobunString(R.string.common_country),
                    value = BrowseCountryOptions.selectionLabel(state.browseSelection.countries),
                    selected = state.browseSelection.countries.isNotEmpty(),
                    icon = TankobunIcons.Public,
                    onClick = { onOpenPicker(BrowsePicker.COUNTRY) },
                )
            }
            item {
                BrowseFilterPill(
                    label = tankobunString(R.string.common_year),
                    value = state.browseSelection.years.filterLabel(),
                    selected = state.browseSelection.years != null,
                    icon = TankobunIcons.CalendarMonth,
                    onClick = { onOpenPicker(BrowsePicker.YEAR) },
                )
            }
            item {
                BrowseIconFilterPill(
                    contentDescription = tankobunString(R.string.browse_options),
                    onClick = onOpenAdvanced,
                )
            }
            if (state.hasBrowseQueryOrFilters() || state.browseSearched) {
                item {
                    TankobunClearFiltersChip(onClick = viewModel::resetBrowseFilters)
                }
            }
        }
    }
}

@Composable
internal fun BrowseFilterPill(
    label: String,
    value: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    TankobunChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = { TankobunChipIcon(icon) },
        label = {
            Text(
                if (selected) "$label: $value" else label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
internal fun BrowseIconFilterPill(
    contentDescription: String,
    onClick: () -> Unit,
) {
    TankobunChip(
        selected = false,
        onClick = onClick,
        label = {
            Icon(
                TankobunIcons.Tune,
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

@Composable
internal fun BrowseLanding(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onSelectMedia: (AnilistMedia) -> Unit,
    trackedStatuses: Map<Int, MediaStatus>,
    selectedMediaIds: Set<Int>,
    selectionMode: Boolean,
    onToggleMediaSelection: (AnilistMedia) -> Unit,
    onLongPressMedia: (AnilistMedia) -> Unit,
    modifier: Modifier,
    header: @Composable () -> Unit,
) {
    val chromeInsets = LocalTankobunChromeInsets.current
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            top = chromeInsets.top + BrowseLandingContentPadding,
            bottom = chromeInsets.bottom + BrowseLandingContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(BrowseHeaderTextGap),
    ) {
        item(key = "browse-header") {
            header()
        }
        item {
            BrowseMangaShelf(
                title = tankobunString(R.string.browse_trending_now),
                media = state.browseTrending,
                trackedStatuses = trackedStatuses,
                selectedMediaIds = selectedMediaIds,
                selectionMode = selectionMode,
                onViewAll = { viewModel.viewAllBrowseSection("TRENDING_DESC") },
                onSelectMedia = onSelectMedia,
                onToggleMediaSelection = onToggleMediaSelection,
                onLongPressMedia = onLongPressMedia,
            )
        }
        if (state.browseForYou.isNotEmpty()) {
            item(key = "mangabaka-mix") {
                BrowseMangaShelf(
                    title = tankobunString(R.string.catalog_for_you),
                    media = state.browseForYou,
                    trackedStatuses = trackedStatuses,
                    selectedMediaIds = selectedMediaIds,
                    selectionMode = selectionMode,
                    onViewAll = viewModel::viewAllBrowseForYou,
                    onSelectMedia = onSelectMedia,
                    onToggleMediaSelection = onToggleMediaSelection,
                    onLongPressMedia = onLongPressMedia,
                )
            }
        }
        item {
            BrowseMangaShelf(
                title = tankobunString(R.string.browse_all_time_popular),
                media = state.browsePopular,
                trackedStatuses = trackedStatuses,
                selectedMediaIds = selectedMediaIds,
                selectionMode = selectionMode,
                onViewAll = { viewModel.viewAllBrowseSection("POPULARITY_DESC") },
                onSelectMedia = onSelectMedia,
                onToggleMediaSelection = onToggleMediaSelection,
                onLongPressMedia = onLongPressMedia,
            )
        }
        item {
            BrowseMangaShelf(
                title = tankobunString(R.string.browse_popular_manhwa),
                media = state.browsePopularManhwa,
                trackedStatuses = trackedStatuses,
                selectedMediaIds = selectedMediaIds,
                selectionMode = selectionMode,
                onViewAll = viewModel::viewAllPopularManhwa,
                onSelectMedia = onSelectMedia,
                onToggleMediaSelection = onToggleMediaSelection,
                onLongPressMedia = onLongPressMedia,
            )
        }
        item {
            BrowseMangaShelf(
                title = tankobunString(R.string.browse_top_100_manga),
                media = state.browseTopManga,
                trackedStatuses = trackedStatuses,
                selectedMediaIds = selectedMediaIds,
                selectionMode = selectionMode,
                onViewAll = { viewModel.viewAllBrowseSection("SCORE_DESC") },
                onSelectMedia = onSelectMedia,
                onToggleMediaSelection = onToggleMediaSelection,
                onLongPressMedia = onLongPressMedia,
            )
        }
    }
}

private val BrowseLandingContentPadding = 18.dp
private val BrowseHeaderTextGap = 24.dp
private val BrowseShelfTitleHeight = 62.dp
private val BrowseShelfItemGap = 12.dp

private fun DockAlignment.browseBatchBarAlignment(): Alignment =
    when (this) {
        DockAlignment.LEFT -> Alignment.BottomStart
        DockAlignment.RIGHT -> Alignment.BottomEnd
        DockAlignment.CENTER -> Alignment.BottomCenter
    }

@Composable
internal fun BrowseMangaShelf(
    title: String,
    media: List<AnilistMedia>,
    trackedStatuses: Map<Int, MediaStatus>,
    selectedMediaIds: Set<Int>,
    selectionMode: Boolean,
    onViewAll: (() -> Unit)?,
    onSelectMedia: (AnilistMedia) -> Unit,
    onToggleMediaSelection: (AnilistMedia) -> Unit,
    onLongPressMedia: (AnilistMedia) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TankobunSectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = BrowseLandingContentPadding),
            actionLabel = if (onViewAll != null) tankobunString(R.string.browse_view_all) else null,
            onAction = onViewAll,
        )
        AnimatedContent(
            targetState = media.take(BROWSE_LANDING_SECTION_SIZE),
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 220)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 160))
            },
            label = "BrowseShelfRefresh",
        ) { shelfMedia ->
            if (shelfMedia.isEmpty()) {
                Text(
                    tankobunString(R.string.browse_cached_discovery_empty),
                    modifier = Modifier.padding(horizontal = BrowseLandingContentPadding),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = BrowseLandingContentPadding),
                    horizontalArrangement = Arrangement.spacedBy(BrowseShelfItemGap),
                ) {
                    items(shelfMedia, key = { it.id }) { item ->
                        BrowseShelfTile(
                            media = item,
                            trackedStatus = trackedStatuses[item.id],
                            selected = item.id in selectedMediaIds,
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) {
                                    onToggleMediaSelection(item)
                                } else {
                                    onSelectMedia(item)
                                }
                            },
                            onLongClick = { onLongPressMedia(item) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BrowseShelfTile(
    media: AnilistMedia,
    trackedStatus: MediaStatus?,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(190.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 1.dp,
                shadowElevation = 2.dp,
                border = if (selected) BorderStroke(2.dp, LocalTankobunStyle.current.colors.accent) else null,
            ) {
                TrackedCoverImage(
                    url = media.coverImage,
                    title = media.title.userPreferred,
                    trackedStatus = trackedStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f),
                )
            }
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                )
            }
        }
        Column(
            modifier = Modifier.height(BrowseShelfTitleHeight),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = media.title.userPreferred,
                style = MaterialTheme.typography.titleSmall.copy(lineHeight = 16.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TankobunMediaStatusLabel(text = media.status.statusLabel())
        }
    }
}

@Composable
private fun BrowseForYouResults(
    state: TankobunUiState,
    viewModel: MainViewModel,
    trackedStatuses: Map<Int, MediaStatus>,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    val chromeInsets = LocalTankobunChromeInsets.current
    MediaCollection(
        media = state.browseForYou,
        viewMode = state.browseViewMode,
        coverColumns = state.browseCoverColumns,
        showWholeCovers = state.browseShowWholeCovers,
        trackedStatuses = trackedStatuses,
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = chromeInsets.top + 18.dp, bottom = chromeInsets.bottom + 18.dp),
        header = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tankobunString(R.string.catalog_for_you),
                        modifier = Modifier.weight(1f),
                        style = LocalTankobunStyle.current.typography.sectionLabel,
                        color = LocalTankobunStyle.current.colors.accent,
                    )
                    IconButton(
                        onClick = viewModel::refreshBrowseForYou,
                        enabled = !state.browseForYouRefreshing,
                    ) {
                        if (state.browseForYouRefreshing) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(TankobunIcons.Refresh, tankobunString(R.string.catalog_refresh_recommendations))
                        }
                    }
                }
                Text(
                    tankobunString(R.string.catalog_for_you_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onSelectMedia = onSelectMedia,
        selectedMediaIds = state.selectedLibraryMediaIds,
        selectionMode = state.selectedLibraryMediaIds.isNotEmpty(),
        onToggleMediaSelection = viewModel::toggleLibraryBatchSelection,
        onLongPressMedia = viewModel::startLibraryBatchSelection,
        emptyMessage = tankobunString(R.string.browse_no_results),
    )
}

@Composable
internal fun BrowseResults(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onSelectMedia: (AnilistMedia) -> Unit,
    trackedStatuses: Map<Int, MediaStatus>,
    selectedMediaIds: Set<Int>,
    selectionMode: Boolean,
    onToggleMediaSelection: (AnilistMedia) -> Unit,
    onLongPressMedia: (AnilistMedia) -> Unit,
    modifier: Modifier,
    header: @Composable () -> Unit,
) {
    val chromeInsets = LocalTankobunChromeInsets.current
    val resultsHeader: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(BrowseHeaderTextGap)) {
            header()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when {
                        state.browseStaffName != null -> tankobunString(R.string.browse_author_results)
                        state.searchQuery.isBlank() -> tankobunString(R.string.browse_manga)
                        else -> tankobunString(R.string.browse_search_results)
                    },
                    style = LocalTankobunStyle.current.typography.sectionLabel,
                    color = LocalTankobunStyle.current.colors.accent,
                )
                Text(
                    browseSummary(state),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    }
    MediaCollection(
        media = state.searchResults,
        viewMode = state.browseViewMode,
        coverColumns = state.browseCoverColumns,
        showWholeCovers = state.browseShowWholeCovers,
        trackedStatuses = trackedStatuses,
        modifier = modifier.fillMaxWidth(),
        header = resultsHeader,
        contentPadding = PaddingValues(
            top = chromeInsets.top + 18.dp,
            bottom = chromeInsets.bottom + 18.dp,
        ),
        onSelectMedia = onSelectMedia,
        selectedMediaIds = selectedMediaIds,
        selectionMode = selectionMode,
        onToggleMediaSelection = onToggleMediaSelection,
        onLongPressMedia = onLongPressMedia,
        isLoadingMore = state.browseResultsLoadingMore,
        onNearEnd = if (state.browseResultsHasMore) viewModel::loadMoreBrowseResults else null,
        emptyMessage = if (state.busy) {
            tankobunString(R.string.browse_searching_anilist)
        } else {
            tankobunString(R.string.browse_no_results)
        },
    )
}

@Composable
internal fun BrowseGenreDialog(state: TankobunUiState, viewModel: MainViewModel, onDismiss: () -> Unit) {
    CatalogFilterDialog(
        title = tankobunString(R.string.common_genres), taxonomy = state.catalogTaxonomy,
        options = state.catalogTaxonomy.tags, selected = state.browseGenres, includeAdult = state.showNsfwContent,
        genresOnly = true, loading = state.catalogTaxonomyLoading,
        onRefresh = { viewModel.loadBrowseTags(force = true) },
        onApply = viewModel::applyBrowseGenres, onDismiss = onDismiss,
    )
}

@Composable
internal fun BrowseTagDialog(state: TankobunUiState, viewModel: MainViewModel, onDismiss: () -> Unit) {
    CatalogFilterDialog(
        title = tankobunString(R.string.common_tags), taxonomy = state.catalogTaxonomy,
        options = state.catalogTaxonomy.tags, selected = state.browseTags, includeAdult = state.showNsfwContent,
        loading = state.catalogTaxonomyLoading, onRefresh = { viewModel.loadBrowseTags(force = true) },
        onApply = viewModel::applyBrowseTags, onDismiss = onDismiss,
    )
}

@Composable
internal fun LibraryOptionsDialog(
    state: TankobunUiState, viewModel: MainViewModel, sort: String,
    onSortChange: (String?) -> Unit, onReset: () -> Unit, onDismiss: () -> Unit,
) {
    MediaFilterOptionsDialog(
        title = tankobunString(R.string.browse_library_options), sortOptions = LibrarySortOptions,
        selectedSort = sort, onSortChange = onSortChange, sortIcon = { it.librarySortIcon() },
        viewMode = state.libraryViewMode, onViewMode = viewModel::setLibraryViewMode,
        coverColumns = state.libraryCoverColumns, onCoverColumns = viewModel::setLibraryCoverColumns,
        showWholeCovers = state.libraryShowWholeCovers, onWholeCovers = viewModel::setLibraryShowWholeCovers,
        onReset = onReset, onApply = onDismiss, onDismiss = onDismiss,
    )
}

@Composable
internal fun BrowseAdvancedDialog(state: TankobunUiState, viewModel: MainViewModel, onDismiss: () -> Unit) {
    MediaFilterOptionsDialog(
        title = tankobunString(R.string.browse_browse_options), sortOptions = BrowseSortOptions,
        selectedSort = state.browseSort, onSortChange = { it?.let(viewModel::setBrowseSort) }, sortIcon = { it.browseSortIcon() },
        viewMode = state.browseViewMode, onViewMode = viewModel::setBrowseViewMode,
        coverColumns = state.browseCoverColumns, onCoverColumns = viewModel::setBrowseCoverColumns,
        showWholeCovers = state.browseShowWholeCovers, onWholeCovers = viewModel::setBrowseShowWholeCovers,
        onReset = viewModel::resetBrowseFilters, onApply = { onDismiss(); viewModel.searchAniList() }, onDismiss = onDismiss,
    )
}

@Composable
internal fun CoverColumnsRow(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val maxColumns = if (configuration.smallestScreenWidthDp in 1 until 600) 4 else 8
    val selectedColumns = selected.supportedCoverColumns().coerceAtMost(maxColumns)
    FlowRowCompat {
        (2..maxColumns).forEach { count ->
            TankobunChip(
                selected = selectedColumns == count,
                onClick = { onSelect(count) },
                leadingIcon = { TankobunChipIcon(TankobunIcons.GridView) },
                label = { Text(count.toString()) },
                modifier = modifier,
            )
        }
    }
}

@Composable
internal fun CoverFramingRow(
    showWholeCover: Boolean,
    onShowWholeCoverChange: (Boolean) -> Unit,
) {
    FlowRowCompat {
        TankobunChip(
            selected = !showWholeCover,
            onClick = { onShowWholeCoverChange(false) },
            leadingIcon = { TankobunChipIcon(TankobunIcons.Crop) },
            label = { Text(tankobunString(R.string.cover_framing_fill)) },
        )
        TankobunChip(
            selected = showWholeCover,
            onClick = { onShowWholeCoverChange(true) },
            leadingIcon = { TankobunChipIcon(TankobunIcons.FitScreen) },
            label = { Text(tankobunString(R.string.cover_framing_whole)) },
        )
    }
}

private fun BrowseOption.librarySortIcon(): ImageVector =
    when (value) {
        LIBRARY_SORT_LIST_ORDER -> TankobunIcons.Label
        LIBRARY_SORT_TITLE -> TankobunIcons.SortByAlpha
        LIBRARY_SORT_UPDATED -> TankobunIcons.Refresh
        LIBRARY_SORT_PROGRESS -> TankobunIcons.PlayArrow
        LIBRARY_SORT_SCORE -> TankobunIcons.Star
        else -> TankobunIcons.Sort
    }

private fun BrowseOption.browseSortIcon(): ImageVector =
    when (value) {
        BROWSE_SORT_SEARCH_MATCH_UI -> TankobunIcons.Search
        "TRENDING_DESC" -> TankobunIcons.TrendingUp
        "POPULARITY_DESC" -> TankobunIcons.Whatshot
        "FAVOURITES_DESC" -> TankobunIcons.Star
        "SCORE_DESC" -> TankobunIcons.Check
        "UPDATED_AT_DESC" -> TankobunIcons.Refresh
        "START_DATE_DESC" -> TankobunIcons.CalendarMonth
        "TITLE_ROMAJI" -> TankobunIcons.SortByAlpha
        else -> TankobunIcons.Sort
    }

@Composable
internal fun BrowseOption.labelText(): String =
    labelRes?.let { tankobunString(it) } ?: label

@Composable
internal fun List<BrowseOption>.labelFor(value: String?): String =
    firstOrNull { it.value == value }?.labelText() ?: tankobunString(R.string.common_any)

internal fun List<AnilistMediaTag>.visibleTags(
    query: String,
    includeAdult: Boolean,
): List<AnilistMediaTag> {
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    return asSequence()
        .filter { includeAdult || !it.isAdult }
        .filter { tag ->
            normalizedQuery.isBlank() ||
                tag.name.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                tag.category.orEmpty().lowercase(Locale.ROOT).contains(normalizedQuery)
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        .toList()
}

internal fun TankobunUiState.browseControlsActive(): Boolean =
    browseFiltersOrSortActive()

internal fun TankobunUiState.browseFiltersOrSortActive(): Boolean =
    browseGenres.isNotEmpty() ||
        browseTags.isNotEmpty() ||
        browseSelection.isActive ||
        browseStaffName != null ||
        browseSort != BROWSE_SORT_SEARCH_MATCH_UI

@Composable
internal fun browseSummary(state: TankobunUiState): String {
    val parts = mutableListOf<String>()
    state.searchQuery.trim().takeIf { it.isNotBlank() }?.let {
        parts += tankobunString(R.string.browse_summary_search, it)
    }
    state.browseStaffName?.let {
        parts += tankobunString(R.string.browse_summary_author, it)
    }
    if (state.browseGenres.isNotEmpty()) {
        val genreLabels = mutableListOf<String>()
        state.browseGenres.sorted().forEach { genre ->
            genreLabels += browseGenreLabel(state.catalogTaxonomy.label(genre))
        }
        parts += genreLabels.joinToString(", ")
    }
    if (state.browseTags.isNotEmpty()) parts += state.browseTags.map(state.catalogTaxonomy::label).sorted().joinToString(", ")
    state.browseSelection.formats.sorted().forEach { parts += BrowseFormatOptions.labelFor(it) }
    state.browseSelection.statuses.sorted().forEach { parts += BrowseStatusOptions.labelFor(it) }
    state.browseSelection.countries.sorted().forEach { parts += BrowseCountryOptions.labelFor(it) }
    state.browseSelection.years?.let { parts += it.filterLabel() }
    val defaultSortLabel = tankobunString(R.string.browse_sort_search_match)
    val selectedSortLabel = BrowseSortOptions.labelFor(state.browseSort)
    if (selectedSortLabel != defaultSortLabel) {
        parts += tankobunString(R.string.browse_summary_sort, selectedSortLabel)
    }
    return parts.ifEmpty { listOf(tankobunString(R.string.browse_summary_default)) }.joinToString(" / ")
}

@Composable
internal fun String?.statusColor(): Color = when (this) {
    "RELEASING" -> Color(0xFF7ED957)
    "FINISHED" -> Color(0xFFFF7A7A)
    "HIATUS" -> Color(0xFFFFB15C)
    "NOT_YET_RELEASED" -> MaterialTheme.colorScheme.tertiary
    "CANCELLED" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@StringRes
internal fun String?.publishingStatusLabelRes(): Int = when (this) {
    "RELEASING" -> R.string.publishing_releasing
    "FINISHED" -> R.string.publishing_finished
    "HIATUS" -> R.string.publishing_hiatus
    "NOT_YET_RELEASED" -> R.string.publishing_not_yet_released
    "CANCELLED" -> R.string.publishing_cancelled
    else -> R.string.publishing_unknown
}

@Composable
internal fun String?.statusLabel(): String =
    tankobunString(publishingStatusLabelRes())

@StringRes
internal fun String?.mediaFormatLabelRes(): Int = when (this) {
    "MANGA" -> R.string.media_type_manga
    "NOVEL" -> R.string.media_type_novel
    "ONE_SHOT" -> R.string.media_type_one_shot
    else -> R.string.media_type_manga
}

@Composable
internal fun AnilistMedia.mediaTypeLabel(): String = when (countryOfOrigin?.uppercase(Locale.ROOT)) {
    "KR" -> tankobunString(R.string.media_type_manhwa)
    "CN", "TW", "HK" -> tankobunString(R.string.media_type_manhua)
    else -> tankobunString(format.mediaFormatLabelRes())
}

@Composable
internal fun String?.mediaFormatLabel(): String =
    tankobunString(mediaFormatLabelRes())

@Composable
internal fun AnilistMedia.publishingYearLabel(): String {
    val startYear = startDateYear
    val endYear = endDateYear
    return when {
        startYear != null && endYear != null && startYear != endYear -> "$startYear - $endYear"
        startYear != null && status == "RELEASING" -> tankobunString(R.string.media_year_since, startYear)
        startYear != null -> startYear.toString()
        else -> tankobunString(R.string.media_date_unknown)
    }
}

@Composable
internal fun List<String>.authorLabel(): String =
    take(3).joinToString(", ").ifBlank { tankobunString(R.string.media_unknown_author) }

@Composable
internal fun browseGenreLabel(genre: String): String {
    val labelRes = when (genre) {
        "Action" -> R.string.browse_genre_action
        "Adventure" -> R.string.browse_genre_adventure
        "Comedy" -> R.string.browse_genre_comedy
        "Drama" -> R.string.browse_genre_drama
        "Ecchi" -> R.string.browse_genre_ecchi
        "Fantasy" -> R.string.browse_genre_fantasy
        "Horror" -> R.string.browse_genre_horror
        "Mahou Shoujo" -> R.string.browse_genre_mahou_shoujo
        "Mecha" -> R.string.browse_genre_mecha
        "Music" -> R.string.browse_genre_music
        "Mystery" -> R.string.browse_genre_mystery
        "Psychological" -> R.string.browse_genre_psychological
        "Romance" -> R.string.browse_genre_romance
        "Sci-Fi" -> R.string.browse_genre_sci_fi
        "Slice of Life" -> R.string.browse_genre_slice_of_life
        "Sports" -> R.string.browse_genre_sports
        "Supernatural" -> R.string.browse_genre_supernatural
        "Thriller" -> R.string.browse_genre_thriller
        "Martial Arts" -> R.string.browse_genre_martial_arts
        "Historical" -> R.string.browse_genre_historical
        "Tragedy" -> R.string.browse_genre_tragedy
        else -> null
    }
    return labelRes?.let { tankobunString(it) } ?: genre
}

internal fun Int.formatCompact(): String =
    when {
        this >= 1_000_000 -> "${this / 1_000_000}m"
        this >= 10_000 -> "${this / 1_000}k"
        else -> toString()
    }
