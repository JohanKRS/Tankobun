package com.tankobun.app.ui.settings

import android.content.Context
import com.tankobun.app.ui.icons.TankobunIcons
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
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ThemePicker(
    selected: TankobunThemePreference,
    onSelect: (TankobunThemePreference) -> Unit,
) {
    val normalized = selected.normalized()
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val palettes = remember { tankobunPaletteChoices() }
    val dynamicAvailable = remember { dynamicColorAvailable() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(156.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val previewModes = when (normalized.mode) {
                TankobunColorMode.SYSTEM -> listOf(false, true)
                TankobunColorMode.LIGHT -> listOf(false)
                TankobunColorMode.DARK -> listOf(true)
            }
            previewModes.forEach { dark ->
                ThemePreviewCard(
                    colors = remember(normalized, dark, context) { tankobunColorScheme(normalized, dark, context) },
                    direction = normalized.direction,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }

        ThemeOptionLabel(tankobunString(R.string.settings_theme_mode))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val modes = TankobunColorMode.entries
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = normalized.mode == mode,
                    onClick = { onSelect(normalized.copy(mode = mode)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                ) {
                    Text(tankobunString(mode.themeNameRes()), maxLines = 1)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(
                    value = normalized.pureBlack,
                    role = Role.Switch,
                    onValueChange = { onSelect(normalized.copy(pureBlack = it)) },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tankobunString(R.string.settings_theme_pure_black), style = MaterialTheme.typography.bodyLarge)
                Text(
                    tankobunString(R.string.settings_theme_pure_black_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = normalized.pureBlack, onCheckedChange = null)
        }

        ThemeOptionLabel(tankobunString(R.string.settings_theme_palette))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 560.dp) 6 else 4
            val spacing = 6.dp
            // Leave a pixel of slack so rounding never pushes the last column to a new row.
            val itemWidth = (maxWidth - spacing * (columns - 1)) / columns - 1.dp
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = columns,
            ) {
                palettes.forEach { choice ->
                    PaletteSwatchButton(
                        name = tankobunString(choice.id.themeNameRes()),
                        description = tankobunString(choice.id.themeDescriptionRes()),
                        light = choice.lightSwatches,
                        dark = choice.darkSwatches,
                        showDark = normalized.isDark(systemDark),
                        selected = normalized.palette == choice.id,
                        onClick = { onSelect(normalized.copy(palette = choice.id)) },
                        modifier = Modifier.width(itemWidth),
                    )
                }
                if (dynamicAvailable) {
                    val dynamicLight = remember(context) { tankobunColorScheme(TankobunThemePreference(palette = TankobunPaletteId.DYNAMIC), false, context) }
                    val dynamicDark = remember(context) { tankobunColorScheme(TankobunThemePreference(palette = TankobunPaletteId.DYNAMIC), true, context) }
                    PaletteSwatchButton(
                        name = tankobunString(TankobunPaletteId.DYNAMIC.themeNameRes()),
                        description = tankobunString(TankobunPaletteId.DYNAMIC.themeDescriptionRes()),
                        light = listOf(dynamicLight.background, dynamicLight.primary),
                        dark = listOf(dynamicDark.background, dynamicDark.primary),
                        showDark = normalized.isDark(systemDark),
                        selected = normalized.palette == TankobunPaletteId.DYNAMIC,
                        onClick = { onSelect(normalized.copy(palette = TankobunPaletteId.DYNAMIC)) },
                        modifier = Modifier.width(itemWidth),
                    )
                }
            }
        }
        Text(
            tankobunString(normalized.palette.themeDescriptionRes()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ThemeOptionLabel(tankobunString(R.string.settings_theme_corners))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val directions = tankobunArtDirectionChoices()
            directions.forEachIndexed { index, choice ->
                SegmentedButton(
                    selected = normalized.direction == choice.id,
                    onClick = { onSelect(normalized.copy(direction = choice.id)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = directions.size),
                ) {
                    Text(tankobunString(choice.id.themeNameRes()), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A miniature screen drawn with the candidate theme's own colors, independent of the current theme. */
@Composable
private fun ThemePreviewCard(
    colors: androidx.compose.material3.ColorScheme,
    direction: TankobunArtDirection,
    modifier: Modifier = Modifier,
) {
    val shapes = tankobunThemeShapeSet(direction)
    Surface(
        modifier = modifier,
        shape = shapes.panel,
        color = colors.background,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 60.dp)
                        .clip(shapes.cover)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(colors.tertiary, colors.primary),
                            ),
                        ),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Tankobun",
                        style = LocalTankobunStyle.current.typography.sectionLabel,
                        maxLines = 1,
                    )
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .fillMaxWidth(0.7f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.primary),
                    )
                    Box(
                        modifier = Modifier
                            .clip(shapes.chip)
                            .background(colors.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            tankobunString(R.string.settings_theme_sample_tag),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSecondaryContainer,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(shapes.control)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tankobunString(R.string.settings_theme_sample_action),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onPrimary,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(50))
                    .background(colors.surfaceContainerHigh)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.size(width = 30.dp, height = 16.dp).clip(RoundedCornerShape(50)).background(colors.primaryContainer))
                repeat(3) { Box(Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
private fun PaletteSwatchButton(
    name: String,
    description: String,
    light: List<Color>,
    dark: List<Color>,
    showDark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ringColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val dotColor = (if (showDark) dark else light).getOrElse(1) { MaterialTheme.colorScheme.primary }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "$name. $description" }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(2.dp, ringColor, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .drawBehind {
                    drawRect(light.firstOrNull() ?: Color.White)
                    val half = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(half, dark.firstOrNull() ?: Color.Black)
                    drawCircle(dotColor, radius = size.minDimension * 0.2f)
                    drawCircle(
                        Color.White.copy(alpha = 0.85f),
                        radius = size.minDimension * 0.2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    TankobunIcons.Check,
                    contentDescription = null,
                    tint = if (dotColor.luminance() > 0.5f) Color.Black else Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
