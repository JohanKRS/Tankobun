package com.tankobun.core.extensions

import com.tankobun.core.network.InputLimitExceededException
import com.tankobun.core.network.TransferLimitException
import com.tankobun.core.network.TransferLimits
import com.tankobun.core.network.readBytesLimited
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.asObservable
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class SourceResponseLimitsTest {
    @Test fun sourceClientCapsGzipFilenameBeforeImageOrChallengeDecoding() {
        MockWebServer().use { server ->
            server.start()
            val gzip = ByteArrayOutputStream().apply { GZIPOutputStream(this).use { it.write("tiny image".toByteArray()) } }.toByteArray()
            val filenameBomb = Buffer().write(gzip, 0, 10)
            // Set the FNAME flag and insert a filename larger than the raw limit.
            val header = filenameBomb.readByteArray().also { it[3] = 8 }
            val payload = Buffer().write(header)
            val chunk = ByteArray(8192) { 'x'.code.toByte() }
            repeat(TransferLimits.IMAGE_BYTES / chunk.size + 1) { payload.write(chunk) }
            payload.writeByte(0).write(gzip, 10, gzip.size - 10)
            for (code in listOf(200, 503)) {
                server.enqueue(MockResponse.Builder().code(code).addHeader("Server", "cloudflare")
                    .addHeader("Content-Encoding", "gzip").chunkedBody(payload.clone(), 8192).build())
                val result = runCatching {
                    NetworkHelper().client.newBuilder().cookieJar(okhttp3.CookieJar.NO_COOKIES).build()
                        .newCall(Request.Builder().url(server.url("/image")).build()).execute().use {
                        it.body.readBytesLimited(TransferLimits.IMAGE_BYTES)
                    }
                }
                assertTrue(result.exceptionOrNull().toString(), result.exceptionOrNull() is TransferLimitException)
            }
        }
    }

    @Test fun imageDeadlineCancelsTransportBeforeDelayedHeadersArrive() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body("image").headersDelay(2, TimeUnit.SECONDS).build())
            val cancelled = AtomicBoolean()
            val client = OkHttpClient.Builder().eventListener(object : EventListener() {
                override fun canceled(call: Call) { cancelled.set(true) }
            }).build()
            val result = runCatching {
                client.newCall(Request.Builder().url(server.url("/image")).build()).asObservable()
                    .map { it.use { response -> response.body.bytes() } }.withImageDeadline(100).awaitSourceValue()
            }
            assertTrue(result.exceptionOrNull() is TimeoutException)
            assertTrue(cancelled.get())
        }
    }

    @Test fun oversizedImagesAreTerminalInsteadOfRepeatedDownloads() {
        assertFalse(InputLimitExceededException(100).isTransientSourceImageFailure())
        assertFalse(RuntimeException(TransferLimitException(100)).isTransientSourceImageFailure())
        assertFalse(TimeoutException().isTransientSourceImageFailure())
        assertTrue(java.io.IOException("ordinary connection interruption").isTransientSourceImageFailure())
    }
}
