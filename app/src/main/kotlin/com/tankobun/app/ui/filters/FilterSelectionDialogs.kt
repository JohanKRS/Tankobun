package com.tankobun.app.ui.filters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.R
import com.tankobun.app.tankobunString
import com.tankobun.app.ui.browse.BrowseOption
import com.tankobun.app.ui.browse.labelFor
import com.tankobun.app.ui.browse.labelText
import com.tankobun.core.model.PublicationYears
import com.tankobun.app.ui.icons.TankobunIcons
import kotlinx.coroutines.launch
import java.time.Year
import kotlin.math.abs

@Composable
internal fun FilterChoiceDialog(title: String, options: List<BrowseOption>, selected: Set<String>, onApply: (Set<String>) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(selected) }
    FilterDialogFrame(title, onDismiss, footer = {
        FilterDialogActions(draft.size, onClear = { draft = emptySet() }, onApply = { onApply(draft); onDismiss() })
    }) {
        LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(options, key = { it.value ?: "any" }) { option ->
                val checked = if (option.value == null) draft.isEmpty() else option.value in draft
                Surface(shape = LocalTankobunStyle.current.themeShapes.control,
                    color = if (checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                    modifier = Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox, onValueChange = {
                        draft = option.value?.let { if (it in draft) draft - it else draft + it } ?: emptySet()
                    })) {
                    Row(Modifier.padding(start = 12.dp, end = 4.dp).heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(filterChoiceIcon(option.value), null, Modifier.size(22.dp),
                            tint = if (checked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text(option.labelText(), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
    }
}

private fun filterChoiceIcon(value: String?) = when (value) {
    "MANGA" -> TankobunIcons.MenuBook
    "NOVEL" -> TankobunIcons.Pencil
    "ONE_SHOT" -> TankobunIcons.Photo
    "RELEASING" -> TankobunIcons.PlayArrow
    "FINISHED" -> TankobunIcons.Check
    "NOT_YET_RELEASED" -> TankobunIcons.CalendarMonth
    "CANCELLED" -> TankobunIcons.Close
    "HIATUS" -> TankobunIcons.Pause
    "JP", "KR", "CN", "TW" -> TankobunIcons.Flag
    else -> TankobunIcons.Category
}

private const val FIRST_PUBLICATION_YEAR = 1679
private const val LAST_PUBLICATION_YEAR = 2262

/** Read the centered row directly, so Apply never relies on a delayed scroll callback. */
private fun LazyListState.centeredYear(): Int {
    val layout = layoutInfo
    val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
    val index = layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
        ?: firstVisibleItemIndex
    return (FIRST_PUBLICATION_YEAR + index).coerceIn(FIRST_PUBLICATION_YEAR, LAST_PUBLICATION_YEAR)
}

@Composable
internal fun FilterYearDialog(selected: PublicationYears?, onApply: (PublicationYears?) -> Unit, onDismiss: () -> Unit) {
    val currentYear = remember { Year.now().value.coerceIn(FIRST_PUBLICATION_YEAR, LAST_PUBLICATION_YEAR) }
    val fromState = rememberLazyListState((selected?.from ?: currentYear) - FIRST_PUBLICATION_YEAR)
    val toState = rememberLazyListState((selected?.to ?: currentYear) - FIRST_PUBLICATION_YEAR)
    var fromUnlimited by remember { mutableStateOf(selected?.from == null) }
    var toUnlimited by remember { mutableStateOf(selected?.to == null) }
    val lower by remember { derivedStateOf { if (fromUnlimited) null else fromState.centeredYear() } }
    val upper by remember { derivedStateOf { if (toUnlimited) null else toState.centeredYear() } }
    val valid = lower == null || upper == null || lower!! <= upper!!
    val scrolling = fromState.isScrollInProgress || toState.isScrollInProgress
    FilterDialogFrame(tankobunString(R.string.filters_year_range), onDismiss, footer = {
        FilterDialogActions(enabled = valid && !scrolling, onClear = { fromUnlimited = true; toUnlimited = true }, onApply = {
            onApply(if (lower == null && upper == null) null else PublicationYears(lower, upper))
            onDismiss()
        })
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterYearWheel(tankobunString(R.string.filters_year_from), fromState, fromUnlimited,
                { fromUnlimited = it }, Modifier.weight(1f))
            FilterYearWheel(tankobunString(R.string.filters_year_to), toState, toUnlimited,
                { toUnlimited = it }, Modifier.weight(1f))
        }
        Text(tankobunString(if (valid) R.string.filters_year_hint else R.string.filters_year_invalid), minLines = 2,
            style = MaterialTheme.typography.bodySmall,
            color = if (valid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun FilterYearWheel(label: String, state: LazyListState, unlimited: Boolean, onUnlimited: (Boolean) -> Unit, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val dragging by state.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragging) { if (dragging) onUnlimited(false) }
    val centered by remember(state) { derivedStateOf { state.centeredYear() } }
    val rowHeight = 44.dp
    val noLimitLabel = tankobunString(R.string.filters_year_unlimited)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(TankobunIcons.CalendarMonth, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
        Box(Modifier.fillMaxWidth().height(rowHeight * 5).clip(LocalTankobunStyle.current.themeShapes.control)) {
            Surface(Modifier.align(Alignment.Center).fillMaxWidth().height(rowHeight),
                shape = LocalTankobunStyle.current.themeShapes.control,
                color = if (unlimited) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.secondaryContainer) {}
            LazyColumn(state = state, flingBehavior = rememberSnapFlingBehavior(state),
                contentPadding = PaddingValues(vertical = rowHeight * 2),
                modifier = Modifier.fillMaxSize().semantics { contentDescription = label; stateDescription = if (unlimited) noLimitLabel else centered.toString() }) {
                items(LAST_PUBLICATION_YEAR - FIRST_PUBLICATION_YEAR + 1, key = { it }) { index ->
                    val year = FIRST_PUBLICATION_YEAR + index
                    val focused = year == centered
                    Box(Modifier.fillMaxWidth().height(rowHeight).semantics { selected = focused && !unlimited }.alpha(if (unlimited) 0.38f else if (focused) 1f else 0.55f)
                        .clickable {
                            onUnlimited(false)
                            scope.launch { state.animateScrollToItem(index) }
                        }, contentAlignment = Alignment.Center) {
                        Text(year.toString(), style = if (focused) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (focused && !unlimited) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().toggleable(unlimited, role = Role.Checkbox, onValueChange = onUnlimited)
            .heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Checkbox(unlimited, onCheckedChange = null)
            Spacer(Modifier.width(4.dp))
            Text(noLimitLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun List<BrowseOption>.selectionLabel(selected: Set<String>): String =
    if (selected.size <= 1) labelFor(selected.singleOrNull()) else selected.size.toString()

@Composable
internal fun PublicationYears?.filterLabel(): String = when {
    this == null -> tankobunString(R.string.common_any)
    from == to -> from.toString()
    from == null -> tankobunString(R.string.filters_year_until, to ?: 0)
    to == null -> tankobunString(R.string.filters_year_since, from ?: 0)
    else -> "$from–$to"
}
