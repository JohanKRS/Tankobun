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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemePicker(
    selected: TankobunThemePreference,
    onSelect: (TankobunThemePreference) -> Unit,
) {
    val normalized = selected.normalized()
    val directions = tankobunArtDirectionChoices()
    val palettes = tankobunPaletteChoices()
    val currentDirectionName = tankobunString(normalized.direction.themeNameRes())
    val currentPaletteName = tankobunString(normalized.palette.themeNameRes())
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(tankobunString(R.string.settings_theme_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TankobunChip(
                selected = normalized.automatic,
                onClick = { onSelect(normalized.copy(automatic = true)) },
                label = { Text(tankobunString(R.string.settings_theme_automatic)) },
            )
            TankobunChip(
                selected = !normalized.automatic,
                onClick = { onSelect(normalized.copy(automatic = false)) },
                label = { Text(tankobunString(R.string.settings_theme_custom)) },
            )
        }
        if (normalized.automatic) {
            TankobunPanel(color = LocalTankobunTokens.current.softAccent) {
                Text(
                    tankobunString(R.string.settings_theme_automatic_desc),
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Text(tankobunString(R.string.settings_theme_art_direction), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                directions.forEach { choice ->
                    ArtDirectionCard(
                        modifier = Modifier.weight(1f),
                        choice = choice,
                        selected = choice.id == normalized.direction,
                        onClick = {
                            onSelect(normalized.copy(automatic = false, direction = choice.id))
                        },
                    )
                }
            }
            Text(tankobunString(R.string.settings_theme_palette), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                palettes.forEach { choice ->
                    PaletteChoiceCard(
                        modifier = Modifier.weight(1f),
                        choice = choice,
                        selected = choice.id == normalized.palette,
                        onClick = { onSelect(normalized.copy(automatic = false, palette = choice.id)) },
                    )
                }
            }
        }
        Text(tankobunString(R.string.settings_theme_preview), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        ThemeSampler(
            title = if (normalized.automatic) tankobunString(R.string.settings_theme_automatic) else currentDirectionName,
            subtitle = if (normalized.automatic) tankobunString(R.string.settings_theme_automatic_pair) else currentPaletteName,
        )
    }
}

@Composable
private fun ArtDirectionCard(
    modifier: Modifier = Modifier,
    choice: TankobunArtDirectionChoice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val previewShape = tankobunThemeShapeSet(choice.id).control
    val secondaryTextColor = if (selected) {
        LocalTankobunStyle.current.colors.selectedChipContent.copy(alpha = 0.78f)
    } else {
        LocalTankobunStyle.current.colors.mutedContent
    }
    Surface(
        modifier = modifier.height(74.dp).clickable(onClick = onClick),
        shape = previewShape,
        color = if (selected) LocalTankobunStyle.current.colors.selectedChip else LocalTankobunStyle.current.colors.panel,
        contentColor = if (selected) LocalTankobunStyle.current.colors.selectedChipContent else LocalTankobunStyle.current.colors.panelContent,
        border = BorderStroke(
            if (selected) LocalTankobunStyle.current.strokes.emphasizedWidth else LocalTankobunStyle.current.strokes.defaultWidth,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(modifier = Modifier.size(34.dp), shape = previewShape, color = MaterialTheme.colorScheme.secondary) {}
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tankobunString(choice.id.themeNameRes()), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(tankobunString(choice.id.themeDescriptionRes()), style = MaterialTheme.typography.labelSmall, color = secondaryTextColor, maxLines = 2)
            }
        }
    }
}

@Composable
private fun PaletteChoiceCard(
    modifier: Modifier = Modifier,
    choice: TankobunPaletteChoice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val secondaryTextColor = if (selected) {
        LocalTankobunStyle.current.colors.selectedChipContent.copy(alpha = 0.78f)
    } else {
        LocalTankobunStyle.current.colors.mutedContent
    }
    Surface(
        modifier = modifier.height(58.dp).clickable(onClick = onClick),
        shape = LocalTankobunStyle.current.themeShapes.control,
        color = if (selected) LocalTankobunStyle.current.colors.selectedChip else LocalTankobunStyle.current.colors.panel,
        contentColor = if (selected) LocalTankobunStyle.current.colors.selectedChipContent else LocalTankobunStyle.current.colors.panelContent,
        border = BorderStroke(
            LocalTankobunStyle.current.strokes.defaultWidth,
            if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.36f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            ThemeSwatches(choice.swatches)
            Column {
                Text(tankobunString(choice.id.themeNameRes()), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    if (choice.dark) tankobunString(R.string.common_dark) else tankobunString(R.string.common_light),
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryTextColor,
                )
            }
        }
    }
}

@Composable
private fun ThemeSampler(title: String, subtitle: String) {
    TankobunPanel(
        modifier = Modifier.fillMaxWidth(),
        color = LocalTankobunTokens.current.gradientStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = LocalTankobunStyle.current.themeShapes.indicator,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Box(contentAlignment = Alignment.Center) { Text("01", fontWeight = FontWeight.Bold) } }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalTankobunStyle.current.colors.mutedContent)
            }
            TankobunChip(selected = true, onClick = {}, label = { Text(tankobunString(R.string.settings_theme_sample_tag)) })
            TankobunActionButton(label = tankobunString(R.string.settings_theme_sample_action), onClick = {})
        }
    }
}

@Composable
internal fun ThemeSwatches(colors: List<Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy((-7).dp), verticalAlignment = Alignment.CenterVertically) {
        colors.take(3).forEach { color ->
            Surface(
                modifier = Modifier.size(24.dp),
                shape = RoundedCornerShape(999.dp),
                color = color,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {}
        }
    }
}
