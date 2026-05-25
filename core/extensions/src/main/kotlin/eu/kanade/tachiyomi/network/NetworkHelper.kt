package eu.kanade.tachiyomi.network

/*
 * Extension compatibility behavior adapted from Mihon's Tachiyomi-compatible
 * network layer, with Tankobun-specific integration changes. See NOTICE.md.
 */

import android.content.Context
import android.webkit.WebSettings
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.IgnoreGzipInterceptor
import eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.brotli.BrotliInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

class NetworkHelper {
    val client: OkHttpClient = baseBuilder()
        .build()

    val cloudflareClient: OkHttpClient = client

    val defaultUserAgentProvider: () -> String = { defaultUserAgent() }

    companion object {
        @Volatile
        private var appContext: Context? = null

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
                .callTimeout(45, TimeUnit.SECONDS)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .addInterceptor(UncaughtExceptionInterceptor)
                .addInterceptor(SupportedEncodingInterceptor)
                .addInterceptor(DefaultUserAgentInterceptor)
                .addInterceptor(CloudflareInterceptor)
                .addInterceptor(PoliteHostThrottleInterceptor)
                .addNetworkInterceptor(IgnoreGzipInterceptor)
                .addNetworkInterceptor(BrotliInterceptor)

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

private object DefaultUserAgentInterceptor : Interceptor {
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

private object PoliteHostThrottleInterceptor : Interceptor {
    private const val MIN_SPACING_MILLIS = 200L
    private val nextRequestAtByHost = mutableMapOf<String, Long>()

    override fun intercept(chain: Interceptor.Chain): Response {
        awaitTurn(chain.request().url.host)
        return chain.proceed(chain.request())
    }

    private fun awaitTurn(host: String) {
        val delayMillis = synchronized(nextRequestAtByHost) {
            val now = System.currentTimeMillis()
            val nextAt = nextRequestAtByHost[host] ?: 0L
            val delay = (nextAt - now).coerceAtLeast(0L)
            nextRequestAtByHost[host] = maxOf(now, nextAt) + MIN_SPACING_MILLIS
            delay
        }
        if (delayMillis > 0L) {
            Thread.sleep(delayMillis)
        }
    }
}
