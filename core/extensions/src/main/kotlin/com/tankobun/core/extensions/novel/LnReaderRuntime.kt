package com.tankobun.core.extensions.novel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

/** A bounded, disposable WebView JS realm. Network and storage are scoped to installed plugins. */
class LnReaderRuntime(private val context: Context) {
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun call(plugin: LnReaderPlugin, code: String, operation: String, args: JSONArray, persistStorage: Boolean = true): Any? =
        locks[(plugin.packageName.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            slots.withPermit {
                withTimeout(120_000L) {
                    coroutineScope {
                        val scope = this
                        val result = CompletableDeferred<JSONObject>()
                        val preferences = context.getSharedPreferences("novel_${plugin.packageName}", Context.MODE_PRIVATE)
                        val stored = JSONObject(preferences.getString("storage", "{}").orEmpty())
                        val pluginSettings = LnReaderSettingsStore(context, plugin.packageName)
                        val configured = pluginSettings.configuredValues()
                        val applied = JSONObject(preferences.getString("appliedSettings", "{}").orEmpty())
                        val overrides = JSONObject().apply {
                            configured.keys().forEach { key ->
                                if (!applied.has(key) || configured.opt(key).toString() != applied.opt(key).toString()) put(key, configured.get(key))
                            }
                        }
                        val web = JSONObject().put("site", plugin.site).put("origins", pluginSettings.webStorage())
                        val assets = if (operation == "parseChapter") withContext(Dispatchers.IO) {
                            LnReaderPluginStore(context).record(plugin.packageName)?.let { LnReaderPluginStore(context).chapterAssets(plugin.packageName) }
                        } else null
                        val chapterAssets = JSONObject().put("javascript", assets?.javascript.orEmpty()).put("css", assets?.css.orEmpty())
                        val runtime = withContext(Dispatchers.IO) { context.assets.open("novel/runtime.js").bufferedReader().use { it.readText() } }
                        var view: WebView? = null
                        val bridge = object {
                            @JavascriptInterface fun complete(value: String) {
                                runCatching { JSONObject(value) }.onSuccess { payload ->
                                    if (payload.has("error")) result.completeExceptionally(IllegalStateException(payload.getString("error")))
                                    else result.complete(payload)
                                }.onFailure { result.completeExceptionally(it) }
                            }
                            @JavascriptInterface fun fetch(value: String) {
                                scope.launch(Dispatchers.IO) {
                                    val payload = JSONObject(value)
                                    val id = payload.getInt("id")
                                    val reply = try { fetchResponse(payload).put("id", id) }
                                    catch (error: CancellationException) { throw error }
                                    catch (error: Exception) { JSONObject().put("id", id).put("error", error.message ?: "Network request failed") }
                                    withContext(Dispatchers.Main) { view?.evaluateJavascript("TankobunRuntime.reply($reply)", null) }
                                }
                            }
                        }
                        try {
                            withContext(Dispatchers.Main) {
                                view = WebView(context.applicationContext).apply {
                                    settings.javaScriptEnabled = true
                                    settings.allowFileAccess = false
                                    settings.allowContentAccess = false
                                    settings.blockNetworkLoads = true
                                    settings.domStorageEnabled = false
                                    addJavascriptInterface(bridge, "TankobunNative")
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
                                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) = WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                                        override fun onPageFinished(view: WebView, url: String) {
                                            view.evaluateJavascript(runtime + "\nTankobunRuntime.run(${JSONObject.quote(code)},${JSONObject.quote(operation)},$args,$stored,$overrides,$web,$chapterAssets).then(r=>TankobunNative.complete(JSON.stringify(r))).catch(e=>TankobunNative.complete(JSON.stringify({error:String(e.message||e)})));", null)
                                        }
                                    }
                                    loadDataWithBaseURL("https://runtime.invalid/", "<!doctype html><meta charset=utf-8>", "text/html", "UTF-8", null)
                                }
                            }
                            val response = result.await()
                            if (persistStorage) withContext(Dispatchers.IO) {
                                check(preferences.edit().putString("storage", response.optJSONObject("storage")?.toString() ?: "{}").putString("appliedSettings", configured.toString()).commit())
                                response.optJSONObject("website")?.let { website ->
                                    pluginSettings.capture(website.getString("origin"), website.getJSONObject("local"), website.getJSONObject("session"))
                                }
                            }
                            response.opt("value").takeUnless { it === JSONObject.NULL }
                        } finally {
                            withContext(NonCancellable + Dispatchers.Main) {
                                view?.apply { removeJavascriptInterface("TankobunNative"); stopLoading(); destroy() }
                                view = null
                            }
                        }
                    }
                }
            }
        }

    private suspend fun fetchResponse(payload: JSONObject): JSONObject {
        val method = payload.optString("method", "GET")
        val headers = payload.optJSONObject("headers") ?: JSONObject()
        val body = if (payload.isNull("body")) null else Base64.decode(payload.getString("body"), Base64.DEFAULT)
            .toRequestBody(headers.optString("content-type").toMediaTypeOrNull())
        val request = Request.Builder().url(payload.getString("url")).method(method, body).apply {
            header("Connection", "keep-alive")
            header("Accept", "*/*")
            header("Accept-Language", "*")
            header("Sec-Fetch-Mode", "cors")
            header("Cache-Control", "max-age=0")
            // The shared client supplies the configured User-Agent and negotiates/decompresses gzip.
            headers.keys().forEach { name -> header(name, headers.getString(name)) }
        }.build()
        val call = NetworkHelper().client.newCall(request)
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, error: java.io.IOException) {
                    if (continuation.isActive) continuation.resumeWith(Result.failure(error))
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    val result = runCatching {
                        response.use {
                            val bytes = it.body.byteStream().use { input -> readBounded(input, 16 * 1024 * 1024) }
                            JSONObject().put("status", it.code).put("url", it.request.url.toString())
                                .put("headers", JSONObject(it.headers.toMap()))
                                .put("body", Base64.encodeToString(bytes, Base64.NO_WRAP)).apply {
                                    payload.optString("encoding").takeIf { name -> name.isNotBlank() && name != "null" }?.let { name -> put("text", bytes.toString(Charset.forName(name))) }
                                }
                        }
                    }
                    if (continuation.isActive) continuation.resumeWith(result)
                }
            })
        }
    }

    companion object {
        private val slots = Semaphore(2)
        private val locks = Array(32) { Mutex() }
    }
}

internal fun readBounded(input: java.io.InputStream, limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        check(output.size() + read <= limit) { "Source response is too large" }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}
