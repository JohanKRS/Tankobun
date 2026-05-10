package com.tankobun.app

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceSearchResult
import kotlinx.coroutines.launch

private enum class SettingsRoute {
    MAIN,
    SOURCES,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TankobunApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var settingsRoute by remember { mutableStateOf(SettingsRoute.MAIN) }
    val selectedMedia = state.selectedMedia
    val readerOpen = state.activeChapter != null && state.readerPages.isNotEmpty()

    BackHandler(enabled = readerOpen) {
        viewModel.closeReader()
    }

    BackHandler(enabled = !readerOpen && (selectedMedia != null || (selectedTab == 3 && settingsRoute != SettingsRoute.MAIN))) {
        if (selectedMedia != null) {
            viewModel.clearSelectedMedia()
        } else {
            settingsRoute = SettingsRoute.MAIN
        }
    }

    TankobunTheme(themeMode = state.themeMode) {
        Box(Modifier.fillMaxSize()) {
            TankobunScaffold(
                state = state,
                viewModel = viewModel,
                selectedTab = selectedTab,
                onSelectTab = {
                    selectedTab = it
                    settingsRoute = SettingsRoute.MAIN
                },
                selectedMedia = selectedMedia,
                settingsRoute = settingsRoute,
                onOpenSettingsRoute = { settingsRoute = it },
            )
            if (readerOpen) {
                FullScreenReader(state, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TankobunScaffold(
    state: TankobunUiState,
    viewModel: MainViewModel,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    selectedMedia: AnilistMedia?,
    settingsRoute: SettingsRoute,
    onOpenSettingsRoute: (SettingsRoute) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (selectedMedia != null || (selectedTab == 3 && settingsRoute != SettingsRoute.MAIN)) {
                        IconButton(
                            onClick = {
                                if (selectedMedia != null) {
                                    viewModel.clearSelectedMedia()
                                } else {
                                    onOpenSettingsRoute(SettingsRoute.MAIN)
                                }
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            selectedMedia?.title?.userPreferred
                                ?: if (selectedTab == 3 && settingsRoute == SettingsRoute.SOURCES) "Sources" else "Tankobun",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            state.viewerName?.let { "AniList: $it" } ?: "AniList-first manga reader",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (selectedMedia == null) {
                NavigationBar {
                    listOf(
                        Triple("Library", Icons.AutoMirrored.Filled.LibraryBooks, 0),
                        Triple("Browse", Icons.Default.Explore, 1),
                        Triple("Downloads", Icons.Default.Download, 2),
                        Triple("Settings", Icons.Default.Settings, 3),
                    ).forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { onSelectTab(index) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (selectedMedia != null) {
                MangaDetailScreen(state, viewModel, selectedMedia)
            } else {
                when (selectedTab) {
                    0 -> LibraryScreen(state, viewModel)
                    1 -> BrowseScreen(state, viewModel)
                    2 -> DownloadsScreen(state)
                    3 -> when (settingsRoute) {
                        SettingsRoute.MAIN -> SettingsScreen(
                            state = state,
                            viewModel = viewModel,
                            onOpenSources = { onOpenSettingsRoute(SettingsRoute.SOURCES) },
                        )
                        SettingsRoute.SOURCES -> SourcesSettingsScreen(state, viewModel)
                    }
                }
            }
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LibraryScreen(state: TankobunUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!state.loggedIn) {
            Text("Connect AniList to fetch your manga library.")
            Button(
                enabled = state.clientConfigured,
                onClick = {
                    viewModel.loginUrl()?.let { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
            ) {
                Text(if (state.clientConfigured) "Connect AniList" else "Set AniList client id")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = viewModel::refreshLibrary) {
                    Text("Sync")
                }
                AssistChip(onClick = {}, label = { Text("${state.libraryItems.size} manga") })
                state.librarySyncedAtEpochMillis.takeIf { it > 0 }?.let {
                    AssistChip(onClick = {}, label = { Text("Cached ${cacheAgeLabel(it)}") })
                }
            }
        }

        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary)
        }

        LibraryPager(
            sections = state.librarySections,
            viewMode = state.libraryViewMode,
            onViewModeChange = viewModel::setLibraryViewMode,
            modifier = Modifier.weight(1f),
            onSelectMedia = viewModel::selectMedia,
        )
    }
}

@Composable
private fun LibraryPager(
    sections: List<LibrarySection>,
    viewMode: MediaViewMode,
    onViewModeChange: (MediaViewMode) -> Unit,
    modifier: Modifier,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    if (sections.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            Text("No manga in your AniList library yet.")
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { sections.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        MediaViewModeRow(
            selected = viewMode,
            onSelect = onViewModeChange,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage.coerceAtMost(sections.lastIndex),
            edgePadding = 0.dp,
        ) {
            sections.forEachIndexed { index, section ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            "${section.title} ${section.items.size}",
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
                media = sections[page].items.map { it.media },
                viewMode = viewMode,
                onSelectMedia = onSelectMedia,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun BrowseScreen(state: TankobunUiState, viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Search AniList manga") },
            )
            IconButton(onClick = viewModel::searchAniList) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        }

        MediaViewModeRow(
            selected = state.browseViewMode,
            onSelect = viewModel::setBrowseViewMode,
        )

        MediaCollection(
            media = state.searchResults,
            viewMode = state.browseViewMode,
            modifier = Modifier.weight(1f),
            onSelectMedia = viewModel::selectMedia,
        )
    }
}

@Composable
private fun MediaViewModeRow(
    selected: MediaViewMode,
    onSelect: (MediaViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            MediaViewMode.COVER_GRID to "Covers",
            MediaViewMode.COVER_WITH_INFO to "Info",
            MediaViewMode.MASONRY to "Masonry",
            MediaViewMode.JUSTIFIED to "Justified",
            MediaViewMode.LIST to "List",
        ).forEach { (mode, label) ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(label) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaCollection(
    media: List<AnilistMedia>,
    viewMode: MediaViewMode,
    onSelectMedia: (AnilistMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (media.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            Text("No manga here yet.")
        }
        return
    }

    when (viewMode) {
        MediaViewMode.LIST -> LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(media, key = { it.id }) { item ->
                MediaRow(media = item, onClick = { onSelectMedia(item) })
            }
        }
        MediaViewMode.MASONRY -> LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(3),
            modifier = modifier,
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            staggeredItems(media, key = { it.id }) { item ->
                MediaCoverTile(
                    media = item,
                    viewMode = MediaViewMode.MASONRY,
                    onClick = { onSelectMedia(item) },
                )
            }
        }
        else -> LazyVerticalGrid(
            columns = GridCells.Fixed(if (viewMode == MediaViewMode.COVER_WITH_INFO) 3 else 4),
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            gridItems(media, key = { it.id }) { item ->
                MediaCoverTile(
                    media = item,
                    viewMode = viewMode,
                    onClick = { onSelectMedia(item) },
                )
            }
        }
    }
}

@Composable
private fun MediaCoverTile(
    media: AnilistMedia,
    viewMode: MediaViewMode,
    onClick: () -> Unit,
) {
    val coverModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(
            when (viewMode) {
                MediaViewMode.JUSTIFIED -> 0.82f
                MediaViewMode.MASONRY -> if (media.id % 3 == 0) 0.72f else 0.58f
                else -> 2f / 3f
            },
        )

    Column(
        modifier = Modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CoverImage(
            url = media.coverImage,
            title = media.title.userPreferred,
            modifier = coverModifier,
        )
        if (viewMode != MediaViewMode.COVER_GRID) {
            Text(
                media.title.userPreferred,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (viewMode == MediaViewMode.COVER_WITH_INFO) {
                Text(
                    listOfNotNull(media.status, media.chapters?.let { "$it ch" }).joinToString(" / "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaRow(media: AnilistMedia, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick) {
        ListItem(
            headlineContent = {
                Text(media.title.userPreferred, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(
                    listOfNotNull(media.status, media.chapters?.let { "$it chapters" })
                        .joinToString(" / "),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                CoverImage(
                    url = media.coverImage,
                    title = media.title.userPreferred,
                    modifier = Modifier.size(width = 56.dp, height = 78.dp),
                )
            },
        )
    }
}

@Composable
private fun MangaDetailScreen(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (state.sourcePickerOpen) 8.dp else 0.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    CoverImage(
                        url = media.coverImage,
                        title = media.title.userPreferred,
                        modifier = Modifier
                            .width(190.dp)
                            .aspectRatio(2f / 3f),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(media.title.userPreferred, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            listOfNotNull(media.status, media.chapters?.let { "$it chapters" }, media.volumes?.let { "$it volumes" })
                                .joinToString(" / "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            media.description?.replace(Regex("<[^>]*>"), "").orEmpty(),
                            maxLines = 12,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (media.genres.isNotEmpty()) {
                            FlowRowCompat {
                                media.genres.take(8).forEach { genre ->
                                    AssistChip(onClick = {}, label = { Text(genre) })
                                }
                            }
                        }
                        media.siteUrl?.let { site ->
                            Text(
                                site,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            item {
                state.message?.let {
                    Text(it, color = MaterialTheme.colorScheme.secondary)
                }
            }

            item {
                SourceSummarySection(state, viewModel, media)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Chapters", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    FilterChip(
                        selected = state.readerMode == ReaderMode.PAGED,
                        onClick = { viewModel.setReaderMode(ReaderMode.PAGED) },
                        label = { Text("Paged") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                    )
                    FilterChip(
                        selected = state.readerMode == ReaderMode.WEBTOON,
                        onClick = { viewModel.setReaderMode(ReaderMode.WEBTOON) },
                        label = { Text("Webtoon") },
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (state.selectedSourceManga == null) {
                    Text("Choose a source first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Button(onClick = viewModel::loadChaptersForCurrentMatch) {
                        Text(if (state.sourceChapters.isEmpty()) "Load chapters" else "Refresh chapters")
                    }
                }
            }

            if (state.sourceChapters.isEmpty()) {
                item {
                    Text("No chapters loaded yet.")
                }
            } else {
                items(state.sourceChapters, key = { "${it.sourceId}:${it.url}" }) { chapter ->
                    ChapterRow(chapter, viewModel)
                }
            }
        }

        if (state.sourcePickerOpen) {
            SourcePickerDialog(state, viewModel, media)
        }
    }
}

@Composable
private fun SourceSummarySection(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
    val selectedManga = state.selectedSourceManga
    val selectedSource = state.selectedSource
    val latestProgress = state.latestProgress
    val resumeChapter = latestProgress?.let { progress ->
        state.sourceChapters.firstOrNull { it.url == progress.chapterUrl }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Source", style = MaterialTheme.typography.titleLarge)
        if (selectedManga == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = viewModel::openSourcePicker) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Find source")
                }
                Text(
                    if (state.allInstalledSources.isEmpty()) "Install source extensions in Settings." else "No source selected.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            ElevatedCard {
                ListItem(
                    headlineContent = {
                        Text(selectedManga.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    supportingContent = {
                        Column {
                            Text(
                                listOfNotNull(
                                    selectedSource?.let { "${it.name} (${it.lang})" },
                                    state.sourceChapters.takeIf { it.isNotEmpty() }?.let { "${it.size} chapters" },
                                ).joinToString(" / "),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (latestProgress != null) {
                                Text(
                                    "Last read: chapter ${latestProgress.chapterNumber.takeIf { it > 0 } ?: "?"}, page ${latestProgress.pageIndex + 1}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    leadingContent = {
                        CoverImage(
                            url = selectedManga.thumbnailUrl ?: media.coverImage,
                            title = selectedManga.title,
                            modifier = Modifier.size(width = 50.dp, height = 70.dp),
                        )
                    },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (resumeChapter != null) {
                                OutlinedButton(onClick = { viewModel.openChapter(resumeChapter) }) {
                                    Text("Resume")
                                }
                            }
                            Button(onClick = viewModel::openSourcePicker) {
                                Text("Change")
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SourcePickerDialog(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
    val matches = state.sourceMatches.filter { match ->
        state.sourceMatchChapterCounts[sourceMatchKey(match.source.id, match.manga.url)] != null
    }

    Dialog(
        onDismissRequest = viewModel::closeSourcePicker,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Find source", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            media.title.userPreferred,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = viewModel::closeSourcePicker) {
                        Text("Close")
                    }
                }

                if (state.sourcePickerLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                if (matches.isEmpty() && !state.sourcePickerLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No readable matches yet.", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { viewModel.findSourceMatches(forceRefresh = true) }) {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Search again")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(matches, key = { "${it.source.id}:${it.manga.url}" }) { match ->
                            val count = state.sourceMatchChapterCounts[sourceMatchKey(match.source.id, match.manga.url)] ?: 0
                            SourceMatchRow(
                                match = match,
                                chapterCount = count,
                                current = state.selectedSourceId == match.source.id &&
                                    state.selectedSourceManga?.url == match.manga.url,
                                mediaCover = media.coverImage,
                                onClick = { viewModel.bindSourceMatch(match) },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { viewModel.findSourceMatches(forceRefresh = true) }) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Refresh")
                    }
                    if (state.sourcePickerLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                    Text(
                        "${matches.size} sources",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceMatchRow(
    match: SourceSearchResult,
    chapterCount: Int,
    current: Boolean,
    mediaCover: String?,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick) {
        ListItem(
            headlineContent = { Text(match.manga.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Text(
                    "${match.source.name} (${match.source.lang}) / $chapterCount chapters / ${(match.score * 100).toInt()}% match",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                CoverImage(
                    url = match.manga.thumbnailUrl ?: mediaCover,
                    title = match.manga.title,
                    modifier = Modifier.size(width = 48.dp, height = 68.dp),
                )
            },
            trailingContent = {
                if (current) {
                    Text("Current", color = MaterialTheme.colorScheme.secondary)
                }
            },
        )
    }
}

private fun sourceMatchKey(sourceId: Long, mangaUrl: String): String =
    "$sourceId:$mangaUrl"

@Composable
private fun CoverImage(url: String?, title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (url.isNullOrBlank()) {
            Text(
                title.take(1),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChapterRow(chapter: SourceChapter, viewModel: MainViewModel) {
    ElevatedCard {
        ListItem(
            headlineContent = {
                Text(chapter.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text("Chapter ${chapter.chapterNumber.takeIf { it > 0 } ?: "?"}")
            },
            trailingContent = {
                Row {
                    IconButton(onClick = { viewModel.enqueueDownload(chapter) }) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                    }
                    IconButton(onClick = { viewModel.openChapter(chapter) }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Read")
                    }
                }
            },
        )
    }
}

@Composable
private fun FullScreenReader(state: TankobunUiState, viewModel: MainViewModel) {
    val chapter = state.activeChapter ?: return
    if (state.readerPages.isEmpty()) return
    var controlsVisible by remember(chapter.url) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(chapter.url, state.currentPageIndex, controlsVisible) {
                detectTapGestures { offset ->
                    val centerX = size.width / 3f..size.width * 2f / 3f
                    val centerY = size.height / 3f..size.height * 2f / 3f
                    when {
                        offset.x in centerX && offset.y in centerY -> controlsVisible = !controlsVisible
                        !controlsVisible && state.readerMode == ReaderMode.PAGED && offset.x < size.width / 3f -> {
                            viewModel.moveReaderPage(-1)
                        }
                        !controlsVisible && state.readerMode == ReaderMode.PAGED && offset.x > size.width * 2f / 3f -> {
                            viewModel.moveReaderPage(1)
                        }
                    }
                }
            },
    ) {
        if (state.readerMode == ReaderMode.WEBTOON) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(state.readerPages, key = { _, page -> "${page.index}:${page.imageUrl}" }) { _, page ->
                    AsyncImage(
                        model = readerImageRequest(page),
                        contentDescription = chapter.name,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth,
                    )
                }
            }
        } else {
            val page = state.readerPages[state.currentPageIndex]
            AsyncImage(
                model = readerImageRequest(page),
                contentDescription = chapter.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = ContentScale.Fit,
            )
        }

        if (controlsVisible) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::closeReader) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close reader",
                            tint = Color.White,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            chapter.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${state.currentPageIndex + 1}/${state.readerPages.size}",
                            color = Color.White.copy(alpha = 0.74f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { viewModel.moveReaderPage(-1) }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous page", tint = Color.White)
                    }
                    FilterChip(
                        selected = state.readerMode == ReaderMode.PAGED,
                        onClick = { viewModel.setReaderMode(ReaderMode.PAGED) },
                        label = { Text("Paged") },
                    )
                    FilterChip(
                        selected = state.readerMode == ReaderMode.WEBTOON,
                        onClick = { viewModel.setReaderMode(ReaderMode.WEBTOON) },
                        label = { Text("Webtoon") },
                    )
                    IconButton(onClick = { viewModel.moveReaderPage(1) }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next page", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun readerImageRequest(page: ReaderPage): ImageRequest {
    val context = LocalContext.current
    return remember(page.imageUrl, page.headers) {
        val headers = NetworkHeaders.Builder().apply {
            page.headers.forEach { (name, value) ->
                if (name.isNotBlank() && value.isNotBlank()) {
                    set(name, value)
                }
            }
        }.build()

        ImageRequest.Builder(context)
            .data(page.imageUrl)
            .httpHeaders(headers)
            .listener(
                onError = { _, result ->
                    Log.w(
                        "TankobunMain",
                        "Reader image failed index=${page.index} host=${Uri.parse(page.imageUrl).host} headers=${page.headers.keys}",
                        result.throwable,
                    )
                },
            )
            .build()
    }
}

@Composable
private fun DownloadsScreen(state: TankobunUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Downloads", style = MaterialTheme.typography.titleLarge)
        if (state.downloads.isEmpty()) {
            Text("No downloads yet.")
        } else {
            state.downloads.forEach { job ->
                ElevatedCard {
                    ListItem(
                        headlineContent = { Text(job.chapterName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(job.state.name.lowercase()) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onOpenSources: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeModeChip("System", TankobunThemeMode.SYSTEM, state, viewModel)
            ThemeModeChip("Light", TankobunThemeMode.LIGHT, state, viewModel)
            ThemeModeChip("Dark", TankobunThemeMode.DARK, state, viewModel)
        }

        Text("Library view", style = MaterialTheme.typography.titleMedium)
        MediaViewModeRow(selected = state.libraryViewMode, onSelect = viewModel::setLibraryViewMode)

        Text("Browse view", style = MaterialTheme.typography.titleMedium)
        MediaViewModeRow(selected = state.browseViewMode, onSelect = viewModel::setBrowseViewMode)

        ElevatedCard(onClick = onOpenSources) {
            ListItem(
                headlineContent = { Text("Sources") },
                supportingContent = {
                    Text(
                        "${state.installedSources.size} active / ${state.allInstalledSources.size} installed. Extension repositories are user-provided.",
                    )
                },
                trailingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
            )
        }

        Text("AniList", style = MaterialTheme.typography.titleMedium)
        Text("Redirect URI: ${BuildConfig.ANILIST_REDIRECT_URI}")
        state.librarySyncedAtEpochMillis.takeIf { it > 0 }?.let {
            Text("Library cache: ${cacheAgeLabel(it)}")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.loggedIn) {
                Button(onClick = viewModel::refreshLibrary) {
                    Text("Sync AniList")
                }
                Button(onClick = viewModel::signOut) {
                    Text("Sign out")
                }
            }
        }

        Text("Unofficial app. Source extensions and content providers are third parties.")
    }
}

@Composable
private fun SourcesSettingsScreen(state: TankobunUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val languageOptions = (state.allInstalledSources.map { it.lang.normalizedSourceLanguage() } + state.sourceLanguages)
        .filter { it.isNotBlank() }
        .distinct()
        .sortedWith(compareBy<String> { if (it in setOf("en", "all")) 0 else 1 }.thenBy { it })

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Source Languages", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRowCompat {
                languageOptions.forEach { language ->
                    FilterChip(
                        selected = language in state.sourceLanguages,
                        onClick = {
                            viewModel.setSourceLanguageEnabled(
                                language = language,
                                enabled = language !in state.sourceLanguages,
                            )
                        },
                        label = { Text(sourceLanguageLabel(language)) },
                    )
                }
            }
            Text(
                "Tankobun only searches active languages from the manga page.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Text("Installed Sources", style = MaterialTheme.typography.titleMedium)
        }

        if (state.allInstalledSources.isEmpty()) {
            item {
                Text("No installed Tachiyomi-compatible source extensions found.")
            }
        } else {
            items(state.allInstalledSources, key = { "${it.packageName}:${it.id}:${it.lang}" }) { source ->
                val active = state.installedSources.any { it.id == source.id }
                ElevatedCard {
                    ListItem(
                        headlineContent = { Text(source.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(
                                listOfNotNull(
                                    sourceLanguageLabel(source.lang.normalizedSourceLanguage()),
                                    source.versionName?.let { "v$it" },
                                    if (source.isNsfw) "NSFW" else null,
                                    if (active) "active" else "filtered",
                                ).joinToString(" / "),
                            )
                        },
                    )
                }
            }
        }

        item {
            Text("Extension Repository", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.extensionRepositoryUrl,
                onValueChange = viewModel::setExtensionRepositoryUrl,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("User-provided repository index URL") },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::refreshInstalledSources) {
                    Text("Refresh installed")
                }
                Button(onClick = viewModel::refreshExtensionIndex) {
                    Text("Browse repository")
                }
            }
        }

        if (state.availableExtensions.isNotEmpty()) {
            item {
                Text("Repository", style = MaterialTheme.typography.titleMedium)
            }
            items(state.availableExtensions, key = { "${it.packageName}:${it.versionCode}" }) { extension ->
                ElevatedCard(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.extensionApkUrl(extension))))
                    },
                ) {
                    ListItem(
                        headlineContent = { Text(extension.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("${sourceLanguageLabel(extension.lang.normalizedSourceLanguage())} / v${extension.versionName}") },
                        trailingContent = { Text("Install") },
                    )
                }
            }
        }

        item {
            Text(
                "Source-specific options will appear here as Tankobun's compatibility layer learns how to render each extension's settings safely.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun cacheAgeLabel(syncedAtEpochMillis: Long): String {
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

private fun String.normalizedSourceLanguage(): String =
    trim().lowercase().replace('_', '-')

private fun sourceLanguageLabel(language: String): String =
    when (language.normalizedSourceLanguage()) {
        "all" -> "All"
        "en" -> "English"
        "pt" -> "Portuguese"
        "pt-br" -> "Portuguese (BR)"
        "es" -> "Spanish"
        "fr" -> "French"
        "ja" -> "Japanese"
        "ko" -> "Korean"
        "zh" -> "Chinese"
        else -> language.uppercase()
    }

@Composable
private fun ThemeModeChip(
    label: String,
    mode: TankobunThemeMode,
    state: TankobunUiState,
    viewModel: MainViewModel,
) {
    FilterChip(
        selected = state.themeMode == mode,
        onClick = { viewModel.setThemeMode(mode) },
        label = { Text(label) },
    )
}

@Composable
private fun FlowRowCompat(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}
