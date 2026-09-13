package com.tankobun.app.ui.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tankobun.app.R
import com.tankobun.core.model.ChapterGroupPreference
import com.tankobun.app.logic.ChapterTranslationGroup
import com.tankobun.app.logic.chapterGroupKey
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.tankobunString
import com.tankobun.app.ui.components.TankobunDialog
import com.tankobun.app.ui.components.TankobunDialogHeader
import com.tankobun.app.ui.icons.TankobunIcons

@Composable
internal fun ChapterGroupsButton(state: TankobunUiState, onChange: (ChapterGroupPreference) -> Unit) {
    val groups = state.chapterGroupSelection.groups
    val preference = state.chapterGroupPreference
    if (groups.size < 2 && !preference.oneVersionPerChapter && preference.preferredGroup == null) return
    var open by remember(state.chapterGroupPreferenceKey) { mutableStateOf(false) }
    TextButton(onClick = { open = true }, modifier = Modifier.widthIn(max = 200.dp)) {
        Icon(TankobunIcons.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(
            tankobunString(if (preference.oneVersionPerChapter) R.string.chapter_groups_filtered else R.string.chapter_groups_title),
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (open) ChapterGroupsDialog(groups, preference, onChange, onDismiss = { open = false })
}

@Composable
private fun ChapterGroupsDialog(
    groups: List<ChapterTranslationGroup>,
    preference: ChapterGroupPreference,
    onChange: (ChapterGroupPreference) -> Unit,
    onDismiss: () -> Unit,
) {
    TankobunDialog(onDismiss = onDismiss) {
        TankobunDialogHeader(tankobunString(R.string.chapter_groups_title), onDismiss)
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                onChange(preference.copy(oneVersionPerChapter = !preference.oneVersionPerChapter))
            }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(tankobunString(R.string.chapter_groups_one_version), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Switch(preference.oneVersionPerChapter, onCheckedChange = { onChange(preference.copy(oneVersionPerChapter = it)) })
        }
        Text(
            tankobunString(R.string.chapter_groups_fallback),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(tankobunString(R.string.chapter_groups_preferred), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.labelLarge)
        Column(Modifier.selectableGroup()) {
            ChapterGroupChoice(
                tankobunString(R.string.chapter_groups_automatic),
                selected = preference.preferredGroup == null,
                onClick = { onChange(ChapterGroupPreference(oneVersionPerChapter = true)) },
            )
            val selectedKey = preference.preferredGroup?.let(::chapterGroupKey)
            val missing = preference.preferredGroup?.takeIf { groups.none { group -> group.key == selectedKey } }
            val choices = groups + listOfNotNull(missing?.let { ChapterTranslationGroup(chapterGroupKey(it), it) })
            choices.forEach { group ->
                ChapterGroupChoice(group.label, selected = selectedKey == group.key, missing = group.label == missing) {
                    onChange(ChapterGroupPreference(oneVersionPerChapter = true, preferredGroup = group.label))
                }
            }
        }
    }
}

@Composable
private fun ChapterGroupChoice(label: String, selected: Boolean, missing: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (missing) Text(tankobunString(R.string.chapter_groups_missing), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
