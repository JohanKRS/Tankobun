package eu.kanade.tachiyomi.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkHelperTest {
    @Test
    fun keepsSupportedAcceptEncodingValues() {
        assertNull(supportedAcceptEncoding(null))
        assertEquals("gzip", supportedAcceptEncoding("gzip"))
        assertEquals("identity", supportedAcceptEncoding("identity"))
    }

    @Test
    fun stripsEncodingsOkHttpDoesNotDecode() {
        assertNull(supportedAcceptEncoding("gzip, deflate, br"))
        assertNull(supportedAcceptEncoding("gzip, zstd"))
    }

    @Test
    fun replacesNonBrowserDefaultUserAgents() {
        assertTrue(shouldReplaceHttpAgent(null))
        assertTrue(shouldReplaceHttpAgent(""))
        assertTrue(shouldReplaceHttpAgent("Tankobun Android"))
        assertTrue(shouldReplaceHttpAgent("Dalvik/2.1.0 (Linux; U; Android 16)"))
    }

    @Test
    fun keepsBrowserLikeDefaultUserAgent() {
        assertFalse(
            shouldReplaceHttpAgent(
                "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/125.0.0.0 Mobile Safari/537.36",
            ),
        )
    }
}
