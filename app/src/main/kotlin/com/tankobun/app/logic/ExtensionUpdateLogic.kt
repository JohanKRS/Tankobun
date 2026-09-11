package com.tankobun.app.logic

import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.model.SourceDescriptor
import kotlinx.coroutines.CancellationException

internal fun pendingExtensionUpdates(installed: List<SourceDescriptor>, available: List<ExtensionIndexEntry>): List<ExtensionIndexEntry> {
    val versions = installed.groupBy { it.packageName }.mapValues { (_, sources) -> sources.mapNotNull { it.versionCode }.maxOrNull() }
    return available.groupBy { it.packageName }.values.map { entries -> entries.maxBy { it.versionCode } }
        .filter { entry -> versions[entry.packageName]?.let { entry.versionCode > it } == true }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
}

enum class ExtensionUpdateResult { UPDATED, SKIPPED, FAILED, CANCELLED }
data class ExtensionUpdateProgress(val total: Int, val completed: Int = 0, val updated: Int = 0,
    val failed: Int = 0, val currentName: String = "", val stopping: Boolean = false)
data class ExtensionUpdateSummary(val updated: Int, val failed: Int, val stopped: Boolean)

/** One install, including its Android result, finishes before the next download starts. */
internal suspend fun runExtensionUpdates(
    entries: List<ExtensionIndexEntry>,
    shouldStop: () -> Boolean,
    onProgress: (ExtensionUpdateProgress) -> Unit,
    install: suspend (ExtensionIndexEntry) -> ExtensionUpdateResult,
): ExtensionUpdateSummary {
    val unique = entries.distinctBy { it.packageName }
    var progress = ExtensionUpdateProgress(unique.size)
    for (entry in unique) {
        if (shouldStop()) return ExtensionUpdateSummary(progress.updated, progress.failed, true)
        onProgress(progress.copy(currentName = entry.name))
        val result = try { install(entry) } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { ExtensionUpdateResult.FAILED }
        if (result == ExtensionUpdateResult.CANCELLED) return ExtensionUpdateSummary(progress.updated, progress.failed, true)
        progress = progress.copy(completed = progress.completed + 1,
            updated = progress.updated + if (result == ExtensionUpdateResult.UPDATED) 1 else 0,
            failed = progress.failed + if (result == ExtensionUpdateResult.FAILED) 1 else 0)
    }
    return ExtensionUpdateSummary(progress.updated, progress.failed, false)
}
