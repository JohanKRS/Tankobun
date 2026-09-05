package com.tankobun.app.cache

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Single owner of reader cache files. Call on an IO dispatcher, never on the UI thread. */
internal class PageDiskStore(
    private val root: File,
    private val maxBytes: () -> Long,
    private val minimumFreeBytes: Long = 256 * MIB,
    private val usableSpace: () -> Long = { root.usableSpace },
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private data class Entry(val file: File, val size: Long, var accessedAt: Long)
    private val lock = Any()
    private var entries: MutableMap<String, Entry>? = null
    private var generation = 0L

    fun generation(): Long = synchronized(lock) { generation }

    fun read(key: String, allowStale: Boolean = false): ByteArray? = synchronized(lock) {
        val entry = index()[key] ?: return null
        val age = nowMillis() - entry.file.lastModified()
        if (!allowStale && (age < 0 || age >= PAGE_CACHE_FRESH_MILLIS)) return null
        try {
            entry.file.readBytes().takeIf { it.isNotEmpty() }?.also {
                entry.accessedAt = nowMillis()
                // Keep the file's write time for freshness. Directory access times
                // retain chapter recency across process restarts without per-read journals.
                entry.file.parentFile?.setLastModified(entry.accessedAt)
            }
        } catch (_: IOException) {
            index().remove(key)
            null
        }
    }

    fun write(key: String, bytes: ByteArray, expectedGeneration: Long): String? = synchronized(lock) {
        if (expectedGeneration != generation || bytes.isEmpty() || bytes.size > maxBytes()) return null
        root.mkdirs()
        val target = File(root, key)
        require(target.canonicalPath.startsWith(root.canonicalPath + File.separator))
        val oldSize = index()[key]?.size ?: 0L
        val targetBytes = (maxBytes() - bytes.size + oldSize).coerceAtLeast(0)
        trimLocked(targetBytes, key, bytes.size.toLong())
        if (sizeBytes() > targetBytes || usableSpace() < minimumFreeBytes + bytes.size) return null
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, target.name + ".part")
        try {
            partial.writeBytes(bytes)
            Files.move(partial.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            val now = nowMillis()
            target.setLastModified(now)
            target.parentFile?.setLastModified(now)
            index()[key] = Entry(target, bytes.size.toLong(), now)
            target.absolutePath
        } finally {
            partial.delete()
        }
    }

    fun trim() = synchronized(lock) { trimLocked(maxBytes(), null, 0) }

    fun sizeBytes(): Long = synchronized(lock) { index().values.sumOf { it.size } }

    fun clear() = synchronized(lock) {
        generation++
        root.deleteRecursively()
        root.mkdirs()
        entries = null
    }

    private fun trimLocked(targetBytes: Long, keepKey: String?, incomingBytes: Long) {
        val files = index()
        var size = files.values.sumOf { it.size }
        if (size <= targetBytes && usableSpace() >= minimumFreeBytes + incomingBytes) return
        for ((key, entry) in files.entries.sortedBy { it.value.accessedAt }) {
            if (size <= targetBytes && usableSpace() >= minimumFreeBytes + incomingBytes) break
            if (key == keepKey) continue
            if (entry.file.delete() || !entry.file.exists()) {
                files.remove(key)
                size -= entry.size
                removeEmptyParents(entry.file.parentFile)
            }
        }
    }

    private fun removeEmptyParents(start: File?) {
        var directory = start
        while (directory != null && directory != root && directory.listFiles()?.isEmpty() == true) {
            directory.delete()
            directory = directory.parentFile
        }
    }

    private fun index(): MutableMap<String, Entry> {
        entries?.let { return it }
        root.mkdirs()
        return root.walkTopDown().filter { it.isFile }.mapNotNull { file ->
            if (file.name.endsWith(".part") || file.length() == 0L) {
                file.delete()
                null
            } else {
                file.relativeTo(root).path to Entry(file, file.length(), maxOf(file.lastModified(), file.parentFile?.lastModified() ?: 0L))
            }
        }.toMap(mutableMapOf()).also { entries = it }
    }
}
