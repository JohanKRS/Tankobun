package com.tankobun.core.network

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class InputLimitExceededException(val maxBytes: Int) : IOException("File exceeds the allowed size ($maxBytes bytes)")

/** Enforces the actual bytes read, including streams with no declared length. */
fun InputStream.readBytesLimited(maxBytes: Int, checkActive: () -> Unit = {}): ByteArray {
    require(maxBytes >= 0)
    val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        checkActive()
        val remaining = maxBytes - output.size()
        // Read one extra byte to distinguish an exact-limit file from an oversized one.
        val read = read(buffer, 0, minOf(buffer.size.toLong(), remaining.toLong() + 1).toInt())
        if (read < 0) return output.toByteArray()
        if (read > remaining) throw InputLimitExceededException(maxBytes)
        if (read == 0) {
            val byte = read()
            if (byte < 0) return output.toByteArray()
            if (remaining == 0) throw InputLimitExceededException(maxBytes)
            output.write(byte)
        } else {
            output.write(buffer, 0, read)
        }
    }
}
