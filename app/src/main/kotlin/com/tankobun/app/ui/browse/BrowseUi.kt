package com.tankobun.app.ui.browse

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

internal enum class BrowsePicker {
    FORMAT,
    STATUS,
    COUNTRY,
    YEAR,
}

internal data class BrowseOption(
    val label: String,
    val value: String?,
)

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
    BrowseOption("Any", null),
    BrowseOption("Manga", "MANGA"),
    BrowseOption("Novel", "NOVEL"),
    BrowseOption("One Shot", "ONE_SHOT"),
)

internal val BrowseStatusOptions = listOf(
    BrowseOption("Any", null),
    BrowseOption("Releasing", "RELEASING"),
    BrowseOption("Finished", "FINISHED"),
    BrowseOption("Not Yet Released", "NOT_YET_RELEASED"),
    BrowseOption("Cancelled", "CANCELLED"),
    BrowseOption("Hiatus", "HIATUS"),
)

internal val BrowseCountryOptions = listOf(
    BrowseOption("Any", null),
    BrowseOption("Japan", "JP"),
    BrowseOption("South Korea", "KR"),
    BrowseOption("China", "CN"),
    BrowseOption("Taiwan", "TW"),
)

internal val BrowseSortOptions = listOf(
    BrowseOption("Search Match", "SEARCH_MATCH"),
    BrowseOption("Trending", "TRENDING_DESC"),
    BrowseOption("Popularity", "POPULARITY_DESC"),
    BrowseOption("Favorites", "FAVOURITES_DESC"),
    BrowseOption("Average Score", "SCORE_DESC"),
    BrowseOption("Recently Updated", "UPDATED_AT_DESC"),
    BrowseOption("Newest", "START_DATE_DESC"),
    BrowseOption("Title", "TITLE_ROMAJI"),
)

