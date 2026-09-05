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
    imageUrl = imageUrl.takeIf { imageUrlResolved && sourcePageUri == null },
    uri = sourcePageUri?.let(Uri::parse),
)
