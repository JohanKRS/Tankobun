package eu.kanade.tachiyomi.network

/*
 * Extension compatibility behavior adapted from Mihon's Tachiyomi-compatible
 * network layer, with Tankobun-specific integration changes. See NOTICE.md.
 */

import android.os.Build
import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

internal object AndroidCookieJar : CookieJar {
    private val cookieManager: CookieManager by lazy {
        CookieManager.getInstance().also { manager ->
            manager.setAcceptCookie(true)
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie ->
            cookieManager.setCookie(url.toString(), cookie.toString())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.flush()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return get(url)
    }

    fun get(url: HttpUrl): List<Cookie> {
        val cookieHeader = cookieManager.getCookie(url.toString()).orEmpty()
        if (cookieHeader.isBlank()) return emptyList()

        return cookieHeader
            .split(';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { Cookie.parse(url, it) }
    }

    fun remove(url: HttpUrl, cookieNames: List<String>? = null, maxAge: Int = -1): Int {
        val cookieHeader = cookieManager.getCookie(url.toString()) ?: return 0
        return cookieHeader
            .split(';')
            .map { it.substringBefore('=').trim() }
            .filter { it.isNotBlank() }
            .filter { name -> cookieNames == null || name in cookieNames }
            .onEach { name -> cookieManager.setCookie(url.toString(), "$name=;Max-Age=$maxAge") }
            .count()
    }
}
