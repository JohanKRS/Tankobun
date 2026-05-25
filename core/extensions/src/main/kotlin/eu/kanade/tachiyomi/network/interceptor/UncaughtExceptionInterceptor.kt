package eu.kanade.tachiyomi.network.interceptor

/*
 * Extension compatibility behavior adapted from Mihon's Tachiyomi-compatible
 * network layer, with Tankobun-specific integration changes. See NOTICE.md.
 */

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

internal object UncaughtExceptionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        try {
            chain.proceed(chain.request())
        } catch (error: Exception) {
            if (error is IOException) throw error
            throw IOException(error)
        }
}
