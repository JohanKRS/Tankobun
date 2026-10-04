package com.tankobun.app.ui.filters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.R
import com.tankobun.app.tankobunString
import com.tankobun.app.tankobunLocale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import com.tankobun.app.TankobunDisplayFontFamily
import com.tankobun.app.tankobunQuantityString
import com.tankobun.app.ui.browse.*
import com.tankobun.app.ui.components.*
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.app.ui.icons.genreIcon
import com.tankobun.core.model.CatalogTag
import com.tankobun.core.model.CatalogTaxonomy
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * Phones get a bottom sheet within thumb reach; tablets keep a centered dialog. Wheel pickers
 * opt out of the sheet so their vertical drags never fight the sheet's own drag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    expanded: Boolean = false,
    fitContent: Boolean = false,
    sheetOnPhone: Boolean = true,
    footer: (@Composable () -> Unit)? = null,
    // "2 selecionados" or "412 opções", set beside the title.
    count: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    if (sheetOnPhone && configuration.smallestScreenWidthDp in 1 until 600) {
        val maxSheetHeight = configuration.screenHeightDp.dp * 0.88f
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = LocalTankobunStyle.current.colors.panel,
            contentColor = LocalTankobunStyle.current.colors.panelContent,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (expanded) Modifier.height(maxSheetHeight) else Modifier.heightIn(max = maxSheetHeight))
                    .imePadding()
                    .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        style = TextStyle(fontFamily = TankobunDisplayFontFamily, fontSize = 30.sp, lineHeight = 30.sp, letterSpacing = 1.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    count?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(bottom = 3.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                Column(Modifier.weight(1f, fill = expanded), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
                if (footer != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    footer()
                }
            }
        }
        return
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.imePadding(), contentAlignment = Alignment.Center) {
            // Base the options height on the screen, not the dialog's own wrap-content measurement.
            val heightLimit = if (fitContent) minOf((LocalConfiguration.current.screenHeightDp.dp - 48.dp).coerceAtLeast(240.dp), 720.dp) else 720.dp
            val widthLimit = if (fitContent) 820.dp else 620.dp
            val size = Modifier.widthIn(max = widthLimit).then(
                when {
                    fitContent -> Modifier.height(heightLimit)
                    expanded -> Modifier.height(minOf(maxHeight * 0.82f, 720.dp))
                    else -> Modifier
                },
            )
            TankobunDialogSurface(modifier = size, maxWidth = widthLimit, maxHeight = heightLimit, scrollable = false) {
                TankobunDialogHeader(title, onDismiss)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                Column(Modifier.weight(1f, fill = expanded || fitContent), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
                if (footer != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    footer()
                }
            }
        }
    }
}

@Composable
internal fun FilterDialogActions(selectedCount: Int? = null, enabled: Boolean = true, onClear: () -> Unit, onApply: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onClear) { Text(tankobunString(R.string.common_clear)) }
        Spacer(Modifier.weight(1f))
        TankobunActionButton(
            label = if (selectedCount == null || selectedCount == 0) tankobunString(R.string.common_apply)
                else tankobunString(R.string.filters_apply_count, selectedCount),
            onClick = onApply,
            enabled = enabled,
        )
    }
}

internal fun CatalogTag.filterCategory(): String = when {
    mangaBakaId != null && parentId == null -> name
    else -> category.orEmpty().substringBefore(" > ").substringBefore('-').ifBlank { "Other" }
}

