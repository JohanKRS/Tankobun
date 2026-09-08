package com.tankobun.core.extensions

import com.tankobun.core.model.ReadingContentKind
import eu.kanade.tachiyomi.source.NovelSource
import eu.kanade.tachiyomi.source.Source

const val NOVEL_EXTENSION_PREFIX = "eu.kanade.tachiyomi.novelextension"
const val NOVEL_EXTENSION_FEATURE = "tachiyomi.novelextension"

fun isSupportedExtensionPackageName(name: String): Boolean =
    name.startsWith("${InstalledExtensionScanner.TACHIYOMI_EXTENSION_PREFIX}.") ||
        name.startsWith("$NOVEL_EXTENSION_PREFIX.")

fun Source.readingContentKind(): ReadingContentKind =
    if (this is NovelSource || runCatching { isNovelSource }.getOrDefault(false)) {
        ReadingContentKind.NOVEL
    } else {
        ReadingContentKind.MANGA
    }
