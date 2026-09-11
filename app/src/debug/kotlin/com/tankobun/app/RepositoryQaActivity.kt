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
import com.tankobun.app.ui.settings.SourcesSettingsScreen
import com.tankobun.core.extensions.ExtensionIndexRepository
import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.Job
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Original repository fixtures served in memory; no source executable or reading content. */
class RepositoryQaActivity : ComponentActivity() {
    lateinit var model: MainViewModel
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".novelqa"))
        val container = (application as TankobunApplication).container
        if (intent.getBooleanExtra("identityChangeFixture", false)) {
            val url = "https://repositories.example.invalid/panels/index.json"
            container.settingsStore.saveExtensionRepositories(listOf(url))
            com.tankobun.core.extensions.RepositoryTrustStore(this).checkAndRemember(url,
                com.tankobun.core.extensions.ExtensionIndexResult(emptyList(), url, "a".repeat(64)))
        }
        if (intent.getBooleanExtra("filterFixture", false)) {
            container.settingsStore.saveExtensionRepositories(repositoryFilterQaUrls())
        }
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val path = chain.request().url.encodedPath
            if (path.endsWith(".json")) requests.add(path)
            if (path == "/slow/index.json") gate?.await(10, TimeUnit.SECONDS)
            val body = when (path) {
                "/alias.json" -> """{"index_v2":"/panels/index.json"}"""
                "/panels/index.json", "/slow/index.json" -> """[{"name":"Paper Panels","pkg":"eu.kanade.tachiyomi.extension.en.repositoryqa","apk":"paper.apk","lang":"en","code":1,"version":"1.0"}]"""
                "/mirror/index.json" -> """[{"name":"Paper Panels","pkg":"eu.kanade.tachiyomi.extension.en.repositoryqa","apk":"mirror.apk","lang":"en","code":2,"version":"2.0"}]"""
                "/novels/index.json" -> """[{"id":"repositoryqa","name":"Paper Words","site":"https://example.invalid","lang":"English","version":"1.0.0","url":"scripts/paper.js"}]"""
                else -> null
            }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(if (body != null) 200 else 404).message(if (body != null) "OK" else "Not found")
                .body((body ?: "").toResponseBody()).build()
        }.build()
        AppContainer::class.java.getDeclaredField("okHttpClient").apply { isAccessible = true }.set(container, client)
        AppContainer::class.java.getDeclaredField("extensionRepository").apply { isAccessible = true }
            .set(container, ExtensionIndexRepository(client, RespectfulRateLimiter(0)))
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(container).also { model ->
                    MainViewModel::class.java.declaredFields.filter {
                        Job::class.java.isAssignableFrom(it.type) && it.name != "extensionRepositoryJob"
                    }.forEach { field -> field.isAccessible = true; (field.get(model) as? Job)?.cancel() }
                } as T
            }
        })[MainViewModel::class.java]
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            TankobunTheme(SettingsStore(this).themePreference()) {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                    SourcesSettingsScreen(if (intent.getBooleanExtra("filterFixture", false)) repositoryFilterQaState(state) else state,
                        model, openRepository = true)
                }
            }
        }
    }

    companion object {
        val requests = CopyOnWriteArrayList<String>()
        @Volatile var gate: CountDownLatch? = null
    }
}
