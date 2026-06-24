package com.tankobun.app.logic

import com.tankobun.core.model.SourceDescriptor
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceSettingsLogicTest {
    @Test
    fun preserveSelectedSourceKeepsSourceHiddenByLanguageFilter() {
        val english = source(id = 1, lang = "en", packageName = "pkg.en")
        val portuguese = source(id = 2, lang = "pt-BR", packageName = "pkg.pt")
        val allSources = listOf(english, portuguese)
        val visibleSources = allSources.preferredVisibleSources(
            preferredLanguages = setOf("en"),
        )

        val selected = preserveSelectedSourceOrFirst(
            selectedSourceId = portuguese.id,
            selectedSourcePackageName = portuguese.packageName,
            visibleSources = visibleSources,
            allSources = allSources,
        )

        assertEquals(portuguese, selected)
    }

    @Test
    fun preserveSelectedSourceKeepsSourceHiddenByDisabledKey() {
        val english = source(id = 1, lang = "en", packageName = "pkg.en")
        val portuguese = source(id = 2, lang = "pt-BR", packageName = "pkg.pt")
        val allSources = listOf(english, portuguese)
        val visibleSources = allSources.preferredVisibleSources(
            preferredLanguages = setOf("en", "pt-br"),
            disabledSourceKeys = setOf(portuguese.sourceSettingsKey()),
        )

        val selected = preserveSelectedSourceOrFirst(
            selectedSourceId = portuguese.id,
            selectedSourcePackageName = portuguese.packageName,
            visibleSources = visibleSources,
            allSources = allSources,
        )

        assertEquals(portuguese, selected)
    }

    @Test
    fun preserveSelectedSourceFallsBackWhenSourceIsUninstalled() {
        val english = source(id = 1, lang = "en", packageName = "pkg.en")

        val selected = preserveSelectedSourceOrFirst(
            selectedSourceId = 2,
            selectedSourcePackageName = "pkg.pt",
            visibleSources = listOf(english),
            allSources = listOf(english),
        )

        assertEquals(english, selected)
    }

    @Test
    fun preserveSelectedSourceCanAvoidFallbackWhenBindingIsTemporarilyMissing() {
        val english = source(id = 1, lang = "en", packageName = "pkg.en")

        val selected = preserveSelectedSourceOrFirst(
            selectedSourceId = 2,
            selectedSourcePackageName = "pkg.pt",
            visibleSources = listOf(english),
            allSources = listOf(english),
            fallbackToFirst = false,
        )

        assertEquals(null, selected)
    }

    private fun source(
        id: Long,
        lang: String,
        packageName: String,
        name: String = "Source $id",
    ): SourceDescriptor =
        SourceDescriptor(
            id = id,
            name = name,
            lang = lang,
            packageName = packageName,
            versionName = null,
            versionCode = null,
            isNsfw = false,
            installed = true,
        )
}
