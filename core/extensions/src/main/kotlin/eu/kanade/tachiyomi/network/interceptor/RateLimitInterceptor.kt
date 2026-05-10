package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RateLimitInterceptor(
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
) : Interceptor {
    private val throttle = RequestThrottle(permits, unit.toMillis(period))

    override fun intercept(chain: Interceptor.Chain): Response {
        throttle.awaitTurn()
        return chain.proceed(chain.request())
    }
}

fun OkHttpClient.Builder.rateLimit(
    permits: Int,
    period: Duration = 1.seconds,
): OkHttpClient.Builder = addInterceptor(
    RateLimitInterceptor(permits, period.inWholeMilliseconds, TimeUnit.MILLISECONDS),
)

fun OkHttpClient.Builder.rateLimit(
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
): OkHttpClient.Builder = addInterceptor(RateLimitInterceptor(permits, period, unit))

internal class RequestThrottle(
    permits: Int,
    periodMillis: Long,
) {
    private val delayMillis = (periodMillis.coerceAtLeast(1L) / permits.coerceAtLeast(1)).coerceAtLeast(1L)
    private var nextAtMillis: Long = 0L

    @Synchronized
    fun awaitTurn() {
        val now = System.currentTimeMillis()
        if (nextAtMillis > now) {
            Thread.sleep(nextAtMillis - now)
        }
        nextAtMillis = maxOf(System.currentTimeMillis(), nextAtMillis) + delayMillis
    }
}