@Composable
internal fun BrowseScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.loadBrowseLanding()
        viewModel.loadBrowseTags()
    }

    var picker by remember { mutableStateOf<BrowsePicker?>(null) }
    var genresOpen by remember { mutableStateOf(false) }
    var tagsOpen by remember { mutableStateOf(false) }
    var advancedOpen by remember { mutableStateOf(false) }
    val controlsActive = state.browseControlsActive()
    val browseHeader: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BrowseFilterBar(
                state = state,
                viewModel = viewModel,
                onOpenGenres = { genresOpen = true },
                onOpenTags = { tagsOpen = true },
                onOpenPicker = { picker = it },
                onOpenAdvanced = { advancedOpen = true },
            )

            state.message?.let {
                TankobunMessageBanner(it)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (controlsActive || state.browseSearched) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
            ) {
                BrowseResults(
                    state = state,
                    viewModel = viewModel,
                    onSelectMedia = onSelectMedia,
                    modifier = Modifier.fillMaxSize(),
                    header = browseHeader,
                )
            }
        } else {
            BrowseLanding(
                state = state,
                viewModel = viewModel,
                onSelectMedia = onSelectMedia,
                modifier = Modifier.fillMaxSize(),
                header = browseHeader,
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
            BrowseOptionDialog(
                title = when (activePicker) {
                    BrowsePicker.FORMAT -> "Format"
                    BrowsePicker.STATUS -> "Publishing Status"
                    BrowsePicker.COUNTRY -> "Country Of Origin"
                    BrowsePicker.YEAR -> "Year"
                },
                options = when (activePicker) {
                    BrowsePicker.FORMAT -> BrowseFormatOptions
                    BrowsePicker.STATUS -> BrowseStatusOptions
                    BrowsePicker.COUNTRY -> BrowseCountryOptions
                    BrowsePicker.YEAR -> browseYearOptions()
                },
                selectedValue = when (activePicker) {
                    BrowsePicker.FORMAT -> state.browseFormat
                    BrowsePicker.STATUS -> state.browsePublishingStatus
                    BrowsePicker.COUNTRY -> state.browseCountryOfOrigin
                    BrowsePicker.YEAR -> state.browseYear?.toString()
                },
                onSelect = { value ->
                    when (activePicker) {
                        BrowsePicker.FORMAT -> viewModel.setBrowseFormat(value)
                        BrowsePicker.STATUS -> viewModel.setBrowsePublishingStatus(value)
                        BrowsePicker.COUNTRY -> viewModel.setBrowseCountryOfOrigin(value)
                        BrowsePicker.YEAR -> viewModel.setBrowseYear(value?.toIntOrNull())
                    }
                    picker = null
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
    }
}

@Composable
internal fun BrowseFilterBar(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onOpenGenres: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenPicker: (BrowsePicker) -> Unit,
    onOpenAdvanced: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TankobunSearchField(
            value = state.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            placeholder = "Search AniList manga",
            onSearch = viewModel::searchAniList,
        )
        TankobunFilterRow {
            BrowseFilterPill(
                label = "Genres",
                value = if (state.browseGenres.isEmpty()) "Any" else state.browseGenres.size.toString(),
                selected = state.browseGenres.isNotEmpty(),
                onClick = onOpenGenres,
            )
            BrowseFilterPill(
                label = "Tags",
                value = if (state.browseTags.isEmpty()) "Any" else state.browseTags.size.toString(),
                selected = state.browseTags.isNotEmpty(),
                onClick = onOpenTags,
            )
            BrowseFilterPill(
                label = "Format",
                value = BrowseFormatOptions.labelFor(state.browseFormat),
                selected = state.browseFormat != null,
                onClick = { onOpenPicker(BrowsePicker.FORMAT) },
            )
            BrowseFilterPill(
                label = "Status",
                value = BrowseStatusOptions.labelFor(state.browsePublishingStatus),
                selected = state.browsePublishingStatus != null,
                onClick = { onOpenPicker(BrowsePicker.STATUS) },
            )
            BrowseFilterPill(
                label = "Country",
                value = BrowseCountryOptions.labelFor(state.browseCountryOfOrigin),
                selected = state.browseCountryOfOrigin != null,
                onClick = { onOpenPicker(BrowsePicker.COUNTRY) },
            )
            BrowseFilterPill(
                label = "Year",
                value = state.browseYear?.toString() ?: "Any",
                selected = state.browseYear != null,
                onClick = { onOpenPicker(BrowsePicker.YEAR) },
            )
            BrowseFilterPill(
                label = "Sort",
                value = BrowseSortOptions.labelFor(state.browseSort),
                selected = state.browseSort != BROWSE_SORT_SEARCH_MATCH_UI,
                onClick = onOpenAdvanced,
            )
            BrowseIconFilterPill(
                contentDescription = "Browse options",
                onClick = onOpenAdvanced,
            )
        }
    }
}

@Composable
internal fun BrowseFilterPill(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TankobunChip(
        selected = selected,
        onClick = onClick,
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
                Icons.Default.Tune,
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
    modifier: Modifier,
    header: @Composable () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = BrowseLandingContentPadding),
        verticalArrangement = Arrangement.spacedBy(34.dp),
    ) {
        item(key = "browse-header") {
            Box(Modifier.padding(horizontal = BrowseLandingContentPadding)) {
                header()
            }
        }
        item {
            BrowseMangaShelf(
                title = "TRENDING NOW",
                media = state.browseTrending,
                onViewAll = { viewModel.viewAllBrowseSection("TRENDING_DESC") },
                onSelectMedia = onSelectMedia,
            )
        }
        item {
            BrowseMangaShelf(
                title = "ALL TIME POPULAR",
                media = state.browsePopular,
                onViewAll = { viewModel.viewAllBrowseSection("POPULARITY_DESC") },
                onSelectMedia = onSelectMedia,
            )
        }
        item {
            BrowseMangaShelf(
                title = "POPULAR MANHWA",
                media = state.browsePopularManhwa,
                onViewAll = viewModel::viewAllPopularManhwa,
                onSelectMedia = onSelectMedia,
            )
        }
        item {
            BrowseMangaShelf(
                title = "TOP 100 MANGA",
                media = state.browseTopManga,
                onViewAll = { viewModel.viewAllBrowseSection("SCORE_DESC") },
                onSelectMedia = onSelectMedia,
            )
        }
    }
}

