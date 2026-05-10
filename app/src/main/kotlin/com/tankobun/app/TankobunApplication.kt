package com.tankobun.app

import android.app.Application
import com.tankobun.core.anilist.AnilistGraphQlClient
import com.tankobun.core.anilist.AnilistRepository
import com.tankobun.core.database.DatabaseFactory
import com.tankobun.core.extensions.ExtensionIndexRepository
import com.tankobun.core.extensions.InstalledExtensionScanner
import com.tankobun.core.extensions.SourceMatcher
import com.tankobun.core.extensions.TachiyomiSourceHost
import com.tankobun.core.network.RespectfulRateLimiter
import okhttp3.Cache
import okhttp3.OkHttpClient
import uy.kohesive.injekt.TankobunInjektRegistry
import java.io.File
import java.util.concurrent.TimeUnit

class TankobunApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        TankobunInjektRegistry.registerApplication(this)
    }
}

class AppContainer(application: Application) {
    val database = DatabaseFactory.create(application)

    private val cacheDir = File(application.cacheDir, "http").also { it.mkdirs() }
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(cacheDir, 128L * 1024L * 1024L))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val tokenStore = SecureTokenStore(application)
    val settingsStore = SettingsStore(application)

    val anilistRepository = AnilistRepository(
        AnilistGraphQlClient(
            okHttpClient = okHttpClient,
            rateLimiter = RespectfulRateLimiter(minSpacingMillis = 2_500L),
        ),
    )

    val extensionRepository = ExtensionIndexRepository(
        okHttpClient = okHttpClient,
        rateLimiter = RespectfulRateLimiter(minSpacingMillis = 1_500L),
    )

    val extensionScanner = InstalledExtensionScanner(application)
    val sourceHost = TachiyomiSourceHost(application)
    val sourceMatcher = SourceMatcher()
}
