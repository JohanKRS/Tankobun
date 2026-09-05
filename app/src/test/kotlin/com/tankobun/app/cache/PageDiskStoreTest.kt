package com.tankobun.app.cache

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PageDiskStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private var clock = 1_000_000L
    private var budget = 20L
    private var free = 1000L
    private fun store() = PageDiskStore(temporary.root, { budget }, 10, { free }, { clock })

    @Test fun evictsLeastRecentlyUsedPagesAcrossDifferentManga() {
        val store = store()
        store.write("one/chapter/a", ByteArray(10) { 1 }, 0)
        clock += 1000
        store.write("two/chapter/b", ByteArray(10) { 2 }, 0)
        clock += 1000
        assertNotNull(store.read("one/chapter/a"))
        store.write("three/chapter/c", ByteArray(10) { 3 }, 0)
        assertNull(store.read("two/chapter/b"))
        assertNotNull(store.read("one/chapter/a"))
        assertEquals(20L, store.sizeBytes())
    }

    @Test fun loweringBudgetTrimsExistingDataWithoutDeletingDownloadsElsewhere() {
        val downloads = Files.createTempDirectory("reader-download-test").toFile()
        try {
            val kept = downloads.resolve("page").apply { writeText("download") }
            val store = store()
            store.write("a", ByteArray(10), 0)
            store.write("b", ByteArray(10), 0)
            budget = 10
            store.trim()
            assertEquals(10L, store.sizeBytes())
            assertTrue(kept.exists())
        } finally { downloads.deleteRecursively() }
    }

    @Test fun clearingInvalidatesWritesAlreadyInFlight() {
        val store = store()
        val generation = store.generation()
        store.clear()
        assertNull(store.write("a", ByteArray(10), generation))
        assertEquals(0L, store.sizeBytes())
        assertNotNull(store.write("b", ByteArray(10), store.generation()))
    }

    @Test fun lowSpaceAndOversizedPagesDoNotConsumeTheReserve() {
        val store = store()
        assertNull(store.write("huge", ByteArray(21), 0))
        free = 15
        assertNull(store.write("a", ByteArray(10), 0))
        assertEquals(0L, store.sizeBytes())
    }

    @Test fun readsDoNotExtendFreshnessButOldBytesRemainAvailableOffline() {
        val store = store()
        store.write("a", byteArrayOf(3), 0)
        clock += PAGE_CACHE_FRESH_MILLIS - 1
        assertNotNull(store.read("a"))
        clock += 1
        assertNull(store.read("a"))
        assertArrayEquals(byteArrayOf(3), store.read("a", allowStale = true))
    }

    @Test fun reopeningKeepsBytesAndBudgetAndExternalEvictionIsACacheMiss() {
        store().write("a", byteArrayOf(4), 0)
        val reopened = store()
        assertArrayEquals(byteArrayOf(4), reopened.read("a"))
        temporary.root.resolve("a").delete()
        assertNull(reopened.read("a"))
        assertEquals(0L, reopened.sizeBytes())
    }
}
