package com.tankobun.core.network

import java.io.InputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BoundedInputTest {
    @Test
    fun permitsAnExactLimitAndEmptyFiles() {
        assertArrayEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 3).inputStream().readBytesLimited(3))
        assertArrayEquals(byteArrayOf(), byteArrayOf().inputStream().readBytesLimited(0))
    }

    @Test
    fun stopsAnEndlessProviderAfterTheLimitPlusOneByte() {
        var consumed = 0
        val endless = object : InputStream() {
            override fun read(): Int { consumed++; return 1 }
            override fun available(): Int = 0 // Provider metadata is not a size guarantee.
        }
        assertThrows(InputLimitExceededException::class.java) { endless.readBytesLimited(12) }
        assertEquals(13, consumed)
    }

    @Test
    fun supportsShortReadsAndChecksCancellationBetweenChunks() {
        val data = "çã漢字".toByteArray()
        val shortReads = object : InputStream() {
            var offset = 0
            override fun read(): Int = if (offset == data.size) -1 else data[offset++].toInt() and 255
            override fun read(bytes: ByteArray, off: Int, len: Int): Int {
                val next = read()
                if (next < 0) return -1
                bytes[off] = next.toByte()
                return 1
            }
        }
        var checks = 0
        assertArrayEquals(data, shortReads.readBytesLimited(data.size) { checks++ })
        assertEquals(data.size + 1, checks)
    }
}
