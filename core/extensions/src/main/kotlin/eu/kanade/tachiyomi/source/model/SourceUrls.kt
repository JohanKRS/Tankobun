package eu.kanade.tachiyomi.source.model

import java.net.URI

internal fun sourceUrlWithoutDomain(url: String): String = runCatching {
    val uri = URI(url.replace(" ", "%20"))
    if (uri.isOpaque) return url
    buildString {
        append(uri.rawPath.orEmpty().ifEmpty { if (uri.rawAuthority != null) "/" else "" })
        uri.rawQuery?.let { append('?').append(it) }
        uri.rawFragment?.let { append('#').append(it) }
    }
}.getOrDefault(url)
