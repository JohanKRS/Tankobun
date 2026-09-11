package com.tankobun.core.network

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer

class UnsafeDistributionException(message: String) : IOException(message)
class TransferLimitException(val maxBytes: Long) : IOException("Download exceeds the allowed size ($maxBytes bytes)")

object TransferLimits {
    const val INDEX_BYTES = 8 * 1024 * 1024
    const val EXPANDED_INDEX_BYTES = 32 * 1024 * 1024
    const val IMAGE_BYTES = 32 * 1024 * 1024
    const val EXTENSION_APK_BYTES = 64L * 1024 * 1024
    const val APP_APK_BYTES = 128L * 1024 * 1024
    const val METADATA_TIMEOUT_MILLIS = 60_000L
    const val DOWNLOAD_TIMEOUT_MILLIS = 10 * 60_000L
}

fun requireDistributionUrl(value: String): HttpUrl = value.toHttpUrlOrNull()
    ?.takeIf { it.isHttps && it.username.isEmpty() && it.password.isEmpty() }
    ?: throw UnsafeDistributionException("Extension and update downloads require HTTPS without URL credentials")

/** Independent from source-content clients: reject a downgrade before following it. */
fun OkHttpClient.codeDistributionClient(): OkHttpClient = newBuilder()
    .followSslRedirects(false)
    .addInterceptor { chain ->
        requireDistributionUrl(chain.request().url.toString())
        chain.proceed(chain.request()).also { response ->
            try { requireDistributionUrl(response.request.url.toString()) }
            catch (error: Exception) { response.close(); throw error }
        }
    }
    .addNetworkInterceptor { chain ->
        requireDistributionUrl(chain.request().url.toString())
        val response = chain.proceed(chain.request())
        try {
            if (response.isRedirect) response.header("Location")?.let { location ->
                val target = response.request.url.resolve(location)
                    ?: throw UnsafeDistributionException("Invalid distribution redirect")
                requireDistributionUrl(target.toString())
            }
            // This runs before transparent gzip decoding and cache writes.
            val limit = chain.request().tag(ResponseByteLimit::class.java)?.bytes ?: TransferLimits.APP_APK_BYTES
            response.newBuilder().body(response.body.limited(limit)).build()
        } catch (error: Exception) { response.close(); throw error }
    }
    .build()

data class ResponseByteLimit(val bytes: Long)

/** Network placement bounds gzip headers/compressed bytes; application placement bounds expansion. */
class ResponseSizeLimitInterceptor(private val maxBytes: Long) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        return try { response.newBuilder().body(response.body.limited(maxBytes)).build() }
        catch (error: Exception) { response.close(); throw error }
    }
}

private fun ResponseBody.limited(maxBytes: Long): ResponseBody {
    if (contentLength() > maxBytes) throw TransferLimitException(maxBytes)
    val original = this
    val limitedSource = object : ForwardingSource(source()) {
        private var total = 0L
        override fun read(sink: Buffer, byteCount: Long): Long {
            if (total > maxBytes) throw TransferLimitException(maxBytes)
            val count = super.read(sink, minOf(byteCount, maxBytes - total + 1))
            if (count > 0) {
                total += count
                if (total > maxBytes) throw TransferLimitException(maxBytes)
            }
            return count
        }
    }.buffer()
    return object : ResponseBody() {
        override fun contentType() = original.contentType()
        override fun contentLength() = original.contentLength()
        override fun source() = limitedSource
    }
}

/** Keep cancellation attached until body consumption completes, including blocking reads. */
suspend fun <T> Call.consumeCancellable(block: (Response, () -> Unit) -> T): T = withContext(Dispatchers.IO) {
    coroutineScope {
        val context = coroutineContext
        val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
            try { awaitCancellation() } finally { this@consumeCancellable.cancel() }
        }
        try {
            context.ensureActive()
            execute().use { response -> block(response) { context.ensureActive() } }
        } catch (error: Exception) {
            context.ensureActive()
            throw error
        } finally { watcher.cancel() }
    }
}