private val BrowseLandingContentPadding = 18.dp
private val BrowseShelfTitleHeight = 62.dp

@Composable
internal fun BrowseMangaShelf(
    title: String,
    media: List<AnilistMedia>,
    onViewAll: () -> Unit,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TankobunSectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = BrowseLandingContentPadding),
            actionLabel = "View All",
            onAction = onViewAll,
        )
        if (media.isEmpty()) {
            Text(
                "Cached discovery will appear here after AniList responds.",
                modifier = Modifier.padding(horizontal = BrowseLandingContentPadding),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = BrowseLandingContentPadding),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                items(media, key = { it.id }) { item ->
                    BrowseShelfTile(media = item, onClick = { onSelectMedia(item) })
                }
            }
        }
    }
}

@Composable
internal fun BrowseShelfTile(media: AnilistMedia, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(190.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
        ) {
            CoverImage(
                url = media.coverImage,
                title = media.title.userPreferred,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
            )
        }
        Column(
            modifier = Modifier.height(BrowseShelfTitleHeight),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = media.title.userPreferred,
                style = MaterialTheme.typography.titleSmall,
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
internal fun BrowseResults(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onSelectMedia: (AnilistMedia) -> Unit,
    modifier: Modifier,
    header: @Composable () -> Unit,
) {
    val resultsHeader: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            header()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when {
                        state.browseStaffName != null -> "Author Results"
                        state.searchQuery.isBlank() -> "Browse Manga"
                        else -> "Search Results"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    browseSummary(state),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = viewModel::resetBrowseFilters) {
                Text("Reset")
            }
        }
    }
    }
    MediaCollection(
        media = state.searchResults,
        viewMode = state.browseViewMode,
        coverColumns = state.browseCoverColumns,
        showWholeCovers = state.browseShowWholeCovers,
        modifier = modifier.fillMaxWidth(),
        header = resultsHeader,
        contentPadding = PaddingValues(vertical = 18.dp),
        onSelectMedia = onSelectMedia,
        emptyMessage = if (state.busy) {
            "Searching AniList..."
        } else {
            "No AniList manga found for these filters."
        },
    )
}

@Composable
internal fun BrowseGenreDialog(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        TankobunDialogSurface(fillMaxHeightFraction = 0.78f, scrollable = false) {
            TankobunDialogHeader(title = "Genres", onDismiss = onDismiss)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                FlowRowCompat {
                    BrowseGenres.forEach { genre ->
                    TankobunChip(
                        selected = genre in state.browseGenres,
                        onClick = { viewModel.setBrowseGenre(genre, genre !in state.browseGenres) },
                        label = {
                            Text(genre, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                    )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        state.browseGenres.forEach { viewModel.setBrowseGenre(it, false) }
                    },
                ) {
                    Text("Clear")
                }
                Spacer(Modifier.weight(1f))
                TankobunActionButton(
                    label = "Apply",
                    onClick = {
                        onDismiss()
                        viewModel.searchAniList()
                    },
                )
            }
        }
    }
}

