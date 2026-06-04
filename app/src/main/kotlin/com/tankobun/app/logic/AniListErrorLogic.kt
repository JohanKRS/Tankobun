package com.tankobun.app.logic

import com.tankobun.core.anilist.AnilistGraphQlException

internal fun Throwable.userMessage(fallback: String): String = when (this) {
    is AnilistGraphQlException -> when (statusCode) {
        401 -> "AniList session expired. Sign in again."
        429 -> "AniList is rate limiting requests. Try again in a minute."
        500 -> "AniList returned a server error while syncing. Try again in a moment."
        else -> "AniList request failed${statusCode?.let { " ($it)" }.orEmpty()}."
    }
    else -> message ?: fallback
}
