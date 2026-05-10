package eu.kanade.tachiyomi.network

import android.content.Context
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
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
            return System.getProperty("http.agent").takeUnless { it.isNullOrBlank() } ?: "Tankobun Android"
        }

        private fun ensureUserAgent() {
            if (System.getProperty("http.agent").isNullOrBlank()) {
                System.setProperty("http.agent", "Tankobun Android")
            }
        }

        private fun baseBuilder(): OkHttpClient.Builder =
            OkHttpClient.Builder()
                .cache(sourceCache())
                .callTimeout(45, TimeUnit.SECONDS)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .addInterceptor(DefaultUserAgentInterceptor)
                .addInterceptor(PoliteHostThrottleInterceptor)

        private fun sourceCache(): Cache? {
            val context = appContext ?: return null
            val cacheDir = File(context.cacheDir, "source-http").also { it.mkdirs() }
            return Cache(cacheDir, 128L * 1024L * 1024L)
        }
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
