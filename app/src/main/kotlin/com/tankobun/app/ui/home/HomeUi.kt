package com.tankobun.app.ui.home

import com.tankobun.app.ui.icons.TankobunIcons

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.LocalTankobunTokens
import com.tankobun.app.R
import com.tankobun.app.TankobunDisplayFontFamily
import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.tankobunString
import com.tankobun.app.ui.shell.LocalTankobunChromeInsets
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
    val horizontalPadding = if (androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp >= 600) {
        28.dp
    } else {
        12.dp
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = chromeInsets.top + 14.dp,
            bottom = chromeInsets.bottom + 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            HomeSection(
                title = tankobunString(R.string.home_trending),
                icon = TankobunIcons.Whatshot,
                onViewAll = onOpenBrowse,
                modifier = Modifier.padding(horizontal = horizontalPadding),
            ) {
                when {
                    state.homeTrending.isNotEmpty() -> TrendingHeroCarousel(
                        media = state.homeTrending,
                        onSelectMedia = onSelectMedia,
                    )
                    !state.homeLoaded -> HomeLoadingPanel(Modifier.height(338.dp))
                    else -> HomeEmptyPanel(tankobunString(R.string.home_trending_empty))
                }
            }
        }

        if (state.recentReadingProgress.isNotEmpty()) {
            item {
                HomeSection(
                    title = tankobunString(R.string.home_continue_reading),
                    icon = TankobunIcons.MenuBook,
                    onViewAll = onOpenLibrary,
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
                modifier = Modifier.padding(horizontal = horizontalPadding),
            ) {
                when {
                    state.homeGenreHighlights.isNotEmpty() -> GenreHighlightList(
                        highlights = state.homeGenreHighlights,
                        onSelectMedia = onSelectMedia,
                    )
                    !state.homeLoaded -> HomeLoadingPanel(Modifier.height(220.dp))
                    else -> HomeEmptyPanel(tankobunString(R.string.home_genres_empty))
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
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val heroHeight = if (maxWidth >= 600.dp) 410.dp else 338.dp
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 10.dp,
            ) { page ->
                TrendingHero(
                    media = media[page],
                    rank = page + 1,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = LocalTankobunStyle.current
    val backdrop = style.colors.panel
    val image = media.mainCharacterImage ?: media.coverImage ?: media.bannerImage
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
            image?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = media.title.userPreferred,
                    contentScale = ContentScale.FillHeight,
                    alignment = Alignment.CenterEnd,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxSize()
                        .widthIn(min = 220.dp),
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to backdrop,
                            0.38f to backdrop.copy(alpha = 0.98f),
                            0.70f to backdrop.copy(alpha = 0.55f),
                            1.0f to Color.Transparent,
                        ),
                    ),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.68f to Color.Transparent,
                            1.0f to backdrop.copy(alpha = 0.9f),
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
                Spacer(Modifier.weight(1f))
                Text(
                    text = media.title.userPreferred.uppercase(),
                    fontFamily = TankobunDisplayFontFamily,
                    fontSize = 42.sp,
                    lineHeight = 40.sp,
                    letterSpacing = 0.6.sp,
                    color = style.colors.panelContent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.68f),
                )
                media.staff.firstOrNull()?.let { author ->
                    Text(
                        text = tankobunString(R.string.home_by_author, author),
                        style = MaterialTheme.typography.bodyMedium,
                        color = style.colors.mutedContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth(0.66f)
                            .padding(top = 4.dp),
                    )
                }
                media.cleanDescription()?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = style.colors.panelContent.copy(alpha = 0.86f),
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
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
private fun HeroGenreChip(genre: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = LocalTankobunStyle.current.colors.chip.copy(alpha = 0.8f),
        contentColor = LocalTankobunStyle.current.colors.chipContent,
        border = BorderStroke(1.dp, LocalTankobunStyle.current.colors.outline.copy(alpha = 0.46f)),
    ) {
        Text(
            text = genre,
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
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val available = maxWidth - horizontalPadding * 2
        val cardWidth = ((available - 30.dp) / 4).coerceIn(92.dp, 150.dp)
        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.media.id }) { item ->
                ContinueReadingCard(item = item, width = cardWidth, onClick = { onOpen(item) })
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(
    item: RecentReadingProgress,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val progress = if (item.progress.totalPages > 0) {
        ((item.progress.pageIndex + 1).toFloat() / item.progress.totalPages).coerceIn(0f, 1f)
    } else if (item.progress.completed) {
        1f
    } else {
        0f
    }
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
                .aspectRatio(0.72f)
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
            text = tankobunString(R.string.home_chapter, item.progress.chapterNumber.compactNumber()),
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
                text = "${(progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GenreHighlightList(
    highlights: List<AnilistGenreHighlight>,
    onSelectMedia: (AnilistMedia) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        highlights.forEach { highlight ->
            GenreHighlightCard(highlight = highlight, onClick = { onSelectMedia(highlight.media) })
        }
    }
}

@Composable
private fun GenreHighlightCard(
    highlight: AnilistGenreHighlight,
    onClick: () -> Unit,
) {
    val media = highlight.media
    val image = media.bannerImage ?: media.mainCharacterImage ?: media.coverImage
    val style = LocalTankobunStyle.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = style.colors.panel,
        contentColor = style.colors.panelContent,
        border = BorderStroke(1.dp, style.colors.outline.copy(alpha = 0.58f)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(170.dp)
                    .height(56.dp),
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
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .size(40.dp),
                    shape = RoundedCornerShape(7.dp),
                    color = style.colors.chip.copy(alpha = 0.88f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(1.dp, style.colors.outline.copy(alpha = 0.52f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = highlight.genre.genreIcon(),
                            contentDescription = highlight.genre,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = highlight.genre.uppercase(),
                    fontFamily = TankobunDisplayFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Text(
                    text = media.title.userPreferred,
                    fontFamily = TankobunDisplayFontFamily,
                    fontSize = 18.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.padding(end = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = TankobunIcons.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = media.popularity.compactPopularity(),
                    style = MaterialTheme.typography.bodySmall,
                    color = style.colors.mutedContent,
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

private fun Int?.compactPopularity(): String {
    val value = this ?: return "—"
    return when {
        value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000f)}M"
        value >= 1_000 -> "${"%.1f".format(value / 1_000f)}K"
        else -> value.toString()
    }
}

private fun String.genreIcon(): ImageVector = when (this) {
    "Action" -> TankobunIcons.GenreAction
    "Adventure" -> TankobunIcons.GenreAdventure
    "Comedy" -> TankobunIcons.GenreComedy
    "Drama" -> TankobunIcons.GenreDrama
    "Ecchi" -> TankobunIcons.GenreEcchi
    "Fantasy" -> TankobunIcons.GenreFantasy
    "Horror" -> TankobunIcons.GenreHorror
    "Mahou Shoujo" -> TankobunIcons.GenreMahouShoujo
    "Mecha" -> TankobunIcons.GenreMecha
    "Music" -> TankobunIcons.GenreMusic
    "Mystery" -> TankobunIcons.GenreMystery
    "Psychological" -> TankobunIcons.GenrePsychological
    "Romance" -> TankobunIcons.GenreRomance
    "Sci-Fi" -> TankobunIcons.GenreSciFi
    "Slice of Life" -> TankobunIcons.GenreSliceOfLife
    "Sports" -> TankobunIcons.GenreSports
    "Supernatural" -> TankobunIcons.GenreSupernatural
    "Thriller" -> TankobunIcons.GenreThriller
    else -> TankobunIcons.Category
}
