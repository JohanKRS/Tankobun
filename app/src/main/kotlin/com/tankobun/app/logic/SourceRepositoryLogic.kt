package com.tankobun.app.logic

import com.tankobun.core.extensions.ExtensionIndexEntry
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal data class SourceRepositoryOption(val url: String, val name: String)

/** An APK can be offered by several repositories. Keep its download/signing metadata in each one. */
internal fun latestExtensionsPerRepository(entries: List<ExtensionIndexEntry>): List<ExtensionIndexEntry> =
    entries.groupBy { it.repositoryUrl to it.packageName }.values.map { versions -> versions.maxBy { it.versionCode } }

internal fun sourceRepositoryOptions(urls: List<String>, customNames: Map<String, String> = emptyMap()): List<SourceRepositoryOption> {
    val options = urls.filter { it.isNotBlank() }.distinct().map { url ->
        val parsed = url.toHttpUrlOrNull()
        val segments = parsed?.pathSegments.orEmpty().filter { it.isNotBlank() }
        val name = when {
            !customNames[url].isNullOrBlank() -> customNames.getValue(url)
            parsed == null -> url
            parsed.host in setOf("github.com", "raw.githubusercontent.com") && segments.size >= 2 ->
                segments.take(2).joinToString("/")
            else -> (listOf(parsed.host) + segments.dropLast(1)).joinToString("/")
        }
        SourceRepositoryOption(url, name)
    }
    val repeatedNames = options.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
    return options.mapIndexed { index, option ->
        if (option.name in repeatedNames) option.copy(name = "${index + 1} · ${option.name}") else option
    }
}

internal fun normalizedRepositoryNames(names: Map<String, String>, urls: List<String>): Map<String, String> =
    names.filterKeys { it in urls }.mapValues { (_, name) -> name.trim().replace(Regex("\\s+"), " ").take(80) }
        .filterValues { it.isNotBlank() }

/** Index membership, not a claim about where an Android-installed APK was originally downloaded. */
internal fun sourceRepositoriesByPackage(entries: List<ExtensionIndexEntry>): Map<String, Set<String>> =
    entries.groupBy { it.packageName }.mapValues { (_, versions) ->
        versions.mapNotNull { it.repositoryUrl.takeIf(String::isNotBlank) }.toSet()
    }

internal fun sourceRepositoryVisible(repositories: Set<String>, hiddenRepositories: Set<String>): Boolean =
    repositories.isEmpty() || repositories.any { it !in hiddenRepositories }

/** Shared packages appear once, using a visible repository's own download/signing metadata. */
internal fun visibleRepositoryExtensions(entries: List<ExtensionIndexEntry>, hiddenRepositories: Set<String>): List<ExtensionIndexEntry> =
    entries.filter { it.repositoryUrl !in hiddenRepositories }
        .groupBy { it.packageName }.values.map { versions -> versions.maxBy { it.versionCode } }
