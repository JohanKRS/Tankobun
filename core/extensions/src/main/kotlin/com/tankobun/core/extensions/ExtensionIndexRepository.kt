@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.tankobun.core.extensions

import com.tankobun.core.network.RespectfulRateLimiter
import com.tankobun.core.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.zip.GZIPInputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class ExtensionIndexRepository(
    private val okHttpClient: OkHttpClient,
    private val rateLimiter: RespectfulRateLimiter,
) {
    private val distributionClient = okHttpClient.codeDistributionClient()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun fetchIndex(indexUrl: String): ExtensionIndexResult {
        val requestedUrl = requireDistributionUrl(indexUrl.trim()).toString()

        legacyRepositoryMetadataUrl(requestedUrl)?.let { metadataUrl ->
            val descriptor = try {
                decodeRepositoryDescriptor(fetchBytes(metadataUrl))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                error.rethrowDistributionFailure()
                null
            }
            if (descriptor != null) {
                descriptor.indexV2?.takeIf { it.isNotBlank() }?.let { v2Url ->
                    try {
                        return decodeIndex(resolveUrl(metadataUrl, v2Url), linkedSetOf(), descriptor.meta?.signingKeyFingerprint)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        error.rethrowDistributionFailure()
                        // Keep legacy repositories usable while a v2 endpoint is unavailable.
                    }
                }
                return decodeIndex(
                    indexUrl = requestedUrl,
                    visitedUrls = linkedSetOf(),
                    repositorySigningKey = descriptor.meta?.signingKeyFingerprint,
                )
            }
        }

        return decodeIndex(requestedUrl, linkedSetOf())
    }

    fun apkUrl(indexUrl: String, entry: ExtensionIndexEntry): String {
        if (entry.apkName.startsWith("https://", ignoreCase = true) || entry.apkName.startsWith("http://", ignoreCase = true)) {
            return requireDistributionUrl(entry.apkName).toString()
        }
        val uri = URI(entry.repositoryUrl.ifBlank { indexUrl })
        val indexPath = uri.path.substringBeforeLast('/', "")
        val apkPath = "$indexPath/apk/${entry.apkName}".replace("//", "/")
        return requireDistributionUrl(URI(uri.scheme, uri.authority, apkPath, null, null).toString()).toString()
    }

    fun iconUrl(indexUrl: String, entry: ExtensionIndexEntry): String {
        entry.iconUrl?.takeIf { it.isRemoteUrl() }?.let { return it }
        val uri = URI(entry.repositoryUrl.ifBlank { indexUrl })
        val indexPath = uri.path.substringBeforeLast('/', "")
        val iconPath = "$indexPath/icon/${entry.packageName}.png".replace("//", "/")
        return URI(uri.scheme, uri.authority, iconPath, null, null).toString()
    }

    private suspend fun decodeIndex(
        indexUrl: String,
        visitedUrls: MutableSet<String>,
        repositorySigningKey: String? = null,
    ): ExtensionIndexResult {
        check(visitedUrls.add(indexUrl)) { "Extension repository redirect loop" }
        check(visitedUrls.size <= MAX_REDIRECT_DEPTH) { "Too many extension repository redirects" }
        val context = currentCoroutineContext()
        val payload = fetchBytes(indexUrl).decompressIfGzipped { context.ensureActive() }

        return when (payload.firstMeaningfulByte()) {
            JSON_ARRAY_START -> ExtensionIndexResult(
                entries = (com.tankobun.core.extensions.novel.parseLnReaderIndex(payload.decodeToString(), indexUrl)
                    ?: json.decodeFromString<List<ExtensionIndexEntry>>(payload.decodeToString())).map { entry ->
                    entry.copy(repositorySigningKey = repositorySigningKey?.takeIf { it.isNotBlank() }, repositoryUrl = indexUrl)
                },
                resolvedIndexUrl = indexUrl,
                repositorySigningKey = repositorySigningKey,
            )
            JSON_OBJECT_START -> {
                val descriptor = decodeRepositoryDescriptor(payload)
                val v2Url = descriptor.indexV2?.takeIf { it.isNotBlank() }
                if (v2Url != null) {
                    decodeIndex(resolveUrl(indexUrl, v2Url), visitedUrls, mergeSigningKeys(repositorySigningKey, descriptor.meta?.signingKeyFingerprint))
                } else {
                    decodeV2Store(indexUrl, json.decodeFromString<ExtensionStoreV2>(payload.decodeToString()), repositorySigningKey)
                }
            }
            else -> decodeV2Store(indexUrl, ProtoBuf.decodeFromByteArray<ExtensionStoreV2>(payload), repositorySigningKey)
        }
    }

    private suspend fun decodeV2Store(
        indexUrl: String,
        store: ExtensionStoreV2,
        inheritedSigningKey: String? = null,
    ): ExtensionIndexResult {
        val extensionList = store.extensionList ?: store.extensionListUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { listUrl -> decodeV2ExtensionList(resolveUrl(indexUrl, listUrl)) }
            ?: error("Extension repository does not contain an extension list")
        val signingKey = mergeSigningKeys(inheritedSigningKey, store.signingKey)
        return ExtensionIndexResult(
            entries = extensionList.toIndexEntries(signingKey).map { it.copy(repositoryUrl = indexUrl) },
            resolvedIndexUrl = indexUrl,
            repositorySigningKey = signingKey,
        )
    }

    private suspend fun decodeV2ExtensionList(listUrl: String): ExtensionStoreV2.ExtensionList {
        val context = currentCoroutineContext()
        val payload = fetchBytes(listUrl).decompressIfGzipped { context.ensureActive() }
        return if (payload.firstMeaningfulByte() == JSON_OBJECT_START) {
            json.decodeFromString(payload.decodeToString())
        } else {
            ProtoBuf.decodeFromByteArray(payload)
        }
    }

    private suspend fun fetchBytes(url: String): ByteArray = rateLimiter.run {
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(requireDistributionUrl(url))
                .tag(ResponseByteLimit::class.java, ResponseByteLimit(TransferLimits.INDEX_BYTES.toLong())).build()
            val call = distributionClient.newCall(request).also { it.timeout().timeout(TransferLimits.METADATA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS) }
            call.consumeCancellable { response, checkActive ->
                rateLimiter.recordResponse(response.headers, response.code)
                if (!response.isSuccessful) {
                    error("Failed to fetch extension index: HTTP ${response.code}")
                }
                response.body.readBytesLimited(TransferLimits.INDEX_BYTES, checkActive)
            }
        }
    }

    private fun decodeRepositoryDescriptor(payload: ByteArray): ExtensionRepositoryDescriptor =
        json.decodeFromString(payload.decodeToString())

    private fun legacyRepositoryMetadataUrl(indexUrl: String): String? {
        val uri = URI(indexUrl)
        if (uri.path.substringAfterLast('/') != LEGACY_INDEX_FILE_NAME) return null
        val parentPath = uri.path.substringBeforeLast('/', "")
        return URI(uri.scheme, uri.authority, "$parentPath/$REPOSITORY_METADATA_FILE_NAME", null, null).toString()
    }

    private fun resolveUrl(baseUrl: String, candidate: String): String =
        requireDistributionUrl(requireDistributionUrl(baseUrl).resolve(candidate)?.toString().orEmpty()).toString()

    private fun ByteArray.firstMeaningfulByte(): Byte? =
        firstOrNull { byte -> !byte.toInt().toChar().isWhitespace() }

    private fun ByteArray.decompressIfGzipped(checkActive: () -> Unit): ByteArray {
        val isGzip = size >= 2 &&
            (this[0].toInt() and 0xFF) == GZIP_MAGIC_BYTE_1 &&
            (this[1].toInt() and 0xFF) == GZIP_MAGIC_BYTE_2
        if (!isGzip) return this
        return GZIPInputStream(ByteArrayInputStream(this)).use { input -> input.readBytesLimited(TransferLimits.EXPANDED_INDEX_BYTES, checkActive) }
    }

    private fun String.isRemoteUrl(): Boolean =
        startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)

    private companion object {
        const val LEGACY_INDEX_FILE_NAME = "index.min.json"
        const val REPOSITORY_METADATA_FILE_NAME = "repo.json"
        const val MAX_REDIRECT_DEPTH = 4
        const val JSON_ARRAY_START: Byte = 0x5B
        const val JSON_OBJECT_START: Byte = 0x7B
        const val GZIP_MAGIC_BYTE_1 = 0x1F
        const val GZIP_MAGIC_BYTE_2 = 0x8B
    }
}

private fun Exception.rethrowDistributionFailure() {
    if (this is UnsafeDistributionException || this is TransferLimitException || this is InputLimitExceededException) throw this
}

private fun mergeSigningKeys(first: String?, second: String?): String? {
    val a = first?.takeIf { it.isNotBlank() }?.let(::repositoryIdentity)
    val b = second?.takeIf { it.isNotBlank() }?.let(::repositoryIdentity)
    if (a != null && b != null && a != b) throw UnsafeDistributionException("Conflicting repository signing identities")
    return b ?: a
}
