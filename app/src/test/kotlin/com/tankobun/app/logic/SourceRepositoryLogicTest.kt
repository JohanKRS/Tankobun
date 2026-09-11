package com.tankobun.app.logic

import com.tankobun.core.extensions.ExtensionIndexEntry
import org.junit.Assert.*
import org.junit.Test

class SourceRepositoryLogicTest {
    private val first = "https://example.invalid/panels/index.json"
    private val second = "https://example.invalid/words/index.json"
    private fun entry(repo: String, version: Int = 1) = ExtensionIndexEntry(
        "Paper", "example.paper", "paper.apk", "en", version, "$version.0", repositoryUrl = repo,
        repositorySigningKey = repo,
    )

    @Test fun sharedPackagesKeepEachRepositoryDownloadAndSigningMetadata() {
        val entries = latestExtensionsPerRepository(listOf(entry(first), entry(second), entry(first, 2)))
        assertEquals(2, entries.size)
        assertEquals(entry(first, 2), entries.first())
        assertEquals(entry(second), entries.last())
        val membership = sourceRepositoriesByPackage(entries).getValue("example.paper")
        assertEquals(setOf(first, second), membership)
        assertTrue(sourceRepositoryVisible(membership, setOf(first)))
        assertTrue(sourceRepositoryVisible(membership, setOf(second)))
        assertFalse(sourceRepositoryVisible(membership, setOf(first, second)))
        // Removing one repository must not erase an extension offered by the other.
        assertEquals(setOf(second), sourceRepositoriesByPackage(entries.filterNot { it.repositoryUrl == first }).getValue("example.paper"))
    }

    @Test fun unidentifiedSourcesStayVisibleWithoutInventingAnOrigin() {
        val memberships = sourceRepositoriesByPackage(listOf(entry("")))
        assertTrue(memberships.getValue("example.paper").isEmpty())
        assertTrue(sourceRepositoryVisible(emptySet(), emptySet()))
        assertTrue(sourceRepositoryVisible(emptySet(), setOf(first, second)))
        assertFalse(sourceRepositoryVisible(setOf(first), setOf(first)))
    }

    @Test fun visibilitySupportsAnyCombinationWithoutDuplicatingSharedExtensions() {
        val entries = listOf(entry(first), entry(second, 2))
        assertEquals(listOf(entry(second, 2)), visibleRepositoryExtensions(entries, emptySet()))
        assertEquals(listOf(entry(first)), visibleRepositoryExtensions(entries, setOf(second)))
        assertEquals(listOf(entry(second, 2)), visibleRepositoryExtensions(entries, setOf(first)))
        assertTrue(visibleRepositoryExtensions(entries, setOf(first, second)).isEmpty())
        assertEquals(listOf(entry(second, 2)), visibleRepositoryExtensions(entries, setOf("unrelated")))
    }

    @Test fun arbitrarilyManyRepositoriesKeepTheirUrlIdentityAndDistinctReadableNames() {
        val urls = (1..50).map { "https://example.invalid/repository-$it/index.json" }
        val options = sourceRepositoryOptions(urls + urls + "")
        assertEquals(urls, options.map { it.url })
        assertEquals(50, options.map { it.name }.toSet().size)
        assertEquals("example.invalid/repository-50", options.last().name)
        val aliases = sourceRepositoryOptions(listOf(first, first.replace("index.json", "alternate.json")))
        assertEquals(2, aliases.map { it.name }.toSet().size)
    }

    @Test fun renamingDoesNotChangeFilterIdentityAndBlankNamesRestoreAutomaticLabels() {
        val names = normalizedRepositoryNames(mapOf(first to "  My\n panels  ", second to " ", "removed" to "Old"), listOf(first, second))
        assertEquals(mapOf(first to "My panels"), names)
        val options = sourceRepositoryOptions(listOf(first, second), names)
        assertEquals(first, options.first().url)
        assertEquals("My panels", options.first().name)
        assertEquals("example.invalid/words", options.last().name)
        val duplicateNames = sourceRepositoryOptions(listOf(first, second), mapOf(first to "Reading", second to "Reading"))
        assertEquals(2, duplicateNames.map { it.name }.toSet().size)
    }
}
