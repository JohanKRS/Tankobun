package com.tankobun.app.logic

import android.content.Context
import com.tankobun.app.R
import com.tankobun.core.anilist.AnilistGraphQlException

internal fun Throwable.userMessage(context: Context, fallback: String): String = when (this) {
    is AnilistGraphQlException -> when (statusCode) {
        401 -> context.getString(R.string.anilist_error_session_expired)
        429 -> context.getString(R.string.anilist_error_rate_limited)
        500 -> context.getString(R.string.anilist_error_server)
        else -> context.getString(
            R.string.anilist_error_request_failed,
            statusCode?.let { context.getString(R.string.anilist_error_status_suffix, it) }.orEmpty(),
        )
    }
    else -> message ?: fallback
}

internal fun Throwable.userMessage(fallback: String): String = message ?: fallback
