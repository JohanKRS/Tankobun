package com.tankobun.app.cache

import coil3.disk.DiskCache
import okio.Path.Companion.toOkioPath
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AdjustableDiskCacheTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun resizeWaitsForEditorAndSnapshotAndKeepsExistingImages() {
        val created = mutableListOf<Long>()
        val directory = temporary.root.toOkioPath()
        val cache = AdjustableDiskCache(directory, 1024) { size ->
            created += size
            DiskCache.Builder().directory(directory).maxSizeBytes(size).build()
        }
        val editor = cache.openEditor("cover")!!
        cache.fileSystem.write(editor.data) { writeUtf8("original cover") }
        cache.fileSystem.write(editor.metadata) { writeUtf8("metadata") }
        cache.resize(2048)
        assertEquals(listOf(1024L), created)
        val snapshot = editor.commitAndOpenSnapshot()!!
        cache.resize(4096)
        assertEquals(listOf(1024L), created)
        assertEquals("original cover", cache.fileSystem.read(snapshot.data) { readUtf8() })
        snapshot.close()
        snapshot.close()
        assertEquals(listOf(1024L, 4096L), created)
        cache.openSnapshot("cover")!!.use { assertEquals("original cover", cache.fileSystem.read(it.data) { readUtf8() }) }
        cache.shutdown()
    }

    @Test fun snapshotToEditorTransfersLeaseAndClearRejectsOldWrites() {
        val cache = AdjustableDiskCache(temporary.root.toOkioPath(), 1024)
        val editor = cache.openEditor("cover")!!
        cache.fileSystem.write(editor.data) { writeUtf8("original") }
        cache.fileSystem.write(editor.metadata) { writeUtf8("metadata") }
        val snapshot = editor.commitAndOpenSnapshot()!!
        val rewrite = snapshot.closeAndOpenEditor()!!
        cache.resize(2048)
        rewrite.abort()
        assertNotNull(cache.openSnapshot("cover")?.also { it.close() })
        val before = cache.generation()
        cache.clear()
        assertNull(cache.openEditor("cover", before))
        assertNull(cache.openSnapshot("cover"))
        cache.shutdown()
    }

    @Test fun shrinkingEvictsLeastUsedImagesWithoutClearingEverything() {
        val cache = AdjustableDiskCache(temporary.root.toOkioPath(), 1024)
        for (key in listOf("one", "two", "three")) {
            val editor = cache.openEditor(key)!!
            cache.fileSystem.write(editor.data) { write(ByteArray(200)) }
            cache.fileSystem.write(editor.metadata) { writeUtf8("1") }
            editor.commit()
        }
        cache.openSnapshot("one")!!.close()
        cache.resize(450)
        // Journal maintenance runs on Coil's cleanup dispatcher.
        for (attempt in 0..100) { if (cache.size <= 450) break; Thread.sleep(10) }
        assertTrue("Cache size ${cache.size}", cache.size <= 450)
        assertNotNull(cache.openSnapshot("one")?.also { it.close() })
        assertNull(cache.openSnapshot("two"))
        cache.shutdown()
    }
}
