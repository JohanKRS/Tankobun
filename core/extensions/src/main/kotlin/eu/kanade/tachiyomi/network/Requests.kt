package eu.kanade.tachiyomi.network

import java.util.concurrent.TimeUnit
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.RequestBody

internal val DEFAULT_SOURCE_CACHE_CONTROL: CacheControl = CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build()

fun GET(url: String, headers: Headers = Headers.headersOf()): Request =
    Request.Builder().url(url).headers(headers).cacheControl(DEFAULT_SOURCE_CACHE_CONTROL).get().build()

fun GET(
    url: String,
    headers: Headers = Headers.headersOf(),
    cache: CacheControl = DEFAULT_SOURCE_CACHE_CONTROL,
): Request =
    Request.Builder().url(url).headers(headers).cacheControl(cache).get().build()

fun GET(
    url: HttpUrl,
    headers: Headers = Headers.headersOf(),
    cache: CacheControl = DEFAULT_SOURCE_CACHE_CONTROL,
): Request =
    Request.Builder().url(url).headers(headers).cacheControl(cache).get().build()

fun POST(url: String, headers: Headers = Headers.headersOf()): Request =
    Request.Builder().url(url).headers(headers).post(ByteArray(0).toRequestBody()).build()

fun POST(
    url: String,
    headers: Headers = Headers.headersOf(),
    body: RequestBody = ByteArray(0).toRequestBody(),
): Request =
    Request.Builder().url(url).headers(headers).post(body).build()

fun POST(
    url: String,
    headers: Headers = Headers.headersOf(),
    body: RequestBody = ByteArray(0).toRequestBody(),
    cache: CacheControl = DEFAULT_SOURCE_CACHE_CONTROL,
): Request =
    Request.Builder().url(url).headers(headers).cacheControl(cache).post(body).build()

fun POST(
    url: HttpUrl,
    headers: Headers = Headers.headersOf(),
    body: RequestBody = ByteArray(0).toRequestBody(),
): Request =
    Request.Builder().url(url).headers(headers).post(body).build()

fun POST(
    url: HttpUrl,
    headers: Headers = Headers.headersOf(),
    body: RequestBody = ByteArray(0).toRequestBody(),
    cache: CacheControl = DEFAULT_SOURCE_CACHE_CONTROL,
): Request =
    Request.Builder().url(url).headers(headers).cacheControl(cache).post(body).build()

fun PUT(
    url: String,
    headers: Headers = Headers.headersOf(),
    body: RequestBody = ByteArray(0).toRequestBody(),
): Request =
    Request.Builder().url(url).headers(headers).put(body).build()

fun PUT(
    url: HttpUrl,
    headers: Headers = Headers.headersOf(),
    body: RequestBody = ByteArray(0).toRequestBody(),
): Request =
    Request.Builder().url(url).headers(headers).put(body).build()

fun PATCH(
    url: String,
    headers: Headers = Headers.headersOf(),
    body: RequestBody = ByteArray(0).toRequestBody(),
): Request =
    Request.Builder().url(url).headers(headers).patch(body).build()

fun PATCH(
    url: HttpUrl,
    headers: Headers = Headers.headersOf(),
    body: RequestBody = ByteArray(0).toRequestBody(),
): Request =
    Request.Builder().url(url).headers(headers).patch(body).build()

fun DELETE(url: String, headers: Headers = Headers.headersOf()): Request =
    Request.Builder().url(url).headers(headers).delete().build()

fun DELETE(url: HttpUrl, headers: Headers = Headers.headersOf()): Request =
    Request.Builder().url(url).headers(headers).delete().build()

fun DELETE(
    url: String,
    headers: Headers = Headers.headersOf(),
    body: RequestBody,
): Request =
    Request.Builder().url(url).headers(headers).delete(body).build()

fun DELETE(
    url: HttpUrl,
    headers: Headers = Headers.headersOf(),
    body: RequestBody,
): Request =
    Request.Builder().url(url).headers(headers).delete(body).build()

fun HEAD(url: String, headers: Headers = Headers.headersOf()): Request =
    Request.Builder().url(url).headers(headers).head().build()

fun HEAD(url: HttpUrl, headers: Headers = Headers.headersOf()): Request =
    Request.Builder().url(url).headers(headers).head().build()

fun PUT(
    url: String,
    headers: Headers = Headers.headersOf(),
    body: RequestBody = ByteArray(0).toRequestBody(),
    cache: CacheControl = DEFAULT_SOURCE_CACHE_CONTROL,
): Request = Request.Builder().url(url).headers(headers).cacheControl(cache).put(body).build()

fun DELETE(
    url: String,
    headers: Headers = Headers.headersOf(),
    body: RequestBody = ByteArray(0).toRequestBody(),
    cache: CacheControl = DEFAULT_SOURCE_CACHE_CONTROL,
): Request = Request.Builder().url(url).headers(headers).cacheControl(cache).delete(body).build()
