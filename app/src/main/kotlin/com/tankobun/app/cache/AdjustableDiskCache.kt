package com.tankobun.app.cache

import coil3.disk.DiskCache
import okio.FileSystem
import okio.Path
import java.util.concurrent.atomic.AtomicBoolean

/** Reopens Coil's cache with a new budget only after every reader/editor releases it. */
internal class AdjustableDiskCache(
    override val directory: Path,
    initialMaxSize: Long,
    private val factory: (Long) -> DiskCache = { size -> DiskCache.Builder().directory(directory).maxSizeBytes(size).build() },
) : DiskCache {
    private val lock = Any()
    private var delegate = factory(initialMaxSize)
    private var desiredMaxSize = initialMaxSize
    private var leases = 0
    private var maintenanceRequested = false
    private var closed = false
    private var generation = 0L
    fun generation(): Long = synchronized(lock) { generation }
    fun openEditor(key: String, expectedGeneration: Long): DiskCache.Editor? = synchronized(lock) {
        if (expectedGeneration != generation) null else openEditor(key)
    }
    override val fileSystem: FileSystem get() = delegate.fileSystem
    override val maxSize: Long get() = synchronized(lock) { desiredMaxSize }
    override val size: Long get() = synchronized(lock) { if (closed) 0L else delegate.size }

    fun resize(bytes: Long) = synchronized(lock) {
        require(bytes > 0)
        if (!closed) {
            desiredMaxSize = bytes
            maintenanceRequested = true
            reopenWhenIdle()
        }
    }

    override fun openSnapshot(key: String): DiskCache.Snapshot? = synchronized(lock) {
        if (closed) return null
        delegate.openSnapshot(key)?.let { leases++; Snapshot(it) }
    }

    override fun openEditor(key: String): DiskCache.Editor? = synchronized(lock) {
        if (closed) return null
        delegate.openEditor(key)?.let { leases++; Editor(it) }
    }

    override fun remove(key: String): Boolean = synchronized(lock) { !closed && delegate.remove(key) }
    override fun clear() = synchronized(lock) {
        generation++
        if (!closed) delegate.clear()
    }
    override fun shutdown() = synchronized(lock) {
        closed = true
        if (leases == 0) delegate.shutdown()
    }

    private fun release() = synchronized(lock) {
        check(leases > 0)
        leases--
        if (closed && leases == 0) delegate.shutdown() else reopenWhenIdle()
    }

    private fun reopenWhenIdle() {
        if (closed || leases != 0 || !maintenanceRequested) return
        if (delegate.maxSize != desiredMaxSize) {
            delegate.shutdown()
            delegate = factory(desiredMaxSize)
        }
        // Coil trims on writes or shutdown, not when reopening a smaller cache.
        // Initialize and close that owner once to perform its normal LRU trim.
        if (delegate.size > desiredMaxSize) {
            delegate.shutdown()
            delegate = factory(desiredMaxSize)
        }
        maintenanceRequested = false
    }

    private inner class Snapshot(private val snapshot: DiskCache.Snapshot) : DiskCache.Snapshot {
        private val finished = AtomicBoolean()
        override val data: Path get() = snapshot.data
        override val metadata: Path get() = snapshot.metadata
        override fun close() {
            if (finished.compareAndSet(false, true)) try { snapshot.close() } finally { release() }
        }
        override fun closeAndOpenEditor(): DiskCache.Editor? {
            if (!finished.compareAndSet(false, true)) return null
            val editor = try { snapshot.closeAndOpenEditor() } catch (error: Throwable) { release(); throw error }
            if (editor == null) release()
            return editor?.let(::Editor)
        }
    }

    private inner class Editor(private val editor: DiskCache.Editor) : DiskCache.Editor {
        private val finished = AtomicBoolean()
        override val data: Path get() = editor.data
        override val metadata: Path get() = editor.metadata
        override fun commit() {
            if (finished.compareAndSet(false, true)) try { editor.commit() } finally { release() }
        }
        override fun abort() {
            if (finished.compareAndSet(false, true)) try { editor.abort() } finally { release() }
        }
        override fun commitAndOpenSnapshot(): DiskCache.Snapshot? {
            if (!finished.compareAndSet(false, true)) return null
            val snapshot = try { editor.commitAndOpenSnapshot() } catch (error: Throwable) { release(); throw error }
            if (snapshot == null) release()
            return snapshot?.let(::Snapshot)
        }
    }
}
