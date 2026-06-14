package com.tankobun.app.logic

import com.tankobun.app.UNIVERSAL_SOURCE_LANGUAGE
import com.tankobun.core.model.SourceDescriptor
import java.util.Locale

internal fun List<SourceDescriptor>.visibleSources(): List<SourceDescriptor> =
    distinctBy { "${it.packageName}:${it.id}:${it.name}:${it.lang}" }
        .sortedWith(compareBy<SourceDescriptor> { it.normalizedLanguage() }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.lang })

internal fun List<SourceDescriptor>.preferredVisibleSources(
    preferredLanguages: Set<String>,
    disabledSourceKeys: Set<String> = emptySet(),
): List<SourceDescriptor> {
    val preferredSources = filter {
        val language = it.normalizedLanguage()
        language in preferredLanguages || language == UNIVERSAL_SOURCE_LANGUAGE
    }
    return (preferredSources.ifEmpty { this })
        .filterNot { it.sourceSettingsKey() in disabledSourceKeys }
        .distinctBy { "${it.packageName}:${it.id}:${it.name}:${it.lang}" }
        .sortedWith(compareBy<SourceDescriptor> { it.languageSortPriority(preferredLanguages) }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.lang })
}

internal fun preserveSelectedSourceOrFirst(
    selectedSourceId: Long?,
    selectedSourcePackageName: String?,
    visibleSources: List<SourceDescriptor>,
    allSources: List<SourceDescriptor> = visibleSources,
): SourceDescriptor? =
    selectedSourceId?.let { sourceId ->
        allSources.matchingSelectedSource(sourceId, selectedSourcePackageName)
            ?: visibleSources.matchingSelectedSource(sourceId, selectedSourcePackageName)
            ?: visibleSources.firstOrNull()
    } ?: visibleSources.firstOrNull()

internal fun SourceDescriptor.languageSortPriority(preferredLanguages: Set<String>): Int =
    when (normalizedLanguage()) {
        "en" -> 0
        Locale.getDefault().language.lowercase(Locale.ROOT) -> 1
        Locale.getDefault().toLanguageTag().lowercase(Locale.ROOT) -> 1
        "all" -> 2
        else -> if (normalizedLanguage() in preferredLanguages) 3 else 4
    }

internal fun SourceDescriptor.normalizedLanguage(): String =
    lang.lowercase(Locale.ROOT).replace('_', '-')

internal fun SourceDescriptor.sourceSettingsKey(): String =
    "$packageName:$id"

private fun List<SourceDescriptor>.matchingSelectedSource(
    sourceId: Long,
    packageName: String?,
): SourceDescriptor? =
    if (packageName == null) {
        firstOrNull { it.id == sourceId }
    } else {
        firstOrNull { it.id == sourceId && it.packageName == packageName }
    }

