package eu.kanade.tachiyomi.network

import com.tankobun.core.network.awaitResponse

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import rx.subscriptions.Subscriptions
import uy.kohesive.injekt.TankobunInjektRegistry
import java.io.IOException

class HttpException(val code: Int) : IllegalStateException("HTTP error $code")

fun Call.asObservable(): Observable<Response> =
    Observable.create { subscriber ->
        val call = clone()
        subscriber.add(Subscriptions.create { call.cancel() })
        call.enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onError(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (subscriber.isUnsubscribed) {
                        response.close()
                        return
                    }
                    subscriber.onNext(response)
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onCompleted()
                    }
                }
            },
        )
    }

fun Call.asObservableSuccess(): Observable<Response> =
    asObservable().doOnNext { response ->
        if (!response.isSuccessful) {
            response.close()
            throw HttpException(response.code)
        }
    }

suspend fun Call.await(): Response = awaitResponse()

suspend fun Call.awaitSuccess(): Response {
    val response = await()
    if (!response.isSuccessful) {
        response.close()
        throw HttpException(response.code)
    }
    return response
}

suspend fun OkHttpClient.get(
    url: String,
    headers: Headers = Headers.headersOf(),
    cache: CacheControl = CacheControl.FORCE_NETWORK,
): Response =
    newCall(GET(url, headers, cache)).await()

suspend fun OkHttpClient.get(
    url: HttpUrl,
    headers: Headers = Headers.headersOf(),
    cache: CacheControl = CacheControl.FORCE_NETWORK,
): Response =
    newCall(GET(url, headers, cache)).await()

suspend fun OkHttpClient.post(
    url: String,
    headers: Headers = Headers.headersOf(),
    body: RequestBody = ByteArray(0).toRequestBody(),
    cache: CacheControl = CacheControl.FORCE_NETWORK,
): Response =
    newCall(POST(url, headers, body, cache)).await()

suspend fun OkHttpClient.post(
    url: HttpUrl,
    headers: Headers = Headers.headersOf(),
    body: RequestBody = ByteArray(0).toRequestBody(),
    cache: CacheControl = CacheControl.FORCE_NETWORK,
): Response =
    newCall(POST(url, headers, body, cache)).await()

inline fun <reified T> Response.parseAs(json: Json = TankobunInjektRegistry.json()): T =
    use { response ->
        json.decodeFromString(response.body?.string().orEmpty())
    }
