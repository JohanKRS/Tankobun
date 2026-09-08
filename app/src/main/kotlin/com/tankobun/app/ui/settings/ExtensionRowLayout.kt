package com.tankobun.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tankobun.app.R
import com.tankobun.app.tankobunString
import com.tankobun.app.LocalTankobunStyle

/** Reserve the full row for the source name on phones; use one line on wide layouts. */
@Composable
internal fun ExtensionRowLayout(
    modifier: Modifier = Modifier,
    identity: @Composable (Modifier) -> Unit,
    actions: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth < 600.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                identity(Modifier.fillMaxWidth())
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    actions()
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                identity(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    actions()
                }
            }
        }
    }
}

@Composable
internal fun ExtensionRowIdentity(
    packageName: String?, name: String, language: String, iconUrl: String?, metadata: String,
    modifier: Modifier = Modifier, reviewRequired: Boolean = false,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ExtensionIcon(packageName, name, iconUrl, Modifier.size(42.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val flag = sourceLanguageFlag(language)
                val label = sourceLanguageLocalizedLabel(language)
                Text(flag ?: label, Modifier.semantics { contentDescription = label }, style = MaterialTheme.typography.bodyMedium)
                Text(name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            }
            Text(metadata, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (reviewRequired) {
                Text(tankobunString(R.string.sources_trust_status), style = MaterialTheme.typography.bodySmall,
                    color = LocalTankobunStyle.current.colors.accent)
            }
        }
    }
}
