package com.tankobun.app.ui.home

import com.tankobun.app.ui.icons.TankobunIcons

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.LocalTankobunTokens
import com.tankobun.app.R
import com.tankobun.app.TankobunDisplayFontFamily
import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.tankobunString
import com.tankobun.app.logic.tabletHeroCharacterImages
import com.tankobun.app.logic.mobileHeroCharacterImages
import com.tankobun.app.ui.browse.browseGenreLabel
import com.tankobun.app.ui.components.TankobunMediaStatusLabel
import com.tankobun.app.ui.shell.LocalTankobunChromeInsets
import com.tankobun.app.ui.media.AutoResizingMangaTitle
import com.tankobun.core.model.AnilistGenreHighlight
import com.tankobun.core.model.AnilistMedia
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeScreen(
    state: TankobunUiState,
    onSelectMedia: (AnilistMedia) -> Unit,
    onOpenRecentProgress: (RecentReadingProgress) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenBrowse: () -> Unit,
) {
    val chromeInsets = LocalTankobunChromeInsets.current
    val expanded = androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp >= 600
    val horizontalPadding = 18.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = chromeInsets.top + 18.dp,
            bottom = chromeInsets.bottom + 18.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            HomeSection(
                title = tankobunString(R.string.home_trending),
                icon = TankobunIcons.Whatshot,
                onViewAll = onOpenBrowse,
                headerModifier = Modifier.padding(horizontal = horizontalPadding),
            ) {
                when {
                    state.homeTrending.isNotEmpty() -> TrendingHeroCarousel(
                        media = state.homeTrending,
                        expanded = expanded,
                        horizontalPadding = horizontalPadding,
                        onSelectMedia = onSelectMedia,
                    )
                    !state.homeLoaded -> Box(Modifier.padding(horizontal = horizontalPadding)) {
                        HomeLoadingPanel(Modifier.height(if (expanded) 410.dp else 338.dp))
                    }
                    else -> Box(Modifier.padding(horizontal = horizontalPadding)) {
                        HomeEmptyPanel(tankobunString(R.string.home_trending_empty))
                    }
                }
            }
        }

        if (state.recentReadingProgress.isNotEmpty()) {
            item {
                HomeSection(
                    title = tankobunString(R.string.home_continue_reading),
                    icon = TankobunIcons.MenuBook,
                    onViewAll = onOpenLibrary,
                    headerModifier = Modifier.padding(horizontal = horizontalPadding),
                ) {
                    ContinueReadingRow(
                        items = state.recentReadingProgress,
                        horizontalPadding = horizontalPadding,
                        onOpen = onOpenRecentProgress,
                    )
                }
            }
        }

        item {
            HomeSection(
                title = tankobunString(R.string.home_trending_by_genre),
                icon = TankobunIcons.TrendingUp,
                onViewAll = onOpenBrowse,
                headerModifier = Modifier.padding(horizontal = horizontalPadding),
            ) {
                Box(Modifier.padding(horizontal = horizontalPadding)) {
                    when {
                        state.homeGenreHighlights.isNotEmpty() -> GenreHighlightList(
                            highlights = state.homeGenreHighlights,
                            expanded = expanded,
                            onSelectMedia = onSelectMedia,
                        )
                        !state.homeLoaded -> HomeLoadingPanel(Modifier.height(220.dp))
                        else -> HomeEmptyPanel(tankobunString(R.string.home_genres_empty))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSection(
    title: String,
    icon: ImageVector,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = headerModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(27.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = TankobunDisplayFontFamily,
                    fontSize = 25.sp,
                    lineHeight = 26.sp,
                    letterSpacing = 1.2.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewAll)
                    .padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tankobunString(R.string.home_view_all),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = TankobunIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrendingHeroCarousel(
    media: List<AnilistMedia>,
    expanded: Boolean,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { media.size })
    LaunchedEffect(media.map { it.id }) {
        if (media.size <= 1) return@LaunchedEffect
        while (true) {
            delay(6_000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % media.size)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.fillMaxWidth()) {
            val heroHeight = if (expanded) 410.dp else 338.dp
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                pageSpacing = 10.dp,
            ) { page ->
                TrendingHero(
                    media = media[page],
                    rank = page + 1,
                    expanded = expanded,
                    onClick = { onSelectMedia(media[page]) },
                    modifier = Modifier.height(heroHeight),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            media.forEachIndexed { index, _ ->
                val selected = index == pagerState.currentPage
                Box(
                    Modifier
                        .width(if (selected) 24.dp else 8.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        ),
                )
            }
        }
    }
}

@Composable
private fun TrendingHero(
    media: AnilistMedia,
    rank: Int,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = LocalTankobunStyle.current
    val backdrop = style.colors.panel
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val mosaicImages = if (expanded) {
        media.tabletHeroCharacterImages(
            landscape = landscape,
            screenWidthDp = configuration.screenWidthDp,
        )
    } else {
        media.mobileHeroCharacterImages()
    }
    val showCharacterMosaic = mosaicImages.isNotEmpty()
    val image = if (expanded) {
        media.bannerImage ?: if (showCharacterMosaic) null else media.coverImage ?: media.mainCharacterImage
    } else {
        if (showCharacterMosaic) null else media.mainCharacterImage ?: media.coverImage ?: media.bannerImage
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = backdrop,
        contentColor = style.colors.panelContent,
        border = BorderStroke(1.dp, style.colors.outline.copy(alpha = 0.78f)),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (showCharacterMosaic) {
                HeroCharacterMosaic(
                    imageUrls = mosaicImages,
                    title = media.title.userPreferred,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(0.72f),
                )
            } else {
                image?.let { url ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .fillMaxWidth(if (expanded) 0.72f else 1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = media.title.userPreferred,
                            contentScale = if (expanded) ContentScale.Crop else ContentScale.FillHeight,
                            alignment = if (expanded) Alignment.Center else Alignment.CenterEnd,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = if (expanded) {
                                arrayOf(
                                    0.0f to backdrop,
                                    0.26f to backdrop.copy(alpha = 0.94f),
                                    0.36f to backdrop.copy(alpha = 0.70f),
                                    0.50f to backdrop.copy(alpha = 0.38f),
                                    0.64f to backdrop.copy(alpha = 0.14f),
                                    0.78f to Color.Transparent,
                                    1.0f to Color.Transparent,
                                )
                            } else {
                                arrayOf(
                                    0.0f to backdrop,
                                    0.26f to backdrop.copy(alpha = 0.95f),
                                    0.36f to backdrop.copy(alpha = 0.74f),
                                    0.52f to backdrop.copy(alpha = 0.42f),
                                    0.70f to backdrop.copy(alpha = 0.14f),
                                    1.0f to Color.Transparent,
                                )
                            },
                        ),
                    ),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.72f to Color.Transparent,
                            1.0f to backdrop.copy(alpha = 0.68f),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = style.colors.chip.copy(alpha = 0.82f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)),
                ) {
                    Text(
                        text = rank.toString().padStart(2, '0'),
                        fontFamily = TankobunDisplayFontFamily,
                        fontSize = 24.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
                if (expanded) {
                    Spacer(Modifier.height(18.dp))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                AutoResizingMangaTitle(
                    title = media.title.userPreferred,
                    compact = true,
                    color = style.colors.panelContent,
                    modifier = Modifier
                        .fillMaxWidth(if (expanded) 0.38f else 0.68f)
                        .height(if (expanded) 116.dp else 92.dp),
                )
                media.staff.firstOrNull()?.let { author ->
                    TankobunMediaStatusLabel(
                        text = tankobunString(R.string.home_by_author, author),
                        modifier = Modifier
                            .fillMaxWidth(if (expanded) 0.36f else 0.66f)
                            .padding(top = 4.dp),
                    )
                }
                media.cleanDescription()?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = style.colors.panelContent.copy(alpha = 0.86f),
                        lineHeight = 18.sp,
                        maxLines = if (expanded) 5 else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth(if (expanded) 0.38f else 0.62f)
                            .padding(top = 8.dp),
                    )
                }
                if (expanded) {
                    Spacer(Modifier.weight(1f))
                } else {
                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        media.genres.take(2).forEach { genre -> HeroGenreChip(genre) }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = style.colors.action,
                        contentColor = style.colors.actionContent,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = tankobunString(R.string.home_check_it_out),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(TankobunIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCharacterMosaic(
    imageUrls: List<String>,
    title: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val overlap = 14.dp
        val panelWidth = (maxWidth + overlap * (imageUrls.size - 1).toFloat()) / imageUrls.size
        imageUrls.forEachIndexed { index, imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .offset(x = (panelWidth - overlap) * index.toFloat())
                    .width(panelWidth)
                    .fillMaxHeight()
                    .clip(
                        SlantedHeroPanelShape(
                            first = index == 0,
                            last = index == imageUrls.lastIndex,
                        ),
                    ),
            )
        }
    }
}

private class SlantedHeroPanelShape(
    private val first: Boolean,
    private val last: Boolean,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val slant = (size.width * 0.10f).coerceAtMost(with(density) { 16.dp.toPx() })
        val path = Path().apply {
            moveTo(if (first) 0f else slant, 0f)
            lineTo(size.width, 0f)
            lineTo(if (last) size.width else size.width - slant, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun HeroGenreChip(genre: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = LocalTankobunStyle.current.colors.chip.copy(alpha = 0.8f),
        contentColor = LocalTankobunStyle.current.colors.chipContent,
        border = BorderStroke(1.dp, LocalTankobunStyle.current.colors.outline.copy(alpha = 0.46f)),
    ) {
        Text(
            text = browseGenreLabel(genre),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ContinueReadingRow(
    items: List<RecentReadingProgress>,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onOpen: (RecentReadingProgress) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.media.id }) { item ->
            ContinueReadingCard(item = item, width = 190.dp, onClick = { onOpen(item) })
        }
    }
}

@Composable
private fun ContinueReadingCard(
    item: RecentReadingProgress,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val progress = item.overallProgress ?: 0f
    Column(
        modifier = Modifier
            .width(width)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AsyncImage(
            model = item.media.coverImage,
            contentDescription = item.media.title.userPreferred,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Text(
            text = item.media.title.userPreferred,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.currentChapterNumber
                ?.let { chapterNumber -> tankobunString(R.string.home_chapter, chapterNumber.compactNumber()) }
                ?: item.chapter?.name
                ?: tankobunString(R.string.reader_saved_chapter),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Text(
                text = item.overallProgress?.let { "${(it * 100).roundToInt()}%" } ?: "—",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GenreHighlightList(
    highlights: List<AnilistGenreHighlight>,
    expanded: Boolean,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    if (expanded) {
        val splitIndex = (highlights.size + 1) / 2
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(highlights.take(splitIndex), highlights.drop(splitIndex)).forEach { columnItems ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    columnItems.forEach { highlight ->
                        GenreHighlightCard(
                            highlight = highlight,
                            expanded = true,
                            onClick = { onSelectMedia(highlight.media) },
                        )
                    }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            highlights.forEach { highlight ->
                GenreHighlightCard(
                    highlight = highlight,
                    expanded = false,
                    onClick = { onSelectMedia(highlight.media) },
                )
            }
        }
    }
}

@Composable
private fun GenreHighlightCard(
    highlight: AnilistGenreHighlight,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val media = highlight.media
    val image = media.bannerImage ?: media.mainCharacterImage ?: media.coverImage
    val style = LocalTankobunStyle.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (expanded) 70.dp else 74.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = style.colors.panel,
        contentColor = style.colors.panelContent,
        border = BorderStroke(1.dp, style.colors.outline.copy(alpha = 0.58f)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(if (expanded) 148.dp else 170.dp)
                    .fillMaxHeight(),
            ) {
                AsyncImage(
                    model = image,
                    contentDescription = media.title.userPreferred,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0f to style.colors.panel.copy(alpha = 0.2f),
                                0.82f to Color.Transparent,
                                1f to style.colors.panel,
                            ),
                        ),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TankobunMediaStatusLabel(text = browseGenreLabel(highlight.genre))
                Text(
                    text = media.title.userPreferred,
                    fontFamily = TankobunDisplayFontFamily,
                    fontSize = 20.sp,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeLoadingPanel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = LocalTankobunStyle.current.colors.panel,
        border = BorderStroke(1.dp, LocalTankobunStyle.current.colors.outline.copy(alpha = 0.45f)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun HomeEmptyPanel(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = LocalTankobunTokens.current.elevatedSurface,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
    }
}

private fun AnilistMedia.cleanDescription(): String? = description
    ?.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
    ?.replace(Regex("<[^>]*>"), "")
    ?.replace("&quot;", "\"")
    ?.replace("&#039;", "'")
    ?.replace("&amp;", "&")
    ?.replace(Regex("\\s+"), " ")
    ?.trim()
    ?.takeIf { it.isNotBlank() }

private fun Float.compactNumber(): String =
    if (this % 1f == 0f) toInt().toString() else toString().trimEnd('0').trimEnd('.')
