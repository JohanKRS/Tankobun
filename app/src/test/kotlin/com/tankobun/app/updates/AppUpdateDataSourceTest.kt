package com.tankobun.app.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateDataSourceTest {
    @Test
    fun parseTankobunUpdateManifestReadsStableRelease() {
        val json = """
            {
              "type": "tankobun.update-manifest",
              "version": 1,
              "stable": {
                "versionCode": 21,
                "versionName": "2.0.1",
                "apkUrl": "https://github.com/JohanKRS/Tankobun/releases/download/v2.0.1/tankobun-2.0.1.apk",
                "apkSha256": "sha256:abc123",
                "releaseUrl": "https://github.com/JohanKRS/Tankobun/releases/tag/v2.0.1",
                "publishedAt": "2026-06-13T00:00:00Z",
                "sizeBytes": 42,
                "mandatory": false,
                "changelog": {
                  "en": ["Fixes"],
                  "pt-BR": ["Correções"],
                  "es": "Correcciones"
                }
              }
            }
        """.trimIndent()

        val update = parseTankobunUpdateManifestJson(json)

        assertEquals(21, update.versionCode)
        assertEquals("2.0.1", update.versionName)
        assertEquals("abc123", update.apkSha256?.removePrefix("sha256:"))
        assertEquals(42L, update.sizeBytes)
        assertEquals(listOf("Fixes"), update.changelog["en"])
        assertEquals(listOf("Correções"), update.changelog["pt-BR"])
        assertEquals(listOf("Correcciones"), update.changelog["es"])
    }

    @Test
    fun parseTankobunUpdateManifestAllowsMissingOptionalFields() {
        val json = """
            {
              "type": "tankobun.update-manifest",
              "version": 1,
              "stable": {
                "versionCode": 20,
                "versionName": "2.0.0",
                "apkUrl": "https://example.test/tankobun.apk"
              }
            }
        """.trimIndent()

        val update = parseTankobunUpdateManifestJson(json)

        assertEquals(20, update.versionCode)
        assertNull(update.apkSha256)
        assertNull(update.releaseUrl)
        assertNull(update.sizeBytes)
        assertTrue(update.changelog.isEmpty())
    }
}
