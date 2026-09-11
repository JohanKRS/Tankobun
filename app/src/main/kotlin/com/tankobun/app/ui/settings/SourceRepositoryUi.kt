package com.tankobun.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.R
import com.tankobun.app.tankobunString
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.app.ui.components.TankobunPanel

@Composable
internal fun SourceRepositoryControls(
    repositoryUrl: String,
    hasRepositories: Boolean,
    loading: Boolean,
    onRepositoryUrlChange: (String) -> Unit,
    onAddRepository: () -> Unit,
) {
    val focus = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tankobunString(R.string.sources_repository),
            style = LocalTankobunStyle.current.typography.sectionLabel,
            color = LocalTankobunStyle.current.colors.accent)
        OutlinedTextField(
            value = repositoryUrl,
            onValueChange = onRepositoryUrlChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(tankobunString(R.string.sources_repository_index_url)) },
            shape = LocalTankobunStyle.current.themeShapes.control,
        )
        Button(
            onClick = { focus.clearFocus(); onAddRepository() },
            enabled = repositoryUrl.isNotBlank() && !loading,
            modifier = Modifier.align(Alignment.End),
            shape = LocalTankobunStyle.current.themeShapes.control,
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
            else Icon(TankobunIcons.Add, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(tankobunString(if (hasRepositories) R.string.sources_add_another_repository else R.string.common_add))
        }
    }
}

@Composable
internal fun SourceRepositoryListHeading(count: Int, loading: Boolean, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tankobunString(R.string.sources_added_repositories, count), Modifier.weight(1f),
            style = LocalTankobunStyle.current.typography.sectionLabel,
            color = LocalTankobunStyle.current.colors.accent)
        SourceSettingsIconActionButton(TankobunIcons.Refresh,
            contentDescription = tankobunString(R.string.sources_refresh_repositories),
            enabled = !loading, onClick = onRefresh)
    }
}

@Composable
internal fun SourceRepositoryRow(number: Int, url: String, enabled: Boolean, onRemove: () -> Unit) {
    TankobunPanel(Modifier.fillMaxWidth(), color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(TankobunIcons.Link, contentDescription = null, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tankobunString(R.string.sources_repository_number, number), style = MaterialTheme.typography.bodyLarge)
                Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            SourceSettingsIconActionButton(TankobunIcons.Delete,
                contentDescription = tankobunString(R.string.sources_remove_repository, number),
                enabled = enabled, onClick = onRemove)
        }
    }
}
