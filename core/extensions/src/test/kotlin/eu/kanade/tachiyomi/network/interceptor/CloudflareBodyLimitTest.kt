package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.Assert.*
import org.junit.Test

class CloudflareBodyLimitTest {
    private fun response(body: ResponseBody) = Response.Builder().request(Request.Builder().url("https://site.test").build())
        .protocol(Protocol.HTTP_1_1).code(403).message("Forbidden").header("Server", "cloudflare").body(body).build()

    @Test fun recognizesNormalChallengeWithinPrefix() {
        response("<html><h1 id=challenge-error-title>Challenge</h1></html>".toResponseBody()).use {
            assertTrue(with(CloudflareInterceptor) { it.isCloudflareChallenge() })
        }
    }

    @Test fun challengeDetectionNeverConsumesAnUnboundedBody() {
        var consumed = 0L
        val source = object : Source {
            override fun timeout() = Timeout.NONE
            override fun close() {}
            override fun read(sink: Buffer, byteCount: Long): Long {
                val count = minOf(8192L, byteCount)
                sink.write(ByteArray(count.toInt()) { 'x'.code.toByte() }); consumed += count; return count
            }
        }.buffer()
        val body = object : ResponseBody() {
            override fun contentType() = null
            override fun contentLength() = -1L
            override fun source() = source
        }
        response(body).use { assertFalse(with(CloudflareInterceptor) { it.isCloudflareChallenge() }) }
        assertTrue(consumed <= 64L * 1024 + 8192)
    }
}
