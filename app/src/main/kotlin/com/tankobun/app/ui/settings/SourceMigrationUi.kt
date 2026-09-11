package com.tankobun.app.ui.settings

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.tankobun.app.R
import com.tankobun.app.tankobunString
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.core.model.SourceDescriptor

@Composable
internal fun SourceMigrationButton(source: SourceDescriptor, enabled: Boolean, onMigrate: () -> Unit) {
    val context = LocalContext.current
    var showMigration by rememberSaveable(source.packageName) { mutableStateOf(false) }
    SourceSettingsIconActionButton(
        icon = if (source.isPrivateExtension) TankobunIcons.StayCurrentPortrait else TankobunIcons.SwapHoriz,
        contentDescription = tankobunString(if (source.isPrivateExtension) R.string.sources_remove_android_copy else R.string.sources_migrate_cd,
            source.name.extensionDisplayName()),
        enabled = enabled,
        onClick = {
            if (source.isPrivateExtension) requestExtensionUninstall(context, source.packageName)
            else showMigration = true
        },
    )
    if (showMigration) AlertDialog(
        onDismissRequest = { showMigration = false },
        title = { Text(tankobunString(R.string.sources_migrate_title)) },
        text = { Text(tankobunString(R.string.sources_migrate_description, source.name.extensionDisplayName())) },
        confirmButton = { TextButton(onClick = { showMigration = false; onMigrate() }, enabled = enabled) {
            Text(tankobunString(R.string.sources_migrate_action))
        } },
        dismissButton = { TextButton(onClick = { showMigration = false }) { Text(tankobunString(R.string.common_cancel)) } },
    )
}
