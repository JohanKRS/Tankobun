package com.tankobun.app

import android.app.Instrumentation
import android.content.Context
import com.tankobun.app.catalog.CatalogDataSource
import com.tankobun.core.anilist.AnilistGraphQlClient
import com.tankobun.core.anilist.AnilistRepository
import com.tankobun.core.mangabaka.MangaBakaRepository
import com.tankobun.core.model.CatalogMode
import com.tankobun.core.network.RespectfulRateLimiter
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.CopyOnWriteArrayList

/** Exercise real catalog routing without accounts, network access or bundled source content. */
internal suspend fun Instrumentation.checkCatalogStartup() {
    val container = (targetContext.applicationContext as TankobunApplication).container
    val prefs = targetContext.getSharedPreferences("tankobun_settings", Context.MODE_PRIVATE)
    val previousMode = prefs.getString("catalog.navigation.mode", null)
    val originalAniList = container.anilistRepository
    val originalMangaBaka = container.mangaBakaRepository
    fun replace(name: String, value: Any) {
        AppContainer::class.java.getDeclaredField(name).apply { isAccessible = true }.set(container, value)
    }
    try {
        prefs.edit().remove("catalog.navigation.mode").commit()
        check(SettingsStore(targetContext).catalogMode() == CatalogMode.ANILIST)
        for (savedMode in listOf(null, CatalogMode.MANGABAKA, CatalogMode.COMBINED)) {
            savedMode?.let { container.settingsStore.saveCatalogMode(it) }
            val mode = SettingsStore(targetContext).catalogMode()
            check(mode == (savedMode ?: CatalogMode.ANILIST))
            for (browse in listOf(false, true)) {
                for (failAniList in if (savedMode == null) listOf(false, true) else listOf(false)) {
                    val requests = CopyOnWriteArrayList<String>()
                    val client = OkHttpClient.Builder().addInterceptor { chain ->
                        val aniList = chain.request().url.encodedPath == "/graphql"
                        requests.add(if (aniList) "AniList" else "MangaBaka")
                        check(chain.request().header("Authorization") == null)
                        val media = """{"id":700001,"title":{"romaji":"Paper Garden"},"genres":[],"isAdult":false}"""
                        val body = when {
                            aniList && failAniList -> """{"errors":[{"message":"Fixture unavailable"}]}"""
                            aniList -> """{"data":{${listOf("trending", "popular", "popularManhwa", "topManga").joinToString(",") { "\"$it\":{\"media\":[$media]}" }}}}"""
                            chain.request().url.encodedPath.endsWith("/tags") -> """{"data":[]}"""
                            else -> """{"data":[{"id":700002,"state":"active","type":"manga","titles":[{"language":"en","is_primary":true,"title":"Paper Sky"}],"content_rating":"safe"}],"pagination":{"next":null}}"""
                        }
                        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                            .code(if (aniList && failAniList) 503 else 200).message("Fixture")
                            .body(body.toResponseBody()).build()
                    }.build()
                    replace("anilistRepository", AnilistRepository(AnilistGraphQlClient(client, RespectfulRateLimiter(0), "https://example.invalid/graphql")))
                    replace("mangaBakaRepository", MangaBakaRepository(client, baseUrl = "https://example.invalid"))
                    val catalog = CatalogDataSource(container)
                    val trending = if (browse) catalog.browseLanding(10, null, false, mode).trending
                        else catalog.homeFeed(emptyList(), null, false, mode).trending
                    check(trending.isNotEmpty())
                    when (mode) {
                        CatalogMode.ANILIST -> {
                            check(requests.first() == "AniList")
                            if (failAniList) {
                                check(requests.drop(1).isNotEmpty() && requests.drop(1).all { it == "MangaBaka" })
                                check(trending.all { it.mangaBakaId == 700002 })
                            } else {
                                check(requests == listOf("AniList"))
                                check(trending.all { it.id == 700001 })
                            }
                        }
                        CatalogMode.MANGABAKA -> check(requests.all { it == "MangaBaka" })
                        CatalogMode.COMBINED -> check(requests.toSet() == setOf("AniList", "MangaBaka") && trending.size == 2)
                    }
                    check(SettingsStore(targetContext).catalogMode() == mode)
                }
            }
        }
    } finally {
        replace("anilistRepository", originalAniList)
        replace("mangaBakaRepository", originalMangaBaka)
        prefs.edit().putString("catalog.navigation.mode", previousMode).commit()
    }
}
