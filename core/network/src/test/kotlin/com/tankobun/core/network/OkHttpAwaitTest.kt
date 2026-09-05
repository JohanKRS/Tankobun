package com.tankobun.core.network

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Timeout
import okio.buffer
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OkHttpAwaitTest {
    @Test
    fun closesAResponseCancelledBeforeTheCallerResumes() = runTest {
        val call = ControlledCall()
        val result = async(start = CoroutineStart.UNDISPATCHED) { call.awaitResponse().use { it.body.string() } }
        val body = TrackingBody()
        call.respond(body)
        result.cancel()
        runCurrent()
        assertTrue(body.closed)
        assertTrue(call.cancelled)
    }

    @Test
    fun closesALateResponseAfterTransportCancellation() = runTest {
        val call = ControlledCall()
        val result = async(start = CoroutineStart.UNDISPATCHED) { call.awaitResponse() }
        result.cancel()
        val body = TrackingBody()
        call.respond(body)
        runCurrent()
        assertTrue(body.closed)
        assertTrue(call.cancelled)
    }

    @Test
    fun leavesAReceivedResponseOpenForTheCaller() = runTest {
        val call = ControlledCall()
        val result = async(start = CoroutineStart.UNDISPATCHED) { call.awaitResponse() }
        val body = TrackingBody()
        call.respond(body)
        runCurrent()
        assertFalse(body.closed)
        result.await().use { assertEquals("abc", it.body.string()) }
        assertTrue(body.closed)
    }

    private class TrackingBody : ResponseBody() {
        var closed = false
        private val content = object : ForwardingSource(Buffer().writeUtf8("abc")) {
            override fun close() { closed = true; super.close() }
        }.buffer()
        override fun contentType(): MediaType? = null
        override fun contentLength(): Long = 3
        override fun source(): BufferedSource = content
    }

    private class ControlledCall : Call by OkHttpClient().newCall(Request.Builder().url("https://example.test/").build()) {
        private lateinit var callback: Callback
        var cancelled = false
        override fun request(): Request = Request.Builder().url("https://example.test/").build()
        override fun execute(): Response = error("Unused synchronous path")
        override fun enqueue(responseCallback: Callback) { callback = responseCallback }
        override fun cancel() { cancelled = true }
        override fun isExecuted(): Boolean = ::callback.isInitialized
        override fun isCanceled(): Boolean = cancelled
        override fun timeout(): Timeout = Timeout.NONE
        override fun clone(): Call = ControlledCall()
        fun respond(body: ResponseBody) {
            callback.onResponse(this, Response.Builder().request(request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(body).build())
        }
    }
}
