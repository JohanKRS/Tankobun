package com.tankobun.app.source

import com.tankobun.core.model.SourceDescriptor

/** Keep a saved package identity even while that extension is unavailable or awaiting trust. */
internal fun List<SourceDescriptor>.sourceFor(
    sourceId: Long,
    packageName: String,
    sourceName: String? = null,
    sourceLang: String? = null,
): SourceDescriptor? {
    if (packageName.isBlank()) return firstOrNull { it.id == sourceId }
    val packageSources = filter { it.packageName == packageName }
    return packageSources.firstOrNull { it.id == sourceId }
        ?: packageSources.firstOrNull {
            sourceName != null && sourceLang != null &&
                it.name.equals(sourceName, ignoreCase = true) &&
                it.lang.equals(sourceLang, ignoreCase = true)
        }
        ?: packageSources.singleOrNull()
}
