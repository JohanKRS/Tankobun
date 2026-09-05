package com.tankobun.app.source

import com.tankobun.core.model.SourceDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceBindingResolutionTest {
    private val source = SourceDescriptor(42, "Original source", "en", "fixture.extension", "1", 1, false, true)

    @Test fun sameIdAndNameInAnotherPackageCannotReplaceAnUnavailableExtension() {
        val other = source.copy(packageName = "another.extension")
        assertNull(listOf(other).sourceFor(source.id, source.packageName, source.name, source.lang))
    }

    @Test fun exactPackageAndIdTakePriority() {
        val other = source.copy(packageName = "another.extension")
        assertEquals(source, listOf(other, source).sourceFor(source.id, source.packageName))
    }

    @Test fun aSingleSourceCanKeepItsBindingAfterItsRuntimeIdChanges() {
        val updated = source.copy(id = 99)
        assertEquals(updated, listOf(updated).sourceFor(source.id, source.packageName))
    }

    @Test fun aFactorySourceCanBeRecoveredByNameAndLanguageInsideItsPackage() {
        val updated = source.copy(id = 99)
        val translated = source.copy(id = 100, lang = "es")
        assertEquals(updated, listOf(translated, updated).sourceFor(source.id, source.packageName, source.name, "EN"))
    }

    @Test fun ambiguousSourcesAreNotChosenWithoutAnIdentityMatch() {
        assertNull(listOf(source.copy(id = 99), source.copy(id = 100)).sourceFor(source.id, source.packageName))
    }

    @Test fun legacyBindingsWithoutAPackageCanStillResolveTheirId() {
        assertEquals(source, listOf(source).sourceFor(source.id, ""))
    }
}
