package eu.kanade.tachiyomi.network.interceptor

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SpecificHostRateLimitInterceptor(
    private val httpUrl: HttpUrl,
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
) : Interceptor {
    private val throttle = RequestThrottle(permits, unit.toMillis(period))

    override fun intercept(chain: Interceptor.Chain): Response {
        if (chain.request().url.host == httpUrl.host) {
            throttle.awaitTurn(chain.call())
        }
        return chain.proceed(chain.request())
    }
}

fun OkHttpClient.Builder.rateLimitHost(
    url: String,
    permits: Int,
    period: Duration = 1.seconds,
): OkHttpClient.Builder = rateLimitHost(url.toHttpUrl(), permits, period)

fun OkHttpClient.Builder.rateLimitHost(
    httpUrl: HttpUrl,
    permits: Int,
    period: Duration = 1.seconds,
): OkHttpClient.Builder = addNetworkInterceptor(
    SpecificHostRateLimitInterceptor(httpUrl, permits, period.inWholeMilliseconds, TimeUnit.MILLISECONDS),
)

fun OkHttpClient.Builder.rateLimitHost(
    httpUrl: HttpUrl,
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
): OkHttpClient.Builder = addNetworkInterceptor(SpecificHostRateLimitInterceptor(httpUrl, permits, period, unit))