@Composable
internal fun BrowseTagDialog(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visibleTags = state.browseAvailableTags.visibleTags(query)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        TankobunDialogSurface(fillMaxHeightFraction = 0.82f, scrollable = false) {
            TankobunDialogHeader(title = "Tags", onDismiss = onDismiss)
            TankobunSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Find a tag",
                showSearchAction = false,
            )
            if (state.browseAvailableTags.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Tags will appear here after AniList responds.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TankobunActionButton(
                        label = "Refresh tags",
                        onClick = { viewModel.loadBrowseTags(force = true) },
                        filled = false,
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
                            selected = tag.name in state.browseTags,
                            onClick = { viewModel.setBrowseTag(tag.name, tag.name !in state.browseTags) },
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
                TextButton(
                    onClick = {
                        state.browseTags.forEach { viewModel.setBrowseTag(it, false) }
                    },
                ) {
                    Text("Clear")
                }
                Spacer(Modifier.weight(1f))
                TankobunActionButton(
                    label = "Apply",
                    onClick = {
                        onDismiss()
                        viewModel.searchAniList()
                    },
                )
            }
        }
    }
}

@Composable
internal fun BrowseOptionDialog(
    title: String,
    options: List<BrowseOption>,
    selectedValue: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    TankobunDialog(onDismiss = onDismiss, maxHeight = 640.dp) {
        TankobunDialogHeader(title = title, onDismiss = onDismiss)
        options.forEach { option ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LocalTankobunStyle.current.radii.control))
                    .clickable { onSelect(option.value) },
                color = if (option.value == selectedValue) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                },
            )
            {
                ListItem(
                    headlineContent = { Text(option.label) },
                )
            }
        }
    }
}