internal fun String.filterSearchKey(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()

@Composable
internal fun filterCategoryLabel(category: String): String {
    val res = when (category) {
        "Activities" -> R.string.filters_category_activities
        "Audience Demographics", "Demographic" -> R.string.filters_category_audience
        "Character Archetype" -> R.string.filters_category_archetypes
        "Character Traits" -> R.string.filters_category_traits
        "Character Types", "Cast" -> R.string.filters_category_characters
        "Derivative Work" -> R.string.filters_category_derivative
        "Locations", "Setting" -> R.string.filters_category_locations
        "Narrative Tropes" -> R.string.filters_category_narrative
        "Objects" -> R.string.filters_category_objects
        "Occupations" -> R.string.filters_category_occupations
        "Relationship" -> R.string.filters_category_relationships
        "Settings" -> R.string.filters_category_settings
        "Sexual Content", "Sexual Content & Nudity" -> R.string.filters_category_sexual
        "Species & Creatures" -> R.string.filters_category_creatures
        "Themes", "Theme" -> R.string.filters_category_themes
        "Time Period" -> R.string.filters_category_period
        "Tone" -> R.string.filters_category_tone
        "Work Info", "Technical" -> R.string.filters_category_work
        "World Building" -> R.string.filters_category_world
        "Genres" -> R.string.common_genres
        else -> null
    }
    return res?.let { tankobunString(it) } ?: if (category == "Other") tankobunString(R.string.filters_category_other) else category
}

private data class FilterTagLabel(val tag: CatalogTag, val label: String, val group: String, val searchText: String)

/** Categories are collapsed initially; searching covers the complete tree, including closed groups. */
@Composable
internal fun CatalogFilterDialog(
    title: String,
    taxonomy: CatalogTaxonomy,
    options: List<CatalogTag>,
    selected: Set<String>,
    includeAdult: Boolean,
    genresOnly: Boolean = false,
    loading: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(selected.map { taxonomy.resolve(it)?.key ?: it }.toSet()) }
    var selectedOnly by remember { mutableStateOf(false) }
    var expandedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Resolve selected legacy names when the on-disk taxonomy finishes loading.
    LaunchedEffect(taxonomy, includeAdult) {
        draft = draft.map { taxonomy.resolve(it)?.key ?: it }
            .filter { includeAdult || taxonomy.resolve(it)?.isAdult != true }.toSet()
    }
    val allowed = remember(options, includeAdult, genresOnly) {
        options.filter { (includeAdult || !it.isAdult) && (!genresOnly || it.isGenre) }.distinctBy { it.key }
    }
    val categoryNames = remember(allowed) { allowed.map { it.filterCategory() }.distinct() }
    val genreNames = remember(allowed) { allowed.filter { it.isGenre }.map { it.name }.distinct() }
    val categoryLabels = categoryNames.associateWith { filterCategoryLabel(it) }
    val genreLabels = genreNames.associateWith { browseGenreLabel(it) }
    // Localized labels sort by the app language's rules, so "Ação" comes before "Aventura".
    val locale = tankobunLocale()
    val collator = remember(locale) { Collator.getInstance(locale).apply { strength = Collator.SECONDARY } }
    val labels = remember(allowed, categoryLabels, genreLabels, collator) { allowed.map { tag ->
        val label = genreLabels[tag.name] ?: tag.name
        val group = categoryLabels.getValue(tag.filterCategory())
        FilterTagLabel(tag, label, group, "$label ${tag.name} $group ${tag.category.orEmpty()}".filterSearchKey())
    }.sortedWith(compareBy(collator) { it.label })
    }
    val search = query.filterSearchKey()
    val selectedFilter = if (selectedOnly) draft else emptySet()
    val visible = remember(labels, search, selectedOnly, selectedFilter) {
        labels.filter { item ->
            (!selectedOnly || item.tag.key in draft) && (search.isBlank() || search in item.searchText)
        }
    }
    val groups = remember(visible, collator) { visible.groupBy { it.group }.toSortedMap(collator) }
    fun toggle(key: String) { draft = if (key in draft) draft - key else draft + key }

    val count = if (draft.isNotEmpty()) {
        tankobunQuantityString(R.plurals.filters_selected_plural, draft.size, draft.size)
    } else {
        tankobunQuantityString(R.plurals.filters_options_plural, visible.size, visible.size)
    }
    FilterDialogFrame(title, onDismiss, expanded = true, count = count, footer = {
        FilterDialogActions(draft.size, onClear = { draft = emptySet(); selectedOnly = false }, onApply = {
            onApply(draft)
            onDismiss()
        })
    }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                TankobunSearchField(
                    query,
                    { query = it },
                    placeholder = tankobunString(if (genresOnly) R.string.filters_search_genres else R.string.filters_search_tags),
                    showSearchAction = false,
                    filled = true,
                )
            }
            if (onRefresh != null) {
                IconButton(onClick = onRefresh, enabled = !loading) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(TankobunIcons.Refresh, tankobunString(R.string.filters_refresh))
                }
            }
        }
        // Tags keep a fixed-height row for their selection, so the first pick never pushes the list
        // away from the finger. Genres show every choice in the grid and need no such row.
        if (!genresOnly) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (draft.isNotEmpty()) {
                    val labelByKey = remember(labels) { labels.associate { it.tag.key to it.label } }
                    LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(draft.toList(), key = { it }) { key ->
                            InputChip(
                                selected = true,
                                onClick = { toggle(key) },
                                label = { Text(labelByKey[key] ?: key, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                trailingIcon = {
                                    Icon(TankobunIcons.Close, contentDescription = tankobunString(R.string.common_remove), modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.widthIn(max = 220.dp),
                            )
                        }
                    }
                    FilterChip(selected = selectedOnly, onClick = { selectedOnly = !selectedOnly },
                        label = { Text(tankobunString(R.string.filters_selected_count, draft.size)) })
                } else {
                    Text(tankobunString(R.string.filters_tags_hint), Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (visible.isEmpty()) {
                item { Text(tankobunString(if (loading) R.string.filters_loading else R.string.filters_no_matches),
                    Modifier.padding(vertical = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (genresOnly) {
                item(key = "genre-grid") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        visible.forEach { item ->
                            val selectedGenre = item.tag.key in draft
                            FilterChip(
                                selected = selectedGenre,
                                onClick = { toggle(item.tag.key) },
                                label = { Text(item.label, maxLines = 1) },
                                // A picked genre trades its icon for a check, as in the design.
                                leadingIcon = {
                                    Icon(
                                        if (selectedGenre) TankobunIcons.Check else genreIcon(item.tag.name),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            } else if (search.isNotBlank() || selectedOnly) {
                items(visible, key = { it.tag.key }) { item -> FilterTagRow(item, item.tag.key in draft, genresOnly) { toggle(item.tag.key) } }
            } else {
                for ((group, children) in groups) {
                    item(key = "group:$group") {
                        val expanded = group in expandedGroups
                        // A step darker than the sheet, so categories read as rows to open.
                        Surface(shape = LocalTankobunStyle.current.themeShapes.control,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth().clickable {
                                expandedGroups = if (expanded) expandedGroups - group else expandedGroups + group
                            }) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(if (expanded) TankobunIcons.ExpandMore else TankobunIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text(group, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                val count = children.count { it.tag.key in draft }
                                Text(if (count == 0) children.size.toString() else "$count / ${children.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (count == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                    fontWeight = if (count == 0) FontWeight.Normal else FontWeight.Bold)
                            }
                        }
                    }
                    if (group in expandedGroups) {
                        // Inside a category the group is already named: no subtitle, and an indent under the arrow.
                        items(children, key = { it.tag.key }) { item ->
                            FilterTagRow(item, item.tag.key in draft, compact = true, indent = 30.dp) { toggle(item.tag.key) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTagRow(item: FilterTagLabel, selected: Boolean, compact: Boolean, indent: Dp = 0.dp, onClick: () -> Unit) {
    Surface(shape = LocalTankobunStyle.current.themeShapes.control,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        modifier = Modifier.fillMaxWidth().toggleable(selected, role = Role.Checkbox, onValueChange = { onClick() })) {
        Row(Modifier.padding(start = 12.dp + indent, end = 4.dp).heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.tag.isGenre) {
                Icon(genreIcon(item.tag.name), null, Modifier.size(22.dp),
                    tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!compact && !item.tag.category.isNullOrBlank()) Text(item.tag.category.orEmpty(),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.padding(10.dp))
        }
    }
}

@Composable
internal fun FilterOptionsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
internal fun MediaFilterOptionsDialog(
    title: String,
    sortOptions: List<BrowseOption>,
    selectedSort: String,
    onSortChange: (String?) -> Unit,
    sortIcon: (BrowseOption) -> androidx.compose.ui.graphics.vector.ImageVector,
    viewMode: com.tankobun.app.MediaViewMode,
    onViewMode: (com.tankobun.app.MediaViewMode) -> Unit,
    coverColumns: Int,
    onCoverColumns: (Int) -> Unit,
    showWholeCovers: Boolean,
    onWholeCovers: (Boolean) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scroll = rememberScrollState()
    FilterDialogFrame(title, onDismiss, fitContent = true, footer = {
        FilterDialogActions(onClear = onReset, onApply = onApply)
    }) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FilterOptionsSection(tankobunString(R.string.browse_sort)) {
                BoxWithConstraints {
                    val columns = if (maxWidth >= 500.dp) 2 else 1
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        sortOptions.chunked(columns).forEach { options ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                options.forEach { option ->
                                    Surface(shape = LocalTankobunStyle.current.themeShapes.control,
                                        color = if (selectedSort == option.value) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                        modifier = Modifier.weight(1f).clickable { onSortChange(option.value) }) {
                                        Row(Modifier.padding(horizontal = 12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Icon(sortIcon(option), null, Modifier.size(20.dp))
                                            Text(option.labelText(), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                            RadioButton(selected = selectedSort == option.value, onClick = null)
                                        }
                                    }
                                }
                                if (options.size < columns) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            FilterOptionsSection(tankobunString(R.string.common_view)) { MediaViewModeRow(viewMode, onViewMode) }
            FilterOptionsSection(tankobunString(R.string.settings_covers_per_row)) { CoverColumnsRow(coverColumns, onCoverColumns) }
            FilterOptionsSection(tankobunString(R.string.settings_cover_framing)) { CoverFramingRow(showWholeCovers, onWholeCovers) }
        }
    }
}
