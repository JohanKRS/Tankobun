package com.tankobun.core.extensions

import org.junit.Assert.*
import org.junit.Test

class ExtensionPackageStoreTest {
    @Test fun migrationPrefersThePrivateCopyWithoutDuplicatingTheSystemPackage() {
        assertTrue(preferPrivateExtension(1, 1, setOf("a"), setOf("a")))
        assertTrue(preferPrivateExtension(1, 2, setOf("a"), setOf("a")))
        assertFalse(preferPrivateExtension(3, 2, setOf("a"), setOf("a")))
    }
    @Test fun aDifferentSignerCannotShadowAnInstalledAndroidExtension() {
        assertFalse(preferPrivateExtension(1, 99, setOf("a"), setOf("b")))
        assertFalse(preferPrivateExtension(1, 99, emptySet(), emptySet()))
        assertFalse(preferPrivateExtension(1, 99, setOf("a", "b"), setOf("a")))
        assertTrue(preferPrivateExtension(1, 2, setOf("a"), setOf("a", "rotated")))
    }
    @Test fun onlyCommunityExtensionPackageIdentifiersCanBecomeStoragePaths() {
        assertTrue(validExtensionStoragePackage("eu.kanade.tachiyomi.extension.en.fixture"))
        assertTrue(validExtensionStoragePackage("eu.kanade.tachiyomi.novelextension.en.fixture"))
        listOf("", "android", "com.other.app", "eu.kanade.tachiyomi.extension../fixture", "eu.kanade.tachiyomi.extension.en.fixture/../other", "eu.kanade.tachiyomi.extension.").forEach {
            assertFalse(it, validExtensionStoragePackage(it))
        }
    }
}