fun ResponseBody.readBytesLimited(maxBytes: Int, checkActive: () -> Unit = {}): ByteArray {
    if (contentLength() > maxBytes) throw InputLimitExceededException(maxBytes)
    return byteStream().use { it.readBytesLimited(maxBytes, checkActive) }
}

/** Closing the provider stream on cancellation unblocks Android's file/pipe reads. */
suspend fun InputStream.readBytesCancellable(maxBytes: Int, timeoutMillis: Long): ByteArray =
    kotlinx.coroutines.withTimeout(timeoutMillis) {
        withContext(Dispatchers.IO) {
            coroutineScope {
                val context = coroutineContext
                val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                    try { awaitCancellation() } finally { runCatching { this@readBytesCancellable.close() } }
                }
                try {
                    kotlinx.coroutines.runInterruptible {
                        use { it.readBytesLimited(maxBytes) { context.ensureActive() } }
                    }
                } catch (error: Exception) { context.ensureActive(); throw error }
                finally { watcher.cancel() }
            }
        }
    }

fun InputStream.copyToLimited(output: OutputStream, maxBytes: Long, checkActive: () -> Unit = {}): Long {
    require(maxBytes >= 0)
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        checkActive()
        val remaining = maxBytes - total
        val read = read(buffer, 0, minOf(buffer.size.toLong(), remaining + 1).toInt())
        if (read < 0) return total
        if (read > remaining) throw TransferLimitException(maxBytes)
        if (read == 0) {
            val byte = read()
            if (byte < 0) return total
            if (remaining == 0L) throw TransferLimitException(maxBytes)
            output.write(byte)
            total++
        } else {
            output.write(buffer, 0, read)
            total += read
        }
    }
}

fun normalizedSha256(value: String?): String? = value?.trim()?.let {
    (if (it.startsWith("sha256:", ignoreCase = true)) it.substringAfter(':').trim() else it)
        .lowercase().takeIf { hash -> hash.matches(Regex("[a-f0-9]{64}")) }
}

fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

/** Verify before publishing the final file; all failure/cancellation paths remove the partial. */
suspend fun downloadCodeFile(
    client: OkHttpClient,
    url: String,
    target: File,
    maxBytes: Long,
    expectedSize: Long? = null,
    expectedSha256: String? = null,
    timeoutMillis: Long = TransferLimits.DOWNLOAD_TIMEOUT_MILLIS,
    validate: (File) -> Unit = {},
): File = withContext(Dispatchers.IO) {
    requireDistributionUrl(url)
    require(maxBytes > 0 && (expectedSize == null || expectedSize in 1..maxBytes)) { "Invalid download size" }
    val expectedHash = expectedSha256?.let { normalizedSha256(it) ?: error("Invalid SHA-256") }
    val limit = expectedSize ?: maxBytes
    val partial = File(target.parentFile, "${target.name}.part")
    try {
        val request = Request.Builder().url(url).tag(ResponseByteLimit::class.java, ResponseByteLimit(limit)).build()
        val call = client.codeDistributionClient().newCall(request).also { it.timeout().timeout(timeoutMillis, TimeUnit.MILLISECONDS) }
        call.consumeCancellable { response, checkActive ->
            requireDistributionUrl(response.request.url.toString())
            check(response.isSuccessful) { "APK download failed: HTTP ${response.code}" }
            if (response.body.contentLength() > limit) throw TransferLimitException(limit)
            partial.outputStream().use { output -> response.body.byteStream().use { it.copyToLimited(output, limit, checkActive) } }
            checkActive()
        }
        coroutineContext.ensureActive()
        check(partial.length() > 0 && (expectedSize == null || partial.length() == expectedSize)) { "APK download failed: size mismatch" }
        check(expectedHash == null || partial.sha256() == expectedHash) { "APK download failed: SHA-256 mismatch" }
        validate(partial)
        coroutineContext.ensureActive()
        check(partial.renameTo(target)) { "APK download failed: could not finalize file" }
        target
    } finally { partial.delete() }
}
