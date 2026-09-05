package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit
import java.io.InterruptedIOException
import com.tankobun.core.network.saturatedAddMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RateLimitInterceptor(
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
) : Interceptor {
    private val throttle = RequestThrottle(permits, unit.toMillis(period))

    override fun intercept(chain: Interceptor.Chain): Response {
        throttle.awaitTurn(chain.call())
        return chain.proceed(chain.request())
    }
}

fun OkHttpClient.Builder.rateLimit(
    permits: Int,
    period: Duration = 1.seconds,
): OkHttpClient.Builder = addNetworkInterceptor(
    RateLimitInterceptor(permits, period.inWholeMilliseconds, TimeUnit.MILLISECONDS),
)

fun OkHttpClient.Builder.rateLimit(
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
): OkHttpClient.Builder = addNetworkInterceptor(RateLimitInterceptor(permits, period, unit))

internal class RequestThrottle(
    permits: Int,
    periodMillis: Long,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000L },
    private val sleep: (Long) -> Unit = { Thread.sleep(it) },
) {
    // Round up: e.g. three permits/second must not become four in 999 ms.
    private val spacingMillis = periodMillis.coerceAtLeast(1L).let { period ->
        val count = permits.coerceAtLeast(1)
        period / count + if (period % count == 0L) 0L else 1L
    }
    private val lock = Any()
    private var nextAtMillis: Long? = null

    fun awaitTurn(call: Call) {
        while (true) {
            if (call.isCanceled() || Thread.currentThread().isInterrupted) throw InterruptedIOException("Canceled")
            val wait = synchronized(lock) {
                val now = nowMillis()
                val next = nextAtMillis
                if (next == null || now >= next) {
                    nextAtMillis = saturatedAddMillis(now, spacingMillis)
                    0L
                } else {
                    // Bounded waits let cancellation stop queued calls promptly.
                    (next - now).let { if (it <= 0L) 100L else minOf(it, 100L) }
                }
            }
            if (wait == 0L) return
            try {
                sleep(wait)
            } catch (error: InterruptedException) {
                Thread.currentThread().interrupt()
                throw InterruptedIOException("Canceled").apply { initCause(error) }
            }
        }
    }

    fun deferBy(delayMillis: Long) {
        synchronized(lock) {
            val deadline = saturatedAddMillis(nowMillis(), delayMillis)
            nextAtMillis = maxOf(nextAtMillis ?: deadline, deadline)
        }
    }
}
