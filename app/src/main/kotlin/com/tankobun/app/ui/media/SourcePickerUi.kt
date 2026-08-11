package com.tankobun.app.ui.media

import com.tankobun.app.ui.icons.TankobunIcons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.MainViewModel
import com.tankobun.app.R
import com.tankobun.app.SourceThumbnailImageModel
import com.tankobun.app.tankobunQuantityString
import com.tankobun.app.tankobunString
import com.tankobun.app.logic.sourceMatchKey
import com.tankobun.app.logic.sourceSettingsKey
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.ui.components.TankobunDialogSurface
import com.tankobun.app.ui.components.TankobunIconActionButton
import com.tankobun.app.ui.components.TankobunMessageBanner
import com.tankobun.app.ui.settings.ExtensionIcon
import com.tankobun.app.ui.settings.extensionInitials
import com.tankobun.app.ui.settings.normalizedSourceLanguage
import com.tankobun.app.ui.settings.sourceLanguageDisplay
import com.tankobun.app.ui.settings.sourceLanguageSortPriority
import com.tankobun.app.ui.settings.sourceMetadata
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceSearchResult

@Composable
internal fun SourceSummarySection(state: TankobunUiState, viewModel: MainViewModel) {
    val selectedManga = state.selectedSourceManga
    val selectedSource = state.selectedSource

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailSectionTitle(tankobunString(R.string.common_source))
        if (selectedManga == null) {
            SourceActionCard(
                title = tankobunString(R.string.source_none_selected),
                subtitle = if (state.allInstalledSources.isEmpty()) {
                    tankobunString(R.string.source_install_in_settings)
                } else {
                    tankobunString(R.string.source_choose_to_read)
                },
                onFindSource = viewModel::openSourcePicker,
            )
        } else {
            SelectedSourceCard(
                source = selectedSource,
                sourceName = selectedSource?.let { "${it.name} (${sourceLanguageDisplay(it.lang)})" }
                    ?: tankobunString(R.string.source_selected_source),
                chapterLine = if (state.sourceChapters.isEmpty()) {
                    tankobunString(R.string.source_no_chapters_loaded)
                } else {
                    tankobunQuantityString(R.plurals.chapter_count, state.sourceChapters.size, state.sourceChapters.size)
                },
                onChange = viewModel::openSourcePicker,
            )
        }
    }
}

@Composable
internal fun SourceActionCard(
    title: String,
    subtitle: String,
    onFindSource: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalTankobunStyle.current.themeShapes.panel,
        color = mediaDetailPanelColor(),
        contentColor = mediaDetailForegroundColor(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailIconBadge(icon = TankobunIcons.Link)
            SourceActionText(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
            TankobunIconActionButton(
                icon = TankobunIcons.Search,
                contentDescription = tankobunString(R.string.source_find_source),
                onClick = onFindSource,
            )
        }
    }
}

@Composable
internal fun SourceActionText(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SelectedSourceCard(
    source: SourceDescriptor?,
    sourceName: String,
    chapterLine: String,
    onChange: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalTankobunStyle.current.themeShapes.panel,
        color = mediaDetailPanelColor(),
        contentColor = mediaDetailForegroundColor(),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 430.dp
            val iconSize = if (compact) 48.dp else 52.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (compact) 12.dp else 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlainSourceIcon(
                    source = source,
                    fallbackName = sourceName,
                    modifier = Modifier.size(iconSize),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        sourceName,
                        style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        chapterLine,
                        style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TankobunIconActionButton(
                    icon = TankobunIcons.SwapHoriz,
                    contentDescription = tankobunString(R.string.source_change_source),
                    onClick = onChange,
                )
            }
        }
    }
}

