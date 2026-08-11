@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.tankobun.core.extensions

import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class ExtensionIndexRepositoryTest {
    @Test
    fun legacyUrlMigratesThroughRepositoryMetadataToV2Index() = runTest {
        MockWebServer().use { server ->
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
        MockWebServer().use { server ->
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
        MockWebServer().use { server ->
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
        MockWebServer().use { server ->
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
            okHttpClient = OkHttpClient(),
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
