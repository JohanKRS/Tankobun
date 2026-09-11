package com.tankobun.app

import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.model.SourceDescriptor

/** Display-only original fixtures for filtering; no source executable, content or external repository. */
internal fun repositoryFilterQaUrls(): List<String> = (1..12).map { "https://example.invalid/catalog-$it/index.json" }

internal fun repositoryFilterQaState(state: TankobunUiState): TankobunUiState {
    val repositories = repositoryFilterQaUrls()
    fun entry(name: String, pkg: String, repository: Int, language: String = "en", version: Int = 1) =
        ExtensionIndexEntry(name, pkg, "fixture.apk", language, version, "$version.0", repositoryUrl = repositories[repository - 1])
    val entries = listOf(
        entry("Paper Panels", "example.panels", 1),
        entry("Paper Panels", "example.panels", 2, version = 2),
        entry("Paper Words", "example.words", 2),
        entry("Paper Leaves", "example.leaves", 3, "pt-br"),
    )
    val installed = listOf(
        SourceDescriptor(1, "Paper Panels", "en", "example.panels", "2.0", 2, false, true),
        SourceDescriptor(2, "Paper Words", "en", "example.words", "1.0", 1, false, true),
        SourceDescriptor(3, "Paper Offline", "en", "example.offline", "1.0", 1, false, true),
    )
    return state.copy(extensionRepositories = repositories, availableExtensions = entries,
        allInstalledSources = installed, installedSources = installed, untrustedExtensions = emptyList(),
        sourceLanguages = setOf("en"), message = null)
}