@Composable
internal fun LibraryOptionsDialog(
    state: TankobunUiState,
    viewModel: MainViewModel,
    sort: String,
    onSortChange: (String?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    TankobunDialog(onDismiss = onDismiss, maxHeight = 680.dp) {
        TankobunDialogHeader(title = "Library Options", onDismiss = onDismiss)
        Text("Sort", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRowCompat {
            LibrarySortOptions.forEach { option ->
                TankobunChip(
                    selected = sort == option.value,
                    onClick = { onSortChange(option.value) },
                    label = { Text(option.label) },
                )
            }
        }
        Text("View", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        MediaViewModeRow(
            selected = state.libraryViewMode,
            onSelect = viewModel::setLibraryViewMode,
        )
        Text("Covers per row", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        CoverColumnsRow(
            selected = state.libraryCoverColumns,
            onSelect = viewModel::setLibraryCoverColumns,
        )
        Text("Cover framing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        CoverFramingRow(
            showWholeCover = state.libraryShowWholeCovers,
            onShowWholeCoverChange = viewModel::setLibraryShowWholeCovers,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onReset) {
                Text("Reset")
            }
            Spacer(Modifier.weight(1f))
            TankobunActionButton(label = "Apply", onClick = onDismiss)
        }
    }
}

@Composable
internal fun BrowseAdvancedDialog(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    TankobunDialog(onDismiss = onDismiss, maxHeight = 680.dp) {
        TankobunDialogHeader(title = "Browse Options", onDismiss = onDismiss)
        Text("Sort", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRowCompat {
            BrowseSortOptions.forEach { option ->
                TankobunChip(
                    selected = state.browseSort == option.value,
                    onClick = { option.value?.let(viewModel::setBrowseSort) },
                    label = { Text(option.label) },
                )
            }
        }
        Text("View", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        MediaViewModeRow(
            selected = state.browseViewMode,
            onSelect = viewModel::setBrowseViewMode,
        )
        Text("Covers per row", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        CoverColumnsRow(
            selected = state.browseCoverColumns,
            onSelect = viewModel::setBrowseCoverColumns,
        )
        Text("Cover framing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        CoverFramingRow(
            showWholeCover = state.browseShowWholeCovers,
            onShowWholeCoverChange = viewModel::setBrowseShowWholeCovers,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = viewModel::resetBrowseFilters) {
                Text("Reset")
            }
            Spacer(Modifier.weight(1f))
            TankobunActionButton(
                label = "Apply",
                onClick = {
                    onDismiss()
                    viewModel.searchAniList()
                }
            )
        }
    }
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
            label = { Text("Fill frame") },
        )
        TankobunChip(
            selected = showWholeCover,
            onClick = { onShowWholeCoverChange(true) },
            label = { Text("Show whole cover") },
        )
    }
}

internal fun List<BrowseOption>.labelFor(value: String?): String =
    firstOrNull { it.value == value }?.label ?: "Any"

internal fun List<AnilistMediaTag>.visibleTags(query: String): List<AnilistMediaTag> {
    val normalizedQuery = query.trim().lowercase()
    return asSequence()
        .filter { !it.isAdult }
        .filter { tag ->
            normalizedQuery.isBlank() ||
                tag.name.lowercase().contains(normalizedQuery) ||
                tag.category.orEmpty().lowercase().contains(normalizedQuery)
        }
        .sortedWith(compareBy<AnilistMediaTag> { it.category.orEmpty() }.thenBy { it.name })
        .toList()
}

internal fun browseYearOptions(): List<BrowseOption> {
    val currentYear = java.time.Year.now().value
    return listOf(BrowseOption("Any", null)) +
        (currentYear downTo 1970).map { BrowseOption(it.toString(), it.toString()) }
}

internal fun TankobunUiState.browseControlsActive(): Boolean =
    searchQuery.isNotBlank() ||
        browseGenres.isNotEmpty() ||
        browseTags.isNotEmpty() ||
        browseFormat != null ||
        browsePublishingStatus != null ||
        browseCountryOfOrigin != null ||
        browseYear != null ||
        browseStaffName != null ||
        browseSort != BROWSE_SORT_SEARCH_MATCH_UI

internal fun browseSummary(state: TankobunUiState): String {
    val parts = buildList {
        state.searchQuery.trim().takeIf { it.isNotBlank() }?.let { add("Search \"$it\"") }
        state.browseStaffName?.let { add("Author: $it") }
        if (state.browseGenres.isNotEmpty()) add(state.browseGenres.sorted().joinToString(", "))
        if (state.browseTags.isNotEmpty()) add(state.browseTags.sorted().joinToString(", "))
        state.browseFormat?.let { add(BrowseFormatOptions.labelFor(it)) }
        state.browsePublishingStatus?.let { add(BrowseStatusOptions.labelFor(it)) }
        state.browseCountryOfOrigin?.let { add(BrowseCountryOptions.labelFor(it)) }
        state.browseYear?.let { add(it.toString()) }
        BrowseSortOptions.labelFor(state.browseSort).takeIf { it != "Search Match" }?.let { add("Sort: $it") }
    }
    return parts.ifEmpty { listOf("AniList manga database") }.joinToString(" / ")
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

internal fun String?.statusLabel(): String = when (this) {
    "RELEASING" -> "Releasing"
    "FINISHED" -> "Finished"
    "HIATUS" -> "Hiatus"
    "NOT_YET_RELEASED" -> "Not yet released"
    "CANCELLED" -> "Cancelled"
    else -> "Status unknown"
}

internal fun String?.mediaFormatLabel(): String = when (this) {
    "MANGA" -> "Manga"
    "NOVEL" -> "Novel"
    "ONE_SHOT" -> "One Shot"
    else -> "Manga"
}

internal fun AnilistMedia.publishingYearLabel(): String = when {
    startDateYear != null && endDateYear != null && startDateYear != endDateYear -> "$startDateYear - $endDateYear"
    startDateYear != null && status == "RELEASING" -> "Since $startDateYear"
    startDateYear != null -> startDateYear.toString()
    else -> "Date unknown"
}

internal fun List<String>.authorLabel(): String =
    take(3).joinToString(", ").ifBlank { "Unknown" }

internal fun Int.formatCompact(): String =
    when {
        this >= 1_000_000 -> "${this / 1_000_000}m"
        this >= 10_000 -> "${this / 1_000}k"
        else -> toString()
    }
