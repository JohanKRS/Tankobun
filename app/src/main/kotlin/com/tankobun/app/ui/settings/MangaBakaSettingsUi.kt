package com.tankobun.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tankobun.app.MainViewModel
import com.tankobun.app.R
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.tankobunString

@Composable
internal fun MangaBakaSettings(state: TankobunUiState, viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf("") }
    val uri = LocalUriHandler.current
    SettingsGroupDivider("MangaBaka")
    Text(tankobunString(R.string.catalog_description), style = MaterialTheme.typography.bodySmall)
    TextButton(onClick = { expanded = !expanded }) {
        Text(state.mangaBakaAccountName ?: tankobunString(R.string.catalog_tracking_optional))
    }
    if (expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tankobunString(R.string.catalog_tracking_description), style = MaterialTheme.typography.bodySmall)
            if (state.mangaBakaAccountName == null) {
                OutlinedTextField(value = token, onValueChange = { token = it }, singleLine = true,
                    label = { Text(tankobunString(R.string.catalog_token)) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { uri.openUri("https://mangabaka.org/my/settings/api-and-apps") }) { Text(tankobunString(R.string.catalog_create_token)) }
                    Button(onClick = { viewModel.connectMangaBaka(token); token = "" }, enabled = !state.mangaBakaBusy && token.isNotBlank()) { Text(tankobunString(R.string.catalog_connect)) }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::syncMangaBaka, enabled = !state.mangaBakaBusy) { Text(tankobunString(R.string.catalog_sync)) }
                    TextButton(onClick = viewModel::disconnectMangaBaka, enabled = !state.mangaBakaBusy) { Text(tankobunString(R.string.common_sign_out)) }
                }
            }
            if (state.mangaBakaBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}
