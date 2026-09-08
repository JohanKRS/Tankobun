package com.tankobun.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tankobun.app.R
import com.tankobun.app.tankobunString
import com.tankobun.core.model.CatalogMode

@Composable
internal fun CatalogNavigationSettings(mode: CatalogMode, onSelect: (CatalogMode) -> Unit) {
    Text(tankobunString(R.string.catalog_navigation), style = MaterialTheme.typography.titleMedium)
    Text(tankobunString(R.string.catalog_navigation_description), style = MaterialTheme.typography.bodySmall)
    Column(Modifier.fillMaxWidth().selectableGroup()) {
        CatalogMode.entries.forEach { option ->
            val title = when (option) {
                CatalogMode.ANILIST -> "AniList"
                CatalogMode.MANGABAKA -> "MangaBaka"
                CatalogMode.COMBINED -> tankobunString(R.string.catalog_combined)
            }
            val description = when (option) {
                CatalogMode.ANILIST -> R.string.catalog_priority_anilist
                CatalogMode.MANGABAKA -> R.string.catalog_priority_mangabaka
                CatalogMode.COMBINED -> R.string.catalog_priority_combined
            }
            Row(
                Modifier.fillMaxWidth().selectable(selected = mode == option, role = Role.RadioButton,
                    onClick = { onSelect(option) }).padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(selected = mode == option, onClick = null)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(tankobunString(description), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
