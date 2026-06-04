package com.tankobun.app.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException

class ReaderLoadErrorLogicTest {
    @Test
    fun classifiesForbiddenHttpErrors() {
        val error = IllegalStateException("HTTP error 403")

        val readerError = readerLoadErrorForOnlineSource(error, "DemoSource")

        assertEquals("Source blocked the request", readerError.title)
        assertTrue(readerError.message.contains("DemoSource refused"))
    }

    @Test
    fun classifiesTimeoutCauseChain() {
        val error = IllegalStateException("Wrapper", SocketTimeoutException("timed out"))

        val readerError = readerLoadErrorForOnlineSource(error, "DemoSource")

        assertEquals("Source is not responding", readerError.title)
    }

    @Test
    fun classifiesCloudflareMessages() {
        val error = IllegalStateException("Cloudflare bypass failed")

        val readerError = readerLoadErrorForOnlineSource(error, "DemoSource")

        assertEquals("Source protection stopped the request", readerError.title)
    }
}
