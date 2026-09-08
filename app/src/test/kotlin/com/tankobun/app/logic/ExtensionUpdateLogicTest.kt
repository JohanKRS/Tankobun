package com.tankobun.app.logic

import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.extensions.novel.LnReaderPlugin
import com.tankobun.core.model.SourceDescriptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExtensionUpdateLogicTest {
    @Test fun selectsNewestInstalledPackageUpdateOnceAcrossMultipleSourcesAndRepositories() {
        val old = entry("panels", 1)
        val current = entry("current", 4)
        val available = listOf(entry("new", 2), old, entry("panels", 3), entry("panels", 2), current, entry("current", 3), entry("unknown", 9))
        val installed = listOf(source(old, 1), source(old, 2), source(current), source(entry("unknown", 1)).copyVersion(null))
        assertEquals(listOf(entry("panels", 3)), pendingExtensionUpdates(installed, available))
    }

    @Test fun includesNovelPluginsAlongsideApksAndUsesHighestKnownInstalledVersion() {
        val plugin = LnReaderPlugin("qa", "Novel QA", "https://example.invalid", "English", "1.0.0", "https://example.invalid/qa.js", repositoryUrl = "https://example.invalid/index.json")
        val newer = plugin.copy(version = "1.1.0").indexEntry()
        val apk = entry("panels", 1)
        assertEquals(setOf(newer, entry("panels", 2)), pendingExtensionUpdates(
            listOf(source(plugin.indexEntry()), source(apk), source(entry("current", 2)), source(entry("current", 4))),
            listOf(newer, entry("panels", 2), entry("current", 3))).toSet())
    }

    @Test fun waitsForInstallerCompletionBeforeStartingTheNextDownload() = runBlocking {
        val result = CompletableDeferred<ExtensionUpdateResult>()
        val started = mutableListOf<String>()
        val task = async(start = CoroutineStart.UNDISPATCHED) {
            runExtensionUpdates(listOf(entry("first"), entry("second")), { false }, {}) {
                started += it.packageName
                if (started.size == 1) result.await() else ExtensionUpdateResult.UPDATED
            }
        }
        assertEquals(listOf("first"), started)
        assertFalse(task.isCompleted)
        result.complete(ExtensionUpdateResult.UPDATED)
        assertEquals(ExtensionUpdateSummary(2, 0, false), task.await())
        assertEquals(listOf("first", "second"), started)
    }

    @Test fun failureDoesNotBlockFollowingUpdatesAndSkippedPackagesAreNotCountedAsUpdated() = runBlocking {
        val progress = mutableListOf<ExtensionUpdateProgress>()
        val summary = runExtensionUpdates(listOf(entry("failure"), entry("skipped"), entry("success"), entry("success")), { false }, progress::add) {
            when (it.packageName) {
                "failure" -> error("download unavailable")
                "skipped" -> ExtensionUpdateResult.SKIPPED
                else -> ExtensionUpdateResult.UPDATED
            }
        }
        assertEquals(ExtensionUpdateSummary(1, 1, false), summary)
        assertEquals(listOf(0, 1, 2), progress.map { it.completed })
        assertTrue(progress.all { it.total == 3 })
    }

    @Test fun stoppingFinishesCurrentInstallAndLeavesLaterEntriesUntouched() = runBlocking {
        var stop = false
        val started = mutableListOf<String>()
        val summary = runExtensionUpdates(listOf(entry("first"), entry("second")), { stop }, {}) {
            started += it.packageName
            stop = true
            ExtensionUpdateResult.UPDATED
        }
        assertEquals(ExtensionUpdateSummary(1, 0, true), summary)
        assertEquals(listOf("first"), started)
    }

    @Test fun cancellingAndroidInstallerStopsTheQueue() = runBlocking {
        val started = mutableListOf<String>()
        val summary = runExtensionUpdates(listOf(entry("first"), entry("second")), { false }, {}) {
            started += it.packageName
            ExtensionUpdateResult.CANCELLED
        }
        assertEquals(ExtensionUpdateSummary(0, 0, true), summary)
        assertEquals(listOf("first"), started)
    }

    @Test fun coroutineCancellationIsNotReportedAsDownloadFailure() = runBlocking {
        var calls = 0
        try {
            runExtensionUpdates(listOf(entry("first"), entry("second")), { false }, {}) {
                calls++
                throw CancellationException("screen owner destroyed")
            }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertEquals(1, calls)
        }
    }

    private fun entry(name: String, version: Int = 2) = ExtensionIndexEntry(name, name, "$name.apk", "en", version, "$version.0")
    private fun source(entry: ExtensionIndexEntry, id: Long = 1) = SourceDescriptor(id, entry.name, "en", entry.packageName, entry.versionName, entry.versionCode, false, true)
    private fun SourceDescriptor.copyVersion(version: Int?) = copy(versionCode = version)
}