@Composable
internal fun PlainSourceIcon(
    source: SourceDescriptor?,
    fallbackName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sourceIcon = remember(source?.packageName) {
        source?.packageName?.let { packageName ->
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toSourceImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        modifier = modifier.clip(LocalTankobunStyle.current.themeShapes.control),
        contentAlignment = Alignment.Center,
    ) {
        if (sourceIcon != null) {
            Image(
                bitmap = sourceIcon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = LocalTankobunStyle.current.themeShapes.control,
                color = mediaDetailAccentColor().copy(alpha = 0.16f),
                contentColor = mediaDetailAccentColor(),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        extensionInitials(fallbackName),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun Drawable.toSourceImageBitmap(): ImageBitmap {
    val drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this is AdaptiveIconDrawable) {
        foreground ?: this
    } else {
        this
    }
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 192
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 192
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    val visibleBounds = bitmap.visibleAlphaBounds()
    val trimmed = if (visibleBounds != null && visibleBounds.width() > 0 && visibleBounds.height() > 0) {
        Bitmap.createBitmap(bitmap, visibleBounds.left, visibleBounds.top, visibleBounds.width(), visibleBounds.height())
    } else {
        bitmap
    }
    return trimmed.asImageBitmap()
}

private fun Bitmap.visibleAlphaBounds(alphaThreshold: Int = 8): Rect? {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val alpha = pixels[rowOffset + x] ushr 24
            if (alpha > alphaThreshold) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }
    return if (right >= left && bottom >= top) {
        Rect(left, top, right + 1, bottom + 1)
    } else {
        null
    }
}

@Composable
internal fun SourcePickerDialog(state: TankobunUiState, viewModel: MainViewModel, media: AnilistMedia) {
    val matches = state.sourceMatches.filter { match ->
        state.sourceMatchChapterCounts[match.source.sourceMatchKey(match.manga.url)] != null
    }
    val availableSources = remember(state.installedSources, state.selectedSourceId, state.selectedSourcePackageName) {
        state.installedSources
            .distinctBy { it.sourceSettingsKey() }
            .sortedWith(
                compareBy<SourceDescriptor> {
                    if (
                        it.id == state.selectedSourceId &&
                        (state.selectedSourcePackageName == null ||
                            state.selectedSourcePackageName == it.packageName)
                    ) {
                        0
                    } else {
                        1
                    }
                }
                    .thenBy { sourceLanguageSortPriority(it.lang.normalizedSourceLanguage()) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.lang },
            )
    }
    val matchSourceKeys = remember(matches) {
        matches.mapTo(mutableSetOf()) { it.source.sourceSettingsKey() }
    }
    val diagnostics = state.sourcePickerDiagnostics
    var searchTitleEditorOpen by remember(media.id) { mutableStateOf(false) }
    val showSearchTitleEditor = searchTitleEditorOpen ||
        (matches.isEmpty() && availableSources.isNotEmpty() && !state.sourcePickerLoading)

    Dialog(
        onDismissRequest = viewModel::closeSourcePicker,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        TankobunDialogSurface(maxWidth = 720.dp, fillMaxHeightFraction = 0.86f, scrollable = false) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(tankobunString(R.string.source_find_source), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        tankobunString(R.string.source_enabled_count, media.title.userPreferred, availableSources.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = viewModel::closeSourcePicker) {
                    Text(tankobunString(R.string.common_close))
                }
            }

            if (state.sourcePickerLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            state.sourcePickerMessage?.let { pickerMessage ->
                TankobunMessageBanner(pickerMessage)
            }

            AnimatedVisibility(showSearchTitleEditor) {
                SourcePickerSearchTitleEditor(
                    title = state.sourcePickerSearchTitle,
                    enabled = availableSources.isNotEmpty(),
                    onTitleChange = viewModel::updateSourcePickerSearchTitle,
                    onSearch = viewModel::findSourceMatchesWithEditedTitle,
                )
            }

            if (matches.isEmpty() && availableSources.isEmpty() && !state.sourcePickerLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(tankobunString(R.string.source_no_enabled), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            tankobunString(R.string.source_enable_or_install),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (matches.isNotEmpty()) {
                        item {
                            Text(tankobunString(R.string.source_readable_matches), style = MaterialTheme.typography.titleMedium)
                        }
                        items(matches, key = { "match:${it.source.sourceMatchKey(it.manga.url)}" }) { match ->
                            val count = state.sourceMatchChapterCounts[match.source.sourceMatchKey(match.manga.url)] ?: 0
                            SourceMatchRow(
                                match = match,
                                chapterCount = count,
                                current = state.selectedSourceId == match.source.id &&
                                    (state.selectedSourcePackageName == null ||
                                        state.selectedSourcePackageName == match.source.packageName) &&
                                    state.selectedSourceManga?.url == match.manga.url,
                                mediaCover = media.coverImage,
                                onClick = { viewModel.bindSourceMatch(match) },
                            )
                        }
                    }
                    val fallbackSources = availableSources.filterNot { it.sourceSettingsKey() in matchSourceKeys }
                    if (fallbackSources.isNotEmpty()) {
                        item {
                            Text(tankobunString(R.string.source_try_specific), style = MaterialTheme.typography.titleMedium)
                        }
                        items(fallbackSources, key = { "source:${it.sourceSettingsKey()}" }) { source ->
                            SourceCandidateRow(
                                source = source,
                                current = state.selectedSourceId == source.id &&
                                    (state.selectedSourcePackageName == null ||
                                        state.selectedSourcePackageName == source.packageName),
                                onClick = { viewModel.bindSource(source) },
                            )
                        }
                    }
                    if (diagnostics.isNotEmpty()) {
                        item {
                            Text(tankobunString(R.string.source_skipped_sources), style = MaterialTheme.typography.titleMedium)
                        }
                        items(diagnostics, key = { "diagnostic:$it" }) { diagnostic ->
                            SourceDiagnosticRow(diagnostic)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { searchTitleEditorOpen = true },
                    enabled = availableSources.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(TankobunIcons.Tune, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(tankobunString(R.string.source_edit_search_title))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.sourcePickerLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(
                        tankobunString(R.string.source_readable_enabled_count, matches.size, availableSources.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SourcePickerSearchTitleEditor(
    title: String,
    enabled: Boolean,
    onTitleChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
            val compact = maxWidth < 420.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SourceSearchTitleField(
                        title = title,
                        enabled = enabled,
                        onTitleChange = onTitleChange,
                        onSearch = onSearch,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = onSearch,
                        enabled = enabled && title.trim().length >= 2,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(TankobunIcons.Search, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(tankobunString(R.string.common_search))
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SourceSearchTitleField(
                        title = title,
                        enabled = enabled,
                        onTitleChange = onTitleChange,
                        onSearch = onSearch,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = onSearch,
                        enabled = enabled && title.trim().length >= 2,
                    ) {
                        Icon(TankobunIcons.Search, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(tankobunString(R.string.common_search))
                    }
                }
            }
        }
    }
}

@Composable
internal fun SourceSearchTitleField(
    title: String,
    enabled: Boolean,
    onTitleChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        label = { Text(tankobunString(R.string.source_search_title)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { if (enabled && title.trim().length >= 2) onSearch() }),
    )
}

@Composable
internal fun SourceDiagnosticRow(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
internal fun SourceMatchRow(
    match: SourceSearchResult,
    chapterCount: Int,
    current: Boolean,
    mediaCover: String?,
    onClick: () -> Unit,
) {
    val sourceThumbnailUrl = match.manga.thumbnailUrl?.takeIf(String::isNotBlank)
    var useMediaCover by remember(sourceThumbnailUrl, mediaCover) { mutableStateOf(false) }
    val imageUrl = if (useMediaCover) mediaCover else sourceThumbnailUrl ?: mediaCover
    val imageModel = if (!useMediaCover && sourceThumbnailUrl != null) {
        SourceThumbnailImageModel(match.source, sourceThumbnailUrl)
    } else {
        mediaCover
    }
    ElevatedCard(onClick = onClick, shape = LocalTankobunStyle.current.themeShapes.panel) {
        ListItem(
            headlineContent = { Text(match.manga.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                val chapterCountLabel = tankobunQuantityString(R.plurals.chapter_count, chapterCount, chapterCount)
                Text(
                    tankobunString(
                        R.string.source_match_detail,
                        match.source.name,
                        sourceLanguageDisplay(match.source.lang),
                        chapterCountLabel,
                        (match.score * 100).toInt(),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                CoverImage(
                    url = imageUrl,
                    title = match.manga.title,
                    modifier = Modifier.size(width = 48.dp, height = 68.dp),
                    imageModel = imageModel,
                    onImageError = {
                        if (!useMediaCover && mediaCover != null) useMediaCover = true
                    },
                )
            },
            trailingContent = {
                if (current) {
                    Text(tankobunString(R.string.common_current), color = MaterialTheme.colorScheme.secondary)
                }
            },
        )
    }
}

@Composable
internal fun SourceCandidateRow(
    source: SourceDescriptor,
    current: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick, shape = LocalTankobunStyle.current.themeShapes.panel) {
        ListItem(
            headlineContent = { Text(source.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Text(
                    sourceMetadata(source),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                ExtensionIcon(
                    packageName = source.packageName,
                    name = source.name,
                    iconUrl = null,
                    modifier = Modifier.size(42.dp),
                )
            },
            trailingContent = {
                Text(
                    if (current) tankobunString(R.string.common_selected) else tankobunString(R.string.common_try),
                    color = MaterialTheme.colorScheme.secondary,
                )
            },
        )
    }
}
