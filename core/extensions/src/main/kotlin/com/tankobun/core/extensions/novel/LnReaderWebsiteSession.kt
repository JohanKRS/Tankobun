package com.tankobun.core.extensions.novel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import eu.kanade.tachiyomi.network.NetworkHelper
import org.json.JSONArray
import org.json.JSONObject

/** A user-visible website has no native JS bridge. Only its own main-frame storage is captured. */
class LnReaderWebsiteSession(context: Context, packageName: String, val initialUrl: String) {
    private val origin = requireNotNull(webOrigin(initialUrl)) { "Invalid source website" }
    private val store = LnReaderSettingsStore(context, packageName)

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(view: WebView, onNavigation: (String, Boolean) -> Unit = { _, _ -> }) {
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(false)
            userAgentString = NetworkHelper.defaultUserAgent()
        }
        CookieManager.getInstance().setAcceptCookie(true)
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = webOrigin(request.url.toString()) == null
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) { onNavigation(url.orEmpty(), true) }
            override fun onPageFinished(view: WebView, url: String?) {
                onNavigation(url.orEmpty(), false)
                capture(view)
            }
        }
    }

    fun capture(view: WebView, onComplete: (Boolean) -> Unit = {}) {
        if (webOrigin(view.url.orEmpty()) != origin) { onComplete(false); return }
        view.evaluateJavascript("""
            (() => {
              const read = storage => { const result = {}; for (let i=0;i<storage.length;i++) { const key=storage.key(i); result[key]=storage.getItem(key); } return result; };
              return JSON.stringify({origin:location.origin,local:read(localStorage),session:read(sessionStorage)});
            })()
        """.trimIndent()) { encoded ->
            val saved = runCatching {
                check(encoded.length <= 4 * 1024 * 1024)
                val value = JSONObject(JSONArray("[$encoded]").getString(0))
                check(value.getString("origin") == origin && webOrigin(view.url.orEmpty()) == origin)
                store.capture(origin, value.getJSONObject("local"), value.getJSONObject("session"))
                CookieManager.getInstance().flush()
                true
            }.getOrDefault(false)
            onComplete(saved)
        }
    }
}
