package com.tankobun.core.extensions

import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The combined API permits only one update of a given manga at a time. */
internal class SourceUpdateGate {
    private data class Key(val source: Source, val url: String)
    private class Entry(val mutex: Mutex = Mutex(), var users: Int = 0)
    private val entries = mutableMapOf<Key, Entry>()

    suspend fun <T> run(source: Source, url: String, block: suspend () -> T): T {
        val key = Key(source, url)
        val entry = synchronized(entries) { entries.getOrPut(key) { Entry() }.also { it.users++ } }
        try {
            return entry.mutex.withLock { block() }
        } finally {
            synchronized(entries) {
                if (--entry.users == 0) entries.remove(key)
            }
        }
    }
}
