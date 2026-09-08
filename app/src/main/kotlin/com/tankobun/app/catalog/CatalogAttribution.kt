package com.tankobun.app.catalog

import org.json.JSONArray
import org.json.JSONObject

internal const val MANGA_BAKA_URL = "https://mangabaka.org"
internal const val MANGA_BAKA_DATA_LICENSE_URL = "https://mangabaka.org/about/data-license"
internal const val MANGA_BAKA_TERMS_URL = "https://mangabaka.org/about/terms"
internal const val MANGA_BAKA_PRIVACY_URL = "https://mangabaka.org/about/privacy"
internal const val MANGA_BAKA_CC_LICENSE_URL = "https://creativecommons.org/licenses/by-nc-sa/4.0/"

internal data class CatalogCredit(val name: String, val url: String)

internal val upstreamCatalogCredits = listOf(
    CatalogCredit("AniList", "https://anilist.co"),
    CatalogCredit("MangaUpdates", "https://www.mangaupdates.com"),
    CatalogCredit("MyAnimeList", "https://myanimelist.net"),
    CatalogCredit("Kitsu", "https://kitsu.app"),
    CatalogCredit("Shikimori", "https://shikimori.one"),
    CatalogCredit("Anime-Planet", "https://www.anime-planet.com"),
    CatalogCredit("Anime News Network", "https://www.animenewsnetwork.com"),
)

/** Keep the data notice with user exports; it does not license third-party data as app code. */
internal fun JSONObject.withCatalogAttribution(hasMangaBaka: Boolean): JSONObject {
    if (!hasMangaBaka) return this
    return put("attribution", JSONObject()
        .put("catalogs", JSONArray((listOf(CatalogCredit("MangaBaka", MANGA_BAKA_URL)) + upstreamCatalogCredits)
            .map { JSONObject().put("name", it.name).put("url", it.url) }))
        .put("mangaBakaOriginalDataLicense", MANGA_BAKA_CC_LICENSE_URL)
        .put("dataTerms", MANGA_BAKA_DATA_LICENSE_URL)
        .put("notice", "MangaBaka-original data: CC BY-NC-SA 4.0. Third-party data and images retain their respective rights and terms. Tankobun combines and formats metadata for display; adaptations of MangaBaka-original data retain CC BY-NC-SA 4.0. This notice grants no additional rights to third-party data. Tankobun is independent of these providers."))
}
