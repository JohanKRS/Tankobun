package com.tankobun.core.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

class InstalledExtensionScannerTest {
    @Test
    fun readsLanguageFromTachiyomiPackageSegment() {
        assertEquals(
            "en",
            extensionLanguageFromPackage("eu.kanade.tachiyomi.extension.en.atsumaru"),
        )
        assertEquals(
            "all",
            extensionLanguageFromPackage("eu.kanade.tachiyomi.extension.all.fixture"),
        )
    }
}
