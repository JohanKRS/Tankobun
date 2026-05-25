package eu.kanade.tachiyomi.network.interceptor

/*
 * Extension compatibility behavior adapted from Mihon's Tachiyomi-compatible
 * network layer, with Tankobun-specific integration changes. See NOTICE.md.
 */

import okhttp3.Interceptor
import okhttp3.Response

internal object IgnoreGzipInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = if (chain.request().header("Accept-Encoding") == "gzip") {
            chain.request().newBuilder().removeHeader("Accept-Encoding").build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
