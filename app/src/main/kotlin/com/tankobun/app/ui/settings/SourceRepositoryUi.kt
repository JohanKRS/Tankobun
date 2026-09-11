package com.tankobun.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    val canAdd = repositoryUrl.isNotBlank() && !loading
    val addRepository = {
        if (canAdd) {
            focus.clearFocus()
            onAddRepository()
        }
    }
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { addRepository() }),
            trailingIcon = {
                FilledIconButton(
                    onClick = addRepository,
                    enabled = canAdd,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    shape = LocalTankobunStyle.current.themeShapes.control,
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(
                        TankobunIcons.Add,
                        contentDescription = tankobunString(if (hasRepositories) R.string.sources_add_another_repository else R.string.common_add),
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )
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
internal fun SourceRepositoryRow(number: Int, url: String, enabled: Boolean, visible: Boolean, name: String? = null,
    onVisibilityChange: (Boolean) -> Unit, onRename: () -> Unit, onRemove: () -> Unit) {
    val displayName = name ?: tankobunString(R.string.sources_repository_number, number)
    TankobunPanel(Modifier.fillMaxWidth(), color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(TankobunIcons.Link, contentDescription = null, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).clickable(enabled = enabled,
                onClickLabel = tankobunString(R.string.sources_rename_repository), onClick = onRename),
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(displayName, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(TankobunIcons.Pencil, contentDescription = null, modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            IconToggleButton(checked = visible, onCheckedChange = onVisibilityChange, enabled = enabled,
                modifier = Modifier.size(LocalTankobunStyle.current.sizes.iconAction)) {
                Icon(if (visible) TankobunIcons.Visibility else TankobunIcons.VisibilityOff,
                    contentDescription = tankobunString(if (visible) R.string.sources_hide_repository else R.string.sources_show_repository, displayName),
                    modifier = Modifier.size(21.dp),
                    tint = if (visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SourceSettingsIconActionButton(TankobunIcons.Delete,
                contentDescription = tankobunString(R.string.sources_remove_repository, number),
                enabled = enabled, onClick = onRemove)
        }
    }
}

@Composable
internal fun SourceRepositoryNameDialog(currentName: String, automaticName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tankobunString(R.string.sources_rename_repository)) },
        text = {
            OutlinedTextField(name, { name = it.take(80) },
                label = { Text(tankobunString(R.string.sources_repository_name)) },
                placeholder = { Text(automaticName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingText = { Text(tankobunString(R.string.sources_repository_name_automatic)) },
                singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(focus),
                shape = LocalTankobunStyle.current.themeShapes.control,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave(name) }),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text(tankobunString(R.string.common_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tankobunString(R.string.common_cancel)) } },
    )
}
