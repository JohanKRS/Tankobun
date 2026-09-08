package com.tankobun.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.ui.settings.SourcesSettingsScreen
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.extensions.novel.LnReaderPlugin
import com.tankobun.core.extensions.novel.LnReaderPluginStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.File

/** Content-free APKs and original JS; real updater/installer with in-memory HTTP responses. Debug only. */
class ExtensionUpdatesQaActivity : ComponentActivity() {
    private var qaViewModel: MainViewModel? = null
    override fun onResume() {
        super.onResume()
        qaViewModel?.refreshInstalledSources()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".novelqa"))
        val container = (application as TankobunApplication).container
        val plugin = LnReaderPlugin("update-qa", "C Paper Words", "https://example.invalid", "English", "1.0.0",
            "https://example.invalid/1.js", repositoryUrl = "https://example.invalid/index.json")
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val name = chain.request().url.pathSegments.last()
            val bytes = if (name.endsWith(".js")) {
                val version = if (name == "1.js") "1.0.0" else "2.0.0"
                "exports.default={id:'update-qa',name:'C Paper Words',site:'https://example.invalid',version:'$version',pluginSettings:{quality:{type:'Switch',label:'Quality',value:true}},parseChapter:()=>'<p>Original QA text.</p>'};".toByteArray()
            } else File(filesDir, "update-qa/$name").readBytes()
            android.util.Log.i("ExtensionUpdatesQA", "download $name")
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(bytes.toResponseBody()).build()
        }.build()
        AppContainer::class.java.getDeclaredField("okHttpClient").apply { isAccessible = true }.set(container, client)
        val vm = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(container).also { model ->
                    // Keep this offline fixture from running unrelated startup catalog jobs.
                    MainViewModel::class.java.declaredFields.filter { Job::class.java.isAssignableFrom(it.type) }.forEach {
                        it.isAccessible = true
                        (it.get(model) as? Job)?.cancel()
                    }
                } as T
            }
        })[MainViewModel::class.java]
        qaViewModel = vm
        lifecycleScope.launch {
            val store = LnReaderPluginStore(application)
            if (store.record(plugin.packageName) == null) store.install(plugin, client, application)
            val apks = listOf("preferencesqa", "updatesqa").mapIndexed { index, suffix ->
                ExtensionIndexEntry("${if (index == 0) "A" else "B"} Paper Panels", "eu.kanade.tachiyomi.extension.en.$suffix", "$suffix.apk", "en", 2, "2.0", repositoryUrl = "https://example.invalid/index.min.json")
            }
            if (savedInstanceState == null) {
                @Suppress("UNCHECKED_CAST")
                val flow = MainViewModel::class.java.getDeclaredField("_state").apply { isAccessible = true }.get(vm) as MutableStateFlow<TankobunUiState>
                val available = apks + plugin.copy(version = "2.0.0", url = "https://example.invalid/2.js").indexEntry() +
                    apks.first().copy(name = "D Paper Garden", packageName = "eu.kanade.tachiyomi.extension.en.notinstalledqa")
                flow.update { it.copy(availableExtensions = available, message = null) }
            }
            vm.refreshInstalledSources()
        }
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            TankobunTheme(SettingsStore(this).themePreference()) {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) { SourcesSettingsScreen(state, vm) }
            }
        }
    }
}
