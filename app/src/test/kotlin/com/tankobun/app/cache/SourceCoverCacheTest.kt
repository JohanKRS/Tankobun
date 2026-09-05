package com.tankobun.app.cache

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineStart
import okio.Path.Companion.toOkioPath
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SourceCoverCacheTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun duplicateCoverRequestsShareBytesAndSurviveReopen() = runBlocking {
        val directory = temporary.root.toOkioPath()
        val disk = AdjustableDiskCache(directory, 4096)
        val cache = SourceCoverCache(disk)
        var calls = 0
        val release = CompletableDeferred<Unit>()
        val fetch: suspend () -> ByteArray = { calls++; release.await(); byteArrayOf(1, 2, 3) }
        val first = async(start = CoroutineStart.UNDISPATCHED) { cache.load("cover", true, true, true, fetch) }
        val second = async(start = CoroutineStart.UNDISPATCHED) { cache.load("cover", true, true, true, fetch) }
        release.complete(Unit)
        assertFalse(first.await().fromCache)
        assertTrue(second.await().fromCache)
        assertEquals(1, calls)
        disk.shutdown()
        val reopened = AdjustableDiskCache(directory, 4096)
        assertTrue(SourceCoverCache(reopened).load("cover", true, true, true) { error("Unexpected network") }.fromCache)
        reopened.shutdown()
    }

    @Test fun staleCoverRefreshesAndCancellationDoesNotReturnOldBytes() = runBlocking {
        val disk = AdjustableDiskCache(temporary.root.toOkioPath(), 4096)
        val cache = SourceCoverCache(disk)
        cache.load("cover", true, true, true) { byteArrayOf(1) }
        fun expire() { val editor = disk.openEditor("cover")!!; disk.fileSystem.write(editor.metadata) { writeUtf8("1") }; editor.commit() }
        expire()
        assertTrue(cache.load("cover", true, true, true) { throw java.io.IOException("offline") }.fromCache)
        var canceled = false
        try { cache.load("cover", true, true, true) { throw CancellationException() } } catch (_: CancellationException) { canceled = true }
        assertTrue(canceled)
        assertFalse(cache.load("cover", true, true, true) { byteArrayOf(2) }.fromCache)
        assertArrayEquals(byteArrayOf(2), cache.load("cover", true, true, false) { error("network disabled") }.bytes)
        disk.shutdown()
    }
}
