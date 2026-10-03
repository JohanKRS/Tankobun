package com.tankobun.core.model

private val mangaBakaCover = Regex("^(https://cdn\\.mangabaka\\.dev/imgproxy/plain/x350@)([123])(/.+)$")

/** Keep a fetched larger variant of the same asset when a search returns its thumbnail again. */
fun String?.withCoverFallback(fallback: String?): String? {
    if (this == null) return fallback
    val incoming = mangaBakaCover.matchEntire(this) ?: return this
    val cached = fallback?.let(mangaBakaCover::matchEntire) ?: return this
    return if (incoming.groupValues[3] == cached.groupValues[3] && cached.groupValues[2] > incoming.groupValues[2]) fallback else this
}

/**
 * MangaBaka lists send the 1× rendition of a cover (350 px tall). The 3× rendition of the same
 * asset is capped at the original's size, so it is the sharpest copy without another request.
 * Meant for covers shown large; grids keep the light one. Other URLs are returned unchanged.
 */
fun String?.sharpestMangaBakaCover(): String? {
    val match = this?.let(mangaBakaCover::matchEntire) ?: return this
    return match.groupValues[1] + "3" + match.groupValues[3]
}
