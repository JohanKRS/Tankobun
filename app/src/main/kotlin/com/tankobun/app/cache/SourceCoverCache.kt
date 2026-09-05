package com.tankobun.app.cache

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Source fetchers return bytes rather than HTTP responses, so store their covers explicitly. */
internal class SourceCoverCache(private val disk: AdjustableDiskCache) {
    private class Lock(val mutex: Mutex = Mutex(), val users: AtomicInteger = AtomicInteger())
    private val locks = ConcurrentHashMap<String, Lock>()
    data class Result(val bytes: ByteArray, val fromCache: Boolean)

    suspend fun load(
        key: String,
        readEnabled: Boolean,
        writeEnabled: Boolean,
        networkEnabled: Boolean,
        fetch: suspend () -> ByteArray,
    ): Result {
        val entry = locks.compute(key) { _, previous -> (previous ?: Lock()).also { it.users.incrementAndGet() } }!!
        try {
            return entry.mutex.withLock {
                if (readEnabled) cached(key, allowStale = !networkEnabled)?.let { return@withLock Result(it, true) }
                if (!networkEnabled) throw IOException("Cover is not cached")
                val generation = disk.generation()
                val bytes = try { fetch() } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    if (readEnabled) cached(key, allowStale = true)?.let { return@withLock Result(it, true) }
                    throw error
                }
                if (writeEnabled && bytes.isNotEmpty() && bytes.size <= disk.maxSize) {
                    try {
                        disk.openEditor(key, generation)?.let { editor ->
                            try {
                                disk.fileSystem.write(editor.data) { write(bytes) }
                                disk.fileSystem.write(editor.metadata) { writeUtf8(System.currentTimeMillis().toString()) }
                                editor.commit()
                            } finally { editor.abort() }
                        }
                    } catch (_: IOException) { /* A cache write must not prevent displaying a cover. */ }
                }
                Result(bytes, false)
            }
        } finally {
            locks.computeIfPresent(key) { _, current -> if (current.users.decrementAndGet() == 0) null else current }
        }
    }

    private fun cached(key: String, allowStale: Boolean): ByteArray? = try {
        disk.openSnapshot(key)?.use { snapshot ->
            val timestamp = disk.fileSystem.read(snapshot.metadata) { readUtf8().toLongOrNull() }
            val age = timestamp?.let { System.currentTimeMillis() - it }
            if (!allowStale && (age == null || age < 0 || age >= PAGE_CACHE_FRESH_MILLIS)) null
            else disk.fileSystem.read(snapshot.data) { readByteArray() }.takeIf { it.isNotEmpty() }
        }
    } catch (_: IOException) { null }
}
