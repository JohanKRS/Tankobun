package com.tankobun.app

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceSearchResult
import kotlinx.coroutines.launch

private enum class SettingsRoute {
    MAIN,
    SOURCES,
}

private enum class QuickDrawerMode {
    CLOSED,
    OVERLAY,
    PINNED,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TankobunApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var settingsRoute by remember { mutableStateOf(SettingsRoute.MAIN) }
    var quickDrawerMode by remember { mutableStateOf(QuickDrawerMode.CLOSED) }
    val selectedMedia = state.selectedMedia
    val readerOpen = state.activeChapter != null && state.readerPages.isNotEmpty()

    BackHandler(enabled = readerOpen) {
        viewModel.closeReader()
    }

    BackHandler(enabled = !readerOpen && quickDrawerMode == QuickDrawerMode.OVERLAY) {
        quickDrawerMode = QuickDrawerMode.CLOSED
    }

    BackHandler(
        enabled = !readerOpen &&
            quickDrawerMode != QuickDrawerMode.OVERLAY &&
            (selectedMedia != null || (selectedTab == 3 && settingsRoute != SettingsRoute.MAIN)),
    ) {
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
                quickDrawerMode = quickDrawerMode,
                onOpenQuickDrawer = { quickDrawerMode = QuickDrawerMode.OVERLAY },
                onCloseQuickDrawer = { quickDrawerMode = QuickDrawerMode.CLOSED },
                onToggleQuickDrawerPin = {
                    quickDrawerMode = if (quickDrawerMode == QuickDrawerMode.PINNED) {
                        QuickDrawerMode.OVERLAY
                    } else {
                        QuickDrawerMode.PINNED
                    }
                },
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
    quickDrawerMode: QuickDrawerMode,
    onOpenQuickDrawer: () -> Unit,
    onCloseQuickDrawer: () -> Unit,
    onToggleQuickDrawerPin: () -> Unit,
) {
    Scaffold(
        containerColor = LocalTankobunTokens.current.appBackdrop,
        topBar = {
            TankobunTopBar(
                title = selectedMedia?.title?.userPreferred
                    ?: if (selectedTab == 3 && settingsRoute == SettingsRoute.SOURCES) "Sources" else "Tankobun",
                subtitle = state.viewerName?.let { "AniList: $it" } ?: "AniList-first manga reader",
                showBack = selectedMedia != null || (selectedTab == 3 && settingsRoute != SettingsRoute.MAIN),
                onBack = {
                    if (selectedMedia != null) {
                        viewModel.clearSelectedMedia()
                    } else {
                        onOpenSettingsRoute(SettingsRoute.MAIN)
                    }
                },
                onSync = viewModel::refreshLibrary,
                onSearch = {
                    if (selectedMedia != null) {
                        viewModel.clearSelectedMedia()
                    }
                    onSelectTab(1)
                    onOpenSettingsRoute(SettingsRoute.MAIN)
                },
                onOpenQuickDrawer = onOpenQuickDrawer,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .background(LocalTankobunTokens.current.appBackdrop),
            ) {
                if (selectedMedia == null) {
                    TankobunNavigationRail(selectedTab = selectedTab, onSelectTab = onSelectTab)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
                }
                if (quickDrawerMode == QuickDrawerMode.PINNED) {
                    val pinnedWidth by animateDpAsState(
                        targetValue = 320.dp,
                        animationSpec = tween(durationMillis = 220),
                        label = "Pinned drawer width",
                    )
                    QuickDrawer(
                        state = state,
                        viewModel = viewModel,
                        selectedMedia = selectedMedia,
                        pinned = true,
                        onClose = onCloseQuickDrawer,
                        onTogglePin = onToggleQuickDrawerPin,
                        modifier = Modifier.width(pinnedWidth).fillMaxHeight(),
                    )
                }
            }
            if (quickDrawerMode != QuickDrawerMode.PINNED) {
                QuickDrawerHandle(
                    onClick = onOpenQuickDrawer,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
            if (quickDrawerMode == QuickDrawerMode.OVERLAY) {
                AnimatedVisibility(
                    visible = true,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(durationMillis = 260),
                    ) + fadeIn(tween(180)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(durationMillis = 220),
                    ) + fadeOut(tween(140)),
                ) {
                    QuickDrawer(
                        state = state,
                        viewModel = viewModel,
                        selectedMedia = selectedMedia,
                        pinned = false,
                        onClose = onCloseQuickDrawer,
                        onTogglePin = onToggleQuickDrawerPin,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(340.dp),
                    )
                }
            }
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TankobunTopBar(
    title: String,
    subtitle: String,
    showBack: Boolean,
    onBack: () -> Unit,
    onSync: () -> Unit,
    onSearch: () -> Unit,
    onOpenQuickDrawer: () -> Unit,
) {
    Surface(color = LocalTankobunTokens.current.elevatedSurface, tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = LocalTankobunTokens.current.softAccent,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxSize(),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onSync) {
                Icon(Icons.Default.Refresh, contentDescription = "Sync")
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = onOpenQuickDrawer) {
                Icon(Icons.AutoMirrored.Filled.MenuOpen, contentDescription = "Quick drawer")
            }
        }
    }
}

@Composable
private fun TankobunNavigationRail(selectedTab: Int, onSelectTab: (Int) -> Unit) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Spacer(Modifier.height(8.dp))
        listOf(
            Triple("Library", Icons.AutoMirrored.Filled.LibraryBooks, 0),
            Triple("Browse", Icons.Default.Explore, 1),
            Triple("Downloads", Icons.Default.Download, 2),
            Triple("Settings", Icons.Default.Settings, 3),
        ).forEach { (label, icon, index) ->
            NavigationRailItem(
                selected = selectedTab == index,
                onClick = { onSelectTab(index) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun QuickDrawerHandle(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .padding(end = 2.dp)
            .width(10.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
            .clickable(onClick = onClick),
        color = LocalTankobunTokens.current.drawerHandle,
        tonalElevation = 2.dp,
    ) {}
}

@Composable
private fun QuickDrawer(
    state: TankobunUiState,
    viewModel: MainViewModel,
    selectedMedia: AnilistMedia?,
    pinned: Boolean,
    onClose: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = if (pinned) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
        color = LocalTankobunTokens.current.elevatedSurface,
        tonalElevation = if (pinned) 1.dp else 10.dp,
        shadowElevation = if (pinned) 0.dp else 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier
                    .width(34.dp)
                    .height(4.dp)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f),
            ) {}
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Quick Drawer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (pinned) "Pinned utility panel" else "Resume, sources, sync",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onTogglePin) {
                    Text(if (pinned) "Unpin" else "Pin")
                }
                TextButton(onClick = onClose) {
                    Text("Close")
                }
            }

            QuickDrawerSection(title = "Continue Reading") {
                val progress = state.latestProgress
                val chapter = progress?.let { saved -> state.sourceChapters.firstOrNull { it.url == saved.chapterUrl } }
                if (chapter != null) {
                    Text(chapter.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Page ${progress.pageIndex + 1}/${progress.totalPages} / chapter ${progress.chapterNumber.takeIf { it > 0 } ?: "?"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { viewModel.openChapter(chapter) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Resume")
                    }
                } else {
                    Text(
                        selectedMedia?.let { "Open a chapter to create a resume point." } ?: "Open a title to see resume actions here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            QuickDrawerSection(title = "Source Health") {
                val source = state.selectedSource
                val manga = state.selectedSourceManga
                Text(
                    manga?.title ?: "No source selected",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        source?.let { "${it.name} (${it.lang})" },
                        state.sourceChapters.takeIf { it.isNotEmpty() }?.let { "${it.size} chapters cached" },
                    ).ifEmpty { listOf("Find a readable source from the manga page.") }.joinToString(" / "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selectedMedia != null) {
                    OutlinedButton(onClick = viewModel::openSourcePicker, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (manga == null) "Find source" else "Change source")
                    }
                }
            }

            QuickDrawerSection(title = "Downloads") {
                if (state.downloads.isEmpty()) {
                    Text("No downloads queued.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.downloads.take(4).forEach { job ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(job.chapterName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    job.state.name.lowercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (selectedMedia != null) {
                QuickDrawerSection(title = "AniList") {
                    AniListTrackingSection(state, viewModel, selectedMedia)
                }

                if (state.sourceMatches.isNotEmpty()) {
                    QuickDrawerSection(title = "Source Matches") {
                        state.sourceMatches.take(3).forEach { match ->
                            val count = state.sourceMatchChapterCounts[sourceMatchKey(match.source.id, match.manga.url)] ?: 0
                            Text(
                                "${match.source.name} / ${count.takeIf { it > 0 } ?: "?"} chapters / ${(match.score * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            QuickDrawerSection(title = "Sync") {
                Text(
                    state.librarySyncedAtEpochMillis.takeIf { it > 0 }?.let { "Library cache: ${cacheAgeLabel(it)}" }
                        ?: "Library has not synced yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = viewModel::refreshLibrary, enabled = state.loggedIn, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sync AniList")
                }
            }
        }
    }
}

@Composable
private fun QuickDrawerSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun LibraryScreen(state: TankobunUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LibraryHero(
            state = state,
            onSync = viewModel::refreshLibrary,
            onConnect = {
                viewModel.loginUrl()?.let { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            },
        )

        state.message?.let {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(it, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
            }
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
private fun LibraryHero(
    state: TankobunUiState,
    onSync: () -> Unit,
    onConnect: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = LocalTankobunTokens.current.elevatedSurface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(8.dp),
                color = LocalTankobunTokens.current.softAccent,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.LibraryBooks,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (state.loggedIn) "Your AniList library" else "Connect AniList",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (state.loggedIn) {
                        listOfNotNull(
                            "${state.libraryItems.size} manga",
                            state.librarySyncedAtEpochMillis.takeIf { it > 0 }?.let { "cached ${cacheAgeLabel(it)}" },
                        ).joinToString(" / ")
                    } else {
                        "Fetch your manga library, progress, custom lists, and recommendations."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                enabled = if (state.loggedIn) true else state.clientConfigured,
                onClick = if (state.loggedIn) onSync else onConnect,
            ) {
                Icon(
                    if (state.loggedIn) Icons.Default.Refresh else Icons.Default.Link,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(if (state.loggedIn) "Sync" else if (state.clientConfigured) "Connect" else "Setup")
            }
        }
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
    val currentSection = sections[pagerState.currentPage.coerceAtMost(sections.lastIndex)]

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                MediaViewModeRow(
                    selected = viewMode,
                    onSelect = onViewModeChange,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage.coerceAtMost(sections.lastIndex),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
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
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(currentSection.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${currentSection.items.size} titles",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                sections.forEachIndexed { index, _ ->
                    val width by animateDpAsState(
                        targetValue = if (index == pagerState.currentPage) 18.dp else 6.dp,
                        animationSpec = tween(durationMillis = 180),
                        label = "Library pager dot",
                    )
                    Surface(
                        modifier = Modifier
                            .width(width)
                            .height(6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (index == pagerState.currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
                        },
                    ) {}
                }
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

private enum class BrowsePicker {
    FORMAT,
    STATUS,
    COUNTRY,
    YEAR,
}

private data class BrowseOption(
    val label: String,
    val value: String?,
)

private const val BROWSE_SORT_SEARCH_MATCH_UI = "SEARCH_MATCH"

private val BrowseGenres = listOf(
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

private val BrowseFormatOptions = listOf(
    BrowseOption("Any", null),
    BrowseOption("Manga", "MANGA"),
    BrowseOption("Novel", "NOVEL"),
    BrowseOption("One Shot", "ONE_SHOT"),
)

private val BrowseStatusOptions = listOf(
    BrowseOption("Any", null),
    BrowseOption("Releasing", "RELEASING"),
    BrowseOption("Finished", "FINISHED"),
    BrowseOption("Not Yet Released", "NOT_YET_RELEASED"),
    BrowseOption("Cancelled", "CANCELLED"),
    BrowseOption("Hiatus", "HIATUS"),
)

private val BrowseCountryOptions = listOf(
    BrowseOption("Any", null),
    BrowseOption("Japan", "JP"),
    BrowseOption("South Korea", "KR"),
    BrowseOption("China", "CN"),
    BrowseOption("Taiwan", "TW"),
)

private val BrowseSortOptions = listOf(
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
private fun BrowseScreen(state: TankobunUiState, viewModel: MainViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadBrowseLanding()
    }

    var picker by remember { mutableStateOf<BrowsePicker?>(null) }
    var genresOpen by remember { mutableStateOf(false) }
    var advancedOpen by remember { mutableStateOf(false) }
    val controlsActive = state.browseControlsActive()

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BrowseFilterBar(
                state = state,
                viewModel = viewModel,
                onOpenGenres = { genresOpen = true },
                onOpenPicker = { picker = it },
                onOpenAdvanced = { advancedOpen = true },
            )

            state.message?.let {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(it, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
            }

            if (controlsActive || state.browseSearched) {
                BrowseResults(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                )
            } else {
                BrowseLanding(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (genresOpen) {
            BrowseGenreDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { genresOpen = false },
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
private fun BrowseFilterBar(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onOpenGenres: () -> Unit,
    onOpenPicker: (BrowsePicker) -> Unit,
    onOpenAdvanced: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier.width(260.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BrowseFilterLabel("Search")
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = viewModel::searchAniList) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.searchAniList() }),
            )
        }
        BrowseSelectField(
            label = "Genres",
            value = if (state.browseGenres.isEmpty()) "Any" else "${state.browseGenres.size} selected",
            width = 230.dp,
            onClick = onOpenGenres,
        )
        BrowseSelectField(
            label = "Format",
            value = BrowseFormatOptions.labelFor(state.browseFormat),
            onClick = { onOpenPicker(BrowsePicker.FORMAT) },
        )
        BrowseSelectField(
            label = "Publishing Status",
            value = BrowseStatusOptions.labelFor(state.browsePublishingStatus),
            width = 260.dp,
            onClick = { onOpenPicker(BrowsePicker.STATUS) },
        )
        BrowseSelectField(
            label = "Country Of Origin",
            value = BrowseCountryOptions.labelFor(state.browseCountryOfOrigin),
            width = 260.dp,
            onClick = { onOpenPicker(BrowsePicker.COUNTRY) },
        )
        BrowseSelectField(
            label = "Year",
            value = state.browseYear?.toString() ?: "Any",
            onClick = { onOpenPicker(BrowsePicker.YEAR) },
        )
        IconButton(
            onClick = onOpenAdvanced,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LocalTankobunTokens.current.elevatedSurface),
        ) {
            Icon(Icons.Default.Tune, contentDescription = "Advanced filters")
        }
    }
}

@Composable
private fun BrowseFilterLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun BrowseSelectField(
    label: String,
    value: String,
    width: androidx.compose.ui.unit.Dp = 230.dp,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BrowseFilterLabel(label)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
            color = LocalTankobunTokens.current.elevatedSurface,
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    value,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
    }
}

@Composable
private fun BrowseLanding(
    state: TankobunUiState,
    viewModel: MainViewModel,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(34.dp),
    ) {
        item {
            BrowseMangaShelf(
                title = "TRENDING NOW",
                media = state.browseTrending,
                onViewAll = { viewModel.viewAllBrowseSection("TRENDING_DESC") },
                onSelectMedia = viewModel::selectMedia,
            )
        }
        item {
            BrowseMangaShelf(
                title = "ALL TIME POPULAR",
                media = state.browsePopular,
                onViewAll = { viewModel.viewAllBrowseSection("POPULARITY_DESC") },
                onSelectMedia = viewModel::selectMedia,
            )
        }
    }
}

@Composable
private fun BrowseMangaShelf(
    title: String,
    media: List<AnilistMedia>,
    onViewAll: () -> Unit,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onViewAll) {
                Text("View All")
            }
        }
        if (media.isEmpty()) {
            Text(
                "Cached discovery will appear here after AniList responds.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                items(media, key = { it.id }) { item ->
                    BrowseShelfTile(media = item, onClick = { onSelectMedia(item) })
                }
            }
        }
    }
}

@Composable
private fun BrowseShelfTile(media: AnilistMedia, onClick: () -> Unit) {
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
        Text(
            media.title.userPreferred,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BrowseResults(
    state: TankobunUiState,
    viewModel: MainViewModel,
    modifier: Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (state.searchQuery.isBlank()) "Browse Manga" else "Search Results",
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
        MediaViewModeRow(
            selected = state.browseViewMode,
            onSelect = viewModel::setBrowseViewMode,
        )
        MediaCollection(
            media = state.searchResults,
            viewMode = state.browseViewMode,
            modifier = Modifier.weight(1f),
            onSelectMedia = viewModel::selectMedia,
            emptyMessage = if (state.busy) {
                "Searching AniList..."
            } else {
                "No AniList manga found for these filters."
            },
        )
    }
}

@Composable
private fun BrowseGenreDialog(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .fillMaxHeight(0.72f),
            shape = RoundedCornerShape(12.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DialogHeader(title = "Genres", onDismiss = onDismiss)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    gridItems(BrowseGenres, key = { it }) { genre ->
                        FilterChip(
                            selected = genre in state.browseGenres,
                            onClick = { viewModel.setBrowseGenre(genre, genre !in state.browseGenres) },
                            label = {
                                Text(genre, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                        )
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
                    Button(
                        onClick = {
                            onDismiss()
                            viewModel.searchAniList()
                        },
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseOptionDialog(
    title: String,
    options: List<BrowseOption>,
    selectedValue: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(380.dp)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(12.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DialogHeader(title = title, onDismiss = onDismiss)
                options.forEach { option ->
                    FilterChip(
                        selected = option.value == selectedValue,
                        onClick = { onSelect(option.value) },
                        label = { Text(option.label) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseAdvancedDialog(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.72f),
            shape = RoundedCornerShape(12.dp),
            color = LocalTankobunTokens.current.elevatedSurface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                DialogHeader(title = "Browse Options", onDismiss = onDismiss)
                Text("Sort", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRowCompat {
                    BrowseSortOptions.forEach { option ->
                        FilterChip(
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = viewModel::resetBrowseFilters) {
                        Text("Reset")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            onDismiss()
                            viewModel.searchAniList()
                        },
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(title: String, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

private fun List<BrowseOption>.labelFor(value: String?): String =
    firstOrNull { it.value == value }?.label ?: "Any"

private fun browseYearOptions(): List<BrowseOption> {
    val currentYear = java.time.Year.now().value
    return listOf(BrowseOption("Any", null)) +
        (currentYear downTo 1970).map { BrowseOption(it.toString(), it.toString()) }
}

private fun TankobunUiState.browseControlsActive(): Boolean =
    searchQuery.isNotBlank() ||
        browseGenres.isNotEmpty() ||
        browseFormat != null ||
        browsePublishingStatus != null ||
        browseCountryOfOrigin != null ||
        browseYear != null ||
        browseSort != BROWSE_SORT_SEARCH_MATCH_UI

private fun browseSummary(state: TankobunUiState): String {
    val parts = buildList {
        state.searchQuery.trim().takeIf { it.isNotBlank() }?.let { add("Search \"$it\"") }
        if (state.browseGenres.isNotEmpty()) add(state.browseGenres.sorted().joinToString(", "))
        state.browseFormat?.let { add(BrowseFormatOptions.labelFor(it)) }
        state.browsePublishingStatus?.let { add(BrowseStatusOptions.labelFor(it)) }
        state.browseCountryOfOrigin?.let { add(BrowseCountryOptions.labelFor(it)) }
        state.browseYear?.let { add(it.toString()) }
        BrowseSortOptions.labelFor(state.browseSort).takeIf { it != "Search Match" }?.let { add("Sort: $it") }
    }
    return parts.ifEmpty { listOf("AniList manga database") }.joinToString(" / ")
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
            MediaViewMode.MASONRY to "Flow",
            MediaViewMode.JUSTIFIED to "Large",
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
    emptyMessage: String = "No manga here yet.",
) {
    if (media.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            Text(emptyMessage)
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
            columns = StaggeredGridCells.Adaptive(150.dp),
            modifier = modifier,
            verticalItemSpacing = 16.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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
            columns = GridCells.Adaptive(if (viewMode == MediaViewMode.COVER_WITH_INFO) 180.dp else 140.dp),
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "Cover press scale",
    )
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
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            shadowElevation = if (pressed) 1.dp else 3.dp,
        ) {
            CoverImage(
                url = media.coverImage,
                title = media.title.userPreferred,
                modifier = coverModifier,
            )
        }
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = LocalTankobunTokens.current.elevatedSurface,
                    tonalElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
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
                            Text(media.title.userPreferred, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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

            if (state.selectedRecommendations.isNotEmpty()) {
                item {
                    RecommendationsSection(
                        recommendations = state.selectedRecommendations,
                        onSelectMedia = viewModel::selectMedia,
                    )
                }
            }
        }

        if (state.sourcePickerOpen) {
            SourcePickerDialog(state, viewModel, media)
        }
    }
}

@Composable
private fun AniListTrackingSection(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AniList", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            AssistChip(
                onClick = {},
                label = {
                    Text(state.selectedListEntry?.status?.displayName() ?: "Not tracked")
                },
            )
        }

        FlowRowCompat {
            trackingStatuses().forEach { status ->
                FilterChip(
                    selected = state.trackingStatus == status,
                    onClick = { viewModel.setTrackingStatus(status) },
                    label = { Text(status.displayName()) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.trackingProgress,
                onValueChange = viewModel::setTrackingProgress,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Progress") },
                suffix = { Text("/ ${media.chapters ?: "?"}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = state.trackingScore,
                onValueChange = viewModel::setTrackingScore,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Score") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }

        OutlinedTextField(
            value = state.trackingNotes,
            onValueChange = viewModel::setTrackingNotes,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            label = { Text("Notes") },
        )

        OutlinedTextField(
            value = state.trackingCustomLists,
            onValueChange = viewModel::setTrackingCustomLists,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Custom lists") },
            placeholder = { Text("Favorites, To buy") },
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Private", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = state.trackingPrivate, onCheckedChange = viewModel::setTrackingPrivate)
            Spacer(Modifier.weight(1f))
            Button(onClick = viewModel::saveTracking, enabled = state.loggedIn) {
                Text(if (state.selectedListEntry == null) "Track manga" else "Save AniList")
            }
        }
        if (!state.loggedIn) {
            Text(
                "Connect AniList to track, rate, and organize this manga.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecommendationsSection(
    recommendations: List<AnilistRecommendation>,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Recommendations", style = MaterialTheme.typography.titleLarge)
        recommendations.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { recommendation ->
                    MediaCoverTile(
                        media = recommendation.media,
                        viewMode = MediaViewMode.COVER_WITH_INFO,
                        onClick = { onSelectMedia(recommendation.media) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
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

private fun trackingStatuses(): List<MediaStatus> = listOf(
    MediaStatus.CURRENT,
    MediaStatus.PLANNING,
    MediaStatus.COMPLETED,
    MediaStatus.PAUSED,
    MediaStatus.DROPPED,
    MediaStatus.REPEATING,
)

private fun MediaStatus.displayName(): String = when (this) {
    MediaStatus.CURRENT -> "Reading"
    MediaStatus.PLANNING -> "Plan"
    MediaStatus.COMPLETED -> "Completed"
    MediaStatus.PAUSED -> "Paused"
    MediaStatus.DROPPED -> "Dropped"
    MediaStatus.REPEATING -> "Rereading"
    MediaStatus.UNKNOWN -> "Unknown"
}

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
                        .background(LocalTankobunTokens.current.readerOverlay)
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
                        .background(LocalTankobunTokens.current.readerOverlay)
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
