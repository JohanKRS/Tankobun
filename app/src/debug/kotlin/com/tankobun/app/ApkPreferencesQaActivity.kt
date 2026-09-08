package com.tankobun.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tankobun.app.ui.settings.SourceSettingsRow

/** Uses only a separately installed original fixture APK. Excluded from release. */
class ApkPreferencesQaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".novelqa"))
        val container = (application as TankobunApplication).container
        val descriptor = container.extensionScanner.installedExtensions().first { it.packageName == "eu.kanade.tachiyomi.extension.en.preferencesqa" }
        val source = container.sourceHost.loadSources(descriptor.packageName).first { it.id == 987654401L }
        setContent {
            TankobunTheme(SettingsStore(this).themePreference()) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().padding(top = 20.dp)) {
                        SourceSettingsRow(descriptor.copy(id = source.id, name = source.name, lang = source.lang), true, null, null, false, {}, {}, {})
                    }
                }
            }
        }
    }
}
