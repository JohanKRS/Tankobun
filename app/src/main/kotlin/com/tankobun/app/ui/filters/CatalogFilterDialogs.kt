package com.tankobun.app.ui.filters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.tankobun.app.ui.browse.*
import com.tankobun.app.ui.components.*
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.app.ui.icons.genreIcon
import com.tankobun.core.model.CatalogTag
import com.tankobun.core.model.CatalogTaxonomy
import java.text.Normalizer
import java.util.Locale

@Composable
internal fun FilterDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    expanded: Boolean = false,
    fitContent: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
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
    val labels = remember(allowed, categoryLabels, genreLabels) { allowed.map { tag ->
        val label = genreLabels[tag.name] ?: tag.name
        val group = categoryLabels.getValue(tag.filterCategory())
        FilterTagLabel(tag, label, group, "$label ${tag.name} $group ${tag.category.orEmpty()}".filterSearchKey())
    }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
    val search = query.filterSearchKey()
    val selectedFilter = if (selectedOnly) draft else emptySet()
    val visible = remember(labels, search, selectedOnly, selectedFilter) {
        labels.filter { item ->
            (!selectedOnly || item.tag.key in draft) && (search.isBlank() || search in item.searchText)
        }
    }
    val groups = remember(visible) { visible.groupBy { it.group }.toSortedMap(String.CASE_INSENSITIVE_ORDER) }
    fun toggle(key: String) { draft = if (key in draft) draft - key else draft + key }

    FilterDialogFrame(title, onDismiss, expanded = true, footer = {
        FilterDialogActions(draft.size, onClear = { draft = emptySet(); selectedOnly = false }, onApply = {
            onApply(draft)
            onDismiss()
        })
    }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                TankobunSearchField(query, { query = it }, placeholder = tankobunString(R.string.filters_find_option), showSearchAction = false)
            }
            if (onRefresh != null) {
                IconButton(onClick = onRefresh, enabled = !loading) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(TankobunIcons.Refresh, tankobunString(R.string.filters_refresh))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tankobunString(R.string.filters_option_count, visible.size), Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (draft.isNotEmpty()) {
                FilterChip(selected = selectedOnly, onClick = { selectedOnly = !selectedOnly },
                    label = { Text(tankobunString(R.string.filters_selected_count, draft.size)) })
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (visible.isEmpty()) {
                item { Text(tankobunString(if (loading) R.string.filters_loading else R.string.filters_no_matches),
                    Modifier.padding(vertical = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (genresOnly || search.isNotBlank() || selectedOnly) {
                items(visible, key = { it.tag.key }) { item -> FilterTagRow(item, item.tag.key in draft, genresOnly) { toggle(item.tag.key) } }
            } else {
                for ((group, children) in groups) {
                    item(key = "group:$group") {
                        val expanded = group in expandedGroups
                        Surface(shape = LocalTankobunStyle.current.themeShapes.control,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth().clickable {
                                expandedGroups = if (expanded) expandedGroups - group else expandedGroups + group
                            }) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(if (expanded) TankobunIcons.ExpandMore else TankobunIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text(group, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                val count = children.count { it.tag.key in draft }
                                Text(if (count == 0) children.size.toString() else "$count / ${children.size}",
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (group in expandedGroups) {
                        items(children, key = { it.tag.key }) { item -> FilterTagRow(item, item.tag.key in draft, false) { toggle(item.tag.key) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTagRow(item: FilterTagLabel, selected: Boolean, compact: Boolean, onClick: () -> Unit) {
    Surface(shape = LocalTankobunStyle.current.themeShapes.control,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        modifier = Modifier.fillMaxWidth().toggleable(selected, role = Role.Checkbox, onValueChange = { onClick() })) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp).heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
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
