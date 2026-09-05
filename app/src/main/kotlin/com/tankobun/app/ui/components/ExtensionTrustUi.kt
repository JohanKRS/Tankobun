package com.tankobun.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.tankobunString
import com.tankobun.app.R
import com.tankobun.app.ui.settings.extensionDisplayName
import com.tankobun.core.extensions.UntrustedExtension

/** Shared by the extension manager and a manga whose saved source needs approval. */
@Composable
internal fun ExtensionTrustDialog(
    candidate: UntrustedExtension,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        shape = LocalTankobunStyle.current.themeShapes.dialog,
        containerColor = LocalTankobunStyle.current.colors.panel,
        titleContentColor = LocalTankobunStyle.current.colors.panelContent,
        textContentColor = LocalTankobunStyle.current.colors.mutedContent,
        tonalElevation = 0.dp,
        onDismissRequest = onDismiss,
        title = { Text(tankobunString(R.string.sources_trust_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tankobunString(R.string.sources_trust_explanation, candidate.descriptor.name.extensionDisplayName()))
                Text(candidate.descriptor.packageName, style = MaterialTheme.typography.bodySmall)
                Text("SHA-256\n" + candidate.signerFingerprints.sorted().joinToString("\n"), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = candidate.signerFingerprints.isNotEmpty()) {
                Text(tankobunString(R.string.sources_trust_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tankobunString(R.string.common_cancel)) }
        },
    )
}
