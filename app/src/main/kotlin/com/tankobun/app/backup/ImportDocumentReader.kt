package com.tankobun.app.backup

import android.content.ContentResolver
import android.net.Uri
import android.os.CancellationSignal
import com.tankobun.core.network.readBytesLimited
import com.tankobun.core.network.InputLimitExceededException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference

internal const val MAX_BACKUP_BYTES = 32 * 1024 * 1024
internal const val MAX_RECOMMENDATION_BYTES = 4 * 1024 * 1024
internal class ImportReadTimeoutException : IOException("The file provider took too long to respond")
internal class ImportDocumentReadException(cause: Throwable? = null) : IOException("Could not read import document", cause)

internal suspend fun ContentResolver.readImportBytes(uri: Uri, maxBytes: Int): ByteArray {
    if (uri.scheme != ContentResolver.SCHEME_CONTENT && uri.scheme != ContentResolver.SCHEME_FILE) {
        throw ImportDocumentReadException()
    }
    return try {
        withTimeout(30_000L) {
            coroutineScope {
                val signal = CancellationSignal()
                val openedStream = AtomicReference<InputStream?>()
                val reader = async(Dispatchers.IO) {
                    openAssetFileDescriptor(uri, "r", signal).use { descriptor ->
                        if (descriptor == null) throw ImportDocumentReadException()
                        descriptor.createInputStream().use { input ->
                            openedStream.set(input)
                            val context = currentCoroutineContext()
                            context.ensureActive()
                            input.readBytesLimited(maxBytes) { context.ensureActive() }
                        }
                    }
                }
                try {
                    reader.await()
                } finally {
                    signal.cancel()
                    runCatching { openedStream.getAndSet(null)?.close() }
                }
            }
        }
    } catch (error: TimeoutCancellationException) {
        if (!currentCoroutineContext().isActive) throw error
        throw ImportReadTimeoutException()
    } catch (error: InputLimitExceededException) {
        throw error
    } catch (error: IOException) {
        throw ImportDocumentReadException(error)
    } catch (error: SecurityException) {
        throw ImportDocumentReadException(error)
    }
}
