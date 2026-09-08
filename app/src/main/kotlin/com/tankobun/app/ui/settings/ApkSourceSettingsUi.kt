package com.tankobun.app.ui.settings

import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tankobun.app.R
import com.tankobun.app.SourcePreferencesActivity
import com.tankobun.app.TankobunApplication
import com.tankobun.app.tankobunString
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.core.model.SourceDescriptor
import eu.kanade.tachiyomi.source.ConfigurableSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ApkSourceSettingsButton(source: SourceDescriptor, displayName: String) {
    val context = LocalContext.current
    val host = (context.applicationContext as TankobunApplication).container.sourceHost
    var configurable by remember(source) { mutableStateOf(false) }
    LaunchedEffect(source) {
        configurable = withContext(Dispatchers.IO) {
            host.loadSources(source.packageName).any { it.id == source.id && it is ConfigurableSource }
        }
    }
    if (configurable) SourceSettingsIconActionButton(TankobunIcons.Settings,
        tankobunString(R.string.novel_source_settings, displayName), {
            context.startActivity(Intent(context, SourcePreferencesActivity::class.java)
                .putExtra(SourcePreferencesActivity.PACKAGE, source.packageName)
                .putExtra(SourcePreferencesActivity.SOURCE_ID, source.id)
                .putExtra(SourcePreferencesActivity.SOURCE_NAME, displayName))
        })
}
