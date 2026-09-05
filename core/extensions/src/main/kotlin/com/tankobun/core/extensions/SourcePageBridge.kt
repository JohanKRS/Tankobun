package com.tankobun.core.extensions

import android.net.Uri
import com.tankobun.core.model.ReaderPage
import eu.kanade.tachiyomi.source.model.Page

internal fun List<Page>.toReaderPages(headers: Map<String, String> = emptyMap()): List<ReaderPage> =
    mapIndexed { position, page ->
        ReaderPage(
            index = position,
            imageUrl = page.imageUrl ?: page.uri?.toString() ?: page.url,
            cachedFilePath = null,
            headers = headers,
            sourcePageUrl = page.url,
            imageUrlResolved = page.imageUrl != null || page.uri != null,
            sourcePageIndex = page.index,
            sourcePageUri = page.uri?.toString(),
        )
    }

internal fun ReaderPage.toSourcePage(): Page = Page(
    index = sourcePageIndex ?: index,
    url = if (sourcePageIndex != null) sourcePageUrl else sourcePageUrl.ifBlank { imageUrl },
    imageUrl = imageUrl.takeIf { imageUrlResolved && sourcePageUri?.isLocalPageUri() != true },
    uri = sourcePageUri?.let(Uri::parse),
)

// Some extensions populate the deprecated URI field with an HTTP address too.
// Only Android-readable local schemes belong in ContentResolver; remote pages
// must still use the extension's client, authentication and request builder.
internal fun String.isLocalPageUri(): Boolean =
    substringBefore(':', "").lowercase(java.util.Locale.ROOT) in setOf("content", "file", "android.resource")
