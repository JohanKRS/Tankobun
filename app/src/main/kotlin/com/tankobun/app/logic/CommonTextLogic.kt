package com.tankobun.app.logic

import java.util.Locale

internal fun Iterable<String>.normalizedCustomLists(): List<String> =
    map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase(Locale.ROOT) }

