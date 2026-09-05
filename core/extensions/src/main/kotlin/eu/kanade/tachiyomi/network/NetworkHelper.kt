package eu.kanade.tachiyomi.network

/*
 * Extension compatibility behavior adapted from Mihon's Tachiyomi-compatible
 * network layer, with Tankobun-specific integration changes. See NOTICE.md.
 */

import android.content.Context
import android.webkit.WebSettings
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.File
import com.tankobun.core.network.serverBackoffMillis
import eu.kanade.tachiyomi.network.interceptor.RequestThrottle
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class NetworkHelper {
    val client: OkHttpClient
        get() = sharedClient

    val cloudflareClient: OkHttpClient = client

    val defaultUserAgentProvider: () -> String = { defaultUserAgent() }

    companion object {
        @Volatile
        private var appContext: Context? = null
        private val sharedClient: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            baseBuilder().build()
        }

        fun configure(context: Context) {
            appContext = context.applicationContext
            ensureUserAgent()
        }

        fun defaultUserAgent(): String {
            ensureUserAgent()
            return System.getProperty("http.agent").takeUnless { it.isNullOrBlank() } ?: FALLBACK_BROWSER_USER_AGENT
        }

        internal fun applicationContextOrNull(): Context? = appContext

        private fun ensureUserAgent() {
            val current = System.getProperty("http.agent")
            if (shouldReplaceHttpAgent(current)) {
                System.setProperty("http.agent", platformUserAgent() ?: FALLBACK_BROWSER_USER_AGENT)
            }
        }

        private fun baseBuilder(): OkHttpClient.Builder =
            OkHttpClient.Builder()
                .cache(sourceCache())
                .cookieJar(AndroidCookieJar)
                // Connect/read timeouts bound network stalls; quota waits remain cancellable.
                .callTimeout(0, TimeUnit.SECONDS)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .addInterceptor(UncaughtExceptionInterceptor)
                .addInterceptor(SupportedEncodingInterceptor)
                .addInterceptor(UserAgentInterceptor)
                .addInterceptor(CloudflareInterceptor)
                .addNetworkInterceptor(PoliteHostThrottleInterceptor())

        private fun sourceCache(): Cache? {
            val context = appContext ?: return null
            val cacheDir = File(context.cacheDir, "source-http").also { it.mkdirs() }
            return Cache(cacheDir, 128L * 1024L * 1024L)
        }

        private fun platformUserAgent(): String? =
            appContext?.let { context ->
                runCatching { WebSettings.getDefaultUserAgent(context) }
                    .getOrNull()
                    ?.takeUnless { it.isBlank() }
                    ?.toBrowserLikeUserAgent()
            }

        private const val FALLBACK_BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36"
    }
}

private fun String.toBrowserLikeUserAgent(): String =
    replace("; Android .*?\\)".toRegex(), "; Android 10; K)")
        .replace("Version/.* Chrome/".toRegex(), "Chrome/")

internal fun shouldReplaceHttpAgent(agent: String?): Boolean =
    agent.isNullOrBlank() ||
        agent == "Tankobun Android" ||
        agent.startsWith("Dalvik/", ignoreCase = true)

private object SupportedEncodingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val nextEncoding = supportedAcceptEncoding(request.header("Accept-Encoding"))
        val nextRequest = if (nextEncoding != request.header("Accept-Encoding")) {
            request.newBuilder().also { builder ->
                if (nextEncoding == null) {
                    builder.removeHeader("Accept-Encoding")
                } else {
                    builder.header("Accept-Encoding", nextEncoding)
                }
            }.build()
        } else {
            request
        }
        return chain.proceed(nextRequest)
    }
}

private object UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header("User-Agent") != null) {
            return chain.proceed(request)
        }
        return chain.proceed(
            request.newBuilder()
                .header("User-Agent", NetworkHelper.defaultUserAgent())
                .build(),
        )
    }
}

internal fun supportedAcceptEncoding(encoding: String?): String? {
    if (encoding.isNullOrBlank()) return encoding
    val tokens = encoding
        .split(',')
        .map { it.substringBefore(';').trim().lowercase() }
        .filter { it.isNotBlank() }
    return if (tokens.any { it == "br" || it == "zstd" || it == "deflate" }) null else encoding
}

internal class PoliteHostThrottleInterceptor(
    minSpacingMillis: Long = 200L,
) : Interceptor {
    private val spacingMillis = minSpacingMillis
    private val throttles = ConcurrentHashMap<String, RequestThrottle>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val throttle = throttles.computeIfAbsent(chain.request().url.host) { RequestThrottle(1, spacingMillis) }
        throttle.awaitTurn(chain.call())
        return chain.proceed(chain.request()).also { response ->
            serverBackoffMillis(response.headers, response.code, System.currentTimeMillis())?.let(throttle::deferBy)
        }
    }
}
