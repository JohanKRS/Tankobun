package com.tankobun.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tankobun.app.MainViewModel
import com.tankobun.app.R
import com.tankobun.app.logic.ExtensionUpdateProgress
import com.tankobun.app.tankobunString
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.core.extensions.ExtensionIndexEntry

@Composable
internal fun ExtensionUpdatesControl(pendingCount: Int, progress: ExtensionUpdateProgress?, enabled: Boolean,
    onUpdateAll: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier) {
    if (progress == null && pendingCount == 0) return
    Column(modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (progress == null) {
            FilledTonalButton(onClick = onUpdateAll, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Icon(TankobunIcons.Download, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(tankobunString(R.string.extensions_update_all, pendingCount))
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (progress.stopping) tankobunString(R.string.extensions_updates_finishing_current)
                    else tankobunString(R.string.extensions_updates_progress, progress.completed + 1, progress.total, progress.currentName.extensionDisplayName()),
                    Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onStop, enabled = !progress.stopping) { Text(tankobunString(R.string.extensions_updates_stop)) }
            }
            LinearProgressIndicator(progress = { progress.completed.toFloat() / progress.total.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

internal fun requestAllExtensionUpdates(context: Context, viewModel: MainViewModel, pending: List<ExtensionIndexEntry>) {
    if (pending.any { viewModel.requiresAndroidInstaller(it.packageName) } && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
        viewModel.requireExtensionInstallPermission()
        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
        return
    }
    viewModel.updateAllExtensions()
}
