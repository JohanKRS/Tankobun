@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.tankobun.core.extensions

import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import com.tankobun.core.network.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class ExtensionIndexRepositoryTest {
    private val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
    private val serverCertificates = HandshakeCertificates.Builder().heldCertificate(certificate).build()
    private val clientCertificates = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
    private fun tlsServer() = MockWebServer().apply { useHttps(serverCertificates.sslSocketFactory()) }

    @Test fun descriptorOnlyKeySurvivesRefreshingTheResolvedIndex() = runTest {
        tlsServer().use { server ->
            server.start()
            val saved = mutableMapOf<String, String>()
            val trust = RepositoryTrustStore(saved::get) { saved.putAll(it); true }
            val descriptor = server.url("/repo.json").toString()
            repeat(2) {
                server.enqueue(jsonResponse("""{"meta":{"signingKeyFingerprint":"$SIGNING_KEY"},"index_v2":"/list.json"}"""))
                server.enqueue(jsonResponse("""[]"""))
            }
            val first = repository().fetchIndex(descriptor)
            trust.checkAndRemember(descriptor, first)
            val fetchUrl = trust.fetchUrl(first.resolvedIndexUrl)
            assertEquals(descriptor, fetchUrl)
            val second = repository().fetchIndex(fetchUrl)
            trust.checkAndRemember(fetchUrl, second)
            assertEquals(SIGNING_KEY, second.repositorySigningKey)
            assertEquals(first.resolvedIndexUrl, second.resolvedIndexUrl)
        }
    }

    @Test fun unsafeMetadataCannotFallBackToLegacy() = runTest {
        tlsServer().use { server ->
            server.start()
            server.enqueue(jsonResponse("""{"index_v2":"http://example.test/index.pb"}"""))
            val result = runCatching { repository().fetchIndex(server.url("/index.min.json").toString()) }
            assertTrue(result.exceptionOrNull() is UnsafeDistributionException)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun rejectsInsecureIndexExternalListAndApkUrls() = runTest {
        assertTrue(runCatching { repository().fetchIndex("http://example.test/index.json") }.exceptionOrNull() is UnsafeDistributionException)
        val entry = ExtensionIndexEntry("Paper", "pkg.paper", "http://example.test/a.apk", "en", 1, "1")
        assertThrows(UnsafeDistributionException::class.java) { repository().apkUrl("https://repo.test/index.json", entry) }
        tlsServer().use { server ->
            server.start()
            server.enqueue(jsonResponse("""{"extensionListUrl":"http://example.test/list.pb"}"""))
            assertTrue(runCatching { repository().fetchIndex(server.url("/index.json").toString()) }.exceptionOrNull() is UnsafeDistributionException)
        }
    }

    @Test fun rejectsOversizedGzipBeforeParsingExpandedIndex() = runTest {
        tlsServer().use { server ->
            server.start()
            val compressed = ByteArrayOutputStream().apply {
                GZIPOutputStream(this).use { gzip ->
                    val chunk = ByteArray(8192)
                    repeat(TransferLimits.EXPANDED_INDEX_BYTES / chunk.size + 1) { gzip.write(chunk) }
                }
            }.toByteArray()
            server.enqueue(MockResponse.Builder().body(Buffer().write(compressed)).build())
            assertTrue(runCatching { repository().fetchIndex(server.url("/index.pb").toString()) }.exceptionOrNull() is InputLimitExceededException)
        }
    }

    @Test fun oversizedLegacyMetadataDoesNotFallBack() = runTest {
        tlsServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body("x".repeat(TransferLimits.INDEX_BYTES + 1)).build())
            val result = runCatching { repository().fetchIndex(server.url("/index.min.json").toString()) }
            assertTrue(result.exceptionOrNull() is TransferLimitException)
            assertEquals(1, server.requestCount)
        }
    }
    @Test fun lnReaderManifestUsesItsOwnRepositoryForStablePluginIdentity() = runTest {
        tlsServer().use { server ->
            server.start()
            server.enqueue(jsonResponse("""[{"id":"fiction","name":"Paper Observatory","site":"https://example.invalid","lang":"English","version":"1.2.3","url":"scripts/fiction.js"}]"""))
            val url = server.url("/plugins.json").toString()
            val result = repository().fetchIndex(url)
            val entry = result.entries.single()
            assertEquals(url, entry.repositoryUrl)
            assertEquals("en", entry.lang)
            assertEquals(server.url("/scripts/fiction.js").toString(), entry.apkName)
            assertEquals(entry.packageName, entry.lnReaderPlugin!!.packageName)
        }
    }
    @Test fun relativeApkUsesEntryOriginAfterAnotherRepositoryIsAdded() {
        val entry = ExtensionIndexEntry("Paper", "pkg.paper", "paper.apk", "en", 1, "1", repositoryUrl = "https://one.invalid/index.json")
        assertEquals("https://one.invalid/apk/paper.apk", repository().apkUrl("https://two.invalid/index.json", entry))
    }

    @Test
    fun legacyUrlMigratesThroughRepositoryMetadataToV2Index() = runTest {
        tlsServer().use { server ->
            server.start()
            val v2Url = server.url("/index.pb").toString()
            server.enqueue(jsonResponse("""{"index_v2":"$v2Url","meta":{}}"""))
            server.enqueue(protoResponse(v2Store()))

            val result = repository().fetchIndex(server.url("/index.min.json").toString())

            assertEquals(v2Url, result.resolvedIndexUrl)
            assertEquals(1, result.entries.size)
            assertEquals("Example", result.entries.single().name)
            assertEquals("https://cdn.example.test/example.apk", result.entries.single().apkName)
            assertEquals("https://cdn.example.test/example.png", result.entries.single().iconUrl)
            assertEquals(SIGNING_KEY, result.entries.single().repositorySigningKey)
            assertEquals(2, server.requestCount)
            assertEquals("/repo.json", server.takeRequest().url.encodedPath)
            assertEquals("/index.pb", server.takeRequest().url.encodedPath)
        }
    }

    @Test
    fun legacyListStillWorksWhenRepositoryMetadataIsUnavailable() = runTest {
        tlsServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().code(404).build())
            server.enqueue(
                jsonResponse(
                    """[{"name":"Legacy","pkg":"test.legacy","apk":"legacy.apk","lang":"en","code":7,"version":"1.4.7"}]""",
                ),
            )

            val result = repository().fetchIndex(server.url("/index.min.json").toString())

            assertEquals("Legacy", result.entries.single().name)
            assertEquals(2, server.requestCount)
        }
    }

    @Test
    fun legacyRepositorySigningKeyIsPropagatedWithoutChangingItsIndexFormat() = runTest {
        tlsServer().use { server ->
            server.start()
            server.enqueue(
                jsonResponse(
                    """{"meta":{"name":"Legacy","signingKeyFingerprint":"$SIGNING_KEY"}}""",
                ),
            )
            server.enqueue(
                jsonResponse(
                    """[{"name":"Legacy","pkg":"test.legacy","apk":"legacy.apk","lang":"en","code":7,"version":"1.4.7"}]""",
                ),
            )

            val result = repository().fetchIndex(server.url("/index.min.json").toString())

            assertEquals(SIGNING_KEY, result.entries.single().repositorySigningKey)
            assertEquals("Legacy", result.entries.single().name)
        }
    }

    @Test
    fun unavailableV2IndexFallsBackToSignedLegacyIndex() = runTest {
        tlsServer().use { server ->
            server.start()
            val v2Url = server.url("/missing-index.pb").toString()
            server.enqueue(
                jsonResponse(
                    """{"index_v2":"$v2Url","meta":{"signingKeyFingerprint":"$SIGNING_KEY"}}""",
                ),
            )
            server.enqueue(MockResponse.Builder().code(503).build())
            server.enqueue(
                jsonResponse(
                    """[{"name":"Legacy","pkg":"test.legacy","apk":"legacy.apk","lang":"en","code":7,"version":"1.4.7"}]""",
                ),
            )

            val result = repository().fetchIndex(server.url("/index.min.json").toString())

            assertEquals(SIGNING_KEY, result.entries.single().repositorySigningKey)
            assertEquals(3, server.requestCount)
        }
    }

    private fun repository(): ExtensionIndexRepository =
        ExtensionIndexRepository(
            okHttpClient = OkHttpClient.Builder().sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager).build(),
            rateLimiter = RespectfulRateLimiter(minSpacingMillis = 0L),
        )

    private fun v2Store(): ExtensionStoreV2 =
        ExtensionStoreV2(
            signingKey = SIGNING_KEY,
            extensionList = ExtensionStoreV2.ExtensionList(
                extensions = listOf(
                    ExtensionStoreV2.Extension(
                        name = "Example",
                        packageName = "test.example",
                        resources = ExtensionStoreV2.Resources(
                            apkUrl = "https://cdn.example.test/example.apk",
                            iconUrl = "https://cdn.example.test/example.png",
                        ),
                        extensionLib = "1.6",
                        versionCode = 9,
                        versionName = "1.6.9",
                        contentWarning = ExtensionStoreV2.ContentWarning.SAFE,
                        sources = listOf(
                            ExtensionStoreV2.Source(id = 1L, name = "Example", language = "en"),
                        ),
                    ),
                ),
            ),
        )

    private fun jsonResponse(body: String): MockResponse =
        MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body(body)
            .build()

    private fun protoResponse(store: ExtensionStoreV2): MockResponse =
        MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/protobuf")
            .body(Buffer().write(gzip(ProtoBuf.encodeToByteArray(store))))
            .build()

    private fun gzip(bytes: ByteArray): ByteArray =
        ByteArrayOutputStream().use { output ->
            GZIPOutputStream(output).use { gzip -> gzip.write(bytes) }
            output.toByteArray()
        }

    private companion object {
        const val SIGNING_KEY = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
}
