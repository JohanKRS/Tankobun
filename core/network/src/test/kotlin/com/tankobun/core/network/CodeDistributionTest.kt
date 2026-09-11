package com.tankobun.core.network

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.*
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.Buffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CodeDistributionTest {
    @get:Rule val folder = TemporaryFolder()
    private val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
    private val serverCertificates = HandshakeCertificates.Builder().heldCertificate(certificate).build()
    private val clientCertificates = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
    private val client = OkHttpClient.Builder().sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager).build()
    private fun server() = MockWebServer().apply { useHttps(serverCertificates.sslSocketFactory()); start() }

    @Test fun rejectsCleartextAndUrlCredentials() {
        listOf("http://example.test/a", "HTTP://example.test/a", "https://u:p@example.test/a", "file:///a", "garbage").forEach {
            assertThrows(UnsafeDistributionException::class.java) { requireDistributionUrl(it) }
        }
        assertEquals("https://example.test/a", requireDistributionUrl("HTTPS://example.test/a").toString())
    }

    @Test fun acceptsHttpsRedirectAndVerifiesBeforeFinalizing() = runBlocking {
        server().use { server ->
            server.enqueue(MockResponse.Builder().code(302).addHeader("Location", "/final").build())
            server.enqueue(MockResponse.Builder().body("valid fixture").build())
            val target = folder.root.resolve("update.apk")
            val expected = folder.newFile("expected").apply { writeText("valid fixture") }
            var validated = false
            downloadCodeFile(client, server.url("/start").toString(), target, 100, expected.length(), expected.sha256()) {
                assertFalse(target.exists()); assertEquals("valid fixture", it.readText()); validated = true
            }
            assertTrue(validated)
            assertEquals("valid fixture", target.readText())
            assertFalse(folder.root.resolve("update.apk.part").exists())
        }
    }

    @Test fun blocksDowngradeBeforeAnyHttpRequestEvenIfItWouldRedirectBack() = runBlocking {
        server().use { tls -> MockWebServer().use { plain ->
            plain.start()
            tls.enqueue(MockResponse.Builder().code(302).addHeader("Location", plain.url("/middle")).build())
            plain.enqueue(MockResponse.Builder().code(302).addHeader("Location", tls.url("/final")).build())
            val result = runCatching { downloadCodeFile(client, tls.url("/start").toString(), folder.root.resolve("bad.apk"), 100) }
            assertTrue(result.exceptionOrNull() is UnsafeDistributionException)
            assertEquals(0, plain.requestCount)
            assertTrue(folder.root.listFiles()!!.isEmpty())
        } }
    }

    @Test fun capsDeclaredAndChunkedDownloadsAndRemovesPartials() = runBlocking {
        server().use { server ->
            listOf(MockResponse.Builder().body("x".repeat(1025)).build(),
                MockResponse.Builder().chunkedBody("x".repeat(1025), 17).build()).forEach { response ->
                server.enqueue(response)
                val result = runCatching { downloadCodeFile(client, server.url("/apk").toString(), folder.root.resolve("bad.apk"), 1024) }
                assertTrue(result.exceptionOrNull() is TransferLimitException)
                assertTrue(folder.root.listFiles()!!.isEmpty())
            }
        }
    }

    @Test fun rejectsMeasuredSizeHashAndArchiveFailuresWithoutPublishing() = runBlocking {
        server().use { server ->
            for (case in 0..2) {
                server.enqueue(MockResponse.Builder().chunkedBody("bad", 1).build())
                val result = runCatching {
                    downloadCodeFile(client, server.url("/apk").toString(), folder.root.resolve("bad.apk"), 100,
                        expectedSize = if (case == 0) 4 else null, expectedSha256 = if (case == 1) "a".repeat(64) else null) {
                        if (case == 2) error("wrong package")
                    }
                }
                assertTrue(result.isFailure)
                assertTrue(folder.root.listFiles()!!.isEmpty())
            }
        }
    }

    @Test fun cancellationStopsBodyConsumptionAndCleansPartial() = runBlocking {
        server().use { server ->
            server.enqueue(MockResponse.Builder().body("x".repeat(100_000)).throttleBody(1024, 100, TimeUnit.MILLISECONDS).build())
            val target = folder.root.resolve("cancel.apk")
            val job = launch(Dispatchers.IO) { downloadCodeFile(client, server.url("/apk").toString(), target, 200_000) }
            withTimeout(3_000) { while (!folder.root.resolve("cancel.apk.part").exists()) delay(10) }
            withTimeout(2_000) { job.cancelAndJoin() }
            assertTrue(folder.root.listFiles()!!.isEmpty())
        }
    }

    @Test fun totalDeadlineStopsContinuouslyArrivingBytes() = runBlocking {
        server().use { server ->
            server.enqueue(MockResponse.Builder().body("x".repeat(100_000)).throttleBody(100, 50, TimeUnit.MILLISECONDS).build())
            val result = withTimeout(3_000) {
                runCatching { downloadCodeFile(client, server.url("/apk").toString(), folder.root.resolve("slow.apk"), 200_000, timeoutMillis = 200) }
            }
            assertTrue(result.exceptionOrNull() is IOException)
            assertTrue(folder.root.listFiles()!!.isEmpty())
        }
    }

    @Test fun transparentGzipIsLimitedAfterExpansion() = runBlocking {
        server().use { server ->
            val compressed = ByteArrayOutputStream().apply { GZIPOutputStream(this).use { it.write(ByteArray(100_000)) } }.toByteArray()
            server.enqueue(MockResponse.Builder().addHeader("Content-Encoding", "gzip").body(Buffer().write(compressed)).build())
            val request = Request.Builder().url(server.url("/index")).tag(ResponseByteLimit::class.java, ResponseByteLimit(1024)).build()
            val result = runCatching { client.codeDistributionClient().newCall(request).consumeCancellable { response, active -> response.body.readBytesLimited(1024, active) } }
            assertTrue(result.exceptionOrNull() is InputLimitExceededException)
        }
    }

    @Test fun compressedWireBytesAreLimitedToo() = runBlocking {
        server().use { server ->
            val bytes = ByteArray(4096).also { java.util.Random(42).nextBytes(it) }
            val compressed = ByteArrayOutputStream().apply { GZIPOutputStream(this).use { it.write(bytes) } }.toByteArray()
            server.enqueue(MockResponse.Builder().addHeader("Content-Encoding", "gzip").body(Buffer().write(compressed)).build())
            val request = Request.Builder().url(server.url("/index")).tag(ResponseByteLimit::class.java, ResponseByteLimit(100)).build()
            val result = runCatching { client.codeDistributionClient().newCall(request).consumeCancellable { response, _ -> response.body.bytes() } }
            assertTrue(result.exceptionOrNull() is TransferLimitException)
        }
    }
}
