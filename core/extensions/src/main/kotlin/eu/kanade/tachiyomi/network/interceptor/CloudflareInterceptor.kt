package eu.kanade.tachiyomi.network.interceptor

/*
 * Extension compatibility behavior adapted from Mihon's Tachiyomi-compatible
 * Cloudflare handling, with Tankobun-specific integration changes. See NOTICE.md.
 */

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import eu.kanade.tachiyomi.network.AndroidCookieJar
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.Cookie
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal object CloudflareInterceptor : Interceptor {
    private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!response.isCloudflareChallenge()) {
            return response
        }

        response.close()
        try {
            resolveWithWebView(request)
        } catch (error: Exception) {
            throw IOException("Cloudflare bypass failed", error)
        }
        return chain.proceed(request)
    }

    private fun Response.isCloudflareChallenge(): Boolean {
        if (code !in ERROR_CODES) return false
        if (!header("Server").orEmpty().contains("cloudflare", ignoreCase = true)) return false
        if (header("Cf-Mitigated").orEmpty().equals("challenge", ignoreCase = true)) return true

        return runCatching {
            val document = Jsoup.parse(peekBody(Long.MAX_VALUE).string(), request.url.toString())
            document.getElementById("challenge-error-title") != null ||
                document.getElementById("challenge-error-text") != null
        }.getOrDefault(false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(originalRequest: Request) {
        val context = NetworkHelper.applicationContextOrNull()
            ?: throw IllegalStateException("NetworkHelper has not been configured")
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw IllegalStateException("Cloudflare bypass cannot run on the main thread")
        }

        val latch = CountDownLatch(1)
        val oldCookie = AndroidCookieJar.get(originalRequest.url)
            .firstOrNull { it.name == CF_CLEARANCE_COOKIE }
        var cloudflareBypassed = false
        var challengeFound = false
        var webView: WebView? = null

        AndroidCookieJar.remove(originalRequest.url, COOKIE_NAMES, 0)
        mainHandler.post {
            runCatching {
                webView = WebView(context).also { view ->
                    view.configure(originalRequest)
                    view.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            if (hasNewClearance(originalRequest, oldCookie)) {
                                cloudflareBypassed = true
                                latch.countDown()
                            }
                            if (url == originalRequest.url.toString() && !challengeFound) {
                                latch.countDown()
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?,
                        ) {
                            if (request?.isForMainFrame == true) {
                                if (errorResponse?.statusCode in ERROR_CODES) {
                                    challengeFound = true
                                } else {
                                    latch.countDown()
                                }
                            }
                        }
                    }
                    view.loadUrl(originalRequest.url.toString(), originalRequest.headers.toWebViewRequestHeaders())
                }
            }.onFailure {
                latch.countDown()
            }
        }

        latch.await(WEBVIEW_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        mainHandler.post {
            webView?.run {
                stopLoading()
                destroy()
            }
        }

        if (!cloudflareBypassed) {
            throw CloudflareBypassException()
        }
    }

    private fun hasNewClearance(request: Request, oldCookie: Cookie?): Boolean =
        AndroidCookieJar.get(request.url)
            .firstOrNull { it.name == CF_CLEARANCE_COOKIE }
            .let { it != null && it != oldCookie }

    private fun WebView.configure(request: Request) {
        with(settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportMultipleWindows(true)
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            userAgentString = request.header("User-Agent") ?: NetworkHelper.defaultUserAgent()
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    }

    private fun Headers.toWebViewRequestHeaders(): Map<String, String> =
        filter { (name, value) -> isRequestHeaderSafe(name, value) }
            .groupBy(keySelector = { (name, _) -> name }) { (_, value) -> value }
            .mapValues { (_, values) -> values.firstOrNull().orEmpty() }

    private fun isRequestHeaderSafe(rawName: String, rawValue: String): Boolean {
        val name = rawName.lowercase(Locale.ENGLISH)
        val value = rawValue.lowercase(Locale.ENGLISH)
        if (name in UNSAFE_HEADER_NAMES || name.startsWith("proxy-")) return false
        if (name == "connection" && value == "upgrade") return false
        return true
    }

    private const val WEBVIEW_TIMEOUT_SECONDS = 30L
    private const val CF_CLEARANCE_COOKIE = "cf_clearance"
    private val ERROR_CODES = listOf(403, 503)
    private val COOKIE_NAMES = listOf(CF_CLEARANCE_COOKIE)
    private val UNSAFE_HEADER_NAMES = listOf(
        "content-length",
        "host",
        "trailer",
        "te",
        "upgrade",
        "cookie2",
        "keep-alive",
        "transfer-encoding",
        "set-cookie",
    )
}

private class CloudflareBypassException : Exception()
