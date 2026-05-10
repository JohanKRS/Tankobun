package com.tankobun.core.extensions

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlin.jvm.internal.DefaultConstructorMarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TachiyomiSourceHostTest {
    @Test
    fun expandsManifestRelativeSourceClassNames() {
        val packageName = "eu.kanade.tachiyomi.extension.all.mangadex"

        assertEquals(
            "eu.kanade.tachiyomi.extension.all.mangadex.MangaDexFactory",
            ".MangaDexFactory".toFullyQualifiedSourceClassName(packageName),
        )
        assertEquals(
            "eu.kanade.tachiyomi.extension.all.mangadex.MangaDexFactory",
            "MangaDexFactory".toFullyQualifiedSourceClassName(packageName),
        )
        assertEquals(
            "other.package.Factory",
            "other.package.Factory".toFullyQualifiedSourceClassName(packageName),
        )
    }

    @Test
    fun httpSourceHeadersAlwaysHaveNonNullUserAgent() {
        System.clearProperty("http.agent")

        val source = object : HttpSource() {
            override val baseUrl: String = "https://example.test"
            override val name: String = "Fixture"
            override val lang: String = "en"
        }

        assertNotNull(source.headers["User-Agent"])
    }

    @Test
    fun pageKeepsLegacyConstructorAndModernUriProperty() {
        val page = Page(1, "/chapter/1", "https://example.test/1.jpg")

        assertEquals(1, page.index)
        assertEquals("/chapter/1", page.url)
        assertEquals("https://example.test/1.jpg", page.imageUrl)
        assertEquals(null, page.uri)
    }

    @Test
    fun pageExposesModernDefaultArgumentConstructorForExtensions() {
        val constructor = Page::class.java.getDeclaredConstructor(
            Int::class.javaPrimitiveType,
            String::class.java,
            String::class.java,
            android.net.Uri::class.java,
            Int::class.javaPrimitiveType,
            DefaultConstructorMarker::class.java,
        )

        assertNotNull(constructor)
    }
}
